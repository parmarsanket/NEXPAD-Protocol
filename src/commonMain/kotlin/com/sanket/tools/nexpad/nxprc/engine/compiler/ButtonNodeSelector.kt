package com.sanket.tools.nexpad.nxprc.engine.compiler

import com.sanket.tools.nexpad.nxprc.engine.css.CssCascadeResolver
import com.sanket.tools.nexpad.nxprc.engine.css.CssSelector
import com.sanket.tools.nexpad.nxprc.engine.css.CssStylesheet
import com.sanket.tools.nexpad.nxprc.engine.dom.DomNode

/**
 * Heuristics and tree-search algorithms to identify the primary controller button
 * container, text label leaves, and element visibility from parsed DOM markup.
 */
internal object ButtonNodeSelector {

    /**
     * Determines whether an element has visible rendering based on CSS display and visibility properties.
     */
    fun isVisible(props: Map<String, String>): Boolean {
        val d = props["display"]?.trim()?.lowercase()
        val v = props["visibility"]?.trim()?.lowercase()
        return d != "none" && v != "hidden"
    }

    /**
     * Locates the primary gamepad button node in the DOM tree using three progressive tiers:
     * 1. Explicit <button> tag with gamepad metadata or button-related class names.
     * 2. Semantic class names containing button/gamepad keywords.
     * 3. <body> child matching custom CSS stylesheet selectors.
     */
    fun findPrimaryButtonNode(root: DomNode, stylesheet: CssStylesheet): DomNode {
        // 0. Explicit data-primitive="box" container or gamepad metadata
        root.findFirst {
            it.attributes["data-primitive"]?.equals("box", ignoreCase = true) == true
        }?.let { return it }

        root.findFirst {
            it.attributes["data-control"] != null && it.attributes["data-category"] != null
        }?.let { return it }

        // 1. Explicit <button> tag
        val buttons = root.findByTag("button")
        buttons.firstOrNull {
            it.classNames.any { cls -> cls.equals("nexpad-btn", true) || cls.endsWith("-btn", true) }
        }?.let { return it }
        if (buttons.isNotEmpty()) return buttons[0]

        // 2. Class names matching button keywords
        val keywords = listOf("btn", "button", "pad", "nexpad", "control", "key", "trigger", "action", "circle", "dpad", "stick", "thumb", "bumper", "wedge", "switch", "knob", "hud")
        val candidates = mutableListOf<DomNode>()
        fun scan(node: DomNode) {
            if (node.classNames.any { cls -> keywords.any { kw -> cls.contains(kw, ignoreCase = true) } }) {
                candidates.add(node)
            }
            node.children.forEach { scan(it) }
        }
        scan(root)
        if (candidates.isNotEmpty()) return candidates[0]

        // 3. Search children of <body> for an element matching CSS rules
        val bodyNode = root.findByTag("body").firstOrNull() ?: root
        for (child in bodyNode.children) {
            val hasMatchingRules = stylesheet.rules.any { r -> r.selectors.any { sel -> matchesNode(child, sel) } }
            if (hasMatchingRules) return child
        }

        return bodyNode.children.firstOrNull() ?: root.children.firstOrNull() ?: root
    }

    /**
     * Identifies the primary text label DOM node inside the button hierarchy,
     * prioritizing elements with `.btn-label`, `.label`, `.text`, `.glyph` classes.
     */
    fun findRealTextNode(node: DomNode): DomNode? {
        val labeledChild = node.children.firstOrNull { child ->
            child.classNames.any { it.contains("label") || it.contains("text") || it.contains("glyph") } &&
                    child.findFirstText()?.isNotBlank() == true
        }
        if (labeledChild != null) return findRealTextNode(labeledChild) ?: labeledChild

        if (node.textContent.isNotBlank()) {
            return node
        }
        for (child in node.children) {
            findRealTextNode(child)?.let { return it }
        }
        return null
    }

    /**
     * Collects all terminal element leaf nodes that contain non-blank text content.
     */
    fun collectTextLeaves(primaryNode: DomNode): List<DomNode> {
        val textNodes = mutableListOf<DomNode>()
        fun scan(node: DomNode) {
            val elementChildren = node.children
            if (elementChildren.isEmpty() && node.findFirstText() != null) {
                textNodes.add(node)
            } else {
                elementChildren.forEach { scan(it) }
            }
        }
        scan(primaryNode)
        return textNodes
    }

    private fun matchesNode(node: DomNode, sel: CssSelector): Boolean {
        return CssCascadeResolver.matchesNode(node, sel)
    }
}
