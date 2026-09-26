package com.example.ipcameraviewer.player

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MjpegFrameReaderTest {
    @Test fun findsFramesAcrossMultipartHeadersAndReadsSequentialFrames() {
        val first = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1, 2, 0xff.toByte(), 0xd9.toByte())
        val second = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 3, 0xff.toByte(), 0xd9.toByte())
        val stream = "--boundary\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray() + first +
            "\r\n--boundary\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray() + second
        val input = ByteArrayInputStream(stream)
        assertArrayEquals(first, MjpegFrameReader.nextFrame(input))
        assertArrayEquals(second, MjpegFrameReader.nextFrame(input))
        assertNull(MjpegFrameReader.nextFrame(input))
    }

    @Test fun returnsNullForTruncatedFrameAndRejectsOversizedFrame() {
        assertNull(MjpegFrameReader.nextFrame(ByteArrayInputStream(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1))))
        val bytes = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1, 2, 0xff.toByte(), 0xd9.toByte())
        assertThrows(IllegalArgumentException::class.java) {
            MjpegFrameReader.nextFrame(ByteArrayInputStream(bytes), maxFrameBytes = 4)
        }
    }
}
