package com.sanket.tools.nexpad.nxprc.engine.css

/**
 * Industry-standard CSS Selector Parser.
 *
 * Parses raw CSS selector strings into structured [CssSelector] AST nodes.
 * Accurately calculates:
 * - Specificity according to W3C rules (IDs = 100, Classes/Pseudo-classes = 10, Elements/Pseudo-elements = 1)
 * - Combinators: child ('>') and descendant (' ')
 * - Pseudo-elements: ::before, ::after (and legacy :before, :after)
 * - Pseudo-classes: :active, :hover, :focus
 * - Compound class names (.btn.active) and ID selectors (#btn)
 */
object CssSelectorParser {
    private val CLASS_REGEX = CssRulePattern.CLASS_SELECTOR.regex

    fun parse(rawSelector: String): CssSelector {
        var s = rawSelector.trim()
        var pseudoEl: String? = null
        var pseudoCl: String? = null

        // 1. Extract pseudo-element (::before, ::after or double-colon pseudo-elements)
        if (s.contains("::")) {
            val parts = s.split("::", limit = 2)
            s = parts[0]
            val rest = parts[1].trim().lowercase()
            if (rest.contains(":")) {
                val subParts = rest.split(":", limit = 2)
                pseudoEl = subParts[0]
                if (pseudoCl == null) pseudoCl = subParts[1]
            } else {
                pseudoEl = rest
            }
        }

        // 2. Extract pseudo-class (:active, :hover, etc. or legacy single-colon :before, :after)
        if (s.contains(":")) {
            val parts = s.split(":", limit = 2)
            s = parts[0]
            val pseudo = parts[1].trim().lowercase()
            if (pseudo == "before" || pseudo == "after") {
                if (pseudoEl == null) pseudoEl = pseudo
            } else {
                pseudoCl = pseudo
            }
        }

        var ancestorSelector: CssSelector? = null
        var ancestorCombinator = " "
        var specificity = 0

        // Handle child selector: e.g. ".button-a > span"
        if (s.contains(">")) {
            val split = s.lastIndexOf('>')
            val ancestorPart = s.substring(0, split).trim()
            s = s.substring(split + 1).trim()
            if (ancestorPart.isNotBlank()) {
                ancestorSelector = parse(ancestorPart)
                ancestorCombinator = ">"
                specificity += ancestorSelector.specificity
            }
        // Handle descendant selector: e.g. ".button-a span"
        } else if (s.contains(" ")) {
            val lastSpace = s.lastIndexOf(' ')
            val ancestorPart = s.substring(0, lastSpace).trim()
            s = s.substring(lastSpace + 1).trim()
            if (ancestorPart.isNotBlank()) {
                val parsedAncestor = parse(ancestorPart)
                ancestorSelector = parsedAncestor
                ancestorCombinator = " "
                specificity += parsedAncestor.specificity
            }
        }

        var id: String? = null
        var className: String? = null
        var tag: String? = null

        if (s.contains("#")) {
            val parts = s.split("#", limit = 2)
            if (parts[0].isNotBlank()) tag = parts[0].trim().lowercase()
            val sub = parts[1].split(".", limit = 2)
            id = sub[0].trim()
            if (sub.size > 1) className = sub[1].trim()
        } else if (s.contains(".")) {
            val parts = s.split(".", limit = 2)
            if (parts[0].isNotBlank()) tag = parts[0].trim().lowercase()
            className = parts[1].trim()
        } else if (s.isNotBlank() && s != "*") {
            tag = s.trim().lowercase()
        }

        val parsedClasses = CLASS_REGEX
            .findAll(s)
            .map { it.groupValues[1] }
            .toList()
        if (parsedClasses.isNotEmpty()) {
            className = parsedClasses.first()
        }

        // Strictly additive CSS specificity accumulation (W3C standard)
        if (id != null) specificity += 100
        if (parsedClasses.isNotEmpty()) {
            specificity += 10 * parsedClasses.size
        } else if (className != null) {
            specificity += 10
        }
        if (tag != null && tag != "*") specificity += 1
        if (pseudoEl != null) specificity += 1
        if (pseudoCl != null) specificity += 10

        return CssSelector(
            raw = rawSelector,
            tag = tag,
            className = className,
            classNames = parsedClasses,
            id = id,
            pseudoClass = pseudoCl,
            pseudoElement = pseudoEl,
            ancestorSelector = ancestorSelector,
            ancestorCombinator = ancestorCombinator,
            specificity = specificity
        )
    }
}
