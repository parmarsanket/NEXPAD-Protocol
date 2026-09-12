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
