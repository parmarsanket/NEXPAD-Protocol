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

    // ── Content stacking context zone (base = 5_000) ─────────────────────────
    /** Base slot for in-flow content (::before, DOM children, ::after). */
    const val CONTENT_BASE: Int   = 5_000
    /** Alias for CONTENT_BASE to maintain backwards compatibility. */
    const val CHILDREN_BASE: Int  = 5_000
    /** Per-unit CSS z-index step. */
    const val Z_STEP: Int         =   100

    const val BEFORE_OFFSET: Int  =    10
    const val CHILD_OFFSET: Int   =    20
    const val AFTER_OFFSET: Int   =    80
    const val TEXT_OFFSET: Int    =    90

    // ── Text / glyph zone ────────────────────────────────────────────────────

    /** Default text/CenterGlyph slot (no explicit CSS z-index; renders on top of un-indexed DOM children). */
    const val TEXT_DEFAULT: Int   = 7_000

    // ── Joystick thumb-cap offset ────────────────────────────────────────────

    /**
     * Added to any slot to move a layer into the thumb-cap zone.
     * Ensures all cap layers render above all base/housing layers
     * regardless of their z-index within their own zone.
     */
    const val THUMB_CAP_OFFSET: Int = 20_000

    // ── Slot computation helpers ─────────────────────────────────────────────

    /**
     * Computes the stack slot for a `::before` pseudo-element.
     */
    fun beforeSlot(zIndex: Int): Int =
        CONTENT_BASE + zIndex * Z_STEP + BEFORE_OFFSET

    /**
     * Computes the stack slot for a `::after` pseudo-element.
     */
    fun afterSlot(zIndex: Int): Int =
        CONTENT_BASE + zIndex * Z_STEP + AFTER_OFFSET

    /**
     * Computes the stack slot for a DOM child layer with inherited parent stack.
     * When [zIndex] != 0, sorts strictly by z-index.
     * When [zIndex] == 0, stacks above [parentStack] by depth (+2).
     */
    fun childSlot(parentStack: Int, zIndex: Int): Int =
        if (zIndex != 0) CONTENT_BASE + zIndex * Z_STEP + CHILD_OFFSET
        else parentStack + 2

    /**
     * Computes the stack slot for a DOM child layer given only [zIndex].
     */
    fun childSlot(zIndex: Int): Int =
        CONTENT_BASE + zIndex * Z_STEP + CHILD_OFFSET

    /**
     * Computes the stack slot for a text/glyph layer.
     * Returns [TEXT_DEFAULT] (7_000) when `zIndex == 0` so text renders above un-indexed children.
     */
    fun textSlot(zIndex: Int): Int =
        if (zIndex == 0) TEXT_DEFAULT else CONTENT_BASE + zIndex * Z_STEP + TEXT_OFFSET

    /**
     * Returns the thumb-cap adjusted slot — adds [THUMB_CAP_OFFSET] so all
     * cap layers sort above all base layers unconditionally.
     */
    fun thumbCapSlot(baseSlot: Int): Int = baseSlot + THUMB_CAP_OFFSET
}
