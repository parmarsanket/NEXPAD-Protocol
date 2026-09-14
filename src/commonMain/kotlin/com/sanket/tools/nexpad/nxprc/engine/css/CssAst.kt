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

/**
 * CSS Selector AST node.
 * Supported pseudo-classes: :active, :hover, :focus.
 * Supported pseudo-elements: ::before, ::after (and single-colon :before, :after).
 */
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
        fun parse(rawSelector: String): CssSelector = CssSelectorParser.parse(rawSelector)
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
