package com.sanket.tools.nexpad.nxprc.engine.dom

class DomNode(
    val tag: String,
    val id: String? = null,
    val classNames: List<String> = emptyList(),
    val inlineStyles: Map<String, String> = emptyMap(),
    val attributes: Map<String, String> = emptyMap(),
    var textContent: String = "",
    val children: MutableList<DomNode> = mutableListOf(),
    val nodeIndex: Int = nextNodeIndex()
) {
    var parent: DomNode? = null

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = nodeIndex

    companion object {
        private val counter = java.util.concurrent.atomic.AtomicInteger(0)
        private fun nextNodeIndex(): Int = counter.incrementAndGet()
    }

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

    fun getAllSvgPaths(): List<String> = getAllSvgShapes().map { it.pathData }

    fun getAllSvgShapes(): List<com.sanket.tools.nexpad.nxprc.engine.parsers.SvgShapeElement> {
        val shapes = mutableListOf<com.sanket.tools.nexpad.nxprc.engine.parsers.SvgShapeElement>()
        fun recurse(node: DomNode) {
            val path = com.sanket.tools.nexpad.nxprc.engine.parsers.SvgGeometryParser.toPathData(node)
            if (!path.isNullOrBlank()) {
                val fill = com.sanket.tools.nexpad.nxprc.engine.parsers.SvgGeometryParser.parseFill(node)
                val stroke = com.sanket.tools.nexpad.nxprc.engine.parsers.SvgGeometryParser.parseStroke(node)
                shapes.add(com.sanket.tools.nexpad.nxprc.engine.parsers.SvgShapeElement(path, fill, stroke))
            }
            node.children.forEach { recurse(it) }
        }
        recurse(this)
        return shapes
    }
}
