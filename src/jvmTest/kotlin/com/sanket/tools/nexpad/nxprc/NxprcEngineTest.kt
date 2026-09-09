package com.sanket.tools.nexpad.nxprc

import com.sanket.tools.nexpad.nxprc.engine.compiler.NxprcCompiler
import com.sanket.tools.nexpad.nxprc.engine.parsers.AnimationParser
import com.sanket.tools.nexpad.nxprc.engine.parsers.FilterParser
import com.sanket.tools.nexpad.nxprc.engine.parsers.GradientParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NxprcEngineTest {

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

        // Check for GlossReflection (highlight with blur)
        val gloss = doc.canvas.layers.filterIsInstance<CanvasLayer.GlossReflection>().firstOrNull()
        assertNotNull(gloss)
        assertEquals(1.5f, gloss.blurRadius)
        assertEquals(-15f, gloss.rotationDegrees)

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

        // 5. GlossReflection highlights (from ::after, .a-reflection, .a-highlight)
        val glosses = doc.canvas.layers.filterIsInstance<CanvasLayer.GlossReflection>()
        assertTrue(glosses.size >= 2, "Should generate GlossReflection for specular highlights")
        assertTrue(glosses.any { it.blurRadius > 0f }, "Blur filter should be applied to gloss reflection")

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
}

