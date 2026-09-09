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
            val lenMatcher = Pattern.compile("(-?\\d+(?:\\.\\d+)?)(?:px)?").matcher(cleanLengths)
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
            val lenMatcher = Pattern.compile("(-?\\d+(?:\\.\\d+)?)(?:px)?").matcher(cleanLengths)
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
        val paren = Pattern.compile("(?:rgba?|hsla?)\\([^)]+\\)").matcher(text)
        if (paren.find()) return paren.group(0)
        val hex = Pattern.compile("#[0-9a-fA-F]{3,8}\\b").matcher(text)
        if (hex.find()) return hex.group(0)
        // Check named CSS colors
        val lower = text.lowercase()
        for (name in ColorParser.NAMED_COLORS.keys) {
            val idx = lower.indexOf(name)
            if (idx >= 0) return text.substring(idx, idx + name.length)
        }
        return null
    }
}
