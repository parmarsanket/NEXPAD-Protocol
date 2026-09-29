package com.sanket.tools.nexpad.nxprc.engine.classifier

import com.sanket.tools.nexpad.nxprc.engine.model.ShapeDescriptor
import com.sanket.tools.nexpad.nxprc.engine.model.SizeMetrics
import com.sanket.tools.nexpad.nxprc.engine.model.isOval
import com.sanket.tools.nexpad.nxprc.engine.parsers.CornerRadii
import com.sanket.tools.nexpad.nxprc.engine.parsers.ParsedClipShape

/**
 * Single authoritative shape classifier for NXPRC canvas layers.
 *
 * Replaces the four independently copy-pasted `isOval = (radii.topLeft >= buttonWidth * 0.35f ...)` 
 * blocks in [NxprcCompiler] and the duplicate string checks in [DomTreeCompiler].
 *
 * Call [classify] once per element at compile time. The returned [ShapeDescriptor]
 * carries everything the layer builder needs — shape type, corner radii, polygon
 * sides, and path data.
 *
 * Classification priority (mirrors CSS cascade):
 * 1. Explicit `clip-path` → determines shape from the clip geometry
 * 2. `border-radius: 50%` CSS string → always Oval
 * 3. All four corners ≥ [SizeMetrics.ovalCornerThreshold] → Oval
 * 4. Otherwise → RoundedRect with the parsed corner radii
 */
object ShapeClassifier {

    /**
     * Classifies the shape of a CSS element into a [ShapeDescriptor].
     *
     * @param radii            Parsed corner radii in dp.
     * @param width            Element width in dp.
     * @param height           Element height in dp.
     * @param borderRadiusCss  Raw CSS `border-radius` string (checked for "50%").
     * @param clipPath         Parsed `clip-path` result (takes highest priority).
     */
    fun classify(
        radii: CornerRadii,
        width: Float,
        height: Float,
        borderRadiusCss: String? = null,
        clipPath: ParsedClipShape? = null
    ): ShapeDescriptor {
        // Priority 1: explicit clip-path overrides everything
        if (clipPath != null) return ShapeDescriptor.fromClip(clipPath)

        // Priority 2: CSS "50%" is always a full circle/oval
        if (borderRadiusCss?.contains("50%") == true) return ShapeDescriptor.Oval

        // Priority 3: corner-radii threshold check (uses SizeMetrics.OVAL_RADIUS_RATIO)
        // Only elements that are roughly circular/square (aspect ratio 0.8..1.25) can be classified
        // as an Oval via corner radius threshold. High-aspect-ratio elements (e.g. 3x33 strips or pills)
        // are RoundedRect with their defined radii, preserving straight sides.
        val metrics = SizeMetrics(width, height)
        val isNearSquare = metrics.aspectRatio in 0.8f..1.25f
        if (isNearSquare && radii.isOval(metrics)) return ShapeDescriptor.Oval

        // Default: rounded rectangle
        return ShapeDescriptor.RoundedRect(radii)
    }

    /**
     * Convenience overload — derives dimensions from [metrics].
     */
    fun classify(
        radii: CornerRadii,
        metrics: SizeMetrics,
        borderRadiusCss: String? = null,
        clipPath: ParsedClipShape? = null
    ): ShapeDescriptor = classify(
        radii          = radii,
        width          = metrics.buttonWidth,
        height         = metrics.buttonHeight,
        borderRadiusCss = borderRadiusCss,
        clipPath       = clipPath
    )
}
