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
    val layer: CanvasLayer,
    val isThumbCap: Boolean = false
)

/**
 * Accumulator for compiled layers supporting automatic sequential order tracking
 * and multi-key sorting by (stackIndex, order).
 */
internal class LayerCollector {
    private val entries = mutableListOf<LayerEntry>()
    private var orderSeq = 0

    fun addLayer(stackIndex: Int, layer: CanvasLayer, isThumbCap: Boolean = false) {
        entries.add(LayerEntry(stackIndex, orderSeq++, layer, isThumbCap))
    }

    fun getSortedLayers(): List<CanvasLayer> =
        entries.sortedWith(compareBy({ it.stackIndex }, { it.order })).map { it.layer }

    fun getSortedLayersAndCapIndices(): Pair<List<CanvasLayer>, List<Int>> {
        val sorted = entries.sortedWith(compareBy({ it.stackIndex }, { it.order }))
        val layers = sorted.map { it.layer }
        val capIndices = sorted.mapIndexedNotNull { index, entry -> if (entry.isThumbCap) index else null }
        return Pair(layers, capIndices)
    }

    fun getAllEntries(): List<LayerEntry> = entries
}
