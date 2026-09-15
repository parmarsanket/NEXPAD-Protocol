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
     *
     * @param html       Raw HTML/CSS/SVG source. Must be non-blank and < 2 MB.
     * @param id         Manifest ID (e.g. "rc.cyber_hex_a"). Must be non-blank.
     * @param name       Human-readable button name. Must be non-blank.
     * @param category   One of: BUTTON, DPAD, JOYSTICK, TRIGGER, BUMPER, HOME, SYSTEM, MACRO.
     * @param defaultControl  Control key (A, B, LT, RS, …). Blank = auto-detect from HTML attributes.
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
        NxprcInputValidator.validateCategory(category)
        NxprcInputValidator.validateControl(defaultControl)
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
