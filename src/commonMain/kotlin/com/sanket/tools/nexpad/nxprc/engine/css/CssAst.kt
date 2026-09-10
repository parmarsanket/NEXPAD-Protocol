package com.sanket.tools.nexpad.nxprc.engine.css

data class CssStylesheet(
    val rules: List<CssRule> = emptyList(),
    val keyframes: Map<String, CssKeyframes> = emptyMap(),
    val customProperties: Map<String, String> = emptyMap()
)

data class CssRule(
    val selectors: List<CssSelector>,
    val declarations: Map<String, String>
)

data class CssSelector(
    val raw: String,
    val tag: String? = null,
    val className: String? = null,
    val classNames: List<String> = emptyList(),
    val id: String? = null,
    val pseudoClass: String? = null,
    val pseudoElement: String? = null,
    val ancestorSelector: CssSelector? = null,
    val ancestorCombinator: String = " ",
    val specificity: Int = 0
) {
    companion object {
        private val CLASS_REGEX = Regex("\\.([a-zA-Z0-9_-]+)")

        fun parse(rawSelector: String): CssSelector {
            var s = rawSelector.trim()
            var pseudoEl: String? = null
            var pseudoCl: String? = null

            if (s.contains("::")) {
                val parts = s.split("::", limit = 2)
                s = parts[0]
                pseudoEl = parts[1].trim().lowercase()
            } else if (s.contains(":")) {
                val parts = s.split(":", limit = 2)
                s = parts[0]
                val pseudo = parts[1].trim().lowercase()
                if (pseudo == "before" || pseudo == "after") {
                    pseudoEl = pseudo
                } else {
                    pseudoCl = pseudo
                }
            }

            var ancestorSelector: CssSelector? = null
            var ancestorCombinator = " "
            var specificity = 0

            // Handle child selector: e.g. ".button-a > span"
            if (s.contains(">")) {
                val split = s.lastIndexOf('>')
                val ancestorPart = s.substring(0, split).trim()
                s = s.substring(split + 1).trim()
                if (ancestorPart.isNotBlank()) {
                    ancestorSelector = parse(ancestorPart)
                    ancestorCombinator = ">"
                    specificity += ancestorSelector.specificity
                }
            // Handle descendant selector: e.g. ".button-a span"
            } else if (s.contains(" ")) {
                val lastSpace = s.lastIndexOf(' ')
                val ancestorPart = s.substring(0, lastSpace).trim()
                s = s.substring(lastSpace + 1).trim()
                if (ancestorPart.isNotBlank()) {
                    val parsedAncestor = parse(ancestorPart)
                    ancestorSelector = parsedAncestor
                    ancestorCombinator = " "
                    specificity += parsedAncestor.specificity
                }
            }

            var id: String? = null
            var className: String? = null
            var tag: String? = null

            if (s.contains("#")) {
                val parts = s.split("#", limit = 2)
                if (parts[0].isNotBlank()) tag = parts[0].trim().lowercase()
                val sub = parts[1].split(".", limit = 2)
                id = sub[0].trim()
                if (sub.size > 1) className = sub[1].trim()
                specificity += 100
            } else if (s.contains(".")) {
                val parts = s.split(".", limit = 2)
                if (parts[0].isNotBlank()) tag = parts[0].trim().lowercase()
                className = parts[1].trim()
                specificity += 10
            } else if (s.isNotBlank() && s != "*") {
                tag = s.trim().lowercase()
                specificity += 1
            }

            val parsedClasses = CLASS_REGEX
                .findAll(s)
                .map { it.groupValues[1] }
                .toList()
            if (parsedClasses.isNotEmpty()) {
                className = parsedClasses.first()
                specificity += 10 * (parsedClasses.size - 1)
            }

            if (pseudoEl != null) specificity += 1
            if (pseudoCl != null) specificity += 10

            return CssSelector(
                raw = rawSelector,
                tag = tag,
                className = className,
                classNames = parsedClasses,
                id = id,
                pseudoClass = pseudoCl,
                pseudoElement = pseudoEl,
                ancestorSelector = ancestorSelector,
                ancestorCombinator = ancestorCombinator,
                specificity = specificity
            )
        }
    }
}

data class CssKeyframes(
    val name: String,
    val steps: List<CssKeyframeStep>
)

data class CssKeyframeStep(
    val percentage: Float, // 0.0f to 1.0f
    val declarations: Map<String, String>
)
