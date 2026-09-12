package com.sanket.tools.nexpad.nxprc.engine.compiler

import com.sanket.tools.nexpad.nxprc.*
import com.sanket.tools.nexpad.nxprc.engine.parsers.ParsedFilter

/**
 * Resolves compositing strategies, visual filter definitions, and bounding outsets
 * for layers and full canvas viewports.
 */
internal object EffectsResolver {

    /**
     * Converts a [ParsedFilter] AST result into a serializable [FilterDef].
     */
    fun toFilterDef(parsed: ParsedFilter): FilterDef = FilterDef(
        blurRadius = parsed.blurRadiusPx,
        brightness = parsed.brightness,
        saturation = parsed.saturate,
        hueRotateDegrees = parsed.hueRotateDegrees,
        renderEffect = if (parsed.renderEffect.blurRadiusX > 0f || parsed.renderEffect.blurRadiusY > 0f) {
            parsed.renderEffect
        } else if (parsed.blurRadiusPx > 0f) {
            RenderEffectDef(
                blurRadiusX = parsed.blurRadiusPx,
                blurRadiusY = parsed.blurRadiusPx,
                tileMode = "CLAMP"
            )
        } else RenderEffectDef()
    )

    /**
     * Determines whether offscreen compositing buffer is required to prevent opacity bleeds
     * across multi-layer fills, shadows, or visual filters.
     */
    fun computeCompositingStrategy(
        opacity: Float,
        hasMultipleFillsOrChildren: Boolean = false,
        hasFilter: Boolean = false
    ): CompositingStrategy {
        return when {
            opacity < 1.0f && (hasMultipleFillsOrChildren || hasFilter) -> CompositingStrategy.OFFSCREEN
            opacity < 1.0f -> CompositingStrategy.MODULATE_ALPHA
            hasFilter -> CompositingStrategy.OFFSCREEN
            else -> CompositingStrategy.AUTO
        }
    }

    /**
     * Computes the bounding rectangular reach added by outset box-shadows.
     */
    fun computeShadowOutsets(boxShadows: List<BoxShadowDef>): LayerOutsets {
        var maxLeft = 0f
        var maxTop = 0f
        var maxRight = 0f
        var maxBottom = 0f

        for (shadow in boxShadows) {
            if (shadow.isInset) continue
            val reach = shadow.blurRadius + shadow.spreadRadius
            if (reach <= 0f && shadow.offsetX == 0f && shadow.offsetY == 0f) continue
            maxLeft = maxOf(maxLeft, reach - shadow.offsetX)
            maxRight = maxOf(maxRight, reach + shadow.offsetX)
            maxTop = maxOf(maxTop, reach - shadow.offsetY)
            maxBottom = maxOf(maxBottom, reach + shadow.offsetY)
        }

        return LayerOutsets(
            left = maxOf(0f, maxLeft),
            top = maxOf(0f, maxTop),
            right = maxOf(0f, maxRight),
            bottom = maxOf(0f, maxBottom)
        )
    }

    /**
     * Computes the radial bounding expansion caused by a glow ring.
     */
    fun computeGlowOutsets(glowRadius: Float): LayerOutsets {
        return LayerOutsets(
            left = glowRadius,
            top = glowRadius,
            right = glowRadius,
            bottom = glowRadius
        )
    }

    /**
     * Computes the union of all layer outsets across the document to prevent clipping of shadows/glows.
     */
    fun computeTotalCanvasOutsets(entries: List<LayerEntry>): LayerOutsets {
        var totalCanvasOutsets = LayerOutsets()
        for (entry in entries) {
            when (val layer = entry.layer) {
                is CanvasLayer.BoxLayer -> {
                    totalCanvasOutsets += layer.effectiveEffects.layerOutsets
                }
                is CanvasLayer.GradientShape -> {
                    totalCanvasOutsets += layer.effectiveEffects.layerOutsets
                }
                is CanvasLayer.GlowRing -> {
                    totalCanvasOutsets += computeGlowOutsets(layer.blurRadius * 1.5f)
                }
                else -> {}
            }
        }
        return totalCanvasOutsets
    }
}
