package com.example.ipcameraviewer.onvif

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.UUID

class WsDiscoveryScanner(context: Context) : CameraDiscovery {
    private val appContext = context.applicationContext

    override suspend fun discover(): List<DiscoveredOnvifDevice> = withContext(Dispatchers.IO) {
        val wifi = appContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = wifi?.createMulticastLock("ip-camera-ws-discovery")?.apply { setReferenceCounted(false) }
        val devices = linkedMapOf<String, DiscoveredOnvifDevice>()
        try {
            lock?.acquire()
            DatagramSocket(null).use { socket ->
                socket.reuseAddress = true
                socket.bind(InetSocketAddress(0))
                val message = probeMessage().toByteArray(Charsets.UTF_8)
                socket.send(DatagramPacket(message, message.size, InetAddress.getByName(MULTICAST_ADDRESS), PORT))
                val deadline = System.nanoTime() + SCAN_WINDOW_MS * 1_000_000
                val buffer = ByteArray(12_288)
                while (System.nanoTime() < deadline) {
                    socket.soTimeout = ((deadline - System.nanoTime()) / 1_000_000).toInt().coerceIn(50, 500)
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val xml = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                        OnvifXmlParser.parseDiscovery(xml, packet.address.hostAddress ?: continue)?.let { device ->
                            devices.putIfAbsent(device.endpointReference ?: device.endpoint, device)
                        }
                    } catch (_: SocketTimeoutException) {
                        // Keep listening until the bounded scan window expires.
                    }
                }
            }
        } finally {
            if (lock?.isHeld == true) lock.release()
        }
        devices.values.toList()
    }

    private fun probeMessage(): String {
        val id = "urn:uuid:${UUID.randomUUID()}"
        return """<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:a="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery" xmlns:dn="http://www.onvif.org/ver10/network/wsdl"><s:Header><a:MessageID>$id</a:MessageID><a:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</a:To><a:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</a:Action></s:Header><s:Body><d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types></d:Probe></s:Body></s:Envelope>"""
    }

    private companion object {
        const val MULTICAST_ADDRESS = "239.255.255.250"
        const val PORT = 3702
        const val SCAN_WINDOW_MS = 4_000L
    }
}
