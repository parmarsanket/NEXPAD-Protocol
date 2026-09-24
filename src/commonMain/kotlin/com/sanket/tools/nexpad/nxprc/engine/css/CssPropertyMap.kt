package com.sanket.tools.nexpad.nxprc.engine.css

import com.sanket.tools.nexpad.nxprc.*
import com.sanket.tools.nexpad.nxprc.engine.model.CssLengthValue
import com.sanket.tools.nexpad.nxprc.engine.parsers.*

/**
 * Typed CSS property accessor for a computed element style.
 *
 * Wraps the raw `Map<String, String>` produced by [CssCascadeResolver] with
 * strongly-typed property accessors. Parsing happens once, on first access (lazy),
 * and is cached. Callers get back typed values — no more ad-hoc `?.toFloatOrNull()`,
 * `?.trim()?.lowercase()`, or repeated `contains("50%")` at call sites.
 *
 * Backwards compatible: [get] still allows raw key lookups for any CSS property
 * not yet promoted to a typed accessor.
 *
 * Usage:
 *   val map = CssPropertyMap(resolvedStyle)
 *   val isOval = map.borderRadius.isOval(map.width.resolveOrDefault(100f, 100f), …)
 *   if (map.display.isClipping) { … }  // instead of props["overflow"] == "hidden"
 */
class CssPropertyMap(private val raw: Map<String, String>) {

    // ── Raw fallback ──────────────────────────────────────────────────────────
    operator fun get(key: String): String? = raw[key]
    fun getOrEmpty(key: String): String = raw[key] ?: ""
    val rawMap: Map<String, String> get() = raw

    // ── Layout & box-model ───────────────────────────────────────────────────
    val display: DisplayValue     by lazy { DisplayValue.parse(raw["display"]) }
    val position: PositionValue   by lazy { PositionValue.parse(raw["position"]) }
    val overflow: OverflowValue   by lazy { OverflowValue.parse(raw["overflow"]) }
    val visibility: VisibilityValue by lazy { VisibilityValue.parse(raw["visibility"]) }

    val width: CssLengthValue     by lazy { CssLengthValue.parse(raw["width"]) }
    val height: CssLengthValue    by lazy { CssLengthValue.parse(raw["height"]) }
    val minWidth: CssLengthValue  by lazy { CssLengthValue.parse(raw["min-width"]) }
    val minHeight: CssLengthValue by lazy { CssLengthValue.parse(raw["min-height"]) }
    val maxWidth: CssLengthValue  by lazy { CssLengthValue.parse(raw["max-width"]) }
    val maxHeight: CssLengthValue by lazy { CssLengthValue.parse(raw["max-height"]) }

    val zIndex: Int by lazy { raw["z-index"]?.trim()?.toIntOrNull() ?: 0 }
    val opacity: Float by lazy { raw["opacity"]?.toFloatOrNull() ?: 1f }

    // ── Flex layout ──────────────────────────────────────────────────────────
    val flexDirection: FlexDirection   by lazy { FlexDirection.parse(raw["flex-direction"]) }
    val justifyContent: JustifyContent by lazy { JustifyContent.parse(raw["justify-content"]) }
    val alignItems: AlignItems         by lazy { AlignItems.parse(raw["align-items"]) }
    val alignSelf: AlignItems?         by lazy { AlignItems.parseSelf(raw["align-self"]) }

    // ── Border & shape ───────────────────────────────────────────────────────
    val borderRadius: CornerRadii by lazy {
        GeometryParser.parseBorderRadius(raw["border-radius"])
    }
    val border: StrokeStyle? by lazy {
        val isTopOnly = raw["border"] == null && raw["border-top"] != null
        GeometryParser.parseBorder(
            raw["border"] ?: raw["border-top"] ?: raw["border-width"],
            isTopOnly = isTopOnly
        )
    }
    val clipPath: ParsedClipShape? by lazy {
        GeometryParser.parseClipPath(
            raw["clip-path"] ?: raw["-webkit-clip-path"],
            width = raw["width"]?.let { GeometryParser.parsePixelOrPercent(it, 100f, 100f) } ?: 100f,
            height = raw["height"]?.let { GeometryParser.parsePixelOrPercent(it, 100f, 100f) } ?: 100f
        )
    }

    // ── Fills / backgrounds ──────────────────────────────────────────────────
    val fills: List<FillBrush> by lazy {
        val raw = GradientParser.parseAll(
            this.raw["background"] ?: this.raw["background-color"] ?: this.raw["fill"],
            this.raw["background-position"],
            this.raw["background-size"]
        )
        val solidBg = ColorParser.parse(this.raw["background-color"])
        if (solidBg != null && solidBg != 0x00000000L && raw.none { it is FillBrush.Solid && it.color == solidBg })
            raw + FillBrush.Solid(solidBg)
        else raw
    }

    val boxShadows: List<BoxShadowDef> by lazy {
        ShadowParser.parseBoxShadows(raw["box-shadow"])
    }

    val insetShadows: List<BoxShadowDef>  by lazy { boxShadows.filter { it.isInset } }
    val outsetShadows: List<BoxShadowDef> by lazy { boxShadows.filter { !it.isInset } }

    // ── Visibility helper ────────────────────────────────────────────────────
    val isVisible: Boolean
        get() = display != DisplayValue.NONE && visibility.isVisible

    // ── Text ─────────────────────────────────────────────────────────────────
    val textAlign: TextAlignValue by lazy { TextAlignValue.parse(raw["text-align"]) }
    val fontWeight: Int by lazy { FontWeightParser.parse(raw["font-weight"]) }
    val fontSize: CssLengthValue by lazy { CssLengthValue.parse(raw["font-size"]) }
    val color: Long? by lazy { ColorParser.parse(raw["color"]) }

    // ── Clipping shorthand ───────────────────────────────────────────────────
    /** True when this element clips its children (overflow:hidden or has clip-path). */
    val isClipping: Boolean get() = overflow.isClipping || clipPath != null

    companion object {
        /** Create a [CssPropertyMap] from any existing raw property map. */
        fun of(raw: Map<String, String>): CssPropertyMap = CssPropertyMap(raw)

        /** Empty map for elements with no resolved styles. */
        val EMPTY = CssPropertyMap(emptyMap())
    }
}
