package com.sanket.tools.nexpad.nxprc.engine.parsers

/**
 * Standard CSS font weights with numeric and named aliases.
 */
enum class FontWeight(val numericWeight: Int, val aliases: List<String>) {
    THIN(100, listOf("100", "thin")),
    EXTRA_LIGHT(200, listOf("200", "extralight")),
    LIGHT(300, listOf("300", "light")),
    NORMAL(400, listOf("400", "normal", "regular")),
    MEDIUM(500, listOf("500", "medium")),
    SEMI_BOLD(600, listOf("600", "semibold")),
    BOLD(700, listOf("700", "bold")),
    EXTRA_BOLD(800, listOf("800", "extrabold")),
    BLACK(900, listOf("900", "black"));

    companion object {
        private val LOOKUP: Map<String, Int> = entries.flatMap { w ->
            w.aliases.map { it to w.numericWeight }
        }.toMap()

        fun parse(str: String?): Int {
            if (str == null) return BOLD.numericWeight
            return LOOKUP[str.trim().lowercase()] ?: BOLD.numericWeight
        }
    }
}

/**
 * Dedicated parser for CSS typography properties: font-size, font-weight.
 */
object TypographyParser {

    private val FONT_SIZE_PATTERN = CssSyntaxPattern.FONT_SIZE.pattern

    fun parseFontSize(str: String?): Float? {
        if (str == null) return null
        val m = FONT_SIZE_PATTERN.matcher(str)
        return if (m.find()) m.group(1).toFloatOrNull() else null
    }

    fun parseFontWeight(str: String?): Int = FontWeight.parse(str)
}
