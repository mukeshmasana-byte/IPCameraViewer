package com.example.ipcameraviewer.player

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.ipcameraviewer.model.Camera
import com.example.ipcameraviewer.util.RtspCredentials
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class MjpegPlaybackState(
    val frame: Bitmap? = null,
    val connecting: Boolean = true,
    val connected: Boolean = false,
    val error: String? = null,
)

interface VideoFrameSource {
    val state: StateFlow<MjpegPlaybackState>
    fun start()
    fun stop()
    fun release()
}

class MjpegFrameSource(private val camera: Camera) : VideoFrameSource {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(MjpegPlaybackState())
    override val state = mutableState.asStateFlow()
    @Volatile private var connection: HttpURLConnection? = null
    private var job: Job? = null

    @Synchronized
    override fun start() {
        if (job?.isActive == true) return
        job = scope.launch { readStream() }
    }

    @Synchronized
    override fun stop() {
        connection?.disconnect()
        connection = null
        job?.cancel()
        job = null
        mutableState.value = MjpegPlaybackState()
    }

    override fun release() {
        stop()
        scope.cancel()
    }

    private suspend fun readStream() {
        val url = camera.httpUrl ?: camera.streamUrl
        val cleanUrl = RtspCredentials.withoutUserInfo(url)
        var opened: HttpURLConnection? = null
        try {
            val http = URL(cleanUrl).openConnection() as HttpURLConnection
            opened = http
            connection = http
            http.connectTimeout = TIMEOUT_MS
            http.readTimeout = TIMEOUT_MS
            http.instanceFollowRedirects = true
            if (!camera.username.isNullOrBlank()) {
                val raw = "${camera.username}:${camera.password.orEmpty()}".toByteArray(StandardCharsets.UTF_8)
                http.setRequestProperty("Authorization", "Basic ${Base64.encodeToString(raw, Base64.NO_WRAP)}")
            }
            http.connect()
            val status = http.responseCode
            if (status !in 200..299) {
                val reason = when (status) {
                    401, 403 -> "MJPEG authentication failed. Check the camera credentials."
                    404 -> "MJPEG stream was not found at this address."
                    else -> "MJPEG request failed (HTTP $status)."
                }
                throw MjpegException(reason)
            }
            val mimeType = http.contentType.orEmpty()
            if (!mimeType.contains("multipart", true) && !mimeType.contains("jpeg", true)) {
                throw MjpegException("The HTTP endpoint did not return an MJPEG/JPEG stream.")
            }
            mutableState.value = MjpegPlaybackState(connecting = false, connected = true)
            BufferedInputStream(http.inputStream).use { input ->
                while (currentCoroutineContext().isActive) {
                    val bytes = MjpegFrameReader.nextFrame(input) ?: throw EOFException("The MJPEG stream ended.")
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: continue
                    mutableState.value = MjpegPlaybackState(frame = bitmap, connecting = false, connected = true)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            mutableState.value = MjpegPlaybackState(error = (failure as? MjpegException)?.message ?: "MJPEG stream unavailable. Check the camera and local network.")
        } finally {
            opened?.disconnect()
            if (connection === opened) connection = null
        }
    }

    private class MjpegException(message: String) : Exception(message)

    private companion object { const val TIMEOUT_MS = 5_000 }
}

/** Reads multipart JPEG frames by their SOI/EOI markers, independent of packet and boundary chunking. */
object MjpegFrameReader {
    fun nextFrame(input: java.io.InputStream, maxFrameBytes: Int = MAX_FRAME_BYTES): ByteArray? {
        var previous = -1
        while (true) {
            val value = input.read()
            if (value < 0) return null
            if (previous == 0xff && value == 0xd8) break
            previous = value
        }
        val frame = ByteArrayOutputStream()
        frame.write(0xff)
        frame.write(0xd8)
        previous = 0xd8
        while (true) {
            val value = input.read()
            if (value < 0) return null
            frame.write(value)
            if (frame.size() > maxFrameBytes) throw IllegalArgumentException("MJPEG frame exceeded the size limit.")
            if (previous == 0xff && value == 0xd9) return frame.toByteArray()
            previous = value
        }
    }

    private const val MAX_FRAME_BYTES = 12 * 1024 * 1024
}
