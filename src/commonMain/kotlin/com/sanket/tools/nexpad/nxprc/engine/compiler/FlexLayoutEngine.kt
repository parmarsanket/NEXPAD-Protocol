package com.sanket.tools.nexpad.nxprc.engine.compiler

import com.sanket.tools.nexpad.nxprc.engine.css.CssCascadeResolver
import com.sanket.tools.nexpad.nxprc.engine.css.CssStylesheet
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
     * Resolves the small but important subset of flex alignment for individual children
     * (such as pseudo-elements and centered overlays).
     */
    fun resolveFlexChildBounds(
        parentStyle: Map<String, String>,
        childStyle: Map<String, String>,
        rawBounds: ComputedBoxBounds,
        parentWidth: Float,
        parentHeight: Float,
        allowAbsoluteFlexAlignment: Boolean = false
    ): ComputedBoxBounds {
        if (parentStyle["display"]?.trim()?.lowercase() != "flex") return rawBounds

        // Absolutely positioned children follow inset/left/top and must not
        // be moved by flex alignment.
        if ((!allowAbsoluteFlexAlignment && childStyle["position"]?.trim()?.lowercase() == "absolute") ||
            childStyle.keys.any { it == "inset" || it == "left" || it == "right" || it == "top" || it == "bottom" }
        ) return rawBounds

        val direction = parentStyle["flex-direction"]?.trim()?.lowercase() ?: "row"
        val justify = parentStyle["justify-content"]?.trim()?.lowercase() ?: "flex-start"
        val align = parentStyle["align-items"]?.trim()?.lowercase() ?: "stretch"

        fun centered(start: Float, available: Float, size: Float): Float =
            when (start) {
                0f -> (available - size).coerceAtLeast(0f) / 2f
                else -> start
            }

        val isColumn = direction == "column" || direction == "column-reverse"
        var left = rawBounds.left
        var top = rawBounds.top

        if (!isColumn && (justify == "center" || justify == "space-around" || justify == "space-evenly")) {
            left = centered(rawBounds.left, parentWidth, rawBounds.width)
        } else if (isColumn && (justify == "center" || justify == "space-around" || justify == "space-evenly")) {
            top = centered(rawBounds.top, parentHeight, rawBounds.height)
        }

        if (!isColumn && (align == "center" || align == "space-around" || align == "space-evenly")) {
            top = centered(rawBounds.top, parentHeight, rawBounds.height)
        } else if (isColumn && (align == "center" || align == "space-around" || align == "space-evenly")) {
            left = centered(rawBounds.left, parentWidth, rawBounds.width)
        }

        return rawBounds.copy(left = left, top = top)
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
        parentHeight: Float
    ): Map<DomNode, ComputedBoxBounds> {
        val result = mutableMapOf<DomNode, ComputedBoxBounds>()
        val children = parentNode.children
        if (parentStyle["display"]?.trim()?.lowercase() != "flex") {
            for (child in children) {
                val childStyle = CssCascadeResolver.computeStyle(child, stylesheet).base
                result[child] = GeometryParser.computeBoxBounds(childStyle, parentWidth, parentHeight)
            }
            return result
        }

        val direction = parentStyle["flex-direction"]?.trim()?.lowercase() ?: "row"
        val isColumn = direction == "column" || direction == "column-reverse"
        val isReverse = direction == "row-reverse" || direction == "column-reverse"
        val justify = parentStyle["justify-content"]?.trim()?.lowercase() ?: "flex-start"
        val align = parentStyle["align-items"]?.trim()?.lowercase() ?: "stretch"

        val pad = GeometryParser.parseInset(parentStyle["padding"], parentWidth, parentHeight)
        val pTop = GeometryParser.parsePixelOrPercent(parentStyle["padding-top"], parentHeight, pad?.top ?: 0f)
        val pBottom = GeometryParser.parsePixelOrPercent(parentStyle["padding-bottom"], parentHeight, pad?.bottom ?: 0f)
        val pLeft = GeometryParser.parsePixelOrPercent(parentStyle["padding-left"], parentWidth, pad?.left ?: 0f)
        val pRight = GeometryParser.parsePixelOrPercent(parentStyle["padding-right"], parentWidth, pad?.right ?: 0f)

        val gapStr = parentStyle["gap"] ?: (if (isColumn) parentStyle["row-gap"] else parentStyle["column-gap"])
        val gap = GeometryParser.parsePixelOrPercent(gapStr, if (isColumn) parentHeight else parentWidth, 0f)

        val inFlowList = mutableListOf<Pair<DomNode, ComputedBoxBounds>>()

        for (child in children) {
            val childStyle = CssCascadeResolver.computeStyle(child, stylesheet).base
            val isAbsolute = childStyle["position"]?.trim()?.lowercase() == "absolute" ||
                    childStyle.keys.any { it == "inset" || it == "left" || it == "right" || it == "top" || it == "bottom" }
            val rawBounds = GeometryParser.computeBoxBounds(childStyle, parentWidth, parentHeight)

            if (isAbsolute) {
                result[child] = rawBounds
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
                "center" -> (if (isColumn) pTop else pLeft) + availMain / 2f
                "flex-end" -> (if (isColumn) parentHeight - pBottom - totalMain else parentWidth - pRight - totalMain)
                "space-around" -> (if (isColumn) pTop else pLeft) + (availMain / (orderedList.size * 2f))
                "space-evenly" -> (if (isColumn) pTop else pLeft) + (availMain / (orderedList.size + 1f))
                else -> if (isColumn) pTop else pLeft
            }

            val extraSpacing = when (justify) {
                "space-between" -> if (orderedList.size > 1) availMain / (orderedList.size - 1) else 0f
                "space-around" -> availMain / orderedList.size
                "space-evenly" -> availMain / (orderedList.size + 1f)
                else -> 0f
            }

            for ((child, bounds) in orderedList) {
                val childW = bounds.width
                val childH = bounds.height

                val (cLeft, cTop) = if (isColumn) {
                    val top = currentMain
                    val left = when (align) {
                        "center" -> pLeft + (parentWidth - pLeft - pRight - childW).coerceAtLeast(0f) / 2f
                        "flex-end" -> parentWidth - pRight - childW
                        else -> pLeft
                    }
                    currentMain += childH + (if (justify.startsWith("space-")) extraSpacing else gap)
                    left to top
                } else {
                    val left = currentMain
                    val top = when (align) {
                        "center" -> pTop + (parentHeight - pTop - pBottom - childH).coerceAtLeast(0f) / 2f
                        "flex-end" -> parentHeight - pBottom - childH
                        else -> pTop
                    }
                    currentMain += childW + (if (justify.startsWith("space-")) extraSpacing else gap)
                    left to top
                }

                result[child] = ComputedBoxBounds(cLeft, cTop, childW, childH)
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
                "center" -> (if (isColumn) pLeft else pTop) + availCross / 2f
                "flex-end" -> (if (isColumn) parentWidth - pRight - totalCross else parentHeight - pBottom - totalCross)
                else -> if (isColumn) pLeft else pTop
            }

            for (line in finalLines) {
                val lineCrossSize = line.maxOfOrNull { if (isColumn) it.second.width else it.second.height } ?: 0f
                val lineTotalMain = line.sumOf { (if (isColumn) it.second.height else it.second.width).toDouble() }.toFloat() +
                        ((line.size - 1).coerceAtLeast(0) * gap)
                val lineAvailMain = (maxMainSize - lineTotalMain).coerceAtLeast(0f)

                var currentMain = when (justify) {
                    "center" -> (if (isColumn) pTop else pLeft) + lineAvailMain / 2f
                    "flex-end" -> (if (isColumn) parentHeight - pBottom - lineTotalMain else parentWidth - pRight - lineTotalMain)
                    "space-around" -> (if (isColumn) pTop else pLeft) + (lineAvailMain / (line.size * 2f))
                    "space-evenly" -> (if (isColumn) pTop else pLeft) + (lineAvailMain / (line.size + 1f))
                    else -> if (isColumn) pTop else pLeft
                }

                val extraSpacing = when (justify) {
                    "space-between" -> if (line.size > 1) lineAvailMain / (line.size - 1) else 0f
                    "space-around" -> lineAvailMain / line.size
                    "space-evenly" -> lineAvailMain / (line.size + 1f)
                    else -> 0f
                }

                for ((child, bounds) in line) {
                    val childW = bounds.width
                    val childH = bounds.height

                    val (cLeft, cTop) = if (isColumn) {
                        val top = currentMain
                        val left = when (align) {
                            "center" -> currentCross + (lineCrossSize - childW) / 2f
                            "flex-end" -> currentCross + lineCrossSize - childW
                            else -> currentCross
                        }
                        currentMain += childH + (if (justify.startsWith("space-")) extraSpacing else gap)
                        left to top
                    } else {
                        val left = currentMain
                        val top = when (align) {
                            "center" -> currentCross + (lineCrossSize - childH) / 2f
                            "flex-end" -> currentCross + lineCrossSize - childH
                            else -> currentCross
                        }
                        currentMain += childW + (if (justify.startsWith("space-")) extraSpacing else gap)
                        left to top
                    }

                    result[child] = ComputedBoxBounds(cLeft, cTop, childW, childH)
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
        rootH: Float
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
            val parentStyle = CssCascadeResolver.computeStyle(currentParent, stylesheet).base
            val flexBoundsMap = layoutFlexContainerChildren(currentParent, parentStyle, stylesheet, pW, pH)
            val b = flexBoundsMap[elem] ?: run {
                val elemStyle = CssCascadeResolver.computeStyle(elem, stylesheet).base
                val raw = GeometryParser.computeBoxBounds(elemStyle, pW, pH)
                val isParentFlex = parentStyle["display"]?.trim()?.lowercase() == "flex"
                val isParentAlignCenter = parentStyle["align-items"]?.trim()?.lowercase() == "center"
                val isParentJustifyCenter = parentStyle["justify-content"]?.trim()?.lowercase() == "center"
                val left = if (raw.left == 0f && (isParentJustifyCenter || elemStyle["margin"] == "auto" || (isParentFlex && !parentStyle.containsKey("justify-content")))) {
                    (pW - raw.width) / 2f
                } else raw.left
                val top = if (raw.top == 0f && (isParentAlignCenter || elemStyle["margin"] == "auto" || (isParentFlex && !parentStyle.containsKey("align-items")))) {
                    (pH - raw.height) / 2f
                } else raw.top
                raw.copy(left = left, top = top)
            }
            curX += b.left
            curY += b.top
            pW = b.width
            pH = b.height
            currentParent = elem
        }
        return Pair(curX + pW / 2f, curY + pH / 2f)
    }
}
