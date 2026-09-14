package com.sanket.tools.nexpad.nxprc.engine.parsers

/**
 * Four-corner border radii in density-independent pixels.
 */
data class CornerRadii(
    val topLeft: Float = 0f,
    val topRight: Float = 0f,
    val bottomRight: Float = 0f,
    val bottomLeft: Float = 0f
)

/**
 * Rectangular inset offsets for CSS padding, margins, or absolute insets.
 */
data class InsetRect(
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f
)

/**
 * Semantic representation of a CSS positional offset (left, right, top, bottom, inset).
 * Explicitly distinguishes unspecified/auto offsets from explicit pixels or percentages.
 */
sealed class CssPositionValue {
    data object Unspecified : CssPositionValue()
    data object Auto : CssPositionValue()
    data class Px(val value: Float) : CssPositionValue()
    data class Percent(val value: Float) : CssPositionValue()

    fun resolve(parentDim: Float): Float? = when (this) {
        is Px -> value
        is Percent -> (value / 100f) * parentDim
        is Auto, is Unspecified -> null
    }

    val isExplicit: Boolean
        get() = this is Px || this is Percent
}

/**
 * Group of positional constraints along horizontal and vertical axes for an element.
 */
data class PositionConstraints(
    val left: CssPositionValue = CssPositionValue.Unspecified,
    val right: CssPositionValue = CssPositionValue.Unspecified,
    val top: CssPositionValue = CssPositionValue.Unspecified,
    val bottom: CssPositionValue = CssPositionValue.Unspecified
) {
    val hasExplicitHorizontal: Boolean
        get() = left.isExplicit || right.isExplicit

    val hasExplicitVertical: Boolean
        get() = top.isExplicit || bottom.isExplicit
}

/**
 * Absolute or parent-relative bounding coordinates computed by the box model or flex layout engine.
 */
data class ComputedBoxBounds(
    val left: Float = 0f,
    val top: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)

/**
 * Parsed CSS clip-path representation supporting polygons, vector SVG paths, and oval shapes.
 */
data class ParsedClipShape(
    val shapeType: String, // POLYGON, PATH, OVAL
    val polygonSides: Int = 0,
    val pathData: String = "",
    val normalizedVertices: List<Pair<Float, Float>> = emptyList()
)
