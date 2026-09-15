package com.sanket.tools.nexpad.nxprc.engine.compiler

import com.sanket.tools.nexpad.nxprc.*
import com.sanket.tools.nexpad.nxprc.engine.css.CssCascadeResolver
import com.sanket.tools.nexpad.nxprc.engine.css.CssTokenizer
import com.sanket.tools.nexpad.nxprc.engine.dom.DomNode
import com.sanket.tools.nexpad.nxprc.engine.dom.HtmlDomParser
import com.sanket.tools.nexpad.nxprc.engine.parsers.*

/**
 * High-Level Multiplatform Compiler: Transforms HTML/CSS/SVG markup into native .nxprc documents.
 * Orchestrates DOM node discovery, flex layout resolution, visual effects calculation,
 * recursive tree compilation, and document packaging.
 */
object NxprcCompiler {

    fun compile(
        html: String,
        id: String,
        name: String,
        category: String = "BUTTON",
        defaultControl: String = "A"
    ): NxprcDocument = compileWithWarnings(html, id, name, category, defaultControl).document

    fun compileWithWarnings(
        html: String,
        id: String,
        name: String,
        category: String = "BUTTON",
        defaultControl: String = "A"
    ): CompileResult {
        val warnings = CompileWarningCollector()
        val parsed = HtmlDomParser.parse(html)
        val stylesheet = CssTokenizer.parse(parsed.embeddedCss)

        // 0. Extract SVG Filter Graph Definitions
        val svgFilters = SvgFilterParser.parseFilterMap(parsed.root)

        val styleCache = mutableMapOf<com.sanket.tools.nexpad.nxprc.engine.dom.DomNode, com.sanket.tools.nexpad.nxprc.engine.css.ComputedElementStyle>()
        val resolvedNodeBounds = mutableMapOf<com.sanket.tools.nexpad.nxprc.engine.dom.DomNode, ComputedBoxBounds>()

        // 1. Find the Primary Gamepad Button Node
        val primaryNode = ButtonNodeSelector.findPrimaryButtonNode(parsed.root, stylesheet)
        val style = CssCascadeResolver.computeStyle(primaryNode, stylesheet, styleCache)

        val autoControl = primaryNode.attributes["data-control"]
            ?: primaryNode.attributes["data-key"]
            ?: parsed.root.attributes["data-control"]
            ?: parsed.root.attributes["data-key"]
            ?: defaultControl

        val autoCategory = primaryNode.attributes["data-category"]
            ?: parsed.root.attributes["data-category"]
            ?: if (primaryNode.classNames.any { it.contains("stick") || it.contains("joy") || it.contains("thumb") }) "JOYSTICK" else category

        val autoName = primaryNode.attributes["data-name"]
            ?: parsed.root.attributes["data-name"]
            ?: name

        val autoId = primaryNode.attributes["data-id"]
            ?: primaryNode.id
            ?: parsed.root.attributes["data-id"]
            ?: id

        val layerCollector = LayerCollector()

        val baseProps = style.base
        val beforeStyle = style.before
        val afterStyle = style.after

        // Scan for CSS properties the compiler cannot represent and emit warnings
        scanUnsupportedCssProps(baseProps, warnings)

        val buttonWidth = GeometryParser.parsePixelOrPercent(baseProps["width"], 100f, 100f).coerceAtLeast(1f)
        val buttonHeight = GeometryParser.parsePixelOrPercent(baseProps["height"], 100f, 100f).coerceAtLeast(1f)
        resolvedNodeBounds[primaryNode] = ComputedBoxBounds(0f, 0f, buttonWidth, buttonHeight)
        val baseOpacity = baseProps["opacity"]?.toFloatOrNull() ?: 1.0f
        val baseFilter = FilterParser.parse(baseProps["filter"] ?: primaryNode.attributes["filter"], svgFilters)

        // 2. Parse Outset Box Shadows & Socket Bezel (including SVG feDropShadow)
        val urlFilterMatch = Regex("""url\(['"]?#?([^'")]+)['"]?\)""").find(baseProps["filter"] ?: primaryNode.attributes["filter"] ?: "")
        val svgFilterRef = urlFilterMatch?.let { svgFilters[it.groupValues[1]] }
        val allBoxShadows = ShadowParser.parseBoxShadows(baseProps["box-shadow"]) + (svgFilterRef?.dropShadows ?: emptyList())
        val outsetShadows = allBoxShadows.filter { !it.isInset }
        val insetShadows = allBoxShadows.filter { it.isInset }

        val isRootTopOnly = baseProps["border"] == null && baseProps["border-top"] != null
        val border = GeometryParser.parseBorder(
            baseProps["border"] ?: baseProps["border-top"] ?: baseProps["border-width"],
            isTopOnly = isRootTopOnly
        )
        val radii = GeometryParser.parseBorderRadius(baseProps["border-radius"], defaultSizeDp = buttonWidth)

        val rootClip = GeometryParser.parseClipPath(baseProps["clip-path"] ?: baseProps["-webkit-clip-path"], buttonWidth, buttonHeight)
        val isOval = (radii.topLeft >= (buttonWidth * 0.35f) && radii.topRight >= (buttonWidth * 0.35f) &&
                      radii.bottomRight >= (buttonWidth * 0.35f) && radii.bottomLeft >= (buttonWidth * 0.35f)) ||
                      baseProps["border-radius"]?.contains("50%") == true
        val shapeType = when {
            rootClip != null -> rootClip.shapeType
            isOval -> LayerShapeType.OVAL.name
            else -> LayerShapeType.ROUNDED_RECT.name
        }
        val rootPolySides = rootClip?.polygonSides ?: 0
        val rootPathData = rootClip?.pathData ?: ""

        val hasExplicitBezel = primaryNode.attributes["data-bezel"] == "true" ||
                primaryNode.classNames.any { it.contains("bezel") || it.contains("socket") } ||
                (isOval && outsetShadows.any { it.spreadRadius >= 3f })

        val rawFills = GradientParser.parseAll(
            baseProps["background"] ?: baseProps["background-color"] ?: baseProps["fill"],
            baseProps["background-position"],
            baseProps["background-size"]
        )
        val bgColor = ColorParser.parse(baseProps["background-color"])
        val allFills = if (bgColor != null && bgColor != 0x00000000L && rawFills.none { it is FillBrush.Solid && it.color == bgColor }) {
            rawFills + FillBrush.Solid(bgColor)
        } else {
            rawFills
        }

        val isBoxPrimitive = primaryNode.attributes["data-primitive"] == "box" ||
                (!hasExplicitBezel && (allFills.size > 1 || allBoxShadows.size > 1 || rootClip != null || primaryNode.classNames.any {
                    it.contains("box") || it.contains("card") || it.contains("panel")
                }))

        // Drop shadow / Atmospheric Glow Layer (requires blur > 0 and bright non-dark color)
        val glowShadow = outsetShadows.firstOrNull { it.blurRadius > 0f && !ColorParser.isDark(it.color) }
        if (glowShadow != null) {
            layerCollector.addLayer(
                5,
                CanvasLayer.GlowRing(glowColor = glowShadow.color, blurRadius = glowShadow.blurRadius, pulseEnabled = true)
            )
        }

        val baseTransform = AnimationParser.parseTransforms(
            baseProps["transform"],
            baseProps["transform-origin"],
            buttonWidth,
            buttonHeight
        )

        val baseFilterDef = EffectsResolver.toFilterDef(baseFilter)
        val baseRotating = AnimationParser.isRotatingAnimation(stylesheet, baseProps)

        var surfaceSvgNode: DomNode? = null

        if (isBoxPrimitive) {
            val rootBox = BoxLayerBuilder.buildBoxLayer(
                shapeType = shapeType,
                polygonSides = rootPolySides,
                pathData = rootPathData,
                radii = radii,
                width = buttonWidth,
                height = buttonHeight,
                left = 0f,
                top = 0f,
                buttonWidth = buttonWidth,
                buttonHeight = buttonHeight,
                clipToBounds = baseProps["overflow"] == "hidden" || rootClip != null,
                fills = allFills,
                stroke = border,
                boxShadows = allBoxShadows,
                filterDef = baseFilterDef,
                opacity = baseOpacity,
                transform = baseTransform,
                isRotating = baseRotating,
                fallbackFill = FillBrush.Solid(NxprcDefaults.DEFAULT_FILL_COLOR),
                drawCacheHint = !baseRotating
            )
            layerCollector.addLayer(10, rootBox)
        } else {
            // Outer Bezel Socket Layer
            if (hasExplicitBezel) {
                val outerBezelColor = border?.color ?: 0xFF1C1D24L
                val ringShadow = outsetShadows.firstOrNull { it.spreadRadius > 0f }
                val strokeColor = ringShadow?.color ?: border?.color ?: 0xFF292A30L
                val primaryDarkShadow = outsetShadows.firstOrNull { ColorParser.isDark(it.color) }?.color ?: NxprcDefaults.DEFAULT_SHADOW_COLOR

                layerCollector.addLayer(
                    8,
                    CanvasLayer.BezelSocket(
                        outerBezelColor = outerBezelColor,
                        outerBevelStroke = strokeColor,
                        shadowColor = primaryDarkShadow
                    )
                )
            }

            // 3. Main Surface Background (SVG Shapes/Paths or Multi-layer Gradients)
            val paintServers = SvgGeometryParser.extractPaintServers(primaryNode, stylesheet)
            val directSurfaceChild = if (primaryNode.tag.equals("svg", ignoreCase = true)) {
                primaryNode
            } else if (allFills.isEmpty() && primaryNode.children.size == 1 && primaryNode.children[0].tag.equals("svg", ignoreCase = true)) {
                primaryNode.children[0]
            } else if (allFills.isEmpty()) {
                primaryNode.children.firstOrNull { it.tag.equals("svg", ignoreCase = true) && it.classNames.any { c -> c.contains("surface") || c.contains("bg") } }
            } else {
                null
            }

            surfaceSvgNode = directSurfaceChild
            val svgShapes = if (surfaceSvgNode != null) surfaceSvgNode.getAllSvgShapes(stylesheet, paintServers) else emptyList()

            if (svgShapes.isNotEmpty()) {
                svgShapes.forEachIndexed { index, shape ->
                    val resolvedFill = shape.fill ?: if (index == 0) allFills.firstOrNull() ?: FillBrush.Solid(NxprcDefaults.DEFAULT_FILL_COLOR) else FillBrush.Solid(0x00000000L)
                    val resolvedStroke = shape.stroke ?: border
                    layerCollector.addLayer(
                        10,
                        CanvasLayer.VectorPath(
                            pathData = shape.pathData,
                            fill = resolvedFill,
                            stroke = resolvedStroke,
                            isRotating = baseRotating
                        )
                    )
                }
            } else {
                allFills.reversed().forEachIndexed { index, fillBrush ->
                    val shape = BoxLayerBuilder.buildGradientShape(
                        shapeType = shapeType,
                        cornerRadius = radii.topLeft,
                        fill = fillBrush,
                        stroke = if (index == allFills.size - 1) border else null,
                        width = buttonWidth,
                        height = buttonHeight,
                        left = 0f,
                        top = 0f,
                        buttonWidth = buttonWidth,
                        buttonHeight = buttonHeight,
                        filterDef = baseFilterDef,
                        opacity = baseOpacity,
                        transform = baseTransform,
                        boxShadows = allBoxShadows,
                        hasMultipleFills = allFills.size > 1,
                        drawCacheHint = !baseRotating
                    )
                    layerCollector.addLayer(10, shape)
                }
            }

            // 4. Inset Shadows / Perimeter Groove
            val afterShadows = afterStyle?.get("box-shadow")?.let { ShadowParser.parseBoxShadows(it) } ?: emptyList()
            val afterInsets = afterShadows.filter { it.isInset }
            val combinedInset = (insetShadows + afterInsets)

            if (combinedInset.isNotEmpty()) {
                val darkInset = combinedInset.firstOrNull { ColorParser.isDark(it.color) }?.color ?: NxprcDefaults.DEFAULT_SHADOW_COLOR
                val lightInset = combinedInset.firstOrNull { !ColorParser.isDark(it.color) }?.color ?: 0x30FFFFFFL
                layerCollector.addLayer(15, CanvasLayer.InnerShadow(shadowColor = darkInset, highlightColor = lightInset, strokeWidth = 3.5f))
            }
        }

        // Text nodes
        val textNode = ButtonNodeSelector.findRealTextNode(primaryNode)
        val allTextNodes = ButtonNodeSelector.collectTextLeaves(primaryNode)

        // 4. ::before: Socket groove, inner recessed core, or radial depth
        if (beforeStyle != null) {
            val rawBeforeBgs = beforeStyle["background"]?.let {
                GradientParser.parseAll(it, beforeStyle["background-position"], beforeStyle["background-size"])
            } ?: emptyList()
            val beforeBgColor = ColorParser.parse(beforeStyle["background-color"])
            val beforeBgs = if (beforeBgColor != null && beforeBgColor != 0x00000000L && rawBeforeBgs.none { it is FillBrush.Solid && it.color == beforeBgColor }) {
                rawBeforeBgs + FillBrush.Solid(beforeBgColor)
            } else rawBeforeBgs

            val beforeOpacity = beforeStyle["opacity"]?.toFloatOrNull() ?: 1.0f
            val beforeFilter = FilterParser.parse(beforeStyle["filter"], svgFilters)
            val isBeforeTopOnly = beforeStyle["border"] == null && beforeStyle["border-top"] != null
            val beforeBorder = GeometryParser.parseBorder(
                beforeStyle["border"] ?: beforeStyle["border-top"] ?: beforeStyle["border-width"],
                isTopOnly = isBeforeTopOnly
            )
            val rawBeforeBounds = GeometryParser.computeBoxBounds(beforeStyle, buttonWidth, buttonHeight)
            val bounds = FlexLayoutEngine.resolvePositionedChildBounds(
                parentStyle = baseProps,
                childStyle = beforeStyle,
                rawBounds = rawBeforeBounds,
                parentWidth = buttonWidth,
                parentHeight = buttonHeight
            )
            val beforeWidth = bounds.width
            val beforeHeight = bounds.height
            val beforeLeft = bounds.left
            val beforeTop = bounds.top

            val beforeRadii = GeometryParser.parseBorderRadius(beforeStyle["border-radius"], defaultSizeDp = beforeWidth)
            val beforeClip = GeometryParser.parseClipPath(beforeStyle["clip-path"] ?: beforeStyle["-webkit-clip-path"], beforeWidth, beforeHeight)
            val isBeforeOval = beforeStyle["border-radius"]?.contains("50%") == true ||
                (beforeRadii.topLeft >= (beforeWidth * 0.35f) && beforeRadii.topRight >= (beforeWidth * 0.35f) &&
                 beforeRadii.bottomRight >= (beforeWidth * 0.35f) && beforeRadii.bottomLeft >= (beforeWidth * 0.35f))
            val beforeShape = when {
                beforeClip != null -> beforeClip.shapeType
                isBeforeOval -> "OVAL"
                else -> shapeType
            }
            val beforePolySides = beforeClip?.polygonSides ?: 0
            val beforePolyPath = beforeClip?.pathData ?: ""

            val beforeZ = GeometryParser.parseZIndex(beforeStyle)
            val beforeStack = 50 + beforeZ * 10

            val beforeTransform = AnimationParser.parseTransforms(
                beforeStyle["transform"],
                beforeStyle["transform-origin"],
                beforeWidth,
                beforeHeight
            )
            val beforeShadows = beforeStyle["box-shadow"]?.let { ShadowParser.parseBoxShadows(it) } ?: emptyList()
            val beforeFilterDef = EffectsResolver.toFilterDef(beforeFilter)

            if (isBoxPrimitive && (beforeBgs.isNotEmpty() || beforeBorder != null || beforeShadows.isNotEmpty())) {
                val beforeBox = BoxLayerBuilder.buildBoxLayer(
                    shapeType = beforeShape,
                    polygonSides = beforePolySides,
                    pathData = beforePolyPath,
                    radii = beforeRadii,
                    width = beforeWidth,
                    height = beforeHeight,
                    left = beforeLeft,
                    top = beforeTop,
                    buttonWidth = buttonWidth,
                    buttonHeight = buttonHeight,
                    clipToBounds = beforeStyle["overflow"] == "hidden" || beforeClip != null || baseProps["overflow"] == "hidden",
                    fills = beforeBgs,
                    stroke = beforeBorder,
                    boxShadows = beforeShadows,
                    filterDef = beforeFilterDef,
                    opacity = beforeOpacity,
                    transform = beforeTransform,
                    drawCacheHint = true
                )
                layerCollector.addLayer(beforeStack, beforeBox)
            } else {
                beforeBgs.reversed().forEachIndexed { index, bg ->
                    val shape = BoxLayerBuilder.buildGradientShape(
                        shapeType = beforeShape,
                        cornerRadius = beforeRadii.topLeft,
                        fill = bg,
                        stroke = if (index == beforeBgs.size - 1) beforeBorder else null,
                        width = beforeWidth,
                        height = beforeHeight,
                        left = beforeLeft,
                        top = beforeTop,
                        buttonWidth = buttonWidth,
                        buttonHeight = buttonHeight,
                        filterDef = beforeFilterDef,
                        opacity = beforeOpacity,
                        transform = beforeTransform,
                        boxShadows = beforeShadows,
                        hasMultipleFills = beforeBgs.size > 1,
                        drawCacheHint = true
                    )
                    layerCollector.addLayer(beforeStack, shape)
                }

                val beforeInsets = beforeShadows.filter { it.isInset }
                if (beforeInsets.isNotEmpty() && !isBoxPrimitive) {
                    val darkB = beforeInsets.firstOrNull { ColorParser.isDark(it.color) }?.color ?: NxprcDefaults.DEFAULT_SHADOW_COLOR
                    val lightB = beforeInsets.firstOrNull { !ColorParser.isDark(it.color) }?.color ?: 0x30FFFFFFL
                    layerCollector.addLayer(beforeStack + 2, CanvasLayer.InnerShadow(shadowColor = darkB, highlightColor = lightB, strokeWidth = 3.0f))
                }
            }
        }

        // 5. Recursive DOM Tree Compilation
        DomTreeCompiler.compileDomChildren(
            parentNode = primaryNode,
            parentWidth = buttonWidth,
            parentHeight = buttonHeight,
            parentGlobalX = 0f,
            parentGlobalY = 0f,
            isParentClipping = baseProps["overflow"] == "hidden" || rootClip != null,
            parentStackBase = 60,
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
            category = autoCategory
        )

        // 6. ::after: Top specular arc gloss & glass reflection edge
        if (afterStyle != null) {
            val rawAfterBgs = afterStyle["background"]?.let {
                GradientParser.parseAll(it, afterStyle["background-position"], afterStyle["background-size"])
            } ?: emptyList()
            val afterBgColor = ColorParser.parse(afterStyle["background-color"])
            val afterBgs = if (afterBgColor != null && afterBgColor != 0x00000000L && rawAfterBgs.none { it is FillBrush.Solid && it.color == afterBgColor }) {
                rawAfterBgs + FillBrush.Solid(afterBgColor)
            } else rawAfterBgs

            val afterOpacity = afterStyle["opacity"]?.toFloatOrNull() ?: 1.0f
            val afterFilter = FilterParser.parse(afterStyle["filter"], svgFilters)
            val rawAfterBounds = GeometryParser.computeBoxBounds(afterStyle, buttonWidth, buttonHeight)
            val bounds = FlexLayoutEngine.resolvePositionedChildBounds(
                parentStyle = baseProps,
                childStyle = afterStyle,
                rawBounds = rawAfterBounds,
                parentWidth = buttonWidth,
                parentHeight = buttonHeight
            )
            val afterWidth = bounds.width
            val afterHeight = bounds.height
            val afterLeft = bounds.left
            val afterTop = bounds.top

            val afterTransform = AnimationParser.parseTransforms(
                afterStyle["transform"],
                afterStyle["transform-origin"],
                afterWidth,
                afterHeight
            )
            val afterShadows = afterStyle["box-shadow"]?.let { ShadowParser.parseBoxShadows(it) } ?: emptyList()
            val isAfterTopOnly = afterStyle["border"] == null && afterStyle["border-top"] != null
            val afterBorder = GeometryParser.parseBorder(afterStyle["border"] ?: afterStyle["border-top"], isTopOnly = isAfterTopOnly)
            val afterRadii = GeometryParser.parseBorderRadius(afterStyle["border-radius"], defaultSizeDp = afterWidth)
            val afterClip = GeometryParser.parseClipPath(afterStyle["clip-path"] ?: afterStyle["-webkit-clip-path"], afterWidth, afterHeight)
            val isAfterOval = afterStyle["border-radius"]?.contains("50%") == true ||
                (afterRadii.topLeft >= (afterWidth * 0.35f) && afterRadii.topRight >= (afterWidth * 0.35f) &&
                 afterRadii.bottomRight >= (afterWidth * 0.35f) && afterRadii.bottomLeft >= (afterWidth * 0.35f))
            val afterShape = when {
                afterClip != null -> afterClip.shapeType
                isAfterOval -> "OVAL"
                else -> shapeType
            }
            val afterPolySides = afterClip?.polygonSides ?: 0
            val afterPolyPath = afterClip?.pathData ?: ""

            val afterZ = GeometryParser.parseZIndex(afterStyle)
            val afterStack = 70 + afterZ * 10

            val afterFilterDef = EffectsResolver.toFilterDef(afterFilter)

            if (afterBgs.isNotEmpty() || afterBorder != null || (isBoxPrimitive && afterShadows.isNotEmpty())) {
                val afterBox = BoxLayerBuilder.buildBoxLayer(
                    shapeType = afterShape,
                    polygonSides = afterPolySides,
                    pathData = afterPolyPath,
                    radii = afterRadii,
                    width = afterWidth,
                    height = afterHeight,
                    left = afterLeft,
                    top = afterTop,
                    buttonWidth = buttonWidth,
                    buttonHeight = buttonHeight,
                    clipToBounds = afterStyle["overflow"] == "hidden" || afterClip != null || baseProps["overflow"] == "hidden",
                    fills = afterBgs,
                    stroke = afterBorder,
                    boxShadows = afterShadows,
                    filterDef = afterFilterDef,
                    opacity = afterOpacity,
                    transform = afterTransform,
                    drawCacheHint = true
                )
                layerCollector.addLayer(afterStack, afterBox)
            } else if (afterBgs.isNotEmpty()) {
                afterBgs.reversed().forEachIndexed { index, bg ->
                    val shape = BoxLayerBuilder.buildGradientShape(
                        shapeType = shapeType,
                        cornerRadius = radii.topLeft,
                        fill = bg,
                        stroke = if (index == afterBgs.size - 1) afterBorder else null,
                        width = afterWidth,
                        height = afterHeight,
                        left = 0f,
                        top = 0f,
                        buttonWidth = buttonWidth,
                        buttonHeight = buttonHeight,
                        filterDef = afterFilterDef,
                        opacity = afterOpacity,
                        transform = afterTransform,
                        boxShadows = afterShadows,
                        hasMultipleFills = afterBgs.size > 1,
                        drawCacheHint = true
                    )
                    layerCollector.addLayer(afterStack, shape)
                }
            }
        }

        // 7. Center Text Label (Embossed 3D + Glow Text Shadows)
        var centerGlyphAdded = false
        if (textNode != null) {
            val textStyle = CssCascadeResolver.computeStyle(textNode, stylesheet, styleCache).base
            if (ButtonNodeSelector.isVisible(textStyle)) {
                val text = textNode.findFirstText() ?: defaultControl
                val textOpacity = textStyle["opacity"]?.toFloatOrNull() ?: baseOpacity
                val rawTextColor = ColorParser.parse(textStyle["color"] ?: baseProps["color"]) ?: 0xFFFFFFFFL
                val textColor = if (textOpacity < 1.0f) {
                    val a = (((rawTextColor shr 24) and 0xFF) * textOpacity).toInt().coerceIn(0, 255)
                    (a.toLong() shl 24) or (rawTextColor and 0x00FFFFFFL)
                } else rawTextColor

                val fontSize = GeometryParser.parseFontSize(textStyle["font-size"] ?: baseProps["font-size"])
                    ?: (buttonHeight * 0.40f)
                val textShadows = ShadowParser.parseTextShadows(textStyle["text-shadow"] ?: baseProps["text-shadow"])

                val darkTextShadow = textShadows.firstOrNull { ColorParser.isDark(it.color) }
                val lightTextHighlight = textShadows.firstOrNull { !ColorParser.isDark(it.color) }

                val textZ = GeometryParser.parseZIndex(textStyle)
                val textStack = 200 + textZ * 10

                val textBounds = resolvedNodeBounds[textNode]
                val (tcX, tcY) = if (textBounds != null) {
                    Pair(textBounds.left + textBounds.width / 2f, textBounds.top + textBounds.height / 2f)
                } else {
                    FlexLayoutEngine.computeNodeGlobalCenter(textNode, primaryNode, stylesheet, buttonWidth, buttonHeight, styleCache)
                }
                val offXRatio = (tcX - buttonWidth / 2f) / buttonWidth
                val offYRatio = (tcY - buttonHeight / 2f) / buttonHeight

                val lineResult = com.sanket.tools.nexpad.nxprc.engine.text.TextLineBreaker.breakLines(
                    text = text,
                    maxWidth = buttonWidth * 0.9f,
                    fontSizeSp = fontSize,
                    fontWeight = 700,
                    whiteSpace = textStyle["white-space"] ?: baseProps["white-space"],
                    wordBreak = textStyle["word-break"] ?: baseProps["word-break"]
                )

                if (lineResult.lines.size > 1) {
                    val totalH = lineResult.totalHeight
                    val startY = tcY - totalH / 2f + lineResult.lineHeight / 2f

                    lineResult.lines.forEachIndexed { lIdx, line ->
                        if (line.isNotEmpty()) {
                            val lineCenterY = startY + lIdx * lineResult.lineHeight
                            val lineOffYRatio = (lineCenterY - buttonHeight / 2f) / buttonHeight
                            layerCollector.addLayer(
                                textStack + lIdx,
                                CanvasLayer.TextLayer(
                                    text = line,
                                    fontSizeSp = fontSize,
                                    textColor = textColor,
                                    offsetXRatio = offXRatio,
                                    offsetYRatio = lineOffYRatio,
                                    textShadows = textShadows,
                                    maxLines = 1,
                                    lineHeightSp = lineResult.lineHeight,
                                    textAlign = textStyle["text-align"]?.uppercase() ?: "CENTER"
                                ),
                                isThumbCap = autoCategory.equals("JOYSTICK", ignoreCase = true)
                            )
                        }
                    }
                } else {
                    layerCollector.addLayer(
                        textStack,
                        CanvasLayer.CenterGlyph(
                            text = text,
                            fontSizeSp = fontSize,
                            textColor = textColor,
                            shadowColor = darkTextShadow?.color ?: NxprcDefaults.DEFAULT_SHADOW_COLOR,
                            shadowOffsetY = darkTextShadow?.offsetY ?: 2.5f,
                            highlightColor = lightTextHighlight?.color ?: NxprcDefaults.DEFAULT_HIGHLIGHT_COLOR,
                            textShadows = textShadows,
                            offsetXRatio = offXRatio,
                            offsetYRatio = offYRatio
                        ),
                        isThumbCap = autoCategory.equals("JOYSTICK", ignoreCase = true)
                    )
                }
                centerGlyphAdded = true
            }
        }

        // Fallback CenterGlyph if no child text node was found
        if (!centerGlyphAdded) {
            val explicitText = primaryNode.findFirstText()
            val hasChildElements = primaryNode.children.any { it.tag != "#text" && it.tag != "style" }
            val suppressFallbackText = autoCategory.equals("JOYSTICK", ignoreCase = true) ||
                ((autoCategory.equals("SYSTEM", ignoreCase = true) || autoCategory.equals("DPAD", ignoreCase = true)) && hasChildElements)

            val centerText = if (suppressFallbackText) {
                explicitText
            } else {
                explicitText ?: defaultControl
            }

            if (centerText != null) {
                val textColor = ColorParser.parse(baseProps["color"]) ?: 0xFFFFFFFFL
                val fontSize = GeometryParser.parseFontSize(baseProps["font-size"]) ?: (buttonHeight * 0.35f)
            val textShadows = ShadowParser.parseTextShadows(baseProps["text-shadow"])

            val darkTextShadow = textShadows.firstOrNull { ColorParser.isDark(it.color) }
            val lightTextHighlight = textShadows.firstOrNull { !ColorParser.isDark(it.color) }

            val lineResult = com.sanket.tools.nexpad.nxprc.engine.text.TextLineBreaker.breakLines(
                text = centerText,
                maxWidth = buttonWidth * 0.9f,
                fontSizeSp = fontSize,
                fontWeight = 700,
                whiteSpace = baseProps["white-space"],
                wordBreak = baseProps["word-break"]
            )

            if (lineResult.lines.size > 1) {
                val totalH = lineResult.totalHeight
                val startY = buttonHeight / 2f - totalH / 2f + lineResult.lineHeight / 2f

                lineResult.lines.forEachIndexed { lIdx, line ->
                    if (line.isNotEmpty()) {
                        val lineCenterY = startY + lIdx * lineResult.lineHeight
                        val lineOffYRatio = (lineCenterY - buttonHeight / 2f) / buttonHeight
                        layerCollector.addLayer(
                            200 + lIdx,
                            CanvasLayer.TextLayer(
                                text = line,
                                fontSizeSp = fontSize,
                                textColor = textColor,
                                offsetXRatio = 0f,
                                offsetYRatio = lineOffYRatio,
                                textShadows = textShadows,
                                maxLines = 1,
                                lineHeightSp = lineResult.lineHeight,
                                textAlign = baseProps["text-align"]?.uppercase() ?: "CENTER"
                            ),
                            isThumbCap = autoCategory.equals("JOYSTICK", ignoreCase = true)
                        )
                    }
                }
            } else {
                layerCollector.addLayer(
                    200,
                    CanvasLayer.CenterGlyph(
                        text = centerText,
                        fontSizeSp = fontSize,
                        textColor = textColor,
                        shadowColor = darkTextShadow?.color ?: NxprcDefaults.DEFAULT_SHADOW_COLOR,
                        shadowOffsetY = darkTextShadow?.offsetY ?: 2.5f,
                        highlightColor = lightTextHighlight?.color ?: NxprcDefaults.DEFAULT_HIGHLIGHT_COLOR,
                        textShadows = textShadows
                    ),
                    isThumbCap = autoCategory.equals("JOYSTICK", ignoreCase = true)
                )
            }
            }
        }

        // Touch Active Animations, State Micro-Physics & Idle Keyframes
        val activeTransform = AnimationParser.parseTransforms(style.active["transform"])
        val pressScale = if (activeTransform.scaleX != 1.0f) activeTransform.scaleX else 0.92f
        val pressOffsetY = activeTransform.translateY

        val dampingRatio = baseProps["--spring-damping"]?.toFloatOrNull()
            ?: primaryNode.attributes["data-damping"]?.toFloatOrNull()
            ?: stylesheet.customProperties["--spring-damping"]?.toFloatOrNull()
            ?: 0.75f

        val stiffness = baseProps["--spring-stiffness"]?.toFloatOrNull()
            ?: primaryNode.attributes["data-stiffness"]?.toFloatOrNull()
            ?: stylesheet.customProperties["--spring-stiffness"]?.toFloatOrNull()
            ?: 400f

        val finalPressScale = baseProps["--press-scale"]?.toFloatOrNull()
            ?: primaryNode.attributes["data-press-scale"]?.toFloatOrNull()
            ?: stylesheet.customProperties["--press-scale"]?.toFloatOrNull()
            ?: pressScale

        val springPhysics = SpringPhysicsDef(
            dampingRatio = dampingRatio,
            stiffness = stiffness,
            pressedScale = finalPressScale,
            enabled = primaryNode.attributes["data-physics"] != "none" && baseProps["--spring-enabled"] != "false"
        )

        val isRotating = AnimationParser.isRotatingAnimation(stylesheet, baseProps)
        val isPulsing = AnimationParser.isPulsingAnimation(stylesheet, baseProps)
        val isRgbCycle = AnimationParser.isRgbCycleAnimation(stylesheet, baseProps)
        val isShimmer = AnimationParser.isShimmerAnimation(stylesheet, baseProps)
        val svgAnim = AnimationParser.detectSvgAnimation(primaryNode) ?: AnimationParser.detectSvgAnimation(parsed.root)

        val cssTracks = AnimationParser.extractAnimationTracks(stylesheet, baseProps)
        val svgTracks = AnimationParser.extractSvgAnimationTracks(primaryNode).ifEmpty {
            AnimationParser.extractSvgAnimationTracks(parsed.root)
        }
        val allTracks = (cssTracks + svgTracks).distinctBy { it.property }

        val idleType = when {
            allTracks.any { it.property == com.sanket.tools.nexpad.nxprc.AnimatedProperty.HUE_ROTATE } -> com.sanket.tools.nexpad.nxprc.IdleAnimationType.RGB_CYCLE.name
            allTracks.any { it.property == com.sanket.tools.nexpad.nxprc.AnimatedProperty.ROTATION } -> com.sanket.tools.nexpad.nxprc.IdleAnimationType.ROTATE.name
            allTracks.any { it.property == com.sanket.tools.nexpad.nxprc.AnimatedProperty.SCALE || it.property == com.sanket.tools.nexpad.nxprc.AnimatedProperty.OPACITY } -> com.sanket.tools.nexpad.nxprc.IdleAnimationType.PULSE.name
            svgAnim != null -> svgAnim.name
            isRgbCycle -> com.sanket.tools.nexpad.nxprc.IdleAnimationType.RGB_CYCLE.name
            isRotating -> com.sanket.tools.nexpad.nxprc.IdleAnimationType.ROTATE.name
            isPulsing -> com.sanket.tools.nexpad.nxprc.IdleAnimationType.PULSE.name
            isShimmer -> com.sanket.tools.nexpad.nxprc.IdleAnimationType.SHIMMER.name
            allTracks.isNotEmpty() -> com.sanket.tools.nexpad.nxprc.IdleAnimationType.CUSTOM.name
            else -> com.sanket.tools.nexpad.nxprc.IdleAnimationType.NONE.name
        }

        val primaryClass = primaryNode.classNames.firstOrNull()?.replace("-", "_")
        val resolvedId = when {
            autoId.isNotBlank() && autoId != "rc.custom" -> if (autoId.startsWith("rc.")) autoId else "rc.$autoId"
            primaryClass != null -> "rc.$primaryClass"
            else -> "rc.custom_$autoControl"
        }

        val resolvedName = when {
            autoName.isNotBlank() && autoName != "Custom Button" -> autoName
            primaryClass != null -> primaryClass.split("_", "-").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            else -> "Custom $autoControl Button"
        }

        val overflow = baseProps["overflow"]?.trim()?.lowercase()
        val clipToBounds = overflow == "hidden" || rootClip != null

        val totalCanvasOutsets = EffectsResolver.computeTotalCanvasOutsets(layerCollector.getAllEntries())
        val (allSortedLayers, initialCapIndices) = layerCollector.getSortedLayersAndCapIndices()
        val capIndices = if (autoCategory.equals("JOYSTICK", ignoreCase = true)) {
            val hasCapShapes = initialCapIndices.any {
                val layer = allSortedLayers.getOrNull(it)
                layer is CanvasLayer.BoxLayer || layer is CanvasLayer.GradientShape
            }

            fun isConcentricDome(wRatio: Float, hRatio: Float, xRatio: Float, yRatio: Float): Boolean {
                val cx = xRatio + wRatio / 2f
                val cy = yRatio + hRatio / 2f
                return wRatio in 0.40f..0.75f && hRatio in 0.40f..0.75f &&
                        kotlin.math.abs(cx - 0.5f) <= 0.15f && kotlin.math.abs(cy - 0.5f) <= 0.15f
            }

            if (hasCapShapes) {
                // DOM tree compiler explicitly partitioned cap layers (e.g. <div class="stick-cap"> and children).
                // Maintain strict boundary: never re-classify base layers (ticks, bezel, socket, markers) into the cap.
                initialCapIndices.sorted()
            } else if (initialCapIndices.isNotEmpty()) {
                // Only text layers were marked as cap. Find concentric inner thumb dome shapes to move with the text.
                val capSet = initialCapIndices.toMutableSet()
                allSortedLayers.forEachIndexed { index, layer ->
                    val matches = when (layer) {
                        is CanvasLayer.BoxLayer -> isConcentricDome(layer.widthRatio, layer.heightRatio, layer.offsetXRatio, layer.offsetYRatio)
                        is CanvasLayer.GradientShape -> isConcentricDome(layer.widthRatio, layer.heightRatio, layer.offsetXRatio, layer.offsetYRatio)
                        else -> false
                    }
                    if (matches) capSet.add(index)
                }
                capSet.sorted()
            } else {
                // Fallback for documents with no explicit cap metadata at all:
                allSortedLayers.mapIndexedNotNull { index, layer ->
                    when (layer) {
                        is CanvasLayer.BoxLayer -> if (isConcentricDome(layer.widthRatio, layer.heightRatio, layer.offsetXRatio, layer.offsetYRatio)) index else null
                        is CanvasLayer.GradientShape -> if (isConcentricDome(layer.widthRatio, layer.heightRatio, layer.offsetXRatio, layer.offsetYRatio)) index else null
                        is CanvasLayer.CenterGlyph -> index
                        is CanvasLayer.TextLayer -> index
                        else -> null
                    }
                }
            }
        } else emptyList()

        val document = NxprcDocument(
            manifest = NxprcManifest(
                id = resolvedId,
                name = resolvedName,
                category = autoCategory.uppercase(),
                defaultControl = autoControl.uppercase(),
                widthDp = buttonWidth.toInt().coerceIn(NxprcDefaults.DEFAULT_MIN_SIZE_DP, NxprcDefaults.DEFAULT_MAX_SIZE_DP),
                heightDp = buttonHeight.toInt().coerceIn(NxprcDefaults.DEFAULT_MIN_SIZE_DP, NxprcDefaults.DEFAULT_MAX_SIZE_DP),
                description = "Compiled from HTML/CSS/SVG DOM Engine",
                springPhysics = springPhysics
            ),
            canvas = NxprcCanvas(
                viewBoxWidth = buttonWidth,
                viewBoxHeight = buttonHeight,
                layers = allSortedLayers,
                clipToBounds = clipToBounds,
                canvasOutsets = totalCanvasOutsets,
                capLayerIndices = capIndices
            ),
            animations = NxprcAnimations(
                idleType = idleType,
                idleDurationMs = allTracks.firstOrNull()?.durationMs ?: 2000,
                pressScale = finalPressScale,
                pressOffsetY = pressOffsetY,
                springStiffness = stiffness,
                springDamping = dampingRatio,
                enableGameRumble = true,
                rumbleIntensity = 1.0f,
                joystickSpringTension = if (autoCategory.equals("JOYSTICK", ignoreCase = true)) 800f else 750f,
                triggerMaxPullDepth = if (autoCategory.equals("TRIGGER", ignoreCase = true)) 16f else 12f,
                tracks = allTracks
            )
        )

        return CompileResult(document, warnings.build())
    }

    /**
     * Scans computed CSS properties for values the NXPRC engine cannot represent.
     * Emits DROPPED warnings for each unsupported property and warnings for lossy conversions.
     */
    private fun scanUnsupportedCssProps(props: Map<String, String>, w: CompileWarningCollector) {
        val droppedProps = mapOf(
            "mix-blend-mode" to "mix-blend-mode is not supported by the NXPRC renderer. Layer will render without blending.",
            "backdrop-filter" to "backdrop-filter is not supported. Use CSS filter: or SVG <filter> instead.",
            "animation" to "CSS @keyframes animations are not supported. Use --spring-stiffness/--spring-damping for physics, or SVG animations.",
            "transition" to "CSS transitions are not supported. The compiler uses spring physics for press interactions.",
            "mask" to "CSS mask is not supported. Use clip-path: polygon() or border-radius for shape masking.",
            "mask-image" to "CSS mask-image is not supported. Use clip-path: polygon() or border-radius.",
            "perspective" to "CSS 3D perspective transforms are not supported. Use 2D transform only."
        )
        droppedProps.forEach { (prop, msg) ->
            if (props.containsKey(prop)) w.dropped("UNSUPPORTED_CSS_PROPERTY", msg, prop)
        }

        if (props.containsKey("grid") || props["display"]?.trim()?.equals("grid", ignoreCase = true) == true) {
            w.dropped("UNSUPPORTED_CSS_PROPERTY", "CSS Grid layout is not supported. Use position:absolute with explicit px coordinates.", "grid")
        }

        // Warn on multi-function filter (only first function is parsed)
        val filterVal = props["filter"]
        if (filterVal != null && filterVal.contains(")") &&
            filterVal.indexOf(")") < filterVal.lastIndexOf("(")
        ) {
            w.warn(
                "FILTER_MULTI_FUNCTION",
                "Multiple CSS filter functions detected. Only the first supported function is compiled. Use SVG <filter> graphs for compound effects.",
                "filter: $filterVal"
            )
        }

        // Info on conic-gradient with many stops
        val bg = props["background"] ?: props["background-image"] ?: ""
        if (bg.contains("conic-gradient") && bg.count { it == ',' } > 8) {
            w.info(
                "CONIC_GRADIENT_MANY_STOPS",
                "conic-gradient with many color stops may reduce rendering performance. Consider an SVG radialGradient paint server instead.",
                "background"
            )
        }
    }
}
