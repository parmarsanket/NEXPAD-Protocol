package com.sanket.tools.nexpad.category

/**
 * Shared behavior for anything that has a visual [CategorySymbol].
 *
 * Kotlin enums can't extend a base class, but they can implement an interface — this is
 * the idiomatic way to share fields/behavior across [CategoryType] and [ControlKey] without
 * duplicating `iconName`/`svgPath` getters in both places.
 */
interface IconBearing {
    val symbol: CategorySymbol
    val iconName: String get() = symbol.iconName
    val svgPath: String get() = symbol.svgPath
}

/**
 * Structural role a control plays inside the input layer. Kept separate from [CategoryType]
 * so one category can host mixed component types (e.g. SYSTEM holds plain buttons *and* the
 * Guide orb) without special-casing anywhere.
 */
enum class ComponentType {
    BUTTON,
    DPAD,
    TRIGGER,
    BUMPER,
    JOYSTICK,
    SYSTEM
}

/**
 * Cross-platform icon semantic symbols. Encapsulates icon names, default emojis, and standard
 * SVG path data for native rendering.
 */
enum class CategorySymbol(
    val iconName: String,
    val defaultEmoji: String,
    val svgPath: String
) {
    GAMEPAD("SportsEsports", "🎮", "M21.58 16.09l-1.09-7.66C20.21 6.46 18.52 5 16.53 5H7.47C5.48 5 3.79 6.46 3.51 8.43l-1.09 7.66C2.2 17.63 3.39 19 4.94 19c.68 0 1.32-.27 1.8-.75L9 16h6l2.25 2.25c.48.48 1.13.75 1.81.75 1.55 0 2.74-1.37 2.52-2.91zM11 11H9v2H8v-2H6v-1h2V8h1v2h2v1zm4-1.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm2 3c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5z"),
    DPAD("ControlCamera", "🧭", "M12 2L6.5 7.5h3.5v3H7V7L1.5 12.5 7 18v-3h3v3.5H6.5L12 24l5.5-5.5h-3.5v-3.5h3V18l5.5-5.5L17 7v3.5h-3v-3h3.5z"),
    TRIGGER("Tune", "🎯", "M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z"),
    BUMPER("HorizontalRule", "🛡️", "M4 11h16v2H4z"),
    STICK("Album", "🕹️", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 14.5c-2.49 0-4.5-2.01-4.5-4.5S9.51 7.5 12 7.5s4.5 2.01 4.5 4.5-2.01 4.5-4.5 4.5zm0-5.5c-.55 0-1 .45-1 1s.45 1 1 1 1-.45 1-1-.45-1-1-1z"),
    HOME("Home", "⨂", "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"),
    SYSTEM("Settings", "⚙️", "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"),
    MACRO("Bolt", "⚡", "M11 21h-1l1-7H7.5c-.58 0-.57-.32-.38-.66.19-.34.05-.08.07-.12C8.48 10.94 10.42 7.54 13 3h1l-1 7h3.5c.49 0 .56.33.47.51l-.07.15C12.96 17.55 11 21 11 21z"),
    ALL("Widgets", "🎛️", "M13 13v8h8v-8h-8zM3 21h8v-8H3v8zM3 3v8h8V3H3zm13.66-1.31L11 7.34 16.66 13l5.66-5.66-5.66-5.65z")
}

/**
 * Top-level controller category. Carries its own display + visual metadata directly, so adding
 * a brand-new category is a single enum entry.
 */
enum class CategoryType(
    val displayTitle: String,
    val emoji: String,
    override val symbol: CategorySymbol,
    val description: String,
    val isGroupCluster: Boolean = false,
    /** Category-level aliases (e.g. SYSTEM answers to "XBOX"/"HOME" even before picking a control). */
    val aliasKeys: Set<String> = emptySet()
) : IconBearing {
    ABXY(
        "ABXY", "🎮", CategorySymbol.GAMEPAD,
        "Action button cluster for primary combat and interaction.",
        aliasKeys = setOf("BUTTON", "BUTTONS", "ACTION", "FACE", "FACE_BUTTONS")
    ),
    DPAD(
        "D-Pad", "🧭", CategorySymbol.DPAD,
        "Directional navigation cardinal buttons.",
        aliasKeys = setOf("CROSS", "DIRECTIONAL", "DIRECTIONAL_PAD", "D_PAD", "DPAD")
    ),
    TRIGGERS(
        "Triggers", "🎯", CategorySymbol.TRIGGER,
        "Analog linear pressure triggers with progressive deflection.",
        aliasKeys = setOf("TRIGGER", "ANALOG_TRIGGER", "TRIGGERS", "ANALOG")
    ),
    BUMPERS(
        "Bumpers", "🛡️", CategorySymbol.BUMPER,
        "Curved digital shoulder bumpers with micro-switch click.",
        aliasKeys = setOf("BUMPER", "BUMPERS", "SHOULDER", "SHOULDERS")
    ),
    STICKS(
        "Sticks", "🕹️", CategorySymbol.STICK,
        "Dual 360-degree analog sticks with concave thumb grip.",
        aliasKeys = setOf("STICK", "STICKS", "JOYSTICK", "JOYSTICKS", "THUMBSTICK", "THUMBSTICKS")
    ),
    SYSTEM(
        "System", "⚙️", CategorySymbol.SYSTEM,
        "System, navigation, utility, and special function buttons.",
        aliasKeys = setOf("SYSTEM", "VIEW", "MENU", "SELECT", "HOME", "XBOX", "GUIDE", "START", "BACK", "OPTIONS", "SHARE", "CAPTURE", "SCREENSHOT", "PS")
    ),
    MACROS(
        "Macros", "⚡", CategorySymbol.MACRO,
        "Rear programmable paddles and custom macro actuators.",
        aliasKeys = setOf("MACRO", "MACROS", "PADDLE", "PADDLES", "REAR", "REAR_BUTTONS")
    );

    val id: String get() = name
    val title: String get() = displayTitle

    companion object {
        private val LOOKUP: Map<String, CategoryType> = buildMap {
            CategoryType.entries.forEach { cat ->
                put(cat.name, cat)
                put(cat.displayTitle.uppercase(), cat)
                cat.aliasKeys.forEach { alias -> put(alias.uppercase(), cat) }
            }
        }

        /**
         * Resolves any identifier — enum name, display title, alias, or even a control key
         * (e.g. "A" -> ABXY, "LT" -> TRIGGERS, "RB" -> BUMPERS) — to its [CategoryType].
         */
        fun fromIdentifier(identifier: String?): CategoryType? {
            if (identifier.isNullOrBlank()) return null
            val trimmed = identifier.trim().uppercase()

            LOOKUP[trimmed]?.let { return it }

            // Check if it's a ControlKey (e.g. "LT", "RB", "A", "DPAD", "L2")
            ControlKey.fromIdentifier(trimmed)?.let { return it.categoryType }

            val stripped = trimmed
                .removePrefix("CATEGORY_")
                .removePrefix("CAT_")
                .removeSuffix("S")
            LOOKUP[stripped]?.let { return it }

            return null
        }
    }
}

/**
 * Canonical, type-safe identity for every controller input NEXPAD knows about.
 *
 * This enum IS the single source of truth. The constant's own name (`A`, `LT`, `DPAD`, ...)
 * IS the canonical key — it is never re-typed as a separate string literal anywhere, so it
 * can never drift out of sync. Every hardware/industry synonym ("L1", "L2", "THUMBSTICK_L", ...)
 * is declared right on the constant.
 */
enum class ControlKey(
    val categoryType: CategoryType,
    val componentType: ComponentType,
    val label: String,
    val defaultName: String,
    val defaultId: String,
    val defaultWidthDp: Int,
    val defaultHeightDp: Int,
    val accentColorArgb: Long,
    val description: String,
    val emoji: String,
    override val symbol: CategorySymbol,
    val promptHint: String = "",
    val aliases: Set<String> = emptySet(),
    val starterHtmlPreset: String? = null
) : IconBearing {

    // ── ABXY ─────────────────────────────────────────────────────────────
    A(
        CategoryType.ABXY, ComponentType.BUTTON, "A Button", "Action A Button", "rc.action_a",
        96, 96, 0xFF4ADE80L, "Primary confirmation / jump button.", "🅰️", CategorySymbol.GAMEPAD,
        promptHint = "Free silhouette, distinct tactile depth, glossy central core with high-contrast glyph.",
        aliases = setOf("BUTTON_A")
    ),
    B(
        CategoryType.ABXY, ComponentType.BUTTON, "B Button", "Action B Button", "rc.action_b",
        96, 96, 0xFFF87171L, "Secondary cancellation / evade button.", "🅱️", CategorySymbol.GAMEPAD,
        promptHint = "Free silhouette, aggressive beveling, intense crimson accenting.",
        aliases = setOf("BUTTON_B")
    ),
    X(
        CategoryType.ABXY, ComponentType.BUTTON, "X Button", "Action X Button", "rc.action_x",
        96, 96, 0xFF60A5FAL, "Tertiary light attack / reload button.", "✖️", CategorySymbol.GAMEPAD,
        promptHint = "Free silhouette, energetic cyan/blue glow, crisp industrial styling.",
        aliases = setOf("BUTTON_X")
    ),
    Y(
        CategoryType.ABXY, ComponentType.BUTTON, "Y Button", "Action Y Button", "rc.action_y",
        96, 96, 0xFFFBBF24L, "Quaternary heavy attack / special button.", "🆈", CategorySymbol.GAMEPAD,
        promptHint = "Free silhouette, luminous amber highlights, polished top chamfer.",
        aliases = setOf("BUTTON_Y")
    ),

    // ── D-Pad ────────────────────────────────────────────────────────────
    UP(
        CategoryType.DPAD, ComponentType.DPAD, "D-Pad Up", "Directional Up", "rc.dpad_up",
        80, 80, 0xFF22D3EEL, "Individual upward directional button.", "⬆️", CategorySymbol.DPAD,
        promptHint = "North-facing wedge or directional arrowhead, upward gradient highlight."
    ),
    DOWN(
        CategoryType.DPAD, ComponentType.DPAD, "D-Pad Down", "Directional Down", "rc.dpad_down",
        80, 80, 0xFF22D3EEL, "Individual downward directional button.", "⬇️", CategorySymbol.DPAD,
        promptHint = "South-facing wedge or directional arrowhead, bottom shadow drop."
    ),
    LEFT(
        CategoryType.DPAD, ComponentType.DPAD, "D-Pad Left", "Directional Left", "rc.dpad_left",
        80, 80, 0xFF22D3EEL, "Individual leftward directional button.", "⬅️", CategorySymbol.DPAD,
        promptHint = "West-facing wedge or directional arrowhead, left rim highlight."
    ),
    RIGHT(
        CategoryType.DPAD, ComponentType.DPAD, "D-Pad Right", "Directional Right", "rc.dpad_right",
        80, 80, 0xFF22D3EEL, "Individual rightward directional button.", "➡️", CategorySymbol.DPAD,
        promptHint = "East-facing wedge or directional arrowhead, right rim highlight."
    ),
    DPAD(
        CategoryType.DPAD, ComponentType.DPAD, "4-Way D-Pad", "Directional Pad", "rc.dpad",
        140, 140, 0xFF22D3EEL, "Integrated 4-way directional cross pad.", "🧭", CategorySymbol.DPAD,
        promptHint = "Integrated 4-way cross directional pad with center pivot and cardinal direction wings.",
        aliases = setOf("CROSS", "DIRECTIONAL_PAD")
    ),

    // ── Triggers ─────────────────────────────────────────────────────────
    LT(
        CategoryType.TRIGGERS, ComponentType.TRIGGER, "Left Trigger", "Analog Left Trigger", "rc.trigger_lt",
        110, 140, 0xFFA855F7L, "Left progressive analog trigger (aim / brake).", "🎯", CategorySymbol.TRIGGER,
        promptHint = "Elongated trigger paddle, progressive pressure glow indicator, mechanical ribbing.",
        aliases = setOf("L2", "LEFT_TRIGGER", "TRIGGER_L")
    ),
    RT(
        CategoryType.TRIGGERS, ComponentType.TRIGGER, "Right Trigger", "Analog Right Trigger", "rc.trigger_rt",
        110, 140, 0xFFA855F7L, "Right progressive analog trigger (fire / accelerate).", "🎯", CategorySymbol.TRIGGER,
        promptHint = "Elongated trigger paddle, progressive pressure glow indicator, tactile rear notch.",
        aliases = setOf("R2", "RIGHT_TRIGGER", "TRIGGER_R")
    ),

    // ── Bumpers ──────────────────────────────────────────────────────────
    LB(
        CategoryType.BUMPERS, ComponentType.BUMPER, "Left Bumper", "Shoulder Left Bumper", "rc.bumper_lb",
        120, 60, 0xFF38BDF8L, "Left digital shoulder bumper.", "🛡️", CategorySymbol.BUMPER,
        promptHint = "Horizontal ergonomic pill/capsule curve, top edge metallic reflection, tactile microswitch response.",
        aliases = setOf("L1", "LEFT_BUMPER", "BUMPER_L", "SHOULDER_LB")
    ),
    RB(
        CategoryType.BUMPERS, ComponentType.BUMPER, "Right Bumper", "Shoulder Right Bumper", "rc.bumper_rb",
        120, 60, 0xFF38BDF8L, "Right digital shoulder bumper.", "🛡️", CategorySymbol.BUMPER,
        promptHint = "Horizontal ergonomic pill/capsule curve, top edge metallic reflection, tactile microswitch response.",
        aliases = setOf("R1", "RIGHT_BUMPER", "BUMPER_R", "SHOULDER_RB")
    ),

    // ── Sticks ───────────────────────────────────────────────────────────
    LS(
        CategoryType.STICKS, ComponentType.JOYSTICK, "Left Stick", "Left Analog Stick", "rc.stick_ls",
        130, 130, 0xFF34D399L, "Left 360° analog stick (movement).", "🕹️", CategorySymbol.STICK,
        promptHint = "Stationary spherical/radial gimbal base with inner socket shadow + floating 360-degree deflection thumb cap with concave grip and knurled ring.",
        aliases = setOf("L3", "THUMBSTICK_L", "STICK_L", "LEFT_STICK")
    ),
    RS(
        CategoryType.STICKS, ComponentType.JOYSTICK, "Right Stick", "Right Analog Stick", "rc.stick_rs",
        130, 130, 0xFF34D399L, "Right 360° analog stick (camera/aim).", "🕹️", CategorySymbol.STICK,
        promptHint = "Stationary spherical/radial gimbal base with inner socket shadow + floating 360-degree deflection thumb cap with concave grip and knurled ring.",
        aliases = setOf("R3", "THUMBSTICK_R", "STICK_R", "RIGHT_STICK")
    ),

    // ── System ───────────────────────────────────────────────────────────
    GUIDE(
        CategoryType.SYSTEM, ComponentType.SYSTEM, "Nexus Guide", "Controller Center Guide", "rc.sys_guide",
        84, 84, 0xFFF59E0BL, "Home / Xbox / PlayStation central guide button.", "⨂", CategorySymbol.HOME,
        promptHint = "Large luminous orb or badge, glowing center insignia, prestigious bevel.",
        aliases = setOf("XBOX", "HOME", "PS", "NEXUS")
    ),
    START(
        CategoryType.SYSTEM, ComponentType.SYSTEM, "Menu / Start", "System Menu Button", "rc.sys_start",
        70, 70, 0xFF94A3B8L, "Pause / Options / Start button.", "☰", CategorySymbol.SYSTEM,
        promptHint = "Compact pill or small disc with hamburger lines or forward glyph.",
        aliases = setOf("MENU", "OPTIONS", "PAUSE")
    ),
    BACK(
        CategoryType.SYSTEM, ComponentType.SYSTEM, "View / Back", "System View Button", "rc.sys_back",
        70, 70, 0xFF94A3B8L, "Map / Back / Select button.", "⧉", CategorySymbol.SYSTEM,
        promptHint = "Compact pill or small disc with overlapping squares or rewind glyph.",
        aliases = setOf("VIEW", "SELECT", "MAP")
    ),
    SHARE(
        CategoryType.SYSTEM, ComponentType.SYSTEM, "Share / Capture", "System Share Button", "rc.sys_share",
        70, 70, 0xFF94A3B8L, "Capture screenshot or video clip.", "⇪", CategorySymbol.SYSTEM,
        promptHint = "Minimalist utility button with broadcast or share glyph.",
        aliases = setOf("CAPTURE", "SCREENSHOT")
    ),

    // ── Macros ───────────────────────────────────────────────────────────
    M1(
        CategoryType.MACROS, ComponentType.BUTTON, "Paddle M1", "Paddle M1 Switch", "rc.macro_m1",
        72, 72, 0xFFF59E0BL, "Rear upper-left programmable paddle.", "⚡", CategorySymbol.MACRO,
        promptHint = "Ergonomic angled wing or rear paddle shape with high-tactile snap."
    ),
    M2(
        CategoryType.MACROS, ComponentType.BUTTON, "Paddle M2", "Paddle M2 Switch", "rc.macro_m2",
        72, 72, 0xFFF59E0BL, "Rear upper-right programmable paddle.", "⚡", CategorySymbol.MACRO,
        promptHint = "Ergonomic angled wing or rear paddle shape with high-tactile snap."
    ),
    M3(
        CategoryType.MACROS, ComponentType.BUTTON, "Paddle M3", "Paddle M3 Switch", "rc.macro_m3",
        72, 72, 0xFFF59E0BL, "Rear lower-left programmable paddle.", "⚡", CategorySymbol.MACRO,
        promptHint = "Ergonomic angled wing or rear paddle shape with high-tactile snap."
    ),
    M4(
        CategoryType.MACROS, ComponentType.BUTTON, "Paddle M4", "Paddle M4 Switch", "rc.macro_m4",
        72, 72, 0xFFF59E0BL, "Rear lower-right programmable paddle.", "⚡", CategorySymbol.MACRO,
        promptHint = "Ergonomic angled wing or rear paddle shape with high-tactile snap."
    );

    /** Canonical string key — literally the enum constant's own name. Never re-typed anywhere. */
    val key: String get() = name

    /** True if this control is the composite 4-Way D-Pad cross pad. */
    val isDpadComposite: Boolean get() = this == DPAD

    /** True if this control is one of the 4 discrete directional D-Pad buttons (UP, DOWN, LEFT, RIGHT). */
    val isDpadDiscrete: Boolean get() = this in DISCRETE_DPAD_KEYS

    companion object {
        /** All discrete directional D-Pad buttons (mutually exclusive with composite [DPAD]). */
        val DISCRETE_DPAD_KEYS: Set<ControlKey> = setOf(UP, DOWN, LEFT, RIGHT)

        /** Every alias (including each constant's own canonical name) -> its [ControlKey]. Built once. */
        private val LOOKUP: Map<String, ControlKey> = buildMap {
            ControlKey.entries.forEach { ctrl ->
                put(ctrl.name, ctrl)
                ctrl.aliases.forEach { alias -> put(alias.uppercase(), ctrl) }
            }
        }

        /** All controls belonging to a [CategoryType], in declaration order. */
        fun of(category: CategoryType): List<ControlKey> = ControlKey.entries.filter { it.categoryType == category }

        /**
         * Resolves any arbitrary identifier — canonical name, alias, `defaultId`, builtin id,
         * label, or a prefixed/suffixed variant like `"BUTTON_A"` or `"builtin.cyber_bumper_lb"`
         * — to its [ControlKey]. Returns null if nothing matches.
         */
        fun fromIdentifier(identifier: String?): ControlKey? {
            if (identifier.isNullOrBlank()) return null
            val trimmed = identifier.trim().uppercase()

            LOOKUP[trimmed]?.let { return it }

            ControlKey.entries.forEach { ctrl ->
                if (ctrl.defaultId.equals(trimmed, ignoreCase = true)) return ctrl
                if (ctrl.label.equals(trimmed, ignoreCase = true)) return ctrl
                if (ctrl.defaultName.equals(trimmed, ignoreCase = true)) return ctrl
                if ("BUILTIN.DEFAULT_${ctrl.name}" == trimmed) return ctrl
            }

            val stripped = trimmed
                .removePrefix("BUTTON_").removePrefix("BTN_")
                .removePrefix("DEFAULT_")
                .removePrefix("RC.")
                .removePrefix("BUILTIN.")
            LOOKUP[stripped]?.let { return it }

            if (trimmed.contains('_') || trimmed.contains('.')) {
                val suffix = trimmed.substringAfterLast('_').substringAfterLast('.')
                LOOKUP[suffix]?.let { return it }
            }

            return null
        }
    }
}

/**
 * Backward-compat alias so existing call sites (Compose UI, layout serialization, etc.) that
 * still reference the old type name keep compiling untouched. Prefer [ControlKey] in new code.
 */
typealias SubCategoryDefinition = ControlKey

/**
 * Top-level category grouping, computed from a [CategoryType] + its [ControlKey]s.
 *
 * This is deliberately a plain data holder, not a registry — every field is either the
 * [CategoryType]'s own metadata or a straight filter over [ControlKey.entries]. There is
 * nothing here to hand-maintain: add a [ControlKey] with a given `categoryType` and it shows
 * up automatically.
 */
data class CategoryDefinition(
    val type: CategoryType,
    val controls: List<ControlKey>
) : IconBearing {
    val id: String get() = type.id
    val title: String get() = type.displayTitle
    val emoji: String get() = type.emoji
    override val symbol: CategorySymbol get() = type.symbol
    val isGroupCluster: Boolean get() = type.isGroupCluster
    val description: String get() = type.description
    val aliasKeys: Set<String> get() = type.aliasKeys

    /** Canonical list of subcategories / controls belonging to this category. */
    val subCategories: List<ControlKey> get() = controls

    val keys: Set<String> = buildSet {
        addAll(type.aliasKeys)
        add(type.id)
        add(type.displayTitle)
        controls.forEach { ctrl ->
            add(ctrl.key)
            add(ctrl.defaultId)
            addAll(ctrl.aliases)
        }
    }.mapTo(mutableSetOf()) { it.uppercase() }
}
