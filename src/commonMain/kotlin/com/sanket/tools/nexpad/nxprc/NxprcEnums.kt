package com.sanket.tools.nexpad.nxprc

import com.sanket.tools.nexpad.category.CategoryType

/**
 * Strongly-typed enumeration of supported canvas layer shape types.
 * Used by [CanvasLayer.BoxLayer.shapeType] and [CanvasLayer.GradientShape.shapeType].
 *
 * HIGH 4 FIX: These enums were previously defined but unused (fields were plain Strings).
 * They are now the single source of truth for shape/animation type constants.
 * The string values match the serialized names used in .nxprc JSON for backward compatibility.
 */
enum class LayerShapeType(val id: String) {
    ROUNDED_RECT("ROUNDED_RECT"),
    OVAL("OVAL"),
    POLYGON("POLYGON"),
    PATH("PATH"),
    HEXAGON("HEXAGON"),
    OCTAGON("OCTAGON");

    companion object {
        /** Safe lookup by string ID, returns ROUNDED_RECT as fallback. */
        fun fromId(id: String): LayerShapeType =
            entries.firstOrNull { it.id == id.uppercase() } ?: ROUNDED_RECT
    }
}

/**
 * Strongly-typed enumeration of idle animation modes.
 * Corresponds to [NxprcAnimations.idleType].
 */
enum class IdleAnimationType(val id: String) {
    PULSE("PULSE"),
    ROTATE("ROTATE"),
    SHIMMER("SHIMMER"),
    RGB_CYCLE("RGB_CYCLE"),
    CUSTOM("CUSTOM"),
    NONE("NONE");

    companion object {
        fun fromId(id: String): IdleAnimationType =
            entries.firstOrNull { it.id == id.uppercase() } ?: NONE
    }
}

/**
 * Strongly-typed enumeration of press feedback animation styles.
 * Corresponds to [NxprcAnimations.pressFeedback].
 */
enum class PressFeedbackType(val id: String) {
    SPRING("SPRING"),
    SHOCKWAVE("SHOCKWAVE"),
    FLASH("FLASH");

    companion object {
        fun fromId(id: String): PressFeedbackType =
            entries.firstOrNull { it.id == id.uppercase() } ?: SPRING
    }
}

/**
 * Strongly-typed enumeration of valid NXPRC component categories.
 * Corresponds to [NxprcManifest.category].
 * Used by [NxprcInputValidator.validateCategory] and the renderer for physics dispatch.
 */
enum class NxprcCategory(val id: String) {
    BUTTON("BUTTON"),
    DPAD("DPAD"),
    JOYSTICK("JOYSTICK"),
    TRIGGER("TRIGGER"),
    BUMPER("BUMPER"),
    HOME("HOME"),
    SYSTEM("SYSTEM"),
    MACRO("MACRO");

    /** Multiplatform top-level category mapping. */
    val categoryType: CategoryType get() = when (this) {
        BUTTON -> CategoryType.ABXY
        DPAD -> CategoryType.DPAD
        JOYSTICK -> CategoryType.STICKS
        TRIGGER -> CategoryType.TRIGGERS
        BUMPER -> CategoryType.BUMPERS
        HOME, SYSTEM -> CategoryType.SYSTEM
        MACRO -> CategoryType.MACROS
    }

    companion object {
        /**
         * Resolves any identifier or alias (e.g. "BUTTON", "TRIGGER", "TRIGGERS", "STICKS", "DPAD", "LT")
         * to its canonical [NxprcCategory]. Falls back to [BUTTON] on unknown inputs.
         */
        fun fromId(id: String): NxprcCategory {
            val upper = id.uppercase()
            entries.firstOrNull { it.id == upper }?.let { return it }

            return CategoryType.fromIdentifier(id)?.let { cat ->
                when (cat) {
                    CategoryType.ABXY -> BUTTON
                    CategoryType.DPAD -> DPAD
                    CategoryType.STICKS -> JOYSTICK
                    CategoryType.TRIGGERS -> TRIGGER
                    CategoryType.BUMPERS -> BUMPER
                    CategoryType.SYSTEM -> SYSTEM
                    CategoryType.MACROS -> MACRO
                }
            } ?: BUTTON
        }
    }
}
