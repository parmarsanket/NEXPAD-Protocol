package com.sanket.tools.nexpad.nxprc.engine.parsers

import com.sanket.tools.nexpad.nxprc.FillBrush
import java.util.regex.Pattern

/**
 * Dedicated parser for multi-layer CSS backgrounds and gradients:
 * - Linear gradients with angle, direction keywords (to right, to bottom, etc.), and stops.
 * - Radial gradients with focal centers (circle at X% Y%, ellipse), radius, and stops.
 * - Conic / Sweep gradients with stops.
 * - Comma-separated multi-background stacks (evaluated in CSS stacking order).
 */
object GradientParser {

    fun parseAll(
        bgStr: String?,
        positionStr: String? = null,
        sizeStr: String? = null
    ): List<FillBrush> {
        if (bgStr == null) return listOf(FillBrush.Solid(0xFF0A192FL))
        val clean = bgStr.trim()
        val parts = splitTopLevelCommas(clean)
        if (parts.isEmpty()) return listOf(FillBrush.Solid(0xFF0A192FL))

        val defaultPos = parsePosition(positionStr)
        val defaultSizeRatio = parseSizeRatio(sizeStr)

        val list = parts.mapNotNull { part ->
            val p = part.trim()
            if (p.isBlank()) null else parseSingle(p, defaultPos, defaultSizeRatio)
        }
        return if (list.isNotEmpty()) list else listOf(FillBrush.Solid(0xFF0A192FL))
    }

    fun parseFirst(
        bgStr: String?,
        positionStr: String? = null,
        sizeStr: String? = null
    ): FillBrush {
        return parseAll(bgStr, positionStr, sizeStr).firstOrNull() ?: FillBrush.Solid(0xFF0A192FL)
    }

    private fun parseSingle(
        clean: String,
        defaultPos: Pair<Float, Float>? = null,
        defaultSizeRatio: Float = 1.0f
    ): FillBrush? {
        // Radial Gradient
        if (clean.contains("radial-gradient")) {
            val inner = extractParenthesizedContent(clean, "radial-gradient")
            if (inner != null) {
                var cx = defaultPos?.first ?: 0.5f
                var cy = defaultPos?.second ?: 0.5f
                val atMatcher = Pattern.compile("(?:circle\\s+|ellipse\\s+)?at\\s+(\\d+(?:\\.\\d+)?)%\\s+(\\d+(?:\\.\\d+)?)%").matcher(inner)
                if (atMatcher.find()) {
                    cx = (atMatcher.group(1).toFloatOrNull() ?: 50f) / 100f
                    cy = (atMatcher.group(2).toFloatOrNull() ?: 50f) / 100f
                }

                val (colors, stops) = parseGradientStops(inner)
                if (colors.size >= 2) {
                    return FillBrush.RadialGradient(
                        colors = colors,
                        stops = stops,
                        radiusRatio = 0.55f * defaultSizeRatio,
                        centerXRatio = cx,
                        centerYRatio = cy
                    )
                }
            }
        }

        // Linear Gradient
        if (clean.contains("linear-gradient")) {
            val inner = extractParenthesizedContent(clean, "linear-gradient")
            if (inner != null) {
                var angle = 180f
                val angleMatcher = Pattern.compile("(-?\\d+(?:\\.\\d+)?)deg").matcher(inner)
                if (angleMatcher.find()) {
                    angle = angleMatcher.group(1).toFloatOrNull() ?: 180f
                } else if (inner.contains("to right")) angle = 90f
                else if (inner.contains("to bottom right") || inner.contains("135deg")) angle = 135f
                else if (inner.contains("to top")) angle = 0f
                else if (inner.contains("to left")) angle = 270f

                val (colors, stops) = parseGradientStops(inner)
                if (colors.size >= 2) {
                    return FillBrush.LinearGradient(colors = colors, angleDegrees = angle, stops = stops)
                }
            }
        }

        // Conic / Sweep Gradient
        if (clean.contains("conic-gradient")) {
            val inner = extractParenthesizedContent(clean, "conic-gradient")
            if (inner != null) {
                var cx = 0.5f
                var cy = 0.5f
                val atMatcher = Pattern.compile("at\\s+(\\d+(?:\\.\\d+)?)%\\s+(\\d+(?:\\.\\d+)?)%").matcher(inner)
                if (atMatcher.find()) {
                    cx = (atMatcher.group(1).toFloatOrNull() ?: 50f) / 100f
                    cy = (atMatcher.group(2).toFloatOrNull() ?: 50f) / 100f
                }
                val (colors, stops) = parseGradientStops(inner)
                if (colors.size >= 2) {
                    return FillBrush.SweepGradient(colors = colors, centerXRatio = cx, centerYRatio = cy, stops = stops)
                }
            }
        }

        // Solid Color
        ColorParser.parse(clean)?.let { return FillBrush.Solid(it) }

        return null
    }

    private fun extractParenthesizedContent(text: String, funcName: String): String? {
        val idx = text.indexOf(funcName)
        if (idx == -1) return null
        val openParen = text.indexOf('(', idx)
        if (openParen == -1) return null

        var depth = 1
        for (i in (openParen + 1) until text.length) {
            if (text[i] == '(') depth++
            else if (text[i] == ')') {
                depth--
                if (depth == 0) return text.substring(openParen + 1, i)
            }
        }
        return null
    }

    fun parseGradientStops(inner: String): Pair<List<Long>, List<Float>> {
        val colors = mutableListOf<Long>()
        val stops = mutableListOf<Float>()

        val parts = splitTopLevelCommas(inner)

        parts.forEach { part ->
            val p = part.trim()
            // Skip preamble directives (e.g. "from 180deg at 50% 50%", "to right", "circle at 50% 50%")
            if (p.startsWith("circle") || p.startsWith("ellipse") || p.startsWith("at ") || p.startsWith("to ") || p.startsWith("from ")) {
                return@forEach
            }
            if ((p.contains("at ") || p.contains("from ")) && ColorParser.extractColorAnywhere(p) == null) {
                return@forEach
            }
            val color = ColorParser.extractColorAnywhere(p)
            if (color != null) {
                colors.add(color)
                val pctMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)%").matcher(p)
                val degMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)deg").matcher(p)
                if (pctMatcher.find()) {
                    stops.add((pctMatcher.group(1).toFloatOrNull() ?: 0f) / 100f)
                } else if (degMatcher.find()) {
                    stops.add(((degMatcher.group(1).toFloatOrNull() ?: 0f) / 360f).coerceIn(0f, 1f))
                } else {
                    stops.add(-1f)
                }
            }
        }

        if (colors.isNotEmpty() && (stops.size != colors.size || stops.contains(-1f))) {
            val normalizedStops = colors.indices.map { it.toFloat() / (colors.size - 1).coerceAtLeast(1) }
            return Pair(colors, normalizedStops)
        }

        return Pair(colors, stops)
    }

    fun splitTopLevelCommas(text: String): List<String> {
        val list = mutableListOf<String>()
        var start = 0
        var depth = 0
        for (i in text.indices) {
            val c = text[i]
            if (c == '(') depth++
            else if (c == ')') depth--
            else if (c == ',' && depth == 0) {
                list.add(text.substring(start, i).trim())
                start = i + 1
            }
        }
        if (start < text.length) {
            list.add(text.substring(start).trim())
        }
        return list
    }

    fun parsePosition(posStr: String?): Pair<Float, Float>? {
        if (posStr.isNullOrBlank()) return null
        val parts = posStr.trim().split(Pattern.compile("[,\\s]+")).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null

        fun parseCoord(s: String): Float {
            val clean = s.lowercase()
            return when {
                clean == "left" || clean == "top" -> 0.0f
                clean == "center" -> 0.5f
                clean == "right" || clean == "bottom" -> 1.0f
                clean.endsWith("%") -> (clean.removeSuffix("%").toFloatOrNull() ?: 50f) / 100f
                else -> 0.5f
            }.coerceIn(0f, 1f)
        }

        val px = parseCoord(parts[0])
        val py = if (parts.size > 1) parseCoord(parts[1]) else 0.5f
        return Pair(px, py)
    }

    fun parseSizeRatio(sizeStr: String?): Float {
        if (sizeStr.isNullOrBlank()) return 1.0f
        val clean = sizeStr.trim().lowercase()
        return when {
            clean.contains("cover") -> 1.4f
            clean.contains("contain") -> 0.9f
            clean.endsWith("%") -> {
                val num = clean.split(Pattern.compile("[^0-9.]+")).firstOrNull { it.isNotBlank() }?.toFloatOrNull() ?: 100f
                (num / 100f).coerceIn(0.1f, 3.0f)
            }
            else -> 1.0f
        }
    }
}
