package com.sanket.tools.nexpad.nxprc.engine.dom

import com.sanket.tools.nexpad.nxprc.engine.css.CssTokenizer
import java.util.regex.Pattern

data class ParsedHtmlResult(
    val root: DomNode,
    val embeddedCss: String
)

/**
 * Robust HTML & SVG DOM Parser.
 * Tokenizes markup into a DOM Tree and extracts embedded `<style>` sheets,
 * automatically sanitizing stray HTML tags (like accidental `<button>`) inside `<style>`.
 */
object HtmlDomParser {

    private val SELF_CLOSING = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "path", "source", "track", "wbr",
        "circle", "rect", "polygon", "polyline", "line"
    )

    private val STYLE_PATTERN = Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>", Pattern.CASE_INSENSITIVE)
    private val STRIP_TAG_REGEX = Regex("<[^>]+>")
    private val COMMENT_REGEX = Regex("<!--[\\s\\S]*?-->")
    private val DOCTYPE_REGEX = Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE)
    private val HEAD_REGEX = Regex("<head[\\s\\S]*?</head>", RegexOption.IGNORE_CASE)
    private val SCRIPT_REGEX = Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE)
    private val STYLE_BLOCK_REGEX = Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE)
    private val TAG_PATTERN = Pattern.compile("<(/?)([a-zA-Z0-9_-]+)((?:\\s+[^>]+)?)(/?)>|([^<]+)")
    private val ATTR_PATTERN = Pattern.compile("([a-zA-Z0-9_-]+)(?:\\s*=\\s*(?:([\"'])([\\s\\S]*?)\\2|([^\\s>]+)))?")
    private val WHITESPACE_REGEX = Regex("\\s+")

    fun parse(html: String): ParsedHtmlResult {
        // 1. Extract embedded <style> blocks and sanitize stray tags
        val styleSb = StringBuilder()
        val sm = STYLE_PATTERN.matcher(html)
        while (sm.find()) {
            val content = sm.group(1).replace(STRIP_TAG_REGEX, "")
            styleSb.append(content).append("\n")
        }

        // Clean out <head>, <script>, <style> for DOM parsing
        val bodyContent = html
            .replace(COMMENT_REGEX, "")
            .replace(DOCTYPE_REGEX, "")
            .replace(HEAD_REGEX, "")
            .replace(SCRIPT_REGEX, "")
            .replace(STYLE_BLOCK_REGEX, "")
            .trim()

        val root = DomNode(tag = "root")
        val stack = mutableListOf(root)
        var current: DomNode = root

        // Tokenize tags and text
        val matcher = TAG_PATTERN.matcher(bodyContent)

        while (matcher.find()) {
            val isClosing = matcher.group(1) == "/"
            val tagName = matcher.group(2)?.lowercase()
            val rawAttrs = matcher.group(3) ?: ""
            val selfClose = matcher.group(4) == "/" || (tagName != null && SELF_CLOSING.contains(tagName))
            val text = matcher.group(5)

            if (text != null) {
                val trimmed = text.trim()
                if (trimmed.isNotBlank()) {
                    current.textContent = (current.textContent + " " + trimmed).trim()
                }
            } else if (tagName != null) {
                if (isClosing) {
                    val openIndex = stack.indexOfLast { it.tag.equals(tagName, ignoreCase = true) }
                    if (openIndex > 0) {
                        while (stack.size > openIndex) stack.removeAt(stack.lastIndex)
                        current = stack.last()
                    }
                } else {
                    val attrs = parseAttributes(rawAttrs)
                    val id = attrs["id"]
                    val classNames = attrs["class"]?.split(WHITESPACE_REGEX)?.filter { it.isNotBlank() } ?: emptyList()
                    val inlineStyles = attrs["style"]?.let { parseInlineStyles(it) } ?: emptyMap()

                    val node = DomNode(
                        tag = tagName,
                        id = id,
                        classNames = classNames,
                        inlineStyles = inlineStyles,
                        attributes = attrs
                    ).apply { parent = current }
                    current.children.add(node)

                    if (!selfClose) {
                        stack.add(node)
                        current = node
                    }
                }
            }
        }

        return ParsedHtmlResult(
            root = root,
            embeddedCss = styleSb.toString()
        )
    }

    private fun parseAttributes(raw: String): Map<String, String> {
        val attrs = mutableMapOf<String, String>()
        val m = ATTR_PATTERN.matcher(raw)
        while (m.find()) {
            val key = m.group(1).lowercase()
            val value = m.group(3) ?: m.group(4) ?: ""
            attrs[key] = value
        }
        return attrs
    }

    private fun parseInlineStyles(body: String): Map<String, String> {
        return CssTokenizer.parseDeclarations(body)
    }
}
