package com.sanket.tools.nexpad.nxprc.engine.model

import com.sanket.tools.nexpad.nxprc.LayerShapeType
import com.sanket.tools.nexpad.nxprc.engine.parsers.CornerRadii
import com.sanket.tools.nexpad.nxprc.engine.parsers.ParsedClipShape

/**
 * Strongly-typed sealed hierarchy describing the visual shape of a canvas layer.
 *
 * Previously, shape detection used inline `0.35f` comparisons scattered across
 * [NxprcCompiler] (4 separate copies). Now, [ShapeClassifier.classify] is called
 * once and returns a single [ShapeDescriptor] that carries all shape information
 * needed downstream — no re-detection, no magic numbers at call sites.
 *
 * Serialisation note: the [layerShapeType] property maps back to the
 * [LayerShapeType] enum whose string IDs are preserved in the .nxprc binary format
 * for full backward compatibility.
 */
sealed class ShapeDescriptor {

    /** True circular or elliptical shape (border-radius ≥ 50 % or all corners ≥ oval threshold). */
    data object Oval : ShapeDescriptor()

    /** Standard rectangular shape with individually-rounded corners. */
    data class RoundedRect(val radii: CornerRadii) : ShapeDescriptor()

    /** Regular polygon (equilateral). [sides] must be ≥ 3. */
    data class Polygon(val sides: Int) : ShapeDescriptor()

    /** Arbitrary SVG path (clip-path: path(...)). */
    data class Path(val svgData: String) : ShapeDescriptor()

    /** Regular hexagonal shape — shortcut for Polygon(6) with hex-specific rendering. */
    data object Hexagon : ShapeDescriptor()

    /** Regular octagonal shape — shortcut for Polygon(8) with oct-specific rendering. */
    data object Octagon : ShapeDescriptor()

    // ── Adapters ─────────────────────────────────────────────────────────────

    /** The [LayerShapeType] enum value — preserves serialized string IDs for .nxprc compat. */
    val layerShapeType: LayerShapeType
        get() = when (this) {
            is Oval       -> LayerShapeType.OVAL
            is RoundedRect -> LayerShapeType.ROUNDED_RECT
            is Polygon    -> LayerShapeType.POLYGON
            is Path       -> LayerShapeType.PATH
            is Hexagon    -> LayerShapeType.HEXAGON
            is Octagon    -> LayerShapeType.OCTAGON
        }

    /** String ID for use in [CanvasLayer] `shapeType` fields (backward compat). */
    val shapeTypeId: String get() = layerShapeType.name

    /** Polygon side count — 0 for non-polygon shapes. */
    val polygonSides: Int
        get() = when (this) {
            is Polygon -> sides
            is Hexagon -> 6
            is Octagon -> 8
            else -> 0
        }

    /** SVG path data — empty for non-path shapes. */
    val pathData: String
        get() = when (this) {
            is Path -> svgData
            else    -> ""
        }

    /** Corner radii — zero for non-rounded-rect shapes. */
    val cornerRadii: CornerRadii
        get() = when (this) {
            is RoundedRect -> radii
            else -> CornerRadii()
        }

    companion object {
        /** Construct from a parsed CSS clip-path result. */
        fun fromClip(clip: ParsedClipShape): ShapeDescriptor = when (clip.shapeType) {
            "OVAL"     -> Oval
            "HEXAGON"  -> Hexagon
            "OCTAGON"  -> Octagon
            "POLYGON"  -> Polygon(clip.polygonSides)
            "PATH"     -> Path(clip.pathData)
            else       -> when {
                clip.polygonSides > 0 -> Polygon(clip.polygonSides)
                clip.pathData.isNotEmpty() -> Path(clip.pathData)
                else -> RoundedRect(CornerRadii())
            }
        }
    }
}

/**
 * Extension: returns true if all four corners of this [CornerRadii] qualify as
 * "oval" given the button's [SizeMetrics].
 *
 * Centralises the `radii.topLeft >= (width * 0.35f) && ...` check that was
 * previously copy-pasted four times across [NxprcCompiler].
 */
fun CornerRadii.isOval(metrics: SizeMetrics): Boolean {
    val threshold = metrics.ovalCornerThreshold
    return topLeft >= threshold && topRight >= threshold &&
           bottomRight >= threshold && bottomLeft >= threshold
}

/**
 * Extension: returns true if all four corners qualify as oval given explicit dimensions
 * (convenience overload when a [SizeMetrics] instance is not available).
 */
fun CornerRadii.isOval(buttonWidth: Float, buttonHeight: Float): Boolean =
    isOval(SizeMetrics(buttonWidth, buttonHeight))
