package com.example.ipcameraviewer.nvr

import com.example.ipcameraviewer.onvif.OnvifCredentials
import com.example.ipcameraviewer.onvif.OnvifDeviceInfo

data class NvrConnection(val id: String, val host: String, val port: Int, val vendor: String?, val model: String?)

data class NvrChannel(val number: Int, val name: String, val streamUrl: String, val audioSupported: Boolean? = null)

sealed interface NvrChannelResult {
    data class Available(val channels: List<NvrChannel>) : NvrChannelResult
    data class Unsupported(val explanation: String) : NvrChannelResult
}

/** Implemented only by protocol adapters that can identify and query a specific NVR family. */
interface NvrChannelAdapter {
    val id: String
    fun supports(info: OnvifDeviceInfo): Boolean
    suspend fun listChannels(connection: NvrConnection, credentials: OnvifCredentials): List<NvrChannel>
}

interface NvrDiscovery {
    suspend fun listChannels(connection: NvrConnection, credentials: OnvifCredentials): NvrChannelResult
}

/** Vendor-specific probing stays isolated; an unknown model falls back to user-supplied RTSP channels. */
class NvrAdapterRegistry(
    private val adapters: List<NvrChannelAdapter> = emptyList(),
) : NvrDiscovery {
    override suspend fun listChannels(connection: NvrConnection, credentials: OnvifCredentials): NvrChannelResult {
        val adapter = adapters.firstOrNull { it.supports(OnvifDeviceInfo(connection.vendor, connection.model, null, null)) }
            ?: return NvrChannelResult.Unsupported(
                "This NVR does not expose a standard channel list. Add the channels with the RTSP template supplied by its manufacturer.",
            )
        return runCatching { adapter.listChannels(connection, credentials) }
            .fold(
                onSuccess = { NvrChannelResult.Available(it) },
                onFailure = { NvrChannelResult.Unsupported(it.message ?: "Could not retrieve NVR channels.") },
            )
    }
}
