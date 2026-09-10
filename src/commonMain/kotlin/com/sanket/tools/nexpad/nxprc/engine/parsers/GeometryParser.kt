package com.sanket.tools.nexpad.nxprc.engine.parsers

import com.sanket.tools.nexpad.nxprc.StrokeStyle
import java.util.regex.Pattern

data class CornerRadii(
    val topLeft: Float = 0f,
    val topRight: Float = 0f,
    val bottomRight: Float = 0f,
    val bottomLeft: Float = 0f
)

data class InsetRect(
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f
)

data class ComputedBoxBounds(
    val left: Float = 0f,
    val top: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)

/**
 * Dedicated parser for CSS geometry, borders, and dimensions:
 * - Border style and widths
 * - 4-corner border radius and 50% circle detection
 * - Positional offsets (left, right, top, bottom, inset)
 * - Dimensions in px or %
 */
object GeometryParser {

    fun parseInset(insetStr: String?, parentWidth: Float, parentHeight: Float): InsetRect? {
        if (insetStr.isNullOrBlank()) return null
        val clean = insetStr.trim()
        val values = mutableListOf<Float>()
        val matcher = Pattern.compile("(-?\\d+(?:\\.\\d+)?)(?:px|%)?").matcher(clean)
        while (matcher.find()) {
            val num = matcher.group(1).toFloatOrNull() ?: 0f
            val token = clean.substring(matcher.start(), matcher.end())
            val isPct = token.contains("%")
            val isVert = values.size % 2 == 0 // 0 = top, 1 = right, 2 = bottom, 3 = left
            val parentDim = if (isVert) parentHeight else parentWidth
            val pxVal = if (isPct) (num / 100f) * parentDim else num
            values.add(pxVal)
        }
        return when (values.size) {
            1 -> InsetRect(values[0], values[0], values[0], values[0])
            2 -> InsetRect(values[0], values[1], values[0], values[1])
            3 -> InsetRect(values[0], values[1], values[2], values[1])
            4 -> InsetRect(values[0], values[1], values[2], values[3])
            else -> null
        }
    }

    fun computeBoxBounds(style: Map<String, String>, parentW: Float, parentH: Float): ComputedBoxBounds {
        val inset = parseInset(style["inset"], parentW, parentH)
        if (inset != null) {
            val w = (parentW - inset.left - inset.right).coerceAtLeast(0f)
            val h = (parentH - inset.top - inset.bottom).coerceAtLeast(0f)
            return ComputedBoxBounds(left = inset.left, top = inset.top, width = w, height = h)
        }

        val width = when {
            style["width"] != null -> parsePixelOrPercent(style["width"], parentW, parentW)
            style["left"] != null && style["right"] != null -> {
                val l = parsePixelOrPercent(style["left"], parentW, 0f)
                val r = parsePixelOrPercent(style["right"], parentW, 0f)
                (parentW - l - r).coerceAtLeast(0f)
            }
            else -> parentW
        }

        val height = when {
            style["height"] != null -> parsePixelOrPercent(style["height"], parentH, parentH)
            style["top"] != null && style["bottom"] != null -> {
                val t = parsePixelOrPercent(style["top"], parentH, 0f)
                val b = parsePixelOrPercent(style["bottom"], parentH, 0f)
                (parentH - t - b).coerceAtLeast(0f)
            }
            else -> parentH
        }

        val left = when {
            style["left"] != null -> parsePixelOrPercent(style["left"], parentW, 0f)
            style["right"] != null -> parentW - parsePixelOrPercent(style["right"], parentW, 0f) - width
            else -> 0f
        }

        val top = when {
            style["top"] != null -> parsePixelOrPercent(style["top"], parentH, 0f)
            style["bottom"] != null -> parentH - parsePixelOrPercent(style["bottom"], parentH, 0f) - height
            else -> 0f
        }

        return ComputedBoxBounds(left = left, top = top, width = width, height = height)
    }

    fun parseBorder(borderStr: String?): StrokeStyle? {
        if (borderStr.isNullOrBlank() || borderStr.trim().equals("none", ignoreCase = true)) return null
        val color = ColorParser.extractColorAnywhere(borderStr) ?: 0xFF00F0FFL

        var width = 2.0f
        val wMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)(?:px)?").matcher(borderStr)
        if (wMatcher.find()) {
            width = wMatcher.group(1).toFloatOrNull() ?: 2.0f
        }
        if (width == 0f) return null
        val isDashed = borderStr.contains("dashed", ignoreCase = true)
        return StrokeStyle(color = color, width = width, isDashed = isDashed)
    }

    fun parseBorderRadius(radiusStr: String?, defaultSizeDp: Float = 76f): CornerRadii {
        if (radiusStr.isNullOrBlank()) return CornerRadii()
        val clean = radiusStr.trim()

        if (clean == "50%" || clean.contains("50%")) {
            val half = defaultSizeDp / 2f
            return CornerRadii(half, half, half, half)
        }

        val partBeforeSlash = clean.split("/").first().trim()
        val values = mutableListOf<Float>()
        val numMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)(?:px|%)?").matcher(partBeforeSlash)
        while (numMatcher.find()) {
            val num = numMatcher.group(1).toFloatOrNull() ?: 0f
            val isPct = partBeforeSlash.substring(numMatcher.start(), numMatcher.end()).contains("%")
            val pxVal = if (isPct) (num / 100f) * defaultSizeDp else num
            values.add(pxVal)
        }

        return when {
            values.size == 1 -> CornerRadii(values[0], values[0], values[0], values[0])
            values.size == 2 -> CornerRadii(values[0], values[1], values[0], values[1])
            values.size == 3 -> CornerRadii(values[0], values[1], values[2], values[1])
            values.size >= 4 -> CornerRadii(values[0], values[1], values[2], values[3])
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
            return parentDim - endVal - elemDim
        }
        return fallback
    }

    fun parseZIndex(style: Map<String, String>?): Int {
        if (style == null) return 0
        val z = style["z-index"]?.trim() ?: return 0
        if (z.equals("auto", ignoreCase = true) || z.isBlank()) return 0
        return z.toIntOrNull() ?: 0
    }

    data class ParsedClipShape(
        val shapeType: String, // POLYGON, PATH, OVAL
        val polygonSides: Int = 0,
        val pathData: String = "",
        val normalizedVertices: List<Pair<Float, Float>> = emptyList()
    )

    fun parseClipPath(clipPathStr: String?, width: Float = 100f, height: Float = 100f): ParsedClipShape? {
        if (clipPathStr.isNullOrBlank()) return null
        val clean = clipPathStr.trim()

        // 1. polygon(...)
        if (clean.contains("polygon")) {
            val openParen = clean.indexOf('(')
            val closeParen = clean.lastIndexOf(')')
            if (openParen != -1 && closeParen > openParen) {
                val inner = clean.substring(openParen + 1, closeParen).trim()
                // Vertex pairs separated by commas
                val pairs = inner.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val vertices = mutableListOf<Pair<Float, Float>>()

                for (pair in pairs) {
                    val tokens = pair.split(Pattern.compile("\\s+")).filter { it.isNotEmpty() }
                    if (tokens.size >= 2) {
                        val xRatio = parseCoordRatio(tokens[0], width)
                        val yRatio = parseCoordRatio(tokens[1], height)
                        vertices.add(Pair(xRatio, yRatio))
                    }
                }

                if (vertices.size >= 3) {
                    // Build normalized 0..100 viewBox SVG path
                    val sb = StringBuilder()
                    vertices.forEachIndexed { i, pt ->
                        val cmd = if (i == 0) "M" else "L"
                        val x = pt.first * 100f
                        val y = pt.second * 100f
                        sb.append("$cmd $x $y ")
                    }
                    sb.append("Z")

                    val detectedShape = when (vertices.size) {
                        6 -> "HEXAGON"
                        8 -> "OCTAGON"
                        else -> "POLYGON"
                    }
                    return ParsedClipShape(
                        shapeType = detectedShape,
                        polygonSides = vertices.size,
                        pathData = sb.toString().trim(),
                        normalizedVertices = vertices
                    )
                }
            }
        }

        // 2. path("...") or path('...')
        if (clean.contains("path(")) {
            val openParen = clean.indexOf('(')
            val closeParen = clean.lastIndexOf(')')
            if (openParen != -1 && closeParen > openParen) {
                val raw = clean.substring(openParen + 1, closeParen).trim()
                val unquoted = raw.trim('\'', '"')
                if (unquoted.isNotBlank()) {
                    return ParsedClipShape(
                        shapeType = "PATH",
                        pathData = unquoted
                    )
                }
            }
        }

        // 3. circle(...) / ellipse(...)
        if (clean.contains("circle(") || clean.contains("ellipse(")) {
            return ParsedClipShape(
                shapeType = "OVAL"
            )
        }

        return null
    }

    private fun parseCoordRatio(token: String, parentDim: Float): Float {
        val clean = token.trim().lowercase()
        return when {
            clean.endsWith("%") -> {
                val pct = clean.removeSuffix("%").toFloatOrNull() ?: 0f
                (pct / 100f).coerceIn(0f, 1f)
            }
            clean.endsWith("px") -> {
                val px = clean.removeSuffix("px").toFloatOrNull() ?: 0f
                (px / parentDim.coerceAtLeast(1f)).coerceIn(0f, 1f)
            }
            else -> {
                val v = clean.toFloatOrNull() ?: 0f
                (v / parentDim.coerceAtLeast(1f)).coerceIn(0f, 1f)
            }
        }
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
