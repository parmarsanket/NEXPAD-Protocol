package com.sanket.tools.nexpad.category

/**
 * Top-level Controller Categories.
 */
enum class CategoryType(val id: String, val title: String, val emoji: String) {
    ABXY("ABXY", "ABXY", "🎮"),
    DPAD("DPAD", "D-Pad", "🧭"),
    TRIGGERS("TRIGGERS", "Triggers", "🎯"),
    BUMPERS("BUMPERS", "Bumpers", "🛡️"),
    STICKS("STICKS", "Sticks", "🕹️"),
    SYSTEM("SYSTEM", "System", "⚙️"),
    MACROS("MACROS", "Macros", "⚡")
}

/**
 * Standard NXPRC Component Types.
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
 * Cross-platform icon semantic symbols.
 * Mapped to native Vector Icons in Android and Desktop Compose UI.
 */
enum class CategorySymbol {
    GAMEPAD,
    DPAD,
    TRIGGER,
    BUMPER,
    STICK,
    HOME,
    SYSTEM,
    MACRO,
    ALL
}

/**
 * Sub-category definition representing an individual controller input/button.
 */
data class SubCategoryDefinition(
    val key: String,
    val label: String,
    val defaultName: String,
    val defaultId: String,
    val categoryType: CategoryType,
    val componentType: ComponentType,
    val defaultWidthDp: Int,
    val defaultHeightDp: Int,
    val accentColorArgb: Long,
    val description: String,
    val promptHint: String = "",
    val starterHtmlPreset: String? = null
)

/**
 * Top-level category definition bundling related controls and visual metadata.
 */
data class CategoryDefinition(
    val type: CategoryType,
    val id: String,
    val title: String,
    val emoji: String,
    val symbol: CategorySymbol,
    val isGroupCluster: Boolean = false,
    val description: String,
    val controls: List<SubCategoryDefinition>
) {
    val keys: Set<String> = controls.map { it.key.uppercase() }.toSet()
}
