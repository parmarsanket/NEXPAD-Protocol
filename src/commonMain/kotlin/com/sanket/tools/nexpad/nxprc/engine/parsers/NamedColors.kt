package com.sanket.tools.nexpad.nxprc.engine.parsers

/**
 * Standard W3C CSS Named Colors mapped to 32-bit ARGB values as 64-bit Long integers.
 * Structured as a strongly-typed enum class for compile-time safety and centralized maintenance.
 */
enum class NamedColor(val keyword: String, val argb: Long) {
    TRANSPARENT("transparent", 0x00000000L),
    CURRENT_COLOR("currentcolor", 0xFFFFFFFFL),
    BLACK("black", 0xFF000000L),
    WHITE("white", 0xFFFFFFFFL),
    RED("red", 0xFFFF0000L),
    GREEN("green", 0xFF008000L),
    BLUE("blue", 0xFF0000FFL),
    YELLOW("yellow", 0xFFFFFF00L),
    CYAN("cyan", 0xFF00FFFFL),
    MAGENTA("magenta", 0xFFFF00FFL),
    GRAY("gray", 0xFF808080L),
    GREY("grey", 0xFF808080L),
    DARK_GRAY("darkgray", 0xFFA9A9A9L),
    DARK_GREY("darkgrey", 0xFFA9A9A9L),
    LIGHT_GRAY("lightgray", 0xFFD3D3D3L),
    LIGHT_GREY("lightgrey", 0xFFD3D3D3L),
    ORANGE("orange", 0xFFFFA500L),
    PURPLE("purple", 0xFF800080L),
    PINK("pink", 0xFFFFC0CBL),
    LIME("lime", 0xFF00FF00L),
    NAVY("navy", 0xFF000080L),
    TEAL("teal", 0xFF008080L),
    MAROON("maroon", 0xFF800000L),
    OLIVE("olive", 0xFF808000L),
    AQUA("aqua", 0xFF00FFFFL),
    FUCHSIA("fuchsia", 0xFFFF00FFL),
    SILVER("silver", 0xFFC0C0C0L),
    GOLD("gold", 0xFFFFD700L),
    CORAL("coral", 0xFFFF7F50L),
    CRIMSON("crimson", 0xFFDC143CL),
    INDIGO("indigo", 0xFF4B0082L),
    VIOLET("violet", 0xFFEE82EEL),
    KHAKI("khaki", 0xFFF0E68CL),
    BEIGE("beige", 0xFFF5F5DCL),
    BROWN("brown", 0xFFA52A2AL),
    CHOCOLATE("chocolate", 0xFFD2691EL),
    TURQUOISE("turquoise", 0xFF40E0D0L),
    SNOW("snow", 0xFFFFFAFAF),
    AZURE("azure", 0xFFF0FFFFL);

    companion object {
        private val LOOKUP: Map<String, Long> = entries.associate { it.keyword to it.argb }
        fun find(keyword: String): Long? = LOOKUP[keyword.lowercase()]
        val ALL: Map<String, Long> get() = LOOKUP
    }
}

/**
 * Backward-compatibility facade for existing callers.
 */
internal object NamedColors {
    val ALL: Map<String, Long> get() = NamedColor.ALL
    fun find(name: String): Long? = NamedColor.find(name)
}
