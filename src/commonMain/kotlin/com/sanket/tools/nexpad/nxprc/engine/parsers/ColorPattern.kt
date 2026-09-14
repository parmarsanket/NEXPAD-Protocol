package com.sanket.tools.nexpad.nxprc.engine.parsers

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Centralized Enum of compiled regular expression patterns for color and token parsing.
 * Eliminates duplicate, hardcoded inline pattern strings across ColorParser, ShadowParser, etc.
 */
enum class ColorPattern(val pattern: Pattern) {
    /** Matches #RGB, #RGBA, #RRGGBB, #RRGGBBAA hex color notation with capture group for hex digits */
    HEX(Pattern.compile("#([0-9a-fA-F]{3,8})\\b")),

    /** Matches standalone #RGB, #RGBA, #RRGGBB, #RRGGBBAA without capturing */
    HEX_STANDALONE(Pattern.compile("#[0-9a-fA-F]{3,8}\\b")),

    /** Matches parenthesized functional color notation: rgb(...), rgba(...), hsl(...), hsla(...) */
    PARENTHESIZED(Pattern.compile("(?:rgba?|hsla?)\\([^)]+\\)", Pattern.CASE_INSENSITIVE)),

    /** Matches comma and whitespace delimiters separating color channels or stops */
    DELIMITER(Pattern.compile("[,\\s]+")),

    /** Splits words and token boundaries */
    WORD_SPLIT(Pattern.compile("[^a-z0-9_-]+"));

    fun matcher(input: CharSequence): Matcher = pattern.matcher(input)
    fun split(input: CharSequence): List<String> = pattern.split(input).toList()
}
