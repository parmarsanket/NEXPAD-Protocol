package com.sanket.tools.nexpad.nxprc.engine.parsers

import java.util.regex.Pattern
import kotlin.math.roundToInt

/**
 * Dedicated parser for CSS colors:
 * Supports:
 * - #rgb, #rgba, #rrggbb, #rrggbbaa
 * - rgb(r, g, b), rgba(r, g, b, a), rgb(r g b), rgb(r g b / a), rgba(r g b / a)
 * - Channel percentages e.g. rgb(100%, 0%, 50% / 0.5)
 * - hsl(h, s%, l%), hsla(h, s%, l%, a), hsl(h s% l% / a), hsla(h s% l% / a)
 * - Angle units in hue (deg, rad, turn)
 * - 148 standard W3C named colors, transparent, currentColor
 */
object ColorParser {

    val NAMED_COLORS: Map<String, Long> = NamedColor.ALL

    private val PAREN_COLOR_PATTERN = ColorPattern.PARENTHESIZED.pattern
    private val HEX_COLOR_PATTERN = ColorPattern.HEX.pattern
    private val DELIMITER_PATTERN = ColorPattern.DELIMITER.pattern
    private val WORD_SPLIT_PATTERN = ColorPattern.WORD_SPLIT.pattern

    fun parse(str: String?): Long? {
        if (str == null) return null
        val clean = str.trim().lowercase()
        if (clean.isBlank()) return null

        NamedColor.find(clean)?.let { return it }

        // Hex #RGB, #RGBA, #RRGGBB, #RRGGBBAA
        if (clean.startsWith("#")) {
            val hex = clean.removePrefix("#")
            return when (hex.length) {
                3 -> {
                    val full = "${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
                    ("FF$full").toLongOrNull(16)
                }
                4 -> {
                    val full = "${hex[3]}${hex[3]}${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
                    full.toLongOrNull(16)
                }
                6 -> ("FF$hex").toLongOrNull(16)
                8 -> {
                    // CSS #RRGGBBAA -> Compose AARRGGBB
                    val r = hex.substring(0, 2)
                    val g = hex.substring(2, 4)
                    val b = hex.substring(4, 6)
                    val a = hex.substring(6, 8)
                    "$a$r$g$b".toLongOrNull(16)
                }
                else -> null
            }
        }

        // RGB / RGBA (handles commas, spaces, and slash alpha)
        if (clean.startsWith("rgb")) {
            val inner = extractInner(clean, "rgb") ?: return null
            return parseRgbChannels(inner)
        }

        // HSL / HSLA (handles commas, spaces, and slash alpha)
        if (clean.startsWith("hsl")) {
            val inner = extractInner(clean, "hsl") ?: return null
            return parseHslChannels(inner)
        }

        return null
    }

    private fun extractInner(str: String, prefix: String): String? {
        val start = str.indexOf('(')
        val end = str.lastIndexOf(')')
        if (start != -1 && end > start) {
            return str.substring(start + 1, end).trim()
        }
        return null
    }

    private fun parseRgbChannels(inner: String): Long? {
        // Split on slash for alpha if present: "r g b / a"
        val slashParts = inner.split("/")
        val colorPart = slashParts[0].trim()
        val alphaPart = if (slashParts.size > 1) slashParts[1].trim() else null

        // Tokens in color part can be comma or whitespace delimited
        val tokens = colorPart.split(DELIMITER_PATTERN).map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.size < 3) return null

        fun parseChannel(tok: String): Int {
            return if (tok.endsWith("%")) {
                val pct = tok.removeSuffix("%").toFloatOrNull() ?: 0f
                ((pct / 100f) * 255f).roundToInt().coerceIn(0, 255)
            } else {
                tok.toFloatOrNull()?.roundToInt()?.coerceIn(0, 255) ?: 0
            }
        }

        val r = parseChannel(tokens[0])
        val g = parseChannel(tokens[1])
        val b = parseChannel(tokens[2])

        var a = 255
        if (alphaPart != null) {
            a = parseAlpha(alphaPart)
        } else if (tokens.size >= 4) {
            a = parseAlpha(tokens[3])
        }

        return (a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }

    private fun parseHslChannels(inner: String): Long? {
        val slashParts = inner.split("/")
        val colorPart = slashParts[0].trim()
        val alphaPart = if (slashParts.size > 1) slashParts[1].trim() else null

        val tokens = colorPart.split(DELIMITER_PATTERN).map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.size < 3) return null

        // Hue
        val hStr = tokens[0].lowercase()
        val h = (AngleUnit.parseToDegrees(hStr) ?: 0f) % 360f
        val normH = if (h < 0f) h + 360f else h

        // Saturation & Lightness
        val s = (tokens[1].removeSuffix("%").toFloatOrNull() ?: 0f) / 100f
        val l = (tokens[2].removeSuffix("%").toFloatOrNull() ?: 0f) / 100f

        var a = 255
        if (alphaPart != null) {
            a = parseAlpha(alphaPart)
        } else if (tokens.size >= 4) {
            a = parseAlpha(tokens[3])
        }

        val rgb = hslToRgb(normH, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
        return (a.toLong() shl 24) or (rgb[0].toLong() shl 16) or (rgb[1].toLong() shl 8) or rgb[2].toLong()
    }

    private fun parseAlpha(tok: String): Int {
        val clean = tok.trim()
        return if (clean.endsWith("%")) {
            val pct = clean.removeSuffix("%").toFloatOrNull() ?: 100f
            ((pct / 100f) * 255f).roundToInt().coerceIn(0, 255)
        } else {
            val f = clean.toFloatOrNull() ?: 1.0f
            (f * 255f).roundToInt().coerceIn(0, 255)
        }
    }

    fun extractColorAnywhere(text: String): Long? {
        val parenColor = PAREN_COLOR_PATTERN.matcher(text)
        if (parenColor.find()) {
            parse(parenColor.group(0))?.let { return it }
        }
        val hexMatcher = HEX_COLOR_PATTERN.matcher(text)
        if (hexMatcher.find()) {
            parse(hexMatcher.group(0))?.let { return it }
        }
        // Check named colors (match whole words or tokens)
        val tokens = text.lowercase().split(WORD_SPLIT_PATTERN)
        for (tok in tokens) {
            NamedColor.find(tok)?.let { return it }
        }
        return null
    }

    fun isDark(color: Long): Boolean {
        val a = (color shr 24) and 0xFF
        if (a < 10) return false
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val brightness = (r * 299 + g * 587 + b * 114) / 1000
        return brightness < 50
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): IntArray {
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        var r1 = 0f
        var g1 = 0f
        var b1 = 0f
        when {
            h < 60f -> { r1 = c; g1 = x; b1 = 0f }
            h < 120f -> { r1 = x; g1 = c; b1 = 0f }
            h < 180f -> { r1 = 0f; g1 = c; b1 = x }
            h < 240f -> { r1 = 0f; g1 = x; b1 = c }
            h < 300f -> { r1 = x; g1 = 0f; b1 = c }
            else -> { r1 = c; g1 = 0f; b1 = x }
        }
        val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
        return intArrayOf(r, g, b)
    }
}
