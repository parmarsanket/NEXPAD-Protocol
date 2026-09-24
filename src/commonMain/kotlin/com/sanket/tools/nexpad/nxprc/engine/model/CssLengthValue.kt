package com.sanket.tools.nexpad.nxprc.engine.model

/**
 * Industry-standard typed representation of a CSS length/dimension value.
 *
 * Replaces raw `String` maps and ad-hoc `toFloatOrNull()` parsing scattered across
 * the engine. Every CSS dimension (width, height, padding, font-size, etc.) is
 * parsed once into a [CssLengthValue] and resolved to px at the layout stage —
 * never re-parsed from strings at the rendering stage.
 *
 * CSS units supported: px, %, em, rem, vw, vh, unitless (treated as px).
 * Special keywords: auto, unset, inherit, initial → [Auto] or [Unspecified].
 */
sealed class CssLengthValue {

    /** No value was specified (property absent or has no parseable value). */
    data object Unspecified : CssLengthValue()

    /** CSS `auto` keyword — dimension is computed by the layout engine. */
    data object Auto : CssLengthValue()

    /** Absolute pixel value. */
    data class Px(val value: Float) : CssLengthValue()

    /** Percentage of the parent reference dimension. */
    data class Percent(val value: Float) : CssLengthValue()

    /** Relative to the element's own font-size (or root if `rem`). */
    data class Em(val value: Float, val isRoot: Boolean = false) : CssLengthValue()

    /** Viewport width percentage (1vw = 1% of viewport width). */
    data class Vw(val value: Float) : CssLengthValue()

    /** Viewport height percentage. */
    data class Vh(val value: Float) : CssLengthValue()

    // ── Resolution ───────────────────────────────────────────────────────────

    /**
     * Resolves this length to an absolute pixel value.
     *
     * @param refDimension  Parent dimension (width or height) used for % resolution.
     * @param fontSizePx    Element's computed font-size, used for `em`. Defaults to
     *                      CSS initial value of 16px.
     * @param viewportW     Viewport width for `vw` units.
     * @param viewportH     Viewport height for `vh` units.
     * @return Resolved px value, or `null` if resolution is impossible (Auto/Unspecified).
     */
    fun resolve(
        refDimension: Float,
        fontSizePx: Float = 16f,
        viewportW: Float = refDimension,
        viewportH: Float = refDimension
    ): Float? = when (this) {
        is Px          -> value
        is Percent     -> (value / 100f) * refDimension
        is Em          -> value * fontSizePx
        is Vw          -> (value / 100f) * viewportW
        is Vh          -> (value / 100f) * viewportH
        Auto, Unspecified -> null
    }

    /**
     * Resolves with a fallback — returns [fallback] if the value is Auto or Unspecified.
     */
    fun resolveOrDefault(
        refDimension: Float,
        fallback: Float,
        fontSizePx: Float = 16f
    ): Float = resolve(refDimension, fontSizePx) ?: fallback

    /** True if this value can be resolved to a concrete pixel value. */
    val isSpecified: Boolean get() = this !is Auto && this !is Unspecified

    // ── Factory ──────────────────────────────────────────────────────────────

    companion object {
        private val PX_RE = Regex("""^([+-]?[\d.]+)\s*px$""", RegexOption.IGNORE_CASE)
        private val PCT_RE = Regex("""^([+-]?[\d.]+)\s*%$""")
        private val EM_RE = Regex("""^([+-]?[\d.]+)\s*(r?em)$""", RegexOption.IGNORE_CASE)
        private val VW_RE = Regex("""^([+-]?[\d.]+)\s*vw$""", RegexOption.IGNORE_CASE)
        private val VH_RE = Regex("""^([+-]?[\d.]+)\s*vh$""", RegexOption.IGNORE_CASE)
        private val NUM_RE = Regex("""^([+-]?[\d.]+)$""")

        /**
         * Parses a raw CSS string into a typed [CssLengthValue].
         * Returns [Unspecified] for null/blank input; [Auto] for the "auto" keyword.
         */
        fun parse(raw: String?): CssLengthValue {
            if (raw.isNullOrBlank()) return Unspecified
            val s = raw.trim().lowercase()
            if (s == "auto" || s == "unset" || s == "inherit" || s == "initial" || s == "none") return Auto
            PX_RE.find(s)?.let { return Px(it.groupValues[1].toFloat()) }
            PCT_RE.find(s)?.let { return Percent(it.groupValues[1].toFloat()) }
            EM_RE.find(s)?.let { m -> return Em(m.groupValues[1].toFloat(), isRoot = m.groupValues[2].startsWith('r')) }
            VW_RE.find(s)?.let { return Vw(it.groupValues[1].toFloat()) }
            VH_RE.find(s)?.let { return Vh(it.groupValues[1].toFloat()) }
            // Unitless number → treat as px (CSS 2.1 §4.3.2 for line-height etc.)
            NUM_RE.find(s)?.let { return Px(it.groupValues[1].toFloat()) }
            return Unspecified
        }

        /** Shorthand factories */
        fun px(value: Float): CssLengthValue = Px(value)
        fun percent(value: Float): CssLengthValue = Percent(value)
    }
}
