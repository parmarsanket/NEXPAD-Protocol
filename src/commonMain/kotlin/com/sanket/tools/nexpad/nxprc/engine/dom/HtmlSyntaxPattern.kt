package com.sanket.tools.nexpad.nxprc.engine.dom

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Centralized Enum of compiled regular expression patterns for HTML/SVG lexical analysis.
 * Eliminates duplicate, hardcoded pattern strings across DOM parsers.
 */
enum class HtmlPattern(val pattern: Pattern) {
    STYLE(Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>", Pattern.CASE_INSENSITIVE)),
    TAG(Pattern.compile("<(/?)([a-zA-Z0-9_-]+)((?:\\s+[^>]+)?)(/?)>|([^<]+)")),
    ATTR(Pattern.compile("([a-zA-Z0-9_-]+)(?:\\s*=\\s*(?:([\"'])([\\s\\S]*?)\\2|([^\\s>]+)))?"));

    fun matcher(input: CharSequence): Matcher = pattern.matcher(input)
}

/**
 * Regular expressions for sanitizing HTML markup before DOM tree construction.
 */
enum class HtmlSanitizeRegex(val regex: Regex) {
    STRIP_TAG(Regex("<[^>]+>")),
    COMMENT(Regex("<!--[\\s\\S]*?-->")),
    DOCTYPE(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE)),
    HEAD(Regex("<head[\\s\\S]*?</head>", RegexOption.IGNORE_CASE)),
    SCRIPT(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE)),
    STYLE_BLOCK(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE)),
    WHITESPACE(Regex("\\s+"));

    fun replace(input: CharSequence, replacement: String): String = regex.replace(input, replacement)
    fun split(input: CharSequence): List<String> = regex.split(input)
}
