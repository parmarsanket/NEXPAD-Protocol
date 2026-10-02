package com.sanket.tools.nexpad.nxprc

import kotlin.math.abs

/**
 * Layer-by-layer assertion DSL for NXPRC canvas output.
 *
 * This is the permanent fix for the "bug loop" problem:
 * instead of visual inspection (which misses regressions), every HTML design
 * has a machine-checkable contract that specifies the exact layer type,
 * normalized position/size ratios, shape type, and z-order.
 *
 * ## Architecture note: GradientShape vs BoxLayer
 * The NXPRC engine emits TWO kinds of "box" layers:
 *  - [CanvasLayer.GradientShape] — the PRIMARY layer (root button element)
 *  - [CanvasLayer.BoxLayer]      — CHILD layers (pseudo-elements, nested divs)
 *
 * Use [AuditContext.primaryLayer] to check [GradientShape] (root element).
 * Use [AuditContext.boxLayer] to check [BoxLayer] (children/pseudo-elements).
 * Use [AuditContext.hasLayer] / [AuditContext.noLayer] for presence/absence.
 *
 * ## GradientShape positions
 * [GradientShape] also stores position as ratios via the same field names:
 * `widthRatio`, `heightRatio`, `offsetXRatio`, `offsetYRatio` on `effectiveTransform`.
 *
 * ## Usage
 * ```kotlin
 * NxprcLayerAudit.auditHtml(html, "my-id", "My Button", bW = 80f, bH = 80f) {
 *     totalLayers(atLeast = 2)
 *     primaryLayer(0) {
 *         shapeOval()
 *         ratio(rW = 1f, rH = 1f, rX = 0f, rY = 0f)
 *     }
 *     hasLayer<CanvasLayer.GlowRing>()
 *     hasLayer<CanvasLayer.InnerShadow>()
 *     boxLayer(2) {
 *         px(left = 12f, top = 12f, width = 72f, height = 72f)
 *     }
 * }
 * ```
 */
object NxprcLayerAudit {

    /**
     * Compile [html] through the full NXPRC pipeline and run layer assertions.
     *
     * @param html      Raw HTML/CSS string to compile
     * @param id        Document ID
     * @param name      Human-readable document name (shown in failure messages)
     * @param category  Button category (BUTTON, JOYSTICK, TOUCHPAD, DPAD, etc.)
     * @param control   Default control name (A, B, X, Y, LS, RS, etc.)
     * @param bW        Button canvas width in dp — used to convert px assertions to ratios
     * @param bH        Button canvas height in dp — used to convert px assertions to ratios
     * @param block     Audit DSL block
     */
    fun auditHtml(
        html: String,
        id: String,
        name: String,
        category: String = "BUTTON",
        control: String = "A",
        bW: Float = 96f,
        bH: Float = 96f,
        block: AuditContext.() -> Unit
    ) {
        val result = NxprcPackager.compileWithWarnings(html, id, name, category, control)
        val doc = result.document
        val layers = doc.canvas.layers

        println("=== AUDIT: $name ($id) — ${layers.size} layers ===")
        layers.forEachIndexed { i, l ->
            val summary = when (l) {
                is CanvasLayer.BoxLayer -> {
                    val t = l.effectiveTransform
                    "[BoxLayer] shape=${l.shapeType} poly=${l.polygonSides} " +
                        "offX=%.3f offY=%.3f w=%.3f h=%.3f".format(
                            t.offsetXRatio, t.offsetYRatio, l.widthRatio, l.heightRatio
                        )
                }
                is CanvasLayer.GradientShape -> {
                    val t = l.effectiveTransform
                    "[GradientShape] shape=${l.shapeType} " +
                        "offX=%.3f offY=%.3f w=%.3f h=%.3f".format(
                            t.offsetXRatio, t.offsetYRatio, l.widthRatio, l.heightRatio
                        )
                }
                is CanvasLayer.GlowRing       -> "[GlowRing] blur=${l.blurRadius}"
                is CanvasLayer.BezelSocket    -> "[BezelSocket]"
                is CanvasLayer.InnerShadow    -> "[InnerShadow] stroke=${l.strokeWidth}"
                is CanvasLayer.VectorPath     -> "[VectorPath]"
                is CanvasLayer.TextLayer      -> "[TextLayer] \"${l.text}\""
                is CanvasLayer.CenterGlyph    -> "[CenterGlyph] \"${l.text}\""
                is CanvasLayer.GlossReflection -> "[GlossReflection]"
                else -> "[${l::class.simpleName}]"
            }
            println("  [$i] $summary")
        }

        val ctx = AuditContext(layers, name, bW, bH)
        ctx.block()
        ctx.assertNoPendingFailures()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Audit Context
    // ─────────────────────────────────────────────────────────────────────────

    class AuditContext(
        val layers: List<CanvasLayer>,
        val docName: String,
        val bW: Float,
        val bH: Float
    ) {
        private val failures = mutableListOf<String>()

        fun recordFailure(msg: String) { failures.add(msg) }

        /** Assert total layer count */
        fun totalLayers(exactly: Int? = null, atLeast: Int? = null, atMost: Int? = null) {
            exactly?.let { if (layers.size != it)  recordFailure("[$docName] Expected exactly $it layers, got ${layers.size}") }
            atLeast?.let { if (layers.size < it)   recordFailure("[$docName] Expected at least $it layers, got ${layers.size}") }
            atMost?.let  { if (layers.size > it)   recordFailure("[$docName] Expected at most $it layers, got ${layers.size}") }
        }

        /**
         * Assert that the PRIMARY layer at [idx] is a [CanvasLayer.GradientShape] and run assertions.
         * Use this for root button elements — the engine always emits GradientShape for the primary surface.
         */
        fun primaryLayer(idx: Int = 0, block: GradientLayerContext.() -> Unit = {}) {
            if (idx >= layers.size) { recordFailure("[$docName] Layer[$idx] out of bounds (total: ${layers.size})"); return }
            val layer = layers[idx]
            if (layer !is CanvasLayer.GradientShape) {
                recordFailure("[$docName] Layer[$idx] expected GradientShape (primary), got ${layer::class.simpleName}")
                return
            }
            GradientLayerContext(layer, idx, docName, bW, bH, failures).block()
        }

        /**
         * Assert that a CHILD BoxLayer exists at [idx] and run assertions.
         * Use this for pseudo-elements and nested child divs.
         */
        fun boxLayer(idx: Int, block: BoxLayerContext.() -> Unit = {}) {
            if (idx >= layers.size) { recordFailure("[$docName] Layer[$idx] out of bounds (total: ${layers.size})"); return }
            val layer = layers[idx]
            if (layer !is CanvasLayer.BoxLayer) {
                recordFailure("[$docName] Layer[$idx] expected BoxLayer, got ${layer::class.simpleName}")
                return
            }
            BoxLayerContext(layer, idx, docName, bW, bH, failures).block()
        }

        /** Assert any typed layer exists at [idx] */
        inline fun <reified T : CanvasLayer> layerAt(idx: Int) {
            if (idx >= layers.size) {
                recordFailure("[$docName] Layer[$idx] out of bounds (total: ${layers.size})")
                return
            }
            if (layers[idx] !is T)
                recordFailure("[$docName] Layer[$idx] expected ${T::class.simpleName}, got ${layers[idx]::class.simpleName}")
        }

        /** Assert at least one layer of type T exists anywhere in the list */
        inline fun <reified T : CanvasLayer> hasLayer() {
            if (layers.none { it is T })
                recordFailure("[$docName] Expected at least one ${T::class.simpleName}")
        }

        /** Assert no layer of type T exists */
        inline fun <reified T : CanvasLayer> noLayer() {
            if (layers.any { it is T })
                recordFailure("[$docName] Expected no ${T::class.simpleName} but found one")
        }

        /** Assert at least [count] layers of type T exist */
        inline fun <reified T : CanvasLayer> layerCount(atLeast: Int) {
            val n = layers.count { it is T }
            if (n < atLeast)
                recordFailure("[$docName] Expected at least $atLeast ${T::class.simpleName}, found $n")
        }

        /** Shorthand: primary GradientShape is OVAL */
        fun primaryIsOval(idx: Int = 0) = primaryLayer(idx) { shapeOval() }

        /** Shorthand: primary GradientShape is ROUNDED_RECT */
        fun primaryIsRoundedRect(idx: Int = 0) = primaryLayer(idx) { shapeRoundedRect() }

        /** Shorthand: BoxLayer at [idx] is OVAL */
        fun boxLayerIsOval(idx: Int) = boxLayer(idx) { shapeOval() }

        /** Shorthand: BoxLayer at [idx] is ROUNDED_RECT */
        fun boxLayerIsRoundedRect(idx: Int) = boxLayer(idx) { shapeRoundedRect() }

        fun assertNoPendingFailures() {
            if (failures.isNotEmpty()) {
                kotlin.test.fail(
                    "Layer audit failed for '$docName':\n  ${failures.joinToString("\n  ")}"
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GradientShape Context (primary layer)
    // ─────────────────────────────────────────────────────────────────────────

    class GradientLayerContext(
        val layer: CanvasLayer.GradientShape,
        val idx: Int,
        val docName: String,
        val bW: Float,
        val bH: Float,
        val failures: MutableList<String>
    ) {
        private fun near(a: Float, b: Float, tol: Float) = abs(a - b) <= tol
        private fun fail(msg: String) { failures.add("[$docName][primary layer $idx] $msg") }

        fun ratio(
            rX: Float? = null, rY: Float? = null,
            rW: Float? = null, rH: Float? = null,
            tol: Float = 0.025f
        ) {
            val t = layer.effectiveTransform
            rX?.let { if (!near(t.offsetXRatio, it, tol)) fail("offsetXRatio=${t.offsetXRatio}, expected $it ±$tol") }
            rY?.let { if (!near(t.offsetYRatio, it, tol)) fail("offsetYRatio=${t.offsetYRatio}, expected $it ±$tol") }
            rW?.let { if (!near(layer.widthRatio,  it, tol)) fail("widthRatio=${layer.widthRatio}, expected $it ±$tol") }
            rH?.let { if (!near(layer.heightRatio, it, tol)) fail("heightRatio=${layer.heightRatio}, expected $it ±$tol") }
        }

        fun px(
            left: Float? = null, top: Float? = null,
            width: Float? = null, height: Float? = null,
            bW: Float = this.bW, bH: Float = this.bH,
            tolPx: Float = 3f
        ) {
            val t = layer.effectiveTransform
            left?.let {
                if (!near(t.offsetXRatio, it / bW, tolPx / bW))
                    fail("left=${t.offsetXRatio * bW}px, expected ${it}px ±${tolPx}px")
            }
            top?.let {
                if (!near(t.offsetYRatio, it / bH, tolPx / bH))
                    fail("top=${t.offsetYRatio * bH}px, expected ${it}px ±${tolPx}px")
            }
            width?.let {
                if (!near(layer.widthRatio, it / bW, tolPx / bW))
                    fail("width=${layer.widthRatio * bW}px, expected ${it}px ±${tolPx}px")
            }
            height?.let {
                if (!near(layer.heightRatio, it / bH, tolPx / bH))
                    fail("height=${layer.heightRatio * bH}px, expected ${it}px ±${tolPx}px")
            }
        }

        fun shapeOval() {
            if (layer.shapeType != "OVAL")
                fail("shapeType=${layer.shapeType}, expected OVAL")
        }

        fun shapeRoundedRect() {
            if (layer.shapeType != "ROUNDED_RECT")
                fail("shapeType=${layer.shapeType}, expected ROUNDED_RECT")
        }

        fun shapePolygon(sides: Int = 0) {
            val ok = layer.shapeType == "POLYGON" || layer.shapeType == "OCTAGON" || layer.shapeType == "HEXAGON"
            if (!ok) fail("shapeType=${layer.shapeType}, expected polygon type")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BoxLayer Context (child layers: pseudo-elements, nested divs)
    // ─────────────────────────────────────────────────────────────────────────

    class BoxLayerContext(
        val layer: CanvasLayer.BoxLayer,
        val idx: Int,
        val docName: String,
        val bW: Float,
        val bH: Float,
        val failures: MutableList<String>
    ) {
        private fun near(a: Float, b: Float, tol: Float) = abs(a - b) <= tol
        private fun fail(msg: String) { failures.add("[$docName][layer $idx] $msg") }

        fun ratio(
            rX: Float? = null, rY: Float? = null,
            rW: Float? = null, rH: Float? = null,
            tol: Float = 0.025f
        ) {
            val t = layer.effectiveTransform
            rX?.let { if (!near(t.offsetXRatio, it, tol)) fail("offsetXRatio=${t.offsetXRatio} (${t.offsetXRatio * bW}px), expected $it (${it * bW}px) ±$tol") }
            rY?.let { if (!near(t.offsetYRatio, it, tol)) fail("offsetYRatio=${t.offsetYRatio} (${t.offsetYRatio * bH}px), expected $it (${it * bH}px) ±$tol") }
            rW?.let { if (!near(layer.widthRatio,  it, tol)) fail("widthRatio=${layer.widthRatio} (${layer.widthRatio * bW}px), expected $it (${it * bW}px) ±$tol") }
            rH?.let { if (!near(layer.heightRatio, it, tol)) fail("heightRatio=${layer.heightRatio} (${layer.heightRatio * bH}px), expected $it (${it * bH}px) ±$tol") }
        }

        fun px(
            left: Float? = null, top: Float? = null,
            width: Float? = null, height: Float? = null,
            bW: Float = this.bW, bH: Float = this.bH,
            tolPx: Float = 3f
        ) {
            val t = layer.effectiveTransform
            left?.let {
                val rExp = it / bW; val rTol = tolPx / bW
                if (!near(t.offsetXRatio, rExp, rTol))
                    fail("left=${t.offsetXRatio * bW}px, expected ${it}px ±${tolPx}px")
            }
            top?.let {
                val rExp = it / bH; val rTol = tolPx / bH
                if (!near(t.offsetYRatio, rExp, rTol))
                    fail("top=${t.offsetYRatio * bH}px, expected ${it}px ±${tolPx}px")
            }
            width?.let {
                val rExp = it / bW; val rTol = tolPx / bW
                if (!near(layer.widthRatio, rExp, rTol))
                    fail("width=${layer.widthRatio * bW}px, expected ${it}px ±${tolPx}px")
            }
            height?.let {
                val rExp = it / bH; val rTol = tolPx / bH
                if (!near(layer.heightRatio, rExp, rTol))
                    fail("height=${layer.heightRatio * bH}px, expected ${it}px ±${tolPx}px")
            }
        }

        fun shapeOval() {
            if (layer.shapeType != "OVAL")
                fail("shapeType=${layer.shapeType}, expected OVAL")
        }

        fun shapeRoundedRect() {
            if (layer.shapeType != "ROUNDED_RECT")
                fail("shapeType=${layer.shapeType}, expected ROUNDED_RECT")
        }

        fun shapePolygon(sides: Int = 0) {
            val ok = layer.shapeType == "POLYGON" || layer.shapeType == "OCTAGON" || layer.shapeType == "HEXAGON"
            if (!ok) fail("shapeType=${layer.shapeType}, expected polygon type (POLYGON/OCTAGON/HEXAGON)")
            if (sides > 0 && layer.polygonSides != sides)
                fail("polygonSides=${layer.polygonSides}, expected $sides")
        }

        fun shapePath() {
            if (layer.shapeType != "PATH")
                fail("shapeType=${layer.shapeType}, expected PATH")
        }

        fun opacity(expected: Float, tol: Float = 0.05f) {
            if (!near(layer.effectiveEffects.opacity, expected, tol))
                fail("opacity=${layer.effectiveEffects.opacity}, expected $expected ±$tol")
        }
    }
}
