package com.sanket.tools.nexpad.nxprc.engine.compiler

import com.sanket.tools.nexpad.nxprc.*
import com.sanket.tools.nexpad.nxprc.engine.parsers.CornerRadii
import com.sanket.tools.nexpad.nxprc.engine.parsers.ParsedTransform

/**
 * Factory and builder helper for [CanvasLayer.BoxLayer] and [CanvasLayer.GradientShape],
 * unifying geometric transformations, corner radii, shadow outsets, and compositing effects.
 */
internal object BoxLayerBuilder {

    /**
     * Builds a comprehensive [CanvasLayer.BoxLayer] with normalized dimensions, CSS transform matrices,
     * filter definitions, and compositing strategy.
     */
    fun buildBoxLayer(
        shapeType: String,
        polygonSides: Int = 0,
        pathData: String = "",
        radii: CornerRadii,
        width: Float,
        height: Float,
        left: Float,
        top: Float,
        buttonWidth: Float,
        buttonHeight: Float,
        clipToBounds: Boolean,
        fills: List<FillBrush>,
        stroke: StrokeStyle?,
        boxShadows: List<BoxShadowDef>,
        filterDef: FilterDef,
        opacity: Float,
        transform: ParsedTransform,
        isRotating: Boolean = false,
        fallbackFill: FillBrush = FillBrush.Solid(0x00000000L),
        drawCacheHint: Boolean = true
    ): CanvasLayer.BoxLayer {
        val outsets = EffectsResolver.computeShadowOutsets(boxShadows)
        val strategy = EffectsResolver.computeCompositingStrategy(
            opacity = opacity,
            hasMultipleFillsOrChildren = fills.size > 1,
            hasFilter = filterDef.blurRadius > 0f || filterDef.brightness != 1f || filterDef.saturation != 1f
        )
        return CanvasLayer.BoxLayer(
            shapeType = shapeType,
            polygonSides = polygonSides,
            pathData = pathData,
            cornerRadiusTopLeft = radii.topLeft,
            cornerRadiusTopRight = radii.topRight,
            cornerRadiusBottomRight = radii.bottomRight,
            cornerRadiusBottomLeft = radii.bottomLeft,
            widthRatio = width / buttonWidth,
            heightRatio = height / buttonHeight,
            clipToBounds = clipToBounds,
            fill = fills.firstOrNull() ?: fallbackFill,
            fills = fills.reversed(),
            stroke = stroke,
            boxShadows = boxShadows,
            filter = filterDef,
            opacity = opacity,
            rotationDegrees = transform.rotationDegrees,
            offsetXRatio = (left + transform.translateX) / buttonWidth,
            offsetYRatio = (top + transform.translateY) / buttonHeight,
            scaleX = transform.scaleX,
            scaleY = transform.scaleY,
            skewX = transform.skewX,
            skewY = transform.skewY,
            originXRatio = transform.originXRatio,
            originYRatio = transform.originYRatio,
            isRotating = isRotating,
            effects = EffectsDef(
                opacity = opacity,
                filter = filterDef,
                compositingStrategy = strategy,
                layerOutsets = outsets,
                drawCacheHint = drawCacheHint && !isRotating
            )
        )
    }

    /**
     * Builds a [CanvasLayer.GradientShape] layer for legacy/fallback rendering.
     */
    fun buildGradientShape(
        shapeType: String,
        cornerRadius: Float,
        fill: FillBrush,
        stroke: StrokeStyle?,
        width: Float,
        height: Float,
        left: Float,
        top: Float,
        buttonWidth: Float,
        buttonHeight: Float,
        filterDef: FilterDef,
        opacity: Float,
        transform: ParsedTransform,
        boxShadows: List<BoxShadowDef> = emptyList(),
        hasMultipleFills: Boolean = false,
        drawCacheHint: Boolean = true
    ): CanvasLayer.GradientShape {
        val outsets = EffectsResolver.computeShadowOutsets(boxShadows)
        val strategy = EffectsResolver.computeCompositingStrategy(
            opacity = opacity,
            hasMultipleFillsOrChildren = hasMultipleFills,
            hasFilter = filterDef.blurRadius > 0f || filterDef.brightness != 1f || filterDef.saturation != 1f
        )
        return CanvasLayer.GradientShape(
            shapeType = shapeType,
            cornerRadius = cornerRadius,
            fill = fill,
            stroke = stroke,
            filter = filterDef,
            opacity = opacity,
            rotationDegrees = transform.rotationDegrees,
            offsetXRatio = (left + transform.translateX) / buttonWidth,
            offsetYRatio = (top + transform.translateY) / buttonHeight,
            widthRatio = width / buttonWidth,
            heightRatio = height / buttonHeight,
            scaleX = transform.scaleX,
            scaleY = transform.scaleY,
            originXRatio = transform.originXRatio,
            originYRatio = transform.originYRatio,
            effects = EffectsDef(
                opacity = opacity,
                filter = filterDef,
                compositingStrategy = strategy,
                layerOutsets = outsets,
                drawCacheHint = drawCacheHint
            )
        )
    }
}
