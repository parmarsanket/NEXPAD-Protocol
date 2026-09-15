package com.sanket.tools.nexpad.nxprc

import com.sanket.tools.nexpad.category.CategoryManager

/**
 * Validates public compiler inputs before parsing untrusted HTML/CSS.
 * Enforces the NXPRC manifest spec:
 * - id must be non-blank and max [NxprcDefaults.MAX_ID_LENGTH] chars
 * - name must be non-blank and max [NxprcDefaults.MAX_NAME_LENGTH] chars
 * - category must be one of the known NXPRC category strings
 * - defaultControl must be a key known to [CategoryManager] (or blank to auto-detect)
 */
internal object NxprcInputValidator {

    private val VALID_CATEGORIES = setOf(
        "BUTTON", "ABXY", "DPAD", "JOYSTICK", "STICK", "STICKS", "TRIGGER", "TRIGGERS", "BUMPER", "BUMPERS", "HOME", "SYSTEM", "MACRO", "MACROS"
    )

    fun validateHtml(html: String) {
        require(html.isNotBlank()) { "HTML/CSS input must not be blank" }
        // Check byte size (not character count) for accurate UTF-8 limit enforcement
        val byteSize = html.encodeToByteArray().size
        require(byteSize <= NxprcDefaults.MAX_HTML_SIZE) {
            "HTML/CSS input exceeds the ${NxprcDefaults.MAX_HTML_SIZE}-byte limit (actual: $byteSize bytes)"
        }
    }

    fun validateMetadata(id: String, name: String) {
        // Blank id/name is allowed at the packager level because the compiler
        // auto-detects id and name from HTML data-id, class name, or data-name attributes.
        // When explicitly supplied, length bounds are strictly enforced.
        require(id.length <= NxprcDefaults.MAX_ID_LENGTH) {
            "NXPRC id exceeds ${NxprcDefaults.MAX_ID_LENGTH} characters"
        }
        require(name.length <= NxprcDefaults.MAX_NAME_LENGTH) {
            "NXPRC name exceeds ${NxprcDefaults.MAX_NAME_LENGTH} characters"
        }
    }

    /**
     * HIGH 1 FIX: Validates that [category] is one of the known NXPRC category strings.
     * Case-insensitive. Throws [IllegalArgumentException] on unknown values.
     */
    fun validateCategory(category: String) {
        require(category.isNotBlank()) { "NXPRC category must not be blank" }
        require(category.uppercase() in VALID_CATEGORIES) {
            "Unknown NXPRC category: \"$category\". Must be one of: ${VALID_CATEGORIES.sorted().joinToString()}"
        }
    }

    /**
     * HIGH 6 FIX: Validates that [defaultControl] is a key or category known to [CategoryManager].
     * A blank value is allowed (compiler will auto-detect from HTML attributes or use the caller-supplied default).
     */
    fun validateControl(defaultControl: String) {
        if (defaultControl.isBlank()) return
        val upper = defaultControl.uppercase()
        require(CategoryManager.getControl(upper) != null || CategoryManager.getCategory(upper) != null || upper == "DPAD") {
            "Unknown defaultControl key: \"$defaultControl\". Must be a valid CategoryManager key or category (A, B, X, Y, DPAD, LT, RT, LB, RB, LS, RS, UP, DOWN, LEFT, RIGHT, START, BACK, GUIDE, …)"
        }
    }
}
