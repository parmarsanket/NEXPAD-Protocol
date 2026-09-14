package com.sanket.tools.nexpad.nxprc.engine.text

/**
 * Result of breaking text into typographic lines with measured dimensions.
 */
data class LineLayoutResult(
    val lines: List<String>,
    val lineWidths: List<Float>,
    val totalHeight: Float,
    val lineHeight: Float
)

/**
 * Typographic line-breaker supporting explicit newlines, word wrapping, and CSS wrapping rules.
 */
object TextLineBreaker {

    fun breakLines(
        text: String,
        maxWidth: Float,
        fontSizeSp: Float,
        fontWeight: Int = 400,
        whiteSpace: String? = null,
        wordBreak: String? = null,
        lineHeightMultiplier: Float = 1.25f
    ): LineLayoutResult {
        if (text.isEmpty()) {
            return LineLayoutResult(
                lines = emptyList(),
                lineWidths = emptyList(),
                totalHeight = 0f,
                lineHeight = fontSizeSp * lineHeightMultiplier
            )
        }

        val cleanWhiteSpace = whiteSpace?.trim()?.lowercase() ?: "normal"
        val isNoWrap = cleanWhiteSpace == "nowrap"
        val isBreakAll = wordBreak?.trim()?.lowercase() == "break-all"
        val baseLineHeight = fontSizeSp * lineHeightMultiplier

        // If nowrap and no explicit newlines, treat as single line
        if (isNoWrap && !text.contains('\n')) {
            val metrics = TextMetrics.measure(text, fontSizeSp, fontWeight)
            return LineLayoutResult(
                lines = listOf(text),
                lineWidths = listOf(metrics.width),
                totalHeight = maxOf(metrics.height, baseLineHeight),
                lineHeight = baseLineHeight
            )
        }

        val rawParagraphs = text.split('\n')
        val finalLines = mutableListOf<String>()
        val finalWidths = mutableListOf<Float>()

        for (paragraph in rawParagraphs) {
            if (paragraph.isBlank()) {
                finalLines.add("")
                finalWidths.add(0f)
                continue
            }

            val pMetrics = TextMetrics.measure(paragraph, fontSizeSp, fontWeight)
            if (isNoWrap || maxWidth <= 0f || pMetrics.width <= maxWidth) {
                finalLines.add(paragraph)
                finalWidths.add(pMetrics.width)
                continue
            }

            // Word wrap
            val words = paragraph.split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (words.isEmpty()) continue

            var currentLine = StringBuilder()
            var currentLineWidth = 0f

            for (word in words) {
                val wordMetrics = TextMetrics.measure(word, fontSizeSp, fontWeight)
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val testWidth = TextMetrics.measure(testLine, fontSizeSp, fontWeight).width

                if (testWidth <= maxWidth || currentLine.isEmpty()) {
                    currentLine = StringBuilder(testLine)
                    currentLineWidth = testWidth
                } else {
                    // Line would exceed maxWidth: push currentLine and start new line
                    finalLines.add(currentLine.toString())
                    finalWidths.add(currentLineWidth)

                    if (wordMetrics.width > maxWidth && isBreakAll) {
                        // Break single word character-by-character
                        var charLine = StringBuilder()
                        for (ch in word) {
                            val testCharLine = "$charLine$ch"
                            val testCharWidth = TextMetrics.measure(testCharLine, fontSizeSp, fontWeight).width
                            if (testCharWidth <= maxWidth || charLine.isEmpty()) {
                                charLine.append(ch)
                            } else {
                                finalLines.add(charLine.toString())
                                finalWidths.add(TextMetrics.measure(charLine.toString(), fontSizeSp, fontWeight).width)
                                charLine = StringBuilder("$ch")
                            }
                        }
                        currentLine = charLine
                        currentLineWidth = TextMetrics.measure(currentLine.toString(), fontSizeSp, fontWeight).width
                    } else {
                        currentLine = StringBuilder(word)
                        currentLineWidth = wordMetrics.width
                    }
                }
            }

            if (currentLine.isNotEmpty()) {
                finalLines.add(currentLine.toString())
                finalWidths.add(currentLineWidth)
            }
        }

        val totalHeight = finalLines.size * baseLineHeight
        return LineLayoutResult(
            lines = finalLines,
            lineWidths = finalWidths,
            totalHeight = totalHeight,
            lineHeight = baseLineHeight
        )
    }
}
