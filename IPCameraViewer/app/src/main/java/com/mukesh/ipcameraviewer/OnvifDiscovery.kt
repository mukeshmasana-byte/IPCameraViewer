package com.mukesh.ipcameraviewer

import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import java.util.UUID

object OnvifDiscovery {
    data class Device(val address: String, val xAddrs: String)

    fun discover(timeoutMs: Int = 2500): List<Device> {
        val message = """
            <?xml version="1.0" encoding="UTF-8"?>
            <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:a="http://www.w3.org/2005/08/addressing" xmlns:d="http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01" xmlns:dn="http://www.onvif.org/ver10/network/wsdl">
              <s:Header><a:Action>http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01/Probe</a:Action><a:MessageID>uuid:${UUID.randomUUID()}</a:MessageID><a:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</a:To></s:Header>
              <s:Body><d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types></d:Probe></s:Body>
            </s:Envelope>
        """.trimIndent().toByteArray()

        val found = linkedMapOf<String, Device>()
        val group = InetAddress.getByName("239.255.255.250")
        MulticastSocket(0).use { socket ->
            socket.soTimeout = timeoutMs
            socket.send(DatagramPacket(message, message.size, group, 3702))
            val buffer = ByteArray(16 * 1024)
            while (true) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val text = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                    val x = Regex("(?i)<[^>]*XAddrs[^>]*>(.*?)</[^>]*XAddrs>", RegexOption.DOT_MATCHES_ALL).find(text)?.groupValues?.getOrNull(1)?.trim()?.split(Regex("\\s+"))?.firstOrNull().orEmpty()
                    val key = packet.address.hostAddress ?: x
                    if (!key.isNullOrBlank()) found[key] = Device(key, x)
                } catch (_: SocketTimeoutException) { break }
            }
        }
        return found.values.toList()
    }
}
