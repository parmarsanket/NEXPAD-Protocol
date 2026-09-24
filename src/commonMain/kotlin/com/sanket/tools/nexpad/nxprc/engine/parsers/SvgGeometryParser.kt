package com.sanket.tools.nexpad.nxprc.engine.parsers

import com.sanket.tools.nexpad.nxprc.FillBrush
import com.sanket.tools.nexpad.nxprc.StrokeStyle
import com.sanket.tools.nexpad.nxprc.engine.css.CssCascadeResolver
import com.sanket.tools.nexpad.nxprc.engine.css.CssStylesheet
import com.sanket.tools.nexpad.nxprc.engine.dom.DomNode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

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

    private val URL_REF_REGEX = Regex("""url\(\s*['"]?#([^'")]+)['"]?\s*\)""")

    fun applyAlpha(color: Long, alpha: Float): Long {
        val a = ((color ushr 24 and 0xFF) * alpha.coerceIn(0f, 1f)).roundToInt().coerceIn(0, 255)
        return (a.toLong() shl 24) or (color and 0x00FFFFFFL)
    }

    private fun parseOffset(str: String): Float {
        val trimmed = str.trim()
        return if (trimmed.endsWith("%")) {
            ((trimmed.removeSuffix("%").toFloatOrNull() ?: 0f) / 100f).coerceIn(0f, 1f)
        } else {
            (trimmed.toFloatOrNull() ?: 0f).coerceIn(0f, 1f)
        }
    }

    private fun parseOpacity(str: String): Float {
        val trimmed = str.trim()
        return if (trimmed.endsWith("%")) {
            ((trimmed.removeSuffix("%").toFloatOrNull() ?: 100f) / 100f).coerceIn(0f, 1f)
        } else {
            (trimmed.toFloatOrNull() ?: 1.0f).coerceIn(0f, 1f)
        }
    }

    private fun parseCoord(str: String?): Float? {
        if (str == null) return null
        val trimmed = str.trim()
        return if (trimmed.endsWith("%")) {
            (trimmed.removeSuffix("%").toFloatOrNull() ?: 0f) / 100f
        } else {
            trimmed.toFloatOrNull()
        }
    }

    fun extractPaintServers(root: DomNode, stylesheet: CssStylesheet? = null): Map<String, FillBrush> {
        val paintServers = mutableMapOf<String, FillBrush>()
        val actualRoot = root.findRoot()
        val gradientNodes = actualRoot.findByTag("lineargradient") + actualRoot.findByTag("radialgradient")

        for (gNode in gradientNodes) {
            val id = gNode.attributes["id"]?.trim()?.removePrefix("#") ?: continue
            val brush = parseGradientElement(gNode, actualRoot, stylesheet)
            if (brush != null) {
                paintServers[id] = brush
            }
        }
        return paintServers
    }

    private fun parseGradientElement(
        node: DomNode,
        root: DomNode,
        stylesheet: CssStylesheet?
    ): FillBrush? {
        var stopNodes = node.findByTag("stop")
        if (stopNodes.isEmpty()) {
            val href = (node.attributes["href"] ?: node.attributes["xlink:href"])?.trim()?.removePrefix("#")
            if (href != null) {
                val refNode = root.findFirst { it.attributes["id"] == href }
                if (refNode != null) {
                    stopNodes = refNode.findByTag("stop")
                }
            }
        }

        val stopsList = mutableListOf<Pair<Float, Long>>()
        for (stopNode in stopNodes) {
            val offsetStr = stopNode.attributes["offset"] ?: stopNode.inlineStyles["offset"] ?: "0"
            val offset = parseOffset(offsetStr)

            val colorStr = stopNode.attributes["stop-color"]
                ?: stopNode.inlineStyles["stop-color"]
                ?: (if (stylesheet != null) CssCascadeResolver.computeStyle(stopNode, stylesheet).base["stop-color"] else null)
                ?: stopNode.attributes["fill"]
                ?: "#000000"

            val baseColor = ColorParser.parse(colorStr) ?: 0xFF000000L

            val opacityStr = stopNode.attributes["stop-opacity"]
                ?: stopNode.inlineStyles["stop-opacity"]
                ?: (if (stylesheet != null) CssCascadeResolver.computeStyle(stopNode, stylesheet).base["stop-opacity"] else null)

            val opacity = opacityStr?.let { parseOpacity(it) } ?: 1.0f
            val finalColor = applyAlpha(baseColor, opacity)
            stopsList.add(offset to finalColor)
        }

        val sortedStops = if (stopsList.size >= 2) {
            stopsList.sortedBy { it.first }
        } else if (stopsList.size == 1) {
            listOf(0f to stopsList[0].second, 1f to stopsList[0].second)
        } else {
            listOf(0f to 0xFF000000L, 1f to 0xFFFFFFFFL)
        }

        val colors = sortedStops.map { it.second }
        val stopOffsets = sortedStops.map { it.first }

        val isLinear = node.tag.equals("lineargradient", ignoreCase = true)
        return if (isLinear) {
            val x1 = parseCoord(node.attributes["x1"]) ?: 0f
            val y1 = parseCoord(node.attributes["y1"]) ?: 0f
            val x2 = parseCoord(node.attributes["x2"]) ?: 1f
            val y2 = parseCoord(node.attributes["y2"]) ?: 0f

            val dx = x2 - x1
            val dy = y2 - y1
            var angleDeg = if (dx == 0f && dy == 0f) {
                90f
            } else {
                ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0) % 360.0).toFloat().let {
                    if (it < 0f) it + 360f else it
                }
            }

            val transform = node.attributes["gradientTransform"]
            if (transform != null) {
                val rotateMatch = Regex("""rotate\(\s*(-?\d+(?:\.\d+)?)\s*\)""").find(transform)
                if (rotateMatch != null) {
                    val rDeg = rotateMatch.groupValues[1].toFloatOrNull() ?: 0f
                    angleDeg = (angleDeg + rDeg) % 360f
                    if (angleDeg < 0f) angleDeg += 360f
                }
            }

            FillBrush.LinearGradient(
                colors = colors,
                angleDegrees = angleDeg,
                stops = stopOffsets
            )
        } else {
            val cx = parseCoord(node.attributes["cx"]) ?: 0.5f
            val cy = parseCoord(node.attributes["cy"]) ?: 0.5f
            val r = parseCoord(node.attributes["r"]) ?: 0.5f

            FillBrush.RadialGradient(
                colors = colors,
                radiusRatio = r,
                centerXRatio = cx,
                centerYRatio = cy,
                stops = stopOffsets,
                aspectRatio = 1.0f
            )
        }
    }

    fun findSvgInheritedAttr(node: DomNode, attr: String, stylesheet: CssStylesheet? = null): String? {
        var curr: DomNode? = node
        while (curr != null) {
            val tag = curr.tag.lowercase()
            if (tag !in listOf("path", "circle", "rect", "line", "polyline", "polygon", "ellipse", "g", "svg", "text", "tspan", "defs")) break
            val computed = if (stylesheet != null) CssCascadeResolver.computeStyle(curr, stylesheet).base else emptyMap()
            val v = curr.inlineStyles[attr] ?: computed[attr] ?: curr.attributes[attr]
            if (!v.isNullOrBlank()) return v
            curr = curr.parent
        }
        return null
    }

    fun computeSvgCumulativeOpacity(node: DomNode, stylesheet: CssStylesheet? = null): Float {
        var curr: DomNode? = node
        var mult = 1.0f
        while (curr != null) {
            val tag = curr.tag.lowercase()
            if (tag !in listOf("path", "circle", "rect", "line", "polyline", "polygon", "ellipse", "g", "svg", "text", "tspan", "defs")) break
            val computed = if (stylesheet != null) CssCascadeResolver.computeStyle(curr, stylesheet).base else emptyMap()
            val opStr = curr.attributes["opacity"] ?: curr.inlineStyles["opacity"] ?: computed["opacity"]
            if (!opStr.isNullOrBlank()) {
                mult *= parseOpacity(opStr)
            }
            curr = curr.parent
        }
        return mult
    }

    fun parseFill(
        node: DomNode,
        stylesheet: CssStylesheet? = null,
        paintServers: Map<String, FillBrush> = emptyMap()
    ): FillBrush? {
        val fillStr = findSvgInheritedAttr(node, "fill", stylesheet)
        var brush: FillBrush? = null

        if (fillStr != null) {
            val clean = fillStr.trim()
            val cleanLower = clean.lowercase()
            if (cleanLower == "none" || cleanLower == "transparent") {
                return FillBrush.Solid(0x00000000L)
            }
            val urlMatch = URL_REF_REGEX.find(clean)
            if (urlMatch != null) {
                val refId = urlMatch.groupValues[1].trim()
                brush = paintServers[refId]
            }
            if (brush == null) {
                val color = ColorParser.parse(cleanLower)
                if (color != null) brush = FillBrush.Solid(color)
            }
        }

        // Standard SVG default for stroke-only elements (line, polyline) is transparent fill
        val tag = node.tag.lowercase()
        if (brush == null && (tag == "line" || tag == "polyline")) {
            return FillBrush.Solid(0x00000000L)
        }

        if (brush == null) return null

        // Modulate cumulative opacity and fill-opacity if present
        val totalOpacity = computeSvgCumulativeOpacity(node, stylesheet)
        val fillOpacityStr = findSvgInheritedAttr(node, "fill-opacity", stylesheet)
        val fOp = fillOpacityStr?.let { parseOpacity(it) } ?: 1.0f
        val effectiveOpacity = totalOpacity * fOp

        return if (effectiveOpacity < 1.0f) {
            when (brush) {
                is FillBrush.Solid -> brush.copy(color = applyAlpha(brush.color, effectiveOpacity))
                is FillBrush.LinearGradient -> brush.copy(colors = brush.colors.map { applyAlpha(it, effectiveOpacity) })
                is FillBrush.RadialGradient -> brush.copy(colors = brush.colors.map { applyAlpha(it, effectiveOpacity) })
                is FillBrush.SweepGradient -> brush.copy(colors = brush.colors.map { applyAlpha(it, effectiveOpacity) })
            }
        } else {
            brush
        }
    }

    fun parseStroke(
        node: DomNode,
        stylesheet: CssStylesheet? = null,
        paintServers: Map<String, FillBrush> = emptyMap()
    ): StrokeStyle? {
        val strokeStr = findSvgInheritedAttr(node, "stroke", stylesheet) ?: return null
        val clean = strokeStr.trim()
        val cleanLower = clean.lowercase()
        if (cleanLower == "none" || cleanLower == "transparent") return null

        var color: Long? = null
        val urlMatch = URL_REF_REGEX.find(clean)
        if (urlMatch != null) {
            val refId = urlMatch.groupValues[1].trim()
            val server = paintServers[refId]
            if (server != null) {
                color = when (server) {
                    is FillBrush.Solid -> server.color
                    is FillBrush.LinearGradient -> server.colors.firstOrNull()
                    is FillBrush.RadialGradient -> server.colors.firstOrNull()
                    is FillBrush.SweepGradient -> server.colors.firstOrNull()
                }
            }
        }
        if (color == null) {
            color = ColorParser.parse(cleanLower) ?: return null
        }

        // Apply stroke-opacity and cumulative opacity
        val totalOpacity = computeSvgCumulativeOpacity(node, stylesheet)
        val strokeOpacityStr = findSvgInheritedAttr(node, "stroke-opacity", stylesheet)
        val sOp = strokeOpacityStr?.let { parseOpacity(it) } ?: 1.0f
        val effectiveOpacity = totalOpacity * sOp
        val finalColor = if (effectiveOpacity < 1.0f) applyAlpha(color, effectiveOpacity) else color

        val widthStr = findSvgInheritedAttr(node, "stroke-width", stylesheet)
        val width = widthStr?.let {
            val m = CssSyntaxPattern.LENGTH.matcher(it)
            if (m.find()) m.group(1).toFloatOrNull() else it.toFloatOrNull()
        } ?: 1.0f

        val dashStr = findSvgInheritedAttr(node, "stroke-dasharray", stylesheet)
        val isDashed = !dashStr.isNullOrBlank()

        return StrokeStyle(
            color = finalColor,
            width = width,
            isDashed = isDashed,
            dashWidth = if (isDashed) width * 3f else 0f,
            dashGap = if (isDashed) width * 2f else 0f
        )
    }

    private fun fmt(v: Float): String {
        val rounded = (v * 1000f).roundToInt() / 1000f
        val safeVal = if (rounded == 0f) 0f else rounded
        return if (safeVal == safeVal.toLong().toFloat()) {
            safeVal.toLong().toString()
        } else {
            safeVal.toString()
        }
    }

    fun tokenizePath(d: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        val len = d.length
        while (i < len) {
            val c = d[i]
            if (c.isWhitespace() || c == ',') {
                i++
                continue
            }
            if (c in "MmLlHhVvCcSsQqTtAaZz") {
                tokens.add(c.toString())
                i++
                continue
            }
            val start = i
            if (c == '+' || c == '-') i++
            var hasDot = false
            var hasExponent = false
            while (i < len) {
                val ch = d[i]
                if (ch.isDigit()) {
                    i++
                } else if (ch == '.' && !hasDot && !hasExponent) {
                    hasDot = true
                    i++
                } else if ((ch == 'e' || ch == 'E') && !hasExponent) {
                    hasExponent = true
                    i++
                    if (i < len && (d[i] == '+' || d[i] == '-')) i++
                } else {
                    break
                }
            }
            if (start < i) {
                tokens.add(d.substring(start, i))
            } else {
                i++
            }
        }
        return tokens
    }

    fun transformPathData(d: String, matrix: AffineMatrix2D): String {
        if (matrix.isIdentity || d.isBlank()) return d.trim()

        val tokens = tokenizePath(d)
        if (tokens.isEmpty()) return d.trim()

        val sb = StringBuilder()
        var i = 0
        var currX = 0f
        var currY = 0f
        var startX = 0f
        var startY = 0f
        var lastCtrlX = 0f
        var lastCtrlY = 0f
        var lastCmd = ' '

        while (i < tokens.size) {
            val token = tokens[i]
            val cmdChar = token[0]
            if (token.length == 1 && cmdChar.isLetter()) {
                lastCmd = cmdChar
                i++
            } else if (lastCmd == ' ') {
                i++
                continue
            }

            when (lastCmd) {
                'M' -> {
                    val x = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    val y = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: break
                    i += 2
                    currX = x
                    currY = y
                    startX = x
                    startY = y
                    lastCtrlX = x
                    lastCtrlY = y
                    val tx = matrix.transformX(x, y)
                    val ty = matrix.transformY(x, y)
                    sb.append("M ").append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                    lastCmd = 'L'
                }
                'm' -> {
                    val dx = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    val dy = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: break
                    i += 2
                    currX += dx
                    currY += dy
                    startX = currX
                    startY = currY
                    lastCtrlX = currX
                    lastCtrlY = currY
                    val tx = matrix.transformX(currX, currY)
                    val ty = matrix.transformY(currX, currY)
                    sb.append("M ").append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                    lastCmd = 'l'
                }
                'L' -> {
                    val x = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    val y = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: break
                    i += 2
                    currX = x
                    currY = y
                    lastCtrlX = x
                    lastCtrlY = y
                    val tx = matrix.transformX(x, y)
                    val ty = matrix.transformY(x, y)
                    sb.append("L ").append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                }
                'l' -> {
                    val dx = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    val dy = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: break
                    i += 2
                    currX += dx
                    currY += dy
                    lastCtrlX = currX
                    lastCtrlY = currY
                    val tx = matrix.transformX(currX, currY)
                    val ty = matrix.transformY(currX, currY)
                    sb.append("L ").append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                }
                'H' -> {
                    val x = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    i += 1
                    currX = x
                    lastCtrlX = x
                    lastCtrlY = currY
                    val tx = matrix.transformX(currX, currY)
                    val ty = matrix.transformY(currX, currY)
                    sb.append("L ").append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                }
                'h' -> {
                    val dx = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    i += 1
                    currX += dx
                    lastCtrlX = currX
                    lastCtrlY = currY
                    val tx = matrix.transformX(currX, currY)
                    val ty = matrix.transformY(currX, currY)
                    sb.append("L ").append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                }
                'V' -> {
                    val y = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    i += 1
                    currY = y
                    lastCtrlX = currX
                    lastCtrlY = y
                    val tx = matrix.transformX(currX, currY)
                    val ty = matrix.transformY(currX, currY)
                    sb.append("L ").append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                }
                'v' -> {
                    val dy = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    i += 1
                    currY += dy
                    lastCtrlX = currX
                    lastCtrlY = currY
                    val tx = matrix.transformX(currX, currY)
                    val ty = matrix.transformY(currX, currY)
                    sb.append("L ").append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                }
                'C' -> {
                    val x1 = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    val y1 = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: break
                    val x2 = tokens.getOrNull(i + 2)?.toFloatOrNull() ?: break
                    val y2 = tokens.getOrNull(i + 3)?.toFloatOrNull() ?: break
                    val x = tokens.getOrNull(i + 4)?.toFloatOrNull() ?: break
                    val y = tokens.getOrNull(i + 5)?.toFloatOrNull() ?: break
                    i += 6
                    currX = x
                    currY = y
                    lastCtrlX = x2
                    lastCtrlY = y2
                    val tx1 = matrix.transformX(x1, y1)
                    val ty1 = matrix.transformY(x1, y1)
                    val tx2 = matrix.transformX(x2, y2)
                    val ty2 = matrix.transformY(x2, y2)
                    val tx = matrix.transformX(x, y)
                    val ty = matrix.transformY(x, y)
                    sb.append("C ").append(fmt(tx1)).append(" ").append(fmt(ty1)).append(" ")
                        .append(fmt(tx2)).append(" ").append(fmt(ty2)).append(" ")
                        .append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                }
                'c' -> {
                    val dx1 = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    val dy1 = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: break
                    val dx2 = tokens.getOrNull(i + 2)?.toFloatOrNull() ?: break
                    val dy2 = tokens.getOrNull(i + 3)?.toFloatOrNull() ?: break
                    val dx = tokens.getOrNull(i + 4)?.toFloatOrNull() ?: break
                    val dy = tokens.getOrNull(i + 5)?.toFloatOrNull() ?: break
                    i += 6
                    val x1 = currX + dx1
                    val y1 = currY + dy1
                    val x2 = currX + dx2
                    val y2 = currY + dy2
                    currX += dx
                    currY += dy
                    lastCtrlX = x2
                    lastCtrlY = y2
                    val tx1 = matrix.transformX(x1, y1)
                    val ty1 = matrix.transformY(x1, y1)
                    val tx2 = matrix.transformX(x2, y2)
                    val ty2 = matrix.transformY(x2, y2)
                    val tx = matrix.transformX(currX, currY)
                    val ty = matrix.transformY(currX, currY)
                    sb.append("C ").append(fmt(tx1)).append(" ").append(fmt(ty1)).append(" ")
                        .append(fmt(tx2)).append(" ").append(fmt(ty2)).append(" ")
                        .append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                }
                'S' -> {
                    val x2 = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    val y2 = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: break
                    val x = tokens.getOrNull(i + 2)?.toFloatOrNull() ?: break
                    val y = tokens.getOrNull(i + 3)?.toFloatOrNull() ?: break
                    i += 4
                    val x1 = 2f * currX - lastCtrlX
                    val y1 = 2f * currY - lastCtrlY
                    currX = x
                    currY = y
                    lastCtrlX = x2
                    lastCtrlY = y2
                    val tx1 = matrix.transformX(x1, y1)
                    val ty1 = matrix.transformY(x1, y1)
                    val tx2 = matrix.transformX(x2, y2)
                    val ty2 = matrix.transformY(x2, y2)
                    val tx = matrix.transformX(x, y)
                    val ty = matrix.transformY(x, y)
                    sb.append("C ").append(fmt(tx1)).append(" ").append(fmt(ty1)).append(" ")
                        .append(fmt(tx2)).append(" ").append(fmt(ty2)).append(" ")
                        .append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                }
                's' -> {
                    val dx2 = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    val dy2 = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: break
                    val dx = tokens.getOrNull(i + 2)?.toFloatOrNull() ?: break
                    val dy = tokens.getOrNull(i + 3)?.toFloatOrNull() ?: break
                    i += 4
                    val x1 = 2f * currX - lastCtrlX
                    val y1 = 2f * currY - lastCtrlY
                    val x2 = currX + dx2
                    val y2 = currY + dy2
                    currX += dx
                    currY += dy
                    lastCtrlX = x2
                    lastCtrlY = y2
                    val tx1 = matrix.transformX(x1, y1)
                    val ty1 = matrix.transformY(x1, y1)
                    val tx2 = matrix.transformX(x2, y2)
                    val ty2 = matrix.transformY(x2, y2)
                    val tx = matrix.transformX(currX, currY)
                    val ty = matrix.transformY(currX, currY)
                    sb.append("C ").append(fmt(tx1)).append(" ").append(fmt(ty1)).append(" ")
                        .append(fmt(tx2)).append(" ").append(fmt(ty2)).append(" ")
                        .append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                }
                'Q' -> {
                    val x1 = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    val y1 = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: break
                    val x = tokens.getOrNull(i + 2)?.toFloatOrNull() ?: break
                    val y = tokens.getOrNull(i + 3)?.toFloatOrNull() ?: break
                    i += 4
                    currX = x
                    currY = y
                    lastCtrlX = x1
                    lastCtrlY = y1
                    val tx1 = matrix.transformX(x1, y1)
                    val ty1 = matrix.transformY(x1, y1)
                    val tx = matrix.transformX(x, y)
                    val ty = matrix.transformY(x, y)
                    sb.append("Q ").append(fmt(tx1)).append(" ").append(fmt(ty1)).append(" ")
                        .append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                }
                'q' -> {
                    val dx1 = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    val dy1 = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: break
                    val dx = tokens.getOrNull(i + 2)?.toFloatOrNull() ?: break
                    val dy = tokens.getOrNull(i + 3)?.toFloatOrNull() ?: break
                    i += 4
                    val x1 = currX + dx1
                    val y1 = currY + dy1
                    currX += dx
                    currY += dy
                    lastCtrlX = x1
                    lastCtrlY = y1
                    val tx1 = matrix.transformX(x1, y1)
                    val ty1 = matrix.transformY(x1, y1)
                    val tx = matrix.transformX(currX, currY)
                    val ty = matrix.transformY(currX, currY)
                    sb.append("Q ").append(fmt(tx1)).append(" ").append(fmt(ty1)).append(" ")
                        .append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                }
                'T' -> {
                    val x = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    val y = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: break
                    i += 2
                    val x1 = 2f * currX - lastCtrlX
                    val y1 = 2f * currY - lastCtrlY
                    currX = x
                    currY = y
                    lastCtrlX = x1
                    lastCtrlY = y1
                    val tx1 = matrix.transformX(x1, y1)
                    val ty1 = matrix.transformY(x1, y1)
                    val tx = matrix.transformX(x, y)
                    val ty = matrix.transformY(x, y)
                    sb.append("Q ").append(fmt(tx1)).append(" ").append(fmt(ty1)).append(" ")
                        .append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                }
                't' -> {
                    val dx = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    val dy = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: break
                    i += 2
                    val x1 = 2f * currX - lastCtrlX
                    val y1 = 2f * currY - lastCtrlY
                    currX += dx
                    currY += dy
                    lastCtrlX = x1
                    lastCtrlY = y1
                    val tx1 = matrix.transformX(x1, y1)
                    val ty1 = matrix.transformY(x1, y1)
                    val tx = matrix.transformX(currX, currY)
                    val ty = matrix.transformY(currX, currY)
                    sb.append("Q ").append(fmt(tx1)).append(" ").append(fmt(ty1)).append(" ")
                        .append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                }
                'A', 'a' -> {
                    val rx = tokens.getOrNull(i)?.toFloatOrNull() ?: break
                    val ry = tokens.getOrNull(i + 1)?.toFloatOrNull() ?: break
                    val rot = tokens.getOrNull(i + 2)?.toFloatOrNull() ?: break
                    val largeArc = tokens.getOrNull(i + 3)?.toIntOrNull() ?: break
                    var sweep = tokens.getOrNull(i + 4)?.toIntOrNull() ?: break
                    val endX = tokens.getOrNull(i + 5)?.toFloatOrNull() ?: break
                    val endY = tokens.getOrNull(i + 6)?.toFloatOrNull() ?: break
                    i += 7

                    val targetX = if (lastCmd == 'a') currX + endX else endX
                    val targetY = if (lastCmd == 'a') currY + endY else endY
                    currX = targetX
                    currY = targetY
                    lastCtrlX = targetX
                    lastCtrlY = targetY

                    val det = matrix.a * matrix.d - matrix.b * matrix.c
                    val scaleFactor = sqrt(abs(det)).coerceAtLeast(0.001f)
                    val newRx = rx * scaleFactor
                    val newRy = ry * scaleFactor
                    if (det < 0) {
                        sweep = if (sweep == 0) 1 else 0
                    }
                    val rotDelta = (atan2(matrix.b.toDouble(), matrix.a.toDouble()) * (180.0 / PI)).toFloat()
                    val newRot = (rot + rotDelta) % 360f

                    val tx = matrix.transformX(targetX, targetY)
                    val ty = matrix.transformY(targetX, targetY)
                    sb.append("A ").append(fmt(newRx)).append(" ").append(fmt(newRy)).append(" ")
                        .append(fmt(newRot)).append(" ").append(largeArc).append(" ").append(sweep).append(" ")
                        .append(fmt(tx)).append(" ").append(fmt(ty)).append(" ")
                }
                'Z', 'z' -> {
                    currX = startX
                    currY = startY
                    lastCtrlX = startX
                    lastCtrlY = startY
                    sb.append("Z ")
                    lastCmd = ' '
                }
                else -> i++
            }
        }

        return sb.toString().trim()
    }
}
