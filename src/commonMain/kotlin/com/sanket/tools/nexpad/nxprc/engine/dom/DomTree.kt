package com.sanket.tools.nexpad.nxprc.engine.dom

data class DomNode(
    val tag: String,
    val id: String? = null,
    val classNames: List<String> = emptyList(),
    val inlineStyles: Map<String, String> = emptyMap(),
    val attributes: Map<String, String> = emptyMap(),
    var textContent: String = "",
    val children: MutableList<DomNode> = mutableListOf(),
    var parent: DomNode? = null
) {
    fun findByTag(tagName: String): List<DomNode> {
        val results = mutableListOf<DomNode>()
        fun recurse(node: DomNode) {
            if (node.tag.equals(tagName, ignoreCase = true)) {
                results.add(node)
            }
            node.children.forEach { recurse(it) }
        }
        recurse(this)
        return results
    }

    fun findFirstText(): String? {
        if (textContent.isNotBlank()) return textContent.trim()
        for (child in children) {
            val childText = child.findFirstText()
            if (!childText.isNullOrBlank()) return childText
        }
        return null
    }

    fun getAllSvgPaths(): List<String> {
        val paths = mutableListOf<String>()
        fun recurse(node: DomNode) {
            if (node.tag.equals("path", ignoreCase = true)) {
                node.attributes["d"]?.let { if (it.isNotBlank()) paths.add(it) }
            }
            node.children.forEach { recurse(it) }
        }
        recurse(this)
        return paths
    }
}
