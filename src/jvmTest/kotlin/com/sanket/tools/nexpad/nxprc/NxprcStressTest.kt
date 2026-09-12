package com.sanket.tools.nexpad.nxprc

import com.sanket.tools.nexpad.nxprc.engine.compiler.NxprcCompiler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NxprcStressTest {

    @Test
    fun testExtremeCase1_CyberpunkQuantumCore() {
        // Multi-layer texture stack (radial + conic + linear + solid), multi-box-shadows (inset + outset), glassmorphism filter, pseudo-elements
        val html = """
            <style>
                .quantum-core {
                    width: 120px;
                    height: 120px;
                    border-radius: 28px 12px 36px 16px;
                    background: radial-gradient(circle at 30% 25%, rgba(255, 0, 128, 0.8) 0%, transparent 45%),
                                conic-gradient(from 45deg at 50% 50%, #00f0ff 0deg, #ff0055 90deg, #7928ca 180deg, #00f0ff 360deg),
                                linear-gradient(135deg, rgba(255, 255, 255, 0.25) 0%, rgba(0, 0, 0, 0.7) 100%),
                                #0a0a14;
                    border: 3px solid rgba(0, 240, 255, 0.8);
                    box-shadow: 0 0 20px rgba(0, 240, 255, 0.6),
                                0 10px 30px rgba(121, 40, 202, 0.5),
                                inset 0 2px 8px rgba(255, 255, 255, 0.3),
                                inset 0 -4px 12px rgba(0, 0, 0, 0.8);
                    filter: blur(2px) brightness(1.2) contrast(1.1) saturate(1.4);
                    transform: rotate(5deg) scale(0.95);
                    overflow: hidden;
                }
                .quantum-core::before {
                    content: "";
                    position: absolute;
                    inset: 6px;
                    border-radius: 22px 8px 30px 12px;
                    background: linear-gradient(to right, transparent 0%, rgba(0, 240, 255, 0.4) 50%, transparent 100%);
                    box-shadow: inset 0 0 15px rgba(0, 240, 255, 0.5);
                    opacity: 0.85;
                }
                .quantum-core::after {
                    content: "";
                    position: absolute;
                    top: 10%;
                    left: 10%;
                    width: 80%;
                    height: 80%;
                    border-radius: 50%;
                    border: 2px dashed rgba(255, 0, 128, 0.7);
                    transform: rotate(-15deg);
                }
                .quantum-core:active {
                    transform: scale(0.88);
                    filter: brightness(1.5) saturate(1.8);
                }
            </style>
            <button class="quantum-core">CORE</button>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.quantum_core", name = "Quantum Core")
        assertNotNull(doc)
        println("=== TEST 1: Cyberpunk Quantum Core ===")
        println("Layers count: ${doc.canvas.layers.size}")
        for ((idx, layer) in doc.canvas.layers.withIndex()) {
            println("  Layer $idx: ${layer::class.simpleName} shape=${if (layer is CanvasLayer.BoxLayer) layer.shapeType else "N/A"}")
            if (layer is CanvasLayer.BoxLayer) {
                println("    Fills: ${layer.fills.size} (primary: ${layer.fill::class.simpleName})")
                println("    Shadows: ${layer.boxShadows.size} (insets: ${layer.boxShadows.count { it.isInset }})")
                println("    Filter: blur=${layer.filter.blurRadius}, bright=${layer.filter.brightness}")
            }
        }
        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes")
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.canvas.layers.size, decoded.canvas.layers.size)
    }

    @Test
    fun testExtremeCase2_MechaPolyhedralTrigger() {
        // Complex non-convex 8-point polygon clip-path, nested SVG paths, viewBox scaling, flex alignment
        val html = """
            <style>
                .mecha-trigger {
                    width: 140px;
                    height: 90px;
                    clip-path: polygon(15% 0%, 85% 0%, 100% 25%, 100% 75%, 85% 100%, 15% 100%, 0% 75%, 0% 25%);
                    background: linear-gradient(180deg, #2b303c 0%, #15181f 100%);
                    border: 2px solid #ffaa00;
                    box-shadow: 0 6px 18px rgba(0, 0, 0, 0.7);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                .mecha-icon {
                    width: 48px;
                    height: 48px;
                }
                .trigger-label {
                    font-size: 16px;
                    font-weight: bold;
                    color: #ffaa00;
                    text-shadow: 0 0 8px rgba(255, 170, 0, 0.6);
                }
            </style>
            <button class="mecha-trigger">
                <svg class="mecha-icon" viewBox="0 0 100 100">
                    <polygon points="50,5 95,30 95,70 50,95 5,70 5,30" fill="none" stroke="#ffaa00" stroke-width="4" />
                    <path d="M 30,50 L 50,30 L 70,50 L 50,70 Z" fill="#ffaa00" />
                </svg>
                <span class="trigger-label">RT-01</span>
            </button>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.mecha_trigger", name = "Mecha Trigger")
        assertNotNull(doc)
        println("=== TEST 2: Mecha Polyhedral Trigger ===")
        println("Layers count: ${doc.canvas.layers.size}")
        val polyLayer = doc.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().firstOrNull { it.shapeType == "POLYGON" || it.polygonSides > 0 }
        println("Found polygon clipped layer: ${polyLayer != null}, sides=${polyLayer?.polygonSides}")
        val svgLayers = doc.canvas.layers.filterIsInstance<CanvasLayer.VectorPath>()
        println("Found VectorPath layers: ${svgLayers.size}")
        val textLayers = doc.canvas.layers.filterIsInstance<CanvasLayer.TextLayer>()
        println("Found TextLayers: ${textLayers.size} text=${textLayers.map { it.text }}")
        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes")
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.canvas.layers.size, decoded.canvas.layers.size)
    }

    @Test
    fun testExtremeCase3_CosmicHyperdriveAnimator() {
        // Multi-track, non-linear staggered animation steps, SVG synchronized animations, 6 keyframe steps
        val html = """
            <style>
                @keyframes hyperdrive {
                    0%   { transform: scale(1.0, 1.0) rotate(0deg) translate(0px, 0px); opacity: 1.0; filter: hue-rotate(0deg); }
                    20%  { transform: scale(1.25, 0.85) rotate(45deg) translate(-5px, 2px); opacity: 0.85; filter: hue-rotate(60deg); }
                    45%  { transform: scale(0.9, 1.3) rotate(180deg) translate(8px, -4px); opacity: 0.6; filter: hue-rotate(150deg); }
                    70%  { transform: scale(1.15, 0.95) rotate(270deg) translate(-3px, 6px); opacity: 0.9; filter: hue-rotate(240deg); }
                    85%  { transform: scale(0.98, 1.05) rotate(330deg) translate(2px, -1px); opacity: 0.95; filter: hue-rotate(310deg); }
                    100% { transform: scale(1.0, 1.0) rotate(360deg) translate(0px, 0px); opacity: 1.0; filter: hue-rotate(360deg); }
                }
                .hyperdrive-btn {
                    width: 100px;
                    height: 100px;
                    border-radius: 50%;
                    background: radial-gradient(circle at 50% 50%, #ffffff 0%, #00f0ff 40%, #0a0020 100%);
                    animation: hyperdrive 3s cubic-bezier(0.68, -0.55, 0.265, 1.55) infinite;
                }
            </style>
            <button class="hyperdrive-btn">
                <svg viewBox="0 0 50 50">
                    <circle cx="25" cy="25" r="20" stroke="#00f0ff" stroke-width="2" fill="none">
                        <animateTransform attributeName="transform" type="rotate" from="0 25 25" to="360 25 25" dur="1.5s" repeatCount="indefinite" />
                    </circle>
                </svg>
            </button>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.hyperdrive", name = "Hyperdrive")
        assertNotNull(doc)
        println("=== TEST 3: Cosmic Hyperdrive Animator ===")
        println("Animations idleType: ${doc.animations.idleType}")
        println("Animation tracks count: ${doc.animations.tracks.size}")
        for (track in doc.animations.tracks) {
            println("  Track: ${track.property} dur=${track.durationMs}ms easing=${track.easing} keyframes=${track.keyframes.size}")
            println("    Points: ${track.keyframes.joinToString { "(${it.fraction.toString().take(4)} -> ${it.value.toString().take(5)})" }}")
        }
        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes")
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.animations.tracks.size, decoded.animations.tracks.size)
    }

    @Test
    fun testExtremeCase4_ModularRetroArcadeHud() {
        // Multi-child flexbox tree with header badge, center glyph, bottom ribbon, nested text layers
        val html = """
            <style>
                .arcade-card {
                    width: 160px;
                    height: 110px;
                    border-radius: 14px;
                    background: #111420;
                    border: 3px solid #ff0055;
                    box-shadow: 0 8px 24px rgba(255, 0, 85, 0.4);
                    display: flex;
                    flex-direction: column;
                    justify-content: space-between;
                    align-items: center;
                    padding: 8px;
                }
                .arcade-header {
                    width: 100%;
                    display: flex;
                    justify-content: space-between;
                }
                .badge-lvl {
                    font-size: 11px;
                    font-weight: bold;
                    color: #00f0ff;
                    background: #002535;
                    border-radius: 6px;
                    padding: 2px 6px;
                }
                .badge-status {
                    font-size: 11px;
                    color: #00ff88;
                }
                .arcade-center-glyph {
                    font-size: 28px;
                    font-weight: 900;
                    color: #ff0055;
                    text-shadow: 0 0 12px rgba(255, 0, 85, 0.8);
                }
                .arcade-footer {
                    font-size: 12px;
                    font-weight: bold;
                    color: #ffffff;
                    letter-spacing: 2px;
                }
            </style>
            <button class="arcade-card">
                <div class="arcade-header">
                    <span class="badge-lvl">LVL 99</span>
                    <span class="badge-status">ONLINE</span>
                </div>
                <div class="arcade-center-glyph">READY</div>
                <div class="arcade-footer">PRESS START</div>
            </button>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.arcade_hud", name = "Arcade HUD")
        assertNotNull(doc)
        println("=== TEST 4: Modular Retro Arcade HUD ===")
        println("Layers count: ${doc.canvas.layers.size}")
        val textLayers = doc.canvas.layers.filterIsInstance<CanvasLayer.TextLayer>()
        println("TextLayers count: ${textLayers.size}")
        for (tl in textLayers) {
            println("  TextLayer: '${tl.text}' size=${tl.fontSizeSp} color=#${tl.textColor.toString(16)} off=(${tl.offsetXRatio}, ${tl.offsetYRatio})")
        }
        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes")
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.canvas.layers.size, decoded.canvas.layers.size)
    }

    @Test
    fun testExtremeCase5_SyntaxAbuseAndDirtyCss() {
        // Weird colors (slash alpha, commas, mixed case hex, negative angle gradients, zero px units, comments)
        val html = """
            <style>
                /* Outer container comment */
                .dirty-btn {
                    width: 110px;
                    height: 110px;
                    border-radius: 50% 10px / 10px 50%;
                    background: linear-gradient(-45deg, rgba(255 120 40 / 0.8) 0%, hsla(280, 80%, 50%, 0.7) 60%, #1a1a2e 100%);
                    border: 4px solid #00FfA8;
                    box-shadow: inset 0 0 15px rgba(0 0 0 / 0.9), 0 10px 20px -5px #00FFA880;
                    margin: 0;
                    padding: 0px;
                    transform: skew(-5deg, 3deg) translate(-2px, 4px);
                    opacity: 0.92;
                }
                .dirty-btn:hover {
                    background: #fF00aA;
                }
            </style>
            <button class="dirty-btn">CHAOS</button>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.dirty_syntax", name = "Dirty Syntax")
        assertNotNull(doc)
        println("=== TEST 5: Syntax Abuse & Dirty CSS ===")
        println("Layers count: ${doc.canvas.layers.size}")
        val rootBox = doc.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().first()
        println("Root shape: ${rootBox.shapeType} radii=(${rootBox.cornerRadiusTopLeft}, ${rootBox.cornerRadiusTopRight}, ${rootBox.cornerRadiusBottomRight}, ${rootBox.cornerRadiusBottomLeft})")
        println("Fill brush: ${rootBox.fill::class.simpleName}")
        println("Box shadows: ${rootBox.boxShadows.size}")
        for (s in rootBox.boxShadows) {
            println("  Shadow: off=(${s.offsetX}, ${s.offsetY}) blur=${s.blurRadius} spread=${s.spreadRadius} inset=${s.isInset} color=#${s.color.toString(16)}")
        }
        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes")
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.canvas.layers.size, decoded.canvas.layers.size)
    }

    @Test
    fun testExtremeCase6_DeeplyNestedHierarchy() {
        // 4-level deep nested DOM structure with relative margins, nested flex, and varied z-indices
        val html = """
            <style>
                .outer {
                    width: 150px;
                    height: 150px;
                    background: #111;
                    padding: 10px;
                    border: 2px solid #333;
                }
                .layer-1 {
                    width: 100%;
                    height: 100%;
                    background: #222;
                    border-radius: 20px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                .layer-2 {
                    width: 80%;
                    height: 80%;
                    background: #333;
                    border-radius: 15px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                .layer-3 {
                    width: 60%;
                    height: 60%;
                    background: #ff0055;
                    border-radius: 10px;
                }
            </style>
            <div class="outer">
                <div class="layer-1">
                    <div class="layer-2">
                        <div class="layer-3">
                            <span style="color: #fff; font-size: 14px;">DEPTH</span>
                        </div>
                    </div>
                </div>
            </div>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.depth_test", name = "Depth Test")
        assertNotNull(doc)
        println("=== TEST 6: Deeply Nested Hierarchy ===")
        println("Layers count: ${doc.canvas.layers.size}")
        for ((idx, layer) in doc.canvas.layers.withIndex()) {
            println("  Layer $idx: ${layer::class.simpleName}")
            if (layer is CanvasLayer.BoxLayer) {
                println("    Box: wRatio=${layer.widthRatio} hRatio=${layer.heightRatio} fill=${layer.fill::class.simpleName}")
            } else if (layer is CanvasLayer.TextLayer) {
                println("    Text: '${layer.text}'")
            } else if (layer is CanvasLayer.CenterGlyph) {
                println("    CenterGlyph: '${layer.text}'")
            }
        }
        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes")
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.canvas.layers.size, decoded.canvas.layers.size)
    }

    @Test
    fun testExtremeCase7_StandaloneSvgComplexPaths() {
        // Pure SVG button with complex SVG path data, circle, polygon, and stroke styles
        val html = """
            <svg class="svg-gamepad" viewBox="0 0 120 120" width="120" height="120">
                <circle cx="60" cy="60" r="55" fill="#0d1117" stroke="#58a6ff" stroke-width="4" />
                <path d="M 40 60 L 55 45 L 55 55 L 80 55 L 80 65 L 55 65 L 55 75 Z" fill="#238636" stroke="#2ea043" stroke-width="1.5" />
                <polygon points="60,20 65,30 55,30" fill="#f85149" />
                <text x="60" y="95" fill="#ffffff" font-size="14" font-weight="bold" text-anchor="middle">CROSS</text>
            </svg>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.svg_btn", name = "SVG Button")
        assertNotNull(doc)
        println("=== TEST 7: Standalone Complex SVG ===")
        println("Layers count: ${doc.canvas.layers.size}")
        for ((idx, layer) in doc.canvas.layers.withIndex()) {
            println("  Layer $idx: ${layer::class.simpleName}")
            if (layer is CanvasLayer.VectorPath) {
                println("    VectorPath: d='${layer.pathData.take(30)}...' fill=${layer.fill::class.simpleName} strokeWidth=${layer.stroke?.width}")
            } else if (layer is CanvasLayer.CenterGlyph) {
                println("    CenterGlyph: '${layer.text}'")
            }
        }
        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes")
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.canvas.layers.size, decoded.canvas.layers.size)
    }

    @Test
    fun testExtremeCase8_HoloSciFiDpadCluster() {
        // Multi-directional cross cluster (Container with 4 directional buttons + Center)
        val html = """
            <style>
                .dpad-cluster {
                    width: 140px;
                    height: 140px;
                    background: #0b0e14;
                    border-radius: 50%;
                    border: 2px solid #00f0ff;
                    box-shadow: 0 0 20px rgba(0, 240, 255, 0.4);
                    position: relative;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                .dpad-btn {
                    position: absolute;
                    width: 36px;
                    height: 36px;
                    background: linear-gradient(135deg, #1b202e, #0e121a);
                    border: 1px solid #00f0ff80;
                    border-radius: 8px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: #00f0ff;
                    font-weight: bold;
                    font-size: 14px;
                }
                .btn-up    { top: 8px; left: 52px; clip-path: polygon(50% 0%, 0% 100%, 100% 100%); }
                .btn-down  { bottom: 8px; left: 52px; clip-path: polygon(0% 0%, 100% 0%, 50% 100%); }
                .btn-left  { top: 52px; left: 8px; clip-path: polygon(0% 50%, 100% 0%, 100% 100%); }
                .btn-right { top: 52px; right: 8px; clip-path: polygon(0% 0%, 100% 50%, 0% 100%); }
                .dpad-center {
                    width: 28px;
                    height: 28px;
                    background: radial-gradient(circle, #00f0ff 0%, #0b0e14 70%);
                    border-radius: 50%;
                }
            </style>
            <div class="dpad-cluster" data-primitive="box">
                <button class="dpad-btn btn-up">U</button>
                <button class="dpad-btn btn-down">D</button>
                <button class="dpad-btn btn-left">L</button>
                <button class="dpad-btn btn-right">R</button>
                <div class="dpad-center"></div>
            </div>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.dpad_cluster", name = "D-Pad Cluster")
        assertNotNull(doc)
        println("=== TEST 8: Holo Sci-Fi D-Pad Cluster ===")
        println("Layers count: ${doc.canvas.layers.size}")
        val boxLayers = doc.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>()
        println("BoxLayers count: ${boxLayers.size}")
        val polyCount = boxLayers.count { it.polygonSides > 0 || it.shapeType == "POLYGON" }
        println("Polygon directional buttons: $polyCount")
        val textLayers = doc.canvas.layers.filterIsInstance<CanvasLayer.TextLayer>()
        println("TextLayers: ${textLayers.map { it.text }}")
        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes")
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.canvas.layers.size, decoded.canvas.layers.size)
    }

    @Test
    fun testExtremeCase9_ComplexSvgCompoundMatrix() {
        // Intricate SVG badge: ellipse, rounded rect, compound bezier path, dashed line, polygon, text
        val html = """
            <svg class="svg-matrix-badge" viewBox="0 0 140 140" width="140" height="140">
                <ellipse cx="70" cy="70" rx="65" ry="50" fill="#0d1117" stroke="#388bfd" stroke-width="3" />
                <rect x="25" y="25" width="90" height="90" rx="16" ry="16" fill="none" stroke="#f78166" stroke-width="2" stroke-dasharray="8 4" />
                <path d="M 35 70 C 35 45, 55 35, 70 55 S 105 70, 105 45 Q 85 95, 70 85 T 35 70 Z" fill="#238636" stroke="#2ea043" stroke-width="2" />
                <line x1="15" y1="70" x2="125" y2="70" stroke="#00f0ff" stroke-width="1.5" stroke-dasharray="4 2" />
                <polygon points="70,18 80,32 60,32" fill="#d29922" stroke="#e3b341" stroke-width="1" />
                <text x="70" y="115" fill="#58a6ff" font-size="13" font-weight="900" text-anchor="middle">MATRIX</text>
            </svg>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.svg_matrix", name = "SVG Matrix")
        assertNotNull(doc)
        println("=== TEST 9: Complex SVG Compound Matrix ===")
        println("Layers count: ${doc.canvas.layers.size}")
        val vectorLayers = doc.canvas.layers.filterIsInstance<CanvasLayer.VectorPath>()
        println("VectorPath layers count: ${vectorLayers.size}")
        val dashedStrokes = vectorLayers.count { it.stroke?.isDashed == true }
        println("Dashed stroke layers: $dashedStrokes")
        val glyph = doc.canvas.layers.filterIsInstance<CanvasLayer.CenterGlyph>().firstOrNull()
        println("Found CenterGlyph: ${glyph?.text}")
        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes")
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.canvas.layers.size, decoded.canvas.layers.size)
    }

    @Test
    fun testExtremeCase10_MultiPhase10StepKeyframePulsar() {
        // High density 11-step keyframes: 0%, 10%, 20%, 30%, 40%, 50%, 60%, 70%, 80%, 90%, 100%
        val html = """
            <style>
                @keyframes quantum-pulsar {
                    0%   { transform: scale(1.0) rotate(0deg) translate(0px, 0px); opacity: 1.0; filter: hue-rotate(0deg); }
                    10%  { transform: scale(1.1) rotate(36deg) translate(2px, -1px); opacity: 0.95; filter: hue-rotate(36deg); }
                    20%  { transform: scale(0.95) rotate(72deg) translate(-2px, 3px); opacity: 0.85; filter: hue-rotate(72deg); }
                    30%  { transform: scale(1.2) rotate(108deg) translate(4px, -2px); opacity: 0.75; filter: hue-rotate(108deg); }
                    40%  { transform: scale(0.9) rotate(144deg) translate(-3px, 1px); opacity: 0.8; filter: hue-rotate(144deg); }
                    50%  { transform: scale(1.25) rotate(180deg) translate(0px, -4px); opacity: 0.65; filter: hue-rotate(180deg); }
                    60%  { transform: scale(0.92) rotate(216deg) translate(3px, 2px); opacity: 0.75; filter: hue-rotate(216deg); }
                    70%  { transform: scale(1.18) rotate(252deg) translate(-4px, -2px); opacity: 0.85; filter: hue-rotate(252deg); }
                    80%  { transform: scale(0.96) rotate(288deg) translate(2px, 3px); opacity: 0.9; filter: hue-rotate(288deg); }
                    90%  { transform: scale(1.08) rotate(324deg) translate(-1px, 1px); opacity: 0.95; filter: hue-rotate(324deg); }
                    100% { transform: scale(1.0) rotate(360deg) translate(0px, 0px); opacity: 1.0; filter: hue-rotate(360deg); }
                }
                .pulsar-btn {
                    width: 100px;
                    height: 100px;
                    border-radius: 50%;
                    background: radial-gradient(circle, #ff0055 0%, #7928ca 50%, #000 100%);
                    animation: quantum-pulsar 4s ease-in-out infinite;
                }
            </style>
            <button class="pulsar-btn">PULSE</button>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.quantum_pulsar", name = "Quantum Pulsar")
        assertNotNull(doc)
        println("=== TEST 10: Multi-Phase 10-Step Keyframe Pulsar ===")
        println("Tracks count: ${doc.animations.tracks.size}")
        for (track in doc.animations.tracks) {
            println("  Track: ${track.property} steps=${track.keyframes.size} easing=${track.easing}")
        }
        val scaleTrack = doc.animations.tracks.firstOrNull { it.property == com.sanket.tools.nexpad.nxprc.AnimatedProperty.SCALE || it.property == com.sanket.tools.nexpad.nxprc.AnimatedProperty.SCALE_X }
        assertNotNull(scaleTrack)
        assertEquals(11, scaleTrack.keyframes.size, "Should have exactly 11 keyframe points from 0% to 100%")
        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes")
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.animations.tracks.size, decoded.animations.tracks.size)
    }

    @Test
    fun testExtremeCase11_DynamicCssVariablesAndCascade() {
        // :root variables, fallback chaining, :active variable overrides, child inheritance
        val html = """
            <style>
                :root {
                    --theme-core: #0d1117;
                    --theme-accent: #00f0ff;
                    --theme-glow: rgba(0, 240, 255, 0.6);
                    --btn-radius: 18px;
                    --btn-border-w: 3px;
                }
                .themed-btn {
                    width: 130px;
                    height: 130px;
                    border-radius: var(--btn-radius);
                    background: var(--theme-core);
                    border: var(--btn-border-w) solid var(--theme-accent);
                    box-shadow: 0 0 16px var(--theme-glow), inset 0 0 12px var(--missing-var, var(--fallback-glow, #ff0055));
                    color: var(--theme-accent);
                    font-size: 18px;
                    font-weight: bold;
                }
                .themed-btn:active {
                    background: var(--theme-accent);
                    color: var(--theme-core);
                }
            </style>
            <button class="themed-btn">THEME</button>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.theme_vars", name = "Theme Vars")
        assertNotNull(doc)
        println("=== TEST 11: Dynamic CSS Variables & Cascade ===")
        println("Layers count: ${doc.canvas.layers.size}")
        val rootShape = doc.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().first()
        println("Resolved stroke color: #${rootShape.stroke?.color?.toString(16)}")
        println("Resolved radius: ${rootShape.cornerRadiusTopLeft}")
        assertEquals(18f, rootShape.cornerRadiusTopLeft)
        assertEquals(0xFF00F0FFL, rootShape.stroke?.color)
        val shadowColors = rootShape.boxShadows.map { "#${it.color.toString(16)}" }
        println("Resolved shadows: $shadowColors")
        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes")
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.canvas.layers.size, decoded.canvas.layers.size)
    }

    @Test
    fun testExtremeCase12_AsymmetricArmorPlateAndAngularConic() {
        // 10-point non-convex polygon clip-path, 8-stop conic gradient, layered shadows
        val html = """
            <style>
                .armor-plate {
                    width: 150px;
                    height: 100px;
                    clip-path: polygon(0% 25%, 25% 0%, 75% 0%, 100% 25%, 90% 75%, 100% 85%, 75% 100%, 25% 100%, 0% 85%, 10% 75%);
                    background: conic-gradient(from 0deg at 50% 50%, #ff0055 0deg, #ff7700 45deg, #ffff00 90deg, #00ff88 135deg, #00f0ff 180deg, #0066ff 225deg, #7928ca 270deg, #ff0055 360deg);
                    border: 3px solid #00f0ff;
                    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.8), inset 0 0 15px rgba(255, 255, 255, 0.4);
                }
            </style>
            <button class="armor-plate" data-primitive="box">ARMOR</button>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.armor_plate", name = "Armor Plate")
        assertNotNull(doc)
        println("=== TEST 12: Asymmetric Armor Plate & Angular Conic ===")
        println("Layers count: ${doc.canvas.layers.size}")
        val armorLayer = doc.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().first()
        println("Armor shape: ${armorLayer.shapeType} sides=${armorLayer.polygonSides}")
        assertEquals("POLYGON", armorLayer.shapeType)
        assertEquals(10, armorLayer.polygonSides)
        assertTrue(armorLayer.fill is FillBrush.SweepGradient)
        val sweep = armorLayer.fill as FillBrush.SweepGradient
        println("Sweep gradient stops: ${sweep.stops.size}")
        assertEquals(8, sweep.stops.size)
        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes")
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.canvas.layers.size, decoded.canvas.layers.size)
    }

    @Test
    fun testExtremeCase13_ModernColorSyntaxAndDirtyWhitespace() {
        // Modern CSS Color Module 4: hsl with deg, slash alpha in hsl/rgb, multiline comments, uppercase props
        val html = """
            <style>
                /* Multi-line CSS comment
                   Testing lexer comment stripping
                   across lines with special chars: @#${'$'}%^&* */
                .MODERN-SYNTAX {
                    WIDTH: 115px;
                    HEIGHT: 115px;
                    BORDER-RADIUS: 25px 12px 30px 10px;
                    BACKGROUND-COLOR: hsl(185deg 100% 50% / 80%);
                    BORDER: 3px solid rgb(255 0 128 / 75%);
                    BOX-SHADOW: 0 8px 20px rgba(0 255 168 / 50%), inset 0 0 12px hsla(300, 80%, 40%, 0.85);
                    TRANSFORM: rotate(-15deg) skew(-5deg, 3deg);
                    OPACITY: 0.95;
                }
            </style>
            <button class="MODERN-SYNTAX">NEO</button>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.modern_syntax", name = "Modern Syntax")
        assertNotNull(doc)
        println("=== TEST 13: Modern Color Syntax & Dirty Whitespace ===")
        println("Layers count: ${doc.canvas.layers.size}")
        val box = doc.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().first()
        println("Box fill: #${(box.fill as? FillBrush.Solid)?.color?.toString(16)}")
        println("Box stroke: #${box.stroke?.color?.toString(16)}")
        println("Box shadows count: ${box.boxShadows.size}")
        assertEquals(2, box.boxShadows.size)
        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes")
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.canvas.layers.size, decoded.canvas.layers.size)
    }

    @Test
    fun testExtremeCase14_MultiPrimitiveSvgFilterGraph() {
        // Multi-primitive SVG filter graph: feGaussianBlur + feColorMatrix (hueRotate) + feDropShadow + feBlend
        // combined with CSS filter: brightness() contrast()
        val html = """
            <svg style="display:none;">
                <filter id="holographic-dispersion" x="-20%" y="-20%" width="140%" height="140%">
                    <feGaussianBlur stdDeviation="7.5" result="blurOut"/>
                    <feColorMatrix type="hueRotate" values="135"/>
                    <feDropShadow dx="0" dy="8" stdDeviation="12" flood-color="#ff007f" flood-opacity="0.85"/>
                    <feBlend mode="screen" in="SourceGraphic" in2="blurOut"/>
                </filter>
            </svg>
            <style>
                .holo-core {
                    width: 110px;
                    height: 110px;
                    border-radius: 24px;
                    background: radial-gradient(circle at 40% 30%, #00f0ff 0%, #7928ca 70%, #050510 100%);
                    border: 2px solid rgba(0, 240, 255, 0.9);
                    box-shadow: 0 4px 15px rgba(0, 240, 255, 0.4), inset 0 0 20px rgba(121, 40, 202, 0.6);
                    filter: url(#holographic-dispersion) brightness(1.25) contrast(1.1);
                }
                .holo-core:active {
                    transform: scale(0.92);
                    filter: brightness(1.6) saturate(1.5);
                }
            </style>
            <button class="holo-core">HOLO</button>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.holo_core", name = "Holographic Core")
        assertNotNull(doc)
        println("=== TEST 14: Multi-Primitive SVG Filter Graph ===")
        println("Layers count: ${doc.canvas.layers.size}")
        val box = doc.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().first()
        println("Filter blur: ${box.filter.blurRadius} (expected 7.5)")
        println("Filter hueRotate: ${box.filter.hueRotateDegrees} (expected 135)")
        println("Filter brightness: ${box.filter.brightness} (expected 1.25)")
        assertEquals(7.5f, box.filter.blurRadius, 0.01f)
        assertEquals(135f, box.filter.hueRotateDegrees, 0.01f)
        assertEquals(1.25f, box.filter.brightness, 0.01f)

        // Verify SVG drop shadow was extracted into layer's boxShadows
        println("Box shadows: ${box.boxShadows.size}")
        val dropShadow = box.boxShadows.firstOrNull { it.offsetY == 8f && it.blurRadius == 12f }
        assertNotNull(dropShadow, "feDropShadow (dy=8, blur=12) must be in boxShadows")

        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes")
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.canvas.layers.size, decoded.canvas.layers.size)
        val decodedBox = decoded.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().first()
        assertEquals(7.5f, decodedBox.filter.blurRadius, 0.01f)
        assertEquals(135f, decodedBox.filter.hueRotateDegrees, 0.01f)
    }

    @Test
    fun testExtremeCase15_MultiLineWrappingInsideFlexGrid() {
        // Multi-line flex-wrap grid container with wrapped text buttons
        val html = """
            <style>
                .hud-grid {
                    display: flex;
                    flex-direction: row;
                    flex-wrap: wrap;
                    justify-content: space-between;
                    width: 160px;
                    height: 140px;
                    padding: 8px;
                    background: #080d16;
                    border-radius: 16px;
                }
                .grid-pill {
                    width: 68px;
                    height: 54px;
                    border-radius: 8px;
                    background: #111e30;
                    border: 1.5px solid #00f0ff;
                    font-size: 11px;
                    white-space: pre-line;
                }
            </style>
            <div class="hud-grid" data-primitive="box">
                <div class="grid-pill">TAC
ALPHA</div>
                <div class="grid-pill">HUD
BETA</div>
                <div class="grid-pill">FIRE
LOCK</div>
                <div class="grid-pill">SYS
READY</div>
            </div>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.hud_grid", name = "HUD Grid")
        assertNotNull(doc)
        println("=== TEST 15: Multi-Line Wrapping Inside Flex Grid ===")
        println("Layers count: ${doc.canvas.layers.size}")
        val boxes = doc.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>()
        val texts = doc.canvas.layers.filterIsInstance<CanvasLayer.TextLayer>()
        println("Box count: ${boxes.size}, Text layers count: ${texts.size}")

        // 1 container + 4 pill buttons = 5 boxes
        assertTrue(boxes.size >= 5, "Container + 4 buttons expected, got ${boxes.size}")
        // 4 buttons * 2 lines each = at least 8 text layers
        assertTrue(texts.size >= 8, "Expected at least 8 text layers for multi-line text, got ${texts.size}")

        // Check flex wrapping: row 1 pills vs row 2 pills
        val pill1 = boxes[1]
        val pill2 = boxes[2]
        val pill3 = boxes[3]
        val pill4 = boxes[4]

        // pill1 and pill2 on row 1
        assertEquals(pill1.offsetYRatio, pill2.offsetYRatio, 0.02f)
        // pill3 and pill4 on row 2 (wrapped)
        assertEquals(pill3.offsetYRatio, pill4.offsetYRatio, 0.02f)
        assertTrue(pill3.offsetYRatio > pill1.offsetYRatio, "Pill 3 must wrap to row 2 below Pill 1")

        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes")
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.canvas.layers.size, decoded.canvas.layers.size)
    }

    @Test
    fun testExtremeCase16_SpringMicroPhysicsAndHapticStateTransitions() {
        // Virtual controller button with custom spring micro-physics parameters
        // and animated state transition keyframes
        val html = """
            <style>
                :root {
                    --spring-damping: 0.62;
                    --spring-stiffness: 380;
                    --press-scale: 0.86;
                }
                .spring-tactile {
                    width: 90px;
                    height: 90px;
                    border-radius: 50%;
                    background: linear-gradient(145deg, #1e222d 0%, #0d0f14 100%);
                    border: 2px solid #00f0ff;
                    box-shadow: 0 8px 24px rgba(0, 240, 255, 0.35), inset 0 2px 6px rgba(255, 255, 255, 0.2);
                    transform: scale(1.0);
                    transition: all 0.15s cubic-bezier(0.175, 0.885, 0.32, 1.275);
                }
                .spring-tactile:active {
                    transform: scale(0.86);
                    filter: brightness(1.4) saturate(1.6);
                    box-shadow: 0 2px 8px rgba(0, 240, 255, 0.7), inset 0 0 16px rgba(0, 240, 255, 0.5);
                }
                .spring-tactile::before {
                    content: "";
                    position: absolute;
                    inset: 4px;
                    border-radius: 50%;
                    border: 1.5px dashed rgba(0, 240, 255, 0.6);
                }
            </style>
            <button class="spring-tactile" data-damping="0.62" data-stiffness="380">LT</button>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "rc.spring_tactile", name = "Tactile Spring Trigger")
        assertNotNull(doc)
        println("=== TEST 16: Spring Micro-Physics & State Transitions ===")
        val spring = doc.manifest.springPhysics
        println("Spring enabled: ${spring.enabled}")
        println("Spring dampingRatio: ${spring.dampingRatio} (expected 0.62)")
        println("Spring stiffness: ${spring.stiffness} (expected 380)")
        println("Spring pressedScale: ${spring.pressedScale} (expected 0.86)")

        assertTrue(spring.enabled)
        assertEquals(0.62f, spring.dampingRatio, 0.01f)
        assertEquals(380f, spring.stiffness, 0.1f)
        assertEquals(0.86f, spring.pressedScale, 0.01f)

        // Verify active state animation parameters
        println("Animation spring stiffness: ${doc.animations.springStiffness}")
        println("Animation spring damping: ${doc.animations.springDamping}")
        println("Animation press scale: ${doc.animations.pressScale}")
        assertEquals(380f, doc.animations.springStiffness, 0.1f)
        assertEquals(0.62f, doc.animations.springDamping, 0.01f)
        assertEquals(0.86f, doc.animations.pressScale, 0.01f)

        val bytes = NxprcDocument.encodeToBytes(doc)
        println("Encoded size: ${bytes.size} bytes (target: < 4KB)")
        assertTrue(bytes.size < 4096, "Encoded size must be compact")

        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.manifest.id, decoded.manifest.id)
        assertEquals(spring.dampingRatio, decoded.manifest.springPhysics.dampingRatio, 0.01f)
        assertEquals(spring.stiffness, decoded.manifest.springPhysics.stiffness, 0.1f)
        assertEquals(spring.pressedScale, decoded.manifest.springPhysics.pressedScale, 0.01f)
    }
}


