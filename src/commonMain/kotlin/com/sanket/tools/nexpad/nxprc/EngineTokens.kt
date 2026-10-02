package com.sanket.tools.nexpad.nxprc

/**
 * Universal mathematical and geometric constants for NXPRC coordinate spaces.
 */
object GeometryConstants {
    const val DEFAULT_VIEW_BOX_SIZE = 100f
    const val DEFAULT_BUTTON_SIZE = 100f
    const val DEFAULT_MIN_SIZE_DP = 40
    const val DEFAULT_MAX_SIZE_DP = 300
}

/**
 * Standard design tokens and default styling values for controller components.
 */
object VisualDesignTokens {
    const val DEFAULT_FILL_COLOR = 0xFF0A192FL
    const val DEFAULT_SHADOW_COLOR = 0x73000000L
    const val DEFAULT_HIGHLIGHT_COLOR = 0xB3FFFFFFL
    const val DEFAULT_ACCENT_COLOR = 0xFF00F0FFL
    const val INNER_SHADOW_LIGHT_COLOR = 0x30FFFFFFL
    const val INNER_SHADOW_STROKE_DP = 3.5f
    const val DEFAULT_GLOW_BLUR_DP = 14f
}

/**
 * Typography metrics and label wrapping rules.
 */
object TypographyDefaults {
    /** Default font occupies 40 % of the button height (industry convention for gamepad labels). */
    const val FONT_SIZE_RATIO: Float = 0.40f
    /** Text is line-wrapped if it exceeds 90 % of the button width. */
    const val TEXT_MAX_WIDTH_RATIO: Float = 0.90f
}

/**
 * Specular highlight and gloss reflection proportions.
 */
object GlossMetrics {
    const val GLOSS_OFFSET_X_RATIO: Float = 0.14f
    const val GLOSS_OFFSET_Y_RATIO: Float = 0.07f
    const val GLOSS_WIDTH_RATIO: Float = 0.55f
    const val GLOSS_HEIGHT_RATIO: Float = 0.32f
}

/**
 * Compiler heuristics for inferring shapes when explicit markup metadata is absent.
 */
object ShapeHeuristics {
    /**
     * NXPRC heuristic threshold: corner radius ≥ 45% of min dimension is classified as oval/circle.
     * Note: While W3C CSS defines 50% border-radius as a circle, real-world button designs often use
     * 40–49% to achieve visually circular pill or dome shapes.
     */
    const val OVAL_RADIUS_RATIO: Float = 0.45f
}

/**
 * Input validation limits and parser safety boundaries.
 */
object CompilerLimits {
    const val MAX_HTML_SIZE = 2 * 1024 * 1024
    const val MAX_ID_LENGTH = 128
    const val MAX_NAME_LENGTH = 256
}
