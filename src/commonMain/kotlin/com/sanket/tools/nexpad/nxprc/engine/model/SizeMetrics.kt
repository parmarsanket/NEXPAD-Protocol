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
         * A corner radius must be ≥ 45 % of the smaller button dimension to
         * qualify the shape as an oval/circle. Replaces the former inline `0.35f`.
         * 45 % is derived from the CSS spec: border-radius ≥ 50% = circle, but
         * real-world designs often use values like 40–49 % — so we use 45 % as
         * the conservative threshold.
         */
        const val OVAL_RADIUS_RATIO: Float = 0.45f

        // ── Text ────────────────────────────────────────────────────────────
        /** Default font occupies 40 % of the button height (industry convention for gamepad labels). */
        const val FONT_SIZE_RATIO: Float = 0.40f
        /** Text is line-wrapped if it exceeds 90 % of the button width. */
        const val TEXT_MAX_WIDTH_RATIO: Float = 0.90f

        // ── Gloss reflection ────────────────────────────────────────────────
        const val GLOSS_OFFSET_X_RATIO: Float = 0.14f
        const val GLOSS_OFFSET_Y_RATIO: Float = 0.07f
        const val GLOSS_WIDTH_RATIO:    Float = 0.55f
        const val GLOSS_HEIGHT_RATIO:   Float = 0.32f

        // ── Shadow / stroke defaults (absolute dp values, not ratios) ───────
        /** Default inner-shadow stroke width. */
        const val INNER_SHADOW_STROKE_DP: Float = 3.5f
        /** Default light-highlight alpha for inner shadows. */
        const val INNER_SHADOW_LIGHT_COLOR: Long = 0x30FFFFFFL
        /** Default glow-ring blur when no explicit CSS blur is specified. */
        const val DEFAULT_GLOW_BLUR_DP: Float = 14f
    }
}
