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
    val version: Int = 1,
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
    val description: String = ""
)

@Serializable
data class NxprcCanvas(
    val viewBoxWidth: Float = 100f,
    val viewBoxHeight: Float = 100f,
    val layers: List<CanvasLayer> = emptyList(),
    val clipToBounds: Boolean = false
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
    val saturation: Float = 1f
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
    val filter: FilterDef = FilterDef()
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
        val filter: FilterDef = FilterDef(),
        val opacity: Float = 1.0f,
        val rotationDegrees: Float = 0f,
        val offsetXRatio: Float = 0f,
        val offsetYRatio: Float = 0f,
        val scaleX: Float = 1.0f,
        val scaleY: Float = 1.0f,
        val skewX: Float = 0f,
        val skewY: Float = 0f,
        val originXRatio: Float = 0.5f,
        val originYRatio: Float = 0.5f,
        val isRotating: Boolean = false,
        val transform: TransformDef = TransformDef(
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
        ),
        val effects: EffectsDef = EffectsDef(
            opacity = opacity,
            filter = filter
        )
    ) : CanvasLayer() {
        val effectiveTransform: TransformDef
            get() = if (transform.hasTransform || transform != TransformDef()) transform else TransformDef(
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
        val rotationDegrees: Float = 0f,
        val offsetXRatio: Float = 0f,
        val offsetYRatio: Float = 0f,
        val widthRatio: Float = 1.0f,
        val heightRatio: Float = 1.0f,
        val scaleX: Float = 1.0f,
        val scaleY: Float = 1.0f,
        val originXRatio: Float = 0.5f,
        val originYRatio: Float = 0.5f,
        val transform: TransformDef = TransformDef(
            rotationDegrees = rotationDegrees,
            offsetXRatio = offsetXRatio,
            offsetYRatio = offsetYRatio,
            scaleX = scaleX,
            scaleY = scaleY,
            originXRatio = originXRatio,
            originYRatio = originYRatio
        ),
        val effects: EffectsDef = EffectsDef(
            opacity = opacity,
            filter = filter
        )
    ) : CanvasLayer() {
        val effectiveTransform: TransformDef
            get() = if (transform.hasTransform || transform != TransformDef()) transform else TransformDef(
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
        val textShadows: List<TextShadowDef> = emptyList()
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
        val textShadows: List<TextShadowDef> = emptyList()
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
        val stops: List<Float> = emptyList()
    ) : FillBrush()

    @Serializable
    @SerialName("SweepGradient")
    data class SweepGradient(
        val colors: List<Long>,
        val centerXRatio: Float = 0.5f,
        val centerYRatio: Float = 0.5f,
        val stops: List<Float> = emptyList()
    ) : FillBrush()
}

@Serializable
data class StrokeStyle(
    val color: Long,
    val width: Float = 2f
)

@Serializable
data class NxprcAnimations(
    val idleType: String = "PULSE",       // PULSE, ROTATE, SHIMMER, NONE
    val idleDurationMs: Int = 2000,
    val pressFeedback: String = "SPRING", // SPRING, SHOCKWAVE, FLASH
    val springStiffness: Float = 600f,
    val springDamping: Float = 0.65f,
    val pressScale: Float = 0.88f,
    val pressOffsetY: Float = 0f
)
