package com.sanket.tools.nexpad.nxprc.engine.css

/**
 * Pure Kotlin CSS Lexer & Parser.
 * Strips comments, parses @keyframes at-rules, extracts CSS custom properties (variables),
 * and parses selector rules into a structured CssStylesheet AST.
 */
object CssTokenizer {

    private val KEYFRAMES_REGEX = CssRulePattern.KEYFRAMES.regex
    private val MIN_WIDTH_REGEX = CssRulePattern.MIN_WIDTH.regex
    private val MAX_WIDTH_REGEX = CssRulePattern.MAX_WIDTH.regex

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
        var match = KEYFRAMES_REGEX.find(cssWithoutKeyframes)
        while (match != null) {
            val name = match.groupValues[1]
            val openBrace = match.range.last
            val closeBrace = findMatchingBrace(cssWithoutKeyframes, openBrace)
            if (closeBrace != -1) {
                val body = cssWithoutKeyframes.substring(openBrace + 1, closeBrace)
                val steps = parseKeyframeSteps(body)
                keyframesMap[name] = CssKeyframes(name = name, steps = steps)
                cssWithoutKeyframes = cssWithoutKeyframes.substring(0, match.range.first) + cssWithoutKeyframes.substring(closeBrace + 1)
                match = KEYFRAMES_REGEX.find(cssWithoutKeyframes)
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
        val min = MIN_WIDTH_REGEX.find(selector)?.groupValues?.get(1)?.toFloatOrNull()
        val max = MAX_WIDTH_REGEX.find(selector)?.groupValues?.get(1)?.toFloatOrNull()
        return (min == null || width >= min) && (max == null || width <= max)
    }

    private fun stripComments(css: String): String = CssDeclarationParser.stripComments(css)

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

    fun parseDeclarations(body: String): Map<String, String> = CssDeclarationParser.parseDeclarations(body)

    fun splitDeclarations(body: String): List<String> = CssDeclarationParser.splitDeclarations(body)

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

            val subTimes = timePart.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val decls = parseDeclarations(stepBody)
            for (t in subTimes) {
                val percentage = when {
                    t.equals("from", ignoreCase = true) -> 0.0f
                    t.equals("to", ignoreCase = true) -> 1.0f
                    t.endsWith("%") -> (t.removeSuffix("%").trim().toFloatOrNull() ?: 0f) / 100f
                    else -> t.toFloatOrNull()?.let { if (it > 1.0f) it / 100f else it } ?: 0.0f
                }
                steps.add(CssKeyframeStep(percentage = percentage, declarations = decls))
            }
        }
        return steps
    }
}
