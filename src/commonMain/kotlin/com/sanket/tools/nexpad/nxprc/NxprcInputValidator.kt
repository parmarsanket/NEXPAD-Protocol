package com.sanket.tools.nexpad.nxprc

/** Validates public compiler inputs before parsing untrusted HTML/CSS. */
internal object NxprcInputValidator {
    fun validateHtml(html: String) {
        require(html.isNotBlank()) { "HTML/CSS input must not be blank" }
        require(html.length <= NxprcDefaults.MAX_HTML_SIZE) {
            "HTML/CSS input exceeds the ${NxprcDefaults.MAX_HTML_SIZE}-byte limit"
        }
    }

    fun validateMetadata(id: String, name: String) {
        require(id.length <= NxprcDefaults.MAX_ID_LENGTH) {
            "NXPRC id exceeds ${NxprcDefaults.MAX_ID_LENGTH} characters"
        }
        require(name.length <= NxprcDefaults.MAX_NAME_LENGTH) {
            "NXPRC name exceeds ${NxprcDefaults.MAX_NAME_LENGTH} characters"
        }
    }
}
