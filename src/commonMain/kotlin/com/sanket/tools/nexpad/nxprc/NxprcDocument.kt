package com.sanket.tools.nexpad.nxprc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Unlimited Vector Canvas & Animation Document (.nxprc).
 * Stored as a binary package with magic header "NXRC" (0x4E, 0x58, 0x52, 0x43).
 *
 * Single Source of Truth for both NEXPAD (Android) and NEXPADDesktop.
 */
@Serializable
data class NxprcDocument(
    val version: Int = 2,
    val manifest: NxprcManifest,
    val canvas: NxprcCanvas = NxprcCanvas(),
    val animations: NxprcAnimations = NxprcAnimations()
) {
    companion object {
        val MAGIC = byteArrayOf(0x4E, 0x58, 0x52, 0x43) // "NXRC"
        fun encodeToBytes(doc: NxprcDocument): ByteArray = NxprcBinaryCodec.encode(doc)

        fun decodeFromBytes(bytes: ByteArray): Result<NxprcDocument> = NxprcBinaryCodec.decode(bytes)
    }
}

/** Severity level for a compiler diagnostic. */
enum class WarningSeverity { INFO, WARNING, DROPPED }

/**
 * A diagnostic emitted during HTML→NXPRC compilation.
 * Surfaces CSS properties that were silently ignored, lossy conversions, or fallback decisions.
 */
data class CompileWarning(
    val severity: WarningSeverity,
    val code: String,
    val message: String,
    val source: String = ""
)

/**
 * Result of a full HTML→NXPRC compilation.
 * Always contains the compiled [document]. Any [warnings] surfaces CSS properties
 * that were silently dropped or approximated during compilation.
 * An empty [warnings] list means the HTML compiled with full fidelity.
 */
data class CompileResult(
    val document: NxprcDocument,
    val warnings: List<CompileWarning> = emptyList()
) {
    /** True if any DROPPED-severity warnings were emitted (CSS fully lost). */
    val hasLosses: Boolean get() = warnings.any { it.severity == WarningSeverity.DROPPED }

    /** Formatted summary for display in the UI or AI retry prompts. */
    fun warningsSummary(): String = if (warnings.isEmpty()) ""
        else warnings.joinToString("\n") {
            "[${it.severity}] ${it.code}: ${it.message}" +
            if (it.source.isNotEmpty()) " (source: ${it.source})" else ""
        }
}

@Serializable
data class SpringPhysicsDef(
    val dampingRatio: Float = 0.75f,
    val stiffness: Float = 400f,
    val pressedScale: Float = 0.92f,
    val enabled: Boolean = true
)

@Serializable
data class NxprcManifest(
    val id: String,                    // e.g. "rc.cyber_hex_a" - must start with "rc."
    val name: String,
    val author: String = "Designer",
    val version: String = "1.0.0",
    val category: String = "BUTTON",   // BUTTON, DPAD, JOYSTICK, TRIGGER, BUMPER, HOME, SYSTEM, MACRO
    val defaultControl: String = "A",  // A, B, X, Y, LT, RT, LB, RB, LS, RS, UP, DOWN, etc.
    val widthDp: Int = 76,
    val heightDp: Int = 76,
    val description: String = "",
    val springPhysics: SpringPhysicsDef = SpringPhysicsDef()
)

@Serializable
enum class CompositingStrategy {
    AUTO,
    OFFSCREEN,
    MODULATE_ALPHA
}

@Serializable
data class LayerOutsets(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
) {
    val hasOutsets: Boolean
        get() = left > 0f || top > 0f || right > 0f || bottom > 0f

    operator fun plus(other: LayerOutsets): LayerOutsets = LayerOutsets(
        left = maxOf(left, other.left),
        top = maxOf(top, other.top),
        right = maxOf(right, other.right),
        bottom = maxOf(bottom, other.bottom)
    )
}

@Serializable
data class RenderEffectDef(
    val blurRadiusX: Float = 0f,
    val blurRadiusY: Float = 0f,
    val tileMode: String = "CLAMP"
)

@Serializable
data class NxprcCanvas(
    val viewBoxWidth: Float = 100f,
    val viewBoxHeight: Float = 100f,
    val layers: List<CanvasLayer> = emptyList(),
    val clipToBounds: Boolean = false,
    val canvasOutsets: LayerOutsets = LayerOutsets(),
    val capLayerIndices: List<Int> = emptyList()
)

@Serializable
data class BoxShadowDef(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val blurRadius: Float = 0f,
    val spreadRadius: Float = 0f,
    val color: Long = NxprcDefaults.DEFAULT_SHADOW_COLOR,
    val isInset: Boolean = false
)

/** CSS visual filter subset preserved in the NXPRC render model. */
@Serializable
data class FilterDef(
    val blurRadius: Float = 0f,
    val brightness: Float = 1f,
    val saturation: Float = 1f,
    val hueRotateDegrees: Float = 0f,
    val renderEffect: RenderEffectDef = RenderEffectDef()
)

@Serializable
data class TextShadowDef(
    val offsetX: Float = 0f,
    val offsetY: Float = 2f,
    val blurRadius: Float = 0f,
    val color: Long = NxprcDefaults.DEFAULT_SHADOW_COLOR
)

@Serializable
data class TransformDef(
    val rotationDegrees: Float = 0f,
    val offsetXRatio: Float = 0f,
    val offsetYRatio: Float = 0f,
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val skewX: Float = 0f,
    val skewY: Float = 0f,
    val originXRatio: Float = 0.5f,
    val originYRatio: Float = 0.5f,
    val isRotating: Boolean = false
) {
    val hasTransform: Boolean
        get() = rotationDegrees != 0f || offsetXRatio != 0f || offsetYRatio != 0f ||
                scaleX != 1.0f || scaleY != 1.0f || skewX != 0f || skewY != 0f || isRotating
}

@Serializable
data class EffectsDef(
    val opacity: Float = 1.0f,
    val filter: FilterDef = FilterDef(),
    val compositingStrategy: CompositingStrategy = CompositingStrategy.AUTO,
    val layerOutsets: LayerOutsets = LayerOutsets(),
    val drawCacheHint: Boolean = false
)

@Serializable
sealed class CanvasLayer {

    /**
     * Industry-grade generic CSS box layer.
     * Supports full CSS box model: rounded corners, multi-stop fills, borders,
     * outset box-shadows, inset box-shadows, opacity, transforms.
     */
    @Serializable
    @SerialName("BoxLayer")
    data class BoxLayer(
        val shapeType: String = "ROUNDED_RECT", // ROUNDED_RECT, OVAL, POLYGON, PATH
        val polygonSides: Int = 0,
        val pathData: String = "",
        val cornerRadiusTopLeft: Float = 14f,
        val cornerRadiusTopRight: Float = 14f,
        val cornerRadiusBottomRight: Float = 14f,
        val cornerRadiusBottomLeft: Float = 14f,
        val widthRatio: Float = 1.0f,
        val heightRatio: Float = 1.0f,
        val clipToBounds: Boolean = false,
        val fill: FillBrush = FillBrush.Solid(NxprcDefaults.DEFAULT_FILL_COLOR),
        val fills: List<FillBrush> = emptyList(),
        val stroke: StrokeStyle? = null,
        val boxShadows: List<BoxShadowDef> = emptyList(),
        // Flat legacy fields kept for JSON backward-compat (old files encoded them individually).
        // New code must NOT set these directly — write via [transform] and [effects] instead.
        // [effectiveTransform] always resolves the canonical authoritative TransformDef.
        val filter: FilterDef = FilterDef(),
        val opacity: Float = 1.0f,
        @Deprecated("Use transform.rotationDegrees", ReplaceWith("transform.rotationDegrees"))
        val rotationDegrees: Float = 0f,
        @Deprecated("Use transform.offsetXRatio", ReplaceWith("transform.offsetXRatio"))
        val offsetXRatio: Float = 0f,
        @Deprecated("Use transform.offsetYRatio", ReplaceWith("transform.offsetYRatio"))
        val offsetYRatio: Float = 0f,
        @Deprecated("Use transform.scaleX", ReplaceWith("transform.scaleX"))
        val scaleX: Float = 1.0f,
        @Deprecated("Use transform.scaleY", ReplaceWith("transform.scaleY"))
        val scaleY: Float = 1.0f,
        @Deprecated("Use transform.skewX", ReplaceWith("transform.skewX"))
        val skewX: Float = 0f,
        @Deprecated("Use transform.skewY", ReplaceWith("transform.skewY"))
        val skewY: Float = 0f,
        @Deprecated("Use transform.originXRatio", ReplaceWith("transform.originXRatio"))
        val originXRatio: Float = 0.5f,
        @Deprecated("Use transform.originYRatio", ReplaceWith("transform.originYRatio"))
        val originYRatio: Float = 0.5f,
        @Deprecated("Use transform.isRotating", ReplaceWith("transform.isRotating"))
        val isRotating: Boolean = false,
        /**
         * HIGH 3 FIX: [transform] is the single authoritative source of transform data.
         * The flat fields above exist solely for backward-compat JSON deserialization.
         * Always read transforms via [effectiveTransform].
         */
        val transform: TransformDef = TransformDef(),
        val effects: EffectsDef = EffectsDef(
            opacity = opacity,
            filter = filter
        )
    ) : CanvasLayer() {
        /**
         * Returns the canonical transform for this layer.
         * Priority order (HIGH 3 FIX):
         *   1. [transform] if it has any non-default values (newly encoded documents).
         *   2. Flat legacy fields (old documents encoded before the nested TransformDef existed).
         * This ensures both old and new .nxprc files decode correctly.
         */
        @Suppress("DEPRECATION")
        val effectiveTransform: TransformDef
            get() = if (transform.hasTransform) transform
                    else TransformDef(
                        rotationDegrees = rotationDegrees,
                        offsetXRatio = offsetXRatio,
                        offsetYRatio = offsetYRatio,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        skewX = skewX,
                        skewY = skewY,
                        originXRatio = originXRatio,
                        originYRatio = originYRatio,
                        isRotating = isRotating
                    )

        val effectiveEffects: EffectsDef
            get() = if (effects != EffectsDef()) effects else EffectsDef(
                opacity = opacity,
                filter = filter
            )
    }

    @Serializable
    @SerialName("VectorPath")
    data class VectorPath(
        val pathData: String,
        val fill: FillBrush = FillBrush.Solid(NxprcDefaults.DEFAULT_FILL_COLOR),
        val stroke: StrokeStyle? = StrokeStyle(NxprcDefaults.DEFAULT_ACCENT_COLOR, 2.5f),
        val rotationDegrees: Float = 0f,
        val isRotating: Boolean = false,
        val offsetXRatio: Float = 0f,
        val offsetYRatio: Float = 0f,
        val scale: Float = 1.0f
    ) : CanvasLayer()

    @Serializable
    @SerialName("GradientShape")
    data class GradientShape(
        val shapeType: String = "ROUNDED_RECT", // ROUNDED_RECT, OVAL, HEXAGON, OCTAGON
        val cornerRadius: Float = 14f,
        val fill: FillBrush = FillBrush.LinearGradient(listOf(NxprcDefaults.DEFAULT_FILL_COLOR, 0xFF003366L), 45f),
        val stroke: StrokeStyle? = StrokeStyle(NxprcDefaults.DEFAULT_ACCENT_COLOR, 2f),
        val filter: FilterDef = FilterDef(),
        val opacity: Float = 1.0f,
        // Flat legacy fields — kept for backward-compat JSON deserialization only.
        @Deprecated("Use transform.rotationDegrees", ReplaceWith("transform.rotationDegrees"))
        val rotationDegrees: Float = 0f,
        @Deprecated("Use transform.offsetXRatio", ReplaceWith("transform.offsetXRatio"))
        val offsetXRatio: Float = 0f,
        @Deprecated("Use transform.offsetYRatio", ReplaceWith("transform.offsetYRatio"))
        val offsetYRatio: Float = 0f,
        val widthRatio: Float = 1.0f,
        val heightRatio: Float = 1.0f,
        @Deprecated("Use transform.scaleX", ReplaceWith("transform.scaleX"))
        val scaleX: Float = 1.0f,
        @Deprecated("Use transform.scaleY", ReplaceWith("transform.scaleY"))
        val scaleY: Float = 1.0f,
        @Deprecated("Use transform.originXRatio", ReplaceWith("transform.originXRatio"))
        val originXRatio: Float = 0.5f,
        @Deprecated("Use transform.originYRatio", ReplaceWith("transform.originYRatio"))
        val originYRatio: Float = 0.5f,
        /** Single authoritative source of transform data. Read via [effectiveTransform]. */
        val transform: TransformDef = TransformDef(),
        val effects: EffectsDef = EffectsDef(
            opacity = opacity,
            filter = filter
        )
    ) : CanvasLayer() {
        /** LOW 3 FIX: use hasTransform (not structural equality) — same fix as BoxLayer. */
        @Suppress("DEPRECATION")
        val effectiveTransform: TransformDef
            get() = if (transform.hasTransform) transform
                    else TransformDef(
                        rotationDegrees = rotationDegrees,
                        offsetXRatio = offsetXRatio,
                        offsetYRatio = offsetYRatio,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        originXRatio = originXRatio,
                        originYRatio = originYRatio
                    )

        val effectiveEffects: EffectsDef
            get() = if (effects != EffectsDef()) effects else EffectsDef(
                opacity = opacity,
                filter = filter
            )
    }

    @Serializable
    @SerialName("GlowRing")
    data class GlowRing(
        val glowColor: Long = NxprcDefaults.DEFAULT_ACCENT_COLOR,
        val blurRadius: Float = 14f,
        val pulseEnabled: Boolean = true
    ) : CanvasLayer()

    @Serializable
    @SerialName("BezelSocket")
    data class BezelSocket(
        val outerBezelColor: Long = 0xFF1C1D24L,
        val outerBevelStroke: Long = 0xFF292A30L,
        val shadowColor: Long = NxprcDefaults.DEFAULT_SHADOW_COLOR,
        val insetRatio: Float = 0.04f
    ) : CanvasLayer()

    @Serializable
    @SerialName("InnerShadow")
    data class InnerShadow(
        val shadowColor: Long = 0x73000000L,
        val highlightColor: Long = 0x35FFFFFFL,
        val strokeWidth: Float = 3.5f
    ) : CanvasLayer()

    @Serializable
    @SerialName("GlossReflection")
    data class GlossReflection(
        val offsetXRatio: Float = 0.14f,
        val offsetYRatio: Float = 0.07f,
        val widthRatio: Float = 0.55f,
        val heightRatio: Float = 0.32f,
        val rotationDegrees: Float = -18f,
        val alpha: Float = 0.75f,
        val blurRadius: Float = 0f
    ) : CanvasLayer()

    @Serializable
    @SerialName("CenterGlyph")
    data class CenterGlyph(
        val text: String? = "A",
        val fontSizeSp: Float = 22f,
        val textColor: Long = NxprcDefaults.DEFAULT_ACCENT_COLOR,
        val iconSvgPath: String? = null,
        val iconColor: Long = NxprcDefaults.DEFAULT_ACCENT_COLOR,
        val shadowColor: Long? = null,
        val shadowOffsetY: Float = 2f,
        val highlightColor: Long? = null,
        val textShadows: List<TextShadowDef> = emptyList(),
        val offsetXRatio: Float = 0f,
        val offsetYRatio: Float = 0f
    ) : CanvasLayer()

    @Serializable
    @SerialName("TextLayer")
    data class TextLayer(
        val text: String,
        val fontSizeSp: Float = 22f,
        val fontWeight: Int = 700, // 400 = normal, 700 = bold, 900 = black
        val textColor: Long = 0xFFF5F5F5L,
        val offsetXRatio: Float = 0f,
        val offsetYRatio: Float = 0f,
        val textShadows: List<TextShadowDef> = emptyList(),
        val maxLines: Int = 1,
        val lineHeightSp: Float = 0f,
        val textAlign: String = "CENTER"
    ) : CanvasLayer()
}

@Serializable
sealed class FillBrush {
    @Serializable
    @SerialName("Solid")
    data class Solid(val color: Long) : FillBrush()

    @Serializable
    @SerialName("LinearGradient")
    data class LinearGradient(
        val colors: List<Long>,
        val angleDegrees: Float = 0f,
        val stops: List<Float> = emptyList()
    ) : FillBrush()

    @Serializable
    @SerialName("RadialGradient")
    data class RadialGradient(
        val colors: List<Long>,
        val radiusRatio: Float = 0.5f,
        val centerXRatio: Float = 0.5f,
        val centerYRatio: Float = 0.5f,
        val stops: List<Float> = emptyList(),
        val aspectRatio: Float = 1.0f
    ) : FillBrush()

    @Serializable
    @SerialName("SweepGradient")
    data class SweepGradient(
        val colors: List<Long>,
        val centerXRatio: Float = 0.5f,
        val centerYRatio: Float = 0.5f,
        val stops: List<Float> = emptyList(),
        val startAngleDegrees: Float = 0f
    ) : FillBrush()
}

@Serializable
data class StrokeStyle(
    val color: Long,
    val width: Float = 2f,
    val isDashed: Boolean = false,
    val dashWidth: Float = 0f,
    val dashGap: Float = 0f,
    val isTopOnly: Boolean = false
)

/** Supported animatable property types in the universal timeline track engine. */
@Serializable
enum class AnimatedProperty {
    SCALE,
    SCALE_X,
    SCALE_Y,
    ROTATION,
    OPACITY,
    TRANSLATE_X,
    TRANSLATE_Y,
    HUE_ROTATE
}

/** Normalized keyframe point on an animation timeline track. */
@Serializable
data class AnimationKeyframePoint(
    val fraction: Float, // 0.0f to 1.0f (representing 0% to 100%)
    val value: Float     // scalar value at this keyframe point
)

/** Universal timeline track driving a single animatable property over time. */
@Serializable
data class AnimationTrack(
    val property: AnimatedProperty,
    val keyframes: List<AnimationKeyframePoint>,
    val durationMs: Int = 2000,
    val isInfinite: Boolean = true,
    val easing: String = "LINEAR" // LINEAR, EASE, EASE_IN_OUT, FAST_OUT_SLOW_IN
)

@Serializable
data class NxprcAnimations(
    val idleType: String = "PULSE",       // PULSE, ROTATE, SHIMMER, RGB_CYCLE, CUSTOM, NONE
    val idleDurationMs: Int = 2000,
    val pressFeedback: String = "SPRING", // SPRING, SHOCKWAVE, FLASH
    val springStiffness: Float = 600f,
    val springDamping: Float = 0.65f,
    val pressScale: Float = 0.88f,
    val pressOffsetY: Float = 0f,
    val enableGameRumble: Boolean = true,
    val rumbleIntensity: Float = 1.0f,
    val joystickSpringTension: Float = 750f,
    val triggerMaxPullDepth: Float = 12f,
    val tracks: List<AnimationTrack> = emptyList()
)
