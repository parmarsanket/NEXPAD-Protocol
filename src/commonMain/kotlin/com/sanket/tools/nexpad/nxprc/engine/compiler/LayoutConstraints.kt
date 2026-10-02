package com.sanket.tools.nexpad.nxprc.engine.compiler

/**
 * Compose-inspired layout constraints for the NXPRC constraint solver.
 *
 * Inspired by Compose's `Constraints(minWidth, maxWidth, minHeight, maxHeight)` model:
 * every node in the layout tree receives a `LayoutConstraints` that bounds the
 * dimensions it may produce. The solver clamps candidate widths and heights into the
 * permitted range, enabling intrinsic sizing, `aspect-ratio`, and `calc()` expressions.
 *
 * Design contract (mirrors Compose):
 *  - [minWidth] ≤ [maxWidth] — guaranteed; callers must not invert the range.
 *  - [minHeight] ≤ [maxHeight] — same guarantee.
 *  - A tightly-constrained axis (min == max) models a "fixed" dimension from the parent.
 *  - [Float.POSITIVE_INFINITY] for max means "unconstrained" (like Compose's `Constraints.Infinity`).
 */
data class LayoutConstraints(
    val minWidth: Float = 0f,
    val maxWidth: Float = Float.POSITIVE_INFINITY,
    val minHeight: Float = 0f,
    val maxHeight: Float = Float.POSITIVE_INFINITY
) {
    // ── Clamping helpers ────────────────────────────────────────────────────

    /** Clamp [w] so it satisfies [minWidth] ≤ result ≤ [maxWidth]. */
    fun constrainWidth(w: Float): Float = w.coerceIn(minWidth, maxWidth.coerceAtLeast(minWidth))

    /** Clamp [h] so it satisfies [minHeight] ≤ result ≤ [maxHeight]. */
    fun constrainHeight(h: Float): Float = h.coerceIn(minHeight, maxHeight.coerceAtLeast(minHeight))

    // ── Factory helpers (mirrors Compose's Constraints factories) ───────────

    companion object {
        /** Unconstrained on both axes — equivalent to `Constraints()` in Compose. */
        val Unbounded: LayoutConstraints = LayoutConstraints()

        /**
         * Tightly fixed constraints — both axes are locked to the given size.
         * Equivalent to `Constraints.fixed(width, height)` in Compose.
         */
        fun fixed(width: Float, height: Float): LayoutConstraints = LayoutConstraints(
            minWidth = width, maxWidth = width,
            minHeight = height, maxHeight = height
        )

        /**
         * Maximum-bounded constraints — the node may be smaller but not larger.
         * Equivalent to `Constraints(0, maxWidth, 0, maxHeight)` in Compose.
         */
        fun atMost(maxWidth: Float, maxHeight: Float): LayoutConstraints = LayoutConstraints(
            minWidth = 0f, maxWidth = maxWidth,
            minHeight = 0f, maxHeight = maxHeight
        )

        /**
         * Build constraints from an explicit CSS dimension.
         * If either dimension is unconstrained (null / zero), the corresponding
         * axis is left unbounded.
         */
        fun fromCssDimensions(width: Float?, height: Float?): LayoutConstraints = LayoutConstraints(
            minWidth = if (width != null && width > 0f) width else 0f,
            maxWidth = if (width != null && width > 0f) width else Float.POSITIVE_INFINITY,
            minHeight = if (height != null && height > 0f) height else 0f,
            maxHeight = if (height != null && height > 0f) height else Float.POSITIVE_INFINITY
        )
    }
}

/**
 * Intrinsic (content-driven) size of a layout node.
 *
 * Used internally by the NXPRC constraint solver when a node's width or height
 * is determined by its textual content rather than an explicit CSS dimension.
 * Mirrors Compose's `IntrinsicMeasurable` concept.
 */
data class IntrinsicSize(
    val width: Float,
    val height: Float
) {
    companion object {
        val Zero: IntrinsicSize = IntrinsicSize(0f, 0f)
    }
}
