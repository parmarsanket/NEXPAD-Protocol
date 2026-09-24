package com.sanket.tools.nexpad.nxprc.engine.css

/**
 * Typed CSS `display` property values.
 * Replaces raw string comparisons: `props["display"] == "flex"`, `!= "none"`, etc.
 */
enum class DisplayValue(val cssId: String) {
    BLOCK("block"),
    INLINE("inline"),
    INLINE_BLOCK("inline-block"),
    FLEX("flex"),
    INLINE_FLEX("inline-flex"),
    GRID("grid"),
    INLINE_GRID("inline-grid"),
    CONTENTS("contents"),
    NONE("none");

    companion object {
        fun parse(raw: String?): DisplayValue =
            raw?.trim()?.lowercase()?.let { s -> entries.firstOrNull { it.cssId == s } } ?: BLOCK
    }
}

/**
 * Typed CSS `position` property values.
 * Replaces: `props["position"] == "absolute"`, `== "relative"`, etc.
 */
enum class PositionValue(val cssId: String) {
    STATIC("static"),
    RELATIVE("relative"),
    ABSOLUTE("absolute"),
    FIXED("fixed"),
    STICKY("sticky");

    companion object {
        fun parse(raw: String?): PositionValue =
            raw?.trim()?.lowercase()?.let { s -> entries.firstOrNull { it.cssId == s } } ?: STATIC
    }
}

/**
 * Typed CSS `flex-direction` values.
 * Replaces: `direction == "column"`, `direction == "column-reverse"`, etc.
 */
enum class FlexDirection(val cssId: String) {
    ROW("row"),
    ROW_REVERSE("row-reverse"),
    COLUMN("column"),
    COLUMN_REVERSE("column-reverse");

    val isColumn: Boolean get() = this == COLUMN || this == COLUMN_REVERSE
    val isReverse: Boolean get() = this == ROW_REVERSE || this == COLUMN_REVERSE

    companion object {
        fun parse(raw: String?): FlexDirection =
            raw?.trim()?.lowercase()?.let { s -> entries.firstOrNull { it.cssId == s } } ?: ROW
    }
}

/**
 * Typed CSS `justify-content` values.
 * Replaces: `"center", "space-around", "space-evenly" -> startMain + availMain / 2f`
 */
enum class JustifyContent(val cssId: String) {
    FLEX_START("flex-start"),
    FLEX_END("flex-end"),
    CENTER("center"),
    SPACE_BETWEEN("space-between"),
    SPACE_AROUND("space-around"),
    SPACE_EVENLY("space-evenly"),
    START("start"),
    END("end"),
    NORMAL("normal");

    /** True for modes that center the primary axis — used by the layout solver. */
    val isCentering: Boolean
        get() = this == CENTER || this == SPACE_AROUND || this == SPACE_EVENLY

    /** True for space distribution modes — use `extraSpacing` instead of `gap`. */
    val isSpaced: Boolean
        get() = this == SPACE_BETWEEN || this == SPACE_AROUND || this == SPACE_EVENLY

    companion object {
        fun parse(raw: String?): JustifyContent =
            raw?.trim()?.lowercase()?.let { s -> entries.firstOrNull { it.cssId == s } } ?: FLEX_START
    }
}

/**
 * Typed CSS `align-items` and `align-self` values.
 * Replaces: `align == "center"`, `align == "stretch"`, `align == "flex-end"`, etc.
 */
enum class AlignItems(val cssId: String) {
    FLEX_START("flex-start"),
    FLEX_END("flex-end"),
    CENTER("center"),
    STRETCH("stretch"),
    BASELINE("baseline"),
    START("start"),
    END("end"),
    NORMAL("normal");

    /** True for modes that center the cross axis. */
    val isCentering: Boolean get() = this == CENTER

    companion object {
        fun parse(raw: String?): AlignItems =
            raw?.trim()?.lowercase()?.let { s -> entries.firstOrNull { it.cssId == s } } ?: STRETCH

        /** `align-self` has the extra `auto` value — falls back to [STRETCH]. */
        fun parseSelf(raw: String?): AlignItems? {
            if (raw.isNullOrBlank() || raw.trim().lowercase() == "auto") return null
            return parse(raw)
        }
    }
}

/**
 * Typed CSS `overflow` values.
 * Replaces: `props["overflow"] == "hidden"`.
 */
enum class OverflowValue(val cssId: String) {
    VISIBLE("visible"),
    HIDDEN("hidden"),
    SCROLL("scroll"),
    AUTO("auto"),
    CLIP("clip");

    val isClipping: Boolean get() = this == HIDDEN || this == CLIP

    companion object {
        fun parse(raw: String?): OverflowValue =
            raw?.trim()?.lowercase()?.let { s -> entries.firstOrNull { it.cssId == s } } ?: VISIBLE
    }
}

/**
 * Typed CSS `visibility` values.
 * Replaces: `props["visibility"] == "hidden"`.
 */
enum class VisibilityValue(val cssId: String) {
    VISIBLE("visible"),
    HIDDEN("hidden"),
    COLLAPSE("collapse");

    val isVisible: Boolean get() = this == VISIBLE

    companion object {
        fun parse(raw: String?): VisibilityValue =
            raw?.trim()?.lowercase()?.let { s -> entries.firstOrNull { it.cssId == s } } ?: VISIBLE
    }
}

/**
 * Typed CSS `text-align` values.
 * Replaces: `textStyle["text-align"]?.uppercase() ?: "CENTER"`.
 */
enum class TextAlignValue(val cssId: String, val nxprcId: String) {
    LEFT("left", "LEFT"),
    CENTER("center", "CENTER"),
    RIGHT("right", "RIGHT"),
    JUSTIFY("justify", "CENTER"),  // approximated as center in canvas
    START("start", "LEFT"),
    END("end", "RIGHT");

    companion object {
        fun parse(raw: String?): TextAlignValue =
            raw?.trim()?.lowercase()?.let { s -> entries.firstOrNull { it.cssId == s } } ?: CENTER
    }
}

/**
 * Typed CSS `font-weight` — maps common keyword and numeric forms to an integer.
 * Replaces: ad-hoc string comparisons like `"bold"`, `"700"`, `"normal"`.
 */
object FontWeightParser {
    fun parse(raw: String?): Int {
        if (raw.isNullOrBlank()) return 400
        return when (raw.trim().lowercase()) {
            "thin"        -> 100
            "extralight",
            "ultra-light" -> 200
            "light"       -> 300
            "normal",
            "regular"     -> 400
            "medium"      -> 500
            "semibold",
            "demi-bold"   -> 600
            "bold"        -> 700
            "extrabold",
            "ultra-bold"  -> 800
            "black",
            "heavy"       -> 900
            else          -> raw.trim().toIntOrNull()?.coerceIn(100, 900) ?: 400
        }
    }
}
