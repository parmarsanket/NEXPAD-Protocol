package com.sanket.tools.nexpad.nxprc.engine.parsers

import com.sanket.tools.nexpad.nxprc.StrokeStyle
import java.util.regex.Pattern

data class CornerRadii(
    val topLeft: Float = 0f,
    val topRight: Float = 0f,
    val bottomRight: Float = 0f,
    val bottomLeft: Float = 0f
)

/**
 * Dedicated parser for CSS geometry, borders, and dimensions:
 * - Border style and widths
 * - 4-corner border radius and 50% circle detection
 * - Positional offsets (left, right, top, bottom)
 * - Dimensions in px or %
 */
object GeometryParser {

    fun parseBorder(borderStr: String?): StrokeStyle? {
        if (borderStr.isNullOrBlank() || borderStr.trim().equals("none", ignoreCase = true)) return null
        val color = ColorParser.extractColorAnywhere(borderStr) ?: 0xFF00F0FFL

        var width = 2.0f
        val wMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)px").matcher(borderStr)
        if (wMatcher.find()) {
            width = wMatcher.group(1).toFloatOrNull() ?: 2.0f
        }
        return StrokeStyle(color = color, width = width)
    }

    fun parseBorderRadius(radiusStr: String?, defaultSizeDp: Float = 76f): CornerRadii {
        if (radiusStr.isNullOrBlank()) return CornerRadii()
        val clean = radiusStr.trim()

        if (clean == "50%" || clean.contains("50%")) {
            val half = defaultSizeDp / 2f
            return CornerRadii(half, half, half, half)
        }

        val values = mutableListOf<Float>()
        val numMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)(?:px|%)?").matcher(clean)
        while (numMatcher.find()) {
            val num = numMatcher.group(1).toFloatOrNull() ?: 0f
            val isPct = clean.substring(numMatcher.start(), numMatcher.end()).contains("%")
            val pxVal = if (isPct) (num / 100f) * defaultSizeDp else num
            values.add(pxVal)
        }

        return when (values.size) {
            1 -> CornerRadii(values[0], values[0], values[0], values[0])
            2 -> CornerRadii(values[0], values[1], values[0], values[1])
            3 -> CornerRadii(values[0], values[1], values[2], values[1])
            4 -> CornerRadii(values[0], values[1], values[2], values[3])
            else -> CornerRadii()
        }
    }

    fun parsePixelOrPercent(valStr: String?, parentDim: Float, fallback: Float): Float {
        if (valStr.isNullOrBlank()) return fallback
        val clean = valStr.trim().lowercase()
        if (clean.endsWith("%")) {
            val pct = clean.removeSuffix("%").toFloatOrNull() ?: return fallback
            return (pct / 100f) * parentDim
        }
        val m = Pattern.compile("(-?\\d+(?:\\.\\d+)?)(?:px)?").matcher(clean)
        return if (m.find()) m.group(1).toFloatOrNull() ?: fallback else fallback
    }

    fun parsePositionalOffset(posStart: String?, posEnd: String?, elemDim: Float, parentDim: Float, fallback: Float): Float {
        if (!posStart.isNullOrBlank()) {
            return parsePixelOrPercent(posStart, parentDim, fallback)
        }
        if (!posEnd.isNullOrBlank()) {
            val endVal = parsePixelOrPercent(posEnd, parentDim, fallback)
            return (parentDim - endVal - elemDim).coerceAtLeast(0f)
        }
        return fallback
    }

    fun parseFontSize(str: String?): Float? {
        if (str == null) return null
        val m = Pattern.compile("(\\d+(?:\\.\\d+)?)px").matcher(str)
        return if (m.find()) m.group(1).toFloatOrNull() else null
    }

    fun parseFontWeight(str: String?): Int {
        if (str == null) return 700
        return when (str.trim().lowercase()) {
            "100" -> 100
            "200" -> 200
            "300", "light" -> 300
            "400", "normal" -> 400
            "500", "medium" -> 500
            "600", "semibold" -> 600
            "700", "bold" -> 700
            "800", "extrabold" -> 800
            "900", "black" -> 900
            else -> 700
        }
    }
}
