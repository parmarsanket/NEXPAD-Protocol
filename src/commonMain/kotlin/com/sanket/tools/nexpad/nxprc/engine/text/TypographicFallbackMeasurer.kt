package com.sanket.tools.nexpad.nxprc.engine.text

/**
 * Multiplatform Typographic Fallback Measurer.
 * Accurately measures text width in ems using Unicode codepoint inspection,
 * properly handling surrogate pairs, narrow/wide glyphs, CJK, and emoji.
 */
object TypographicFallbackMeasurer {

    fun measure(text: String, fontSizeSp: Float, fontWeight: Int): TextMetrics.Metrics {
        var totalWidthEm = 0f
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            val cp: Int
            if (ch.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) {
                cp = ((ch.code - 0xD800) shl 10) + (text[i + 1].code - 0xDC00) + 0x10000
                i += 2
            } else {
                cp = ch.code
                i += 1
            }
            totalWidthEm += charWidthEm(cp)
        }

        val weightScale = if (fontWeight >= 700) 1.05f else 1.0f
        val width = totalWidthEm * fontSizeSp * weightScale
        val ascent = fontSizeSp * 0.85f
        val descent = fontSizeSp * 0.25f

        return TextMetrics.Metrics(
            width = width,
            ascent = ascent,
            descent = descent
        )
    }

    private fun charWidthEm(cp: Int): Float {
        return when {
            // Space & control
            cp == 0x20 -> 0.28f
            cp == 0xA0 -> 0.28f // non-breaking space
            cp == 0x09 -> 0.56f // tab

            // Narrow Latin characters & punctuation
            cp in 'i'.code..'l'.code || cp == '1'.code || cp == '!'.code ||
            cp == '.'.code || cp == ','.code || cp == ':'.code || cp == ';'.code ||
            cp == '\''.code || cp == '\"'.code || cp == '|'.code || cp == '/'.code ||
            cp == '\\'.code || cp == '('.code || cp == ')'.code || cp == '['.code ||
            cp == ']'.code || cp == '{'.code || cp == '}'.code || cp == '`'.code -> 0.28f

            // Capital I
            cp == 'I'.code || cp == 'J'.code -> 0.35f

            // Wide Latin characters
            cp == 'M'.code || cp == 'W'.code -> 0.92f
            cp == 'm'.code || cp == 'w'.code -> 0.80f
            cp == '@'.code || cp == '%'.code || cp == '&'.code || cp == '#'.code -> 0.82f

            // Standard Capitals
            cp in 'A'.code..'Z'.code -> 0.68f

            // Standard Lowercase and digits
            cp in 'a'.code..'z'.code || cp in '0'.code..'9'.code -> 0.54f

            // Common symbols
            cp == '-'.code || cp == '+'.code || cp == '='.code || cp == '*'.code ||
            cp == '<'.code || cp == '>'.code || cp == '?'.code || cp == '~'.code || cp == '^'.code -> 0.55f

            // CJK characters (Fullwidth)
            cp in 0x4E00..0x9FFF || cp in 0x3400..0x4DBF || cp in 0x20000..0x2A6DF ||
            cp in 0x3000..0x303F || cp in 0x3040..0x309F || cp in 0x30A0..0x30FF ||
            cp in 0xFF00..0xFFEF -> 1.0f

            // Emoji ranges (Supplementary & Misc symbols)
            cp in 0x1F300..0x1F9FF || cp in 0x2600..0x27BF -> 1.15f

            // Default fallback for any other Unicode character
            else -> 0.58f
        }
    }
}
