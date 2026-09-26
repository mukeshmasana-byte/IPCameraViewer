package com.example.ipcameraviewer.util

import java.net.URI

object RtspUrlValidator {
    fun normalize(value: String): String? = runCatching {
        val uri = URI(value.trim())
        if (!uri.scheme.equals("rtsp", ignoreCase = true) || uri.host.isNullOrBlank()) return null
        if (uri.port !in -1..65535 || uri.port == 0) return null
        uri.normalize().toASCIIString()
    }.getOrNull()
}

object HttpUrlValidator {
    fun normalize(value: String): String? = runCatching {
        val uri = URI(value.trim())
        if (!uri.scheme.equals("http", true) && !uri.scheme.equals("https", true)) return null
        if (uri.host.isNullOrBlank() || uri.port !in -1..65535 || uri.port == 0) return null
        uri.normalize().toASCIIString()
    }.getOrNull()
}

object RtspCredentials {
    fun withoutUserInfo(url: String): String {
        val parsed = runCatching { URI(url) }.getOrNull() ?: return url
        val authority = parsed.rawAuthority?.substringAfterLast('@') ?: return url
        return "${parsed.scheme}://$authority${parsed.rawPath.orEmpty()}${parsed.rawQuery?.let { "?$it" }.orEmpty()}${parsed.rawFragment?.let { "#$it" }.orEmpty()}"
    }

    fun playbackUrl(url: String, username: String?, password: String?): String {
        if (username.isNullOrBlank()) return url
        val parsed = runCatching { URI(url) }.getOrNull() ?: return url
        if (!parsed.scheme.equals("rtsp", true) || parsed.rawUserInfo != null) return url
        val rawAuthority = parsed.rawAuthority ?: return url
        val safeUserInfo = encode(username) + if (password.isNullOrEmpty()) "" else ":${encode(password)}"
        val suffix = buildString {
            append(parsed.rawPath.orEmpty())
            parsed.rawQuery?.let { append('?').append(it) }
            parsed.rawFragment?.let { append('#').append(it) }
        }
        return "${parsed.scheme}://$safeUserInfo@$rawAuthority$suffix"
    }

    private fun encode(value: String): String = buildString {
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            val char = byte.toInt() and 0xff
            if (char in 0x41..0x5a || char in 0x61..0x7a || char in 0x30..0x39 || char in intArrayOf(45, 46, 95, 126)) append(char.toChar())
            else append("%%%02X".format(char))
        }
    }
}
