package com.sanket.tools.nexpad.nxprc.engine.parsers

import java.util.regex.Pattern
import kotlin.math.roundToInt

/**
 * Dedicated parser for CSS colors:
 * Supports #rgb, #rgba, #rrggbb, #rrggbbaa, rgb(), rgba(), hsl(), hsla(), and standard named colors.
 */
object ColorParser {

    val NAMED_COLORS = mapOf(
        "transparent" to 0x00000000L,
        "black" to 0xFF000000L,
        "white" to 0xFFFFFFFFL,
        "red" to 0xFFFF0000L,
        "green" to 0xFF008000L,
        "blue" to 0xFF0000FFL,
        "cyan" to 0xFF00FFFFL,
        "magenta" to 0xFFFF00FFL,
        "yellow" to 0xFFFFFF00L,
        "gray" to 0xFF808080L,
        "grey" to 0xFF808080L,
        "silver" to 0xFFC0C0C0L,
        "gold" to 0xFFFFD700L,
        "orange" to 0xFFFFA500L,
        "purple" to 0xFF800080L,
        "lime" to 0xFF00FF00L
    )

    fun parse(str: String?): Long? {
        if (str == null) return null
        val clean = str.trim().lowercase()
        if (clean.isBlank()) return null

        NAMED_COLORS[clean]?.let { return it }

        // Hex #RGB, #RGBA, #RRGGBB, #RRGGBBAA
        if (clean.startsWith("#")) {
            val hex = clean.removePrefix("#")
            return when (hex.length) {
                3 -> {
                    val full = "${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
                    ("FF$full").toLong(16)
                }
                4 -> {
                    val full = "${hex[3]}${hex[3]}${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
                    full.toLong(16)
                }
                6 -> ("FF$hex").toLong(16)
                8 -> hex.toLong(16)
                else -> null
            }
        }

        // rgba(r, g, b, a) / rgb(r, g, b)
        val rgbaPattern = Pattern.compile("rgba?\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*(?:,\\s*([0-9.]+)\\s*)?\\)")
        val rgbaMatcher = rgbaPattern.matcher(clean)
        if (rgbaMatcher.find()) {
            val r = (rgbaMatcher.group(1).toIntOrNull() ?: 0).coerceIn(0, 255)
            val g = (rgbaMatcher.group(2).toIntOrNull() ?: 0).coerceIn(0, 255)
            val b = (rgbaMatcher.group(3).toIntOrNull() ?: 0).coerceIn(0, 255)
            val aFloat = rgbaMatcher.group(4)?.toFloatOrNull() ?: 1.0f
            val aInt = (aFloat * 255f).roundToInt().coerceIn(0, 255)
            return (aInt.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
        }

        // hsl(h, s%, l%) / hsla(...)
        val hslaPattern = Pattern.compile("hsla?\\s*\\(\\s*([0-9.]+)\\s*,\\s*([0-9.]+)%\\s*,\\s*([0-9.]+)%\\s*(?:,\\s*([0-9.]+)\\s*)?\\)")
        val hslaMatcher = hslaPattern.matcher(clean)
        if (hslaMatcher.find()) {
            val h = (hslaMatcher.group(1).toFloatOrNull() ?: 0f) % 360f
            val s = ((hslaMatcher.group(2).toFloatOrNull() ?: 0f) / 100f).coerceIn(0f, 1f)
            val l = ((hslaMatcher.group(3).toFloatOrNull() ?: 0f) / 100f).coerceIn(0f, 1f)
            val aFloat = hslaMatcher.group(4)?.toFloatOrNull() ?: 1.0f
            val aInt = (aFloat * 255f).roundToInt().coerceIn(0, 255)

            val rgb = hslToRgb(h, s, l)
            return (aInt.toLong() shl 24) or (rgb[0].toLong() shl 16) or (rgb[1].toLong() shl 8) or rgb[2].toLong()
        }

        return null
    }

    fun extractColorAnywhere(text: String): Long? {
        val parenColor = Pattern.compile("(?:rgba?|hsla?)\\([^)]+\\)").matcher(text)
        if (parenColor.find()) {
            parse(parenColor.group(0))?.let { return it }
        }
        val hexMatcher = Pattern.compile("#([0-9a-fA-F]{3,8})\\b").matcher(text)
        if (hexMatcher.find()) {
            parse(hexMatcher.group(0))?.let { return it }
        }
        text.split(Regex("[\\s,]+")).forEach { word ->
            parse(word)?.let { return it }
        }
        return null
    }

    fun isDark(color: Long): Boolean {
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
