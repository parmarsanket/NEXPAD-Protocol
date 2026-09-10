package com.sanket.tools.nexpad.nxprc

import com.sanket.tools.nexpad.nxprc.engine.compiler.NxprcCompiler

/**
 * Single-Entry Multiplatform Facade for .nxprc Packages.
 *
 * Provides a unified API for NEXPAD Desktop, NEXPAD Android, and external tools:
 * - Turns arbitrary HTML/CSS/SVG into ready-to-use .nxprc binary packages.
 * - Decodes and validates .nxprc packages with backward compatibility.
 * - Allows Android and Desktop to stay 100% untouched while web engine parsing evolves.
 */
object NxprcPackager {

    /**
     * Compiles raw HTML/CSS/SVG text into an NxprcDocument.
     */
    fun compile(
        html: String,
        id: String = "rc.custom",
        name: String = "Custom Button",
        category: String = "BUTTON",
        defaultControl: String = "A"
    ): NxprcDocument {
        NxprcInputValidator.validateHtml(html)
        NxprcInputValidator.validateMetadata(id, name)
        return NxprcCompiler.compile(
            html = html,
            id = id,
            name = name,
            category = category,
            defaultControl = defaultControl
        )
    }

    /**
     * Compiles raw HTML/CSS/SVG text directly into binary .nxprc package bytes (magic NXRC + JSON).
     */
    fun pack(
        html: String,
        id: String = "rc.custom",
        name: String = "Custom Button",
        category: String = "BUTTON",
        defaultControl: String = "A"
    ): ByteArray {
        val doc = compile(
            html = html,
            id = id,
            name = name,
            category = category,
            defaultControl = defaultControl
        )
        return NxprcDocument.encodeToBytes(doc)
    }

    /**
     * Decodes and validates raw binary bytes into an NxprcDocument.
     */
    fun unpack(bytes: ByteArray): Result<NxprcDocument> {
        return NxprcDocument.decodeFromBytes(bytes)
    }
}
