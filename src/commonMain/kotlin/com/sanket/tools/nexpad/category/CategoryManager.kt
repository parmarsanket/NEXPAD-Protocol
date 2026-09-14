package com.sanket.tools.nexpad.category

/**
 * Single Source of Truth for all Gamepad Controller Categories and Sub-Categories.
 *
 * Centralizes all dimensions, color accents, naming conventions, AI prompt hints,
 * and key mappings across both Android (`NEXPAD`) and Desktop (`NEXPADDesktop`).
 */
object CategoryManager {

    private val ABXY_CATEGORY = CategoryDefinition(
        type = CategoryType.ABXY,
        id = "ABXY",
        title = "ABXY",
        emoji = "🎮",
        symbol = CategorySymbol.GAMEPAD,
        isGroupCluster = true,
        description = "Action button cluster for primary combat and interaction.",
        controls = listOf(
            SubCategoryDefinition(
                key = "A",
                label = "A Button",
                defaultName = "Action A Button",
                defaultId = "rc.action_a",
                categoryType = CategoryType.ABXY,
                componentType = ComponentType.BUTTON,
                defaultWidthDp = 96,
                defaultHeightDp = 96,
                accentColorArgb = 0xFF4ADE80L, // Neon Green
                description = "Primary confirmation / jump button.",
                emoji = "🅰️",
                symbol = CategorySymbol.GAMEPAD,
                promptHint = "Free silhouette, distinct tactile depth, glossy central core with high-contrast glyph."
            ),
            SubCategoryDefinition(
                key = "B",
                label = "B Button",
                defaultName = "Action B Button",
                defaultId = "rc.action_b",
                categoryType = CategoryType.ABXY,
                componentType = ComponentType.BUTTON,
                defaultWidthDp = 96,
                defaultHeightDp = 96,
                accentColorArgb = 0xFFF87171L, // Neon Red
                description = "Secondary cancellation / evade button.",
                emoji = "🅱️",
                symbol = CategorySymbol.GAMEPAD,
                promptHint = "Free silhouette, aggressive beveling, intense crimson accenting."
            ),
            SubCategoryDefinition(
                key = "X",
                label = "X Button",
                defaultName = "Action X Button",
                defaultId = "rc.action_x",
                categoryType = CategoryType.ABXY,
                componentType = ComponentType.BUTTON,
                defaultWidthDp = 96,
                defaultHeightDp = 96,
                accentColorArgb = 0xFF60A5FAL, // Neon Blue
                description = "Tertiary light attack / reload button.",
                emoji = "✖️",
                symbol = CategorySymbol.GAMEPAD,
                promptHint = "Free silhouette, energetic cyan/blue glow, crisp industrial styling."
            ),
            SubCategoryDefinition(
                key = "Y",
                label = "Y Button",
                defaultName = "Action Y Button",
                defaultId = "rc.action_y",
                categoryType = CategoryType.ABXY,
                componentType = ComponentType.BUTTON,
                defaultWidthDp = 96,
                defaultHeightDp = 96,
                accentColorArgb = 0xFFFBBF24L, // Neon Yellow / Amber
                description = "Quaternary heavy attack / special button.",
                emoji = "🆈",
                symbol = CategorySymbol.GAMEPAD,
                promptHint = "Free silhouette, luminous amber highlights, polished top chamfer."
            )
        )
    )

    private val DPAD_CATEGORY = CategoryDefinition(
        type = CategoryType.DPAD,
        id = "DPAD",
        title = "D-Pad",
        emoji = "🧭",
        symbol = CategorySymbol.DPAD,
        isGroupCluster = false,
        description = "Directional navigation cross and cardinal facets.",
        aliasKeys = setOf("CROSS"),
        controls = listOf(
            SubCategoryDefinition(
                key = "DPAD",
                label = "D-Pad Cross",
                defaultName = "Directional 4-Way Cross",
                defaultId = "rc.dpad_cross",
                categoryType = CategoryType.DPAD,
                componentType = ComponentType.DPAD,
                defaultWidthDp = 140,
                defaultHeightDp = 140,
                accentColorArgb = 0xFF22D3EEL, // Neon Cyan
                description = "Unified 4-way cross directional pad.",
                emoji = "🧭",
                symbol = CategorySymbol.DPAD,
                promptHint = "Central dish or pivot well with 4 directional arms, tactile directional arrows."
            ),
            SubCategoryDefinition(
                key = "UP",
                label = "D-Pad Up",
                defaultName = "Directional Up",
                defaultId = "rc.dpad_up",
                categoryType = CategoryType.DPAD,
                componentType = ComponentType.DPAD,
                defaultWidthDp = 80,
                defaultHeightDp = 80,
                accentColorArgb = 0xFF22D3EEL,
                description = "Individual upward directional button.",
                emoji = "⬆️",
                symbol = CategorySymbol.DPAD,
                promptHint = "North-facing wedge or directional arrowhead, upward gradient highlight."
            ),
            SubCategoryDefinition(
                key = "DOWN",
                label = "D-Pad Down",
                defaultName = "Directional Down",
                defaultId = "rc.dpad_down",
                categoryType = CategoryType.DPAD,
                componentType = ComponentType.DPAD,
                defaultWidthDp = 80,
                defaultHeightDp = 80,
                accentColorArgb = 0xFF22D3EEL,
                description = "Individual downward directional button.",
                emoji = "⬇️",
                symbol = CategorySymbol.DPAD,
                promptHint = "South-facing wedge or directional arrowhead, bottom shadow drop."
            ),
            SubCategoryDefinition(
                key = "LEFT",
                label = "D-Pad Left",
                defaultName = "Directional Left",
                defaultId = "rc.dpad_left",
                categoryType = CategoryType.DPAD,
                componentType = ComponentType.DPAD,
                defaultWidthDp = 80,
                defaultHeightDp = 80,
                accentColorArgb = 0xFF22D3EEL,
                description = "Individual leftward directional button.",
                emoji = "⬅️",
                symbol = CategorySymbol.DPAD,
                promptHint = "West-facing wedge or directional arrowhead, left rim highlight."
            ),
            SubCategoryDefinition(
                key = "RIGHT",
                label = "D-Pad Right",
                defaultName = "Directional Right",
                defaultId = "rc.dpad_right",
                categoryType = CategoryType.DPAD,
                componentType = ComponentType.DPAD,
                defaultWidthDp = 80,
                defaultHeightDp = 80,
                accentColorArgb = 0xFF22D3EEL,
                description = "Individual rightward directional button.",
                emoji = "➡️",
                symbol = CategorySymbol.DPAD,
                promptHint = "East-facing wedge or directional arrowhead, right rim highlight."
            )
        )
    )

    private val TRIGGERS_CATEGORY = CategoryDefinition(
        type = CategoryType.TRIGGERS,
        id = "TRIGGERS",
        title = "Triggers",
        emoji = "🎯",
        symbol = CategorySymbol.TRIGGER,
        isGroupCluster = true,
        description = "Analog linear pressure triggers with progressive deflection.",
        controls = listOf(
            SubCategoryDefinition(
                key = "LT",
                label = "Left Trigger",
                defaultName = "Analog Left Trigger",
                defaultId = "rc.trigger_lt",
                categoryType = CategoryType.TRIGGERS,
                componentType = ComponentType.TRIGGER,
                defaultWidthDp = 110,
                defaultHeightDp = 140,
                accentColorArgb = 0xFFA855F7L, // Neon Purple
                description = "Left progressive analog trigger (aim / brake).",
                emoji = "🎯",
                symbol = CategorySymbol.TRIGGER,
                promptHint = "Elongated trigger paddle, progressive pressure glow indicator, mechanical ribbing."
            ),
            SubCategoryDefinition(
                key = "RT",
                label = "Right Trigger",
                defaultName = "Analog Right Trigger",
                defaultId = "rc.trigger_rt",
                categoryType = CategoryType.TRIGGERS,
                componentType = ComponentType.TRIGGER,
                defaultWidthDp = 110,
                defaultHeightDp = 140,
                accentColorArgb = 0xFFA855F7L,
                description = "Right progressive analog trigger (fire / accelerate).",
                emoji = "🎯",
                symbol = CategorySymbol.TRIGGER,
                promptHint = "Elongated trigger paddle, progressive pressure glow indicator, tactile rear notch."
            )
        )
    )

    private val BUMPERS_CATEGORY = CategoryDefinition(
        type = CategoryType.BUMPERS,
        id = "BUMPERS",
        title = "Bumpers",
        emoji = "🛡️",
        symbol = CategorySymbol.BUMPER,
        isGroupCluster = true,
        description = "Curved digital shoulder bumpers with micro-switch click.",
        controls = listOf(
            SubCategoryDefinition(
                key = "LB",
                label = "Left Bumper",
                defaultName = "Shoulder Left Bumper",
                defaultId = "rc.bumper_lb",
                categoryType = CategoryType.BUMPERS,
                componentType = ComponentType.BUMPER,
                defaultWidthDp = 120,
                defaultHeightDp = 60,
                accentColorArgb = 0xFF38BDF8L, // Sky Blue
                description = "Left digital shoulder bumper.",
                emoji = "🛡️",
                symbol = CategorySymbol.BUMPER,
                promptHint = "Horizontal ergonomic pill/capsule curve, top edge metallic reflection, tactile microswitch response."
            ),
            SubCategoryDefinition(
                key = "RB",
                label = "Right Bumper",
                defaultName = "Shoulder Right Bumper",
                defaultId = "rc.bumper_rb",
                categoryType = CategoryType.BUMPERS,
                componentType = ComponentType.BUMPER,
                defaultWidthDp = 120,
                defaultHeightDp = 60,
                accentColorArgb = 0xFF38BDF8L,
                description = "Right digital shoulder bumper.",
                emoji = "🛡️",
                symbol = CategorySymbol.BUMPER,
                promptHint = "Horizontal ergonomic pill/capsule curve, top edge metallic reflection, tactile microswitch response."
            )
        )
    )

    private val STICKS_CATEGORY = CategoryDefinition(
        type = CategoryType.STICKS,
        id = "STICKS",
        title = "Sticks",
        emoji = "🕹️",
        symbol = CategorySymbol.STICK,
        isGroupCluster = true,
        description = "Dual 360-degree analog joysticks with concave thumb grip.",
        controls = listOf(
            SubCategoryDefinition(
                key = "LS",
                label = "Left Stick",
                defaultName = "Left Analog Thumbstick",
                defaultId = "rc.stick_ls",
                categoryType = CategoryType.STICKS,
                componentType = ComponentType.JOYSTICK,
                defaultWidthDp = 130,
                defaultHeightDp = 130,
                accentColorArgb = 0xFF34D399L, // Neon Emerald
                description = "Left 360° analog thumbstick (movement).",
                emoji = "🕹️",
                symbol = CategorySymbol.STICK,
                promptHint = "Stationary spherical/radial gimbal base with inner socket shadow + floating 360-degree deflection thumb cap with concave grip and knurled ring."
            ),
            SubCategoryDefinition(
                key = "RS",
                label = "Right Stick",
                defaultName = "Right Analog Thumbstick",
                defaultId = "rc.stick_rs",
                categoryType = CategoryType.STICKS,
                componentType = ComponentType.JOYSTICK,
                defaultWidthDp = 130,
                defaultHeightDp = 130,
                accentColorArgb = 0xFF34D399L,
                description = "Right 360° analog thumbstick (camera/aim).",
                emoji = "🕹️",
                symbol = CategorySymbol.STICK,
                promptHint = "Stationary spherical/radial gimbal base with inner socket shadow + floating 360-degree deflection thumb cap with concave grip and knurled ring."
            )
        )
    )

    private val SYSTEM_CATEGORY = CategoryDefinition(
        type = CategoryType.SYSTEM,
        id = "SYSTEM",
        title = "System",
        emoji = "⚙️",
        symbol = CategorySymbol.SYSTEM,
        isGroupCluster = false,
        description = "System, navigation, utility, and special function buttons.",
        aliasKeys = setOf("VIEW", "MENU", "SELECT", "HOME", "XBOX"),
        controls = listOf(
            SubCategoryDefinition(
                key = "START",
                label = "Menu / Start",
                defaultName = "System Menu Button",
                defaultId = "rc.sys_start",
                categoryType = CategoryType.SYSTEM,
                componentType = ComponentType.SYSTEM,
                defaultWidthDp = 70,
                defaultHeightDp = 70,
                accentColorArgb = 0xFF94A3B8L, // Slate Silver
                description = "Pause / Options / Start button.",
                emoji = "☰",
                symbol = CategorySymbol.SYSTEM,
                promptHint = "Compact pill or small disc with hamburger lines or forward glyph."
            ),
            SubCategoryDefinition(
                key = "BACK",
                label = "View / Back",
                defaultName = "System View Button",
                defaultId = "rc.sys_back",
                categoryType = CategoryType.SYSTEM,
                componentType = ComponentType.SYSTEM,
                defaultWidthDp = 70,
                defaultHeightDp = 70,
                accentColorArgb = 0xFF94A3B8L,
                description = "Map / Back / Select button.",
                emoji = "⧉",
                symbol = CategorySymbol.SYSTEM,
                promptHint = "Compact pill or small disc with overlapping squares or rewind glyph."
            ),
            SubCategoryDefinition(
                key = "GUIDE",
                label = "Nexus Guide",
                defaultName = "Controller Center Guide",
                defaultId = "rc.sys_guide",
                categoryType = CategoryType.SYSTEM,
                componentType = ComponentType.SYSTEM,
                defaultWidthDp = 84,
                defaultHeightDp = 84,
                accentColorArgb = 0xFFF59E0BL, // Nexus Amber
                description = "Home / Xbox / PlayStation central guide button.",
                emoji = "⨂",
                symbol = CategorySymbol.HOME,
                promptHint = "Large luminous orb or badge, glowing center insignia, prestigious bevel."
            ),
            SubCategoryDefinition(
                key = "SHARE",
                label = "Share / Capture",
                defaultName = "System Share Button",
                defaultId = "rc.sys_share",
                categoryType = CategoryType.SYSTEM,
                componentType = ComponentType.SYSTEM,
                defaultWidthDp = 70,
                defaultHeightDp = 70,
                accentColorArgb = 0xFF94A3B8L,
                description = "Capture screenshot or video clip.",
                emoji = "📤",
                symbol = CategorySymbol.SYSTEM,
                promptHint = "Minimalist utility button with broadcast or share glyph."
            ),
            SubCategoryDefinition(
                key = "TURBO",
                label = "Turbo",
                defaultName = "Rapid Turbo Trigger",
                defaultId = "rc.sys_turbo",
                categoryType = CategoryType.SYSTEM,
                componentType = ComponentType.SYSTEM,
                defaultWidthDp = 70,
                defaultHeightDp = 70,
                accentColorArgb = 0xFFEC4899L, // Pink
                description = "Hardware rapid-fire turbo switch.",
                emoji = "⚡",
                symbol = CategorySymbol.SYSTEM,
                promptHint = "Lightning insignia with energetic magenta backlighting."
            ),
            SubCategoryDefinition(
                key = "PROFILE",
                label = "Profile",
                defaultName = "Profile Switch Button",
                defaultId = "rc.sys_profile",
                categoryType = CategoryType.SYSTEM,
                componentType = ComponentType.SYSTEM,
                defaultWidthDp = 70,
                defaultHeightDp = 70,
                accentColorArgb = 0xFF8B5CF6L, // Violet
                description = "Toggle between custom layout profiles.",
                emoji = "👤",
                symbol = CategorySymbol.SYSTEM,
                promptHint = "Switch/cycle icon with multi-state indicator LEDs."
            )
        )
    )

    private val MACROS_CATEGORY = CategoryDefinition(
        type = CategoryType.MACROS,
        id = "MACROS",
        title = "Macros",
        emoji = "⚡",
        symbol = CategorySymbol.MACRO,
        isGroupCluster = false,
        description = "Rear programmable paddles and custom macro actuators.",
        controls = listOf(
            SubCategoryDefinition(
                key = "M1",
                label = "Paddle M1",
                defaultName = "Paddle M1 Switch",
                defaultId = "rc.macro_m1",
                categoryType = CategoryType.MACROS,
                componentType = ComponentType.BUTTON,
                defaultWidthDp = 72,
                defaultHeightDp = 72,
                accentColorArgb = 0xFFF59E0BL,
                description = "Rear upper-left programmable paddle.",
                emoji = "⚡",
                symbol = CategorySymbol.MACRO,
                promptHint = "Ergonomic angled wing or rear paddle shape with high-tactile snap."
            ),
            SubCategoryDefinition(
                key = "M2",
                label = "Paddle M2",
                defaultName = "Paddle M2 Switch",
                defaultId = "rc.macro_m2",
                categoryType = CategoryType.MACROS,
                componentType = ComponentType.BUTTON,
                defaultWidthDp = 72,
                defaultHeightDp = 72,
                accentColorArgb = 0xFFF59E0BL,
                description = "Rear upper-right programmable paddle.",
                emoji = "⚡",
                symbol = CategorySymbol.MACRO,
                promptHint = "Ergonomic angled wing or rear paddle shape with high-tactile snap."
            ),
            SubCategoryDefinition(
                key = "M3",
                label = "Paddle M3",
                defaultName = "Paddle M3 Switch",
                defaultId = "rc.macro_m3",
                categoryType = CategoryType.MACROS,
                componentType = ComponentType.BUTTON,
                defaultWidthDp = 72,
                defaultHeightDp = 72,
                accentColorArgb = 0xFFF59E0BL,
                description = "Rear lower-left programmable paddle.",
                emoji = "⚡",
                symbol = CategorySymbol.MACRO,
                promptHint = "Ergonomic angled wing or rear paddle shape with high-tactile snap."
            ),
            SubCategoryDefinition(
                key = "M4",
                label = "Paddle M4",
                defaultName = "Paddle M4 Switch",
                defaultId = "rc.macro_m4",
                categoryType = CategoryType.MACROS,
                componentType = ComponentType.BUTTON,
                defaultWidthDp = 72,
                defaultHeightDp = 72,
                accentColorArgb = 0xFFF59E0BL,
                description = "Rear lower-right programmable paddle.",
                emoji = "⚡",
                symbol = CategorySymbol.MACRO,
                promptHint = "Ergonomic angled wing or rear paddle shape with high-tactile snap."
            )
        )
    )

    private val ALL_CATEGORIES = listOf(
        ABXY_CATEGORY,
        DPAD_CATEGORY,
        TRIGGERS_CATEGORY,
        BUMPERS_CATEGORY,
        STICKS_CATEGORY,
        SYSTEM_CATEGORY,
        MACROS_CATEGORY
    )

    private val CATEGORIES_BY_ID = ALL_CATEGORIES.associateBy { it.id.uppercase() }
    private val CATEGORIES_BY_TYPE = ALL_CATEGORIES.associateBy { it.type }

    private val CONTROLS_BY_KEY: Map<String, SubCategoryDefinition> = buildMap {
        ALL_CATEGORIES.forEach { cat ->
            cat.controls.forEach { ctrl ->
                put(ctrl.key.uppercase(), ctrl)
            }
        }
        // Common aliases
        val start = get("START")
        if (start != null) {
            put("MENU", start)
        }
        val back = get("BACK")
        if (back != null) {
            put("VIEW", back)
            put("SELECT", back)
        }
        val guide = get("GUIDE")
        if (guide != null) {
            put("XBOX", guide)
            put("HOME", guide)
        }
    }

    /**
     * Returns all registered top-level controller categories.
     */
    fun getAllCategories(): List<CategoryDefinition> = ALL_CATEGORIES

    /**
     * Get a category by its CategoryType.
     */
    fun getCategory(type: CategoryType): CategoryDefinition =
        CATEGORIES_BY_TYPE[type] ?: ABXY_CATEGORY

    /**
     * Get a category by ID (case-insensitive).
     */
    fun getCategory(id: String): CategoryDefinition? =
        CATEGORIES_BY_ID[id.uppercase()]

    /**
     * Find a specific control by key (case-insensitive, e.g. "A", "LT", "LS", "DPAD", "START").
     */
    fun getControl(key: String): SubCategoryDefinition? =
        CONTROLS_BY_KEY[key.uppercase()]

    /**
     * Find the parent category for a given control key.
     */
    fun findCategoryForControl(key: String): CategoryDefinition? {
        val ctrl = getControl(key) ?: return null
        return CATEGORIES_BY_TYPE[ctrl.categoryType]
    }

    /**
     * Returns all controls for a given category ID (e.g. "ABXY" -> A, B, X, Y).
     */
    fun getControlsForCategory(categoryId: String): List<SubCategoryDefinition> =
        getCategory(categoryId)?.controls ?: emptyList()

    /**
     * Resolves the default dimensions (widthDp, heightDp) for a given control key.
     */
    fun resolveDefaultDimensions(key: String): Pair<Int, Int> {
        val ctrl = getControl(key)
        return if (ctrl != null) {
            Pair(ctrl.defaultWidthDp, ctrl.defaultHeightDp)
        } else {
            Pair(96, 96)
        }
    }

    /**
     * Get icon emoji for a control key (e.g. "A" -> "🅰️", "LT" -> "🎯").
     */
    fun getIconEmoji(key: String): String =
        getControl(key)?.emoji ?: findCategoryForControl(key)?.emoji ?: "🎮"

    /**
     * Get CategorySymbol for a control key (e.g. "A" -> CategorySymbol.GAMEPAD, "LT" -> CategorySymbol.TRIGGER).
     */
    fun getIconSymbol(key: String): CategorySymbol =
        getControl(key)?.symbol ?: findCategoryForControl(key)?.symbol ?: CategorySymbol.GAMEPAD

    /**
     * Get icon name for a control key (e.g. "SportsEsports", "Tune", "ControlCamera").
     */
    fun getIconName(key: String): String =
        getIconSymbol(key).iconName

    /**
     * Get standard SVG path data for a control key.
     */
    fun getIconSvgPath(key: String): String =
        getIconSymbol(key).svgPath
}
