package com.sanket.tools.nexpad.nxprc.engine.parsers

import java.util.regex.Pattern

data class ParsedFilter(
    val blurRadiusPx: Float = 0f,
    val brightness: Float = 1.0f,
    val saturate: Float = 1.0f
)

/**
 * Lightweight, high-performance CSS filter parser for button styling:
 * - filter: blur(2px)
 * - filter: brightness(1.2) / brightness(120%)
 * - filter: saturate(1.1) / saturate(110%)
 */
object FilterParser {

    private val FUNC_PATTERN = Pattern.compile("([a-zA-Z0-9_-]+)\\s*\\(([^)]+)\\)")
    private val NUM_PX_PATTERN = Pattern.compile("([0-9.]+)\\s*(px)?")

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

            when (func) {
                "blur" -> {
                    val numMatcher = NUM_PX_PATTERN.matcher(arg)
                    if (numMatcher.find()) {
                        blur = numMatcher.group(1).toFloatOrNull() ?: 0f
                    }
                }
                "brightness" -> {
                    if (arg.endsWith("%")) {
                        val pct = arg.removeSuffix("%").trim().toFloatOrNull() ?: 100f
                        brightness = pct / 100f
                    } else {
                        brightness = arg.toFloatOrNull() ?: 1.0f
                    }
                }
                "saturate" -> {
                    if (arg.endsWith("%")) {
                        val pct = arg.removeSuffix("%").trim().toFloatOrNull() ?: 100f
                        saturate = pct / 100f
                    } else {
                        saturate = arg.toFloatOrNull() ?: 1.0f
                    }
                }
            }
        }

        return ParsedFilter(
            blurRadiusPx = blur,
            brightness = brightness,
            saturate = saturate
        )
    }
}
