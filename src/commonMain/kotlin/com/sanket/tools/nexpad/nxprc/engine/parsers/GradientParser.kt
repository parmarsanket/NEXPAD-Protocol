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
                var isExplicitRadius = false
                var radiusRatio = 0.55f * defaultSizeRatio
                var aspectRatio = 1.0f

                // Check for extent keywords
                when {
                    inner.contains("closest-side") -> {
                        radiusRatio = minOf(cx, 1f - cx, cy, 1f - cy).coerceAtLeast(0.1f) * defaultSizeRatio
                        isExplicitRadius = true
                    }
                    inner.contains("closest-corner") -> {
                        val dx = minOf(cx, 1f - cx)
                        val dy = minOf(cy, 1f - cy)
                        radiusRatio = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.1f) * defaultSizeRatio
                        isExplicitRadius = true
                    }
                    inner.contains("farthest-side") -> {
                        radiusRatio = maxOf(cx, 1f - cx, cy, 1f - cy).coerceAtLeast(0.1f) * defaultSizeRatio
                        isExplicitRadius = true
                    }
                    inner.contains("farthest-corner") -> {
                        val dx = maxOf(cx, 1f - cx)
                        val dy = maxOf(cy, 1f - cy)
                        radiusRatio = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.1f) * defaultSizeRatio
                        isExplicitRadius = true
                    }
                }

                // Check explicit radius: e.g. "circle 45px at ..." or "40% at ..." or "ellipse 40px 20px at ..."
                val radiusMatcher = Pattern.compile("(?:circle\\s+|ellipse\\s+)?(\\d+(?:\\.\\d+)?)(px|%)?(?:\\s+(\\d+(?:\\.\\d+)?)(px|%)?)?\\s+at").matcher(inner)
                if (radiusMatcher.find()) {
                    val rVal1 = radiusMatcher.group(1).toFloatOrNull() ?: 50f
                    val unit1 = radiusMatcher.group(2)
                    val rVal2 = radiusMatcher.group(3)?.toFloatOrNull()
                    val unit2 = radiusMatcher.group(4)
                    val r1 = if (unit1 == "%") (rVal1 / 100f) else (rVal1 / 100f)
                    if (rVal2 != null) {
                        val r2 = if (unit2 == "%") (rVal2 / 100f) else (rVal2 / 100f)
                        radiusRatio = maxOf(r1, r2)
                        aspectRatio = if (r2 > 0.001f) r1 / r2 else 1.0f
                    } else {
                        radiusRatio = r1
                    }
                    isExplicitRadius = true
                }

                // Check center position: "at X% Y%" or keyword positions
                val atMatcher = Pattern.compile("at\\s+([a-zA-Z0-9%.-]+)(?:\\s+([a-zA-Z0-9%.-]+))?").matcher(inner)
                if (atMatcher.find()) {
                    val pos1 = atMatcher.group(1).lowercase()
                    val pos2 = atMatcher.group(2)?.lowercase()
                    val resolved = resolveKeywordPosition(pos1, pos2)
                    cx = resolved.first
                    cy = resolved.second
                }


                val (colors, stops) = parseGradientStops(inner)
                if (colors.size >= 2) {
                    return FillBrush.RadialGradient(
                        colors = colors,
                        stops = stops,
                        radiusRatio = radiusRatio,
                        centerXRatio = cx,
                        centerYRatio = cy,
                        aspectRatio = aspectRatio
                    )
                }
            }
        }

        // Linear Gradient
        if (clean.contains("linear-gradient")) {
            val inner = extractParenthesizedContent(clean, "linear-gradient")
            if (inner != null) {
                var angle = 180f // CSS default is "to bottom" = 180deg

                // Directional keywords
                when {
                    inner.contains("to top right") || inner.contains("to right top") -> angle = 45f
                    inner.contains("to bottom right") || inner.contains("to right bottom") -> angle = 135f
                    inner.contains("to bottom left") || inner.contains("to left bottom") -> angle = 225f
                    inner.contains("to top left") || inner.contains("to left top") -> angle = 315f
                    inner.contains("to top") -> angle = 0f
                    inner.contains("to right") -> angle = 90f
                    inner.contains("to bottom") -> angle = 180f
                    inner.contains("to left") -> angle = 270f
                    else -> {
                        // Numeric angles: deg, turn, rad
                        val degMatcher = Pattern.compile("(-?\\d+(?:\\.\\d+)?)deg").matcher(inner)
                        val turnMatcher = Pattern.compile("(-?\\d+(?:\\.\\d+)?)turn").matcher(inner)
                        val radMatcher = Pattern.compile("(-?\\d+(?:\\.\\d+)?)rad").matcher(inner)
                        if (degMatcher.find()) {
                            val d = degMatcher.group(1).toFloatOrNull() ?: 180f
                            angle = ((d % 360f) + 360f) % 360f
                        } else if (turnMatcher.find()) {
                            val t = turnMatcher.group(1).toFloatOrNull() ?: 0.5f
                            angle = (((t * 360f) % 360f) + 360f) % 360f
                        } else if (radMatcher.find()) {
                            val r = radMatcher.group(1).toDoubleOrNull() ?: Math.PI
                            angle = ((Math.toDegrees(r).toFloat() % 360f) + 360f) % 360f
                        }
                    }
                }

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
                var startAngle = 0f

                val fromMatcher = Pattern.compile("from\\s+(-?\\d+(?:\\.\\d+)?)(deg|turn|rad)?").matcher(inner)
                if (fromMatcher.find()) {
                    val num = fromMatcher.group(1).toFloatOrNull() ?: 0f
                    val unit = fromMatcher.group(2)?.lowercase() ?: "deg"
                    startAngle = when (unit) {
                        "turn" -> ((num * 360f) % 360f + 360f) % 360f
                        "rad" -> ((Math.toDegrees(num.toDouble()).toFloat() % 360f) + 360f) % 360f
                        else -> ((num % 360f) + 360f) % 360f
                    }
                }

                val atMatcher = Pattern.compile("at\\s+([a-zA-Z0-9%.-]+)(?:\\s+([a-zA-Z0-9%.-]+))?").matcher(inner)
                if (atMatcher.find()) {
                    val pos1 = atMatcher.group(1).lowercase()
                    val pos2 = atMatcher.group(2)?.lowercase()
                    val resolved = resolveKeywordPosition(pos1, pos2)
                    cx = resolved.first
                    cy = resolved.second
                }
                val (colors, stops) = parseGradientStops(inner)
                if (colors.size >= 2) {
                    return FillBrush.SweepGradient(
                        colors = colors,
                        centerXRatio = cx,
                        centerYRatio = cy,
                        stops = stops,
                        startAngleDegrees = startAngle
                    )
                }
            }
        }

        // Solid Color
        ColorParser.parse(clean)?.let { return FillBrush.Solid(it) }

        return null
    }

    private fun resolveKeywordPosition(pos1: String, pos2: String?): Pair<Float, Float> {
        var x = 0.5f
        var y = 0.5f

        fun parseToken(tok: String, isSecond: Boolean) {
            when (tok) {
                "left" -> x = 0.0f
                "right" -> x = 1.0f
                "top" -> y = 0.0f
                "bottom" -> y = 1.0f
                "center" -> { if (!isSecond) { x = 0.5f; y = 0.5f } }
                else -> {
                    if (tok.endsWith("%")) {
                        val v = (tok.removeSuffix("%").toFloatOrNull() ?: 50f) / 100f
                        if (!isSecond) x = v else y = v
                    } else if (tok.endsWith("px")) {
                        val v = (tok.removeSuffix("px").toFloatOrNull() ?: 50f) / 100f
                        if (!isSecond) x = v else y = v
                    }
                }
            }
        }

        parseToken(pos1, false)
        if (pos2 != null) {
            parseToken(pos2, true)
        }
        return Pair(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
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
                val pcts = mutableListOf<Float>()
                val pctMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)%").matcher(p)
                while (pctMatcher.find()) {
                    pcts.add((pctMatcher.group(1).toFloatOrNull() ?: 0f) / 100f)
                }

                val degMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)deg").matcher(p)

                when {
                    pcts.size >= 2 -> {
                        // Multi-position stop: e.g. "#fff 20% 50%" creates two stops
                        colors.add(color)
                        stops.add(pcts[0])
                        colors.add(color)
                        stops.add(pcts[1])
                    }
                    pcts.size == 1 -> {
                        colors.add(color)
                        stops.add(pcts[0])
                    }
                    degMatcher.find() -> {
                        colors.add(color)
                        stops.add(((degMatcher.group(1).toFloatOrNull() ?: 0f) / 360f).coerceIn(0f, 1f))
                    }
                    else -> {
                        colors.add(color)
                        stops.add(-1f)
                    }
                }
            }
        }

        if (colors.isNotEmpty() && stops.contains(-1f)) {
            val interpolated = stops.toMutableList()
            // Set first/last if undefined
            if (interpolated[0] < 0f) interpolated[0] = 0f
            if (interpolated.last() < 0f) interpolated[interpolated.lastIndex] = 1f
            // Linear-interpolate between defined anchor points
            var lastDefined = 0
            for (i in 1 until interpolated.size) {
                if (interpolated[i] >= 0f) {
                    if (i - lastDefined > 1) {
                        val startVal = interpolated[lastDefined]
                        val endVal = interpolated[i]
                        val span = i - lastDefined
                        for (j in 1 until span) {
                            interpolated[lastDefined + j] = startVal + (endVal - startVal) * j / span
                        }
                    }
                    lastDefined = i
                }
            }
            // Ensure monotonic non-decreasing for Compose Brush requirements
            for (i in 1 until interpolated.size) {
                if (interpolated[i] < interpolated[i - 1]) {
                    interpolated[i] = interpolated[i - 1]
                }
            }
            return Pair(colors, interpolated)
        }

        // Ensure monotonic non-decreasing even if all stops were defined
        if (stops.size > 1) {
            val sortedStops = stops.toMutableList()
            for (i in 1 until sortedStops.size) {
                if (sortedStops[i] < sortedStops[i - 1]) {
                    sortedStops[i] = sortedStops[i - 1]
                }
            }
            return Pair(colors, sortedStops)
        }

        return Pair(colors, stops)
    }

    fun splitTopLevelCommas(text: String): List<String> {
        val list = mutableListOf<String>()
        var start = 0
        var depth = 0
        var inSingleQuote = false
        var inDoubleQuote = false
        for (i in text.indices) {
            val c = text[i]
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote
            } else if (!inSingleQuote && !inDoubleQuote) {
                if (c == '(') depth++
                else if (c == ')') depth--
                else if (c == ',' && depth == 0) {
                    list.add(text.substring(start, i).trim())
                    start = i + 1
                }
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
