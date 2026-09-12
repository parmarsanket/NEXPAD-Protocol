package com.sanket.tools.nexpad.nxprc.engine.parsers

import com.sanket.tools.nexpad.nxprc.AnimatedProperty
import com.sanket.tools.nexpad.nxprc.AnimationKeyframePoint
import com.sanket.tools.nexpad.nxprc.AnimationTrack
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

    private val FUNC_PATTERN = CssSyntaxPattern.FUNCTION_CALL.pattern
    private val DELIMITER_PATTERN = ColorPattern.DELIMITER.pattern
    private val NUMBER_PATTERN = CssSyntaxPattern.NUMBER.pattern

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
        return AngleUnit.parseToDegrees(clean) ?: (parseNumber(clean) ?: 0f)
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
        if (anim.contains("pulse", ignoreCase = true) || anim.contains("glow", ignoreCase = true) || anim.contains("breathe", ignoreCase = true)) return true
        stylesheet.keyframes.values.forEach { kf ->
            if (kf.name.contains("pulse", ignoreCase = true) || kf.name.contains("glow", ignoreCase = true) || kf.name.contains("breathe", ignoreCase = true)) return true
        }
        return false
    }

    fun isRgbCycleAnimation(stylesheet: CssStylesheet, props: Map<String, String>): Boolean {
        val anim = props["animation"] ?: props["animation-name"] ?: ""
        val filter = props["filter"] ?: ""
        if (anim.contains("rgb", ignoreCase = true) || anim.contains("rainbow", ignoreCase = true) ||
            anim.contains("chroma", ignoreCase = true) || anim.contains("hue", ignoreCase = true)) return true
        if (filter.contains("hue-rotate", ignoreCase = true)) return true

        stylesheet.keyframes.values.forEach { kf ->
            val kn = kf.name.lowercase()
            if (kn.contains("rgb") || kn.contains("rainbow") || kn.contains("chroma") || kn.contains("hue")) {
                return true
            }
            if (kf.steps.any { step -> step.declarations.any { (k, v) -> k == "filter" && v.contains("hue-rotate") } }) {
                return true
            }
        }
        return false
    }

    fun isShimmerAnimation(stylesheet: CssStylesheet, props: Map<String, String>): Boolean {
        val anim = props["animation"] ?: props["animation-name"] ?: ""
        if (anim.contains("shimmer", ignoreCase = true) || anim.contains("shine", ignoreCase = true) ||
            anim.contains("glimmer", ignoreCase = true) || anim.contains("sweep", ignoreCase = true)) return true

        stylesheet.keyframes.values.forEach { kf ->
            val kn = kf.name.lowercase()
            if (kn.contains("shimmer") || kn.contains("shine") || kn.contains("glimmer") || kn.contains("sweep")) {
                return true
            }
        }
        return false
    }

    fun detectSvgAnimation(root: com.sanket.tools.nexpad.nxprc.engine.dom.DomNode): com.sanket.tools.nexpad.nxprc.IdleAnimationType? {
        var detected: com.sanket.tools.nexpad.nxprc.IdleAnimationType? = null
        fun recurse(node: com.sanket.tools.nexpad.nxprc.engine.dom.DomNode) {
            if (detected != null) return
            val tag = node.tag.lowercase()
            if (tag == "animatetransform") {
                val type = node.attributes["type"]?.lowercase()
                if (type == "rotate") {
                    detected = com.sanket.tools.nexpad.nxprc.IdleAnimationType.ROTATE
                    return
                } else if (type == "scale") {
                    detected = com.sanket.tools.nexpad.nxprc.IdleAnimationType.PULSE
                    return
                }
            } else if (tag == "animate") {
                val attrName = node.attributes["attributename"]?.lowercase()
                val values = node.attributes["values"] ?: ""
                if (attrName == "opacity" || attrName == "r") {
                    detected = com.sanket.tools.nexpad.nxprc.IdleAnimationType.PULSE
                    return
                } else if (attrName == "fill" || attrName == "stroke" || values.contains("hue-rotate")) {
                    detected = com.sanket.tools.nexpad.nxprc.IdleAnimationType.RGB_CYCLE
                    return
                }
            }
            node.children.forEach { recurse(it) }
        }
        recurse(root)
        return detected
    }

    /**
     * Universal Timeline Track Extractor:
     * Analyzes CSS @keyframes and property declarations, compiling them into
     * dynamic numeric property tracks that can drive any arbitrary animation at 120 FPS.
     */
    fun extractAnimationTracks(stylesheet: CssStylesheet, props: Map<String, String>): List<AnimationTrack> {
        val animStr = props["animation"] ?: ""
        val animName = props["animation-name"]?.trim() ?: extractAnimationName(animStr)
        if (animName.isBlank()) return emptyList()

        val keyframes = stylesheet.keyframes[animName]
            ?: stylesheet.keyframes.values.firstOrNull { it.name.equals(animName, ignoreCase = true) }
            ?: return emptyList()

        val durationMs = extractDurationMs(animStr, props["animation-duration"])
        val isInfinite = animStr.contains("infinite", ignoreCase = true) ||
            props["animation-iteration-count"]?.contains("infinite", ignoreCase = true) == true
        val easing = extractEasing(animStr, props["animation-timing-function"])

        val scaleKeyframes = mutableListOf<AnimationKeyframePoint>()
        val rotationKeyframes = mutableListOf<AnimationKeyframePoint>()
        val opacityKeyframes = mutableListOf<AnimationKeyframePoint>()
        val translateXKeyframes = mutableListOf<AnimationKeyframePoint>()
        val translateYKeyframes = mutableListOf<AnimationKeyframePoint>()
        val hueRotateKeyframes = mutableListOf<AnimationKeyframePoint>()

        val sortedSteps = keyframes.steps.sortedBy { it.percentage }
        for (step in sortedSteps) {
            val frac = step.percentage.coerceIn(0f, 1f)

            // 1. Transform declarations
            val transformStr = step.declarations["transform"]
            if (!transformStr.isNullOrBlank() && !transformStr.equals("none", ignoreCase = true)) {
                val matcher = FUNC_PATTERN.matcher(transformStr)
                while (matcher.find()) {
                    val func = matcher.group(1).lowercase()
                    val rawArgs = matcher.group(2).trim()
                    val args = rawArgs.split(DELIMITER_PATTERN).map { it.trim() }.filter { it.isNotEmpty() }
                    when (func) {
                        "scale" -> {
                            if (args.isNotEmpty()) {
                                val s = parseNumber(args[0]) ?: 1.0f
                                scaleKeyframes.add(AnimationKeyframePoint(frac, s))
                            }
                        }
                        "scalex" -> {
                            if (args.isNotEmpty()) {
                                val s = parseNumber(args[0]) ?: 1.0f
                                scaleKeyframes.add(AnimationKeyframePoint(frac, s))
                            }
                        }
                        "rotate", "rotatez" -> {
                            if (args.isNotEmpty()) {
                                val rot = parseAngle(args[0])
                                rotationKeyframes.add(AnimationKeyframePoint(frac, rot))
                            }
                        }
                        "translate" -> {
                            if (args.isNotEmpty()) {
                                val tx = parseDimension(args[0])
                                translateXKeyframes.add(AnimationKeyframePoint(frac, tx))
                                if (args.size > 1) {
                                    val ty = parseDimension(args[1])
                                    translateYKeyframes.add(AnimationKeyframePoint(frac, ty))
                                }
                            }
                        }
                        "translatex" -> {
                            if (args.isNotEmpty()) {
                                val tx = parseDimension(args[0])
                                translateXKeyframes.add(AnimationKeyframePoint(frac, tx))
                            }
                        }
                        "translatey" -> {
                            if (args.isNotEmpty()) {
                                val ty = parseDimension(args[0])
                                translateYKeyframes.add(AnimationKeyframePoint(frac, ty))
                            }
                        }
                    }
                }
            }

            // 2. Opacity declaration
            val opacityStr = step.declarations["opacity"]
            if (!opacityStr.isNullOrBlank()) {
                val op = parseNumber(opacityStr)?.coerceIn(0f, 1f)
                if (op != null) {
                    opacityKeyframes.add(AnimationKeyframePoint(frac, op))
                }
            }

            // 3. Filter hue-rotate declaration
            val filterStr = step.declarations["filter"]
            if (!filterStr.isNullOrBlank()) {
                val matcher = FUNC_PATTERN.matcher(filterStr)
                while (matcher.find()) {
                    val func = matcher.group(1).lowercase()
                    val rawArgs = matcher.group(2).trim()
                    if (func == "hue-rotate") {
                        val angle = parseAngle(rawArgs)
                        hueRotateKeyframes.add(AnimationKeyframePoint(frac, angle))
                    }
                }
            }
        }

        val tracks = mutableListOf<AnimationTrack>()
        if (scaleKeyframes.isNotEmpty()) {
            normalizeKeyframes(scaleKeyframes, 1.0f)
            tracks.add(AnimationTrack(AnimatedProperty.SCALE, scaleKeyframes, durationMs, isInfinite, easing))
        }
        if (rotationKeyframes.isNotEmpty()) {
            normalizeKeyframes(rotationKeyframes, 0.0f)
            tracks.add(AnimationTrack(AnimatedProperty.ROTATION, rotationKeyframes, durationMs, isInfinite, easing))
        }
        if (opacityKeyframes.isNotEmpty()) {
            normalizeKeyframes(opacityKeyframes, 1.0f)
            tracks.add(AnimationTrack(AnimatedProperty.OPACITY, opacityKeyframes, durationMs, isInfinite, easing))
        }
        if (translateXKeyframes.isNotEmpty()) {
            normalizeKeyframes(translateXKeyframes, 0.0f)
            tracks.add(AnimationTrack(AnimatedProperty.TRANSLATE_X, translateXKeyframes, durationMs, isInfinite, easing))
        }
        if (translateYKeyframes.isNotEmpty()) {
            normalizeKeyframes(translateYKeyframes, 0.0f)
            tracks.add(AnimationTrack(AnimatedProperty.TRANSLATE_Y, translateYKeyframes, durationMs, isInfinite, easing))
        }
        if (hueRotateKeyframes.isNotEmpty()) {
            normalizeKeyframes(hueRotateKeyframes, 0.0f)
            tracks.add(AnimationTrack(AnimatedProperty.HUE_ROTATE, hueRotateKeyframes, durationMs, isInfinite, easing))
        }

        return tracks
    }

    /**
     * Universal SVG Animation Track Extractor:
     * Converts SVG <animate> and <animateTransform> nodes into timeline tracks.
     */
    fun extractSvgAnimationTracks(root: com.sanket.tools.nexpad.nxprc.engine.dom.DomNode): List<AnimationTrack> {
        val tracks = mutableListOf<AnimationTrack>()
        fun recurse(node: com.sanket.tools.nexpad.nxprc.engine.dom.DomNode) {
            val tag = node.tag.lowercase()
            val durMs = parseTimeMs(node.attributes["dur"] ?: "2s")
            val isInfinite = node.attributes["repeatcount"]?.equals("indefinite", ignoreCase = true) ?: true

            if (tag == "animatetransform") {
                val type = node.attributes["type"]?.lowercase() ?: ""
                val fromStr = node.attributes["from"]
                val toStr = node.attributes["to"]
                val valuesStr = node.attributes["values"]

                fun extractValues(): List<Float> {
                    if (!valuesStr.isNullOrBlank()) {
                        return valuesStr.split(";").mapNotNull { it.trim().split(" ").firstOrNull()?.toFloatOrNull() }
                    }
                    val f = fromStr?.trim()?.split(" ")?.firstOrNull()?.toFloatOrNull() ?: 0f
                    val t = toStr?.trim()?.split(" ")?.firstOrNull()?.toFloatOrNull() ?: 0f
                    return listOf(f, t)
                }

                val vals = extractValues()
                if (vals.size >= 2) {
                    val keypoints = vals.mapIndexed { idx, v ->
                        AnimationKeyframePoint(idx.toFloat() / (vals.size - 1).toFloat(), v)
                    }
                    when (type) {
                        "rotate" -> tracks.add(AnimationTrack(AnimatedProperty.ROTATION, keypoints, durMs, isInfinite))
                        "scale" -> tracks.add(AnimationTrack(AnimatedProperty.SCALE, keypoints, durMs, isInfinite))
                        "translate" -> tracks.add(AnimationTrack(AnimatedProperty.TRANSLATE_X, keypoints, durMs, isInfinite))
                    }
                }
            } else if (tag == "animate") {
                val attrName = node.attributes["attributename"]?.lowercase() ?: ""
                val valuesStr = node.attributes["values"]
                val fromStr = node.attributes["from"]
                val toStr = node.attributes["to"]

                fun extractValues(): List<Float> {
                    if (!valuesStr.isNullOrBlank()) {
                        return valuesStr.split(";").mapNotNull { it.trim().toFloatOrNull() }
                    }
                    val f = fromStr?.trim()?.toFloatOrNull() ?: 0f
                    val t = toStr?.trim()?.toFloatOrNull() ?: 0f
                    return listOf(f, t)
                }

                if (attrName == "opacity") {
                    val vals = extractValues()
                    if (vals.size >= 2) {
                        val keypoints = vals.mapIndexed { idx, v ->
                            AnimationKeyframePoint(idx.toFloat() / (vals.size - 1).toFloat(), v)
                        }
                        tracks.add(AnimationTrack(AnimatedProperty.OPACITY, keypoints, durMs, isInfinite))
                    }
                } else if (attrName == "fill" || attrName == "stroke" || (valuesStr?.contains("hue-rotate") == true)) {
                    val keypoints = listOf(
                        AnimationKeyframePoint(0.0f, 0.0f),
                        AnimationKeyframePoint(1.0f, 360.0f)
                    )
                    tracks.add(AnimationTrack(AnimatedProperty.HUE_ROTATE, keypoints, durMs, isInfinite))
                }
            }
            node.children.forEach { recurse(it) }
        }
        recurse(root)
        return tracks
    }

    private fun normalizeKeyframes(points: MutableList<AnimationKeyframePoint>, defaultValue: Float) {
        if (points.isEmpty()) return
        points.sortBy { it.fraction }
        if (points.first().fraction > 0.001f) {
            points.add(0, AnimationKeyframePoint(0.0f, points.first().value))
        }
        if (points.last().fraction < 0.999f) {
            points.add(AnimationKeyframePoint(1.0f, points.last().value))
        }
    }

    private fun extractDurationMs(animStr: String, explicitDuration: String?): Int {
        val candidate = explicitDuration?.trim() ?: run {
            val tokens = animStr.split(Pattern.compile("\\s+"))
            tokens.firstOrNull { it.endsWith("s", ignoreCase = true) || it.endsWith("ms", ignoreCase = true) }
        } ?: "2s"
        return parseTimeMs(candidate)
    }

    private fun parseTimeMs(s: String): Int {
        val clean = s.trim().lowercase()
        return when {
            clean.endsWith("ms") -> clean.removeSuffix("ms").toFloatOrNull()?.toInt() ?: 2000
            clean.endsWith("s") -> ((clean.removeSuffix("s").toFloatOrNull() ?: 2f) * 1000f).toInt()
            else -> clean.toFloatOrNull()?.let { (it * 1000f).toInt() } ?: 2000
        }.coerceAtLeast(100)
    }

    private fun extractEasing(animStr: String, explicitTiming: String?): String {
        val candidate = explicitTiming?.trim()?.uppercase() ?: run {
            val lower = animStr.lowercase()
            when {
                lower.contains("ease-in-out") -> "EASE_IN_OUT"
                lower.contains("ease-in") -> "EASE_IN"
                lower.contains("ease-out") -> "EASE_OUT"
                lower.contains("ease") -> "EASE"
                lower.contains("linear") -> "LINEAR"
                else -> "LINEAR"
            }
        }
        return candidate
    }

    private fun extractAnimationName(animStr: String): String {
        if (animStr.isBlank()) return ""
        val tokens = animStr.trim().split(Pattern.compile("\\s+")).filter { it.isNotBlank() }
        val reservedKeywords = setOf(
            "infinite", "linear", "ease", "ease-in", "ease-out", "ease-in-out",
            "normal", "reverse", "alternate", "alternate-reverse",
            "forwards", "backwards", "both", "none", "running", "paused"
        )
        return tokens.firstOrNull { token ->
            val lower = token.lowercase()
            !reservedKeywords.contains(lower) &&
            !lower.endsWith("s") &&
            !lower.endsWith("ms") &&
            lower.toFloatOrNull() == null
        } ?: ""
    }
}
