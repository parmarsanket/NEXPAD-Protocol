package com.sanket.tools.nexpad.nxprc.engine.compiler

import com.sanket.tools.nexpad.nxprc.CanvasLayer
import com.sanket.tools.nexpad.nxprc.FillBrush
import com.sanket.tools.nexpad.nxprc.LayerShapeType
import com.sanket.tools.nexpad.nxprc.NxprcDefaults
import com.sanket.tools.nexpad.nxprc.engine.css.CssCascadeResolver
import com.sanket.tools.nexpad.nxprc.engine.css.CssStylesheet
import com.sanket.tools.nexpad.nxprc.engine.dom.DomNode
import com.sanket.tools.nexpad.nxprc.engine.parsers.*

/**
 * Compiles nested DOM element trees, child pseudo-elements (`::before`, `::after`),
 * and secondary text leaf layers into ordered [CanvasLayer] instances.
 */
internal object DomTreeCompiler {

    /**
     * Recursively traverses and compiles children of [parentNode] into canvas layers,
     * computing absolute global canvas coordinates, flexbox positioning, and CSS stacking order.
     */
    fun compileDomChildren(
        parentNode: DomNode,
        parentWidth: Float,
        parentHeight: Float,
        parentGlobalX: Float,
        parentGlobalY: Float,
        isParentClipping: Boolean = false,
        parentStackBase: Int = 60,
        buttonWidth: Float,
        buttonHeight: Float,
        baseProps: Map<String, String>,
        stylesheet: CssStylesheet,
        layerCollector: LayerCollector,
        textNode: DomNode?,
        allTextNodes: List<DomNode>,
        hasSurfaceSvg: Boolean = false,
        svgFilters: Map<String, ParsedSvgFilter> = emptyMap()
    ) {
        val parentStyle = CssCascadeResolver.computeStyle(parentNode, stylesheet).base
        val childBoundsMap = FlexLayoutEngine.layoutFlexContainerChildren(
            parentNode = parentNode,
            parentStyle = parentStyle,
            stylesheet = stylesheet,
            parentWidth = parentWidth,
            parentHeight = parentHeight
        )

        fun compilePseudoElement(
            pseudoStyle: Map<String, String>?,
            isBefore: Boolean,
            parentGlobalX: Float,
            parentGlobalY: Float,
            parentW: Float,
            parentH: Float,
            parentStack: Int,
            isParentClipping: Boolean
        ) {
            if (pseudoStyle == null || !ButtonNodeSelector.isVisible(pseudoStyle)) return

            val pOpacity = pseudoStyle["opacity"]?.toFloatOrNull() ?: 1.0f
            val pFilter = FilterParser.parse(pseudoStyle["filter"], svgFilters)

            val pBounds = GeometryParser.computeBoxBounds(pseudoStyle, parentW, parentH)
            val pWidth = pBounds.width
            val pHeight = pBounds.height
            val pLocalLeft = pBounds.left
            val pLocalTop = pBounds.top

            val pGlobalX = parentGlobalX + pLocalLeft
            val pGlobalY = parentGlobalY + pLocalTop

            val pRadii = GeometryParser.parseBorderRadius(pseudoStyle["border-radius"], defaultSizeDp = pWidth)
            val isPseudoTopOnly = pseudoStyle["border"] == null && pseudoStyle["border-top"] != null
            val pBorder = GeometryParser.parseBorder(
                pseudoStyle["border"] ?: pseudoStyle["border-top"] ?: pseudoStyle["border-width"],
                isTopOnly = isPseudoTopOnly
            )
            val pClip = GeometryParser.parseClipPath(pseudoStyle["clip-path"] ?: pseudoStyle["-webkit-clip-path"], pWidth, pHeight)
            val isPOval = pseudoStyle["border-radius"]?.contains("50%") == true ||
                (pRadii.topLeft >= (pWidth * 0.35f) && pRadii.topRight >= (pWidth * 0.35f) &&
                 pRadii.bottomRight >= (pWidth * 0.35f) && pRadii.bottomLeft >= (pWidth * 0.35f))
            val pShape = when {
                pClip != null -> pClip.shapeType
                isPOval -> LayerShapeType.OVAL.name
                else -> LayerShapeType.ROUNDED_RECT.name
            }
            val pPolySides = pClip?.polygonSides ?: 0
            val pPolyPath = pClip?.pathData ?: ""

            val pZ = GeometryParser.parseZIndex(pseudoStyle)
            val pStack = parentStack + (if (isBefore) 1 else 2) + pZ * 10

            val pBg = pseudoStyle["background"] ?: pseudoStyle["background-color"]
            val pFills = if (pBg != null) {
                GradientParser.parseAll(
                    pBg,
                    pseudoStyle["background-position"],
                    pseudoStyle["background-size"]
                )
            } else emptyList()

            val pShadows = ShadowParser.parseBoxShadows(pseudoStyle["box-shadow"])
            val pTransform = AnimationParser.parseTransforms(
                pseudoStyle["transform"],
                pseudoStyle["transform-origin"],
                pWidth,
                pHeight
            )
            val clipPseudo = pseudoStyle["overflow"] == "hidden" || pClip != null || isParentClipping

            if (pFills.isNotEmpty() || pBorder != null || pShadows.isNotEmpty()) {
                val pFilterDef = EffectsResolver.toFilterDef(pFilter)
                val box = BoxLayerBuilder.buildBoxLayer(
                    shapeType = pShape,
                    polygonSides = pPolySides,
                    pathData = pPolyPath,
                    radii = pRadii,
                    width = pWidth,
                    height = pHeight,
                    left = pGlobalX,
                    top = pGlobalY,
                    buttonWidth = buttonWidth,
                    buttonHeight = buttonHeight,
                    clipToBounds = clipPseudo,
                    fills = pFills,
                    stroke = pBorder,
                    boxShadows = pShadows,
                    filterDef = pFilterDef,
                    opacity = pOpacity,
                    transform = pTransform
                )
                layerCollector.addLayer(pStack, box)
            }
        }

        for (child in parentNode.children) {
            if (allTextNodes.size <= 1 && child == textNode) continue

            val childComputed = CssCascadeResolver.computeStyle(child, stylesheet)
            val childStyle = childComputed.base
            if (!ButtonNodeSelector.isVisible(childStyle)) continue

            val cOpacity = childStyle["opacity"]?.toFloatOrNull() ?: 1.0f
            val cFilter = FilterParser.parse(childStyle["filter"] ?: child.attributes["filter"], svgFilters)

            val bounds = childBoundsMap[child] ?: GeometryParser.computeBoxBounds(childStyle, parentWidth, parentHeight)
            val cWidth = bounds.width
            val cHeight = bounds.height
            val localLeft = bounds.left
            val localTop = bounds.top

            val globalX = parentGlobalX + localLeft
            val globalY = parentGlobalY + localTop

            val childZ = GeometryParser.parseZIndex(childStyle)
            val childStack = parentStackBase + childZ * 10

            // If child is an SVG element, extract its shapes directly into VectorPath layers
            if (child.tag.equals("svg", ignoreCase = true)) {
                if (hasSurfaceSvg) continue
                val svgShapes = child.getAllSvgShapes()
                if (svgShapes.isNotEmpty()) {
                    val scale = (minOf(cWidth / buttonWidth, cHeight / buttonHeight)).coerceIn(0.05f, 2.0f)
                    val offX = (globalX + cWidth / 2f - buttonWidth / 2f) / buttonWidth
                    val offY = (globalY + cHeight / 2f - buttonHeight / 2f) / buttonHeight
                    svgShapes.forEachIndexed { sIdx, shape ->
                        val resolvedFill = shape.fill ?: if (sIdx == 0 && shape.stroke == null) FillBrush.Solid(NxprcDefaults.DEFAULT_ACCENT_COLOR) else shape.fill ?: FillBrush.Solid(0x00000000L)
                        layerCollector.addLayer(
                            childStack + 50 + sIdx,
                            CanvasLayer.VectorPath(
                                pathData = shape.pathData,
                                fill = resolvedFill,
                                stroke = shape.stroke,
                                offsetXRatio = offX,
                                offsetYRatio = offY,
                                scale = scale
                            )
                        )
                    }
                }
                continue
            }

            val cRadii = GeometryParser.parseBorderRadius(childStyle["border-radius"], defaultSizeDp = cWidth)
            val isChildTopOnly = childStyle["border"] == null && childStyle["border-top"] != null
            val cBorder = GeometryParser.parseBorder(
                childStyle["border"] ?: childStyle["border-top"] ?: childStyle["border-width"],
                isTopOnly = isChildTopOnly
            )
            val cClip = GeometryParser.parseClipPath(childStyle["clip-path"] ?: childStyle["-webkit-clip-path"], cWidth, cHeight)
            val isCOval = childStyle["border-radius"]?.contains("50%") == true ||
                (cRadii.topLeft >= (cWidth * 0.35f) && cRadii.topRight >= (cWidth * 0.35f) &&
                 cRadii.bottomRight >= (cWidth * 0.35f) && cRadii.bottomLeft >= (cWidth * 0.35f))
            val cShape = when {
                cClip != null -> cClip.shapeType
                isCOval -> LayerShapeType.OVAL.name
                else -> LayerShapeType.ROUNDED_RECT.name
            }
            val cPolySides = cClip?.polygonSides ?: 0
            val cPolyPath = cClip?.pathData ?: ""

            val cBg = childStyle["background"] ?: childStyle["background-color"]
            val cFills = if (cBg != null) {
                GradientParser.parseAll(
                    cBg,
                    childStyle["background-position"],
                    childStyle["background-size"]
                )
            } else emptyList()

            val cShadows = ShadowParser.parseBoxShadows(childStyle["box-shadow"])
            val cTransform = AnimationParser.parseTransforms(
                childStyle["transform"],
                childStyle["transform-origin"],
                cWidth,
                cHeight
            )
            val clipChild = childStyle["overflow"] == "hidden" || cClip != null || isParentClipping

            if (cFills.isNotEmpty() || cBorder != null || cShadows.isNotEmpty()) {
                val cFilterDef = EffectsResolver.toFilterDef(cFilter)
                val box = BoxLayerBuilder.buildBoxLayer(
                    shapeType = cShape,
                    polygonSides = cPolySides,
                    pathData = cPolyPath,
                    radii = cRadii,
                    width = cWidth,
                    height = cHeight,
                    left = globalX,
                    top = globalY,
                    buttonWidth = buttonWidth,
                    buttonHeight = buttonHeight,
                    clipToBounds = clipChild,
                    fills = cFills,
                    stroke = cBorder,
                    boxShadows = cShadows,
                    filterDef = cFilterDef,
                    opacity = cOpacity,
                    transform = cTransform
                )
                layerCollector.addLayer(childStack, box)
            }

            // Compile child ::before pseudo-element
            compilePseudoElement(
                pseudoStyle = childComputed.before,
                isBefore = true,
                parentGlobalX = globalX,
                parentGlobalY = globalY,
                parentW = cWidth,
                parentH = cHeight,
                parentStack = childStack,
                isParentClipping = clipChild
            )

            // If this element has direct text content and multiple text nodes exist in the component, emit TextLayer
            val childText = child.findFirstText()
            if (childText != null && allTextNodes.size > 1 && child.children.none { it.findFirstText() != null }) {
                val tColor = ColorParser.parse(childStyle["color"] ?: baseProps["color"]) ?: 0xFFFFFFFFL
                val tFontSize = GeometryParser.parseFontSize(childStyle["font-size"] ?: baseProps["font-size"]) ?: 14f
                val tWeight = childStyle["font-weight"]?.toIntOrNull() ?: if (childStyle["font-weight"]?.contains("bold", true) == true) 700 else 400
                val tShadows = ShadowParser.parseTextShadows(childStyle["text-shadow"])
                val tAlign = childStyle["text-align"]?.trim()?.uppercase() ?: "CENTER"

                val lineResult = com.sanket.tools.nexpad.nxprc.engine.text.TextLineBreaker.breakLines(
                    text = childText,
                    maxWidth = cWidth,
                    fontSizeSp = tFontSize,
                    fontWeight = tWeight,
                    whiteSpace = childStyle["white-space"],
                    wordBreak = childStyle["word-break"]
                )

                if (lineResult.lines.size > 1) {
                    val baseCenterY = globalY + cHeight / 2f
                    val totalH = lineResult.totalHeight
                    val startY = baseCenterY - totalH / 2f + lineResult.lineHeight / 2f

                    lineResult.lines.forEachIndexed { lIdx, line ->
                        if (line.isNotEmpty()) {
                            val lineCenterY = startY + lIdx * lineResult.lineHeight
                            val offXRatio = (globalX + cWidth / 2f - buttonWidth / 2f) / buttonWidth
                            val offYRatio = (lineCenterY - buttonHeight / 2f) / buttonHeight

                            layerCollector.addLayer(
                                childStack + 150 + lIdx,
                                CanvasLayer.TextLayer(
                                    text = line,
                                    fontSizeSp = tFontSize,
                                    fontWeight = tWeight,
                                    textColor = tColor,
                                    offsetXRatio = offXRatio,
                                    offsetYRatio = offYRatio,
                                    textShadows = tShadows,
                                    maxLines = 1,
                                    lineHeightSp = lineResult.lineHeight,
                                    textAlign = tAlign
                                )
                            )
                        }
                    }
                } else {
                    val childCenterX = globalX + cWidth / 2f
                    val childCenterY = globalY + cHeight / 2f
                    val offXRatio = (childCenterX - buttonWidth / 2f) / buttonWidth
                    val offYRatio = (childCenterY - buttonHeight / 2f) / buttonHeight

                    layerCollector.addLayer(
                        childStack + 150,
                        CanvasLayer.TextLayer(
                            text = childText,
                            fontSizeSp = tFontSize,
                            fontWeight = tWeight,
                            textColor = tColor,
                            offsetXRatio = offXRatio,
                            offsetYRatio = offYRatio,
                            textShadows = tShadows,
                            maxLines = 1,
                            lineHeightSp = lineResult.lineHeight,
                            textAlign = tAlign
                        )
                    )
                }
            }

            if (child.children.isNotEmpty()) {
                compileDomChildren(
                    parentNode = child,
                    parentWidth = cWidth,
                    parentHeight = cHeight,
                    parentGlobalX = globalX,
                    parentGlobalY = globalY,
                    isParentClipping = clipChild,
                    parentStackBase = childStack,
                    buttonWidth = buttonWidth,
                    buttonHeight = buttonHeight,
                    baseProps = baseProps,
                    stylesheet = stylesheet,
                    layerCollector = layerCollector,
                    textNode = textNode,
                    allTextNodes = allTextNodes,
                    hasSurfaceSvg = hasSurfaceSvg,
                    svgFilters = svgFilters
                )
            }

            // Compile child ::after pseudo-element
            compilePseudoElement(
                pseudoStyle = childComputed.after,
                isBefore = false,
                parentGlobalX = globalX,
                parentGlobalY = globalY,
                parentW = cWidth,
                parentH = cHeight,
                parentStack = childStack,
                isParentClipping = clipChild
            )
        }
    }
}
