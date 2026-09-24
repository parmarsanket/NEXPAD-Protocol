package com.sanket.tools.nexpad.nxprc

/**
 * Authoritative single source of truth for NXPRC compilation capabilities and constraints.
 * Synchronizes the compiler, validator, diagnostics, AI prompt generation, and repair loop.
 */
data class EngineCapabilities(
    val cssGrid: Boolean = false,
    val cssMask: Boolean = false,
    val cssBlendMode: Boolean = false,
    val cssBackdropFilter: Boolean = false,
    val cssTransitions: Boolean = false,
    val cssKeyframesTransformOpacity: Boolean = true,

    val flexRowColumn: Boolean = true,
    val flexWrap: Boolean = true,
    val flexGaps: Boolean = true,
    val flexGrow: Boolean = false,
    val flexShrink: Boolean = false,

    val calc: Boolean = true,
    val aspectRatio: Boolean = true,

    val svgTransforms: Boolean = true,
    val svgFilters: Boolean = false, // SVG filter graphs like feGaussianBlur are unsupported, recommend CSS blur
    val nestedDomTransforms: Boolean = false, // DOM transform composition is local to layer
    val dataLayerRoles: Boolean = true
) {
    companion object {
        val CURRENT = EngineCapabilities()
    }
}
