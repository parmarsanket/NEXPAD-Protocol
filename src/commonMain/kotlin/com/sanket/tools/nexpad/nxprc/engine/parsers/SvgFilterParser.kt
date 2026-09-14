package com.sanket.tools.nexpad.nxprc.engine.parsers

import com.sanket.tools.nexpad.nxprc.BoxShadowDef
import com.sanket.tools.nexpad.nxprc.FilterDef
import com.sanket.tools.nexpad.nxprc.RenderEffectDef
import com.sanket.tools.nexpad.nxprc.engine.dom.DomNode

/**
 * Result of compiling an SVG `<filter>` element graph.
 */
data class ParsedSvgFilter(
    val id: String,
    val filterDef: FilterDef = FilterDef(),
    val dropShadows: List<BoxShadowDef> = emptyList()
)

/**
 * Dedicated parser for SVG `<filter>` multi-node graphs.
 * Parses:
 * - `<feGaussianBlur stdDeviation="...">`
 * - `<feColorMatrix type="..." values="...">`
 * - `<feDropShadow dx="..." dy="..." stdDeviation="..." flood-color="..." flood-opacity="...">`
 * - `<feComponentTransfer>` / `<feFuncR slope="...">`
 * - `<feBlend mode="...">`
 */
object SvgFilterParser {

    private val DELIMITER = Regex("[,\\s]+")

    /**
     * Scans [root] DOM tree recursively for all `<filter>` nodes and compiles them into a dictionary keyed by ID.
     */
    fun parseFilterMap(root: DomNode): Map<String, ParsedSvgFilter> {
        val filters = root.findByTag("filter")
        if (filters.isEmpty()) return emptyMap()

        val map = mutableMapOf<String, ParsedSvgFilter>()
        for (fNode in filters) {
            val rawId = fNode.attributes["id"]?.trim() ?: continue
            val id = rawId.removePrefix("#")
            map[id] = parseFilterNode(id, fNode)
        }
        return map
    }

    private fun parseFilterNode(id: String, filterNode: DomNode): ParsedSvgFilter {
        var blurX = 0f
        var blurY = 0f
        var brightness = 1.0f
        var saturation = 1.0f
        var hueRotate = 0f
        val shadows = mutableListOf<BoxShadowDef>()

        for (child in filterNode.children) {
            val tag = child.tag.lowercase()
            when (tag) {
                "fegaussianblur" -> {
                    val stdDev = child.attributes["stddeviation"] ?: child.attributes["stdDeviation"] ?: ""
                    val tokens = stdDev.trim().split(DELIMITER).filter { it.isNotEmpty() }
                    if (tokens.isNotEmpty()) {
                        blurX = tokens[0].toFloatOrNull() ?: 0f
                        blurY = if (tokens.size > 1) tokens[1].toFloatOrNull() ?: blurX else blurX
                    }
                }
                "fecolormatrix" -> {
                    val type = (child.attributes["type"] ?: "matrix").lowercase()
                    val values = child.attributes["values"]?.trim() ?: ""
                    when (type) {
                        "saturate" -> {
                            values.toFloatOrNull()?.let { saturation = it }
                        }
                        "huerotate" -> {
                            AngleUnit.parseToDegrees(values)?.let { hueRotate = it }
                                ?: values.toFloatOrNull()?.let { hueRotate = it }
                        }
                        "matrix" -> {
                            val tokens = values.split(DELIMITER).mapNotNull { it.toFloatOrNull() }
                            // If 20 values matrix: [r0..r4, g0..g4, b0..b4, a0..a4]
                            if (tokens.size >= 19) {
                                val rMult = tokens[0]
                                val gMult = tokens[6]
                                val bMult = tokens[12]
                                val avg = (rMult + gMult + bMult) / 3f
                                if (avg > 0f) brightness = avg
                            }
                        }
                    }
                }
                "fedropshadow" -> {
                    val dx = child.attributes["dx"]?.toFloatOrNull() ?: 0f
                    val dy = child.attributes["dy"]?.toFloatOrNull() ?: 0f
                    val stdDev = child.attributes["stddeviation"] ?: child.attributes["stdDeviation"] ?: ""
                    val blur = stdDev.trim().split(DELIMITER).firstOrNull()?.toFloatOrNull() ?: 0f

                    val floodColorStr = child.attributes["flood-color"] ?: child.attributes["floodColor"] ?: "#000000"
                    val floodOpacityStr = child.attributes["flood-opacity"] ?: child.attributes["floodOpacity"] ?: "1.0"

                    val baseColor = ColorParser.parse(floodColorStr) ?: 0xFF000000L
                    val opacity = floodOpacityStr.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1.0f
                    val alpha = (opacity * 255f).toInt().coerceIn(0, 255)
                    val finalColor = (alpha.toLong() shl 24) or (baseColor and 0x00FFFFFFL)

                    shadows.add(
                        BoxShadowDef(
                            offsetX = dx,
                            offsetY = dy,
                            blurRadius = blur,
                            spreadRadius = 0f,
                            color = finalColor,
                            isInset = false
                        )
                    )
                }
                "fecomponenttransfer" -> {
                    for (fn in child.children) {
                        val slope = fn.attributes["slope"]?.toFloatOrNull()
                        if (slope != null) {
                            brightness = slope
                        }
                    }
                }
            }
        }

        val avgBlur = if (blurX > 0f || blurY > 0f) (blurX + blurY) / 2f else 0f
        val renderEffect = if (blurX > 0f || blurY > 0f) {
            RenderEffectDef(blurRadiusX = blurX, blurRadiusY = blurY, tileMode = "CLAMP")
        } else {
            RenderEffectDef()
        }

        return ParsedSvgFilter(
            id = id,
            filterDef = FilterDef(
                blurRadius = avgBlur,
                brightness = brightness,
                saturation = saturation,
                hueRotateDegrees = hueRotate,
                renderEffect = renderEffect
            ),
            dropShadows = shadows
        )
    }
}
