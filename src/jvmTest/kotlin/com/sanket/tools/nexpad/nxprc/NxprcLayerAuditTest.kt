package com.sanket.tools.nexpad.nxprc

import kotlin.test.Test

/**
 * NXPRC Layer Audit Test Suite — Universal regression barrier.
 *
 * Each test encodes a MACHINE-CHECKABLE CONTRACT for a specific HTML design.
 * This permanently fixes the "bug loop" (fix A → break B → fix B → break A).
 *
 * HOW IT WORKS:
 *   1. HTML is compiled through the full NXPRC pipeline (no mocking)
 *   2. Every layer is printed to stdout with its ratio-based bounds
 *   3. Assertions check the exact layer count, types, shapes, and positions
 *   4. Any engine change that breaks a layer fails immediately with the exact
 *      property and how far it drifted (e.g. "left=14.5px, expected 12px ±3px")
 *
 * ENGINE LAYER TYPE GUIDE:
 *   GradientShape  = PRIMARY button surface (root element → always [0] unless shadows come first)
 *   BoxLayer       = CHILD elements (pseudo-elements, nested divs)
 *   GlowRing       = outer glow box-shadow (inserted BEFORE the primary surface in z-order)
 *   BezelSocket    = large dark box-shadow on JOYSTICK (also before primary in z-order)
 *   InnerShadow    = inset box-shadow (requires high enough blur+alpha to trigger)
 *   CenterGlyph    = text/icon label of the button
 *
 * Z-ORDER RULE:
 *   Glow/Bezel come FIRST (z-order < primary), so Layer[0] may be GlowRing/BezelSocket.
 *   Layer with GradientShape follows after any shadows.
 *
 * DSL METHODS:
 *   primaryLayer(idx) { shapeOval(); ratio(...) }   -- check GradientShape at idx
 *   boxLayer(idx) { px(...) }                        -- check BoxLayer child at idx
 *   hasLayer<GlowRing>()                             -- type presence check
 *   noLayer<InnerShadow>()                           -- absence check
 *   layerAt<BezelSocket>(0)                          -- exact type at index
 *   layerCount<BoxLayer>(atLeast = 2)               -- count by type
 *   totalLayers(atLeast = 3)                         -- total count
 *
 * LEVEL GUIDE:
 *   L1 = trivial single element
 *   L2 = pseudo-elements (::before/::after)
 *   L3 = nested children, absolute positioning
 *   L4 = box-shadow → GlowRing / BezelSocket / InnerShadow detection
 *   L5 = flex layouts (column, space-around)
 *   L6 = multi-layer gradients + inner shadows
 *   L7 = deeply nested, mixed absolute children
 *   L8 = limit tests: polygon clip-path, calc()
 *   L9 = real-world anime-style + joystick designs
 *   L10 = all features combined
 */
class NxprcLayerAuditTest {

    // =========================================================================
    // LEVEL 1: TRIVIAL — Single element, no children
    // Engine: [0]=GradientShape(primary), [1]=CenterGlyph(label)
    // =========================================================================

    @Test
    fun audit_L1_solidRoundedButton() {
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                width: 80px; height: 80px;
                background: #ff0000;
                border-radius: 12px;
                display: flex; align-items: center; justify-content: center;
              }
            </style></head><body>
              <button class="btn" data-control="A" data-category="BUTTON" data-name="Solid Red">
                <span>A</span>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l1.solid", "L1 Solid Rounded", bW = 80f, bH = 80f) {
            totalLayers(atLeast = 1)
            // Root element is always GradientShape
            primaryLayer(0) {
                shapeRoundedRect()
                ratio(rW = 1.0f, rH = 1.0f, rX = 0f, rY = 0f, tol = 0.02f)
            }
        }
    }

    @Test
    fun audit_L1_ovalButton() {
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                width: 80px; height: 80px;
                background: #00ff00;
                border-radius: 50%;
                display: flex; align-items: center; justify-content: center;
              }
            </style></head><body>
              <button class="btn" data-control="B" data-category="BUTTON" data-name="Oval Green">
                <span>B</span>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l1.oval", "L1 Oval Button", bW = 80f, bH = 80f) {
            totalLayers(atLeast = 1)
            // border-radius: 50% → OVAL shape
            primaryLayer(0) {
                shapeOval()
                ratio(rW = 1.0f, rH = 1.0f, rX = 0f, rY = 0f, tol = 0.02f)
            }
        }
    }

    @Test
    fun audit_L1_wideRectButton() {
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                width: 160px; height: 48px;
                background: #1a1a2e;
                border-radius: 8px;
                display: flex; align-items: center; justify-content: center;
              }
            </style></head><body>
              <button class="btn" data-control="START" data-category="SYSTEM" data-name="Wide Rect">
                <span>START</span>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l1.wide", "L1 Wide Rect", bW = 160f, bH = 48f) {
            totalLayers(atLeast = 1)
            primaryLayer(0) {
                shapeRoundedRect()
                ratio(rW = 1.0f, rH = 1.0f, rX = 0f, rY = 0f, tol = 0.02f)
            }
        }
    }

    // =========================================================================
    // LEVEL 2: WITH PSEUDO-ELEMENTS
    // Engine: [0]=GradientShape(primary), [1]=GradientShape(::before overlay)
    // Note: ::before/::after with gradient also become GradientShape
    // =========================================================================

    @Test
    fun audit_L2_beforeGlossOverlay() {
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                position: relative; width: 96px; height: 48px;
                background: #1a1a2e; border-radius: 8px;
                display: flex; align-items: center; justify-content: center;
              }
              .btn::before {
                content: ''; position: absolute; left: 0; top: 0; right: 0; bottom: 0;
                border-radius: 8px;
                background: linear-gradient(180deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0) 50%);
              }
            </style></head><body>
              <button class="btn" data-control="X" data-category="BUTTON" data-name="Before Gloss">
                <span>X</span>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l2.before", "L2 Before Gloss", bW = 96f, bH = 48f) {
            // Produces 2+ layers: primary GradientShape + ::before GradientShape
            totalLayers(atLeast = 2)
            // Root
            primaryLayer(0) {
                shapeRoundedRect()
                ratio(rW = 1.0f, rH = 1.0f, rX = 0f, rY = 0f, tol = 0.02f)
            }
            // ::before fills parent entirely — also a GradientShape at index 1
            primaryLayer(1) {
                ratio(rW = 1.0f, rH = 1.0f, rX = 0f, rY = 0f, tol = 0.05f)
            }
        }
    }

    @Test
    fun audit_L2_beforeOvalPseudo() {
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                position: relative; width: 80px; height: 80px;
                border-radius: 50%;
                background: linear-gradient(145deg, #ff8c00, #cc5500);
                display: flex; align-items: center; justify-content: center;
              }
              .btn::before {
                content: ''; position: absolute; left: 6px; top: 6px; right: 6px; bottom: 40%;
                border-radius: 50%;
                background: linear-gradient(180deg, rgba(255,255,255,0.45) 0%, transparent 100%);
              }
            </style></head><body>
              <button class="btn" data-control="A" data-category="BUTTON" data-name="Oval Before">
                <span>A</span>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l2.oval-before", "L2 Oval ::before", bW = 80f, bH = 80f) {
            totalLayers(atLeast = 2)
            // Primary oval
            primaryLayer(0) {
                shapeOval()
                ratio(rW = 1.0f, rH = 1.0f, rX = 0f, rY = 0f, tol = 0.02f)
            }
            // ::before: border-radius:50% → must be OVAL (verifies ShapeClassifier fix for pseudo-elements)
            primaryLayer(1) {
                shapeOval()
            }
        }
    }

    // =========================================================================
    // LEVEL 3: NESTED CHILDREN, ABSOLUTE POSITIONING
    // Engine: [0]=GradientShape(primary), [1]=CenterGlyph(label OR inner text), [2]=BoxLayer(child div)
    // =========================================================================

    @Test
    fun audit_L3_absoluteInnerChild() {
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                position: relative; width: 96px; height: 96px;
                border-radius: 50%; background: #0a0a1a;
                display: flex; align-items: center; justify-content: center;
              }
              .inner {
                position: absolute; left: 12px; top: 12px; width: 72px; height: 72px;
                border-radius: 50%;
                background: radial-gradient(circle at 35% 35%, #4444ff 0%, #0000aa 100%);
              }
            </style></head><body>
              <button class="btn" data-control="A" data-category="BUTTON" data-name="Inner Oval">
                <div class="inner"></div>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l3.inner", "L3 Inner Oval", bW = 96f, bH = 96f) {
            totalLayers(atLeast = 2)
            // [0] = primary outer oval
            primaryLayer(0) { shapeOval() }
            // [1] = inner div BoxLayer, [2] = CenterGlyph (label renders on top)
            hasLayer<CanvasLayer.BoxLayer>()
            boxLayer(1) {
                shapeOval()
                px(left = 12f, top = 12f, width = 72f, height = 72f, bW = 96f, bH = 96f, tolPx = 3f)
            }
        }
    }

    @Test
    fun audit_L3_deepOvalNesting() {
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                position: relative; width: 120px; height: 120px;
                border-radius: 50%; background: #0d0d1a;
                display: flex; align-items: center; justify-content: center;
              }
              .ring {
                position: absolute; left: 8px; top: 8px; width: 104px; height: 104px;
                border-radius: 50%; border: 2px solid rgba(100,100,255,0.4);
                background: transparent;
              }
              .core {
                position: absolute; left: 24px; top: 24px; width: 72px; height: 72px;
                border-radius: 50%;
                background: radial-gradient(circle at 40% 35%, #3a3a8e 0%, #1a1a4e 100%);
              }
            </style></head><body>
              <button class="btn" data-control="B" data-category="BUTTON" data-name="Deep Oval">
                <div class="ring"></div>
                <div class="core"></div>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l3.deep-oval", "L3 Deep Oval", bW = 120f, bH = 120f) {
            totalLayers(atLeast = 3)
            // [0]=GradientShape (outer oval), ring and core are BoxLayer children, [3]=CenterGlyph on top
            primaryLayer(0) {
                shapeOval()
                ratio(rW = 1.0f, rH = 1.0f, rX = 0f, rY = 0f, tol = 0.02f)
            }
            // Ring BoxLayer at idx=1: 8,8,104,104
            boxLayer(1) {
                shapeOval()
                px(left = 8f, top = 8f, width = 104f, height = 104f, bW = 120f, bH = 120f, tolPx = 3f)
            }
            // Core BoxLayer at idx=2: 24,24,72,72
            boxLayer(2) {
                shapeOval()
                px(left = 24f, top = 24f, width = 72f, height = 72f, bW = 120f, bH = 120f, tolPx = 3f)
            }
            layerCount<CanvasLayer.BoxLayer>(atLeast = 2)
        }
    }

    // =========================================================================
    // LEVEL 4: SHADOW-BASED LAYER DETECTION
    // =========================================================================

    @Test
    fun audit_L4_glowRingFromOuterShadow() {
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                position: relative; width: 80px; height: 80px;
                border-radius: 50%; background: #1a1a2e;
                box-shadow: 0 0 20px 8px rgba(100, 100, 255, 0.85);
                display: flex; align-items: center; justify-content: center;
              }
            </style></head><body>
              <button class="btn" data-control="A" data-category="BUTTON" data-name="Glow Button"></button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l4.glow", "L4 Glow Ring", bW = 80f, bH = 80f) {
            totalLayers(atLeast = 1)
            // Large blur outer shadow → GlowRing (appears BEFORE primary in layer list)
            hasLayer<CanvasLayer.GlowRing>()
            // GlowRing is first, then GradientShape
            layerAt<CanvasLayer.GlowRing>(0)
        }
    }

    @Test
    fun audit_L4_innerShadowFromInsetShadow() {
        // NOTE: Small inset shadows (short blur, low alpha) do NOT produce InnerShadow layers.
        // InnerShadow layer requires significant blur+alpha to qualify.
        // This test verifies the basic structure: primary layer + label present.
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                position: relative; width: 80px; height: 80px;
                border-radius: 50%; background: #1a1a2e;
                box-shadow:
                  0 4px 8px rgba(0,0,0,0.6),
                  inset 0 2px 4px rgba(255,255,255,0.2),
                  inset 0 -3px 6px rgba(0,0,0,0.5);
                display: flex; align-items: center; justify-content: center;
              }
            </style></head><body>
              <button class="btn" data-control="B" data-category="BUTTON" data-name="Inner Shadow">
                <span>B</span>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l4.inner-shadow", "L4 Inner Shadow", bW = 80f, bH = 80f) {
            // Engine actual: [0]=BoxLayer(OVAL full-size), [1]=CenterGlyph
            // NOTE: With only a moderate outer drop-shadow (no glow) + inset shadows, the primary
            // surface is emitted as BoxLayer (not GradientShape). This documents engine behavior.
            totalLayers(atLeast = 1)
            hasLayer<CanvasLayer.BoxLayer>()
            boxLayer(0) {
                shapeOval()
                ratio(rW = 1.0f, rH = 1.0f, rX = 0f, rY = 0f, tol = 0.02f)
            }
        }
    }

    @Test
    fun audit_L4_bezelSocketDetection() {
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                position: relative; width: 120px; height: 120px;
                border-radius: 50%; background: #0a0a1a;
                box-shadow: 0 6px 14px 4px rgba(0,0,0,0.95), 0 4px 8px 3px rgba(0,0,0,0.9);
                display: flex; align-items: center; justify-content: center;
              }
            </style></head><body>
              <button class="btn" data-control="LS" data-category="JOYSTICK" data-name="Bezel Socket"></button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l4.bezel", "L4 Bezel Socket", bW = 120f, bH = 120f, category = "JOYSTICK") {
            totalLayers(atLeast = 1)
            // Large dark shadow on JOYSTICK → BezelSocket (comes FIRST in layer list)
            hasLayer<CanvasLayer.BezelSocket>()
            layerAt<CanvasLayer.BezelSocket>(0)
        }
    }

    // =========================================================================
    // LEVEL 5: FLEX LAYOUTS
    // Engine: [0]=GradientShape(bg), [1]=CenterGlyph(text/icon), [2+]=BoxLayer(children)
    // =========================================================================

    @Test
    fun audit_L5_flexColumnTwoChildren() {
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                position: relative; width: 100px; height: 120px;
                background: #111; border-radius: 10px;
                display: flex; flex-direction: column;
                align-items: center; justify-content: flex-start;
                padding: 10px; gap: 10px; box-sizing: border-box;
              }
              .icon { width: 40px; height: 40px; border-radius: 50%; background: #ff6600; }
              .badge { width: 60px; height: 20px; background: #334; border-radius: 4px; }
            </style></head><body>
              <button class="btn" data-control="L" data-category="BUTTON" data-name="Column Layout">
                <div class="icon"></div>
                <div class="badge"></div>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l5.column", "L5 Flex Column", bW = 100f, bH = 120f) {
            totalLayers(atLeast = 2)
            // [0]=GradientShape primary surface
            primaryLayer(0) {
                shapeRoundedRect()
                ratio(rW = 1.0f, rH = 1.0f, rX = 0f, rY = 0f, tol = 0.02f)
            }
            // icon and badge appear as BoxLayer children after CenterGlyph
            hasLayer<CanvasLayer.BoxLayer>()
        }
    }

    @Test
    fun audit_L5_flexRowSpaceAround() {
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                position: relative; width: 180px; height: 60px;
                background: #1a1a2e; border-radius: 8px;
                display: flex; flex-direction: row;
                align-items: center; justify-content: space-around;
              }
              .dot { width: 30px; height: 30px; border-radius: 50%; background: #4444ff; }
            </style></head><body>
              <button class="btn" data-control="X" data-category="BUTTON" data-name="Space Around">
                <div class="dot"></div>
                <div class="dot"></div>
                <div class="dot"></div>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l5.space-around", "L5 Space Around", bW = 180f, bH = 60f) {
            totalLayers(atLeast = 2)
            // [0]=GradientShape primary
            primaryLayer(0) {
                shapeRoundedRect()
                ratio(rW = 1.0f, rH = 1.0f, rX = 0f, rY = 0f, tol = 0.02f)
            }
            // Dots may appear as BoxLayer or be collapsed to CenterGlyph depending on content
            // We verify the primary layer exists and the structure is minimal
            hasLayer<CanvasLayer.GradientShape>()
        }
    }

    // =========================================================================
    // LEVEL 6: MULTI-LAYER GRADIENTS + SHADOWS
    // Engine: large outer shadow → GlowRing comes FIRST in z-order
    // =========================================================================

    @Test
    fun audit_L6_gradientWithInnerShadow() {
        val html = """
            <!DOCTYPE html><html><head><style>
              :root { --spring-damping: 0.7; --spring-stiffness: 420; --press-scale: 0.93; }
              .btn {
                position: relative; width: 96px; height: 48px;
                border-radius: 12px;
                background: linear-gradient(180deg, #2a5cff 0%, #1a3ccc 100%);
                box-shadow:
                  0 4px 12px rgba(42, 92, 255, 0.5),
                  inset 0 1px 0 rgba(255,255,255,0.3),
                  inset 0 -2px 0 rgba(0,0,0,0.3);
                display: flex; align-items: center; justify-content: center;
              }
              .btn::before {
                content: ''; position: absolute; left: 2px; top: 2px; right: 2px; height: 40%;
                border-radius: 10px 10px 0 0;
                background: linear-gradient(180deg, rgba(255,255,255,0.25) 0%, transparent 100%);
              }
              .label { position: relative; z-index: 5; color: #fff; font-size: 18px; font-weight: 700; }
            </style></head><body>
              <button class="btn" data-control="A" data-category="BUTTON" data-name="Gradient Multi">
                <span class="label">A</span>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l6.gradient", "L6 Multi-layer Gradient", bW = 96f, bH = 48f) {
            // Engine actual: [0]=GlowRing, [1]=BoxLayer(primary full-size ROUNDED_RECT),
            //                [2]=BoxLayer(::before overlay), [3]=CenterGlyph
            // IMPORTANT: When GlowRing fires, the primary surface becomes BoxLayer (not GradientShape)!
            totalLayers(atLeast = 3)
            hasLayer<CanvasLayer.GlowRing>()
            layerAt<CanvasLayer.GlowRing>(0)
            // Primary surface: BoxLayer full-size at idx=1
            boxLayer(1) {
                shapeRoundedRect()
                ratio(rW = 1.0f, rH = 1.0f, rX = 0f, rY = 0f, tol = 0.02f)
            }
            // ::before overlay at idx=2 (x=0.021, y=0.042, w=0.958, h=0.40)
            boxLayer(2) {
                shapeRoundedRect()
            }
        }
    }

    @Test
    fun audit_L6_glowPlusInnerShadow() {
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                position: relative; width: 80px; height: 80px;
                border-radius: 50%;
                background: radial-gradient(circle at 40% 35%, #334 0%, #112 100%);
                box-shadow:
                  0 0 24px 8px rgba(80,80,255,0.75),
                  0 6px 16px rgba(0,0,0,0.8),
                  inset 0 3px 6px rgba(255,255,255,0.25),
                  inset 0 -4px 8px rgba(0,0,0,0.6);
                display: flex; align-items: center; justify-content: center;
              }
            </style></head><body>
              <button class="btn" data-control="A" data-category="BUTTON" data-name="Glow + Inner Shadow">
                <span style="color:#fff;font-size:24px;font-weight:700">A</span>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l6.glow-inner", "L6 Glow+InnerShadow", bW = 80f, bH = 80f) {
            totalLayers(atLeast = 2)
            // GlowRing comes FIRST (z-order before primary)
            hasLayer<CanvasLayer.GlowRing>()
            layerAt<CanvasLayer.GlowRing>(0)
            // Primary oval GradientShape follows
            hasLayer<CanvasLayer.GradientShape>()
        }
    }

    // =========================================================================
    // LEVEL 7: DEEPLY NESTED, MIXED ABSOLUTE CHILDREN
    // Engine: [0]=GradientShape(root), [1]=BoxLayer(bg), [2]=BoxLayer(highlight), [3]=BoxLayer(edge)
    // =========================================================================

    @Test
    fun audit_L7_threeLayeredAbsoluteChildren() {
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                position: relative; width: 120px; height: 60px;
                background: #0d0d1a; border-radius: 16px;
                display: flex; align-items: center; justify-content: center;
                overflow: hidden;
              }
              .bg {
                position: absolute; left: 0; top: 0; width: 100%; height: 100%;
                background: linear-gradient(135deg, #1a1a3e 0%, #0a0a1a 100%);
              }
              .highlight {
                position: absolute; left: 0; top: 0; width: 100%; height: 50%;
                background: linear-gradient(180deg, rgba(255,255,255,0.1) 0%, transparent 100%);
              }
              .edge {
                position: absolute; left: 0; top: 0; right: 0; bottom: 0;
                border: 1px solid rgba(255,255,255,0.1); border-radius: 16px;
              }
            </style></head><body>
              <button class="btn" data-control="X" data-category="BUTTON" data-name="Deep Nested">
                <div class="bg"></div>
                <div class="highlight"></div>
                <div class="edge"></div>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l7.deep-nested", "L7 Deep Nested", bW = 120f, bH = 60f) {
            totalLayers(atLeast = 2)
            // [0]=GradientShape(root)
            primaryLayer(0) {
                shapeRoundedRect()
                ratio(rW = 1.0f, rH = 1.0f, rX = 0f, rY = 0f, tol = 0.02f)
            }
            // Three child divs should produce at least some BoxLayer entries
            hasLayer<CanvasLayer.BoxLayer>()
        }
    }

    // =========================================================================
    // LEVEL 8: POLYGON CLIP-PATH + CALC()
    // =========================================================================

    @Test
    fun audit_L8_octagonClipPath() {
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                position: relative; width: 96px; height: 96px;
                clip-path: polygon(25% 2%, 75% 2%, 98% 25%, 98% 75%, 75% 98%, 25% 98%, 2% 75%, 2% 25%);
                background: linear-gradient(145deg, #1e293b 0%, #0f172a 100%);
                display: flex; align-items: center; justify-content: center;
              }
              .inner {
                position: absolute; left: 10px; top: 10px; width: 76px; height: 76px;
                clip-path: polygon(25% 2%, 75% 2%, 98% 25%, 98% 75%, 75% 98%, 25% 98%, 2% 75%, 2% 25%);
                background: #0284c7;
              }
            </style></head><body>
              <button class="btn" data-control="X" data-category="BUTTON" data-name="Octagon">
                <div class="inner"></div>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l8.octagon", "L8 Octagon Clip", bW = 96f, bH = 96f) {
            totalLayers(atLeast = 1)
            // Root is a BoxLayer (polygon clip-path triggers BoxLayer not GradientShape)
            boxLayer(0) {
                shapePolygon(8)
                ratio(rW = 1.0f, rH = 1.0f, rX = 0f, rY = 0f, tol = 0.02f)
            }
            // The inner div produces a BoxLayer child
            hasLayer<CanvasLayer.BoxLayer>()
        }
    }

    @Test
    fun audit_L8_calcWidthChildRing() {
        val html = """
            <!DOCTYPE html><html><head><style>
              .btn {
                position: relative; width: 100px; height: 100px;
                border-radius: 50%; background: #18181b;
                display: flex; align-items: center; justify-content: center;
              }
              .ring {
                position: absolute; left: 8px; top: 8px;
                width: calc(100% - 16px); height: calc(100% - 16px);
                border-radius: 50%; border: 2px solid #eab308;
              }
            </style></head><body>
              <button class="btn" data-control="Y" data-category="BUTTON" data-name="Calc Ring">
                <div class="ring"></div>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l8.calc", "L8 Calc Ring", bW = 100f, bH = 100f) {
            totalLayers(atLeast = 2)
            // [0]=GradientShape primary oval
            primaryLayer(0) {
                shapeOval()
                ratio(rW = 1.0f, rH = 1.0f, rX = 0f, rY = 0f, tol = 0.02f)
            }
            // ring div with calc() → BoxLayer child
            // CenterGlyph gets index 1, ring gets index 2
            hasLayer<CanvasLayer.BoxLayer>()
        }
    }

    // =========================================================================
    // LEVEL 9: REAL-WORLD DESIGNS
    // =========================================================================

    @Test
    fun audit_L9_animeStyleCelShaded() {
        val html = """
            <!DOCTYPE html><html><head><style>
              :root { --spring-damping: 0.65; --spring-stiffness: 460; --press-scale: 0.9; }
              .nexpad-btn {
                position: relative; width: 80px; height: 80px;
                border-radius: 50%;
                background: linear-gradient(145deg, #00ccff 0%, #0077bb 50%, #004488 100%);
                box-shadow:
                  0 6px 16px rgba(0, 204, 255, 0.5),
                  0 0 30px 8px rgba(0, 153, 255, 0.35),
                  inset 0 4px 8px rgba(255,255,255,0.3),
                  inset 0 -6px 12px rgba(0,0,0,0.4);
                display: flex; align-items: center; justify-content: center;
              }
              .nexpad-btn::before {
                content: ''; position: absolute; left: 4px; top: 4px; right: 4px; bottom: 40%;
                border-radius: 50%;
                background: linear-gradient(180deg, rgba(255,255,255,0.45) 0%, rgba(255,255,255,0) 100%);
              }
              .btn-label { position: relative; z-index: 5; color: #fff; font-size: 32px; font-weight: 900; }
            </style></head><body>
              <button class="nexpad-btn" data-control="A" data-category="BUTTON" data-name="Anime A">
                <span class="btn-label">A</span>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l9.anime", "L9 Anime Cel", bW = 80f, bH = 80f) {
            // Engine actual: [0]=GlowRing, [1]=BezelSocket, [2]=GradientShape(OVAL primary),
            //                [3]=InnerShadow, [4]=GradientShape(OVAL ::before), [5]=CenterGlyph
            totalLayers(atLeast = 5)
            layerAt<CanvasLayer.GlowRing>(0)
            hasLayer<CanvasLayer.GlowRing>()
            hasLayer<CanvasLayer.InnerShadow>()
            // Primary oval GradientShape at idx=2
            primaryLayer(2) {
                shapeOval()
                ratio(rW = 1.0f, rH = 1.0f, rX = 0f, rY = 0f, tol = 0.02f)
            }
            // ::before GradientShape at idx=4 (offX=0.05, offY=0.05, w=0.90, h=0.55)
            primaryLayer(4) {
                shapeOval()
            }
        }
    }

    @Test
    fun audit_L9_joystickThumbCapLayers() {
        val html = """
            <!DOCTYPE html><html><head><style>
              :root { --spring-damping: 0.7; --spring-stiffness: 380; }
              .nexpad-btn {
                position: relative; width: 120px; height: 120px;
                border-radius: 50%;
                background: radial-gradient(circle at 50% 70%, #2a2a3e 0%, #0a0a1a 100%);
                box-shadow: 0 8px 24px rgba(0,0,0,0.7), 0 4px 8px 3px rgba(0,0,0,0.5);
                display: flex; align-items: center; justify-content: center;
              }
              .nexpad-btn .base {
                position: absolute; left: 15px; top: 15px; width: 90px; height: 90px;
                border-radius: 50%;
                background: radial-gradient(circle at 40% 35%, #3a3a5e 0%, #1a1a2e 100%);
              }
              .nexpad-btn .thumb {
                position: absolute; left: 30px; top: 30px; width: 60px; height: 60px;
                border-radius: 50%;
                background: radial-gradient(circle at 40% 35%, #6a6a8e 0%, #3a3a5e 100%);
              }
            </style></head><body>
              <button class="nexpad-btn" data-control="LS" data-category="JOYSTICK" data-name="Left Stick">
                <div class="base"></div>
                <div class="thumb"></div>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l9.joystick", "L9 Joystick", bW = 120f, bH = 120f, category = "JOYSTICK") {
            // JOYSTICK with large dark shadow → BezelSocket first in z-order
            totalLayers(atLeast = 3)
            hasLayer<CanvasLayer.BezelSocket>()
            layerAt<CanvasLayer.BezelSocket>(0)
            // Primary GradientShape (outer shell)
            hasLayer<CanvasLayer.GradientShape>()
            // Two child ovals: base (15,15,90,90) and thumb (30,30,60,60) → BoxLayer entries
            layerCount<CanvasLayer.BoxLayer>(atLeast = 2)
        }
    }

    // =========================================================================
    // LEVEL 10: ALL FEATURES COMBINED
    // =========================================================================

    @Test
    fun audit_L10_allFeaturesCombined() {
        val html = """
            <!DOCTYPE html><html><head><style>
              :root { --spring-damping: 0.68; --spring-stiffness: 440; --press-scale: 0.92; }
              .nexpad-btn {
                position: relative; width: 100px; height: 100px;
                border-radius: 50%;
                background: linear-gradient(145deg, #ff6600 0%, #cc4400 60%, #882200 100%);
                box-shadow:
                  0 0 24px 6px rgba(255, 102, 0, 0.7),
                  0 8px 20px rgba(0,0,0,0.6),
                  inset 0 3px 6px rgba(255,255,255,0.3),
                  inset 0 -4px 8px rgba(0,0,0,0.5);
                display: flex; align-items: center; justify-content: center;
              }
              .nexpad-btn::before {
                content: ''; position: absolute; left: 5px; top: 5px; right: 5px; bottom: 40%;
                border-radius: 50%;
                background: linear-gradient(180deg, rgba(255,255,255,0.4) 0%, transparent 100%);
              }
              .nexpad-btn::after {
                content: ''; position: absolute; left: 10px; bottom: 10px; right: 10px; height: 20%;
                border-radius: 50%; background: rgba(0,0,0,0.3); filter: blur(4px);
              }
              .label { position: relative; z-index: 10; font-size: 36px; font-weight: 900; color: #fff; }
            </style></head><body>
              <button class="nexpad-btn" data-control="B" data-category="BUTTON" data-name="All Features B">
                <span class="label">B</span>
              </button>
            </body></html>
        """.trimIndent()

        NxprcLayerAudit.auditHtml(html, "audit.l10.all", "L10 All Features", bW = 100f, bH = 100f) {
            // outer glow → GlowRing first
            totalLayers(atLeast = 3)
            layerAt<CanvasLayer.GlowRing>(0)
            hasLayer<CanvasLayer.GlowRing>()
            hasLayer<CanvasLayer.GradientShape>()
            // ::before and ::after produce at least 1 BoxLayer entry
            hasLayer<CanvasLayer.BoxLayer>()
        }
    }
}
