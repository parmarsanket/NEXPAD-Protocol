package com.sanket.tools.nexpad.nxprc.engine.parsers

import com.sanket.tools.nexpad.nxprc.LayerShapeType
import com.sanket.tools.nexpad.nxprc.StrokeStyle


/**
 * Dedicated parser for CSS geometry, borders, and dimensions:
 * - Border style and widths
 * - 4-corner border radius and 50% circle detection
 * - Positional offsets (left, right, top, bottom, inset)
 * - Dimensions in px or %
 */
object GeometryParser {

    private val INSET_PATTERN = CssSyntaxPattern.INSET.pattern
    private val BORDER_WIDTH_PATTERN = CssSyntaxPattern.BORDER_WIDTH.pattern
    private val RADIUS_PATTERN = CssSyntaxPattern.RADIUS.pattern
    private val PX_PATTERN = CssSyntaxPattern.PIXEL.pattern
    private val WHITESPACE_PATTERN = CssSyntaxPattern.WHITESPACE.pattern

    fun parseInset(insetStr: String?, parentWidth: Float, parentHeight: Float): InsetRect? {
        if (insetStr.isNullOrBlank()) return null
        val clean = insetStr.trim()
        val values = mutableListOf<Float>()
        val matcher = INSET_PATTERN.matcher(clean)
        while (matcher.find()) {
            val num = matcher.group(1).toFloatOrNull() ?: 0f
            val token = clean.substring(matcher.start(), matcher.end())
            val isPct = token.contains("%")
            val isVert = values.size % 2 == 0 // 0 = top, 1 = right, 2 = bottom, 3 = left
            val parentDim = if (isVert) parentHeight else parentWidth
            val pxVal = if (isPct) (num / 100f) * parentDim else num
            values.add(pxVal)
        }
        return when (values.size) {
            1 -> InsetRect(values[0], values[0], values[0], values[0])
            2 -> InsetRect(values[0], values[1], values[0], values[1])
            3 -> InsetRect(values[0], values[1], values[2], values[1])
            4 -> InsetRect(values[0], values[1], values[2], values[3])
            else -> null
        }
    }

    /**
     * Parses a CSS positional offset string into a semantic [CssPositionValue],
     * strictly distinguishing Unspecified / Auto from explicit Px or Percent.
     */
    fun parsePositionValue(valStr: String?): CssPositionValue {
        if (valStr.isNullOrBlank()) return CssPositionValue.Unspecified
        val clean = valStr.trim().lowercase()
        if (clean == "auto") return CssPositionValue.Auto
        if (clean.endsWith("%")) {
            val pct = clean.removeSuffix("%").toFloatOrNull() ?: return CssPositionValue.Unspecified
            return CssPositionValue.Percent(pct)
        }
        val m = PX_PATTERN.matcher(clean)
        if (m.find()) {
            val px = m.group(1).toFloatOrNull() ?: return CssPositionValue.Unspecified
            return CssPositionValue.Px(px)
        }
        val num = clean.toFloatOrNull()
        return if (num != null) CssPositionValue.Px(num) else CssPositionValue.Unspecified
    }

    /**
     * Extracts horizontal and vertical position constraints from CSS style rules,
     * merging `inset` shorthand with explicit `left`, `right`, `top`, `bottom`.
     */
    fun extractPositionConstraints(style: Map<String, String>, parentW: Float, parentH: Float): PositionConstraints {
        val inset = parseInset(style["inset"], parentW, parentH)
        val left = when {
            style["left"] != null -> parsePositionValue(style["left"])
            inset != null -> CssPositionValue.Px(inset.left)
            else -> CssPositionValue.Unspecified
        }
        val right = when {
            style["right"] != null -> parsePositionValue(style["right"])
            inset != null -> CssPositionValue.Px(inset.right)
            else -> CssPositionValue.Unspecified
        }
        val top = when {
            style["top"] != null -> parsePositionValue(style["top"])
            inset != null -> CssPositionValue.Px(inset.top)
            else -> CssPositionValue.Unspecified
        }
        val bottom = when {
            style["bottom"] != null -> parsePositionValue(style["bottom"])
            inset != null -> CssPositionValue.Px(inset.bottom)
            else -> CssPositionValue.Unspecified
        }
        return PositionConstraints(left = left, right = right, top = top, bottom = bottom)
    }

    fun computeBoxBounds(style: Map<String, String>, parentW: Float, parentH: Float): ComputedBoxBounds {
        val constraints = extractPositionConstraints(style, parentW, parentH)

        val explicitWidth = style["width"]?.let { parsePixelOrPercent(it, parentW, parentW) }
        val explicitHeight = style["height"]?.let { parsePixelOrPercent(it, parentH, parentH) }

        val isBorderBox = style["box-sizing"]?.trim()?.lowercase() != "content-box"
        val padTop = parsePixelOrPercent(style["padding-top"] ?: style["padding"], parentH, 0f)
        val padBottom = parsePixelOrPercent(style["padding-bottom"] ?: style["padding"], parentH, 0f)
        val padLeft = parsePixelOrPercent(style["padding-left"] ?: style["padding"], parentW, 0f)
        val padRight = parsePixelOrPercent(style["padding-right"] ?: style["padding"], parentW, 0f)

        var width = when {
            explicitWidth != null -> explicitWidth
            constraints.left.isExplicit && constraints.right.isExplicit -> {
                val l = constraints.left.resolve(parentW) ?: 0f
                val r = constraints.right.resolve(parentW) ?: 0f
                (parentW - l - r).coerceAtLeast(0f)
            }
            else -> parentW
        }

        var height = when {
            explicitHeight != null -> explicitHeight
            constraints.top.isExplicit && constraints.bottom.isExplicit -> {
                val t = constraints.top.resolve(parentH) ?: 0f
                val b = constraints.bottom.resolve(parentH) ?: 0f
                (parentH - t - b).coerceAtLeast(0f)
            }
            else -> parentH
        }

        if (!isBorderBox) {
            width += padLeft + padRight
            height += padTop + padBottom
        }

        val left = when {
            constraints.left.isExplicit -> constraints.left.resolve(parentW) ?: 0f
            constraints.right.isExplicit -> (parentW - (constraints.right.resolve(parentW) ?: 0f) - width)
            else -> 0f
        }

        val top = when {
            constraints.top.isExplicit -> constraints.top.resolve(parentH) ?: 0f
            constraints.bottom.isExplicit -> (parentH - (constraints.bottom.resolve(parentH) ?: 0f) - height)
            else -> 0f
        }

        return ComputedBoxBounds(left = left, top = top, width = width, height = height)
    }

    fun parseBorder(borderStr: String?, isTopOnly: Boolean = false): StrokeStyle? {
        if (borderStr.isNullOrBlank() || borderStr.trim().equals("none", ignoreCase = true)) return null
        val color = ColorParser.extractColorAnywhere(borderStr) ?: 0xFF00F0FFL

        var width = 2.0f
        val wMatcher = BORDER_WIDTH_PATTERN.matcher(borderStr)
        if (wMatcher.find()) {
            width = wMatcher.group(1).toFloatOrNull() ?: 2.0f
        }
        if (width == 0f) return null
        val isDashed = borderStr.contains("dashed", ignoreCase = true)
        val isDotted = borderStr.contains("dotted", ignoreCase = true)
        val dashWidth = when {
            isDashed -> width * 3f
            isDotted -> width
            else -> 0f
        }
        val dashGap = when {
            isDashed -> width * 2f
            isDotted -> width
            else -> 0f
        }
        return StrokeStyle(
            color = color,
            width = width,
            isDashed = isDashed || isDotted,
            dashWidth = dashWidth,
            dashGap = dashGap,
            isTopOnly = isTopOnly
        )
    }

    fun parseBorderRadius(radiusStr: String?, defaultSizeDp: Float = 76f): CornerRadii {
        if (radiusStr.isNullOrBlank()) return CornerRadii()
        val clean = radiusStr.trim()

        val partBeforeSlash = clean.split("/").first().trim()
        val tokens = partBeforeSlash.split(WHITESPACE_PATTERN).filter { it.isNotBlank() }
        if (tokens.isNotEmpty() && tokens.all { it == "50%" }) {
            val half = defaultSizeDp / 2f
            return CornerRadii(half, half, half, half)
        }
        val values = mutableListOf<Float>()
        val numMatcher = RADIUS_PATTERN.matcher(partBeforeSlash)
        while (numMatcher.find()) {
            val num = numMatcher.group(1).toFloatOrNull() ?: 0f
            val isPct = partBeforeSlash.substring(numMatcher.start(), numMatcher.end()).contains("%")
            val pxVal = if (isPct) (num / 100f) * defaultSizeDp else num
            values.add(pxVal)
        }

        return when {
            values.size == 1 -> CornerRadii(values[0], values[0], values[0], values[0])
            values.size == 2 -> CornerRadii(values[0], values[1], values[0], values[1])
            values.size == 3 -> CornerRadii(values[0], values[1], values[2], values[1])
            values.size >= 4 -> CornerRadii(values[0], values[1], values[2], values[3])
            else -> CornerRadii()
        }
    }

    fun parsePixelOrPercent(valStr: String?, parentDim: Float, fallback: Float): Float {
        if (valStr.isNullOrBlank()) return fallback
        val clean = valStr.trim().lowercase()
        if (clean.endsWith("%")) {
            val pct = clean.removeSuffix("%").toFloatOrNull() ?: return fallback
            return (pct / 100f) * parentDim
        }
        val m = PX_PATTERN.matcher(clean)
        return if (m.find()) m.group(1).toFloatOrNull() ?: fallback else fallback
    }

    fun parsePositionalOffset(posStart: String?, posEnd: String?, elemDim: Float, parentDim: Float, fallback: Float): Float {
        if (!posStart.isNullOrBlank()) {
            return parsePixelOrPercent(posStart, parentDim, fallback)
        }
        if (!posEnd.isNullOrBlank()) {
            val endVal = parsePixelOrPercent(posEnd, parentDim, fallback)
            return parentDim - endVal - elemDim
        }
        return fallback
    }

    fun parseZIndex(style: Map<String, String>?): Int {
        if (style == null) return 0
        val z = style["z-index"]?.trim() ?: return 0
        if (z.equals("auto", ignoreCase = true) || z.isBlank()) return 0
        return z.toIntOrNull() ?: 0
    }

    fun parseClipPath(clipPathStr: String?, width: Float = 100f, height: Float = 100f): ParsedClipShape? {
        if (clipPathStr.isNullOrBlank()) return null
        val clean = clipPathStr.trim()

        // 1. polygon(...)
        if (clean.contains("polygon")) {
            val openParen = clean.indexOf('(')
            val closeParen = clean.lastIndexOf(')')
            if (openParen != -1 && closeParen > openParen) {
                val inner = clean.substring(openParen + 1, closeParen).trim()
                // Vertex pairs separated by commas
                val pairs = inner.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val vertices = mutableListOf<Pair<Float, Float>>()

                for (pair in pairs) {
                    val tokens = pair.split(WHITESPACE_PATTERN).filter { it.isNotEmpty() }
                    if (tokens.size >= 2) {
                        val xRatio = parseCoordRatio(tokens[0], width)
                        val yRatio = parseCoordRatio(tokens[1], height)
                        vertices.add(Pair(xRatio, yRatio))
                    }
                }

                if (vertices.size >= 3) {
                    // Build normalized 0..100 viewBox SVG path
                    val sb = StringBuilder()
                    vertices.forEachIndexed { i, pt ->
                        val cmd = if (i == 0) "M" else "L"
                        val x = pt.first * 100f
                        val y = pt.second * 100f
                        sb.append("$cmd $x $y ")
                    }
                    sb.append("Z")

                    val detectedShape = when (vertices.size) {
                        6 -> LayerShapeType.HEXAGON.name
                        8 -> LayerShapeType.OCTAGON.name
                        else -> LayerShapeType.POLYGON.name
                    }
                    return ParsedClipShape(
                        shapeType = detectedShape,
                        polygonSides = vertices.size,
                        pathData = sb.toString().trim(),
                        normalizedVertices = vertices
                    )
                }
            }
        }

        // 2. path("...") or path('...')
        if (clean.contains("path(")) {
            val openParen = clean.indexOf('(')
            val closeParen = clean.lastIndexOf(')')
            if (openParen != -1 && closeParen > openParen) {
                val raw = clean.substring(openParen + 1, closeParen).trim()
                val unquoted = raw.trim('\'', '"')
                if (unquoted.isNotBlank()) {
                    return ParsedClipShape(
                        shapeType = LayerShapeType.PATH.name,
                        pathData = unquoted
                    )
                }
            }
        }

        // 3. circle(...) / ellipse(...)
        if (clean.contains("circle(") || clean.contains("ellipse(")) {
            return ParsedClipShape(
                shapeType = LayerShapeType.OVAL.name
            )
        }

        return null
    }

    private fun parseCoordRatio(token: String, parentDim: Float): Float {
        val clean = token.trim().lowercase()
        return when {
            clean.endsWith("%") -> {
                val pct = clean.removeSuffix("%").toFloatOrNull() ?: 0f
                (pct / 100f).coerceIn(0f, 1f)
            }
            clean.endsWith("px") -> {
                val px = clean.removeSuffix("px").toFloatOrNull() ?: 0f
                (px / parentDim.coerceAtLeast(1f)).coerceIn(0f, 1f)
            }
            else -> {
                val v = clean.toFloatOrNull() ?: 0f
                (v / parentDim.coerceAtLeast(1f)).coerceIn(0f, 1f)
            }
        }
    }

    fun parseFontSize(str: String?): Float? = TypographyParser.parseFontSize(str)
    fun parseFontWeight(str: String?): Int = TypographyParser.parseFontWeight(str)

}
