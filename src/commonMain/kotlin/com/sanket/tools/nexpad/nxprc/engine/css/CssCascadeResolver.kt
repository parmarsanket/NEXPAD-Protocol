package com.sanket.tools.nexpad.nxprc.engine.css

import com.sanket.tools.nexpad.nxprc.engine.dom.DomNode

data class ComputedElementStyle(
    val base: Map<String, String>,
    val active: Map<String, String>,
    val hover: Map<String, String>,
    val before: Map<String, String>?,
    val after: Map<String, String>?
)

/**
 * Resolves CSS Cascading, Specificity, and CSS Variables across DOM nodes.
 */
object CssCascadeResolver {

    fun computeStyle(node: DomNode, stylesheet: CssStylesheet): ComputedElementStyle {
        val baseRules = mutableListOf<Pair<Int, Map<String, String>>>()
        val activeRules = mutableListOf<Pair<Int, Map<String, String>>>()
        val hoverRules = mutableListOf<Pair<Int, Map<String, String>>>()
        val beforeRules = mutableListOf<Pair<Int, Map<String, String>>>()
        val afterRules = mutableListOf<Pair<Int, Map<String, String>>>()

        for (rule in stylesheet.rules) {
            for (sel in rule.selectors) {
                if (matchesNode(node, sel)) {
                    when {
                        sel.pseudoElement == "before" -> beforeRules.add(Pair(sel.specificity, rule.declarations))
                        sel.pseudoElement == "after" -> afterRules.add(Pair(sel.specificity, rule.declarations))
                        sel.pseudoClass == "active" -> activeRules.add(Pair(sel.specificity, rule.declarations))
                        sel.pseudoClass == "hover" -> hoverRules.add(Pair(sel.specificity, rule.declarations))
                        else -> baseRules.add(Pair(sel.specificity, rule.declarations))
                    }
                }
            }
        }

        // Merge by specificity
        val baseMerged = mergeDeclarations(baseRules)
        // Inline styles have highest specificity (1000)
        val finalBase = baseMerged + node.inlineStyles

        val finalActive = mergeDeclarations(activeRules)
        val finalHover = mergeDeclarations(hoverRules)
        val finalBefore = if (beforeRules.isNotEmpty()) mergeDeclarations(beforeRules) else null
        val finalAfter = if (afterRules.isNotEmpty()) mergeDeclarations(afterRules) else null

        // Resolve CSS Variables: var(--name, fallback)
        return ComputedElementStyle(
            base = resolveVariables(finalBase, stylesheet.customProperties),
            active = resolveVariables(finalActive, stylesheet.customProperties),
            hover = resolveVariables(finalHover, stylesheet.customProperties),
            before = finalBefore?.let { resolveVariables(it, stylesheet.customProperties) },
            after = finalAfter?.let { resolveVariables(it, stylesheet.customProperties) }
        )
    }

    fun matchesNode(node: DomNode, sel: CssSelector): Boolean {
        // Tag check
        if (sel.tag != null && !sel.tag.equals(node.tag, ignoreCase = true)) {
            return false
        }
        // Class check
        if (sel.className != null && !node.classNames.any { it.equals(sel.className, ignoreCase = true) }) {
            return false
        }
        // ID check
        if (sel.id != null && !sel.id.equals(node.id, ignoreCase = true)) {
            return false
        }
        // Ancestor check
        if (sel.ancestorSelector != null) {
            var curr = node.parent
            var matched = false
            while (curr != null) {
                if (matchesNode(curr, sel.ancestorSelector)) {
                    matched = true
                    break
                }
                curr = curr.parent
            }
            if (!matched) return false
        }
        return true
    }

    private fun mergeDeclarations(list: List<Pair<Int, Map<String, String>>>): Map<String, String> {
        val sorted = list.sortedBy { it.first }
        val result = mutableMapOf<String, String>()
        for ((_, decls) in sorted) {
            result.putAll(decls)
        }
        return result
    }

    private fun resolveVariables(decls: Map<String, String>, customProps: Map<String, String>): Map<String, String> {
        val resolved = mutableMapOf<String, String>()
        for ((prop, rawVal) in decls) {
            resolved[prop] = resolveVarExpressions(rawVal, customProps, decls)
        }
        return resolved
    }

    private fun resolveVarExpressions(value: String, customProps: Map<String, String>, decls: Map<String, String>): String {
        var result = value
        var maxIter = 10 // Prevent infinite recursion on circular references
        while (result.contains("var(") && maxIter-- > 0) {
            // Match innermost var() — no nested parens inside
            val regex = Regex("""var\s*\(\s*(--[a-zA-Z0-9_-]+)(?:\s*,\s*([^()]+))?\s*\)""")
            val replaced = regex.replace(result) { match ->
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
