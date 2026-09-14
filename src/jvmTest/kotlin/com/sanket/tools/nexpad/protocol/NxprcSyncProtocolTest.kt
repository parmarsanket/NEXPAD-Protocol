package com.sanket.tools.nexpad.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NxprcSyncProtocolTest {

    @Test
    fun testFileSyncHeaderRoundTrip() {
        val componentId = "rc.neon_cyber_button"
        val payload = "TEST_NXPRC_BINARY_PAYLOAD_BYTES".encodeToByteArray()
        val checksum = NexpadProtocol.computeCrc32(payload)

        val headerBytes = NexpadProtocol.encodeFileSyncHeader(
            componentId = componentId,
            payloadSize = payload.size,
            checksum = checksum
        )

        val decoded = NexpadProtocol.decodeFileSyncHeader(headerBytes)
        assertNotNull(decoded, "Header must decode successfully")
        assertEquals(componentId, decoded.componentId)
        assertEquals(payload.size, decoded.fileSize)
        assertEquals(checksum, decoded.checksum)
        assertEquals(headerBytes.size, decoded.headerSize)
    }

    @Test
    fun testCrc32IntegrityVerification() {
        val payload = "Hello Nexpad Nxprc Sync!".encodeToByteArray()
        val crc = NexpadProtocol.computeCrc32(payload)
        assertTrue(crc != 0, "CRC32 should be non-zero")

        // Corrupted payload should mismatch
        val corrupted = payload.copyOf()
        corrupted[0] = (corrupted[0] + 1).toByte()
        val corruptedCrc = NexpadProtocol.computeCrc32(corrupted)
        assertTrue(crc != corruptedCrc, "Corrupted payload must yield different CRC32")
    }

    @Test
    fun testDecodeRejectsMalformedHeader() {
        // Too short
        val tooShort = ByteArray(5)
        assertNull(NexpadProtocol.decodeFileSyncHeader(tooShort))

        // Wrong magic
        val wrongMagic = ByteArray(20)
        wrongMagic[0] = 0x12.toByte()
        assertNull(NexpadProtocol.decodeFileSyncHeader(wrongMagic))
    }
}
