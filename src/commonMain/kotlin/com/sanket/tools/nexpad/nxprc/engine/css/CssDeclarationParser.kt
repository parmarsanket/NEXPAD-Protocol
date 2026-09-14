package com.sanket.tools.nexpad.nxprc.engine.css

/**
 * Robust CSS Declaration and Statement Parser.
 *
 * Handles:
 * - Stripping C-style comments (/* ... */)
 * - Semicolon splitting respecting parentheses (e.g. gradients, calc, var) and quotes
 * - Extracting property-value declarations
 * - Stripping '!important' flags
 */
object CssDeclarationParser {

    private val COMMENT_REGEX = CssRulePattern.COMMENT.regex
    private val IMPORTANT_REGEX = CssRulePattern.IMPORTANT.regex

    fun stripComments(css: String): String {
        return css.replace(COMMENT_REGEX, "")
    }

    fun parseDeclarations(body: String): Map<String, String> {
        val decls = mutableMapOf<String, String>()
        val parts = splitDeclarations(body)

        for (part in parts) {
            val colonIdx = part.indexOf(':')
            if (colonIdx > 0) {
                val prop = part.substring(0, colonIdx).trim().lowercase()
                val value = part.substring(colonIdx + 1).trim()
                if (prop.isNotBlank() && value.isNotBlank()) {
                    decls[prop] = value.replace(IMPORTANT_REGEX, "").trim()
                }
            }
        }
        return decls
    }

    fun splitDeclarations(body: String): List<String> {
        val list = mutableListOf<String>()
        var start = 0
        var parenDepth = 0
        var inSingleQuote = false
        var inDoubleQuote = false

        for (i in body.indices) {
            val c = body[i]
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote
            } else if (!inSingleQuote && !inDoubleQuote) {
                if (c == '(') parenDepth++
                else if (c == ')') parenDepth--
                else if (c == ';' && parenDepth == 0) {
                    val stmt = body.substring(start, i).trim()
                    if (stmt.isNotBlank()) list.add(stmt)
                    start = i + 1
                }
            }
        }
        if (start < body.length) {
            val last = body.substring(start).trim()
            if (last.isNotBlank()) list.add(last)
        }
        return list
    }
}
