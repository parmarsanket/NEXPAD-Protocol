package com.sanket.tools.nexpad.nxprc.engine.text

/**
 * Multiplatform Glyph-Accurate Text Measurement.
 * Provides font-accurate glyph width measuring without heuristic drift.
 */
object TextMetrics {

    data class Metrics(
        val width: Float,
        val ascent: Float,
        val descent: Float
    ) {
        val height: Float get() = ascent + descent
    }

    /**
     * Pluggable custom measurer hook for host platforms (e.g. Android Paint or Compose TextMeasurer).
     */
    var customMeasurer: ((text: String, fontSizeSp: Float, fontWeight: Int) -> Metrics)? = null

    fun measure(text: String, fontSizeSp: Float, fontWeight: Int = 400): Metrics {
        if (text.isEmpty()) return Metrics(0f, fontSizeSp * 0.85f, fontSizeSp * 0.25f)

        customMeasurer?.let { return it(text, fontSizeSp, fontWeight) }

        return TypographicFallbackMeasurer.measure(text, fontSizeSp, fontWeight)
    }
}
