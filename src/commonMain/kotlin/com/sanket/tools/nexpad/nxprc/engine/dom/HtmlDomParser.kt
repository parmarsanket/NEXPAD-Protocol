package com.sanket.tools.nexpad.nxprc.engine.dom

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
        "path", "circle", "rect", "polygon", "polyline", "line", "img", "br", "hr", "input", "meta", "link"
    )

    fun parse(html: String): ParsedHtmlResult {
        // 1. Extract embedded <style> blocks and sanitize stray tags
        val styleSb = StringBuilder()
        val stylePattern = Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>", Pattern.CASE_INSENSITIVE)
        val sm = stylePattern.matcher(html)
        while (sm.find()) {
            val content = sm.group(1).replace(Regex("<[^>]+>"), "")
            styleSb.append(content).append("\n")
        }

        // Clean out <head>, <script>, <style> for DOM parsing
        val bodyContent = html
            .replace(Regex("<head[\\s\\S]*?</head>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .trim()

        val root = DomNode(tag = "root")
        var current: DomNode = root

        // Tokenize tags and text
        val tagPattern = Pattern.compile("<(/?)([a-zA-Z0-9_-]+)((?:\\s+[^>]+)?)(/?)>|([^<]+)")
        val matcher = tagPattern.matcher(bodyContent)

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
                    if (current.parent != null && current.tag.equals(tagName, ignoreCase = true)) {
                        current = current.parent!!
                    }
                } else {
                    val attrs = parseAttributes(rawAttrs)
                    val id = attrs["id"]
                    val classNames = attrs["class"]?.split(Regex("\\s+"))?.filter { it.isNotBlank() } ?: emptyList()
                    val inlineStyles = attrs["style"]?.let { parseInlineStyles(it) } ?: emptyMap()

                    val node = DomNode(
                        tag = tagName,
                        id = id,
                        classNames = classNames,
                        inlineStyles = inlineStyles,
                        attributes = attrs,
                        parent = current
                    )
                    current.children.add(node)

                    if (!selfClose) {
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
        val attrPattern = Pattern.compile("([a-zA-Z0-9_-]+)(?:\\s*=\\s*([\"'])([\\s\\S]*?)\\2)?")
        val m = attrPattern.matcher(raw)
        while (m.find()) {
            val key = m.group(1).lowercase()
            val value = m.group(3) ?: ""
            attrs[key] = value
        }
        return attrs
    }

    private fun parseInlineStyles(body: String): Map<String, String> {
        val decls = mutableMapOf<String, String>()
        val parts = body.split(";")
        for (part in parts) {
            val colonIdx = part.indexOf(':')
            if (colonIdx > 0) {
                val prop = part.substring(0, colonIdx).trim().lowercase()
                val value = part.substring(colonIdx + 1).trim()
                if (prop.isNotBlank() && value.isNotBlank()) {
                    decls[prop] = value
                }
            }
        }
        return decls
    }
}
