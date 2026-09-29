package com.sanket.tools.nexpad.nxprc.engine.parsers

import java.util.regex.Pattern

/**
 * Parses and interpolates CSS gradient color-stops, position directives, and parenthesized expressions.
 * Enforces Compose Brush constraints including monotonic non-decreasing stop positions.
 */
internal object GradientStopsParser {

    private val NON_DIGIT_DOT_PATTERN = Pattern.compile("[^0-9.]+")

    /**
     * Splits comma-separated CSS values while respecting nested parentheses and quoted substrings.
     */
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

    /**
     * Parses CSS background-position string into normalized (X, Y) ratio coordinates (0..1).
     */
    fun parsePosition(posStr: String?): Pair<Float, Float>? {
        if (posStr.isNullOrBlank()) return null
        val parts = ColorPattern.DELIMITER.split(posStr.trim()).map { it.trim() }.filter { it.isNotEmpty() }
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

    /**
     * Parses CSS background-size keyword (cover, contain) or percentage into a relative scaling ratio.
     */
    fun parseSizeRatio(sizeStr: String?): Float {
        if (sizeStr.isNullOrBlank()) return 1.0f
        val clean = sizeStr.trim().lowercase()
        return when {
            clean.contains("cover") -> 1.4f
            clean.contains("contain") -> 0.9f
            clean.endsWith("%") -> {
                val num = clean.split(NON_DIGIT_DOT_PATTERN).firstOrNull { it.isNotBlank() }?.toFloatOrNull() ?: 100f
                (num / 100f).coerceIn(0.1f, 3.0f)
            }
            else -> 1.0f
        }
    }

    /**
     * Extracts parenthesized content matching the specified CSS functional syntax name.
     */
    fun extractParenthesizedContent(text: String, funcName: String): String? {
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

    /**
     * Resolves positional keywords (left, right, top, bottom, center) or percentages into (X, Y) 0..1 coordinates.
     */
    fun resolveKeywordPosition(pos1: String, pos2: String?): Pair<Float, Float> {
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

    /**
     * Parses gradient stops from inner CSS gradient definitions, interpolating missing stops
     * and enforcing monotonic ordering for GPU brush shaders.
     */
    fun parseGradientStops(
        inner: String,
        isRepeating: Boolean = false,
        axisDimensionPx: Float = 0f
    ): Pair<List<Long>, List<Float>> {
        val rawColors = mutableListOf<Long>()
        val rawPcts = mutableListOf<Float?>()
        val rawPxs = mutableListOf<Float?>()

        val parts = splitTopLevelCommas(inner)
        val pxPattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?)px")

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
                val pctMatcher = CssSyntaxPattern.PERCENT.matcher(p)
                while (pctMatcher.find()) {
                    pcts.add((pctMatcher.group(1).toFloatOrNull() ?: 0f) / 100f)
                }

                val pxs = mutableListOf<Float>()
                val pxMatcher = pxPattern.matcher(p)
                while (pxMatcher.find()) {
                    pxs.add(pxMatcher.group(1).toFloatOrNull() ?: 0f)
                }

                val degMatcher = CssSyntaxPattern.DEGREE.matcher(p)

                when {
                    pcts.size >= 2 -> {
                        rawColors.add(color); rawPcts.add(pcts[0]); rawPxs.add(null)
                        rawColors.add(color); rawPcts.add(pcts[1]); rawPxs.add(null)
                    }
                    pcts.size == 1 -> {
                        rawColors.add(color); rawPcts.add(pcts[0]); rawPxs.add(null)
                    }
                    pxs.size >= 2 -> {
                        rawColors.add(color); rawPcts.add(null); rawPxs.add(pxs[0])
                        rawColors.add(color); rawPcts.add(null); rawPxs.add(pxs[1])
                    }
                    pxs.size == 1 -> {
                        rawColors.add(color); rawPcts.add(null); rawPxs.add(pxs[0])
                    }
                    degMatcher.find() -> {
                        rawColors.add(color)
                        rawPcts.add(((degMatcher.group(1).toFloatOrNull() ?: 0f) / 360f).coerceIn(0f, 1f))
                        rawPxs.add(null)
                    }
                    else -> {
                        rawColors.add(color); rawPcts.add(null); rawPxs.add(null)
                    }
                }
            }
        }

        if (rawColors.isEmpty()) return Pair(emptyList(), emptyList())

        // 1. Pixel-based stops
        val hasPixelStops = rawPxs.any { it != null }
        if (hasPixelStops) {
            val nonNullPxs = rawPxs.filterNotNull()
            val minPx = nonNullPxs.minOrNull() ?: 0f
            val maxPx = nonNullPxs.maxOrNull() ?: 0f
            val periodPx = maxPx - minPx
            val targetDim = if (axisDimensionPx > 0f) axisDimensionPx else if (periodPx > 0f) periodPx * 5f else 48f

            if (isRepeating && periodPx > 0f) {
                val cycles = (targetDim / periodPx).toInt().coerceIn(2, 25) + 1
                val expColors = mutableListOf<Long>()
                val expStops = mutableListOf<Float>()
                for (cycle in 0 until cycles) {
                    val cycleBasePx = cycle * periodPx
                    for (i in rawColors.indices) {
                        val px = rawPxs[i] ?: (i.toFloat() / (rawColors.size - 1).coerceAtLeast(1) * periodPx)
                        val totalPx = cycleBasePx + px
                        val ratio = (totalPx / targetDim)
                        if (ratio <= 1.0f) {
                            expColors.add(rawColors[i])
                            expStops.add(ratio)
                        }
                    }
                }
                if (expStops.isNotEmpty() && expStops.last() < 1.0f) {
                    expColors.add(rawColors.last())
                    expStops.add(1.0f)
                }
                return finalizeStops(expColors, expStops)
            } else {
                val resolvedStops = rawPxs.mapIndexed { i, px ->
                    if (px != null) (px / targetDim).coerceIn(0f, 1f)
                    else i.toFloat() / (rawColors.size - 1).coerceAtLeast(1)
                }
                return finalizeStops(rawColors, resolvedStops)
            }
        }

        // 2. Percentage stops (interpolate missing -1f values)
        val baseStops = rawPcts.map { it ?: -1f }
        val interpolated = baseStops.toMutableList()
        if (interpolated[0] < 0f) interpolated[0] = 0f
        if (interpolated.last() < 0f) interpolated[interpolated.lastIndex] = 1f
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

        if (isRepeating && interpolated.isNotEmpty()) {
            val minS = interpolated.minOrNull() ?: 0f
            val maxS = interpolated.maxOrNull() ?: 1f
            val period = maxS - minS
            if (period in 0.01f..0.99f) {
                val cycles = (1.0f / period).toInt().coerceIn(2, 25) + 1
                val expColors = mutableListOf<Long>()
                val expStops = mutableListOf<Float>()
                for (cycle in 0 until cycles) {
                    val cycleBase = cycle * period
                    for (i in rawColors.indices) {
                        val s = cycleBase + interpolated[i]
                        if (s <= 1.0f) {
                            expColors.add(rawColors[i])
                            expStops.add(s)
                        }
                    }
                }
                if (expStops.isNotEmpty() && expStops.last() < 1.0f) {
                    expColors.add(rawColors.last())
                    expStops.add(1.0f)
                }
                return finalizeStops(expColors, expStops)
            }
        }

        return finalizeStops(rawColors, interpolated)
    }

    private fun finalizeStops(colors: List<Long>, stops: List<Float>): Pair<List<Long>, List<Float>> {
        if (stops.isEmpty()) return Pair(colors, stops)
        val sortedStops = stops.toMutableList()
        for (i in 1 until sortedStops.size) {
            if (sortedStops[i] < sortedStops[i - 1]) {
                sortedStops[i] = sortedStops[i - 1]
            }
        }
        return Pair(colors, sortedStops)
    }
}
