package com.sanket.tools.nexpad.nxprc.engine.stack

/**
 * Canonical layer stack slot assignments for NXPRC canvas composition.
 *
 * Replaces all raw integer literals (5, 8, 10, 15, 60, 200) that were scattered
 * across [NxprcCompiler] and [DomTreeCompiler]. Every call to [LayerCollector.addLayer]
 * MUST use a slot from this object — never a bare integer.
 *
 * Slot architecture (1 000-unit zones):
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │  Zone         │ Slot range  │ Description                           │
 * ├───────────────┼─────────────┼───────────────────────────────────────┤
 * │ GLOW          │  1 000      │ Atmospheric glow ring (behind all)    │
 * │ BEZEL         │  2 000      │ Physical socket / bezel housing       │
 * │ SURFACE       │  3 000      │ Main button surface fill              │
 * │ INNER_SHADOW  │  3 100      │ Inset shadow / groove ring            │
 * │ BEFORE        │  4 000+     │ ::before pseudo-elements              │
 * │ CHILDREN      │  5 000+     │ DOM child layers (recursive)          │
 * │ AFTER         │  6 000+     │ ::after pseudo-elements               │
 * │ TEXT          │  7 000      │ Text / CenterGlyph (default)          │
 * │ THUMB_CAP     │ 10 000+     │ Joystick moveable cap elements        │
 * └──────────────────────────────────────────────────────────────────────┘
 *
 * The 1 000-unit spacing between zones means a child CSS `z-index` of 0..9
 * (the practical real-world range) never overflows into an adjacent zone.
 * Use [childSlot], [beforeSlot], [afterSlot], [textSlot] to compute exact slots.
 */
object LayerStack {

    // ── Zone base slots ──────────────────────────────────────────────────────

    /** Atmospheric glow ring — rendered below the bezel. */
    const val GLOW_RING: Int      = 1_000

    /** Physical bezel / socket housing layer. */
    const val BEZEL_SOCKET: Int   = 2_000

    /** Main surface fill (primary button shape). */
    const val SURFACE: Int        = 3_000

    /** Inset shadow / perimeter groove ring. */
    const val INNER_SHADOW: Int   = 3_100

    // ── Pseudo-element zones ─────────────────────────────────────────────────

    /** Base slot for `::before` pseudo-elements. */
    const val BEFORE_BASE: Int    = 4_000
    /** Per-unit z-index increment within the `::before` zone. */
    const val BEFORE_STEP: Int    =   100

    // ── DOM children zone ────────────────────────────────────────────────────

    /** Base slot for recursively compiled DOM child layers. */
    const val CHILDREN_BASE: Int  = 5_000
    /** Per-unit z-index increment within the DOM children zone. */
    const val CHILDREN_STEP: Int  =   100

    // ── After pseudo-element zone ────────────────────────────────────────────

    /** Base slot for `::after` pseudo-elements. */
    const val AFTER_BASE: Int     = 6_000
    /** Per-unit z-index increment within the `::after` zone. */
    const val AFTER_STEP: Int     =   100

    // ── Text / glyph zone ────────────────────────────────────────────────────

    /** Default text/CenterGlyph slot (no explicit CSS z-index). */
    const val TEXT_DEFAULT: Int   = 7_000
    /** Base slot when an explicit CSS `z-index` is set on the text node. */
    const val TEXT_BASE: Int      = 7_000
    /** Per-unit z-index increment within the text zone. */
    const val TEXT_STEP: Int      =   100

    // ── Joystick thumb-cap offset ────────────────────────────────────────────

    /**
     * Added to any slot to move a layer into the thumb-cap zone.
     * Ensures all cap layers render above all base/housing layers
     * regardless of their z-index within their own zone.
     */
    const val THUMB_CAP_OFFSET: Int = 10_000

    // ── Slot computation helpers ─────────────────────────────────────────────

    /**
     * Computes the exact stack slot for a DOM child layer with a given CSS `z-index`.
     * A z-index of 0 maps to [CHILDREN_BASE]; each additional unit adds [CHILDREN_STEP].
     */
    fun childSlot(zIndex: Int): Int = CHILDREN_BASE + zIndex * CHILDREN_STEP

    /**
     * Computes the stack slot for a `::before` layer.
     * Returns [BEFORE_BASE] when z-index is 0.
     */
    fun beforeSlot(zIndex: Int): Int = BEFORE_BASE + zIndex * BEFORE_STEP

    /**
     * Computes the stack slot for a `::after` layer.
     * A small `+1` offset within the after-step distinguishes it from ::before
     * at the same z-index level.
     */
    fun afterSlot(zIndex: Int): Int = AFTER_BASE + zIndex * AFTER_STEP + 1

    /**
     * Computes the stack slot for a text/glyph layer.
     * Returns [TEXT_DEFAULT] when `zIndex == 0` (most text has no explicit z-index).
     */
    fun textSlot(zIndex: Int): Int =
        if (zIndex == 0) TEXT_DEFAULT else TEXT_BASE + zIndex * TEXT_STEP + 2

    /**
     * Returns the thumb-cap adjusted slot — adds [THUMB_CAP_OFFSET] so all
     * cap layers sort above all base layers unconditionally.
     */
    fun thumbCapSlot(baseSlot: Int): Int = baseSlot + THUMB_CAP_OFFSET
}
