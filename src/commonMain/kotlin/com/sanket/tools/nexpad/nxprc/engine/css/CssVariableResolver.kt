package com.sanket.tools.nexpad.nxprc.engine.css

/**
 * Resolves CSS custom properties (variables) with fallback handling and cycle protection.
 *
 * Syntax supported: `var(--variable-name)` or `var(--variable-name, fallback-value)`.
 * Protects against infinite substitution loops (caps at 10 iterations).
 */
object CssVariableResolver {

    private val INNER_VAR_REGEX = CssRulePattern.CSS_VARIABLE.regex

    fun resolveVariables(
        decls: Map<String, String>,
        customProps: Map<String, String>
    ): Map<String, String> {
        val resolved = mutableMapOf<String, String>()
        for ((prop, rawVal) in decls) {
            resolved[prop] = resolveVarExpressions(rawVal, customProps, decls)
        }
        return resolved
    }

    fun resolveVarExpressions(
        value: String,
        customProps: Map<String, String>,
        decls: Map<String, String>
    ): String {
        var result = value
        var maxIter = 10 // Prevent infinite recursion on circular references
        while (result.contains("var(") && maxIter-- > 0) {
            val replaced = INNER_VAR_REGEX.replace(result) { match ->
                val varName = match.groupValues[1]
                val fallback = match.groupValues[2].trim()
                customProps[varName] ?: decls[varName] ?: fallback
            }
            if (replaced == result) break // No further substitutions possible
            result = replaced
        }
        return result
    }
}
