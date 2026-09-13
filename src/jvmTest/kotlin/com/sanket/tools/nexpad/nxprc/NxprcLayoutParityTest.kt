package com.sanket.tools.nexpad.nxprc

import com.sanket.tools.nexpad.nxprc.engine.compiler.FlexLayoutEngine
import com.sanket.tools.nexpad.nxprc.engine.compiler.NxprcCompiler
import com.sanket.tools.nexpad.nxprc.engine.css.CssTokenizer
import com.sanket.tools.nexpad.nxprc.engine.dom.HtmlDomParser
import com.sanket.tools.nexpad.nxprc.engine.parsers.ComputedBoxBounds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive regression test suite verifying W3C CSS Flexbox §4.1 static positioning
 * and explicit positional constraints parity in NXPRC documents.
 * Covers Test Groups A through K.
 */
class NxprcLayoutParityTest {

    // ---------------------------------------------------------------------------------------------
    // Test Group K: Minimal Reproducer (Bug parity validation)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testGroupK_MinimalReproducer_FlexCenterAbsoluteChild() {
        val parentStyle = mapOf(
            "display" to "flex",
            "justify-content" to "center",
            "align-items" to "center"
        )
        val childStyle = mapOf(
            "position" to "absolute",
            "width" to "20px",
            "height" to "20px"
        )
        val rawBounds = ComputedBoxBounds(0f, 0f, 20f, 20f)
        val resolved = FlexLayoutEngine.resolvePositionedChildBounds(
            parentStyle = parentStyle,
            childStyle = childStyle,
            rawBounds = rawBounds,
            parentWidth = 100f,
            parentHeight = 100f
        )

        // 100x100 container, 20x20 child centered -> (100 - 20) / 2 = 40
        assertEquals(40f, resolved.left, 0.001f, "Horizontal offset must center at 40")
        assertEquals(40f, resolved.top, 0.001f, "Vertical offset must center at 40")
        assertEquals(20f, resolved.width, 0.001f)
        assertEquals(20f, resolved.height, 0.001f)
    }

    // ---------------------------------------------------------------------------------------------
    // Test Group A: Flex-Start (Row and Column)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testGroupA_FlexStart_RowAndColumn() {
        // Row
        val parentRow = mapOf(
            "display" to "flex",
            "flex-direction" to "row",
            "justify-content" to "flex-start",
            "align-items" to "flex-start"
        )
        val child = mapOf("position" to "absolute", "width" to "30px", "height" to "40px")
        val raw = ComputedBoxBounds(0f, 0f, 30f, 40f)

        val resRow = FlexLayoutEngine.resolvePositionedChildBounds(parentRow, child, raw, 120f, 100f)
        assertEquals(0f, resRow.left, 0.001f)
        assertEquals(0f, resRow.top, 0.001f)

        // Column
        val parentCol = mapOf(
            "display" to "flex",
            "flex-direction" to "column",
            "justify-content" to "flex-start",
            "align-items" to "flex-start"
        )
        val resCol = FlexLayoutEngine.resolvePositionedChildBounds(parentCol, child, raw, 120f, 100f)
        assertEquals(0f, resCol.left, 0.001f)
        assertEquals(0f, resCol.top, 0.001f)
    }

    // ---------------------------------------------------------------------------------------------
    // Test Group B: Center (Row and Column)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testGroupB_Center_RowAndColumn() {
        val child = mapOf("position" to "absolute", "width" to "30px", "height" to "40px")
        val raw = ComputedBoxBounds(0f, 0f, 30f, 40f)

        // Row center
        val parentRow = mapOf(
            "display" to "flex",
            "flex-direction" to "row",
            "justify-content" to "center",
            "align-items" to "center"
        )
        val resRow = FlexLayoutEngine.resolvePositionedChildBounds(parentRow, child, raw, 120f, 100f)
        assertEquals(45f, resRow.left, 0.001f) // (120 - 30) / 2 = 45
        assertEquals(30f, resRow.top, 0.001f)  // (100 - 40) / 2 = 30

        // Column center
        val parentCol = mapOf(
            "display" to "flex",
            "flex-direction" to "column",
            "justify-content" to "center",
            "align-items" to "center"
        )
        val resCol = FlexLayoutEngine.resolvePositionedChildBounds(parentCol, child, raw, 120f, 100f)
        assertEquals(45f, resCol.left, 0.001f) // Cross-axis (width): (120 - 30) / 2 = 45
        assertEquals(30f, resCol.top, 0.001f)  // Main-axis (height): (100 - 40) / 2 = 30
    }

    // ---------------------------------------------------------------------------------------------
    // Test Group C: Flex-End (Row and Column)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testGroupC_FlexEnd_RowAndColumn() {
        val child = mapOf("position" to "absolute", "width" to "30px", "height" to "40px")
        val raw = ComputedBoxBounds(0f, 0f, 30f, 40f)

        // Row flex-end
        val parentRow = mapOf(
            "display" to "flex",
            "flex-direction" to "row",
            "justify-content" to "flex-end",
            "align-items" to "flex-end"
        )
        val resRow = FlexLayoutEngine.resolvePositionedChildBounds(parentRow, child, raw, 120f, 100f)
        assertEquals(90f, resRow.left, 0.001f) // 120 - 30 = 90
        assertEquals(60f, resRow.top, 0.001f)  // 100 - 40 = 60

        // Column flex-end
        val parentCol = mapOf(
            "display" to "flex",
            "flex-direction" to "column",
            "justify-content" to "flex-end",
            "align-items" to "flex-end"
        )
        val resCol = FlexLayoutEngine.resolvePositionedChildBounds(parentCol, child, raw, 120f, 100f)
        assertEquals(90f, resCol.left, 0.001f) // Cross-axis align-items: flex-end -> 120 - 30 = 90
        assertEquals(60f, resCol.top, 0.001f)  // Main-axis justify-content: flex-end -> 100 - 40 = 60
    }

    // ---------------------------------------------------------------------------------------------
    // Test Group D: Row-Reverse
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testGroupD_RowReverse() {
        val child = mapOf("position" to "absolute", "width" to "30px", "height" to "40px")
        val raw = ComputedBoxBounds(0f, 0f, 30f, 40f)

        // Row-reverse with justify-content: flex-start sits at the right
        val parentStart = mapOf(
            "display" to "flex",
            "flex-direction" to "row-reverse",
            "justify-content" to "flex-start",
            "align-items" to "flex-start"
        )
        val resStart = FlexLayoutEngine.resolvePositionedChildBounds(parentStart, child, raw, 120f, 100f)
        assertEquals(90f, resStart.left, 0.001f) // Start in row-reverse is right: 120 - 30 = 90
        assertEquals(0f, resStart.top, 0.001f)

        // Row-reverse with justify-content: flex-end sits at the left
        val parentEnd = mapOf(
            "display" to "flex",
            "flex-direction" to "row-reverse",
            "justify-content" to "flex-end",
            "align-items" to "flex-start"
        )
        val resEnd = FlexLayoutEngine.resolvePositionedChildBounds(parentEnd, child, raw, 120f, 100f)
        assertEquals(0f, resEnd.left, 0.001f) // End in row-reverse is left: 0
        assertEquals(0f, resEnd.top, 0.001f)
    }

    // ---------------------------------------------------------------------------------------------
    // Test Group E: Column-Reverse
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testGroupE_ColumnReverse() {
        val child = mapOf("position" to "absolute", "width" to "30px", "height" to "40px")
        val raw = ComputedBoxBounds(0f, 0f, 30f, 40f)

        // Column-reverse with justify-content: flex-start sits at bottom
        val parentStart = mapOf(
            "display" to "flex",
            "flex-direction" to "column-reverse",
            "justify-content" to "flex-start",
            "align-items" to "flex-start"
        )
        val resStart = FlexLayoutEngine.resolvePositionedChildBounds(parentStart, child, raw, 120f, 100f)
        assertEquals(0f, resStart.left, 0.001f)
        assertEquals(60f, resStart.top, 0.001f) // Start in column-reverse is bottom: 100 - 40 = 60

        // Column-reverse with justify-content: flex-end sits at top
        val parentEnd = mapOf(
            "display" to "flex",
            "flex-direction" to "column-reverse",
            "justify-content" to "flex-end",
            "align-items" to "flex-start"
        )
        val resEnd = FlexLayoutEngine.resolvePositionedChildBounds(parentEnd, child, raw, 120f, 100f)
        assertEquals(0f, resEnd.left, 0.001f)
        assertEquals(0f, resEnd.top, 0.001f) // End in column-reverse is top: 0
    }

    // ---------------------------------------------------------------------------------------------
    // Test Group F: Explicit Constraints Precedence
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testGroupF_ExplicitConstraintsPrecedence() {
        val parentStyle = mapOf(
            "display" to "flex",
            "justify-content" to "center",
            "align-items" to "center"
        )

        // F1: left is explicit, top is auto/unspecified -> horizontal fixed at 12px, vertical centered at 30px
        val childLeftOnly = mapOf(
            "position" to "absolute",
            "left" to "12px",
            "width" to "30px",
            "height" to "40px"
        )
        val raw1 = ComputedBoxBounds(12f, 0f, 30f, 40f)
        val res1 = FlexLayoutEngine.resolvePositionedChildBounds(parentStyle, childLeftOnly, raw1, 100f, 100f)
        assertEquals(12f, res1.left, 0.001f)
        assertEquals(30f, res1.top, 0.001f)

        // F2: top is explicit, left is auto/unspecified -> vertical fixed at 15px, horizontal centered at 35px
        val childTopOnly = mapOf(
            "position" to "absolute",
            "top" to "15px",
            "width" to "30px",
            "height" to "40px"
        )
        val raw2 = ComputedBoxBounds(0f, 15f, 30f, 40f)
        val res2 = FlexLayoutEngine.resolvePositionedChildBounds(parentStyle, childTopOnly, raw2, 100f, 100f)
        assertEquals(35f, res2.left, 0.001f)
        assertEquals(15f, res2.top, 0.001f)

        // F3: right and bottom are explicit
        val childRightBottom = mapOf(
            "position" to "absolute",
            "right" to "8px",
            "bottom" to "6px",
            "width" to "30px",
            "height" to "40px"
        )
        val raw3 = ComputedBoxBounds(62f, 54f, 30f, 40f)
        val res3 = FlexLayoutEngine.resolvePositionedChildBounds(parentStyle, childRightBottom, raw3, 100f, 100f)
        assertEquals(62f, res3.left, 0.001f) // 100 - 8 - 30 = 62
        assertEquals(54f, res3.top, 0.001f)  // 100 - 6 - 40 = 54

        // F4: inset shorthand
        val childInset = mapOf(
            "position" to "absolute",
            "inset" to "10px",
            "width" to "30px",
            "height" to "40px"
        )
        val raw4 = ComputedBoxBounds(10f, 10f, 30f, 40f)
        val res4 = FlexLayoutEngine.resolvePositionedChildBounds(parentStyle, childInset, raw4, 100f, 100f)
        assertEquals(10f, res4.left, 0.001f)
        assertEquals(10f, res4.top, 0.001f)

        // F5: Percentage offsets
        val childPercent = mapOf(
            "position" to "absolute",
            "left" to "25%",
            "top" to "50%",
            "width" to "30px",
            "height" to "40px"
        )
        val raw5 = ComputedBoxBounds(25f, 50f, 30f, 40f)
        val res5 = FlexLayoutEngine.resolvePositionedChildBounds(parentStyle, childPercent, raw5, 100f, 100f)
        assertEquals(25f, res5.left, 0.001f)
        assertEquals(50f, res5.top, 0.001f)
    }

    // ---------------------------------------------------------------------------------------------
    // Test Group G: Nested Flex Containers (3 levels deep)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testGroupG_NestedFlexHierarchyGlobalCenter() {
        val html = """
            <style>
              .root-box {
                width: 200px;
                height: 200px;
                display: flex;
                align-items: center;
                justify-content: center;
              }
              .mid-box {
                width: 120px;
                height: 120px;
                display: flex;
                align-items: center;
                justify-content: center;
              }
              .leaf-target {
                position: absolute;
                width: 40px;
                height: 40px;
              }
            </style>
            <div class="root-box">
              <div class="mid-box">
                <div class="leaf-target"></div>
              </div>
            </div>
        """.trimIndent()

        val parsed = HtmlDomParser.parse(html)
        val rootNode = parsed.root
        val stylesheet = CssTokenizer.parse(parsed.embeddedCss)

        // Find leaf node
        val leafNode = rootNode.findFirst { it.classNames.contains("leaf-target") }!!

        val center = FlexLayoutEngine.computeNodeGlobalCenter(
            node = leafNode,
            rootNode = rootNode,
            stylesheet = stylesheet,
            rootW = 200f,
            rootH = 200f
        )

        // Root is 200x200, mid-box is centered at (40, 40), leaf-target is centered at (40, 40) in mid-box
        // Global of leaf-target: (40 + 40, 40 + 40) = (80, 80)
        // Center of leaf-target (40x40): 80 + 20 = 100, 80 + 20 = 100.
        // Thus exact canvas center is (100, 100).
        assertEquals(100f, center.first, 0.01f, "Global center X must be 100")
        assertEquals(100f, center.second, 0.01f, "Global center Y must be 100")
    }

    // ---------------------------------------------------------------------------------------------
    // Test Group H & I: Asymmetric Child and Container Sizes with Padding
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testGroupH_I_AsymmetricDimensionsAndPadding() {
        val parentStyle = mapOf(
            "display" to "flex",
            "justify-content" to "center",
            "align-items" to "center",
            "padding" to "10px 20px 30px 40px" // top: 10, right: 20, bottom: 30, left: 40
        )
        val childStyle = mapOf(
            "position" to "absolute",
            "width" to "30px",
            "height" to "70px"
        )
        val raw = ComputedBoxBounds(0f, 0f, 30f, 70f)
        val res = FlexLayoutEngine.resolvePositionedChildBounds(
            parentStyle = parentStyle,
            childStyle = childStyle,
            rawBounds = raw,
            parentWidth = 150f,
            parentHeight = 200f
        )

        // parentWidth = 150, pLeft = 40, pRight = 20. Available = 150 - 40 - 20 - 30 = 60.
        // staticLeft = pLeft + avail / 2 = 40 + 30 = 70.
        assertEquals(70f, res.left, 0.001f)

        // parentHeight = 200, pTop = 10, pBottom = 30. Available = 200 - 10 - 30 - 70 = 90.
        // staticTop = pTop + avail / 2 = 10 + 45 = 55.
        assertEquals(55f, res.top, 0.001f)
    }

    // ---------------------------------------------------------------------------------------------
    // Test Group J: Full Hulk Button Regression Fixture
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testGroupJ_HulkRegressionFixture() {
        val html = """
            <style>
              .nexpad-btn {
                position: relative;
                width: 96px;
                height: 96px;
                display: flex;
                align-items: center;
                justify-content: center;
              }
              .hulk-core {
                position: relative;
                width: 65px;
                height: 65px;
                display: flex;
                align-items: center;
                justify-content: center;
              }
              .fist-mark {
                position: absolute;
                width: 34px;
                height: 40px;
              }
            </style>
            <button class="nexpad-btn">
              <div class="hulk-core">
                <div class="fist-mark"></div>
              </div>
            </button>
        """.trimIndent()

        val parsed = HtmlDomParser.parse(html)
        val rootNode = parsed.root
        val stylesheet = CssTokenizer.parse(parsed.embeddedCss)

        val hulkCore = rootNode.findFirst { it.classNames.contains("hulk-core") }!!
        val fistMark = rootNode.findFirst { it.classNames.contains("fist-mark") }!!

        // 1. Verify layoutFlexContainerChildren on hulk-core places fist-mark at (15.5, 12.5)
        val coreStyle = mapOf(
            "display" to "flex",
            "align-items" to "center",
            "justify-content" to "center",
            "width" to "65px",
            "height" to "65px"
        )
        val coreBoundsMap = FlexLayoutEngine.layoutFlexContainerChildren(
            parentNode = hulkCore,
            parentStyle = coreStyle,
            stylesheet = stylesheet,
            parentWidth = 65f,
            parentHeight = 65f
        )
        val fistBounds = coreBoundsMap[fistMark]
        assertNotNull(fistBounds, "fist-mark must be present in coreBoundsMap")
        assertEquals(15.5f, fistBounds.left, 0.001f, "fist-mark local X must be 15.5")
        assertEquals(12.5f, fistBounds.top, 0.001f, "fist-mark local Y must be 12.5")

        // 2. Verify computeNodeGlobalCenter places fist-mark global center at (48.0, 48.0)
        val fistCenter = FlexLayoutEngine.computeNodeGlobalCenter(
            node = fistMark,
            rootNode = rootNode,
            stylesheet = stylesheet,
            rootW = 96f,
            rootH = 96f
        )
        assertEquals(48.0f, fistCenter.first, 0.01f, "fist-mark global center X must be 48.0")
        assertEquals(48.0f, fistCenter.second, 0.01f, "fist-mark global center Y must be 48.0")

        // 3. Verify compilation to binary document succeeds
        val compiled = NxprcCompiler.compile(html, id = "rc.hulk_test", name = "Hulk Test")
        assertTrue(compiled.canvas.layers.isNotEmpty(), "Compiled document must contain layers")
    }
}
