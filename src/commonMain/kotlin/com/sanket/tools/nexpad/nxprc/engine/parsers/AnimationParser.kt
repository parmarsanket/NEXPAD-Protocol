package com.sanket.tools.nexpad.nxprc.engine.parsers

import com.sanket.tools.nexpad.nxprc.engine.css.CssStylesheet
import java.util.regex.Pattern

data class ParsedTransform(
    val translateX: Float = 0.0f,
    val translateY: Float = 0.0f,
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val rotationDegrees: Float = 0.0f,
    val skewX: Float = 0.0f,
    val skewY: Float = 0.0f,
    val originXRatio: Float = 0.5f,
    val originYRatio: Float = 0.5f
)

/**
 * Dedicated parser for CSS transforms and animations:
 * - scale(), scaleX(), scaleY(), rotate(), translateY(), translateX(), skew(), skewX(), skewY(), matrix()
 * - transform-origin
 * - @keyframes analysis for idle loops (ROTATE, PULSE)
 * - Touch active deformation and physics
 */
object AnimationParser {

    private val FUNC_PATTERN = Pattern.compile("([a-zA-Z0-9]+)\\s*\\(([^)]+)\\)")
    private val DELIMITER_PATTERN = Pattern.compile("[,\\s]+")
    private val NUMBER_PATTERN = Pattern.compile("(-?[0-9.]+)")

    fun parseTransforms(
        transformStr: String?,
        originStr: String? = null,
        width: Float = 100f,
        height: Float = 100f
    ): ParsedTransform {
        val (ox, oy) = parseOrigin(originStr, width, height)
        if (transformStr.isNullOrBlank() || transformStr.trim().equals("none", ignoreCase = true)) {
            return ParsedTransform(originXRatio = ox, originYRatio = oy)
        }

        var sx = 1.0f
        var sy = 1.0f
        var rot = 0.0f
        var tx = 0.0f
        var ty = 0.0f
        var skx = 0.0f
        var sky = 0.0f

        // Tokenize all CSS transform function calls: funcName(args)
        val matcher = FUNC_PATTERN.matcher(transformStr)

        while (matcher.find()) {
            val func = matcher.group(1).lowercase()
            val rawArgs = matcher.group(2).trim()
            val args = rawArgs.split(DELIMITER_PATTERN).map { it.trim() }.filter { it.isNotEmpty() }

            when (func) {
                "scale" -> {
                    if (args.isNotEmpty()) {
                        val s = parseNumber(args[0]) ?: 1.0f
                        sx *= s
                        sy *= if (args.size > 1) parseNumber(args[1]) ?: s else s
                    }
                }
                "scalex" -> {
                    if (args.isNotEmpty()) {
                        sx *= parseNumber(args[0]) ?: 1.0f
                    }
                }
                "scaley" -> {
                    if (args.isNotEmpty()) {
                        sy *= parseNumber(args[0]) ?: 1.0f
                    }
                }
                "rotate", "rotatez" -> {
                    if (args.isNotEmpty()) {
                        rot += parseAngle(args[0])
                    }
                }
                "skew" -> {
                    if (args.isNotEmpty()) {
                        skx += parseAngle(args[0])
                        if (args.size > 1) {
                            sky += parseAngle(args[1])
                        }
                    }
                }
                "skewx" -> {
                    if (args.isNotEmpty()) {
                        skx += parseAngle(args[0])
                    }
                }
                "skewy" -> {
                    if (args.isNotEmpty()) {
                        sky += parseAngle(args[0])
                    }
                }
                "translate" -> {
                    if (args.isNotEmpty()) {
                            tx += parseDimension(args[0], width)
                        if (args.size > 1) {
                            ty += parseDimension(args[1], height)
                        }
                    }
                }
                "translatex" -> {
                    if (args.isNotEmpty()) {
                        tx += parseDimension(args[0], width)
                    }
                }
                "translatey" -> {
                    if (args.isNotEmpty()) {
                        ty += parseDimension(args[0], height)
                    }
                }
                "matrix" -> {
                    // matrix(a, b, c, d, e, f)
                    if (args.size >= 6) {
                        val a = parseNumber(args[0]) ?: 1f
                        val b = parseNumber(args[1]) ?: 0f
                        val c = parseNumber(args[2]) ?: 0f
                        val d = parseNumber(args[3]) ?: 1f
                        val e = parseDimension(args[4], width)
                        val f = parseDimension(args[5], height)
                        sx *= kotlin.math.sqrt(a * a + b * b)
                        sy *= kotlin.math.sqrt(c * c + d * d)
                        rot += Math.toDegrees(kotlin.math.atan2(b.toDouble(), a.toDouble())).toFloat()
                        tx += e
                        ty += f
                    }
                }
            }
        }

        return ParsedTransform(
            translateX = tx,
            translateY = ty,
            scaleX = sx,
            scaleY = sy,
            rotationDegrees = rot,
            skewX = skx,
            skewY = sky,
            originXRatio = ox,
            originYRatio = oy
        )
    }

    fun parseOrigin(originStr: String?, width: Float = 100f, height: Float = 100f): Pair<Float, Float> {
        if (originStr.isNullOrBlank()) return Pair(0.5f, 0.5f)
        val parts = originStr.trim().split(DELIMITER_PATTERN).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return Pair(0.5f, 0.5f)

        fun parseSingleCoord(s: String, dim: Float): Float {
            val clean = s.lowercase()
            return when {
                clean == "left" || clean == "top" -> 0.0f
                clean == "center" -> 0.5f
                clean == "right" || clean == "bottom" -> 1.0f
                clean.endsWith("%") -> (clean.removeSuffix("%").toFloatOrNull() ?: 50f) / 100f
                clean.endsWith("px") -> (clean.removeSuffix("px").toFloatOrNull() ?: (dim / 2f)) / dim
                else -> (clean.toFloatOrNull() ?: (dim / 2f)) / dim
            }.coerceIn(0f, 1f)
        }

        val ox = parseSingleCoord(parts[0], width)
        val oy = if (parts.size > 1) parseSingleCoord(parts[1], height) else 0.5f
        return Pair(ox, oy)
    }

    private fun parseNumber(s: String): Float? {
        val m = NUMBER_PATTERN.matcher(s)
        return if (m.find()) m.group(1).toFloatOrNull() else null
    }

    private fun parseAngle(s: String): Float {
        val clean = s.trim().lowercase()
        val num = parseNumber(clean) ?: 0.0f
        return when {
            clean.endsWith("rad") -> Math.toDegrees(num.toDouble()).toFloat()
            clean.endsWith("turn") -> num * 360f
            clean.endsWith("grad") -> num * 0.9f
            else -> num // default deg
        }
    }

    private fun parseDimension(s: String, reference: Float = 100f): Float {
        val clean = s.trim().lowercase()
        val num = parseNumber(clean) ?: 0.0f
        return when {
            clean.endsWith("px") -> num
            clean.endsWith("%") -> (num / 100f) * reference
            clean.endsWith("em") || clean.endsWith("rem") -> num * 16f
            else -> num
        }
    }

    fun isRotatingAnimation(stylesheet: CssStylesheet, props: Map<String, String>): Boolean {
        val anim = props["animation"] ?: props["animation-name"] ?: ""
        if (anim.contains("spin", ignoreCase = true) || anim.contains("rotate", ignoreCase = true)) return true
        stylesheet.keyframes.values.forEach { kf ->
            if (kf.name.contains("rotate", ignoreCase = true) || kf.name.contains("spin", ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun isPulsingAnimation(stylesheet: CssStylesheet, props: Map<String, String>): Boolean {
        val anim = props["animation"] ?: props["animation-name"] ?: ""
        if (anim.contains("pulse", ignoreCase = true) || anim.contains("glow", ignoreCase = true)) return true
        stylesheet.keyframes.values.forEach { kf ->
            if (kf.name.contains("pulse", ignoreCase = true)) return true
        }
        return false
    }
}
