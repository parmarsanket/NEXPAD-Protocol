package com.sanket.tools.nexpad.nxprc.engine.css

/**
 * Centralized Enum of compiled regular expression patterns for CSS lexical tokenization and selector parsing.
 * Eliminates duplicate, hardcoded inline pattern strings across CssTokenizer, CssSelectorParser, etc.
 */
enum class CssRulePattern(val regex: Regex) {
    /** Strips C-style block comments */
    COMMENT(Regex("/\\*[\\s\\S]*?\\*/")),

    /** Matches and strips !important flags */
    IMPORTANT(Regex("\\s*!important\\s*$", RegexOption.IGNORE_CASE)),

    /** Matches @keyframes rule headers */
    KEYFRAMES(Regex("@(?:-[a-zA-Z]+-)?keyframes\\s+([a-zA-Z0-9_-]+)\\s*\\{")),

    /** Matches @media min-width media queries */
    MIN_WIDTH(Regex("min-width\\s*:\\s*([0-9.]+)px", RegexOption.IGNORE_CASE)),

    /** Matches @media max-width media queries */
    MAX_WIDTH(Regex("max-width\\s*:\\s*([0-9.]+)px", RegexOption.IGNORE_CASE)),

    /** Matches CSS custom property (variable) references */
    CSS_VARIABLE(Regex("""var\s*\(\s*(--[a-zA-Z0-9_-]+)(?:\s*,\s*([^()]+))?\s*\)""")),

    /** Matches CSS class name selectors */
    CLASS_SELECTOR(Regex("\\.([a-zA-Z0-9_-]+)"));

    fun find(input: CharSequence): MatchResult? = regex.find(input)
    fun findAll(input: CharSequence): Sequence<MatchResult> = regex.findAll(input)
    fun replace(input: CharSequence, replacement: String): String = regex.replace(input, replacement)
    fun replace(input: CharSequence, transform: (MatchResult) -> CharSequence): String = regex.replace(input, transform)
}
