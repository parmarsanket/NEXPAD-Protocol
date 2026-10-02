package com.sanket.tools.nexpad.nxprc.engine.model

/**
 * Universal, named size metrics for an NXPRC button canvas.
 *
 * Centralises every magic ratio that was previously scattered as inline literals
 * (0.35f, 0.40f, 0.55f, 0.90f, etc.) across [NxprcCompiler], [DomTreeCompiler], and
 * [BoxLayerBuilder]. All engine code that needs a size-relative measurement MUST
 * reference these constants — never write a bare float ratio in compiler code.
 *
 * Usage:
 *   val metrics = SizeMetrics(buttonWidth, buttonHeight)
 *   val fontSize = metrics.defaultFontSize
 *   val isOval   = radii.isOval(metrics)
 */
data class SizeMetrics(
    val buttonWidth: Float,
    val buttonHeight: Float
) {
    val minDimension: Float = minOf(buttonWidth, buttonHeight)
    val maxDimension: Float = maxOf(buttonWidth, buttonHeight)
    val aspectRatio: Float  = if (buttonHeight > 0f) buttonWidth / buttonHeight else 1f

    // ── Shape thresholds ────────────────────────────────────────────────────
    /** Minimum radius for a corner to count toward the "oval" classification. */
    val ovalCornerThreshold: Float get() = minDimension * OVAL_RADIUS_RATIO

    // ── Text sizing ─────────────────────────────────────────────────────────
    /** Default font size when the CSS `font-size` property is absent. */
    val defaultFontSize: Float get() = buttonHeight * FONT_SIZE_RATIO
    /** Maximum width at which text is wrapped before line-breaking. */
    val textMaxWidth: Float get() = buttonWidth * TEXT_MAX_WIDTH_RATIO

    // ── Gloss / specular overlay geometry ───────────────────────────────────
    val glossOffsetX: Float  get() = buttonWidth  * GLOSS_OFFSET_X_RATIO
    val glossOffsetY: Float  get() = buttonHeight * GLOSS_OFFSET_Y_RATIO
    val glossWidth: Float    get() = buttonWidth  * GLOSS_WIDTH_RATIO
    val glossHeight: Float   get() = buttonHeight * GLOSS_HEIGHT_RATIO

    companion object {
        // ── Shape thresholds ────────────────────────────────────────────────
        /**
         * NXPRC heuristic threshold: corner radius ≥ 45% of min dimension is classified as oval/circle.
         * Delegated to [com.sanket.tools.nexpad.nxprc.ShapeHeuristics.OVAL_RADIUS_RATIO].
         */
        const val OVAL_RADIUS_RATIO: Float = com.sanket.tools.nexpad.nxprc.ShapeHeuristics.OVAL_RADIUS_RATIO

        // ── Text ────────────────────────────────────────────────────────────
        /** Default font occupies 40 % of the button height (industry convention for gamepad labels). */
        const val FONT_SIZE_RATIO: Float = com.sanket.tools.nexpad.nxprc.TypographyDefaults.FONT_SIZE_RATIO
        /** Text is line-wrapped if it exceeds 90 % of the button width. */
        const val TEXT_MAX_WIDTH_RATIO: Float = com.sanket.tools.nexpad.nxprc.TypographyDefaults.TEXT_MAX_WIDTH_RATIO

        // ── Gloss reflection ────────────────────────────────────────────────
        const val GLOSS_OFFSET_X_RATIO: Float = com.sanket.tools.nexpad.nxprc.GlossMetrics.GLOSS_OFFSET_X_RATIO
        const val GLOSS_OFFSET_Y_RATIO: Float = com.sanket.tools.nexpad.nxprc.GlossMetrics.GLOSS_OFFSET_Y_RATIO
        const val GLOSS_WIDTH_RATIO: Float = com.sanket.tools.nexpad.nxprc.GlossMetrics.GLOSS_WIDTH_RATIO
        const val GLOSS_HEIGHT_RATIO: Float = com.sanket.tools.nexpad.nxprc.GlossMetrics.GLOSS_HEIGHT_RATIO

        // ── Shadow / stroke defaults (absolute dp values, not ratios) ───────
        /** Default inner-shadow stroke width. */
        const val INNER_SHADOW_STROKE_DP: Float = com.sanket.tools.nexpad.nxprc.VisualDesignTokens.INNER_SHADOW_STROKE_DP
        /** Default light-highlight alpha for inner shadows. */
        const val INNER_SHADOW_LIGHT_COLOR: Long = com.sanket.tools.nexpad.nxprc.VisualDesignTokens.INNER_SHADOW_LIGHT_COLOR
        /** Default glow-ring blur when no explicit CSS blur is specified. */
        const val DEFAULT_GLOW_BLUR_DP: Float = com.sanket.tools.nexpad.nxprc.VisualDesignTokens.DEFAULT_GLOW_BLUR_DP
    }
}
