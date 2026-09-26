package com.example.ipcameraviewer.onvif

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/** Namespace-independent parsing for the incomplete and varied SOAP replies seen in ONVIF devices. */
object OnvifXmlParser {
    fun parseDiscovery(xml: String, hostAddress: String): DiscoveredOnvifDevice? = runCatching {
        val document = parseDocument(xml)
        val endpointReference = document.allByLocalName("Address").firstOrNull()?.textContent?.trim()
        val xaddrs = document.allByLocalName("XAddrs").firstOrNull()?.textContent?.trim()?.split(Regex("\\s+"))
            ?.filter { it.startsWith("http://", true) || it.startsWith("https://", true) }.orEmpty()
        val endpoint = xaddrs.firstOrNull() ?: return null
        val scopes = document.allByLocalName("Scopes").firstOrNull()?.textContent?.trim()?.split(Regex("\\s+"))?.filter(String::isNotBlank).orEmpty()
        DiscoveredOnvifDevice(endpointReference, endpoint, scopes, hostAddress)
    }.getOrNull()

    fun parseDeviceInfo(xml: String): OnvifDeviceInfo {
        val document = parseDocument(xml)
        return OnvifDeviceInfo(
            manufacturer = document.value("Manufacturer"),
            model = document.value("Model"),
            firmware = document.value("FirmwareVersion"),
            serialNumber = document.value("SerialNumber"),
        )
    }

    fun parseCapabilities(xml: String): OnvifCapabilities {
        val document = parseDocument(xml)
        val media = document.allByLocalName("Media").firstOrNull()?.firstDescendant("XAddr")?.textContent?.trim()
        val ptz = document.allByLocalName("PTZ").firstOrNull()?.firstDescendant("XAddr")?.textContent?.trim()
        return OnvifCapabilities(media?.takeIf(String::isNotBlank), ptz?.takeIf(String::isNotBlank))
    }

    fun parseProfiles(xml: String): List<OnvifProfile> {
        val document = parseDocument(xml)
        return document.allByLocalName("Profiles").mapNotNull { profile ->
            val token = profile.getAttribute("token").takeIf(String::isNotBlank) ?: return@mapNotNull null
            val name = profile.firstDescendant("Name")?.textContent?.trim()?.ifBlank { null } ?: token
            val audio = profile.allDescendants().any { it.localName in setOf("AudioEncoderConfiguration", "AudioSourceConfiguration") }
            OnvifProfile(token, name, hasAudio = audio)
        }
    }

    fun parseStreamUri(xml: String): String? = runCatching {
        parseDocument(xml).value("Uri")?.trim()?.takeIf { it.startsWith("rtsp://", true) || it.startsWith("rtsps://", true) }
    }.getOrNull()

    fun parseFault(xml: String): String? = runCatching {
        val document = parseDocument(xml)
        document.value("Text") ?: document.value("faultstring") ?: document.value("Reason")
    }.getOrNull()

    private fun parseDocument(xml: String) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        isXIncludeAware = false
        isExpandEntityReferences = false
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "") }
        runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "") }
    }.newDocumentBuilder().apply {
        setErrorHandler(object : org.xml.sax.helpers.DefaultHandler() {
            override fun error(exception: org.xml.sax.SAXParseException) { throw exception }
            override fun fatalError(exception: org.xml.sax.SAXParseException) { throw exception }
        })
    }.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

    private fun org.w3c.dom.Document.allByLocalName(name: String): List<Element> {
        val nodes = getElementsByTagNameNS("*", name)
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    private fun org.w3c.dom.Document.value(name: String): String? = allByLocalName(name).firstOrNull()?.textContent?.trim()?.ifBlank { null }

    private fun Element.firstDescendant(name: String): Element? = allDescendants().firstOrNull { it.localName == name }

    private fun Element.allDescendants(): List<Element> {
        val result = mutableListOf<Element>()
        fun visit(parent: Node) {
            var child = parent.firstChild
            while (child != null) {
                if (child is Element) {
                    result += child
                    visit(child)
                }
                child = child.nextSibling
            }
        }
        visit(this)
        return result
    }
}
