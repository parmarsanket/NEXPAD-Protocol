package com.sanket.tools.nexpad.nxprc

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Binary transport for NXPRC. Kept separate from the document model. */
internal object NxprcBinaryCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
        encodeDefaults = false
    }

    fun encode(doc: NxprcDocument): ByteArray {
        val jsonBytes = json.encodeToString(doc).encodeToByteArray()
        require(jsonBytes.size <= Int.MAX_VALUE - 10) { "NXPRC payload is too large" }
        return ByteBuffer.allocate(10 + jsonBytes.size)
            .order(ByteOrder.BIG_ENDIAN)
            .put(NxprcDocument.MAGIC)
            .putShort(doc.version.toShort())
            .putInt(jsonBytes.size)
            .put(jsonBytes)
            .array()
    }

    fun decode(bytes: ByteArray): Result<NxprcDocument> = runCatching {
        require(bytes.size >= HEADER_SIZE) {
            "File too small to be a valid .nxprc bundle"
        }

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val magic = ByteArray(NxprcDocument.MAGIC.size)
        buffer.get(magic)
        require(magic.contentEquals(NxprcDocument.MAGIC)) {
            "Invalid magic header. Expected NXRC."
        }

        val headerVersion = buffer.short.toInt()
        require(headerVersion > 0) { "Invalid NXPRC version: $headerVersion" }

        val jsonLength = buffer.int
        require(jsonLength >= 0 && jsonLength <= bytes.size - HEADER_SIZE) {
            "Corrupt .nxprc payload (invalid length $jsonLength)."
        }

        val jsonBytes = ByteArray(jsonLength)
        buffer.get(jsonBytes)
        json.decodeFromString<NxprcDocument>(sanitizeLegacyPayload(jsonBytes.decodeToString()))
    }

    private fun sanitizeLegacyPayload(payload: String): String = payload
        .replace("com.sanket.tools.nexpaddesktop.plugins.CanvasLayer.", "")
        .replace("com.sanket.tools.nexpad.runtime.plugin.CanvasLayer.", "")
        .replace("com.sanket.tools.nexpad.nxprc.CanvasLayer.", "")
        .replace("com.sanket.tools.nexpaddesktop.plugins.FillBrush.", "")
        .replace("com.sanket.tools.nexpad.runtime.plugin.FillBrush.", "")
        .replace("com.sanket.tools.nexpad.nxprc.FillBrush.", "")

    private const val HEADER_SIZE = 10
}
