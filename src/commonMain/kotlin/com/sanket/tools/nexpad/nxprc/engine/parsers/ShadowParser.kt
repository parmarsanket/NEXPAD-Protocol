package com.sanket.tools.nexpad.nxprc.engine.parsers

import com.sanket.tools.nexpad.nxprc.BoxShadowDef
import com.sanket.tools.nexpad.nxprc.TextShadowDef
import java.util.regex.Pattern

/**
 * Dedicated parser for CSS box-shadows and text-shadows:
 * - Multi-layer comma-separated shadows
 * - Inset vs Outset detection
 * - Offsets (X, Y), blur radius, spread radius
 * - Extruded typography text shadows
 */
object ShadowParser {

    private val PAREN_COLOR_REGEX = Pattern.compile("(?:rgba?|hsla?)\\([^)]+\\)")
    private val HEX_COLOR_REGEX = Pattern.compile("#[0-9a-fA-F]{3,8}\\b")
    private val LENGTH_REGEX = Pattern.compile("(-?\\d+(?:\\.\\d+)?)(?:px)?")
    private val TOKEN_DELIMITER_REGEX = Pattern.compile("[^a-z0-9_-]+")

    fun parseBoxShadows(shadowStr: String?): List<BoxShadowDef> {
        if (shadowStr.isNullOrBlank() || shadowStr.trim().equals("none", ignoreCase = true)) {
            return emptyList()
        }

        val results = mutableListOf<BoxShadowDef>()
        val shadowList = GradientParser.splitTopLevelCommas(shadowStr)

        for (item in shadowList) {
            val isInset = item.contains("inset", ignoreCase = true)
            val withoutInset = item.replace("inset", "", ignoreCase = true).trim()

            val color = ColorParser.extractColorAnywhere(withoutInset) ?: 0x73000000L
            val colorStr = findColorSubstr(withoutInset)
            val cleanLengths = if (colorStr != null) withoutInset.replace(colorStr, "").trim() else withoutInset

            val lengths = mutableListOf<Float>()
            val lenMatcher = LENGTH_REGEX.matcher(cleanLengths)
            while (lenMatcher.find()) {
                lenMatcher.group(1).toFloatOrNull()?.let { lengths.add(it) }
            }

            val offsetX = lengths.getOrNull(0) ?: 0f
            val offsetY = lengths.getOrNull(1) ?: 0f
            val blur = lengths.getOrNull(2) ?: 0f
            val spread = lengths.getOrNull(3) ?: 0f

            results.add(
                BoxShadowDef(
                    offsetX = offsetX,
                    offsetY = offsetY,
                    blurRadius = blur,
                    spreadRadius = spread,
                    color = color,
                    isInset = isInset
                )
            )
        }

        return results
    }

    fun parseTextShadows(shadowStr: String?): List<TextShadowDef> {
        if (shadowStr.isNullOrBlank() || shadowStr.trim().equals("none", ignoreCase = true)) {
            return emptyList()
        }

        val results = mutableListOf<TextShadowDef>()
        val shadowList = GradientParser.splitTopLevelCommas(shadowStr)

        for (item in shadowList) {
            val color = ColorParser.extractColorAnywhere(item) ?: 0x73000000L
            val colorStr = findColorSubstr(item)
            val cleanLengths = if (colorStr != null) item.replace(colorStr, "").trim() else item

            val lengths = mutableListOf<Float>()
            val lenMatcher = LENGTH_REGEX.matcher(cleanLengths)
            while (lenMatcher.find()) {
                lenMatcher.group(1).toFloatOrNull()?.let { lengths.add(it) }
            }

            val offsetX = lengths.getOrNull(0) ?: 0f
            val offsetY = lengths.getOrNull(1) ?: 2f
            val blur = lengths.getOrNull(2) ?: 0f

            results.add(TextShadowDef(offsetX = offsetX, offsetY = offsetY, blurRadius = blur, color = color))
        }

        return results
    }

    private fun findColorSubstr(text: String): String? {
        val paren = PAREN_COLOR_REGEX.matcher(text)
        if (paren.find()) return paren.group(0)
        val hex = HEX_COLOR_REGEX.matcher(text)
        if (hex.find()) return hex.group(0)
        // Check named CSS colors with word boundary to prevent accidental substring matches
        val lower = text.lowercase()
        val tokens = lower.split(TOKEN_DELIMITER_REGEX)
        for (tok in tokens) {
            if (tok.isNotBlank() && ColorParser.NAMED_COLORS.containsKey(tok)) {
                val m = Pattern.compile("\\b" + Pattern.quote(tok) + "\\b", Pattern.CASE_INSENSITIVE).matcher(text)
                if (m.find()) return m.group(0)
            }
        }
        return null
    }
}
