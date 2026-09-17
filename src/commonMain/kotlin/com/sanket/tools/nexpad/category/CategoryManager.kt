package com.sanket.tools.nexpad.category

/**
 * Single Source of Truth facade over [ControlKey] / [CategoryType].
 *
 * All button metadata (dimensions, colors, aliases, icons, prompt hints...) now lives
 * declaratively on the enums themselves in CategoryModels.kt. This object holds none of it —
 * it only exposes convenient, backward-compatible query functions to the rest of the app
 * (Android `NEXPAD` + Desktop `NEXPADDesktop`), so existing call sites don't need to change.
 */
object CategoryManager {

    private val CATEGORIES: List<CategoryDefinition> =
        CategoryType.entries.map { type -> CategoryDefinition(type = type, controls = ControlKey.of(type)) }

    private val CATEGORIES_BY_ID: Map<String, CategoryDefinition> =
        CATEGORIES.associateBy { it.id.uppercase() }

    private val CATEGORIES_BY_TYPE: Map<CategoryType, CategoryDefinition> =
        CATEGORIES.associateBy { it.type }

    /** Fallback dimensions when a bare category (not an individual control) is asked for size. */
    private val CATEGORY_FALLBACK_DIMENS: Map<CategoryType, Pair<Int, Int>> = mapOf(
        CategoryType.DPAD to (140 to 140),
        CategoryType.STICKS to (130 to 130),
        CategoryType.ABXY to (160 to 160),
        CategoryType.TRIGGERS to (110 to 140),
        CategoryType.BUMPERS to (120 to 60),
        CategoryType.SYSTEM to (70 to 70),
        CategoryType.MACROS to (72 to 72)
    )

    /** Returns all registered top-level controller categories. */
    fun getAllCategories(): List<CategoryDefinition> = CATEGORIES

    /** Returns every individual control across all categories. */
    fun getAllControls(): List<ControlKey> = ControlKey.entries

    /** Get a category by its [CategoryType]. */
    fun getCategory(type: CategoryType): CategoryDefinition = CATEGORIES_BY_TYPE.getValue(type)

    /** Get a category by ID or alias (case-insensitive, e.g. "ABXY", "BUTTON", "TRIGGER", "TRIGGERS", "STICKS"). */
    fun getCategory(id: String): CategoryDefinition? {
        CATEGORIES_BY_ID[id.uppercase()]?.let { return it }
        val resolvedType = CategoryType.fromIdentifier(id) ?: return null
        return CATEGORIES_BY_TYPE[resolvedType]
    }

    /** Find a control by key or alias (case-insensitive, e.g. "A", "LT", "L2", "DPAD", "START"). */
    fun getControl(key: String): ControlKey? = ControlKey.fromIdentifier(key)

    /**
     * Dynamically resolves any arbitrary input identifier to its canonical [ControlKey].
     * Supports keys, aliases, default component IDs, builtin IDs, and human labels.
     */
    fun resolveControl(identifier: String?): ControlKey? = ControlKey.fromIdentifier(identifier)

    /** Find the parent category for a given control key, alias, or category identifier. */
    fun findCategoryForControl(key: String): CategoryDefinition? {
        getControl(key)?.let { return CATEGORIES_BY_TYPE[it.categoryType] }
        return getCategory(key)
    }

    /** Returns all controls for a given category ID or alias (e.g. "ABXY" or "BUTTON" -> A, B, X, Y). */
    fun getControlsForCategory(categoryId: String): List<ControlKey> =
        getCategory(categoryId)?.controls ?: emptyList()

    /** Resolves the default dimensions (widthDp, heightDp) for a given control key or category id. */
    fun resolveDefaultDimensions(key: String): Pair<Int, Int> {
        getControl(key)?.let { return it.defaultWidthDp to it.defaultHeightDp }
        getCategory(key)?.let { return CATEGORY_FALLBACK_DIMENS[it.type] ?: (96 to 96) }
        return 96 to 96
    }

    /**
     * Resolves the single intrinsic maximum dimension (in dp) for scaling calculations.
     * Centralized single source of truth for Button Studio, HUD, and Canvas preview rendering.
     */
    fun resolveIntrinsicMaxDim(key: String, customWidthDp: Int = 0, customHeightDp: Int = 0): Float {
        if (customWidthDp > 0 && customHeightDp > 0) {
            return maxOf(customWidthDp, customHeightDp).toFloat()
        }
        val (w, h) = resolveDefaultDimensions(key)
        return maxOf(w, h, 1).toFloat()
    }

    /** Get icon emoji for a control key (e.g. "A" -> "🅰️", "LT" -> "🎯"). */
    fun getIconEmoji(key: String): String =
        getControl(key)?.emoji ?: findCategoryForControl(key)?.emoji ?: "🎮"

    /** Get [CategorySymbol] for a control key. */
    fun getIconSymbol(key: String): CategorySymbol =
        getControl(key)?.symbol ?: findCategoryForControl(key)?.symbol ?: CategorySymbol.GAMEPAD

    /** Get icon name for a control key (e.g. "SportsEsports", "Tune", "ControlCamera"). */
    fun getIconName(key: String): String = getIconSymbol(key).iconName

    /** Get standard SVG path data for a control key. */
    fun getIconSvgPath(key: String): String = getIconSymbol(key).svgPath
}
