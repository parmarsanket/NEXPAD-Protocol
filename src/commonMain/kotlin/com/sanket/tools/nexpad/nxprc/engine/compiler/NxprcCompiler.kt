package com.sanket.tools.nexpad.nxprc.engine.compiler

import com.sanket.tools.nexpad.nxprc.*
import com.sanket.tools.nexpad.nxprc.engine.css.CssCascadeResolver
import com.sanket.tools.nexpad.nxprc.engine.css.CssSelector
import com.sanket.tools.nexpad.nxprc.engine.css.CssStylesheet
import com.sanket.tools.nexpad.nxprc.engine.css.CssTokenizer
import com.sanket.tools.nexpad.nxprc.engine.dom.DomNode
import com.sanket.tools.nexpad.nxprc.engine.dom.HtmlDomParser
import com.sanket.tools.nexpad.nxprc.engine.parsers.*
import java.util.regex.Pattern

private data class LayerEntry(val stackIndex: Int, val order: Int, val layer: CanvasLayer)

/**
 * High-Level Multiplatform Compiler: Transforms HTML/CSS/SVG markup into native .nxprc documents.
 */
object NxprcCompiler {

    fun compile(
        html: String,
        id: String,
        name: String,
        category: String = "BUTTON",
        defaultControl: String = "A"
    ): NxprcDocument {
        val parsed = HtmlDomParser.parse(html)
        val stylesheet = CssTokenizer.parse(parsed.embeddedCss)

        // 1. Find the Primary Gamepad Button Node
        val primaryNode = findPrimaryButtonNode(parsed.root, stylesheet)
        val style = CssCascadeResolver.computeStyle(primaryNode, stylesheet)

        val layerEntries = mutableListOf<LayerEntry>()
        var layerOrderSeq = 0
        fun addLayer(stackIndex: Int, layer: CanvasLayer) {
            layerEntries.add(LayerEntry(stackIndex, layerOrderSeq++, layer))
        }

        val baseProps = style.base
        val beforeStyle = style.before
        val afterStyle = style.after

        val buttonWidth = GeometryParser.parsePixelOrPercent(baseProps["width"], 100f, 96f)
        val buttonHeight = GeometryParser.parsePixelOrPercent(baseProps["height"], 100f, 96f)
        val baseOpacity = baseProps["opacity"]?.toFloatOrNull() ?: 1.0f
        val baseFilter = FilterParser.parse(baseProps["filter"])

        // 2. Parse Outset Box Shadows & Socket Bezel
        val allBoxShadows = ShadowParser.parseBoxShadows(baseProps["box-shadow"])
        val outsetShadows = allBoxShadows.filter { !it.isInset }
        val insetShadows = allBoxShadows.filter { it.isInset }

        val border = GeometryParser.parseBorder(baseProps["border"] ?: baseProps["border-top"] ?: baseProps["border-width"])
        val radii = GeometryParser.parseBorderRadius(baseProps["border-radius"], defaultSizeDp = buttonWidth)

        val rootClip = GeometryParser.parseClipPath(baseProps["clip-path"] ?: baseProps["-webkit-clip-path"], buttonWidth, buttonHeight)
        val isOval = (radii.topLeft >= (buttonWidth * 0.35f) && radii.topRight >= (buttonWidth * 0.35f) &&
                      radii.bottomRight >= (buttonWidth * 0.35f) && radii.bottomLeft >= (buttonWidth * 0.35f)) ||
                      baseProps["border-radius"]?.contains("50%") == true
        val shapeType = when {
            rootClip != null -> rootClip.shapeType
            isOval -> "OVAL"
            else -> "ROUNDED_RECT"
        }
        val rootPolySides = rootClip?.polygonSides ?: 0
        val rootPathData = rootClip?.pathData ?: ""

        val hasExplicitBezel = primaryNode.attributes["data-bezel"] == "true" ||
                primaryNode.classNames.any { it.contains("bezel") || it.contains("socket") } ||
                (isOval && outsetShadows.any { it.spreadRadius > 0f })

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
            addLayer(5, CanvasLayer.GlowRing(glowColor = glowShadow.color, blurRadius = glowShadow.blurRadius, pulseEnabled = true))
        }

        val baseTransform = AnimationParser.parseTransforms(
            baseProps["transform"],
            baseProps["transform-origin"],
            buttonWidth,
            buttonHeight
        )

        if (isBoxPrimitive) {
            addLayer(
                10,
                CanvasLayer.BoxLayer(
                    shapeType = shapeType,
                    polygonSides = rootPolySides,
                    pathData = rootPathData,
                    cornerRadiusTopLeft = radii.topLeft,
                    cornerRadiusTopRight = radii.topRight,
                    cornerRadiusBottomRight = radii.bottomRight,
                    cornerRadiusBottomLeft = radii.bottomLeft,
                    widthRatio = 1.0f,
                    heightRatio = 1.0f,
                    clipToBounds = baseProps["overflow"] == "hidden" || rootClip != null,
                    fill = allFills.firstOrNull() ?: FillBrush.Solid(0xFF0A192FL),
                    fills = allFills.reversed(),
                    stroke = border,
                    boxShadows = allBoxShadows,
                    opacity = baseOpacity,
                    rotationDegrees = baseTransform.rotationDegrees,
                    offsetXRatio = baseTransform.translateX / buttonWidth,
                    offsetYRatio = baseTransform.translateY / buttonHeight,
                    scaleX = baseTransform.scaleX,
                    scaleY = baseTransform.scaleY,
                    skewX = baseTransform.skewX,
                    skewY = baseTransform.skewY,
                    originXRatio = baseTransform.originXRatio,
                    originYRatio = baseTransform.originYRatio,
                    isRotating = AnimationParser.isRotatingAnimation(stylesheet, baseProps)
                )
            )
        } else {
            // Outer Bezel Socket Layer (only if explicitly requested via data-bezel or explicit #1c1d24 casing ring)
            if (hasExplicitBezel) {
                val outerBezelColor = border?.color ?: 0xFF1C1D24L
                val ringShadow = outsetShadows.firstOrNull { it.spreadRadius > 0f }
                val strokeColor = ringShadow?.color ?: border?.color ?: 0xFF292A30L
                val primaryDarkShadow = outsetShadows.firstOrNull { ColorParser.isDark(it.color) }?.color ?: 0x73000000L

                addLayer(
                    8,
                    CanvasLayer.BezelSocket(
                        outerBezelColor = outerBezelColor,
                        outerBevelStroke = strokeColor,
                        shadowColor = primaryDarkShadow
                    )
                )
            }

            // 3. Main Surface Background (SVG Paths or Multi-layer Gradients)
            val svgPaths = primaryNode.getAllSvgPaths().ifEmpty { parsed.root.getAllSvgPaths() }

            if (svgPaths.isNotEmpty()) {
                svgPaths.forEachIndexed { index, path ->
                    addLayer(
                        10,
                        CanvasLayer.VectorPath(
                            pathData = path,
                            fill = if (index == 0) allFills.first() else FillBrush.Solid(0x00000000L),
                            stroke = border,
                            isRotating = AnimationParser.isRotatingAnimation(stylesheet, baseProps)
                        )
                    )
                }
            } else {
                // Paint bottom-to-top (reversed from CSS declaration)
                allFills.reversed().forEachIndexed { index, fillBrush ->
                    addLayer(
                        10,
                        CanvasLayer.GradientShape(
                            shapeType = shapeType,
                            cornerRadius = radii.topLeft,
                            fill = fillBrush,
                            stroke = if (index == allFills.size - 1) border else null,
                            opacity = baseOpacity,
                            rotationDegrees = baseTransform.rotationDegrees,
                            offsetXRatio = baseTransform.translateX / buttonWidth,
                            offsetYRatio = baseTransform.translateY / buttonHeight,
                            scaleX = baseTransform.scaleX,
                            scaleY = baseTransform.scaleY,
                            originXRatio = baseTransform.originXRatio,
                            originYRatio = baseTransform.originYRatio
                        )
                    )
                }
            }

            // 4. Inset Shadows / Perimeter Groove (combined base + ::after groove)
            val afterShadows = afterStyle?.get("box-shadow")?.let { ShadowParser.parseBoxShadows(it) } ?: emptyList()
            val afterInsets = afterShadows.filter { it.isInset }
            val combinedInset = (insetShadows + afterInsets)

            if (combinedInset.isNotEmpty()) {
                val darkInset = combinedInset.firstOrNull { ColorParser.isDark(it.color) }?.color ?: 0x73000000L
                val lightInset = combinedInset.firstOrNull { !ColorParser.isDark(it.color) }?.color ?: 0x30FFFFFFL
                addLayer(15, CanvasLayer.InnerShadow(shadowColor = darkInset, highlightColor = lightInset, strokeWidth = 3.5f))
            }
        }

        // Identify the actual text node early to exclude it from background child compilation
        fun findRealTextNode(node: DomNode): DomNode? {
            val labeledChild = node.children.firstOrNull { child ->
                child.classNames.any { it.contains("label") || it.contains("text") || it.contains("glyph") } &&
                        child.findFirstText()?.isNotBlank() == true
            }
            if (labeledChild != null) return findRealTextNode(labeledChild) ?: labeledChild

            if (node.textContent.isNotBlank()) {
                return node
            }
            for (child in node.children) {
                findRealTextNode(child)?.let { return it }
            }
            return null
        }

        val textNode = findRealTextNode(primaryNode)

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
            val beforeFilter = FilterParser.parse(beforeStyle["filter"])
            val beforeBorder = GeometryParser.parseBorder(beforeStyle["border"] ?: beforeStyle["border-width"])
            val bounds = GeometryParser.computeBoxBounds(beforeStyle, buttonWidth, buttonHeight)
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

            if (isBoxPrimitive && (beforeBgs.isNotEmpty() || beforeBorder != null || beforeShadows.isNotEmpty())) {
                addLayer(
                    beforeStack,
                    CanvasLayer.BoxLayer(
                        shapeType = beforeShape,
                        polygonSides = beforePolySides,
                        pathData = beforePolyPath,
                        cornerRadiusTopLeft = beforeRadii.topLeft,
                        cornerRadiusTopRight = beforeRadii.topRight,
                        cornerRadiusBottomRight = beforeRadii.bottomRight,
                        cornerRadiusBottomLeft = beforeRadii.bottomLeft,
                        widthRatio = beforeWidth / buttonWidth,
                        heightRatio = beforeHeight / buttonHeight,
                        clipToBounds = beforeStyle["overflow"] == "hidden" || beforeClip != null || baseProps["overflow"] == "hidden",
                        fill = beforeBgs.firstOrNull() ?: FillBrush.Solid(0x00000000L),
                        fills = beforeBgs.reversed(),
                        stroke = beforeBorder,
                        boxShadows = beforeShadows,
                        opacity = beforeOpacity,
                        rotationDegrees = beforeTransform.rotationDegrees,
                        offsetXRatio = (beforeLeft + beforeTransform.translateX) / buttonWidth,
                        offsetYRatio = (beforeTop + beforeTransform.translateY) / buttonHeight,
                        scaleX = beforeTransform.scaleX,
                        scaleY = beforeTransform.scaleY,
                        skewX = beforeTransform.skewX,
                        skewY = beforeTransform.skewY,
                        originXRatio = beforeTransform.originXRatio,
                        originYRatio = beforeTransform.originYRatio
                    )
                )
            } else {
                beforeBgs.reversed().forEachIndexed { index, bg ->
                    addLayer(
                        beforeStack,
                        CanvasLayer.GradientShape(
                            shapeType = beforeShape,
                            cornerRadius = beforeRadii.topLeft,
                            fill = bg,
                            stroke = if (index == beforeBgs.size - 1) beforeBorder else null,
                            opacity = beforeOpacity,
                            rotationDegrees = beforeTransform.rotationDegrees,
                            offsetXRatio = beforeTransform.translateX / buttonWidth,
                            offsetYRatio = beforeTransform.translateY / buttonHeight,
                            scaleX = beforeTransform.scaleX,
                            scaleY = beforeTransform.scaleY,
                            originXRatio = beforeTransform.originXRatio,
                            originYRatio = beforeTransform.originYRatio
                        )
                    )
                }

                val beforeInsets = beforeShadows.filter { it.isInset }
                if (beforeInsets.isNotEmpty() && !isBoxPrimitive) {
                    val darkB = beforeInsets.firstOrNull { ColorParser.isDark(it.color) }?.color ?: 0x73000000L
                    val lightB = beforeInsets.firstOrNull { !ColorParser.isDark(it.color) }?.color ?: 0x30FFFFFFL
                    addLayer(beforeStack + 2, CanvasLayer.InnerShadow(shadowColor = darkB, highlightColor = lightB, strokeWidth = 3.0f))
                }
            }
        }

        // 5. Recursive DOM Tree Compilation (supports arbitrary nested spans, divs, grass blades, lenses)
        fun compileDomChildren(
            parentNode: DomNode,
            parentWidth: Float,
            parentHeight: Float,
            parentGlobalX: Float,
            parentGlobalY: Float,
            isParentClipping: Boolean = false
        ) {
            for (child in parentNode.children) {
                if (child == textNode) continue

                val childStyle = CssCascadeResolver.computeStyle(child, stylesheet).base
                if (!isVisible(childStyle)) continue

                val cOpacity = childStyle["opacity"]?.toFloatOrNull() ?: 1.0f

                val bounds = GeometryParser.computeBoxBounds(childStyle, parentWidth, parentHeight)
                val cWidth = bounds.width
                val cHeight = bounds.height
                val localLeft = bounds.left
                val localTop = bounds.top

                val globalX = parentGlobalX + localLeft
                val globalY = parentGlobalY + localTop

                val cRadii = GeometryParser.parseBorderRadius(childStyle["border-radius"], defaultSizeDp = cWidth)
                val cBorder = GeometryParser.parseBorder(childStyle["border"] ?: childStyle["border-top"] ?: childStyle["border-width"])
                val cClip = GeometryParser.parseClipPath(childStyle["clip-path"] ?: childStyle["-webkit-clip-path"], cWidth, cHeight)
                val isCOval = childStyle["border-radius"]?.contains("50%") == true ||
                    (cRadii.topLeft >= (cWidth * 0.35f) && cRadii.topRight >= (cWidth * 0.35f) &&
                     cRadii.bottomRight >= (cWidth * 0.35f) && cRadii.bottomLeft >= (cWidth * 0.35f))
                val cShape = when {
                    cClip != null -> cClip.shapeType
                    isCOval -> "OVAL"
                    else -> "ROUNDED_RECT"
                }
                val cPolySides = cClip?.polygonSides ?: 0
                val cPolyPath = cClip?.pathData ?: ""

                val childZ = GeometryParser.parseZIndex(childStyle)
                val childStack = 60 + childZ * 10

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
                    addLayer(
                        childStack,
                        CanvasLayer.BoxLayer(
                            shapeType = cShape,
                            polygonSides = cPolySides,
                            pathData = cPolyPath,
                            cornerRadiusTopLeft = cRadii.topLeft,
                            cornerRadiusTopRight = cRadii.topRight,
                            cornerRadiusBottomRight = cRadii.bottomRight,
                            cornerRadiusBottomLeft = cRadii.bottomLeft,
                            widthRatio = cWidth / buttonWidth,
                            heightRatio = cHeight / buttonHeight,
                            clipToBounds = clipChild,
                            fill = cFills.firstOrNull() ?: FillBrush.Solid(0x00000000L),
                            fills = cFills.reversed(),
                            stroke = cBorder,
                            boxShadows = cShadows,
                            opacity = cOpacity,
                            rotationDegrees = cTransform.rotationDegrees,
                            offsetXRatio = (globalX + cTransform.translateX) / buttonWidth,
                            offsetYRatio = (globalY + cTransform.translateY) / buttonHeight,
                            scaleX = cTransform.scaleX,
                            scaleY = cTransform.scaleY,
                            skewX = cTransform.skewX,
                            skewY = cTransform.skewY,
                            originXRatio = cTransform.originXRatio,
                            originYRatio = cTransform.originYRatio
                        )
                    )
                }

                if (child.children.isNotEmpty()) {
                    compileDomChildren(
                        parentNode = child,
                        parentWidth = cWidth,
                        parentHeight = cHeight,
                        parentGlobalX = globalX,
                        parentGlobalY = globalY,
                        isParentClipping = clipChild
                    )
                }
            }
        }

        compileDomChildren(
            parentNode = primaryNode,
            parentWidth = buttonWidth,
            parentHeight = buttonHeight,
            parentGlobalX = 0f,
            parentGlobalY = 0f,
            isParentClipping = baseProps["overflow"] == "hidden" || rootClip != null
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
            val afterFilter = FilterParser.parse(afterStyle["filter"])
            val bounds = GeometryParser.computeBoxBounds(afterStyle, buttonWidth, buttonHeight)
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
            val afterBorder = GeometryParser.parseBorder(afterStyle["border"] ?: afterStyle["border-top"])
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

            if (isBoxPrimitive && afterBgs.isNotEmpty()) {
                addLayer(
                    afterStack,
                    CanvasLayer.BoxLayer(
                        shapeType = afterShape,
                        polygonSides = afterPolySides,
                        pathData = afterPolyPath,
                        cornerRadiusTopLeft = afterRadii.topLeft,
                        cornerRadiusTopRight = afterRadii.topRight,
                        cornerRadiusBottomRight = afterRadii.bottomRight,
                        cornerRadiusBottomLeft = afterRadii.bottomLeft,
                        widthRatio = afterWidth / buttonWidth,
                        heightRatio = afterHeight / buttonHeight,
                        clipToBounds = afterStyle["overflow"] == "hidden" || afterClip != null || baseProps["overflow"] == "hidden",
                        fill = afterBgs.firstOrNull() ?: FillBrush.Solid(0x00000000L),
                        fills = afterBgs.reversed(),
                        stroke = afterBorder,
                        boxShadows = afterShadows,
                        opacity = afterOpacity,
                        rotationDegrees = afterTransform.rotationDegrees,
                        offsetXRatio = (afterLeft + afterTransform.translateX) / buttonWidth,
                        offsetYRatio = (afterTop + afterTransform.translateY) / buttonHeight,
                        scaleX = afterTransform.scaleX,
                        scaleY = afterTransform.scaleY,
                        skewX = afterTransform.skewX,
                        skewY = afterTransform.skewY,
                        originXRatio = afterTransform.originXRatio,
                        originYRatio = afterTransform.originYRatio
                    )
                )
            } else if (afterBgs.isNotEmpty()) {
                afterBgs.reversed().forEachIndexed { index, bg ->
                    addLayer(
                        afterStack,
                        CanvasLayer.GradientShape(
                            shapeType = shapeType,
                            cornerRadius = radii.topLeft,
                            fill = bg,
                            stroke = if (index == afterBgs.size - 1) afterBorder else null,
                            opacity = afterOpacity,
                            rotationDegrees = afterTransform.rotationDegrees,
                            offsetXRatio = afterTransform.translateX / buttonWidth,
                            offsetYRatio = afterTransform.translateY / buttonHeight,
                            scaleX = afterTransform.scaleX,
                            scaleY = afterTransform.scaleY,
                            originXRatio = afterTransform.originXRatio,
                            originYRatio = afterTransform.originYRatio
                        )
                    )
                }
            }
        }

        // 7. Center Text Label (Embossed 3D + Glow Text Shadows)
        var centerGlyphAdded = false
        if (textNode != null) {
            val textStyle = CssCascadeResolver.computeStyle(textNode, stylesheet).base
            if (isVisible(textStyle)) {
                val text = textNode.findFirstText() ?: defaultControl
                val textOpacity = textStyle["opacity"]?.toFloatOrNull() ?: baseOpacity
                val rawTextColor = ColorParser.parse(textStyle["color"] ?: baseProps["color"]) ?: 0xFFFFFFFFL
                val textColor = if (textOpacity < 1.0f) {
                    val a = (((rawTextColor shr 24) and 0xFF) * textOpacity).toInt().coerceIn(0, 255)
                    (a.toLong() shl 24) or (rawTextColor and 0x00FFFFFFL)
                } else rawTextColor

                val fontSize = GeometryParser.parseFontSize(textStyle["font-size"] ?: baseProps["font-size"]) ?: 34f
                val textShadows = ShadowParser.parseTextShadows(textStyle["text-shadow"] ?: baseProps["text-shadow"])

                val darkTextShadow = textShadows.firstOrNull { ColorParser.isDark(it.color) }
                val lightTextHighlight = textShadows.firstOrNull { !ColorParser.isDark(it.color) }

                val textZ = GeometryParser.parseZIndex(textStyle)
                val textStack = 200 + textZ * 10

                addLayer(
                    textStack,
                    CanvasLayer.CenterGlyph(
                        text = text,
                        fontSizeSp = fontSize,
                        textColor = textColor,
                        shadowColor = darkTextShadow?.color ?: 0x73000000L,
                        shadowOffsetY = darkTextShadow?.offsetY ?: 2.5f,
                        highlightColor = lightTextHighlight?.color ?: 0xB3FFFFFFL,
                        textShadows = textShadows
                    )
                )
                centerGlyphAdded = true
            }
        }

        // Fallback CenterGlyph if no child text node was found
        if (!centerGlyphAdded) {
            val centerText = primaryNode.findFirstText() ?: defaultControl
            val textColor = ColorParser.parse(baseProps["color"]) ?: 0xFFFFFFFFL
            val fontSize = GeometryParser.parseFontSize(baseProps["font-size"]) ?: 36f
            val textShadows = ShadowParser.parseTextShadows(baseProps["text-shadow"])

            val darkTextShadow = textShadows.firstOrNull { ColorParser.isDark(it.color) }
            val lightTextHighlight = textShadows.firstOrNull { !ColorParser.isDark(it.color) }

            addLayer(
                200,
                CanvasLayer.CenterGlyph(
                    text = centerText,
                    fontSizeSp = fontSize,
                    textColor = textColor,
                    shadowColor = darkTextShadow?.color ?: 0x73000000L,
                    shadowOffsetY = darkTextShadow?.offsetY ?: 2.5f,
                    highlightColor = lightTextHighlight?.color ?: 0xB3FFFFFFL,
                    textShadows = textShadows
                )
            )
        }

        // 7. Touch Active Animations & Idle Keyframes
        val activeTransform = AnimationParser.parseTransforms(style.active["transform"])
        val pressScale = if (activeTransform.scaleX != 1.0f) activeTransform.scaleX else 0.92f
        val pressOffsetY = activeTransform.translateY

        val isRotating = AnimationParser.isRotatingAnimation(stylesheet, baseProps)
        val isPulsing = AnimationParser.isPulsingAnimation(stylesheet, baseProps)

        val idleType = when {
            isRotating -> "ROTATE"
            isPulsing -> "PULSE"
            else -> "NONE"
        }

        val autoControl = primaryNode.attributes["data-control"]
            ?: primaryNode.attributes["data-key"]
            ?: parsed.root.attributes["data-control"]
            ?: parsed.root.attributes["data-key"]
            ?: defaultControl

        val autoCategory = primaryNode.attributes["data-category"]
            ?: parsed.root.attributes["data-category"]
            ?: category

        val autoName = primaryNode.attributes["data-name"]
            ?: parsed.root.attributes["data-name"]
            ?: name

        val autoId = primaryNode.attributes["data-id"]
            ?: parsed.root.attributes["data-id"]
            ?: id

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
        val clipToBounds = overflow == "hidden" || rootClip != null || baseProps["border-radius"]?.contains("50%") == true || radii.topLeft >= (buttonWidth * 0.4f)

        val layers = layerEntries.sortedWith(compareBy({ it.stackIndex }, { it.order })).map { it.layer }

        return NxprcDocument(
            manifest = NxprcManifest(
                id = resolvedId,
                name = resolvedName,
                category = autoCategory.uppercase(),
                defaultControl = autoControl.uppercase(),
                widthDp = buttonWidth.toInt().coerceIn(40, 200),
                heightDp = buttonHeight.toInt().coerceIn(40, 200),
                description = "Compiled from HTML/CSS/SVG DOM Engine"
            ),
            canvas = NxprcCanvas(
                viewBoxWidth = buttonWidth,
                viewBoxHeight = buttonHeight,
                layers = layers,
                clipToBounds = clipToBounds
            ),
            animations = NxprcAnimations(
                idleType = idleType,
                pressScale = pressScale,
                pressOffsetY = pressOffsetY,
                springStiffness = 850f,
                springDamping = 0.65f
            )
        )
    }

    private fun isVisible(props: Map<String, String>): Boolean {
        val d = props["display"]?.trim()?.lowercase()
        val v = props["visibility"]?.trim()?.lowercase()
        return d != "none" && v != "hidden"
    }

    private fun findPrimaryButtonNode(root: DomNode, stylesheet: CssStylesheet): DomNode {
        // 1. Explicit <button> tag
        val buttons = root.findByTag("button")
        if (buttons.isNotEmpty()) return buttons[0]

        // 2. Class names matching button keywords
        val keywords = listOf("btn", "button", "pad", "nexpad", "control", "key", "trigger", "action", "circle", "dpad", "stick", "thumb", "bumper", "wedge", "switch", "knob", "hud")
        val candidates = mutableListOf<DomNode>()
        fun scan(node: DomNode) {
            if (node.classNames.any { cls -> keywords.any { kw -> cls.contains(kw, ignoreCase = true) } }) {
                candidates.add(node)
            }
            node.children.forEach { scan(it) }
        }
        scan(root)
        if (candidates.isNotEmpty()) return candidates[0]

        // 3. Search children of <body> for an element matching CSS rules
        val bodyNode = root.findByTag("body").firstOrNull() ?: root
        for (child in bodyNode.children) {
            val hasMatchingRules = stylesheet.rules.any { r -> r.selectors.any { sel -> matchesNode(child, sel) } }
            if (hasMatchingRules) return child
        }

        return bodyNode.children.firstOrNull() ?: root.children.firstOrNull() ?: root
    }

    private fun matchesNode(node: DomNode, sel: CssSelector): Boolean {
        return CssCascadeResolver.matchesNode(node, sel)
    }
}
