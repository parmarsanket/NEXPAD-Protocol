package com.sanket.tools.nexpad.nxprc.engine.compiler

import com.sanket.tools.nexpad.nxprc.CanvasLayer

/**
 * Internal tracking entry for a canvas layer during compilation.
 * Associates a [stackIndex] (CSS z-index / elevation tier) and sequential [order]
 * to maintain strict CSS painting order.
 */
internal data class LayerEntry(
    val stackIndex: Int,
    val order: Int,
    val layer: CanvasLayer
)

/**
 * Accumulator for compiled layers supporting automatic sequential order tracking
 * and multi-key sorting by (stackIndex, order).
 */
internal class LayerCollector {
    private val entries = mutableListOf<LayerEntry>()
    private var orderSeq = 0

    fun addLayer(stackIndex: Int, layer: CanvasLayer) {
        entries.add(LayerEntry(stackIndex, orderSeq++, layer))
    }

    fun getSortedLayers(): List<CanvasLayer> =
        entries.sortedWith(compareBy({ it.stackIndex }, { it.order })).map { it.layer }

    fun getAllEntries(): List<LayerEntry> = entries
}
