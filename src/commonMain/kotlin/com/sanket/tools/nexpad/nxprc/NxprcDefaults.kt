package com.sanket.tools.nexpad.nxprc

/** Shared defaults and safety limits for the NXPRC model/compiler. */
object NxprcDefaults {
    const val DEFAULT_VIEW_BOX_SIZE = GeometryConstants.DEFAULT_VIEW_BOX_SIZE
    const val DEFAULT_BUTTON_SIZE = GeometryConstants.DEFAULT_BUTTON_SIZE
    const val DEFAULT_MIN_SIZE_DP = GeometryConstants.DEFAULT_MIN_SIZE_DP
    const val DEFAULT_MAX_SIZE_DP = GeometryConstants.DEFAULT_MAX_SIZE_DP

    const val DEFAULT_FILL_COLOR = VisualDesignTokens.DEFAULT_FILL_COLOR
    const val DEFAULT_SHADOW_COLOR = VisualDesignTokens.DEFAULT_SHADOW_COLOR
    const val DEFAULT_HIGHLIGHT_COLOR = VisualDesignTokens.DEFAULT_HIGHLIGHT_COLOR
    const val DEFAULT_ACCENT_COLOR = VisualDesignTokens.DEFAULT_ACCENT_COLOR

    const val MAX_HTML_SIZE = CompilerLimits.MAX_HTML_SIZE
    const val MAX_ID_LENGTH = CompilerLimits.MAX_ID_LENGTH
    const val MAX_NAME_LENGTH = CompilerLimits.MAX_NAME_LENGTH
}
