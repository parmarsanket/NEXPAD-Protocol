package com.sanket.tools.nexpad.nxprc

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Binary transport for NXPRC. Pure KMP-safe (no java.nio).
 *
 * Binary layout (big-endian):
 *   [0..3]  4 bytes  — magic "NXRC" (0x4E 0x58 0x52 0x43)
 *   [4..5]  2 bytes  — unsigned version (1..65535), big-endian
 *   [6..9]  4 bytes  — JSON payload length (signed int, big-endian)
 *   [10..]  N bytes  — UTF-8 JSON payload
 */
internal object NxprcBinaryCodec {

    // encodeDefaults = true: every field is written regardless of default value.
    // This protects against backward-compatibility breakage — if a default value ever
    // changes in a future SDK version, existing .nxprc files will still decode with
    // their original values rather than silently adopting the new default.
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
        encodeDefaults = true
    }

    private const val HEADER_SIZE = 10

    fun encode(doc: NxprcDocument): ByteArray {
        require(doc.version in 1..65535) {
            "NxprcDocument.version must be in 1..65535, got ${doc.version}"
        }
        val jsonBytes = json.encodeToString(doc).encodeToByteArray()
        require(jsonBytes.size <= Int.MAX_VALUE - HEADER_SIZE) { "NXPRC payload is too large" }

        val out = ByteArray(HEADER_SIZE + jsonBytes.size)

        // Magic "NXRC"
        NxprcDocument.MAGIC.copyInto(out, 0)

        // Version (2 bytes, big-endian, treated as unsigned)
        out[4] = (doc.version ushr 8).toByte()
        out[5] = doc.version.toByte()

        // Payload length (4 bytes, big-endian)
        val len = jsonBytes.size
        out[6] = (len ushr 24).toByte()
        out[7] = (len ushr 16).toByte()
        out[8] = (len ushr 8).toByte()
        out[9] = len.toByte()

        // JSON payload
        jsonBytes.copyInto(out, HEADER_SIZE)
        return out
    }

    fun decode(bytes: ByteArray): Result<NxprcDocument> = runCatching {
        require(bytes.size >= HEADER_SIZE) {
            "File too small to be a valid .nxprc bundle (${bytes.size} bytes)"
        }

        // Validate magic header
        val magic = bytes.copyOfRange(0, 4)
        require(magic.contentEquals(NxprcDocument.MAGIC)) {
            "Invalid magic header. Expected NXRC, got: ${magic.map { it.toInt() and 0xFF }}"
        }

        // Read version as unsigned short (big-endian)
        val headerVersion = ((bytes[4].toInt() and 0xFF) shl 8) or (bytes[5].toInt() and 0xFF)
        require(headerVersion > 0) { "Invalid NXPRC version in header: $headerVersion" }

        // Read payload length (big-endian signed int)
        val jsonLength = ((bytes[6].toInt() and 0xFF) shl 24) or
                         ((bytes[7].toInt() and 0xFF) shl 16) or
                         ((bytes[8].toInt() and 0xFF) shl 8)  or
                          (bytes[9].toInt() and 0xFF)
        require(jsonLength >= 0 && jsonLength <= bytes.size - HEADER_SIZE) {
            "Corrupt .nxprc payload (invalid length $jsonLength, file size ${bytes.size})"
        }

        val jsonStr = sanitizeLegacyPayload(bytes.copyOfRange(HEADER_SIZE, HEADER_SIZE + jsonLength).decodeToString())
        val decoded = json.decodeFromString<NxprcDocument>(jsonStr)

        // BUG 3 FIX: Cross-check the header version against the JSON document version.
        // Prevents tampered or future-format files from silently passing validation.
        require(decoded.version == headerVersion) {
            "Header version ($headerVersion) does not match document version (${decoded.version}). File may be corrupt or tampered."
        }

        decoded
    }

    private fun sanitizeLegacyPayload(payload: String): String = payload
        .replace("com.sanket.tools.nexpaddesktop.plugins.CanvasLayer.", "")
        .replace("com.sanket.tools.nexpad.runtime.plugin.CanvasLayer.", "")
        .replace("com.sanket.tools.nexpad.nxprc.CanvasLayer.", "")
        .replace("com.sanket.tools.nexpaddesktop.plugins.FillBrush.", "")
        .replace("com.sanket.tools.nexpad.runtime.plugin.FillBrush.", "")
        .replace("com.sanket.tools.nexpad.nxprc.FillBrush.", "")
}
