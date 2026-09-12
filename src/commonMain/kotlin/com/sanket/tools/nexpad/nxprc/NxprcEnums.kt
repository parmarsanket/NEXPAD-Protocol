package com.sanket.tools.nexpad.nxprc

/**
 * Strongly-typed enumeration of supported canvas layer shape types.
 */
enum class LayerShapeType(val id: String) {
    ROUNDED_RECT("ROUNDED_RECT"),
    OVAL("OVAL"),
    POLYGON("POLYGON"),
    PATH("PATH"),
    HEXAGON("HEXAGON"),
    OCTAGON("OCTAGON");
}

/**
 * Strongly-typed enumeration of idle animation modes.
 */
enum class IdleAnimationType(val id: String) {
    PULSE("PULSE"),
    ROTATE("ROTATE"),
    SHIMMER("SHIMMER"),
    RGB_CYCLE("RGB_CYCLE"),
    NONE("NONE");
}

/**
 * Strongly-typed enumeration of press feedback animation styles.
 */
enum class PressFeedbackType(val id: String) {
    SPRING("SPRING"),
    SHOCKWAVE("SHOCKWAVE"),
    FLASH("FLASH");
}
