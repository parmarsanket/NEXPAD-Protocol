package com.sanket.tools.nexpad.nxprc.engine.compiler

import com.sanket.tools.nexpad.nxprc.CanvasLayer
import com.sanket.tools.nexpad.nxprc.FillBrush
import com.sanket.tools.nexpad.nxprc.NxprcDefaults
import com.sanket.tools.nexpad.nxprc.engine.classifier.NodeRoleClassifier
import com.sanket.tools.nexpad.nxprc.engine.classifier.ShapeClassifier
import com.sanket.tools.nexpad.nxprc.engine.classifier.allIdentifierTokens
import com.sanket.tools.nexpad.nxprc.engine.css.CssCascadeResolver
import com.sanket.tools.nexpad.nxprc.engine.css.CssStylesheet
import com.sanket.tools.nexpad.nxprc.engine.dom.DomNode
import com.sanket.tools.nexpad.nxprc.engine.model.SizeMetrics
import com.sanket.tools.nexpad.nxprc.engine.stack.LayerStack
import com.sanket.tools.nexpad.nxprc.engine.parsers.*


/**
 * Compiles nested DOM element trees, child pseudo-elements (`::before`, `::after`),
 * and secondary text leaf layers into ordered [CanvasLayer] instances.
 */
internal object DomTreeCompiler {

    /**
     * Returns true if [node] belongs to the thumb-cap layer zone for joystick/touchpad buttons.
     *
     * Delegates to [NodeRoleClassifier] — replacing all previous inline `contains("base")`,
     * `contains("thumb")`, `contains("socket")` heuristics that were fragile and untestable.
     */
    fun isNodeThumbCap(node: DomNode, category: String): Boolean {
        if (!category.equals("JOYSTICK", ignoreCase = true) && !category.equals("TOUCHPAD", ignoreCase = true)) return false
        val nxprcCategory = com.sanket.tools.nexpad.nxprc.NxprcCategory.fromId(category)
            ?: com.sanket.tools.nexpad.nxprc.NxprcCategory.JOYSTICK
        return NodeRoleClassifier.classify(node, nxprcCategory) ==
               NodeRoleClassifier.NodeRole.THUMB_CAP
    }

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
        parentStackBase: Int = LayerStack.CONTENT_BASE + LayerStack.CHILD_OFFSET,
        buttonWidth: Float,
        buttonHeight: Float,
        baseProps: Map<String, String>,
        stylesheet: CssStylesheet,
        layerCollector: LayerCollector,
        textNode: DomNode?,
        allTextNodes: List<DomNode>,
        surfaceSvgNode: DomNode? = null,
        svgFilters: Map<String, ParsedSvgFilter> = emptyMap(),
        styleCache: MutableMap<DomNode, com.sanket.tools.nexpad.nxprc.engine.css.ComputedElementStyle>? = null,
        resolvedNodeBounds: MutableMap<DomNode, ComputedBoxBounds>? = null,
        category: String = "BUTTON"
    ) {
        val parentStyle = CssCascadeResolver.computeStyle(parentNode, stylesheet, styleCache).base
        val childBoundsMap = FlexLayoutEngine.layoutFlexContainerChildren(
            parentNode = parentNode,
            parentStyle = parentStyle,
            stylesheet = stylesheet,
            parentWidth = parentWidth,
            parentHeight = parentHeight,
            styleCache = styleCache
        )

        fun compilePseudoElement(
            node: DomNode,
            parentStyle: Map<String, String>,
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

            val rawPBounds = GeometryParser.computeBoxBounds(pseudoStyle, parentW, parentH)
            val pBounds = FlexLayoutEngine.resolvePositionedChildBounds(
                parentStyle = parentStyle,
                childStyle = pseudoStyle,
                rawBounds = rawPBounds,
                parentWidth = parentW,
                parentHeight = parentH
            )
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
            // ── ShapeClassifier replaces inline 0.35f oval check for pseudo-elements ────────────────
            val pShapeDescriptor = ShapeClassifier.classify(
                radii           = pRadii,
                width           = pWidth,
                height          = pHeight,
                borderRadiusCss = pseudoStyle["border-radius"],
                clipPath        = pClip
            )
            val pShape     = pShapeDescriptor.shapeTypeId
            val pPolySides = pShapeDescriptor.polygonSides
            val pPolyPath  = pShapeDescriptor.pathData

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
                val isPseudoCap = isNodeThumbCap(node, category) ||
                        ((category.equals("JOYSTICK", ignoreCase = true) || category.equals("TOUCHPAD", ignoreCase = true)) &&
                         node.tag.equals("button", ignoreCase = true) &&
                         (pWidth / buttonWidth) <= 0.68f && (pHeight / buttonHeight) <= 0.68f)
                layerCollector.addLayer(pStack, box, isThumbCap = isPseudoCap)
            }
        }

        for (child in parentNode.children) {
            val childComputed = CssCascadeResolver.computeStyle(child, stylesheet, styleCache)
            val childStyle = childComputed.base
            val isChildCap = isNodeThumbCap(child, category)

            val bounds = childBoundsMap[child] ?: run {
                val raw = GeometryParser.computeBoxBounds(childStyle, parentWidth, parentHeight)
                FlexLayoutEngine.resolvePositionedChildBounds(parentStyle, childStyle, raw, parentWidth, parentHeight)
            }
            val cWidth = bounds.width
            val cHeight = bounds.height
            val localLeft = bounds.left
            val localTop = bounds.top

            val globalX = parentGlobalX + localLeft
            val globalY = parentGlobalY + localTop

            resolvedNodeBounds?.put(child, ComputedBoxBounds(globalX, globalY, cWidth, cHeight))

            if (allTextNodes.size <= 1 && child == textNode) continue
            if (!ButtonNodeSelector.isVisible(childStyle)) continue

            val cOpacity = childStyle["opacity"]?.toFloatOrNull() ?: 1.0f
            val cFilter = FilterParser.parse(childStyle["filter"] ?: child.attributes["filter"], svgFilters)

            val childZ = GeometryParser.parseZIndex(childStyle)
            val childStack = LayerStack.childSlot(parentStackBase, childZ)

            // If child is an SVG element, extract its shapes directly into VectorPath layers
            if (child.tag.equals("svg", ignoreCase = true)) {
                if (surfaceSvgNode != null && child == surfaceSvgNode) continue
                val paintServers = SvgGeometryParser.extractPaintServers(child, stylesheet)
                val svgShapes = child.getAllSvgShapes(stylesheet, paintServers)
                if (svgShapes.isNotEmpty()) {
                    val scale = (maxOf(cWidth / buttonWidth, cHeight / buttonHeight)).coerceIn(0.05f, 2.0f)
                    val offX = globalX / buttonWidth
                    val offY = globalY / buttonHeight
                    val svgTransform = AnimationParser.parseTransforms(
                        childStyle["transform"],
                        childStyle["transform-origin"],
                        cWidth,
                        cHeight
                    )
                    val svgRotation = svgTransform.rotationDegrees
                    svgShapes.forEachIndexed { sIdx, shape ->
                        val resolvedFill = shape.fill ?: if (sIdx == 0 && shape.stroke == null) FillBrush.Solid(NxprcDefaults.DEFAULT_ACCENT_COLOR) else shape.fill ?: FillBrush.Solid(0x00000000L)
                        layerCollector.addLayer(
                            childStack + 1 + sIdx,
                            CanvasLayer.VectorPath(
                                pathData = shape.pathData,
                                fill = resolvedFill,
                                stroke = shape.stroke,
                                rotationDegrees = svgRotation,
                                offsetXRatio = offX,
                                offsetYRatio = offY,
                                scale = scale
                            ),
                            isThumbCap = isChildCap
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

            // ── ShapeClassifier replaces the inline 0.35f oval check for DOM children ─────────────
            val cShapeDescriptor = ShapeClassifier.classify(
                radii           = cRadii,
                width           = cWidth,
                height          = cHeight,
                borderRadiusCss = childStyle["border-radius"],
                clipPath        = cClip
            )
            val cShape    = cShapeDescriptor.shapeTypeId
            val cPolySides = cShapeDescriptor.polygonSides
            val cPolyPath  = cShapeDescriptor.pathData


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
                layerCollector.addLayer(childStack, box, isThumbCap = isChildCap)
            }

            // Compile child ::before pseudo-element
            compilePseudoElement(
                node = child,
                parentStyle = childStyle,
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

                val hasExplicitWidth = childStyle["width"] != null
                val hasExplicitHeight = childStyle["height"] != null
                val effectiveTextW = if (!hasExplicitWidth) {
                    val metrics = com.sanket.tools.nexpad.nxprc.engine.text.TextMetrics.measure(childText, tFontSize, tWeight)
                    metrics.width.coerceIn(tFontSize, cWidth)
                } else cWidth
                val effectiveTextH = if (!hasExplicitHeight) {
                    tFontSize * 1.2f
                } else cHeight

                if (lineResult.lines.size > 1) {
                    val baseCenterY = globalY + effectiveTextH / 2f
                    val totalH = lineResult.totalHeight
                    val startY = baseCenterY - totalH / 2f + lineResult.lineHeight / 2f

                    lineResult.lines.forEachIndexed { lIdx, line ->
                        if (line.isNotEmpty()) {
                            val lineCenterY = startY + lIdx * lineResult.lineHeight
                            val offXRatio = (globalX + effectiveTextW / 2f - buttonWidth / 2f) / buttonWidth
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
                                ),
                                isThumbCap = isChildCap
                            )
                        }
                    }
                } else {
                    val childCenterX = globalX + effectiveTextW / 2f
                    val childCenterY = globalY + effectiveTextH / 2f
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
                        ),
                        isThumbCap = isChildCap
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
                    surfaceSvgNode = surfaceSvgNode,
                    svgFilters = svgFilters,
                    styleCache = styleCache,
                    resolvedNodeBounds = resolvedNodeBounds,
                    category = category
                )
            }

            // Compile child ::after pseudo-element
            compilePseudoElement(
                node = child,
                parentStyle = childStyle,
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
