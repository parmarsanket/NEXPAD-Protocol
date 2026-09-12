package com.sanket.tools.nexpad.nxprc.engine.parsers

import com.sanket.tools.nexpad.nxprc.RenderEffectDef

data class ParsedFilter(
    val blurRadiusPx: Float = 0f,
    val brightness: Float = 1.0f,
    val saturate: Float = 1.0f,
    val hueRotateDegrees: Float = 0f,
    val renderEffect: RenderEffectDef = RenderEffectDef()
)

/**
 * Strongly-typed enumeration of supported CSS filter functions.
 */
enum class FilterFunction(val functionName: String) {
    BLUR("blur"),
    BRIGHTNESS("brightness"),
    SATURATE("saturate"),
    HUE_ROTATE("hue-rotate");

    companion object {
        fun fromName(name: String): FilterFunction? = entries.firstOrNull { it.functionName.equals(name, ignoreCase = true) }
    }
}

/**
 * Lightweight, high-performance CSS and SVG filter parser for button styling:
 * - filter: blur(2px)
 * - filter: brightness(1.2) / brightness(120%)
 * - filter: saturate(1.1) / saturate(110%)
 * - filter: hue-rotate(90deg)
 * - filter: url(#filter-id)
 */
object FilterParser {

    private val FUNC_PATTERN = CssSyntaxPattern.FUNCTION_CALL.pattern
    private val NUM_PX_PATTERN = CssSyntaxPattern.NUMBER_PX.pattern
    private val URL_FILTER_REGEX = Regex("""url\(['"]?#?([^'")]+)['"]?\)""")

    fun parse(filterStr: String?, svgFilters: Map<String, ParsedSvgFilter> = emptyMap()): ParsedFilter {
        if (filterStr.isNullOrBlank() || filterStr.trim().equals("none", ignoreCase = true)) {
            return ParsedFilter()
        }

        var blur = 0f
        var brightness = 1.0f
        var saturate = 1.0f
        var hueRotate = 0f
        var effect = RenderEffectDef()

        // 1. Check for SVG url(#filter-id) reference
        val urlMatch = URL_FILTER_REGEX.find(filterStr)
        if (urlMatch != null) {
            val filterId = urlMatch.groupValues[1]
            val svgF = svgFilters[filterId]
            if (svgF != null) {
                blur = maxOf(blur, svgF.filterDef.blurRadius)
                brightness *= svgF.filterDef.brightness
                saturate *= svgF.filterDef.saturation
                hueRotate += svgF.filterDef.hueRotateDegrees
                if (svgF.filterDef.renderEffect.blurRadiusX > 0f || svgF.filterDef.renderEffect.blurRadiusY > 0f) {
                    effect = svgF.filterDef.renderEffect
                }
            }
        }

        // 2. Parse standard CSS filter function calls
        val matcher = FUNC_PATTERN.matcher(filterStr)

        while (matcher.find()) {
            val func = matcher.group(1).lowercase()
            val arg = matcher.group(2).trim()

            when (FilterFunction.fromName(func)) {
                FilterFunction.BLUR -> {
                    val numMatcher = NUM_PX_PATTERN.matcher(arg)
                    if (numMatcher.find()) {
                        blur = numMatcher.group(1).toFloatOrNull() ?: 0f
                    }
                }
                FilterFunction.BRIGHTNESS -> {
                    if (arg.endsWith("%")) {
                        val pct = arg.removeSuffix("%").trim().toFloatOrNull() ?: 100f
                        brightness = pct / 100f
                    } else {
                        brightness = arg.toFloatOrNull() ?: 1.0f
                    }
                }
                FilterFunction.SATURATE -> {
                    if (arg.endsWith("%")) {
                        val pct = arg.removeSuffix("%").trim().toFloatOrNull() ?: 100f
                        saturate = pct / 100f
                    } else {
                        saturate = arg.toFloatOrNull() ?: 1.0f
                    }
                }
                FilterFunction.HUE_ROTATE -> {
                    AngleUnit.parseToDegrees(arg)?.let { hueRotate = it }
                        ?: arg.toFloatOrNull()?.let { hueRotate = it }
                }
                null -> { /* Ignore unhandled filter functions */ }
            }
        }

        return ParsedFilter(
            blurRadiusPx = blur,
            brightness = brightness,
            saturate = saturate,
            hueRotateDegrees = hueRotate,
            renderEffect = effect
        )
    }
}
