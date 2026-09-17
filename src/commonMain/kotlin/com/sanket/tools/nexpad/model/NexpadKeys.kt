package com.sanket.tools.nexpad.model

import com.sanket.tools.nexpad.category.ControlKey

/**
 * Canonical control key constants for the entire NEXPAD app layer.
 *
 * Every value here is `ControlKey.<X>.key`, which is that enum constant's own name —
 * there is no separate string to maintain and nothing that can fall out of sync with
 * [com.sanket.tools.nexpad.category.CategoryManager], because it's literally the same enum.
 * The old runtime lookup-and-throw is gone: if a key doesn't exist, the code simply doesn't
 * compile, so errors are caught at build time instead of surfacing as runtime crashes.
 *
 * Use these constants where a plain `String` is required (persistence, HID mapping, layout
 * serialization, etc). For anything else — rendering, dimensions, color, icon — reference
 * [ControlKey] directly (e.g. `ControlKey.A.accentColorArgb`) instead of round-tripping
 * through a string.
 */
object NexpadKeys {

    // ── ABXY ─────────────────────────────────────────────────────────────
    val A: String = ControlKey.A.key
    val B: String = ControlKey.B.key
    val X: String = ControlKey.X.key
    val Y: String = ControlKey.Y.key

    // ── D-Pad (individual + composite) ──────────────────────────────────
    val UP: String = ControlKey.UP.key
    val DOWN: String = ControlKey.DOWN.key
    val LEFT: String = ControlKey.LEFT.key
    val RIGHT: String = ControlKey.RIGHT.key
    /** Composite 4-way D-Pad cluster (single renderable unit). */
    val DPAD: String = ControlKey.DPAD.key

    // ── Triggers ─────────────────────────────────────────────────────────
    val LT: String = ControlKey.LT.key
    val RT: String = ControlKey.RT.key

    // ── Bumpers ──────────────────────────────────────────────────────────
    val LB: String = ControlKey.LB.key
    val RB: String = ControlKey.RB.key

    // ── Sticks ───────────────────────────────────────────────────────────
    val LS: String = ControlKey.LS.key
    val RS: String = ControlKey.RS.key

    // ── System ───────────────────────────────────────────────────────────
    /** Xbox / Home / Guide center button. */
    val GUIDE: String = ControlKey.GUIDE.key
    /** Menu / Start / Options button. */
    val START: String = ControlKey.START.key
    /** View / Back / Select / Map button. */
    val BACK: String = ControlKey.BACK.key
    val SHARE: String = ControlKey.SHARE.key
    val TURBO: String = ControlKey.TURBO.key
    val PROFILE: String = ControlKey.PROFILE.key

    // ── Macros ───────────────────────────────────────────────────────────
    val M1: String = ControlKey.M1.key
    val M2: String = ControlKey.M2.key
    val M3: String = ControlKey.M3.key
    val M4: String = ControlKey.M4.key

    // ── Hardware Synonyms & Aliases (All bound to single source of truth) ──
    val XBOX: String = ControlKey.GUIDE.key
    val HOME: String = ControlKey.GUIDE.key
    val MENU: String = ControlKey.START.key
    val VIEW: String = ControlKey.BACK.key
    val SELECT: String = ControlKey.BACK.key
    val L1: String = ControlKey.LB.key
    val R1: String = ControlKey.RB.key
    val L2: String = ControlKey.LT.key
    val R2: String = ControlKey.RT.key
    val L3: String = ControlKey.LS.key
    val R3: String = ControlKey.RS.key
    val SCREENSHOT: String = ControlKey.SHARE.key
}
