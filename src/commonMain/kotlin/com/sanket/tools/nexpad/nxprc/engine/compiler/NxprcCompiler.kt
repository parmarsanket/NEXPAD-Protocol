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
        val stylesheet = CssTokenizer.parse(parsed.embeddedCss + "\n" + extractInlineStylesheets(html))

        // 1. Find the Primary Gamepad Button Node
        val primaryNode = findPrimaryButtonNode(parsed.root, stylesheet)
        val style = CssCascadeResolver.computeStyle(primaryNode, stylesheet)

        val layers = mutableListOf<CanvasLayer>()
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

        val isOval = radii.topLeft >= (buttonWidth * 0.35f) || baseProps["border-radius"]?.contains("50%") == true
        val shapeType = when {
            baseProps["clip-path"]?.contains("polygon") == true -> "POLYGON"
            isOval -> "OVAL"
            else -> "ROUNDED_RECT"
        }

        val hasExplicitBezel = primaryNode.attributes["data-bezel"] == "true" ||
                primaryNode.classNames.any { it.contains("bezel") || it.contains("socket") } ||
                outsetShadows.any { it.spreadRadius > 0f }

        val allFills = GradientParser.parseAll(
            baseProps["background"] ?: baseProps["background-color"] ?: baseProps["fill"],
            baseProps["background-position"],
            baseProps["background-size"]
        )

        val isBoxPrimitive = primaryNode.attributes["data-primitive"] == "box" ||
                (!hasExplicitBezel && (allFills.size > 1 || allBoxShadows.size > 1 || primaryNode.classNames.any {
                    it.contains("box") || it.contains("card") || it.contains("panel")
                }))

        // Drop shadow / Atmospheric Glow Layer (requires blur > 0 and bright non-dark color)
        val glowShadow = outsetShadows.firstOrNull { it.blurRadius > 0f && !ColorParser.isDark(it.color) }
        if (glowShadow != null) {
            layers.add(CanvasLayer.GlowRing(glowColor = glowShadow.color, blurRadius = glowShadow.blurRadius, pulseEnabled = true))
        }

        val baseTransform = AnimationParser.parseTransforms(
            baseProps["transform"],
            baseProps["transform-origin"],
            buttonWidth,
            buttonHeight
        )

        if (isBoxPrimitive) {
            layers.add(
                CanvasLayer.BoxLayer(
                    shapeType = shapeType,
                    cornerRadiusTopLeft = radii.topLeft,
                    cornerRadiusTopRight = radii.topRight,
                    cornerRadiusBottomRight = radii.bottomRight,
                    cornerRadiusBottomLeft = radii.bottomLeft,
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

                layers.add(
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
                    layers.add(
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
                    layers.add(
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
                layers.add(CanvasLayer.InnerShadow(shadowColor = darkInset, highlightColor = lightInset, strokeWidth = 3.5f))
            }
        }

        // 4. ::before: Socket groove, outer radial depth, metallic conic rim
        if (beforeStyle != null) {
            val beforeBgs = beforeStyle["background"]?.let {
                GradientParser.parseAll(it, beforeStyle["background-position"], beforeStyle["background-size"])
            } ?: emptyList()
            val beforeOpacity = beforeStyle["opacity"]?.toFloatOrNull() ?: 1.0f
            val beforeFilter = FilterParser.parse(beforeStyle["filter"])
            val beforeBorder = GeometryParser.parseBorder(beforeStyle["border"] ?: beforeStyle["border-width"])
            val beforeRadii = GeometryParser.parseBorderRadius(beforeStyle["border-radius"], defaultSizeDp = buttonWidth)
            val beforeShape = when {
                beforeRadii.topLeft >= (buttonWidth * 0.35f) || beforeStyle["border-radius"]?.contains("50%") == true -> "OVAL"
                else -> shapeType
            }

            val beforeTransform = AnimationParser.parseTransforms(
                beforeStyle["transform"],
                beforeStyle["transform-origin"],
                buttonWidth,
                buttonHeight
            )

            beforeBgs.reversed().forEachIndexed { index, bg ->
                layers.add(
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

            val beforeShadows = beforeStyle["box-shadow"]?.let { ShadowParser.parseBoxShadows(it) } ?: emptyList()
            val beforeInsets = beforeShadows.filter { it.isInset }
            if (beforeInsets.isNotEmpty() && !isBoxPrimitive) {
                val darkB = beforeInsets.firstOrNull { ColorParser.isDark(it.color) }?.color ?: 0x73000000L
                val lightB = beforeInsets.firstOrNull { !ColorParser.isDark(it.color) }?.color ?: 0x30FFFFFFL
                layers.add(CanvasLayer.InnerShadow(shadowColor = darkB, highlightColor = lightB, strokeWidth = 3.0f))
            } else if (beforeBgs.isEmpty()) {
                val glossWidth = GeometryParser.parsePixelOrPercent(beforeStyle["width"], buttonWidth, buttonWidth * 0.55f) / buttonWidth
                val glossHeight = GeometryParser.parsePixelOrPercent(beforeStyle["height"], buttonHeight, buttonHeight * 0.32f) / buttonHeight
                val glossTop = GeometryParser.parsePixelOrPercent(beforeStyle["top"], buttonHeight, buttonHeight * 0.07f) / buttonHeight
                val glossLeft = GeometryParser.parsePixelOrPercent(beforeStyle["left"], buttonWidth, buttonWidth * 0.14f) / buttonWidth
                val glossTransform = AnimationParser.parseTransforms(
                    beforeStyle["transform"],
                    beforeStyle["transform-origin"],
                    buttonWidth,
                    buttonHeight
                )

                layers.add(
                    CanvasLayer.GlossReflection(
                        offsetXRatio = glossLeft,
                        offsetYRatio = glossTop,
                        widthRatio = glossWidth,
                        heightRatio = glossHeight,
                        rotationDegrees = glossTransform.rotationDegrees.takeIf { it != 0f } ?: -18f,
                        alpha = (0.75f * beforeOpacity).coerceIn(0.1f, 1.0f),
                        blurRadius = beforeFilter.blurRadiusPx
                    )
                )
            }
        }

        // 5. Inner Container Elements & Highlights (e.g. .nexpad-a-highlight or .btn-core)
        val containerNodes = primaryNode.children.filter { it.tag != "span" && it.tag != "p" }
        for (child in containerNodes) {
            val childStyle = CssCascadeResolver.computeStyle(child, stylesheet).base
            val childCls = child.classNames.joinToString(" ").lowercase()

            if (!isVisible(childStyle)) continue

            if (childCls.contains("highlight") || childCls.contains("reflection") || childCls.contains("gloss") || childCls.contains("shine")) {
                val w = GeometryParser.parsePixelOrPercent(childStyle["width"], buttonWidth, 30f)
                val h = GeometryParser.parsePixelOrPercent(childStyle["height"], buttonHeight, 10f)
                val left = GeometryParser.parsePositionalOffset(childStyle["left"], childStyle["right"], w, buttonWidth, 20f)
                val top = GeometryParser.parsePositionalOffset(childStyle["top"], childStyle["bottom"], h, buttonHeight, 15f)
                val transform = AnimationParser.parseTransforms(
                    childStyle["transform"],
                    childStyle["transform-origin"],
                    w,
                    h
                )
                val bgBrush = GradientParser.parseFirst(
                    childStyle["background"],
                    childStyle["background-position"],
                    childStyle["background-size"]
                )
                val filter = FilterParser.parse(childStyle["filter"])
                val opacity = childStyle["opacity"]?.toFloatOrNull() ?: 1.0f

                val alpha = when (bgBrush) {
                    is FillBrush.RadialGradient -> {
                        val firstColor = bgBrush.colors.firstOrNull() ?: 0xFFFFFFFFL
                        ((firstColor shr 24) and 0xFF) / 255f
                    }
                    is FillBrush.Solid -> ((bgBrush.color shr 24) and 0xFF) / 255f
                    else -> 0.6f
                }

                layers.add(
                    CanvasLayer.GlossReflection(
                        offsetXRatio = left / buttonWidth,
                        offsetYRatio = top / buttonHeight,
                        widthRatio = w / buttonWidth,
                        heightRatio = h / buttonHeight,
                        rotationDegrees = transform.rotationDegrees.takeIf { it != 0f } ?: -18f,
                        alpha = (alpha * opacity).coerceIn(0.1f, 1.0f),
                        blurRadius = filter.blurRadiusPx
                    )
                )
            } else {
                // General container element (e.g. .btn-core, .inner, .box)
                val cOpacity = childStyle["opacity"]?.toFloatOrNull() ?: 1.0f
                val cBg = childStyle["background"] ?: childStyle["background-color"]
                val cWidth = GeometryParser.parsePixelOrPercent(childStyle["width"], buttonWidth, buttonWidth * 0.85f)
                val cHeight = GeometryParser.parsePixelOrPercent(childStyle["height"], buttonHeight, buttonHeight * 0.85f)
                val cRadii = GeometryParser.parseBorderRadius(childStyle["border-radius"], defaultSizeDp = cWidth)
                val cBorder = GeometryParser.parseBorder(childStyle["border"] ?: childStyle["border-top"] ?: childStyle["border-width"])
                val isCOval = cRadii.topLeft >= (cWidth * 0.35f) || childStyle["border-radius"]?.contains("50%") == true
                val cShape = if (isCOval) "OVAL" else "ROUNDED_RECT"
                val cFills = if (cBg != null) {
                    GradientParser.parseAll(
                        cBg,
                        childStyle["background-position"],
                        childStyle["background-size"]
                    )
                } else emptyList()
                val cShadows = ShadowParser.parseBoxShadows(childStyle["box-shadow"])

                val isChildBox = isBoxPrimitive || child.attributes["data-primitive"] == "box" ||
                        child.classNames.any { it.contains("box") || it.contains("card") || it.contains("panel") }

                val cTransform = AnimationParser.parseTransforms(
                    childStyle["transform"],
                    childStyle["transform-origin"],
                    cWidth,
                    cHeight
                )

                if (isChildBox) {
                    layers.add(
                        CanvasLayer.BoxLayer(
                            shapeType = cShape,
                            cornerRadiusTopLeft = cRadii.topLeft,
                            cornerRadiusTopRight = cRadii.topRight,
                            cornerRadiusBottomRight = cRadii.bottomRight,
                            cornerRadiusBottomLeft = cRadii.bottomLeft,
                            fill = cFills.firstOrNull() ?: FillBrush.Solid(0x00000000L),
                            fills = cFills.reversed(),
                            stroke = cBorder,
                            boxShadows = cShadows,
                            opacity = cOpacity,
                            rotationDegrees = cTransform.rotationDegrees,
                            offsetXRatio = cTransform.translateX / buttonWidth,
                            offsetYRatio = cTransform.translateY / buttonHeight,
                            scaleX = cTransform.scaleX,
                            scaleY = cTransform.scaleY,
                            skewX = cTransform.skewX,
                            skewY = cTransform.skewY,
                            originXRatio = cTransform.originXRatio,
                            originYRatio = cTransform.originYRatio
                        )
                    )
                } else {
                    if (cFills.isNotEmpty()) {
                        cFills.reversed().forEachIndexed { index, fillBrush ->
                            layers.add(
                                CanvasLayer.GradientShape(
                                    shapeType = cShape,
                                    cornerRadius = cRadii.topLeft,
                                    fill = fillBrush,
                                    stroke = if (index == cFills.size - 1) cBorder else null,
                                    opacity = cOpacity,
                                    rotationDegrees = cTransform.rotationDegrees,
                                    offsetXRatio = cTransform.translateX / buttonWidth,
                                    offsetYRatio = cTransform.translateY / buttonHeight,
                                    scaleX = cTransform.scaleX,
                                    scaleY = cTransform.scaleY,
                                    originXRatio = cTransform.originXRatio,
                                    originYRatio = cTransform.originYRatio
                                )
                            )
                        }
                    } else if (cBorder != null) {
                        layers.add(
                            CanvasLayer.GradientShape(
                                shapeType = cShape,
                                cornerRadius = cRadii.topLeft,
                                fill = FillBrush.Solid(0x00000000L),
                                stroke = cBorder,
                                opacity = cOpacity,
                                rotationDegrees = cTransform.rotationDegrees,
                                offsetXRatio = cTransform.translateX / buttonWidth,
                                offsetYRatio = cTransform.translateY / buttonHeight,
                                scaleX = cTransform.scaleX,
                                scaleY = cTransform.scaleY,
                                originXRatio = cTransform.originXRatio,
                                originYRatio = cTransform.originYRatio
                            )
                        )
                    }

                    val cInsets = cShadows.filter { it.isInset }
                    if (cInsets.isNotEmpty() && !isChildBox) {
                        val darkC = cInsets.firstOrNull { ColorParser.isDark(it.color) }?.color ?: 0x73000000L
                        val lightC = cInsets.firstOrNull { !ColorParser.isDark(it.color) }?.color ?: 0x30FFFFFFL
                        layers.add(CanvasLayer.InnerShadow(shadowColor = darkC, highlightColor = lightC, strokeWidth = 2.5f))
                    }
                }
            }
        }

        // 6. ::after: Top specular arc gloss & glass reflection edge
        if (afterStyle != null) {
            val afterBgs = afterStyle["background"]?.let {
                GradientParser.parseAll(it, afterStyle["background-position"], afterStyle["background-size"])
            } ?: emptyList()
            val afterOpacity = afterStyle["opacity"]?.toFloatOrNull() ?: 1.0f
            val afterFilter = FilterParser.parse(afterStyle["filter"])
            val afterTransform = AnimationParser.parseTransforms(
                afterStyle["transform"],
                afterStyle["transform-origin"],
                buttonWidth,
                buttonHeight
            )
            val afterShadows = afterStyle["box-shadow"]?.let { ShadowParser.parseBoxShadows(it) } ?: emptyList()
            val afterBorder = GeometryParser.parseBorder(afterStyle["border"] ?: afterStyle["border-top"])

            val hasOffsets = afterStyle.containsKey("top") || afterStyle.containsKey("left") || afterStyle.containsKey("width") || afterStyle.containsKey("height")
            if (hasOffsets && afterBgs.isNotEmpty()) {
                val glossW = GeometryParser.parsePixelOrPercent(afterStyle["width"], buttonWidth, buttonWidth * 0.70f) / buttonWidth
                val glossH = GeometryParser.parsePixelOrPercent(afterStyle["height"], buttonHeight, buttonHeight * 0.38f) / buttonHeight
                val glossT = GeometryParser.parsePixelOrPercent(afterStyle["top"], buttonHeight, buttonHeight * 0.06f) / buttonHeight
                val glossL = GeometryParser.parsePixelOrPercent(afterStyle["left"], buttonWidth, buttonWidth * 0.15f) / buttonWidth

                val alpha = when (val firstBg = afterBgs.firstOrNull()) {
                    is FillBrush.RadialGradient -> {
                        val firstColor = firstBg.colors.firstOrNull() ?: 0xFFFFFFFFL
                        (((firstColor shr 24) and 0xFF) / 255f).coerceIn(0.1f, 1.0f)
                    }
                    is FillBrush.Solid -> (((firstBg.color shr 24) and 0xFF) / 255f).coerceIn(0.1f, 1.0f)
                    else -> 0.75f
                }

                layers.add(
                    CanvasLayer.GlossReflection(
                        offsetXRatio = glossL,
                        offsetYRatio = glossT,
                        widthRatio = glossW,
                        heightRatio = glossH,
                        rotationDegrees = afterTransform.rotationDegrees.takeIf { it != 0f } ?: -10f,
                        alpha = (alpha * afterOpacity).coerceIn(0.1f, 1.0f),
                        blurRadius = afterFilter.blurRadiusPx
                    )
                )
            } else if (afterBgs.isNotEmpty()) {
                afterBgs.reversed().forEachIndexed { index, bg ->
                    layers.add(
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
        fun findTextNode(node: DomNode): DomNode? {
            if (node.tag == "span" || node.tag == "p" || node.classNames.any { it.contains("label") || it.contains("text") }) {
                return node
            }
            for (child in node.children) {
                findTextNode(child)?.let { return it }
            }
            return null
        }

        val textNode = findTextNode(primaryNode)
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

                layers.add(
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

            layers.add(
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

        val resolvedId = when {
            autoId.isNotBlank() && autoId != "rc.custom" -> if (autoId.startsWith("rc.")) autoId else "rc.$autoId"
            primaryNode.classNames.any { it.contains("nexpad-a") } -> "rc.nexpad_a"
            else -> "rc.custom_$autoControl"
        }

        val resolvedName = when {
            autoName.isNotBlank() && autoName != "Custom Button" -> autoName
            primaryNode.classNames.any { it.contains("nexpad-a") } -> "Nexpad A Button"
            else -> "Custom $autoControl Button"
        }

        val overflow = baseProps["overflow"]?.trim()?.lowercase()
        val clipToBounds = overflow == "hidden" || baseProps["border-radius"]?.contains("50%") == true || radii.topLeft >= (buttonWidth * 0.4f)

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
        val keywords = listOf("btn", "button", "pad", "nexpad", "control", "key", "trigger", "action", "circle")
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

    private fun extractInlineStylesheets(html: String): String {
        val sb = StringBuilder()
        val m = Pattern.compile("style=[\"']([^\"']+)[\"']").matcher(html)
        while (m.find()) {
            sb.append(m.group(1)).append(";\n")
        }
        return sb.toString()
    }
}
