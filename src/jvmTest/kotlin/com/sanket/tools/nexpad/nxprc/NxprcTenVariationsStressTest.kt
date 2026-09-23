package com.sanket.tools.nexpad.nxprc

import com.sanket.tools.nexpad.nxprc.engine.compiler.NxprcCompiler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Autonomous 10-Variation Stress Test Suite for NEXPAD's NXPRC Compiler Engine.
 * Tests 10 radically different, highly complex architectural and visual design patterns.
 */
class NxprcTenVariationsStressTest {

    // =========================================================================
    // VARIATION 1: Multi-Transformed SVG Vector Emblem (Character / Insignia)
    // Tests SVG elements, paths, polygon vertices, multiple fill/stroke colors
    // =========================================================================
    @Test
    fun testVariation1_MultiTransformedSvgVectorEmblem() {
        val html = """
            <!DOCTYPE html>
            <html><head><style>
              :root { --spring-damping: 0.68; --spring-stiffness: 440; --press-scale: 0.92; }
              .nexpad-btn {
                position: relative;
                width: 96px;
                height: 96px;
                border-radius: 50%;
                background: radial-gradient(circle at 35% 25%, #2a3447 0%, #151a24 60%, #090c10 100%);
                box-shadow: 0 8px 24px rgba(0,0,0,0.65), inset 0 2px 4px rgba(255,255,255,0.3);
                display: flex; align-items: center; justify-content: center;
              }
              .btn-emblem {
                position: absolute;
                left: 18px; top: 18px;
                width: 60px; height: 60px;
              }
              .btn-label {
                position: relative; z-index: 5;
                font-size: 36px; font-weight: 900; color: #ffffff;
                text-shadow: 0 2px 4px rgba(0,0,0,0.8);
              }
              .nexpad-btn:active { transform: scale(0.93) translateY(3px); }
            </style></head><body>
              <button class="nexpad-btn" data-control="A" data-category="BUTTON" data-name="Action A">
                <svg class="btn-emblem" viewBox="0 0 100 100">
                  <polygon points="50,10 90,90 10,90" fill="#00f0ff" stroke="#005577" stroke-width="3" />
                  <circle cx="50" cy="60" r="16" fill="#ff0055" />
                  <path d="M 30,50 L 70,50 L 50,20 Z" fill="#ffff00" stroke="#000" stroke-width="2" />
                </svg>
                <span class="btn-label">A</span>
              </button>
            </body></html>
        """.trimIndent()

        val result = NxprcPackager.compileWithWarnings(html, "rc.v1_svg", "V1 SVG Emblem", "BUTTON", "A")
        assertNotNull(result.document)
        val layers = result.document.canvas.layers
        println("=== VARIATION 1: SVG Emblem Layers (${layers.size}) ===")
        layers.forEachIndexed { i, l -> println("  #$i [${l::class.simpleName}]: $l") }

        val vectorLayers = layers.filterIsInstance<CanvasLayer.VectorPath>()
        assertTrue(vectorLayers.size >= 3, "Must compile polygon, circle, and path into at least 3 VectorPaths")
        val glyph = layers.filterIsInstance<CanvasLayer.CenterGlyph>().firstOrNull()
        assertNotNull(glyph, "Must include center glyph A")
        assertEquals("A", glyph.text)
    }

    // =========================================================================
    // VARIATION 2: Cel-Shaded Anime / Hard-Stop Radial Gradients
    // Tests hard color stops, stacked multi-tier box shadows, soft blur highlights
    // =========================================================================
    @Test
    fun testVariation2_CelShadedAnimeHardStops() {
        val html = """
            <!DOCTYPE html>
            <html><head><style>
              :root { --spring-damping: 0.68; --spring-stiffness: 440; --press-scale: 0.92; }
              .nexpad-btn {
                position: relative;
                width: 96px; height: 96px;
                border-radius: 50%;
                background:
                  radial-gradient(circle at 30% 25%, #ffd699 0%, #ffd699 12%, transparent 13%),
                  radial-gradient(circle at 30% 25%, #ffb347 0%, #ffb347 30%, transparent 31%),
                  radial-gradient(circle at 70% 75%, #7a2e00 0%, #7a2e00 40%, transparent 41%),
                  radial-gradient(circle at 50% 50%, #ff8c00 0%, #ff8c00 60%, #cc5500 60%, #cc5500 100%);
                box-shadow:
                  0 0 0 4px #000,
                  0 0 0 7px #ffb347,
                  0 8px 0 0 #7a2e00,
                  0 12px 18px rgba(0,0,0,0.7),
                  inset 0 -6px 0 0 rgba(120,45,0,0.55),
                  inset 0 6px 0 0 rgba(255,220,150,0.45);
                display: flex; align-items: center; justify-content: center;
              }
              .nexpad-btn::before {
                content: "";
                position: absolute; left: 14px; top: 10px; width: 48px; height: 28px;
                border-radius: 50%;
                background: radial-gradient(ellipse at 50% 35%, rgba(255,255,255,0.85) 0%, transparent 70%);
                transform: rotate(-15deg);
                filter: blur(0.5px);
              }
              .btn-label {
                position: relative; z-index: 5;
                font-size: 42px; font-weight: 900; color: #fff;
                text-shadow: 0 0 4px #000, 0 3px 0 #7a2e00;
              }
              .nexpad-btn:active { transform: scale(0.93) translateY(3px); }
            </style></head><body>
              <button class="nexpad-btn" data-control="B" data-category="BUTTON" data-name="Action B">
                <span class="btn-label">B</span>
              </button>
            </body></html>
        """.trimIndent()

        val result = NxprcPackager.compileWithWarnings(html, "rc.v2_cel", "V2 Cel Shaded", "BUTTON", "B")
        assertNotNull(result.document)
        val layers = result.document.canvas.layers
        println("=== VARIATION 2: Cel Shaded Layers (${layers.size}) ===")
        layers.forEachIndexed { i, l -> println("  #$i [${l::class.simpleName}]: $l") }

        // Must parse hard stops and radial gradients
        assertTrue(layers.size >= 4, "Must compile multiple gradient shapes and bezel/socket")
        val glyph = layers.filterIsInstance<CanvasLayer.CenterGlyph>().firstOrNull()
        assertNotNull(glyph)
        assertEquals("B", glyph.text)
    }

    // =========================================================================
    // VARIATION 3: Non-Circular Faceted Silhouette (clip-path: polygon)
    // Tests 8-sided faceted octagon silhouette, asymmetric chamfers
    // =========================================================================
    @Test
    fun testVariation3_FacetedOctagonSilhouette() {
        val html = """
            <!DOCTYPE html>
            <html><head><style>
              :root { --spring-damping: 0.68; --spring-stiffness: 440; --press-scale: 0.92; }
              .nexpad-btn {
                position: relative;
                width: 96px; height: 96px;
                clip-path: polygon(25% 2%, 75% 2%, 98% 25%, 98% 75%, 75% 98%, 25% 98%, 2% 75%, 2% 25%);
                background: linear-gradient(145deg, #1e293b 0%, #0f172a 60%, #020617 100%);
                box-shadow: 0 8px 24px rgba(0,0,0,0.65);
                display: flex; align-items: center; justify-content: center;
              }
              .inner-core {
                position: absolute; left: 10px; top: 10px; width: 76px; height: 76px;
                clip-path: polygon(25% 2%, 75% 2%, 98% 25%, 98% 75%, 75% 98%, 25% 98%, 2% 75%, 2% 25%);
                background: radial-gradient(circle at 35% 35%, #0284c7 0%, #0369a1 60%, #0c4a6e 100%);
                border: 2px solid #38bdf8;
              }
              .btn-label {
                position: relative; z-index: 5;
                font-size: 38px; font-weight: 900; color: #ffffff;
                text-shadow: 0 2px 4px rgba(0,0,0,0.9);
              }
              .nexpad-btn:active { transform: scale(0.93) translateY(3px); }
            </style></head><body>
              <button class="nexpad-btn" data-control="X" data-category="BUTTON" data-name="Action X">
                <div class="inner-core"></div>
                <span class="btn-label">X</span>
              </button>
            </body></html>
        """.trimIndent()

        val result = NxprcPackager.compileWithWarnings(html, "rc.v3_octa", "V3 Octagon", "BUTTON", "X")
        assertNotNull(result.document)
        val layers = result.document.canvas.layers
        println("=== VARIATION 3: Octagon Layers (${layers.size}) ===")
        layers.forEachIndexed { i, l -> println("  #$i [${l::class.simpleName}]: $l") }

        // Must recognize polygon clip path
        val polyBoxes = layers.filterIsInstance<CanvasLayer.BoxLayer>().filter { it.polygonSides == 8 || it.pathData.isNotEmpty() }
        assertTrue(polyBoxes.isNotEmpty(), "Must preserve polygon clip-path geometry")
    }

    // =========================================================================
    // VARIATION 4: Complex Nested Math (calc() & aspect-ratio)
    // Tests dynamic calc() width/height math and aspect-ratio derivation
    // =========================================================================
    @Test
    fun testVariation4_ComplexNestedCalcAndAspectRatio() {
        val html = """
            <!DOCTYPE html>
            <html><head><style>
              :root { --spring-damping: 0.68; --spring-stiffness: 440; --press-scale: 0.92; }
              .nexpad-btn {
                position: relative;
                width: 96px; height: 96px;
                border-radius: 50%;
                background: #18181b;
                display: flex; align-items: center; justify-content: center;
              }
              .outer-inset-ring {
                position: absolute; left: 8px; top: 8px;
                width: calc(100% - 16px); height: calc(100% - 16px);
                border-radius: 50%;
                border: 2px solid #eab308;
              }
              .inner-proportional-core {
                position: absolute; left: 16px; top: 16px;
                width: calc(100% - 32px);
                aspect-ratio: 1;
                border-radius: 50%;
                background: radial-gradient(circle at 40% 30%, #ca8a04 0%, #713f12 100%);
              }
              .btn-label {
                position: relative; z-index: 5;
                font-size: 34px; font-weight: 900; color: #fef08a;
              }
              .nexpad-btn:active { transform: scale(0.93) translateY(3px); }
            </style></head><body>
              <button class="nexpad-btn" data-control="Y" data-category="BUTTON" data-name="Action Y">
                <div class="outer-inset-ring"></div>
                <div class="inner-proportional-core"></div>
                <span class="btn-label">Y</span>
              </button>
            </body></html>
        """.trimIndent()

        val result = NxprcPackager.compileWithWarnings(html, "rc.v4_calc", "V4 Calc", "BUTTON", "Y")
        assertNotNull(result.document)
        val layers = result.document.canvas.layers
        println("=== VARIATION 4: Calc Layers (${layers.size}) ===")
        layers.forEachIndexed { i, l -> println("  #$i [${l::class.simpleName}]: $l") }

        // Inset ring: 96 - 16 = 80px => ratio = 80 / 96 = ~0.8333f
        val ring = layers.filterIsInstance<CanvasLayer.BoxLayer>().firstOrNull { it.stroke != null }
        assertNotNull(ring, "Outer inset ring must be compiled with stroke")
        assertEquals(80f / 96f, ring.widthRatio, 0.02f, "Ring widthRatio must match calc(100% - 16px)")
        assertEquals(80f / 96f, ring.heightRatio, 0.02f, "Ring heightRatio must match calc(100% - 16px)")
    }

    // =========================================================================
    // VARIATION 5: Multi-Element Flexbox Utility Layout (System Menu / Dashboard)
    // Tests column flexbox with hamburger bars, gap, and zero text overlay
    // =========================================================================
    @Test
    fun testVariation5_FlexboxUtilityMenu() {
        val html = """
            <!DOCTYPE html>
            <html><head><style>
              :root { --spring-damping: 0.78; --spring-stiffness: 500; --press-scale: 0.92; }
              .system-btn {
                position: relative;
                width: 68px; height: 38px;
                border-radius: 12px;
                background: linear-gradient(180deg, #242933 0%, #13161c 100%);
                box-shadow: 0 4px 12px rgba(0,0,0,0.6), inset 0 1px 2px rgba(255,255,255,0.2);
                display: flex; flex-direction: column; align-items: center; justify-content: center;
                gap: 5px;
              }
              .burger-bar {
                width: 22px; height: 3px;
                border-radius: 1.5px;
                background: #e2e8f0;
              }
              .system-btn:active { transform: scale(0.92) translateY(2px); }
            </style></head><body>
              <button class="system-btn" data-control="MENU" data-category="SYSTEM" data-name="System MENU">
                <div class="burger-bar"></div>
                <div class="burger-bar"></div>
                <div class="burger-bar"></div>
              </button>
            </body></html>
        """.trimIndent()

        val result = NxprcPackager.compileWithWarnings(html, "rc.v5_menu", "V5 System Menu", "SYSTEM", "MENU")
        assertNotNull(result.document)
        val layers = result.document.canvas.layers
        println("=== VARIATION 5: System Menu Layers (${layers.size}) ===")
        layers.forEachIndexed { i, l -> println("  #$i [${l::class.simpleName}]: $l") }

        // Must NOT stamp automatic text "MENU" over hamburger bars
        val textLayers = layers.filterIsInstance<CanvasLayer.CenterGlyph>() + layers.filterIsInstance<CanvasLayer.TextLayer>()
        assertTrue(textLayers.isEmpty(), "System menu with burger bars must not have text stamped on it")

        // Must have 3 distinct hamburger bar layers
        val bars = layers.filterIsInstance<CanvasLayer.BoxLayer>().filter { it.heightRatio in 0.05f..0.15f }
        assertEquals(3, bars.size, "Must have exactly 3 hamburger bar layers")
    }

    // =========================================================================
    // VARIATION 6: Heavy Conic-Gradient Bezel & Optical Sheen (::before + ::after)
    // Tests conic-gradient with 8+ color stops and rotated ::after gloss arc
    // =========================================================================
    @Test
    fun testVariation6_ConicGradientBezelAndGlossArc() {
        val html = """
            <!DOCTYPE html>
            <html><head><style>
              :root { --spring-damping: 0.72; --spring-stiffness: 480; --press-scale: 0.94; }
              .dpad-btn {
                position: relative;
                width: 80px; height: 80px;
                border-radius: 50%;
                background: conic-gradient(from 45deg, #1f2937, #374151, #111827, #4b5563, #1f2937);
                box-shadow: 0 10px 24px rgba(0,0,0,0.65);
                display: flex; align-items: center; justify-content: center;
              }
              .dpad-btn::after {
                content: "";
                position: absolute; left: 10px; top: 6px; width: 60px; height: 30px;
                border-radius: 50%;
                background: radial-gradient(ellipse at 50% 25%, rgba(255,255,255,0.7) 0%, transparent 70%);
                transform: rotate(-12deg);
              }
              .dpad-arrow {
                font-size: 28px; color: #38bdf8;
                text-shadow: 0 0 8px rgba(56,189,248,0.8);
              }
              .dpad-btn:active { transform: scale(0.92) translateY(2px); }
            </style></head><body>
              <button class="dpad-btn" data-control="UP" data-category="DPAD" data-name="D-Pad UP">
                <span class="dpad-arrow">▲</span>
              </button>
            </body></html>
        """.trimIndent()

        val result = NxprcPackager.compileWithWarnings(html, "rc.v6_conic", "V6 Conic Dpad", "DPAD", "UP")
        assertNotNull(result.document)
        val layers = result.document.canvas.layers
        println("=== VARIATION 6: Conic Bezel Layers (${layers.size}) ===")
        layers.forEachIndexed { i, l -> println("  #$i [${l::class.simpleName}]: $l") }

        // Verify conic sweep gradient compiled
        val sweepLayer = layers.filterIsInstance<CanvasLayer.GradientShape>().firstOrNull { it.fill is FillBrush.SweepGradient }
        assertNotNull(sweepLayer, "Must compile conic-gradient into SweepGradient brush")
        val arrowGlyph = layers.filterIsInstance<CanvasLayer.CenterGlyph>().firstOrNull()
        assertNotNull(arrowGlyph, "Must include arrow glyph")
        assertEquals("▲", arrowGlyph.text)
    }

    // =========================================================================
    // VARIATION 7: Progressive Analog Trigger with Molded Grip Ribs
    // Tests linear gradient slope, 4 horizontal friction ribs, active pull displacement
    // =========================================================================
    @Test
    fun testVariation7_ProgressiveAnalogTrigger() {
        val html = """
            <!DOCTYPE html>
            <html><head><style>
              :root { --spring-damping: 0.65; --spring-stiffness: 380; --press-scale: 0.94; }
              .trigger-btn {
                position: relative;
                width: 90px; height: 130px;
                border-radius: 18px 18px 24px 24px;
                background: linear-gradient(180deg, #282e3d 0%, #151822 55%, #0a0c10 100%);
                box-shadow: 0 10px 24px rgba(0,0,0,0.65), inset 0 2px 4px rgba(255,255,255,0.3);
                display: flex; flex-direction: column; align-items: center; justify-content: flex-start;
                padding-top: 16px; gap: 8px;
              }
              .trigger-label {
                font-size: 28px; font-weight: 900; color: #ffffff;
                text-shadow: 0 2px 4px rgba(0,0,0,0.8);
              }
              .grip-rib {
                width: 54px; height: 4px;
                border-radius: 2px;
                background: rgba(255,255,255,0.15);
                box-shadow: 0 2px 4px rgba(0,0,0,0.6);
              }
              .trigger-btn:active { transform: scaleY(0.94) translateY(4px); }
            </style></head><body>
              <button class="trigger-btn" data-control="RT" data-category="TRIGGER" data-name="Trigger RT">
                <span class="trigger-label">RT</span>
                <div class="grip-rib"></div>
                <div class="grip-rib"></div>
                <div class="grip-rib"></div>
                <div class="grip-rib"></div>
              </button>
            </body></html>
        """.trimIndent()

        val result = NxprcPackager.compileWithWarnings(html, "rc.v7_trigger", "V7 Trigger", "TRIGGER", "RT")
        assertNotNull(result.document)
        val layers = result.document.canvas.layers
        println("=== VARIATION 7: Trigger Layers (${layers.size}) ===")
        layers.forEachIndexed { i, l -> println("  #$i [${l::class.simpleName}]: $l") }

        val glyph = layers.filterIsInstance<CanvasLayer.CenterGlyph>().firstOrNull()
        assertNotNull(glyph, "Must include trigger label RT")
        assertEquals("RT", glyph.text)

        // Must compile the 4 grip ribs
        val ribs = layers.filterIsInstance<CanvasLayer.BoxLayer>().filter { it.heightRatio in 0.02f..0.06f }
        assertTrue(ribs.size >= 4, "Must compile at least 4 grip ribs, found: ${ribs.size}")
    }

    // =========================================================================
    // VARIATION 8: Convex Shoulder Lever with Seam Highlights
    // Tests asymmetric curved rocker profile, microswitch click physics
    // =========================================================================
    @Test
    fun testVariation8_ConvexShoulderBumper() {
        val html = """
            <!DOCTYPE html>
            <html><head><style>
              :root { --spring-damping: 0.75; --spring-stiffness: 520; --press-scale: 0.96; }
              .bumper-btn {
                position: relative;
                width: 120px; height: 50px;
                border-radius: 12px 28px 8px 8px;
                background: linear-gradient(180deg, #2c3342 0%, #171a23 60%, #0c0e13 100%);
                box-shadow: 0 8px 20px rgba(0,0,0,0.6), inset 0 2px 4px rgba(255,255,255,0.35);
                display: flex; align-items: center; justify-content: center;
              }
              .bumper-btn::after {
                content: "";
                position: absolute; left: 14px; top: 4px; width: 92px; height: 18px;
                border-radius: 50%;
                background: radial-gradient(ellipse at 50% 30%, rgba(255,255,255,0.45) 0%, transparent 75%);
              }
              .bumper-label {
                font-size: 24px; font-weight: 900; color: #f1f5f9;
                text-shadow: 0 2px 4px rgba(0,0,0,0.9);
              }
              .bumper-btn:active { transform: scale(0.96) translateY(2px); }
            </style></head><body>
              <button class="bumper-btn" data-control="LB" data-category="BUMPER" data-name="Bumper LB">
                <span class="bumper-label">LB</span>
              </button>
            </body></html>
        """.trimIndent()

        val result = NxprcPackager.compileWithWarnings(html, "rc.v8_bumper", "V8 Bumper", "BUMPER", "LB")
        assertNotNull(result.document)
        val layers = result.document.canvas.layers
        println("=== VARIATION 8: Bumper Layers (${layers.size}) ===")
        layers.forEachIndexed { i, l -> println("  #$i [${l::class.simpleName}]: $l") }

        val glyph = layers.filterIsInstance<CanvasLayer.CenterGlyph>().firstOrNull()
        assertNotNull(glyph, "Must include bumper label LB")
        assertEquals("LB", glyph.text)
    }

    // =========================================================================
    // VARIATION 9: Two-Zone Analog Thumbstick with Knurled Rings & Sparkles
    // Tests stationary base vs movable thumb cap layer separation
    // =========================================================================
    @Test
    fun testVariation9_TwoZoneThumbstickSeparation() {
        val html = """
            <!DOCTYPE html>
            <html><head><style>
              :root { --spring-damping: 0.70; --spring-stiffness: 420; --press-scale: 0.92; }
              .stick-btn {
                position: relative;
                width: 130px; height: 130px;
                background: transparent; border: none; padding: 0;
              }
              .stick-base {
                position: absolute; left: 0; top: 0; width: 130px; height: 130px;
                border-radius: 50%;
                background: radial-gradient(circle at 45% 40%, #2b313d 0%, #14171e 65%, #08090c 100%);
                box-shadow: 0 12px 28px rgba(0,0,0,0.7), inset 0 -8px 16px rgba(0,0,0,0.85);
              }
              .tick-n { position: absolute; left: 63px; top: 6px; width: 4px; height: 10px; background: #fff; }
              .tick-s { position: absolute; left: 63px; top: 114px; width: 4px; height: 10px; background: #fff; }
              .stick-cap {
                position: absolute; left: 25px; top: 25px; width: 80px; height: 80px;
                border-radius: 50%;
                background: radial-gradient(circle at 50% 50%, #1a1e26 0%, #0d0f14 100%);
                box-shadow: inset 0 0 10px rgba(0,0,0,0.9), 0 0 0 2px rgba(255,255,255,0.12);
                display: flex; align-items: center; justify-content: center;
              }
              .knurled-ring {
                position: absolute; left: 12px; top: 12px; width: 56px; height: 56px;
                border-radius: 50%;
                border: 2px dashed rgba(255,255,255,0.35);
              }
              .stick-label {
                position: relative; z-index: 5;
                font-size: 20px; font-weight: 900; color: #fff;
              }
              .stick-btn:active .stick-cap { transform: scale(0.92); }
            </style></head><body>
              <button class="stick-btn" data-control="LS" data-category="JOYSTICK" data-name="Analog Stick LS">
                <div class="stick-base">
                  <div class="tick-n"></div>
                  <div class="tick-s"></div>
                </div>
                <div class="stick-cap">
                  <div class="knurled-ring"></div>
                  <span class="stick-label">LS</span>
                </div>
              </button>
            </body></html>
        """.trimIndent()

        val result = NxprcPackager.compileWithWarnings(html, "rc.v9_stick", "V9 Thumbstick", "JOYSTICK", "LS")
        assertNotNull(result.document)
        val doc = result.document
        val capIndices = doc.canvas.capLayerIndices
        println("=== VARIATION 9: Thumbstick Cap Indices: $capIndices ===")
        doc.canvas.layers.forEachIndexed { i, l ->
            val isCap = i in capIndices
            println("  #$i [${l::class.simpleName}] isCap=$isCap: $l")
        }

        assertTrue(capIndices.isNotEmpty(), "Thumb cap must have movable layers")
        assertFalse(0 in capIndices, "Base layer 0 must remain stationary")
    }

    // =========================================================================
    // VARIATION 10: Multi-Stop Typographic Extrusions & Dual Labels
    // Tests 4-tier extruded text shadows, multi-line wrap, and translation
    // =========================================================================
    @Test
    fun testVariation10_MultiStopTypographicExtrusions() {
        val html = """
            <!DOCTYPE html>
            <html><head><style>
              :root { --spring-damping: 0.68; --spring-stiffness: 440; --press-scale: 0.92; }
              .nexpad-btn {
                position: relative;
                width: 96px; height: 96px;
                border-radius: 50%;
                background: radial-gradient(circle at 35% 25%, #10b981 0%, #059669 50%, #047857 100%);
                box-shadow: 0 8px 24px rgba(0,0,0,0.65), inset 0 2px 4px rgba(255,255,255,0.4);
                display: flex; align-items: center; justify-content: center;
              }
              .btn-label {
                font-size: 38px; font-weight: 900; color: #ffffff;
                text-shadow:
                  0 1px 0 rgba(255,255,255,0.8),
                  0 -1px 0 rgba(0,0,0,0.9),
                  0 3px 6px rgba(0,0,0,0.75),
                  0 0 12px rgba(16,185,129,0.9);
              }
              .nexpad-btn:active { transform: scale(0.93) translateY(3px); }
            </style></head><body>
              <button class="nexpad-btn" data-control="A" data-category="BUTTON" data-name="Action A">
                <span class="btn-label">A</span>
              </button>
            </body></html>
        """.trimIndent()

        val result = NxprcPackager.compileWithWarnings(html, "rc.v10_typo", "V10 Typography", "BUTTON", "A")
        assertNotNull(result.document)
        val layers = result.document.canvas.layers
        println("=== VARIATION 10: Typographic Layers (${layers.size}) ===")
        layers.forEachIndexed { i, l -> println("  #$i [${l::class.simpleName}]: $l") }

        val glyph = layers.filterIsInstance<CanvasLayer.CenterGlyph>().firstOrNull()
        assertNotNull(glyph, "Must include center glyph")
        assertEquals("A", glyph.text)
        assertTrue(glyph.textShadows.size >= 3, "Must preserve multi-tier 3D extruded text shadows, found: ${glyph.textShadows.size}")
    }

    // =========================================================================
    // PARITY TEST: Naruto Nine-Tails SVG Group Transforms
    // Tests <g transform="translate(50,50) rotate(...)"> evaluation and baking
    // =========================================================================
    @Test
    fun testNarutoNineTailsSvgTransformParity() {
        val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
            <meta charset="UTF-8">
            <style>
              .nexpad-btn {
                position: relative;
                width: 96px;
                height: 96px;
                border-radius: 50%;
                background: radial-gradient(circle at 30% 25%, #ffb347 0%, #ff8c00 45%, #e55300 70%, #991b00 95%, #4a0000 100%);
                display: flex;
                align-items: center;
                justify-content: center;
              }
              .emblem-svg {
                position: absolute;
                width: 72px;
                height: 72px;
              }
              .btn-label {
                position: relative;
                z-index: 10;
                font-size: 26px;
                font-weight: 900;
                color: #ffffff;
              }
            </style>
            </head>
            <body>
            <button class="nexpad-btn">
              <svg class="emblem-svg" viewBox="0 0 100 100">
                <defs>
                  <linearGradient id="tailGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#fff0aa"/>
                    <stop offset="40%" stop-color="#ff7700"/>
                    <stop offset="100%" stop-color="#880000"/>
                  </linearGradient>
                </defs>
                <g transform="translate(50,50) rotate(0)">
                  <path d="M 0 0 C -4 -18, -12 -30, 0 -44 C 12 -30, 4 -18, 0 0 Z" fill="url(#tailGrad)" stroke="#1a0000" stroke-width="1.2"/>
                </g>
                <g transform="translate(50,50) rotate(40)">
                  <path d="M 0 0 C -4 -18, -12 -30, 0 -44 C 12 -30, 4 -18, 0 0 Z" fill="url(#tailGrad)" stroke="#1a0000" stroke-width="1.2"/>
                </g>
                <g transform="translate(50,50) rotate(80)">
                  <path d="M 0 0 C -4 -18, -12 -30, 0 -44 C 12 -30, 4 -18, 0 0 Z" fill="url(#tailGrad)" stroke="#1a0000" stroke-width="1.2"/>
                </g>
                <g transform="translate(50,50) rotate(120)">
                  <path d="M 0 0 C -4 -18, -12 -30, 0 -44 C 12 -30, 4 -18, 0 0 Z" fill="url(#tailGrad)" stroke="#1a0000" stroke-width="1.2"/>
                </g>
                <g transform="translate(50,50) rotate(160)">
                  <path d="M 0 0 C -4 -18, -12 -30, 0 -44 C 12 -30, 4 -18, 0 0 Z" fill="url(#tailGrad)" stroke="#1a0000" stroke-width="1.2"/>
                </g>
                <g transform="translate(50,50) rotate(200)">
                  <path d="M 0 0 C -4 -18, -12 -30, 0 -44 C 12 -30, 4 -18, 0 0 Z" fill="url(#tailGrad)" stroke="#1a0000" stroke-width="1.2"/>
                </g>
                <g transform="translate(50,50) rotate(240)">
                  <path d="M 0 0 C -4 -18, -12 -30, 0 -44 C 12 -30, 4 -18, 0 0 Z" fill="url(#tailGrad)" stroke="#1a0000" stroke-width="1.2"/>
                </g>
                <g transform="translate(50,50) rotate(280)">
                  <path d="M 0 0 C -4 -18, -12 -30, 0 -44 C 12 -30, 4 -18, 0 0 Z" fill="url(#tailGrad)" stroke="#1a0000" stroke-width="1.2"/>
                </g>
                <g transform="translate(50,50) rotate(320)">
                  <path d="M 0 0 C -4 -18, -12 -30, 0 -44 C 12 -30, 4 -18, 0 0 Z" fill="url(#tailGrad)" stroke="#1a0000" stroke-width="1.2"/>
                </g>
                <circle cx="50" cy="50" r="18" fill="#1a0000" stroke="#ff4500" stroke-width="1.5"/>
                <circle cx="50" cy="50" r="13" fill="#ff7700" stroke="#000000" stroke-width="1"/>
                <circle cx="50" cy="50" r="7" fill="#ffe066"/>
              </svg>
              <span class="btn-label">A</span>
            </button>
            </body>
            </html>
        """.trimIndent()

        val result = NxprcPackager.compileWithWarnings(html, "rc.naruto_fox", "Naruto Fox", "BUTTON", "A")
        assertNotNull(result.document)
        val vectorLayers = result.document.canvas.layers.filterIsInstance<CanvasLayer.VectorPath>()
        println("=== NARUTO FOX TEST: VectorPath layers count = ${vectorLayers.size} ===")
        vectorLayers.forEachIndexed { i, vl ->
            println("  #$i path: ${vl.pathData.take(50)}... strokeWidth=${vl.stroke?.width}")
        }

        assertEquals(12, vectorLayers.size, "Must compile exactly 9 tails + 3 circles into 12 vector path layers")

        val tailPaths = vectorLayers.take(9).map { it.pathData }
        val uniqueTailPaths = tailPaths.toSet()
        assertEquals(9, uniqueTailPaths.size, "All 9 tails must have distinct baked coordinates from their rotation transforms")

        assertTrue(tailPaths[0].contains("M 50 50"), "Tail 0 must start at (50, 50)")
    }
}
