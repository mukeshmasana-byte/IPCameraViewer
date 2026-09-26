package com.example.ipcameraviewer.onvif

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import android.util.Base64

class OnvifRequestException(message: String, cause: Throwable? = null) : Exception(message, cause)

class OnvifClient {
    suspend fun inspect(device: DiscoveredOnvifDevice, credentials: OnvifCredentials): OnvifCameraDetails = withContext(Dispatchers.IO) {
        val deviceInfoXml = request(device.endpoint, ACTION_DEVICE_INFO, "<tds:GetDeviceInformation/>", credentials)
        val info = runCatching { OnvifXmlParser.parseDeviceInfo(deviceInfoXml) }.getOrElse {
            throw OnvifRequestException("The camera returned invalid device information.", it)
        }
        val capabilities = runCatching {
            val xml = request(
                device.endpoint, ACTION_CAPABILITIES,
                "<tds:GetCapabilities><tds:Category>All</tds:Category></tds:GetCapabilities>", credentials,
            )
            OnvifXmlParser.parseCapabilities(xml)
        }.getOrDefault(OnvifCapabilities(null, null))
        val mediaEndpoint = capabilities.mediaEndpoint ?: device.endpoint
        val profilesXml = runCatching {
            request(mediaEndpoint, ACTION_PROFILES, "<trt:GetProfiles/>", credentials)
        }.recoverCatching {
            if (mediaEndpoint == device.endpoint) throw it
            request(device.endpoint, ACTION_PROFILES, "<trt:GetProfiles/>", credentials)
        }.getOrElse { failure ->
            throw OnvifRequestException(failure.message ?: "Could not retrieve camera stream profiles.", failure)
        }
        val profiles = runCatching { OnvifXmlParser.parseProfiles(profilesXml) }.getOrElse {
            throw OnvifRequestException("The camera returned invalid media profiles.", it)
        }
        if (profiles.isEmpty()) throw OnvifRequestException("The camera did not report any media profiles.")
        OnvifCameraDetails(device, info, profiles, capabilities.mediaEndpoint, capabilities.ptzEndpoint, capabilities.ptzEndpoint != null, credentials)
    }

    suspend fun getStreamUri(details: OnvifCameraDetails, profile: OnvifProfile): String = withContext(Dispatchers.IO) {
        val endpoint = details.mediaEndpoint ?: details.device.endpoint
        val body = """<trt:GetStreamUri><trt:StreamSetup><tt:Stream>RTP-Unicast</tt:Stream><tt:Transport><tt:Protocol>RTSP</tt:Protocol></tt:Transport></trt:StreamSetup><trt:ProfileToken>${xmlEscape(profile.token)}</trt:ProfileToken></trt:GetStreamUri>"""
        val xml = request(endpoint, ACTION_STREAM_URI, body, details.credentials)
        val uri = OnvifXmlParser.parseStreamUri(xml) ?: throw OnvifRequestException("The camera did not return a usable RTSP stream URI.")
        addCredentials(uri, details.credentials)
    }

    private fun request(endpoint: String, action: String, body: String, credentials: OnvifCredentials): String {
        val url = runCatching { URL(endpoint) }.getOrElse { throw OnvifRequestException("Invalid ONVIF service address.", it) }
        val security = if (credentials.username.isBlank()) "" else usernameToken(credentials)
        val envelope = """<?xml version="1.0" encoding="UTF-8"?><s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:tds="http://www.onvif.org/ver10/device/wsdl" xmlns:trt="http://www.onvif.org/ver10/media/wsdl" xmlns:tt="http://www.onvif.org/ver10/schema" xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd" xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"><s:Header>$security</s:Header><s:Body>$body</s:Body></s:Envelope>"""
        val connection = runCatching { url.openConnection() as HttpURLConnection }
            .getOrElse { throw OnvifRequestException("Could not connect to the ONVIF service.", it) }
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8; action=\"$action\"")
            connection.outputStream.use { it.write(envelope.toByteArray(StandardCharsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                ?: throw OnvifRequestException("ONVIF request failed (HTTP $status).")
            val response = stream.use { readBounded(it, MAX_RESPONSE_BYTES) }
            if (status !in 200..299) {
                val fault = OnvifXmlParser.parseFault(response) ?: "ONVIF request failed (HTTP $status)."
                throw OnvifRequestException(if (status == 401) "Camera authentication failed. Check the local credentials." else fault)
            }
            return response
        } catch (failure: OnvifRequestException) {
            throw failure
        } catch (failure: Exception) {
            throw OnvifRequestException("Could not complete the ONVIF request. Check the camera and credentials.", failure)
        } finally {
            connection.disconnect()
        }
    }

    private fun usernameToken(credentials: OnvifCredentials): String {
        val nonce = ByteArray(16).also(SecureRandom()::nextBytes)
        val created = Instant.now().toString()
        val digestInput = nonce + created.toByteArray(StandardCharsets.UTF_8) + credentials.password.toByteArray(StandardCharsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-1").digest(digestInput)
        return """<wsse:Security s:mustUnderstand="1"><wsse:UsernameToken><wsse:Username>${xmlEscape(credentials.username)}</wsse:Username><wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest">${Base64.encodeToString(digest, Base64.NO_WRAP)}</wsse:Password><wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">${Base64.encodeToString(nonce, Base64.NO_WRAP)}</wsse:Nonce><wsu:Created>$created</wsu:Created></wsse:UsernameToken></wsse:Security>"""
    }

    private fun addCredentials(uri: String, credentials: OnvifCredentials): String {
        if (credentials.username.isBlank()) return uri
        val parsed = runCatching { java.net.URI(uri) }.getOrElse { throw OnvifRequestException("Camera returned an invalid RTSP URI.", it) }
        val authority = buildString {
            append(encodeUserInfo(credentials.username))
            if (credentials.password.isNotEmpty()) append(":${encodeUserInfo(credentials.password)}")
            append('@').append(parsed.host ?: throw OnvifRequestException("Camera returned an invalid RTSP host."))
            if (parsed.port >= 0) append(':').append(parsed.port)
        }
        return java.net.URI(parsed.scheme, authority, parsed.path, parsed.query, parsed.fragment).toASCIIString()
    }

    private fun encodeUserInfo(value: String): String = buildString {
        value.toByteArray(StandardCharsets.UTF_8).forEach { byte ->
            val char = byte.toInt() and 0xff
            if (char in 0x41..0x5a || char in 0x61..0x7a || char in 0x30..0x39 || char in intArrayOf(45, 46, 95, 126)) append(char.toChar())
            else append("%%%02X".format(char))
        }
    }

    private fun xmlEscape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

    private fun readBounded(input: java.io.InputStream, max: Int): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > max) throw OnvifRequestException("ONVIF response exceeded the size limit.")
            output.write(buffer, 0, read)
        }
        return output.toString("UTF-8")
    }

    private companion object {
        const val TIMEOUT_MS = 5_000
        const val MAX_RESPONSE_BYTES = 1_000_000
        const val ACTION_DEVICE_INFO = "http://www.onvif.org/ver10/device/wsdl/GetDeviceInformation"
        const val ACTION_CAPABILITIES = "http://www.onvif.org/ver10/device/wsdl/GetCapabilities"
        const val ACTION_PROFILES = "http://www.onvif.org/ver10/media/wsdl/GetProfiles"
        const val ACTION_STREAM_URI = "http://www.onvif.org/ver10/media/wsdl/GetStreamUri"
    }
}
