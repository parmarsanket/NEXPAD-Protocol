package com.sanket.tools.nexpad.nxprc.engine.css

import com.sanket.tools.nexpad.nxprc.engine.dom.DomNode

data class ComputedElementStyle(
    val base: Map<String, String>,
    val active: Map<String, String>,
    val hover: Map<String, String>,
    val focus: Map<String, String> = emptyMap(),
    val before: Map<String, String>?,
    val after: Map<String, String>?
)

/**
 * Resolves CSS Cascading, Specificity, and CSS Variables across DOM nodes.
 */
object CssCascadeResolver {

    fun computeStyle(
        node: DomNode,
        stylesheet: CssStylesheet,
        cache: MutableMap<DomNode, ComputedElementStyle>? = null
    ): ComputedElementStyle {
        if (cache != null) {
            val cached = cache[node]
            if (cached != null) return cached
        }

        val baseRules = mutableListOf<Pair<Int, Map<String, String>>>()
        val activeRules = mutableListOf<Pair<Int, Map<String, String>>>()
        val hoverRules = mutableListOf<Pair<Int, Map<String, String>>>()
        val focusRules = mutableListOf<Pair<Int, Map<String, String>>>()
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
                        sel.pseudoClass == "focus" -> focusRules.add(Pair(sel.specificity, rule.declarations))
                        sel.pseudoClass != null -> { /* Unhandled pseudo-classes do not pollute base rules */ }
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
        val finalFocus = mergeDeclarations(focusRules)
        val finalBefore = if (beforeRules.isNotEmpty()) mergeDeclarations(beforeRules) else null
        val finalAfter = if (afterRules.isNotEmpty()) mergeDeclarations(afterRules) else null

        // Resolve CSS Variables: var(--name, fallback)
        val result = ComputedElementStyle(
            base = CssVariableResolver.resolveVariables(finalBase, stylesheet.customProperties),
            active = CssVariableResolver.resolveVariables(finalActive, stylesheet.customProperties),
            hover = CssVariableResolver.resolveVariables(finalHover, stylesheet.customProperties),
            focus = CssVariableResolver.resolveVariables(finalFocus, stylesheet.customProperties),
            before = finalBefore?.let { CssVariableResolver.resolveVariables(it, stylesheet.customProperties) },
            after = finalAfter?.let { CssVariableResolver.resolveVariables(it, stylesheet.customProperties) }
        )
        cache?.put(node, result)
        return result
    }

    fun matchesNode(node: DomNode, sel: CssSelector): Boolean {
        // Tag check
        if (sel.tag != null && !sel.tag.equals(node.tag, ignoreCase = true)) {
            return false
        }
        // Class check
        if (sel.classNames.isNotEmpty() && sel.classNames.any { required ->
                node.classNames.none { it.equals(required, ignoreCase = true) }
            }) {
            return false
        }
        if (sel.classNames.isEmpty() && sel.className != null && !node.classNames.any { it.equals(sel.className, ignoreCase = true) }) {
            return false
        }
        // ID check
        if (sel.id != null && !sel.id.equals(node.id, ignoreCase = true)) {
            return false
        }
        // Ancestor check
        if (sel.ancestorSelector != null) {
            var curr = node.parent
            if (sel.ancestorCombinator == ">") {
                return curr != null && matchesNode(curr, sel.ancestorSelector)
            }
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
}
