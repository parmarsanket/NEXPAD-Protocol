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

    fun findFirst(predicate: (DomNode) -> Boolean): DomNode? {
        if (predicate(this)) return this
        for (child in children) {
            val res = child.findFirst(predicate)
            if (res != null) return res
        }
        return null
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

    fun findRoot(): DomNode {
        var curr = this
        while (curr.parent != null) {
            curr = curr.parent!!
        }
        return curr
    }

    fun getAllSvgPaths(): List<String> = getAllSvgShapes().map { it.pathData }

    fun getAllSvgShapes(
        stylesheet: com.sanket.tools.nexpad.nxprc.engine.css.CssStylesheet? = null,
        paintServers: Map<String, com.sanket.tools.nexpad.nxprc.FillBrush> = emptyMap()
    ): List<com.sanket.tools.nexpad.nxprc.engine.parsers.SvgShapeElement> {
        val shapes = mutableListOf<com.sanket.tools.nexpad.nxprc.engine.parsers.SvgShapeElement>()

        // 1. Calculate root SVG viewBox normalization matrix if applicable
        val vbAttr = if (tag.equals("svg", ignoreCase = true)) attributes["viewBox"] else null
        val vbMatrix = if (!vbAttr.isNullOrBlank()) {
            val nums = com.sanket.tools.nexpad.nxprc.engine.parsers.ColorPattern.DELIMITER
                .split(vbAttr.trim())
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toFloatOrNull() }
            if (nums.size == 4 && nums[2] > 0.001f && nums[3] > 0.001f) {
                val minX = nums[0]
                val minY = nums[1]
                val vbW = nums[2]
                val vbH = nums[3]
                if (minX != 0f || minY != 0f || vbW != 100f || vbH != 100f) {
                    val sx = 100f / vbW
                    val sy = 100f / vbH
                    com.sanket.tools.nexpad.nxprc.engine.parsers.AffineMatrix2D(
                        a = sx, b = 0f, c = 0f, d = sy, e = -minX * sx, f = -minY * sy
                    )
                } else {
                    com.sanket.tools.nexpad.nxprc.engine.parsers.AffineMatrix2D.IDENTITY
                }
            } else {
                com.sanket.tools.nexpad.nxprc.engine.parsers.AffineMatrix2D.IDENTITY
            }
        } else {
            com.sanket.tools.nexpad.nxprc.engine.parsers.AffineMatrix2D.IDENTITY
        }

        fun recurse(node: DomNode, currentMatrix: com.sanket.tools.nexpad.nxprc.engine.parsers.AffineMatrix2D) {
            if (node.tag.equals("defs", ignoreCase = true)) return

            // Compute cumulative transform from attributes or inline styling
            val nodeTransformStr = node.attributes["transform"] ?: node.inlineStyles["transform"]
            val nodeMatrix = if (!nodeTransformStr.isNullOrBlank()) {
                currentMatrix.multiply(com.sanket.tools.nexpad.nxprc.engine.parsers.AffineMatrix2D.parseTransform(nodeTransformStr))
            } else {
                currentMatrix
            }

            val rawPath = com.sanket.tools.nexpad.nxprc.engine.parsers.SvgGeometryParser.toPathData(node)
            if (!rawPath.isNullOrBlank()) {
                val finalPath = if (!nodeMatrix.isIdentity) {
                    com.sanket.tools.nexpad.nxprc.engine.parsers.SvgGeometryParser.transformPathData(rawPath, nodeMatrix)
                } else {
                    rawPath
                }
                val fill = com.sanket.tools.nexpad.nxprc.engine.parsers.SvgGeometryParser.parseFill(node, stylesheet, paintServers)
                val stroke = com.sanket.tools.nexpad.nxprc.engine.parsers.SvgGeometryParser.parseStroke(node, stylesheet, paintServers)
                shapes.add(com.sanket.tools.nexpad.nxprc.engine.parsers.SvgShapeElement(finalPath, fill, stroke))
            }
            node.children.forEach { recurse(it, nodeMatrix) }
        }
        recurse(this, vbMatrix)
        return shapes
    }
}
