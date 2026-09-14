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
 * Encapsulates icon names, default emojis, and standard SVG path data for native rendering.
 */
enum class CategorySymbol(
    val iconName: String,
    val defaultEmoji: String,
    val svgPath: String
) {
    GAMEPAD(
        iconName = "SportsEsports",
        defaultEmoji = "🎮",
        svgPath = "M21.58 16.09l-1.09-7.66C20.21 6.46 18.52 5 16.53 5H7.47C5.48 5 3.79 6.46 3.51 8.43l-1.09 7.66C2.2 17.63 3.39 19 4.94 19c.68 0 1.32-.27 1.8-.75L9 16h6l2.25 2.25c.48.48 1.13.75 1.81.75 1.55 0 2.74-1.37 2.52-2.91zM11 11H9v2H8v-2H6v-1h2V8h1v2h2v1zm4-1.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm2 3c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5z"
    ),
    DPAD(
        iconName = "ControlCamera",
        defaultEmoji = "🧭",
        svgPath = "M12 2L6.5 7.5h3.5v3H7V7L1.5 12.5 7 18v-3h3v3.5H6.5L12 24l5.5-5.5h-3.5v-3.5h3V18l5.5-5.5L17 7v3.5h-3v-3h3.5z"
    ),
    TRIGGER(
        iconName = "Tune",
        defaultEmoji = "🎯",
        svgPath = "M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z"
    ),
    BUMPER(
        iconName = "HorizontalRule",
        defaultEmoji = "🛡️",
        svgPath = "M4 11h16v2H4z"
    ),
    STICK(
        iconName = "Album",
        defaultEmoji = "🕹️",
        svgPath = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 14.5c-2.49 0-4.5-2.01-4.5-4.5S9.51 7.5 12 7.5s4.5 2.01 4.5 4.5-2.01 4.5-4.5 4.5zm0-5.5c-.55 0-1 .45-1 1s.45 1 1 1 1-.45 1-1-.45-1-1-1z"
    ),
    HOME(
        iconName = "Home",
        defaultEmoji = "⨂",
        svgPath = "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"
    ),
    SYSTEM(
        iconName = "Settings",
        defaultEmoji = "⚙️",
        svgPath = "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"
    ),
    MACRO(
        iconName = "Bolt",
        defaultEmoji = "⚡",
        svgPath = "M11 21h-1l1-7H7.5c-.58 0-.57-.32-.38-.66.19-.34.05-.08.07-.12C8.48 10.94 10.42 7.54 13 3h1l-1 7h3.5c.49 0 .56.33.47.51l-.07.15C12.96 17.55 11 21 11 21z"
    ),
    ALL(
        iconName = "Widgets",
        defaultEmoji = "🎛️",
        svgPath = "M13 13v8h8v-8h-8zM3 21h8v-8H3v8zM3 3v8h8V3H3zm13.66-1.31L11 7.34 16.66 13l5.66-5.66-5.66-5.65z"
    )
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
    val emoji: String = "",
    val symbol: CategorySymbol = CategorySymbol.GAMEPAD,
    val promptHint: String = "",
    val starterHtmlPreset: String? = null
) {
    val iconName: String get() = symbol.iconName
    val svgPath: String get() = symbol.svgPath
}

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
    val controls: List<SubCategoryDefinition>,
    val aliasKeys: Set<String> = emptySet()
) {
    val iconName: String get() = symbol.iconName
    val svgPath: String get() = symbol.svgPath
    val keys: Set<String> = (controls.map { it.key.uppercase() } + aliasKeys.map { it.uppercase() }).toSet()
}
