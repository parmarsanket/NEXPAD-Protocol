package com.sanket.tools.nexpad.nxprc.engine.classifier

import com.sanket.tools.nexpad.nxprc.NxprcCategory
import com.sanket.tools.nexpad.nxprc.engine.dom.DomNode

/**
 * Authoritative DOM node role classifier for NXPRC compilation.
 *
 * Replaces the tangled `contains("base") || contains("socket") || contains("thumb")`
 * heuristics that were scattered across [DomTreeCompiler.isNodeThumbCap] (150+ lines)
 * and [ButtonNodeSelector]. Those heuristics would silently misclassify any element
 * whose class/id used non-English or abbreviated naming conventions.
 *
 * Design principles:
 * - **One definition** — all keyword sets live here; no inline string comparisons elsewhere.
 * - **Priority order**: explicit data-attribute > ancestor context > direct class/id > fallback.
 * - **Fail-safe** — an unrecognised element defaults to [NodeRole.DECORATIVE_LAYER],
 *   which is always rendered (unlike UNKNOWN which might be skipped).
 * - **Extensible** — add keywords to the private sets without touching compiler logic.
 */
object NodeRoleClassifier {

    /**
     * Semantic role of a DOM node within a controller button hierarchy.
     *
     * The role drives z-ordering (thumb cap layers above base), layer type selection,
     * and element visibility during compilation.
     */
    enum class NodeRole {
        /** Moveable joystick cap — rendered in the [LayerStack.THUMB_CAP_OFFSET] zone. */
        THUMB_CAP,
        /** Fixed housing / bezel / chassis — stays in the base layer zone. */
        BASE_SOCKET,
        /** An inline `<svg>` acting as the button's surface artwork. */
        SURFACE_SVG,
        /** Primary text or glyph label. */
        TEXT_LABEL,
        /** Visual decoration that draws but does not define the button outline. */
        DECORATIVE_LAYER,
        /** Explicit data-primitive="box" container element. */
        BOX_PRIMITIVE,
        /** Node is explicitly hidden (display:none / visibility:hidden) — skip. */
        HIDDEN
    }

    // ── Keyword dictionaries ─────────────────────────────────────────────────

    private val THUMB_KEYWORDS = setOf(
        "thumb", "cap", "dome", "knob", "grip",
        "stick-label", "thumb-cap", "stick-cap", "thumbstick-cap",
        "top", "nub", "puck"
    )

    private val BASE_KEYWORDS = setOf(
        "base", "bezel", "chassis", "housing", "outer", "ring",
        "socket", "tick", "axis", "marker", "node", "guide",
        "well", "groove", "recess", "cradle", "mount", "plate",
        "frame", "border", "surround", "shell", "body", "bg", "background"
    )

    /** Keywords that explicitly override a thumb-cap ancestor container. */
    private val EXPLICIT_BASE_KEYWORDS = setOf("base", "socket")

    private val TEXT_KEYWORDS = setOf(
        "label", "text", "glyph", "letter", "char", "caption",
        "title", "name", "symbol", "icon-text", "badge"
    )

    private val SVG_SURFACE_KEYWORDS = setOf("surface", "bg", "background", "artwork", "canvas-svg")

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Classifies the semantic role of [node] within a controller button hierarchy.
     *
     * @param node     The DOM node to classify.
     * @param category The compiled button's [NxprcCategory] — required to apply
     *                 joystick-specific cap/base rules.
     * @param props    The node's computed CSS properties (pass base map). Used to
     *                 detect `display:none` / `visibility:hidden`.
     */
    fun classify(
        node: DomNode,
        category: NxprcCategory,
        props: Map<String, String> = emptyMap()
    ): NodeRole {
        // 0. Visibility check first
        val display = props["display"]?.trim()?.lowercase()
        val visibility = props["visibility"]?.trim()?.lowercase()
        if (display == "none" || visibility == "hidden") return NodeRole.HIDDEN

        // 1. Explicit data-attributes take highest precedence (data-layer-role, data-role, data-motion-group, data-primitive)
        val dataLayerRole = (node.attributes["data-layer-role"] ?: node.attributes["data-role"])?.trim()?.lowercase()
        when (dataLayerRole) {
            "background", "base", "socket" -> return NodeRole.BASE_SOCKET
            "surface", "artwork", "svg" -> return NodeRole.SURFACE_SVG
            "thumb", "cap" -> return NodeRole.THUMB_CAP
            "text", "label" -> return NodeRole.TEXT_LABEL
            "detail", "decorative", "box", "emblem" -> return NodeRole.BOX_PRIMITIVE
        }

        val motionGroup = node.attributes["data-motion-group"]?.trim()?.lowercase()
        when (motionGroup) {
            "cap", "thumb" -> return NodeRole.THUMB_CAP
            "base", "socket", "fixed" -> return NodeRole.BASE_SOCKET
        }

        val dataPrimitive = node.attributes["data-primitive"]?.lowercase()
        if (dataPrimitive == "box") return NodeRole.BOX_PRIMITIVE
        if (dataPrimitive == "thumb" || dataPrimitive == "cap") return NodeRole.THUMB_CAP
        if (dataPrimitive == "base" || dataPrimitive == "socket") return NodeRole.BASE_SOCKET
        if (dataPrimitive == "text" || dataPrimitive == "label") return NodeRole.TEXT_LABEL
        if (dataPrimitive == "svg" || dataPrimitive == "surface") return NodeRole.SURFACE_SVG

        // 2. Inline <svg> tag → surface SVG (if it has known surface class or is the only svg child)
        if (node.tag.equals("svg", ignoreCase = true)) {
            val tokens = node.allIdentifierTokens()
            return if (tokens.any { it in SVG_SURFACE_KEYWORDS }) NodeRole.SURFACE_SVG
            else NodeRole.DECORATIVE_LAYER
        }

        val tokens = node.allIdentifierTokens()

        // 3. Text-label detection (independent of category)
        if (tokens.any { it in TEXT_KEYWORDS } && node.hasTextContent()) return NodeRole.TEXT_LABEL

        // 4. Joystick/touchpad-specific cap vs. base classification
        if (category == NxprcCategory.JOYSTICK || category == NxprcCategory.TOUCHPAD) {
            return classifyJoystickChild(node, tokens)
        }

        // 5. Generic decorative layer (renders fine at default z-order)
        return NodeRole.DECORATIVE_LAYER
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun classifyJoystickChild(node: DomNode, tokens: Set<String>): NodeRole {
        // Walk ancestors — container role takes precedence over direct element class
        var ancestor = node.parent
        while (ancestor != null && !ancestor.tag.equals("button", ignoreCase = true)) {
            val aTokens = ancestor.allIdentifierTokens()
            when {
                aTokens.any { it in THUMB_KEYWORDS } -> {
                    // Inside a thumb-cap container → child is a cap unless explicitly marked base or socket
                    return if (tokens.any { it in EXPLICIT_BASE_KEYWORDS }) NodeRole.BASE_SOCKET
                    else NodeRole.THUMB_CAP
                }
                aTokens.any { it in BASE_KEYWORDS } -> return NodeRole.BASE_SOCKET
            }
            ancestor = ancestor.parent
        }
        // Direct element classification
        return when {
            tokens.any { it in THUMB_KEYWORDS } -> NodeRole.THUMB_CAP
            tokens.any { it in BASE_KEYWORDS }  -> NodeRole.BASE_SOCKET
            else                                 -> NodeRole.DECORATIVE_LAYER
        }
    }
}

// ── DomNode extensions ───────────────────────────────────────────────────────

/**
 * Returns a normalised set of all identifier tokens (class names + id) for this node,
 * lowercased and split on `-_` boundaries so "thumbCap" → {"thumbcap"} and
 * "thumb-cap" → {"thumb", "cap", "thumb-cap"}.
 */
fun DomNode.allIdentifierTokens(): Set<String> {
    val raw = (classNames + listOfNotNull(id)).joinToString(" ").lowercase()
    val tokens = mutableSetOf<String>()
    // Add the full tokens (e.g. "thumb-cap") AND individual parts (e.g. "thumb", "cap")
    raw.split(" ", "_").forEach { token ->
        if (token.isNotBlank()) {
            tokens.add(token)
            token.split("-").forEach { part -> if (part.isNotBlank()) tokens.add(part) }
        }
    }
    return tokens
}

/** True if this node has non-blank text content anywhere in its tree. */
fun DomNode.hasTextContent(): Boolean =
    textContent.isNotBlank() || findFirstText()?.isNotBlank() == true
