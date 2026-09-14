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

    private val RADIUS_AT_PATTERN = Pattern.compile("(?:circle\\s+|ellipse\\s+)?(\\d+(?:\\.\\d+)?)(px|%)?(?:\\s+(\\d+(?:\\.\\d+)?)(px|%)?)?\\s+at")
    private val AT_POSITION_PATTERN = Pattern.compile("at\\s+([a-zA-Z0-9%.-]+)(?:\\s+([a-zA-Z0-9%.-]+))?")
    private val FROM_ANGLE_PATTERN = Pattern.compile("from\\s+(-?\\d+(?:\\.\\d+)?)(deg|turn|rad)?")

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
            val inner = GradientStopsParser.extractParenthesizedContent(clean, "radial-gradient")
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
                val radiusMatcher = RADIUS_AT_PATTERN.matcher(inner)
                if (radiusMatcher.find()) {
                    val rVal1 = radiusMatcher.group(1).toFloatOrNull() ?: 50f
                    val rVal2 = radiusMatcher.group(3)?.toFloatOrNull()
                    val r1 = (rVal1 / 100f).coerceAtLeast(0.01f)
                    if (rVal2 != null) {
                        val r2 = (rVal2 / 100f).coerceAtLeast(0.01f)
                        radiusRatio = maxOf(r1, r2)
                        aspectRatio = if (r2 > 0.001f) r1 / r2 else 1.0f
                    } else {
                        radiusRatio = r1
                    }
                    isExplicitRadius = true
                }

                // Check center position: "at X% Y%" or keyword positions
                val atMatcher = AT_POSITION_PATTERN.matcher(inner)
                if (atMatcher.find()) {
                    val pos1 = atMatcher.group(1).lowercase()
                    val pos2 = atMatcher.group(2)?.lowercase()
                    val resolved = GradientStopsParser.resolveKeywordPosition(pos1, pos2)
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
            val inner = GradientStopsParser.extractParenthesizedContent(clean, "linear-gradient")
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
                        val degMatcher = CssSyntaxPattern.DEGREE.matcher(inner)
                        val turnMatcher = CssSyntaxPattern.TURN.matcher(inner)
                        val radMatcher = CssSyntaxPattern.RADIAN.matcher(inner)
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
            val inner = GradientStopsParser.extractParenthesizedContent(clean, "conic-gradient")
            if (inner != null) {
                var cx = 0.5f
                var cy = 0.5f
                var startAngle = 0f

                val fromMatcher = FROM_ANGLE_PATTERN.matcher(inner)
                if (fromMatcher.find()) {
                    val num = fromMatcher.group(1).toFloatOrNull() ?: 0f
                    val unit = fromMatcher.group(2)?.lowercase() ?: "deg"
                    startAngle = when (unit) {
                        AngleUnit.TURN.suffix -> ((num * AngleUnit.TURN.degreesMultiplier) % 360f + 360f) % 360f
                        AngleUnit.RAD.suffix -> ((num * AngleUnit.RAD.degreesMultiplier) % 360f + 360f) % 360f
                        else -> ((num % 360f) + 360f) % 360f
                    }
                }

                val atMatcher = AT_POSITION_PATTERN.matcher(inner)
                if (atMatcher.find()) {
                    val pos1 = atMatcher.group(1).lowercase()
                    val pos2 = atMatcher.group(2)?.lowercase()
                    val resolved = GradientStopsParser.resolveKeywordPosition(pos1, pos2)
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

    fun parseGradientStops(inner: String): Pair<List<Long>, List<Float>> =
        GradientStopsParser.parseGradientStops(inner)

    fun splitTopLevelCommas(text: String): List<String> =
        GradientStopsParser.splitTopLevelCommas(text)

    fun parsePosition(posStr: String?): Pair<Float, Float>? =
        GradientStopsParser.parsePosition(posStr)

    fun parseSizeRatio(sizeStr: String?): Float =
        GradientStopsParser.parseSizeRatio(sizeStr)
}

