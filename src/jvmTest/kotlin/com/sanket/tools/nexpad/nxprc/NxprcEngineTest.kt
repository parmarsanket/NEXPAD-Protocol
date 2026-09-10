package com.sanket.tools.nexpad.nxprc

import com.sanket.tools.nexpad.nxprc.engine.compiler.NxprcCompiler
import com.sanket.tools.nexpad.nxprc.engine.parsers.AnimationParser
import com.sanket.tools.nexpad.nxprc.engine.parsers.FilterParser
import com.sanket.tools.nexpad.nxprc.engine.parsers.GradientParser
import com.sanket.tools.nexpad.nxprc.engine.parsers.ShadowParser
import com.sanket.tools.nexpad.nxprc.engine.dom.HtmlDomParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NxprcEngineTest {

    @Test
    fun testNxprcInputValidationAndMalformedHeader() {
        assertFails { NxprcPackager.compile("   ") }
        assertFails { NxprcPackager.compile("<button>A</button>", id = "x".repeat(129)) }

        val malformed = ByteArray(10)
        malformed[0] = 'N'.code.toByte()
        malformed[1] = 'X'.code.toByte()
        malformed[2] = 'R'.code.toByte()
        malformed[3] = 'C'.code.toByte()
        // Valid magic/version, negative payload length.
        malformed[5] = 1
        malformed[6] = 0xFF.toByte()
        malformed[7] = 0xFF.toByte()
        malformed[8] = 0xFF.toByte()
        malformed[9] = 0xFF.toByte()
        assertTrue(NxprcDocument.decodeFromBytes(malformed).isFailure)
    }

    @Test
    fun testFilterParser() {
        val f1 = FilterParser.parse("blur(4px)")
        assertEquals(4f, f1.blurRadiusPx)
        assertEquals(1.0f, f1.brightness)

        val f2 = FilterParser.parse("brightness(1.5)")
        assertEquals(1.5f, f2.brightness)

        val f3 = FilterParser.parse("brightness(120%) saturate(130%) blur(2.5px)")
        assertEquals(2.5f, f3.blurRadiusPx)
        assertEquals(1.2f, f3.brightness, 0.01f)
        assertEquals(1.3f, f3.saturate, 0.01f)
    }

    @Test
    fun testFiltersSurviveCompilationAndBinaryRoundTrip() {
        val html = """
            <style>
              .filter-btn {
                width: 80px;
                height: 80px;
                background: #204060;
                filter: blur(4px) brightness(1.2) saturate(1.3);
              }
              .filter-btn::before {
                content: "";
                position: absolute;
                inset: 8px;
                background: #ff00aa;
                filter: brightness(0.8) saturate(0.5);
              }
            </style>
            <button class="filter-btn" data-primitive="box">F</button>
        """.trimIndent()

        val document = NxprcCompiler.compile(html, "rc.filter_test", "Filter Test")
        val boxLayers = document.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>()
        assertTrue(boxLayers.isNotEmpty())

        val root = boxLayers.first { it.widthRatio == 1f && it.heightRatio == 1f }
        assertEquals(4f, root.filter.blurRadius)
        assertEquals(1.2f, root.filter.brightness, 0.01f)
        assertEquals(1.3f, root.filter.saturation, 0.01f)

        val before = boxLayers.first { it !== root }
        assertEquals(0f, before.filter.blurRadius)
        assertEquals(0.8f, before.filter.brightness, 0.01f)
        assertEquals(0.5f, before.filter.saturation, 0.01f)

        val decoded = NxprcDocument.decodeFromBytes(NxprcDocument.encodeToBytes(document)).getOrThrow()
        val decodedRoot = decoded.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>()
            .first { it.widthRatio == 1f && it.heightRatio == 1f }
        assertEquals(root.filter, decodedRoot.filter)
    }

    @Test
    fun testConditionalMediaRulesDoNotOverrideStandaloneButtonByDefault() {
        val css = """
            .button { width: 96px; height: 96px; }
            @media (max-width: 480px) { .button { width: 80px; height: 80px; } }
        """.trimIndent()
        val defaultSheet = com.sanket.tools.nexpad.nxprc.engine.css.CssTokenizer.parse(css)
        val defaultStyle = com.sanket.tools.nexpad.nxprc.engine.css.CssCascadeResolver.computeStyle(
            com.sanket.tools.nexpad.nxprc.engine.dom.DomNode("div", classNames = listOf("button")),
            defaultSheet
        ).base
        assertEquals("96px", defaultStyle["width"])
        assertEquals("80px", com.sanket.tools.nexpad.nxprc.engine.css.CssCascadeResolver.computeStyle(
            com.sanket.tools.nexpad.nxprc.engine.dom.DomNode("div", classNames = listOf("button")),
            com.sanket.tools.nexpad.nxprc.engine.css.CssTokenizer.parse(css, viewportWidth = 360f)
        ).base["width"])
    }

    @Test
    fun testAnimationParserTransformsAndOrigin() {
        val t1 = AnimationParser.parseTransforms(
            "rotate(-18deg) scale(1.05) skewX(5deg)",
            "30% 70%",
            100f,
            100f
        )
        assertEquals(-18f, t1.rotationDegrees)
        assertEquals(1.05f, t1.scaleX)
        assertEquals(1.05f, t1.scaleY)
        assertEquals(5f, t1.skewX)
        assertEquals(0.3f, t1.originXRatio, 0.01f)
        assertEquals(0.7f, t1.originYRatio, 0.01f)

        val t2 = AnimationParser.parseTransforms(
            "skew(10deg, 20deg)",
            "center",
            100f,
            100f
        )
        assertEquals(10f, t2.skewX)
        assertEquals(20f, t2.skewY)
        assertEquals(0.5f, t2.originXRatio)
        assertEquals(0.5f, t2.originYRatio)
    }

    @Test
    fun testGradientParserWithPositionAndSize() {
        val fills = GradientParser.parseAll(
            "radial-gradient(circle, #ff0000 0%, #00ff00 100%)",
            "40% 60%",
            "120% 120%"
        )
        assertTrue(fills.isNotEmpty())
        val radial = fills.first() as FillBrush.RadialGradient
        assertEquals(0.4f, radial.centerXRatio, 0.01f)
        assertEquals(0.6f, radial.centerYRatio, 0.01f)
        assertEquals(0.55f * 1.2f, radial.radiusRatio, 0.01f)
    }

    @Test
    fun testComplexHtmlCssCompilation() {
        val complexHtml = """
            <!DOCTYPE html>
            <html>
            <head>
            <style>
            .nexpad-a {
                position: relative;
                width: 96px;
                height: 96px;
                border-radius: 50%;
                opacity: 0.95;
                background:
                    radial-gradient(
                        circle at 30% 22%,
                        rgba(255,255,255,0.75) 0%,
                        rgba(255,255,255,0.20) 14%,
                        transparent 32%
                    ),
                    radial-gradient(
                        circle at 50% 45%,
                        #d8ffdf 0%,
                        #69d980 38%,
                        #35ac50 68%,
                        #12652b 100%
                    );
                border: 2px solid rgba(255,255,255,0.22);
                box-shadow:
                    0 8px 16px rgba(0,0,0,0.5),
                    0 0 0 4px #1c1d24,
                    inset 0 2px 4px rgba(255,255,255,0.3);
            }
            .nexpad-a-highlight {
                position: absolute;
                top: 8px;
                left: 18px;
                width: 60px;
                height: 24px;
                border-radius: 50%;
                background: radial-gradient(circle, rgba(255,255,255,0.8) 0%, transparent 70%);
                filter: blur(1.5px);
                transform: rotate(-15deg);
                transform-origin: 50% 50%;
                opacity: 0.85;
            }
            .nexpad-a span.label {
                font-size: 32px;
                color: #ffffff;
                font-weight: bold;
                text-shadow: 0 2px 4px rgba(0,0,0,0.6);
            }
            </style>
            </head>
            <body>
                <div class="nexpad-a">
                    <div class="nexpad-a-highlight"></div>
                    <span class="label">A</span>
                </div>
            </body>
            </html>
        """.trimIndent()

        val doc = NxprcCompiler.compile(
            html = complexHtml,
            id = "rc.test_complex_a",
            name = "Test Complex A Button",
            category = "BUTTON",
            defaultControl = "A"
        )

        assertNotNull(doc)
        assertEquals("rc.test_complex_a", doc.manifest.id)
        assertTrue(doc.canvas.layers.isNotEmpty())

        // Check for BezelSocket
        val bezel = doc.canvas.layers.filterIsInstance<CanvasLayer.BezelSocket>().firstOrNull()
        assertNotNull(bezel)

        // Check for GradientShape
        val shapes = doc.canvas.layers.filterIsInstance<CanvasLayer.GradientShape>()
        assertTrue(shapes.isNotEmpty())
        assertEquals("OVAL", shapes.first().shapeType)
        assertEquals(0.95f, shapes.first().opacity)

        // Check for InnerShadow
        val inners = doc.canvas.layers.filterIsInstance<CanvasLayer.InnerShadow>()
        assertTrue(inners.isNotEmpty())

        // Check for GlossReflection or BoxLayer highlight
        val gloss = doc.canvas.layers.filterIsInstance<CanvasLayer.GlossReflection>().firstOrNull()
        val boxHighlight = doc.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().firstOrNull { it.rotationDegrees == -15f }
        assertTrue(gloss != null || boxHighlight != null)

        // Check CenterGlyph
        val glyph = doc.canvas.layers.filterIsInstance<CanvasLayer.CenterGlyph>().firstOrNull()
        assertNotNull(glyph)
        assertEquals("A", glyph.text)

        // Verify binary roundtrip
        val bytes = NxprcDocument.encodeToBytes(doc)
        assertTrue(bytes.isNotEmpty())
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.manifest.id, decoded.manifest.id)
        assertEquals(doc.canvas.layers.size, decoded.canvas.layers.size)
    }

    @Test
    fun testBoxLayerMultiShadowAndMultiFill() {
        val boxHtml = """
            <div class="box" data-primitive="box" style="
                width: 90px;
                height: 90px;
                border-radius: 50%;
                background: radial-gradient(circle at 30% 30%, #ffffff 0%, transparent 60%), linear-gradient(180deg, #00f0ff 0%, #0044ff 100%);
                box-shadow: 0 10px 20px rgba(0,0,0,0.6), 0 0 12px #00f0ff, inset 0 2px 4px rgba(255,255,255,0.4), inset 0 -4px 8px rgba(0,0,0,0.5);
            ">
                <span>X</span>
            </div>
        """.trimIndent()

        val doc = NxprcCompiler.compile(
            html = boxHtml,
            id = "rc.box_test",
            name = "Box Test",
            defaultControl = "X"
        )

        val boxLayers = doc.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>()
        assertTrue(boxLayers.isNotEmpty(), "Should compile to BoxLayer when data-primitive='box'")
        val box = boxLayers.first()

        assertEquals("OVAL", box.shapeType)
        assertTrue(box.fills.size >= 2, "BoxLayer should preserve multiple gradient fills: got ${box.fills.size}")
        assertEquals(4, box.boxShadows.size, "BoxLayer should preserve all 4 box shadows (both inset and outset)")
        val outsetCount = box.boxShadows.count { !it.isInset }
        val insetCount = box.boxShadows.count { it.isInset }
        assertEquals(2, outsetCount)
        assertEquals(2, insetCount)
    }

    @Test
    fun testClipToBoundsWhenOverflowHidden() {
        val overflowHtml = """
            <div class="clipped-btn" style="
                width: 80px;
                height: 80px;
                border-radius: 50%;
                overflow: hidden;
                background: #ff0055;
            ">
                <span>Y</span>
            </div>
        """.trimIndent()

        val doc = NxprcCompiler.compile(
            html = overflowHtml,
            id = "rc.clipped_btn",
            name = "Clipped Button",
            defaultControl = "Y"
        )

        assertTrue(doc.canvas.clipToBounds, "clipToBounds must be true when overflow: hidden is set")
    }

    @Test
    fun testDisplayNoneAndVisibilityHiddenExclusion() {
        val hiddenHtml = """
            <div class="visible-btn" style="width: 80px; height: 80px; background: #222;">
                <div class="hidden-elem" style="display: none; width: 40px; height: 40px; background: #f00;"></div>
                <div class="invisible-elem" style="visibility: hidden; width: 40px; height: 40px; background: #0f0;"></div>
                <span style="font-size: 20px; color: #fff;">B</span>
            </div>
        """.trimIndent()

        val doc = NxprcCompiler.compile(
            html = hiddenHtml,
            id = "rc.hidden_test",
            name = "Hidden Test",
            defaultControl = "B"
        )

        // Only CenterGlyph and main button shapes should exist, no layers for display:none or visibility:hidden
        val shapes = doc.canvas.layers.filterIsInstance<CanvasLayer.GradientShape>()
        // Main button background should be at most 1 shape (from the outer div)
        assertEquals(1, shapes.size, "Hidden child elements must not produce GradientShape layers")
    }

    @Test
    fun testAutoMetadataExtraction() {
        val metaHtml = """
            <div class="gamepad-btn" data-id="rc.cyber_turbo_b" data-name="Cyber Turbo B" data-control="B" data-category="BUTTON" style="width: 80px; height: 80px; background: #000;">
                <span>B</span>
            </div>
        """.trimIndent()

        val doc = NxprcCompiler.compile(
            html = metaHtml,
            id = "",
            name = ""
        )

        assertEquals("rc.cyber_turbo_b", doc.manifest.id)
        assertEquals("Cyber Turbo B", doc.manifest.name)
        assertEquals("B", doc.manifest.defaultControl)
        assertEquals("BUTTON", doc.manifest.category)
    }

    @Test
    fun testUserCompleteAButtonCompilation() {
        val userHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
            :root {
                --button-size: 100px;
                --a-light: #d8ffdf;
                --a-main: #65d67e;
                --a-mid: #32a94d;
                --a-dark: #125d29;
                --white-soft: rgba(255,255,255,0.22);
                --white-strong: rgba(255,255,255,0.58);
                --shadow-dark: rgba(0,0,0,0.42);
            }

            .a-button {
                width: var(--button-size);
                height: var(--button-size);
                position: relative;
                display: block;
                border-radius: 50%;
                overflow: hidden;
                box-sizing: border-box;
                opacity: 0.96;
                transform-origin: 30% 70%;
                transform:
                    translate(0px, 0px)
                    rotate(2deg)
                    scaleX(1)
                    scaleY(1)
                    skewX(0deg)
                    skewY(0deg);
                background:
                    radial-gradient(
                        circle at 26% 18%,
                        rgba(255,255,255,0.85) 0%,
                        rgba(255,255,255,0.32) 10%,
                        transparent 30%
                    ),
                    radial-gradient(
                        circle at 72% 78%,
                        rgba(0,0,0,0.30) 0%,
                        transparent 55%
                    ),
                    radial-gradient(
                        circle at 48% 44%,
                        var(--a-light) 0%,
                        var(--a-main) 34%,
                        var(--a-mid) 68%,
                        var(--a-dark) 100%
                    ),
                    linear-gradient(
                        145deg,
                        rgba(255,255,255,0.12),
                        rgba(0,0,0,0.10)
                    );
                background-position:
                    center,
                    center,
                    center,
                    center;
                background-size:
                    150% 150%,
                    125% 125%,
                    100% 100%,
                    100% 100%;
                background-repeat: no-repeat;
                box-shadow:
                    0px 2px 3px rgba(255,255,255,0.18),
                    0px 5px 8px rgba(0,0,0,0.28),
                    0px 12px 20px rgba(0,0,0,0.34),
                    0px 22px 34px rgba(0,0,0,0.20),
                    inset 0px 2px 3px rgba(255,255,255,0.36),
                    inset 2px 0px 6px rgba(255,255,255,0.10),
                    inset -3px 0px 8px rgba(0,0,0,0.12),
                    inset 0px -9px 15px rgba(0,0,0,0.32);
                filter:
                    blur(0px)
                    brightness(1)
                    saturate(1.05);
            }

            .a-button::before {
                content: "";
                position: absolute;
                left: 5%;
                top: 5%;
                width: 90%;
                height: 90%;
                border-radius: 50%;
                transform-origin: 35% 30%;
                transform:
                    translate(0px, 0px)
                    rotate(-7deg)
                    scaleX(0.98)
                    scaleY(1.02)
                    skewX(-1deg);
                background:
                    radial-gradient(
                        ellipse at 27% 16%,
                        rgba(255,255,255,0.58) 0%,
                        rgba(255,255,255,0.18) 21%,
                        transparent 48%
                    ),
                    radial-gradient(
                        ellipse at 55% 105%,
                        rgba(0,0,0,0.30) 0%,
                        transparent 62%
                    ),
                    radial-gradient(
                        circle at 50% 45%,
                        rgba(255,255,255,0.08),
                        transparent 70%
                    );
                background-position:
                    center,
                    center,
                    50% 45%;
                background-size:
                    130% 100%,
                    120% 120%,
                    100% 100%;
                box-shadow:
                    inset 0px 2px 4px rgba(255,255,255,0.26),
                    inset 2px 0px 5px rgba(255,255,255,0.07),
                    inset -2px 0px 6px rgba(0,0,0,0.10),
                    inset 0px -6px 10px rgba(0,0,0,0.18);
                filter:
                    blur(0.4px)
                    brightness(1.02)
                    saturate(1.08);
                opacity: 0.88;
                pointer-events: none;
            }

            .a-button::after {
                content: "";
                position: absolute;
                left: 14%;
                top: 7%;
                width: 58%;
                height: 29%;
                border-radius: 50%;
                transform-origin: 25% 50%;
                transform:
                    translate(0px, 0px)
                    rotate(-17deg)
                    scaleX(1)
                    scaleY(0.92)
                    skewX(-4deg);
                background:
                    radial-gradient(
                        ellipse at 32% 28%,
                        rgba(255,255,255,0.95) 0%,
                        rgba(255,255,255,0.52) 18%,
                        rgba(255,255,255,0.16) 43%,
                        transparent 76%
                    );
                background-position:
                    30% 25%;
                background-size:
                    120% 120%;
                filter:
                    blur(1.2px)
                    brightness(1.08)
                    saturate(1.02);
                opacity: 0.88;
                pointer-events: none;
            }

            .a-inner-ring {
                position: absolute;
                left: 9px;
                top: 9px;
                width: 82px;
                height: 82px;
                border-radius: 50%;
                transform-origin: 50% 50%;
                transform:
                    translate(0px, 0px)
                    rotate(-4deg)
                    scaleX(1)
                    scaleY(1);
                border-top:
                    1px solid rgba(255,255,255,0.42);
                border-left:
                    1px solid rgba(255,255,255,0.18);
                border-right:
                    1px solid rgba(255,255,255,0.07);
                border-bottom:
                    1px solid rgba(0,0,0,0.20);
                box-shadow:
                    inset 0px 1px 3px rgba(255,255,255,0.16),
                    inset 0px -2px 5px rgba(0,0,0,0.13);
                opacity: 0.9;
            }

            .a-reflection {
                position: absolute;
                right: 12%;
                bottom: 15%;
                width: 33%;
                height: 11%;
                border-radius: 50%;
                transform-origin: 60% 50%;
                transform:
                    translate(0px, 0px)
                    rotate(-20deg)
                    scaleX(1)
                    scaleY(0.9);
                background:
                    radial-gradient(
                        ellipse at center,
                        rgba(255,255,255,0.20) 0%,
                        rgba(255,255,255,0.06) 45%,
                        transparent 75%
                    );
                background-position: 50% 50%;
                background-size: 120% 120%;
                filter:
                    blur(2px)
                    brightness(1.05)
                    saturate(1);
                opacity: 0.75;
            }

            .a-label {
                position: absolute;
                left: 0;
                right: 0;
                top: 0;
                bottom: 0;
                display: flex;
                align-items: center;
                justify-content: center;
                z-index: 10;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif;
                font-size: 46px;
                font-weight: 900;
                line-height: 1;
                letter-spacing: -2px;
                color: rgba(255,255,255,0.97);
                opacity: 0.98;
                transform-origin: 50% 50%;
                transform:
                    translate(0px, -1px)
                    rotate(0deg)
                    scaleX(1)
                    scaleY(1);
                text-shadow:
                    0px -1px 0px rgba(255,255,255,0.90),
                    0px 1px 0px rgba(255,255,255,0.35),
                    0px 2px 2px rgba(0,0,0,0.26),
                    0px 4px 5px rgba(0,0,0,0.28),
                    0px 7px 10px rgba(0,0,0,0.18);
            }

            .a-highlight {
                position: absolute;
                left: 29%;
                top: 14%;
                width: 15%;
                height: 5%;
                border-radius: 50%;
                background:
                    radial-gradient(
                        ellipse,
                        rgba(255,255,255,0.78),
                        rgba(255,255,255,0.20) 45%,
                        transparent 75%
                    );
                background-position: center;
                background-size: 120% 120%;
                transform-origin: 40% 50%;
                transform:
                    translate(0px, 0px)
                    rotate(-8deg)
                    scaleX(1)
                    scaleY(1);
                filter: blur(1px);
                opacity: 0.78;
            }

            .a-button:hover {
                opacity: 1;
                transform-origin: 30% 70%;
                transform:
                    translate(0px, -1px)
                    rotate(3deg)
                    scaleX(1.015)
                    scaleY(1.015)
                    skewX(0deg);
                filter:
                    blur(0px)
                    brightness(1.04)
                    saturate(1.10);
                box-shadow:
                    0px 3px 4px rgba(255,255,255,0.20),
                    0px 7px 10px rgba(0,0,0,0.30),
                    0px 16px 24px rgba(0,0,0,0.36),
                    0px 25px 40px rgba(0,0,0,0.22),
                    inset 0px 3px 4px rgba(255,255,255,0.38),
                    inset 2px 0px 7px rgba(255,255,255,0.10),
                    inset -3px 0px 9px rgba(0,0,0,0.12),
                    inset 0px -10px 16px rgba(0,0,0,0.30);
            }

            .a-button:active {
                opacity: 0.93;
                transform-origin: 50% 50%;
                transform:
                    translate(0px, 1px)
                    rotate(0deg)
                    scaleX(0.97)
                    scaleY(0.97)
                    skewX(0deg);
            }
            </style>
            </head>
            <body>
                <div class="a-button">
                    <div class="a-inner-ring"></div>
                    <div class="a-reflection"></div>
                    <div class="a-highlight"></div>
                    <span class="a-label">A</span>
                </div>
            </body>
            </html>
        """.trimIndent()

        val doc = NxprcCompiler.compile(
            html = userHtml,
            id = "rc.user_a_btn",
            name = "User A Button",
            defaultControl = "A"
        )

        assertNotNull(doc)
        // 1. CSS variables: width/height resolved from var(--button-size: 100px)
        assertEquals(100, doc.manifest.widthDp)
        assertEquals(100, doc.manifest.heightDp)

        // 2. BoxLayer for modern multi-gradient button surface
        val boxLayers = doc.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>()
        assertTrue(boxLayers.isNotEmpty(), "Should generate BoxLayer for modern CSS button")
        val mainBox = boxLayers.first()
        assertEquals("OVAL", mainBox.shapeType)
        assertTrue(mainBox.fills.size >= 4, "Should preserve all 4 multi-gradient fills: got ${mainBox.fills.size}")
        assertTrue(mainBox.boxShadows.size >= 8, "Should preserve all 8 outset and inset box-shadows: got ${mainBox.boxShadows.size}")

        // 3. Verify CSS variable color resolution in radial gradient (var(--a-light: #d8ffdf))
        val radials = mainBox.fills.filterIsInstance<FillBrush.RadialGradient>()
        val linears = mainBox.fills.filterIsInstance<FillBrush.LinearGradient>()
        assertTrue(radials.isNotEmpty(), "Radial gradients should be parsed")
        assertTrue(linears.isNotEmpty(), "Linear gradient should be parsed")
        val resolvedVarColor = radials.any { r -> r.colors.any { c -> (c and 0x00FFFFFFL) == 0x00D8FFDFL } }
        assertTrue(resolvedVarColor, "CSS variable --a-light (#d8ffdf) should be resolved in gradient stop")

        // 3b. Verify transforms on BoxLayer
        assertEquals(2.0f, mainBox.rotationDegrees, 0.01f, "Rotation should be present on BoxLayer")
        assertEquals(0.3f, mainBox.originXRatio, 0.01f, "originXRatio should be 0.3 on BoxLayer")
        assertEquals(0.7f, mainBox.originYRatio, 0.01f, "originYRatio should be 0.7 on BoxLayer")

        // 4. No fake BezelSocket generated
        val bezel = doc.canvas.layers.filterIsInstance<CanvasLayer.BezelSocket>().firstOrNull()
        assertNull(bezel, "Should NOT generate fake BezelSocket when not requested in CSS")

        // 5. Specular highlights (from ::after, .a-reflection, .a-highlight)
        val glosses = doc.canvas.layers.filterIsInstance<CanvasLayer.GlossReflection>()
        val boxGlosses = doc.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().filter { it != mainBox }
        assertTrue(glosses.size >= 2 || boxGlosses.size >= 2, "Should generate BoxLayer or GlossReflection for specular highlights")

        // 6. CenterGlyph label 'A' and multi-text-shadow
        val glyph = doc.canvas.layers.filterIsInstance<CanvasLayer.CenterGlyph>().firstOrNull()
        assertNotNull(glyph, "CenterGlyph should be generated")
        assertEquals("A", glyph.text)
        assertEquals(46f, glyph.fontSizeSp)
        assertEquals(5, glyph.textShadows.size, "All 5 text-shadows should be captured")

        // 7. Touch Active transform
        assertEquals(0.97f, doc.animations.pressScale)
        assertEquals(1.0f, doc.animations.pressOffsetY)

        // 8. Overflow hidden -> clipToBounds
        assertTrue(doc.canvas.clipToBounds, "clipToBounds should be true")

        // 9. Roundtrip binary encoding/decoding test
        val bytes = NxprcDocument.encodeToBytes(doc)
        assertTrue(bytes.isNotEmpty())
        val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
        assertEquals(doc.manifest.id, decoded.manifest.id)
        assertEquals(doc.canvas.layers.size, decoded.canvas.layers.size)
        assertEquals(doc.animations.pressScale, decoded.animations.pressScale)
    }

    @Test
    fun testRotatedDiamondButtonCompilation() {
        val diamondHtml = """
            <div class="diamond-btn" style="
                width: 80px;
                height: 80px;
                background: linear-gradient(45deg, #ff0055, #ff5500);
                transform: rotate(45deg) scale(0.9);
                transform-origin: 50% 50%;
                border-radius: 8px;
            ">
                <span>D</span>
            </div>
        """.trimIndent()

        val doc = NxprcCompiler.compile(
            html = diamondHtml,
            id = "rc.diamond_btn",
            name = "Diamond Button",
            defaultControl = "X"
        )

        assertNotNull(doc)
        val shape = doc.canvas.layers.filterIsInstance<CanvasLayer.GradientShape>().firstOrNull()
        assertNotNull(shape, "Should produce GradientShape for diamond button")
        assertEquals(45f, shape.rotationDegrees, 0.01f, "Diamond button should have 45deg rotation")
        assertEquals(0.9f, shape.scaleX, 0.01f, "Diamond button should have 0.9 scaleX")
        assertEquals(0.9f, shape.scaleY, 0.01f, "Diamond button should have 0.9 scaleY")
        assertEquals(0.5f, shape.originXRatio, 0.01f)
        assertEquals(0.5f, shape.originYRatio, 0.01f)
    }

    @Test
    fun testAnimeButtonParityContract() {
        val html = """
            <style>
              :root { --pink: #ff6fb5; --violet: #7136c9; }
              .nexpad-anime {
                width: 102px; height: 102px; overflow: hidden;
                border-radius: 28% 28% 42% 42% / 28% 28% 42% 42%;
                background: radial-gradient(circle at 50% 47%, #fff4fb 0%, var(--pink) 15%, #d9348e 37%, var(--violet) 64%, #35145f 84%, #120719 100%), linear-gradient(135deg, rgba(255,255,255,.16), transparent 36%, rgba(0,0,0,.30));
                box-shadow: 0 5px 4px rgba(0,0,0,.58), 0 24px 36px rgba(0,0,0,.25), inset 0 -11px 17px rgba(14,0,25,.75);
                transform: rotate(-3deg) skewX(0deg);
              }
              .nexpad-anime::before { content: ""; left: 7px; top: 7px; right: 7px; bottom: 7px; position: absolute; background: radial-gradient(circle, rgba(255,84,174,.18), transparent 53%); }
              .nexpad-anime:active { transform: scale(.93) translateY(3px) rotate(-3deg); }
              .btn-label { font-size: 41px; color: rgba(255,248,252,.98); text-shadow: 0 1px 0 #fff, 0 5px 7px rgba(0,0,0,.56); }
            </style>
            <button class="nexpad-anime" data-control="A" data-category="BUTTON" data-name="Action A"><span class="btn-label">A</span></button>
        """.trimIndent()

        val doc = NxprcPackager.compile(html, id = "", name = "", defaultControl = "A")
        val root = doc.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().first()
        val glyph = doc.canvas.layers.filterIsInstance<CanvasLayer.CenterGlyph>().first()

        assertEquals(102, doc.manifest.widthDp)
        assertEquals(102, doc.manifest.heightDp)
        assertEquals("rc.nexpad_anime", doc.manifest.id)
        assertEquals("Action A", doc.manifest.name)
        assertEquals("ROUNDED_RECT", root.shapeType, "Asymmetric percentage radii must stay chamfered, not become an oval")
        assertTrue(root.fills.size >= 2)
        assertEquals(3f, doc.animations.pressOffsetY, 0.01f)
        assertEquals(0.93f, doc.animations.pressScale, 0.01f)
        assertEquals(41f, glyph.fontSizeSp, 0.01f)
        assertTrue(doc.canvas.layers.any { it is CanvasLayer.BoxLayer && it.widthRatio < 1f }, "::before must retain its inset bounds")
    }

    @Test
    fun testNxprcEngineBugFixes() {
        // 1. Test inline styles containing semicolons within quotes and data URLs
        val inlineHtml = """
            <button style='content: "key;pad"; background: url("data:image/png;base64,iVBOR;w0KGgo="); color: #ffffff;'>
                <span>B</span>
            </button>
        """.trimIndent()
        val parsedDom = HtmlDomParser.parse(inlineHtml)
        val buttonNode = parsedDom.root.findByTag("button").first()
        assertEquals("\"key;pad\"", buttonNode.inlineStyles["content"])
        assertEquals("url(\"data:image/png;base64,iVBOR;w0KGgo=\")", buttonNode.inlineStyles["background"])
        assertEquals("#ffffff", buttonNode.inlineStyles["color"])

        // 2. Test ShadowParser with named color
        val namedShadows = ShadowParser.parseBoxShadows("0 4px 12px crimson")
        assertEquals(1, namedShadows.size)
        assertEquals(0xFFDC143CL, namedShadows[0].color)
        assertEquals(0f, namedShadows[0].offsetX, 0.01f)
        assertEquals(4f, namedShadows[0].offsetY, 0.01f)
        assertEquals(12f, namedShadows[0].blurRadius, 0.01f)

        // 3. Test ShadowParser does NOT false-positive match 'tan' in other text or substrings
        val rgbaShadows = ShadowParser.parseBoxShadows("0 2px 5px rgba(0,0,0,0.4)")
        assertEquals(1, rgbaShadows.size)
        val alphaChannel = ((rgbaShadows[0].color shr 24) and 0xFFL).toInt()
        assertTrue(alphaChannel in 100..105, "Alpha should be ~0.4 * 255")

        // 4. Test GradientParser.splitTopLevelCommas with quoted values
        val splitList = GradientParser.splitTopLevelCommas("linear-gradient(45deg, #111, #222), 'url(foo,bar)', #333")
        assertEquals(3, splitList.size)
        assertEquals("linear-gradient(45deg, #111, #222)", splitList[0])
        assertEquals("'url(foo,bar)'", splitList[1])
        assertEquals("#333", splitList[2])
    }

    @Test
    fun testUnquotedHtmlAttributes() {
        val html = """
            <button class=nexpad-btn id=my_btn data-control=X data-category=BUTTON>
                <span>X</span>
            </button>
        """.trimIndent()
        val parsed = HtmlDomParser.parse(html)
        val btn = parsed.root.findByTag("button").first()
        assertEquals("my_btn", btn.id)
        assertTrue(btn.classNames.contains("nexpad-btn"))
        assertEquals("X", btn.attributes["data-control"])

        val doc = NxprcPackager.compile(html)
        assertEquals("X", doc.manifest.defaultControl)
        assertEquals("rc.my_btn", doc.manifest.id)
    }

    @Test
    fun testNestedCssVariablesWithFallbacks() {
        val css = """
            :root {
                --theme-accent: var(--custom-accent, var(--fallback-accent, #00ffaa));
            }
            .themed-btn {
                background: var(--theme-accent);
            }
        """.trimIndent()
        val sheet = com.sanket.tools.nexpad.nxprc.engine.css.CssTokenizer.parse(css)
        val node = com.sanket.tools.nexpad.nxprc.engine.dom.DomNode("button", classNames = listOf("themed-btn"))
        val style = com.sanket.tools.nexpad.nxprc.engine.css.CssCascadeResolver.computeStyle(node, sheet).base
        assertEquals("#00ffaa", style["background"])
    }

    @Test
    fun testEightDigitHexColorParsing() {
        // CSS #RRGGBBAA -> Compose 0xAARRGGBB
        val color = com.sanket.tools.nexpad.nxprc.engine.parsers.ColorParser.parse("#11223344")
        assertNotNull(color)
        val a = (color shr 24) and 0xFFL
        val r = (color shr 16) and 0xFFL
        val g = (color shr 8) and 0xFFL
        val b = color and 0xFFL
        assertEquals(0x44L, a, "Alpha channel from #RRGGBBAA")
        assertEquals(0x11L, r, "Red channel from #RRGGBBAA")
        assertEquals(0x22L, g, "Green channel from #RRGGBBAA")
        assertEquals(0x33L, b, "Blue channel from #RRGGBBAA")
    }

    @Test
    fun testConcurrentDocumentCompilation() {
        val threads = mutableListOf<Thread>()
        val errors = java.util.concurrent.CopyOnWriteArrayList<Throwable>()
        val docs = java.util.concurrent.CopyOnWriteArrayList<NxprcDocument>()

        val template = """
            <style>
              .btn-%d { width: 90px; height: 90px; border-radius: 50%%; background: #%06x; }
            </style>
            <button class="btn-%d" data-control="A"><span>A</span></button>
        """.trimIndent()

        for (i in 1..25) {
            val t = Thread {
                try {
                    val html = String.format(template, i, (i * 0x050505) and 0xFFFFFF, i)
                    val doc = NxprcPackager.compile(html, id = "rc.concurrent_$i", name = "Button $i")
                    val bytes = NxprcDocument.encodeToBytes(doc)
                    val decoded = NxprcDocument.decodeFromBytes(bytes).getOrThrow()
                    docs.add(decoded)
                } catch (t: Throwable) {
                    errors.add(t)
                }
            }
            threads.add(t)
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertTrue(errors.isEmpty(), "Concurrent compilations had errors: ${errors.map { it.message }}")
        assertEquals(25, docs.size)
    }

    @Test
    fun testCompositingStrategyAutoSelection() {
        // 1. Opaque single-fill -> AUTO
        val htmlAuto = """
            <style>.btn-box { width: 80px; height: 80px; background: #ff0000; }</style>
            <button class="btn-box" data-primitive="box"><span>A</span></button>
        """.trimIndent()
        val docAuto = NxprcPackager.compile(htmlAuto)
        val boxAuto = docAuto.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().first()
        assertEquals(CompositingStrategy.AUTO, boxAuto.effectiveEffects.compositingStrategy)

        // 2. Opacity < 1 single-fill -> MODULATE_ALPHA
        val htmlMod = """
            <style>.btn-box { width: 80px; height: 80px; background: #ff0000; opacity: 0.7; }</style>
            <button class="btn-box" data-primitive="box"><span>A</span></button>
        """.trimIndent()
        val docMod = NxprcPackager.compile(htmlMod)
        val boxMod = docMod.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().first()
        assertEquals(CompositingStrategy.MODULATE_ALPHA, boxMod.effectiveEffects.compositingStrategy)

        // 3. Opacity < 1 multi-fill -> OFFSCREEN
        val htmlOff = """
            <style>.btn-box { width: 80px; height: 80px; background: linear-gradient(#f00, #0f0), radial-gradient(#00f, #fff); opacity: 0.5; }</style>
            <button class="btn-box" data-primitive="box"><span>A</span></button>
        """.trimIndent()
        val docOff = NxprcPackager.compile(htmlOff)
        val boxOff = docOff.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().first()
        assertEquals(CompositingStrategy.OFFSCREEN, boxOff.effectiveEffects.compositingStrategy)
    }

    @Test
    fun testLayerOutsetsFromBoxShadows() {
        val html = """
            <style>
                .btn-box {
                    width: 80px; height: 80px;
                    box-shadow: 5px 15px 20px 4px rgba(0,0,0,0.5);
                }
            </style>
            <button class="btn-box" data-primitive="box"><span>A</span></button>
        """.trimIndent()
        val doc = NxprcPackager.compile(html)
        val box = doc.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().first()
        val outsets = box.effectiveEffects.layerOutsets
        assertTrue(outsets.hasOutsets)
        // reach = 20 + 4 = 24.
        // bottom = reach + offsetY = 24 + 15 = 39.
        // right = reach + offsetX = 24 + 5 = 29.
        assertEquals(39f, outsets.bottom)
        assertEquals(29f, outsets.right)
        assertEquals(19f, outsets.left)
        assertEquals(9f, outsets.top)
    }

    @Test
    fun testRenderEffectDefFromCssBlur() {
        val html = """
            <style>.btn-box { width: 80px; height: 80px; filter: blur(6px); }</style>
            <button class="btn-box" data-primitive="box"><span>A</span></button>
        """.trimIndent()
        val doc = NxprcPackager.compile(html)
        val box = doc.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().first()
        val f = box.effectiveEffects.filter
        assertEquals(6f, f.blurRadius)
        assertEquals(6f, f.renderEffect.blurRadiusX)
        assertEquals(6f, f.renderEffect.blurRadiusY)
        assertEquals("CLAMP", f.renderEffect.tileMode)
    }

    @Test
    fun testDrawCacheHintForStaticLayers() {
        val htmlStatic = """
            <style>.btn-box { width: 80px; height: 80px; background: #112233; }</style>
            <button class="btn-box" data-primitive="box"><span>A</span></button>
        """.trimIndent()
        val docStatic = NxprcPackager.compile(htmlStatic)
        val boxStatic = docStatic.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().first()
        assertTrue(boxStatic.effectiveEffects.drawCacheHint, "Static layer should have drawCacheHint = true")

        val htmlAnimated = """
            <style>
                @keyframes spin { 100% { transform: rotate(360deg); } }
                .btn-box { width: 80px; height: 80px; background: #112233; animation: spin 2s infinite linear; }
            </style>
            <button class="btn-box" data-primitive="box"><span>A</span></button>
        """.trimIndent()
        val docAnimated = NxprcPackager.compile(htmlAnimated)
        val boxAnimated = docAnimated.canvas.layers.filterIsInstance<CanvasLayer.BoxLayer>().first()
        assertTrue(!boxAnimated.effectiveEffects.drawCacheHint, "Rotating layer should have drawCacheHint = false")
    }

    @Test
    fun testClipToBoundsNotAutoSetForCircular() {
        val htmlCircular = """
            <style>.b { width: 80px; height: 80px; border-radius: 50%; background: #222; }</style>
            <button class="b"><span>A</span></button>
        """.trimIndent()
        val docCircular = NxprcPackager.compile(htmlCircular)
        assertTrue(!docCircular.canvas.clipToBounds, "border-radius: 50% should not force clipToBounds on canvas")

        val htmlOverflowHidden = """
            <style>.b { width: 80px; height: 80px; border-radius: 50%; overflow: hidden; background: #222; }</style>
            <button class="b"><span>A</span></button>
        """.trimIndent()
        val docOverflow = NxprcPackager.compile(htmlOverflowHidden)
        assertTrue(docOverflow.canvas.clipToBounds, "overflow: hidden should force clipToBounds")
    }

    @Test
    fun testCanvasOutsetsUnionOfLayerOutsets() {
        val html = """
            <style>
                .b {
                    width: 80px; height: 80px;
                    box-shadow: 0 10px 20px rgba(0,0,0,0.6);
                }
            </style>
            <button class="b"><span>A</span></button>
        """.trimIndent()
        val doc = NxprcPackager.compile(html)
        assertTrue(doc.canvas.canvasOutsets.hasOutsets, "canvasOutsets should reflect layer shadow outsets")
        assertEquals(30f, doc.canvas.canvasOutsets.bottom) // reach(20) + offsetY(10)
    }

    @Test
    fun testBackwardCompatV1Deserialization() {
        // Construct a raw minimal v1 payload that lacks all v2 fields:
        // version: 1, no canvasOutsets, no compositingStrategy, no layerOutsets, no renderEffect
        val v1Json = """
            {
                "version": 1,
                "manifest": {
                    "id": "rc.v1_btn",
                    "name": "V1 Button",
                    "author": "Legacy",
                    "version": "1.0.0",
                    "category": "BUTTON",
                    "defaultControl": "A",
                    "widthDp": 76,
                    "heightDp": 76,
                    "description": ""
                },
                "canvas": {
                    "viewBoxWidth": 100.0,
                    "viewBoxHeight": 100.0,
                    "layers": [
                        {
                            "type": "BoxLayer",
                            "shapeType": "ROUNDED_RECT",
                            "cornerRadiusTopLeft": 14.0,
                            "cornerRadiusTopRight": 14.0,
                            "cornerRadiusBottomRight": 14.0,
                            "cornerRadiusBottomLeft": 14.0,
                            "widthRatio": 1.0,
                            "heightRatio": 1.0,
                            "clipToBounds": false,
                            "fill": { "type": "Solid", "color": -16711936 },
                            "opacity": 0.8
                        }
                    ],
                    "clipToBounds": false
                },
                "animations": {}
            }
        """.trimIndent()

        val jsonBytes = v1Json.encodeToByteArray()
        val buffer = java.nio.ByteBuffer.allocate(10 + jsonBytes.size)
            .order(java.nio.ByteOrder.BIG_ENDIAN)
            .put(NxprcDocument.MAGIC)
            .putShort(1.toShort()) // v1
            .putInt(jsonBytes.size)
            .put(jsonBytes)
            .array()

        val decoded = NxprcDocument.decodeFromBytes(buffer).getOrThrow()
        assertEquals(1, decoded.version)
        assertEquals("rc.v1_btn", decoded.manifest.id)
        // Check new fields have default values
        assertEquals(LayerOutsets(), decoded.canvas.canvasOutsets)
        val layer = decoded.canvas.layers.first() as CanvasLayer.BoxLayer
        assertEquals(CompositingStrategy.AUTO, layer.effectiveEffects.compositingStrategy)
        assertEquals(LayerOutsets(), layer.effectiveEffects.layerOutsets)
        assertEquals(RenderEffectDef(), layer.effectiveEffects.filter.renderEffect)
    }
}
