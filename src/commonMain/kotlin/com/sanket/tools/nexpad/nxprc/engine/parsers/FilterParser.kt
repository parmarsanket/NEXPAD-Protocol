package com.sanket.tools.nexpad.nxprc.engine.parsers

data class ParsedFilter(
    val blurRadiusPx: Float = 0f,
    val brightness: Float = 1.0f,
    val saturate: Float = 1.0f
)

/**
 * Strongly-typed enumeration of supported CSS filter functions.
 */
enum class FilterFunction(val functionName: String) {
    BLUR("blur"),
    BRIGHTNESS("brightness"),
    SATURATE("saturate");

    companion object {
        fun fromName(name: String): FilterFunction? = entries.firstOrNull { it.functionName.equals(name, ignoreCase = true) }
    }
}

/**
 * Lightweight, high-performance CSS filter parser for button styling:
 * - filter: blur(2px)
 * - filter: brightness(1.2) / brightness(120%)
 * - filter: saturate(1.1) / saturate(110%)
 */
object FilterParser {

    private val FUNC_PATTERN = CssSyntaxPattern.FUNCTION_CALL.pattern
    private val NUM_PX_PATTERN = CssSyntaxPattern.NUMBER_PX.pattern

    fun parse(filterStr: String?): ParsedFilter {
        if (filterStr.isNullOrBlank() || filterStr.trim().equals("none", ignoreCase = true)) {
            return ParsedFilter()
        }

        var blur = 0f
        var brightness = 1.0f
        var saturate = 1.0f

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
                null -> { /* Ignore unhandled filter functions */ }
            }
        }

        return ParsedFilter(
            blurRadiusPx = blur,
            brightness = brightness,
            saturate = saturate
        )
    }
}
