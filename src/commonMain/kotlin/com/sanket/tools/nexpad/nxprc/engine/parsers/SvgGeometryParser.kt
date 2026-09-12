package com.sanket.tools.nexpad.nxprc.engine.parsers

import com.sanket.tools.nexpad.nxprc.FillBrush
import com.sanket.tools.nexpad.nxprc.StrokeStyle
import com.sanket.tools.nexpad.nxprc.engine.dom.DomNode

/**
 * Represents a parsed SVG shape with its geometry path and optional presentation styling.
 */
data class SvgShapeElement(
    val pathData: String,
    val fill: FillBrush? = null,
    val stroke: StrokeStyle? = null
)

/**
 * Dedicated parser for SVG shapes and lines:
 * - <path d="...">
 * - <line x1="..." y1="..." x2="..." y2="...">
 * - <polygon points="...">
 * - <polyline points="...">
 * - <rect x="..." y="..." width="..." height="..." rx="..." ry="...">
 * - <circle cx="..." cy="..." r="...">
 * - <ellipse cx="..." cy="..." rx="..." ry="...">
 */
object SvgGeometryParser {

    fun toPathData(node: DomNode): String? {
        return when (node.tag.lowercase()) {
            "path" -> node.attributes["d"]?.trim()?.takeIf { it.isNotEmpty() }
            "line" -> parseLine(node.attributes)
            "polygon" -> parsePoints(node.attributes["points"], isClosed = true)
            "polyline" -> parsePoints(node.attributes["points"], isClosed = false)
            "rect" -> parseRect(node.attributes)
            "circle" -> parseCircle(node.attributes)
            "ellipse" -> parseEllipse(node.attributes)
            else -> null
        }
    }

    private fun parseLine(attrs: Map<String, String>): String {
        val x1 = attrs["x1"]?.toFloatOrNull() ?: 0f
        val y1 = attrs["y1"]?.toFloatOrNull() ?: 0f
        val x2 = attrs["x2"]?.toFloatOrNull() ?: 0f
        val y2 = attrs["y2"]?.toFloatOrNull() ?: 0f
        return "M $x1 $y1 L $x2 $y2"
    }

    private fun parsePoints(pointsStr: String?, isClosed: Boolean): String? {
        if (pointsStr.isNullOrBlank()) return null
        val tokens = ColorPattern.DELIMITER.split(pointsStr.trim()).filter { it.isNotEmpty() }
        if (tokens.size < 4) return null // At least 2 points (4 coordinates) required

        val sb = StringBuilder()
        var i = 0
        while (i + 1 < tokens.size) {
            val x = tokens[i].toFloatOrNull() ?: break
            val y = tokens[i + 1].toFloatOrNull() ?: break
            if (i == 0) {
                sb.append("M $x $y ")
            } else {
                sb.append("L $x $y ")
            }
            i += 2
        }
        if (isClosed) {
            sb.append("Z")
        }
        return sb.toString().trim()
    }

    private fun parseRect(attrs: Map<String, String>): String? {
        val width = attrs["width"]?.toFloatOrNull() ?: return null
        val height = attrs["height"]?.toFloatOrNull() ?: return null
        val x = attrs["x"]?.toFloatOrNull() ?: 0f
        val y = attrs["y"]?.toFloatOrNull() ?: 0f
        val rx = attrs["rx"]?.toFloatOrNull() ?: 0f
        val ry = attrs["ry"]?.toFloatOrNull() ?: rx

        if (rx > 0f || ry > 0f) {
            val effRx = rx.coerceAtMost(width / 2f)
            val effRy = ry.coerceAtMost(height / 2f)
            return "M ${x + effRx} $y " +
                    "H ${x + width - effRx} " +
                    "A $effRx $effRy 0 0 1 ${x + width} ${y + effRy} " +
                    "V ${y + height - effRy} " +
                    "A $effRx $effRy 0 0 1 ${x + width - effRx} ${y + height} " +
                    "H ${x + effRx} " +
                    "A $effRx $effRy 0 0 1 $x ${y + height - effRy} " +
                    "V ${y + effRy} " +
                    "A $effRx $effRy 0 0 1 ${x + effRx} $y Z"
        }
        return "M $x $y L ${x + width} $y L ${x + width} ${y + height} L $x ${y + height} Z"
    }

    private fun parseCircle(attrs: Map<String, String>): String? {
        val r = attrs["r"]?.toFloatOrNull() ?: return null
        val cx = attrs["cx"]?.toFloatOrNull() ?: 0f
        val cy = attrs["cy"]?.toFloatOrNull() ?: 0f
        return "M ${cx - r} $cy A $r $r 0 1 0 ${cx + r} $cy A $r $r 0 1 0 ${cx - r} $cy Z"
    }

    private fun parseEllipse(attrs: Map<String, String>): String? {
        val rx = attrs["rx"]?.toFloatOrNull() ?: return null
        val ry = attrs["ry"]?.toFloatOrNull() ?: return null
        val cx = attrs["cx"]?.toFloatOrNull() ?: 0f
        val cy = attrs["cy"]?.toFloatOrNull() ?: 0f
        return "M ${cx - rx} $cy A $rx $ry 0 1 0 ${cx + rx} $cy A $rx $ry 0 1 0 ${cx - rx} $cy Z"
    }

    fun parseFill(node: DomNode): FillBrush? {
        val fillStr = node.attributes["fill"] ?: node.inlineStyles["fill"]
        if (fillStr != null) {
            val clean = fillStr.trim().lowercase()
            if (clean == "none" || clean == "transparent") {
                return FillBrush.Solid(0x00000000L)
            }
            val color = ColorParser.parse(clean)
            if (color != null) return FillBrush.Solid(color)
        }

        // Standard SVG default for stroke-only elements (line, polyline) is transparent fill
        val tag = node.tag.lowercase()
        if (tag == "line" || tag == "polyline") {
            return FillBrush.Solid(0x00000000L)
        }

        return null
    }

    fun parseStroke(node: DomNode): StrokeStyle? {
        val strokeStr = node.attributes["stroke"] ?: node.inlineStyles["stroke"] ?: return null
        val clean = strokeStr.trim().lowercase()
        if (clean == "none" || clean == "transparent") return null

        val color = ColorParser.parse(clean) ?: return null
        val widthStr = node.attributes["stroke-width"] ?: node.inlineStyles["stroke-width"]
        val width = widthStr?.let {
            val m = CssSyntaxPattern.LENGTH.matcher(it)
            if (m.find()) m.group(1).toFloatOrNull() else it.toFloatOrNull()
        } ?: 1.0f

        val dashStr = node.attributes["stroke-dasharray"] ?: node.inlineStyles["stroke-dasharray"]
        val isDashed = !dashStr.isNullOrBlank()

        return StrokeStyle(
            color = color,
            width = width,
            isDashed = isDashed,
            dashWidth = if (isDashed) width * 3f else 0f,
            dashGap = if (isDashed) width * 2f else 0f
        )
    }
}
