package com.sanket.tools.nexpad.nxprc.engine.css

/**
 * Pure Kotlin CSS Lexer & Parser.
 * Strips comments, parses @keyframes at-rules, extracts CSS custom properties (variables),
 * and parses selector rules into a structured CssStylesheet AST.
 */
object CssTokenizer {

    /**
     * Parse the supported CSS subset. Conditional blocks are ignored unless a
     * viewport width is supplied; this prevents mobile/page rules from
     * silently overriding a standalone NEXPAD control.
     */
    fun parse(cssText: String, viewportWidth: Float? = null): CssStylesheet {
        val clean = stripComments(cssText)
        val rules = mutableListOf<CssRule>()
        val keyframesMap = mutableMapOf<String, CssKeyframes>()
        val customProperties = mutableMapOf<String, String>()

        // 1. Extract @keyframes using proper brace matching
        var cssWithoutKeyframes = clean
        val kfRegex = Regex("@(?:-[a-zA-Z]+-)?keyframes\\s+([a-zA-Z0-9_-]+)\\s*\\{")
        var match = kfRegex.find(cssWithoutKeyframes)
        while (match != null) {
            val name = match.groupValues[1]
            val openBrace = match.range.last
            val closeBrace = findMatchingBrace(cssWithoutKeyframes, openBrace)
            if (closeBrace != -1) {
                val body = cssWithoutKeyframes.substring(openBrace + 1, closeBrace)
                val steps = parseKeyframeSteps(body)
                keyframesMap[name] = CssKeyframes(name = name, steps = steps)
                cssWithoutKeyframes = cssWithoutKeyframes.substring(0, match.range.first) + cssWithoutKeyframes.substring(closeBrace + 1)
                match = kfRegex.find(cssWithoutKeyframes)
            } else {
                break
            }
        }

        // 2. Extract Rules: selector { declarations }
        var i = 0
        val len = cssWithoutKeyframes.length

        while (i < len) {
            val openBrace = cssWithoutKeyframes.indexOf('{', i)
            if (openBrace == -1) break

            val selectorPart = cssWithoutKeyframes.substring(i, openBrace).trim()
            val closeBrace = findMatchingBrace(cssWithoutKeyframes, openBrace)
            if (closeBrace == -1) break

            val bodyPart = cssWithoutKeyframes.substring(openBrace + 1, closeBrace).trim()
            i = closeBrace + 1

            if (selectorPart.startsWith("@")) {
                // For @media or @supports, parse inner body recursively to extract rules
                if (selectorPart.startsWith("@media") && mediaMatches(selectorPart, viewportWidth)) {
                    val innerSheet = parse(bodyPart, viewportWidth)
                    rules.addAll(innerSheet.rules)
                    innerSheet.customProperties.forEach { (k, v) -> customProperties.putIfAbsent(k, v) }
                }
                continue
            }

            val declarations = parseDeclarations(bodyPart)

            // Extract :root CSS variables
            if (selectorPart.contains(":root")) {
                declarations.forEach { (k, v) ->
                    if (k.startsWith("--")) {
                        customProperties[k] = v
                    }
                }
            }

            val selectorList = selectorPart.split(",").mapNotNull { sel ->
                val trimmed = sel.trim()
                if (trimmed.isNotBlank()) CssSelector.parse(trimmed) else null
            }

            if (selectorList.isNotEmpty() && declarations.isNotEmpty()) {
                rules.add(CssRule(selectors = selectorList, declarations = declarations))
            }
        }

        return CssStylesheet(
            rules = rules,
            keyframes = keyframesMap,
            customProperties = customProperties
        )
    }

    private fun mediaMatches(selector: String, viewportWidth: Float?): Boolean {
        val width = viewportWidth ?: return false
        val min = Regex("min-width\\s*:\\s*([0-9.]+)px", RegexOption.IGNORE_CASE)
            .find(selector)?.groupValues?.get(1)?.toFloatOrNull()
        val max = Regex("max-width\\s*:\\s*([0-9.]+)px", RegexOption.IGNORE_CASE)
            .find(selector)?.groupValues?.get(1)?.toFloatOrNull()
        return (min == null || width >= min) && (max == null || width <= max)
    }

    private fun stripComments(css: String): String {
        return css.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
    }

    private fun findMatchingBrace(text: String, openPos: Int): Int {
        var depth = 0
        for (idx in openPos until text.length) {
            if (text[idx] == '{') depth++
            else if (text[idx] == '}') {
                depth--
                if (depth == 0) return idx
            }
        }
        return -1
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
                    decls[prop] = value.replace(Regex("\\s*!important\\s*$", RegexOption.IGNORE_CASE), "").trim()
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

    private fun parseKeyframeSteps(body: String): List<CssKeyframeStep> {
        val steps = mutableListOf<CssKeyframeStep>()
        var i = 0
        val len = body.length

        while (i < len) {
            val openBrace = body.indexOf('{', i)
            if (openBrace == -1) break

            val timePart = body.substring(i, openBrace).trim()
            val closeBrace = findMatchingBrace(body, openBrace)
            if (closeBrace == -1) break

            val stepBody = body.substring(openBrace + 1, closeBrace).trim()
            i = closeBrace + 1

            val percentage = when {
                timePart.equals("from", ignoreCase = true) -> 0.0f
                timePart.equals("to", ignoreCase = true) -> 1.0f
                timePart.endsWith("%") -> (timePart.removeSuffix("%").trim().toFloatOrNull() ?: 0f) / 100f
                else -> 0.0f
            }

            steps.add(
                CssKeyframeStep(
                    percentage = percentage,
                    declarations = parseDeclarations(stepBody)
                )
            )
        }
        return steps
    }
}
