package com.example.ipcameraviewer.onvif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnvifXmlParserTest {
    @Test fun parsesNamespacedDiscoveryWithMultipleAddresses() {
        val xml = """<s:Envelope xmlns:s="urn:soap"><s:Body><d:ProbeMatch xmlns:d="urn:discovery"><a:EndpointReference xmlns:a="urn:address"><a:Address>urn:uuid:camera-1</a:Address></a:EndpointReference><d:XAddrs>http://192.168.1.5/onvif/device_service http://camera.local/onvif/device_service</d:XAddrs><d:Scopes>onvif://www.onvif.org/name/front-door onvif://www.onvif.org/hardware/model-x</d:Scopes></d:ProbeMatch></s:Body></s:Envelope>"""
        val device = OnvifXmlParser.parseDiscovery(xml, "192.168.1.5")
        assertNotNull(device)
        assertEquals("urn:uuid:camera-1", device?.endpointReference)
        assertEquals("http://192.168.1.5/onvif/device_service", device?.endpoint)
        assertEquals(2, device?.scopes?.size)
    }

    @Test fun parsesProfilesAndAudioCapabilityWithoutAssumingTalkback() {
        val xml = """<trt:GetProfilesResponse xmlns:trt="urn:media" xmlns:tt="urn:schema"><trt:Profiles token="main"><tt:Name>Main stream</tt:Name><tt:AudioEncoderConfiguration><tt:Name>AAC</tt:Name></tt:AudioEncoderConfiguration></trt:Profiles><trt:Profiles token="sub"><tt:Name>Sub stream</tt:Name></trt:Profiles></trt:GetProfilesResponse>"""
        val profiles = OnvifXmlParser.parseProfiles(xml)
        assertEquals(2, profiles.size)
        assertTrue(profiles.first().hasAudio)
        assertFalse(profiles.last().hasAudio)
    }

    @Test fun parsesOptionalCapabilitiesAndIgnoresMalformedReplies() {
        val xml = """<tds:GetCapabilitiesResponse xmlns:tds="urn:device"><tds:Capabilities><tds:Media><tds:XAddr>http://192.168.1.5/onvif/media</tds:XAddr></tds:Media><tds:PTZ><tds:XAddr>http://192.168.1.5/onvif/ptz</tds:XAddr></tds:PTZ></tds:Capabilities></tds:GetCapabilitiesResponse>"""
        val capabilities = OnvifXmlParser.parseCapabilities(xml)
        assertEquals("http://192.168.1.5/onvif/media", capabilities.mediaEndpoint)
        assertEquals("http://192.168.1.5/onvif/ptz", capabilities.ptzEndpoint)
        assertNull(OnvifXmlParser.parseDiscovery("not xml", "192.168.1.5"))
    }
}
