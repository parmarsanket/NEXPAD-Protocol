package com.sanket.tools.nexpad.nxprc

/** Shared defaults and safety limits for the NXPRC model/compiler. */
object NxprcDefaults {
    const val DEFAULT_VIEW_BOX_SIZE = 100f
    const val DEFAULT_BUTTON_SIZE = 100f
    const val DEFAULT_MIN_SIZE_DP = 40
    const val DEFAULT_MAX_SIZE_DP = 200

    const val DEFAULT_FILL_COLOR = 0xFF0A192FL
    const val DEFAULT_SHADOW_COLOR = 0x73000000L
    const val DEFAULT_HIGHLIGHT_COLOR = 0xB3FFFFFFL
    const val DEFAULT_ACCENT_COLOR = 0xFF00F0FFL

    const val MAX_HTML_SIZE = 2 * 1024 * 1024
    const val MAX_ID_LENGTH = 128
    const val MAX_NAME_LENGTH = 256
}
