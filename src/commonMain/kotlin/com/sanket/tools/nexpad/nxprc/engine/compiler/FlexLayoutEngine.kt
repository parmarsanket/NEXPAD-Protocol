package com.sanket.tools.nexpad.nxprc.engine.compiler

import com.sanket.tools.nexpad.nxprc.engine.css.AlignItems
import com.sanket.tools.nexpad.nxprc.engine.css.CssCascadeResolver
import com.sanket.tools.nexpad.nxprc.engine.css.CssStylesheet
import com.sanket.tools.nexpad.nxprc.engine.css.DisplayValue
import com.sanket.tools.nexpad.nxprc.engine.css.FlexDirection
import com.sanket.tools.nexpad.nxprc.engine.css.JustifyContent
import com.sanket.tools.nexpad.nxprc.engine.dom.DomNode
import com.sanket.tools.nexpad.nxprc.engine.parsers.ComputedBoxBounds
import com.sanket.tools.nexpad.nxprc.engine.parsers.GeometryParser
import com.sanket.tools.nexpad.nxprc.engine.text.TextMetrics


/**
 * High-precision CSS Flexbox layout engine for NXPRC documents.
 * Handles flex container distributions (row, column, reverse), justify-content, align-items,
 * gap, padding, and hierarchical global centering across arbitrary DOM depths.
 */
internal object FlexLayoutEngine {

    /**
     * Canonical position resolver for out-of-flow (position: absolute) or explicitly positioned elements.
     * When offsets (left, top, right, bottom, inset) are unspecified or auto, computes the W3C CSS Flexbox §4.1
     * static position derived from the containing flex container's alignment properties (justify-content,
     * align-items, align-self, flex-direction, and padding).
     *
     * Explicit author positioning constraints always take precedence over inferred static positioning.
     */
    fun resolvePositionedChildBounds(
        parentStyle: Map<String, String>,
        childStyle: Map<String, String>,
        rawBounds: ComputedBoxBounds,
        parentWidth: Float,
        parentHeight: Float
    ): ComputedBoxBounds {
        if (DisplayValue.parse(parentStyle["display"]) != DisplayValue.FLEX) return rawBounds

        val constraints = GeometryParser.extractPositionConstraints(childStyle, parentWidth, parentHeight)

        // Flex container padding
        val pad = GeometryParser.parseInset(parentStyle["padding"], parentWidth, parentHeight)
        val pTop = GeometryParser.parsePixelOrPercent(parentStyle["padding-top"], parentHeight, pad?.top ?: 0f)
        val pBottom = GeometryParser.parsePixelOrPercent(parentStyle["padding-bottom"], parentHeight, pad?.bottom ?: 0f)
        val pLeft = GeometryParser.parsePixelOrPercent(parentStyle["padding-left"], parentWidth, pad?.left ?: 0f)
        val pRight = GeometryParser.parsePixelOrPercent(parentStyle["padding-right"], parentWidth, pad?.right ?: 0f)

        // ── Typed flex orientation & alignment (replaces raw "center", "column" strings) ──────────
        val direction = FlexDirection.parse(parentStyle["flex-direction"])
        val isColumn  = direction.isColumn
        val isReverse = direction.isReverse
        val justify   = JustifyContent.parse(parentStyle["justify-content"])
        val align     = AlignItems.parseSelf(childStyle["align-self"])
            ?: AlignItems.parse(parentStyle["align-items"])

        val availMain = if (isColumn) {
            (parentHeight - pTop - pBottom - rawBounds.height).coerceAtLeast(0f)
        } else {
            (parentWidth - pLeft - pRight - rawBounds.width).coerceAtLeast(0f)
        }

        val availCross = if (isColumn) {
            (parentWidth - pLeft - pRight - rawBounds.width).coerceAtLeast(0f)
        } else {
            (parentHeight - pTop - pBottom - rawBounds.height).coerceAtLeast(0f)
        }

        val startMain = if (isColumn) pTop else pLeft
        val staticMain = if (!isReverse) {
            when (justify) {
                JustifyContent.CENTER, JustifyContent.SPACE_AROUND, JustifyContent.SPACE_EVENLY -> startMain + availMain / 2f
                JustifyContent.FLEX_END, JustifyContent.END -> startMain + availMain
                else -> startMain // FLEX_START, SPACE_BETWEEN (1 item sits at start)
            }
        } else {
            when (justify) {
                JustifyContent.CENTER, JustifyContent.SPACE_AROUND, JustifyContent.SPACE_EVENLY -> startMain + availMain / 2f
                JustifyContent.FLEX_START, JustifyContent.START, JustifyContent.SPACE_BETWEEN  -> startMain + availMain
                else -> startMain // FLEX_END in reverse is at start
            }
        }

        val startCross = if (isColumn) pLeft else pTop
        val staticCross = when (align) {
            AlignItems.CENTER           -> startCross + availCross / 2f
            AlignItems.FLEX_END, AlignItems.END -> startCross + availCross
            else -> startCross // FLEX_START, STRETCH, BASELINE
        }

        val staticLeft = if (isColumn) staticCross else staticMain
        val staticTop = if (isColumn) staticMain else staticCross

        val resolvedLeft = when {
            constraints.left.isExplicit && constraints.right.isExplicit -> {
                val l = constraints.left.resolve(parentWidth) ?: 0f
                val r = constraints.right.resolve(parentWidth) ?: 0f
                val isMarginAuto = childStyle["margin"]?.contains("auto") == true || childStyle["margin-left"] == "auto"
                if (isMarginAuto) {
                    l + (parentWidth - l - r - rawBounds.width).coerceAtLeast(0f) / 2f
                } else {
                    l
                }
            }
            constraints.left.isExplicit -> constraints.left.resolve(parentWidth) ?: 0f
            constraints.right.isExplicit -> (parentWidth - (constraints.right.resolve(parentWidth) ?: 0f) - rawBounds.width)
            childStyle["margin"]?.contains("auto") == true || childStyle["margin-left"] == "auto" -> {
                pLeft + (parentWidth - pLeft - pRight - rawBounds.width).coerceAtLeast(0f) / 2f
            }
            else -> staticLeft
        }

        val resolvedTop = when {
            constraints.top.isExplicit && constraints.bottom.isExplicit -> {
                val t = constraints.top.resolve(parentHeight) ?: 0f
                val b = constraints.bottom.resolve(parentHeight) ?: 0f
                val isMarginAuto = childStyle["margin"]?.contains("auto") == true || childStyle["margin-top"] == "auto"
                if (isMarginAuto) {
                    t + (parentHeight - t - b - rawBounds.height).coerceAtLeast(0f) / 2f
                } else {
                    t
                }
            }
            constraints.top.isExplicit -> constraints.top.resolve(parentHeight) ?: 0f
            constraints.bottom.isExplicit -> (parentHeight - (constraints.bottom.resolve(parentHeight) ?: 0f) - rawBounds.height)
            childStyle["margin"]?.contains("auto") == true || childStyle["margin-top"] == "auto" -> {
                pTop + (parentHeight - pTop - pBottom - rawBounds.height).coerceAtLeast(0f) / 2f
            }
            else -> staticTop
        }

        return rawBounds.copy(left = resolvedLeft, top = resolvedTop)
    }

    private fun applyRelativeOffset(
        childStyle: Map<String, String>,
        cLeft: Float,
        cTop: Float,
        parentWidth: Float,
        parentHeight: Float
    ): Pair<Float, Float> {
        val position = childStyle["position"]?.trim()?.lowercase()
        if (position != "relative") return cLeft to cTop
        val constraints = GeometryParser.extractPositionConstraints(childStyle, parentWidth, parentHeight)
        val finalLeft = when {
            constraints.left.isExplicit -> cLeft + (constraints.left.resolve(parentWidth) ?: 0f)
            constraints.right.isExplicit -> cLeft - (constraints.right.resolve(parentWidth) ?: 0f)
            else -> cLeft
        }
        val finalTop = when {
            constraints.top.isExplicit -> cTop + (constraints.top.resolve(parentHeight) ?: 0f)
            constraints.bottom.isExplicit -> cTop - (constraints.bottom.resolve(parentHeight) ?: 0f)
            else -> cTop
        }
        return finalLeft to finalTop
    }

    /**
     * Resolves layout positioning for all in-flow children inside a CSS flex container,
     * computing exact (left, top, width, height) coordinates respecting padding, gaps,
     * main-axis distribution, and cross-axis alignment.
     */
    fun layoutFlexContainerChildren(
        parentNode: DomNode,
        parentStyle: Map<String, String>,
        stylesheet: CssStylesheet,
        parentWidth: Float,
        parentHeight: Float,
        styleCache: MutableMap<DomNode, com.sanket.tools.nexpad.nxprc.engine.css.ComputedElementStyle>? = null
    ): Map<DomNode, ComputedBoxBounds> {
        val result = mutableMapOf<DomNode, ComputedBoxBounds>()
        val children = parentNode.children
        if (DisplayValue.parse(parentStyle["display"]) != DisplayValue.FLEX) {
            for (child in children) {
                val childStyle = CssCascadeResolver.computeStyle(child, stylesheet, styleCache).base
                result[child] = GeometryParser.computeBoxBounds(childStyle, parentWidth, parentHeight)
            }
            return result
        }

        // ── Typed flex properties (replaces raw string comparisons) ────────────────────────────────
        val direction    = FlexDirection.parse(parentStyle["flex-direction"])
        val isColumn     = direction.isColumn
        val isReverse    = direction.isReverse
        val justify      = JustifyContent.parse(parentStyle["justify-content"])
        val align        = AlignItems.parse(parentStyle["align-items"])

        val pad = GeometryParser.parseInset(parentStyle["padding"], parentWidth, parentHeight)
        val pTop = GeometryParser.parsePixelOrPercent(parentStyle["padding-top"], parentHeight, pad?.top ?: 0f)
        val pBottom = GeometryParser.parsePixelOrPercent(parentStyle["padding-bottom"], parentHeight, pad?.bottom ?: 0f)
        val pLeft = GeometryParser.parsePixelOrPercent(parentStyle["padding-left"], parentWidth, pad?.left ?: 0f)
        val pRight = GeometryParser.parsePixelOrPercent(parentStyle["padding-right"], parentWidth, pad?.right ?: 0f)

        val gapStr = parentStyle["gap"] ?: (if (isColumn) parentStyle["row-gap"] else parentStyle["column-gap"])
        val gap = GeometryParser.parsePixelOrPercent(gapStr, if (isColumn) parentHeight else parentWidth, 0f)

        val inFlowList = mutableListOf<Pair<DomNode, ComputedBoxBounds>>()

        for (child in children) {
            val childStyle = CssCascadeResolver.computeStyle(child, stylesheet, styleCache).base
            val position = childStyle["position"]?.trim()?.lowercase()
            val isOutOfFlow = position == "absolute" || position == "fixed"
            val rawBounds = GeometryParser.computeBoxBounds(childStyle, parentWidth, parentHeight)

            if (isOutOfFlow) {
                result[child] = resolvePositionedChildBounds(parentStyle, childStyle, rawBounds, parentWidth, parentHeight)
            } else {
                var w = rawBounds.width
                var h = rawBounds.height
                if (childStyle["width"] == null) {
                    val text = child.findFirstText()
                    if (text != null) {
                        val fontSize = GeometryParser.parseFontSize(childStyle["font-size"] ?: parentStyle["font-size"]) ?: 16f
                        val fontWeight = childStyle["font-weight"]?.let { GeometryParser.parseFontWeight(it) } ?: 400
                        val metrics = TextMetrics.measure(text, fontSize, fontWeight)
                        w = metrics.width.coerceIn(fontSize, (parentWidth - pLeft - pRight).coerceAtLeast(fontSize))
                        if (childStyle["height"] == null) {
                            h = metrics.height
                        }
                    }
                }
                if (childStyle["height"] == null && childStyle["width"] != null) {
                    val fontSize = GeometryParser.parseFontSize(childStyle["font-size"] ?: parentStyle["font-size"])
                    if (fontSize != null) {
                        h = fontSize * 1.2f
                    }
                }
                inFlowList.add(child to ComputedBoxBounds(rawBounds.left, rawBounds.top, w, h))
            }
        }

        if (inFlowList.isEmpty()) return result

        val flexWrap = parentStyle["flex-wrap"]?.trim()?.lowercase() ?: "nowrap"
        val isWrap = flexWrap == "wrap" || flexWrap == "wrap-reverse"
        val isWrapReverse = flexWrap == "wrap-reverse"
        val alignContent = parentStyle["align-content"]?.trim()?.lowercase() ?: "flex-start"

        val orderedList = if (isReverse) inFlowList.reversed() else inFlowList

        if (!isWrap) {
            val totalMain = orderedList.sumOf { (if (isColumn) it.second.height else it.second.width).toDouble() }.toFloat() +
                    ((orderedList.size - 1).coerceAtLeast(0) * gap)

            val availMain = if (isColumn) {
                (parentHeight - pTop - pBottom - totalMain).coerceAtLeast(0f)
            } else {
                (parentWidth - pLeft - pRight - totalMain).coerceAtLeast(0f)
            }

            var currentMain = when (justify) {
                JustifyContent.CENTER                         -> (if (isColumn) pTop else pLeft) + availMain / 2f
                JustifyContent.FLEX_END, JustifyContent.END  -> (if (isColumn) parentHeight - pBottom - totalMain else parentWidth - pRight - totalMain)
                JustifyContent.SPACE_AROUND                  -> (if (isColumn) pTop else pLeft) + (availMain / (orderedList.size * 2f))
                JustifyContent.SPACE_EVENLY                  -> (if (isColumn) pTop else pLeft) + (availMain / (orderedList.size + 1f))
                else                                         -> if (isColumn) pTop else pLeft
            }

            val extraSpacing = when (justify) {
                JustifyContent.SPACE_BETWEEN -> if (orderedList.size > 1) availMain / (orderedList.size - 1) else 0f
                JustifyContent.SPACE_AROUND  -> availMain / orderedList.size
                JustifyContent.SPACE_EVENLY  -> availMain / (orderedList.size + 1f)
                else                         -> 0f
            }

            for ((child, bounds) in orderedList) {
                val childW = bounds.width
                val childH = bounds.height

                val (cLeft, cTop) = if (isColumn) {
                    val top = currentMain
                    val left = when (align) {
                        AlignItems.CENTER           -> pLeft + (parentWidth - pLeft - pRight - childW).coerceAtLeast(0f) / 2f
                        AlignItems.FLEX_END, AlignItems.END -> parentWidth - pRight - childW
                        else                        -> pLeft
                    }
                    currentMain += childH + (if (justify.isSpaced) extraSpacing else gap)
                    left to top
                } else {
                    val left = currentMain
                    val top = when (align) {
                        AlignItems.CENTER           -> pTop + (parentHeight - pTop - pBottom - childH).coerceAtLeast(0f) / 2f
                        AlignItems.FLEX_END, AlignItems.END -> parentHeight - pBottom - childH
                        else                        -> pTop
                    }
                    currentMain += childW + (if (justify.isSpaced) extraSpacing else gap)
                    left to top
                }

                val childStyle = CssCascadeResolver.computeStyle(child, stylesheet, styleCache).base
                val (finalLeft, finalTop) = applyRelativeOffset(childStyle, cLeft, cTop, parentWidth, parentHeight)
                result[child] = ComputedBoxBounds(finalLeft, finalTop, childW, childH)
            }
        } else {
            val maxMainSize = if (isColumn) (parentHeight - pTop - pBottom) else (parentWidth - pLeft - pRight)
            val lines = mutableListOf<MutableList<Pair<DomNode, ComputedBoxBounds>>>()
            var curLine = mutableListOf<Pair<DomNode, ComputedBoxBounds>>()
            var curLineMain = 0f

            for (item in orderedList) {
                val itemMain = if (isColumn) item.second.height else item.second.width
                if (curLine.isNotEmpty() && (curLineMain + gap + itemMain > maxMainSize)) {
                    lines.add(curLine)
                    curLine = mutableListOf(item)
                    curLineMain = itemMain
                } else {
                    curLineMain += if (curLine.isEmpty()) itemMain else (gap + itemMain)
                    curLine.add(item)
                }
            }
            if (curLine.isNotEmpty()) {
                lines.add(curLine)
            }

            val finalLines = if (isWrapReverse) lines.reversed() else lines
            val totalCross = finalLines.sumOf { line ->
                line.maxOfOrNull { (if (isColumn) it.second.width else it.second.height).toDouble() } ?: 0.0
            }.toFloat() + ((finalLines.size - 1).coerceAtLeast(0) * gap)

            val availCross = if (isColumn) {
                (parentWidth - pLeft - pRight - totalCross).coerceAtLeast(0f)
            } else {
                (parentHeight - pTop - pBottom - totalCross).coerceAtLeast(0f)
            }

            var currentCross = when (alignContent) {
                "center"   -> (if (isColumn) pLeft else pTop) + availCross / 2f
                "flex-end" -> (if (isColumn) parentWidth - pRight - totalCross else parentHeight - pBottom - totalCross)
                else       -> if (isColumn) pLeft else pTop
            }

            for (line in finalLines) {
                val lineCrossSize = line.maxOfOrNull { if (isColumn) it.second.width else it.second.height } ?: 0f
                val lineTotalMain = line.sumOf { (if (isColumn) it.second.height else it.second.width).toDouble() }.toFloat() +
                        ((line.size - 1).coerceAtLeast(0) * gap)
                val lineAvailMain = (maxMainSize - lineTotalMain).coerceAtLeast(0f)

                var currentMain = when (justify) {
                    JustifyContent.CENTER                        -> (if (isColumn) pTop else pLeft) + lineAvailMain / 2f
                    JustifyContent.FLEX_END, JustifyContent.END  -> (if (isColumn) parentHeight - pBottom - lineTotalMain else parentWidth - pRight - lineTotalMain)
                    JustifyContent.SPACE_AROUND                  -> (if (isColumn) pTop else pLeft) + (lineAvailMain / (line.size * 2f))
                    JustifyContent.SPACE_EVENLY                  -> (if (isColumn) pTop else pLeft) + (lineAvailMain / (line.size + 1f))
                    else                                         -> if (isColumn) pTop else pLeft
                }

                val extraSpacing = when (justify) {
                    JustifyContent.SPACE_BETWEEN -> if (line.size > 1) lineAvailMain / (line.size - 1) else 0f
                    JustifyContent.SPACE_AROUND  -> lineAvailMain / line.size
                    JustifyContent.SPACE_EVENLY  -> lineAvailMain / (line.size + 1f)
                    else                         -> 0f
                }

                for ((child, bounds) in line) {
                    val childW = bounds.width
                    val childH = bounds.height

                    val (cLeft, cTop) = if (isColumn) {
                        val top = currentMain
                        val left = when (align) {
                            AlignItems.CENTER           -> currentCross + (lineCrossSize - childW) / 2f
                            AlignItems.FLEX_END, AlignItems.END -> currentCross + lineCrossSize - childW
                            else                        -> currentCross
                        }
                        currentMain += childH + (if (justify.isSpaced) extraSpacing else gap)
                        left to top
                    } else {
                        val left = currentMain
                        val top = when (align) {
                            AlignItems.CENTER           -> currentCross + (lineCrossSize - childH) / 2f
                            AlignItems.FLEX_END, AlignItems.END -> currentCross + lineCrossSize - childH
                            else                        -> currentCross
                        }
                        currentMain += childW + (if (justify.isSpaced) extraSpacing else gap)
                        left to top
                    }

                    val childStyle = CssCascadeResolver.computeStyle(child, stylesheet, styleCache).base
                    val (finalLeft, finalTop) = applyRelativeOffset(childStyle, cLeft, cTop, parentWidth, parentHeight)
                    result[child] = ComputedBoxBounds(finalLeft, finalTop, childW, childH)
                }

                currentCross += lineCrossSize + gap
            }
        }

        return result
    }

    /**
     * Traverses the DOM hierarchy from [node] up to [rootNode], resolving flexbox and box-model
     * coordinate offsets at every ancestor level to compute the exact global center (X, Y) of the element.
     */
    fun computeNodeGlobalCenter(
        node: DomNode,
        rootNode: DomNode,
        stylesheet: CssStylesheet,
        rootW: Float,
        rootH: Float,
        styleCache: MutableMap<DomNode, com.sanket.tools.nexpad.nxprc.engine.css.ComputedElementStyle>? = null
    ): Pair<Float, Float> {
        if (node == rootNode) return Pair(rootW / 2f, rootH / 2f)
        val path = mutableListOf<DomNode>()
        var cur: DomNode? = node
        while (cur != null && cur != rootNode) {
            path.add(0, cur)
            cur = cur.parent
        }
        var curX = 0f
        var curY = 0f
        var pW = rootW
        var pH = rootH
        var currentParent = rootNode
        for (elem in path) {
            val parentStyle = CssCascadeResolver.computeStyle(currentParent, stylesheet, styleCache).base
            val flexBoundsMap = layoutFlexContainerChildren(currentParent, parentStyle, stylesheet, pW, pH, styleCache)
            val b = flexBoundsMap[elem] ?: run {
                val elemStyle = CssCascadeResolver.computeStyle(elem, stylesheet, styleCache).base
                val raw = GeometryParser.computeBoxBounds(elemStyle, pW, pH)
                resolvePositionedChildBounds(parentStyle, elemStyle, raw, pW, pH)
            }
            curX += b.left
            curY += b.top
            pW = b.width
            pH = b.height
            currentParent = elem
        }
        return Pair(curX + pW / 2f, curY + pH / 2f)
    }

    // ── New: Compose-Constraints helpers (Stage 3) ────────────────────────────

    /**
     * Resolves the concrete pixel width and height of a flex in-flow child respecting:
     * 1. Explicit CSS dimensions (plain px / %)
     * 2. `calc()` expressions via [GeometryParser.parseDimensionWithCalc]
     * 3. `aspect-ratio` enforcement via [GeometryParser.parseAspectRatio]
     * 4. Intrinsic text sizing (existing logic, preserved unchanged)
     *
     * Returns a [ComputedBoxBounds] with resolved width/height (left/top remain 0 — the
     * caller is responsible for position assignment).
     *
     * This function is called by the in-flow child loop in [layoutFlexContainerChildren].
     * Existing callers that do NOT use calc() / aspect-ratio are unaffected — the fast
     * paths short-circuit immediately.
     */
    internal fun resolveInFlowChildSize(
        child: DomNode,
        childStyle: Map<String, String>,
        parentStyle: Map<String, String>,
        rawBounds: ComputedBoxBounds,
        parentWidth: Float,
        parentHeight: Float
    ): ComputedBoxBounds {
        var w = rawBounds.width
        var h = rawBounds.height

        // 1. Resolve calc() on width
        val widthStr = childStyle["width"]
        if (widthStr != null && widthStr.startsWith("calc(", ignoreCase = true)) {
            GeometryParser.parseDimensionWithCalc(widthStr, parentWidth)?.let { w = it }
        }

        // 2. Resolve calc() on height
        val heightStr = childStyle["height"]
        if (heightStr != null && heightStr.startsWith("calc(", ignoreCase = true)) {
            GeometryParser.parseDimensionWithCalc(heightStr, parentHeight)?.let { h = it }
        }

        // 3. Apply aspect-ratio if present (and only one axis is explicitly constrained)
        val aspectRatio = GeometryParser.parseAspectRatio(childStyle["aspect-ratio"])
        if (aspectRatio != null) {
            when {
                widthStr != null && heightStr == null -> {
                    // Width is explicit → derive height
                    h = (w / aspectRatio).coerceAtLeast(1f)
                }
                heightStr != null && widthStr == null -> {
                    // Height is explicit → derive width
                    w = (h * aspectRatio).coerceAtLeast(1f)
                }
                widthStr == null && heightStr == null -> {
                    // Neither axis explicit — enforce ratio on the raw (parent-fill) width
                    h = (w / aspectRatio).coerceAtLeast(1f)
                }
                // Both explicit — author wins, ignore aspect-ratio (matches browser behaviour)
            }
        }

        // 4. Intrinsic text sizing (existing logic, only triggers when width was null originally)
        if (widthStr == null && aspectRatio == null) {
            val text = child.findFirstText()
            if (text != null) {
                val fontSize = GeometryParser.parseFontSize(
                    childStyle["font-size"] ?: parentStyle["font-size"]
                ) ?: 16f
                val fontWeight = childStyle["font-weight"]
                    ?.let { GeometryParser.parseFontWeight(it) } ?: 400
                val metrics = TextMetrics.measure(text, fontSize, fontWeight)
                val maxW = (parentWidth - 0f).coerceAtLeast(fontSize) // padding handled by parent
                w = metrics.width.coerceIn(fontSize, maxW)
                if (heightStr == null) h = metrics.height
            }
        }

        // 5. Clamp via LayoutConstraints (min/max from parent available space)
        val constraints = LayoutConstraints.atMost(parentWidth.coerceAtLeast(1f), parentHeight.coerceAtLeast(1f))
        w = constraints.constrainWidth(w)
        h = constraints.constrainHeight(h)

        return rawBounds.copy(width = w, height = h)
    }
}
