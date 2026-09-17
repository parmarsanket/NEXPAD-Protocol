package com.sanket.tools.nexpad.category

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CategoryManagerTest {

    @Test
    fun testAllCategoriesArePresent() {
        val categories = CategoryManager.getAllCategories()
        assertEquals(7, categories.size)
        val ids = categories.map { it.id }.toSet()
        assertTrue(ids.containsAll(listOf("ABXY", "DPAD", "TRIGGERS", "BUMPERS", "STICKS", "SYSTEM", "MACROS")))
    }

    @Test
    fun testAbxyCategoryHasAllFaceButtons() {
        val abxy = CategoryManager.getCategory(CategoryType.ABXY)
        assertEquals(4, abxy.controls.size)
        val keys = abxy.controls.map { it.key }
        assertEquals(listOf("A", "B", "X", "Y"), keys)
    }

    @Test
    fun testSystemCategoryHasExactFourControls() {
        val sys = CategoryManager.getCategory(CategoryType.SYSTEM)
        assertEquals(4, sys.controls.size)
        val keys = sys.controls.map { it.key }
        assertEquals(listOf("GUIDE", "START", "BACK", "SHARE"), keys)
        assertTrue(sys.keys.containsAll(listOf("GUIDE", "START", "BACK", "SHARE", "HOME", "XBOX", "PS", "MENU", "VIEW", "SELECT", "CAPTURE", "SCREENSHOT")))
    }

    @Test
    fun testDpadCardinalDirections() {
        val dpad = CategoryManager.getCategory("DPAD")
        assertNotNull(dpad)
        val keys = dpad.controls.map { it.key }
        assertEquals(listOf("UP", "DOWN", "LEFT", "RIGHT", "DPAD"), keys)
        assertTrue(dpad.keys.containsAll(listOf("DPAD", "CROSS", "UP", "DOWN", "LEFT", "RIGHT")))
    }

    @Test
    fun testSticksAndTriggersDimensions() {
        val ls = CategoryManager.getControl("LS")
        assertNotNull(ls)
        assertEquals(130, ls.defaultWidthDp)
        assertEquals(130, ls.defaultHeightDp)
        assertEquals(ComponentType.JOYSTICK, ls.componentType)

        val rt = CategoryManager.getControl("RT")
        assertNotNull(rt)
        assertEquals(110, rt.defaultWidthDp)
        assertEquals(140, rt.defaultHeightDp)
        assertEquals(ComponentType.TRIGGER, rt.componentType)

        val lb = CategoryManager.getControl("LB")
        assertNotNull(lb)
        assertEquals(120, lb.defaultWidthDp)
        assertEquals(60, lb.defaultHeightDp)
        assertEquals(ComponentType.BUMPER, lb.componentType)
    }

    @Test
    fun testAliasesResolveCorrectly() {
        val start = CategoryManager.getControl("START")
        val menu = CategoryManager.getControl("MENU")
        assertEquals(start, menu)

        val back = CategoryManager.getControl("BACK")
        val view = CategoryManager.getControl("VIEW")
        val select = CategoryManager.getControl("SELECT")
        assertEquals(back, view)
        assertEquals(back, select)

        val guide = CategoryManager.getControl("GUIDE")
        val xbox = CategoryManager.getControl("XBOX")
        assertEquals(guide, xbox)
    }

    @Test
    fun testFindCategoryForControl() {
        val catA = CategoryManager.findCategoryForControl("A")
        assertEquals(CategoryType.ABXY, catA?.type)

        val catLt = CategoryManager.findCategoryForControl("LT")
        assertEquals(CategoryType.TRIGGERS, catLt?.type)

        val catM1 = CategoryManager.findCategoryForControl("M1")
        assertEquals(CategoryType.MACROS, catM1?.type)
    }

    @Test
    fun testResolveDefaultDimensions() {
        val (wA, hA) = CategoryManager.resolveDefaultDimensions("A")
        assertEquals(96, wA)
        assertEquals(96, hA)

        val (wLb, hLb) = CategoryManager.resolveDefaultDimensions("LB")
        assertEquals(120, wLb)
        assertEquals(60, hLb)
    }

    @Test
    fun testCategoryKeysIncludeAliases() {
        val sys = CategoryManager.getCategory("SYSTEM")
        assertNotNull(sys)
        assertTrue(sys.keys.containsAll(listOf("START", "BACK", "GUIDE", "VIEW", "MENU", "HOME", "XBOX")))

        val dpad = CategoryManager.getCategory("DPAD")
        assertNotNull(dpad)
        assertTrue(dpad.keys.contains("CROSS"))
    }

    @Test
    fun testIconManagementAcrossAllCategoriesAndControls() {
        val categories = CategoryManager.getAllCategories()
        for (cat in categories) {
            assertTrue(cat.emoji.isNotBlank(), "Category ${cat.id} has blank emoji")
            assertTrue(cat.iconName.isNotBlank(), "Category ${cat.id} has blank iconName")
            assertTrue(cat.svgPath.isNotBlank(), "Category ${cat.id} has blank svgPath")

            for (ctrl in cat.controls) {
                assertTrue(ctrl.emoji.isNotBlank(), "Control ${ctrl.key} has blank emoji")
                assertTrue(ctrl.iconName.isNotBlank(), "Control ${ctrl.key} has blank iconName")
                assertTrue(ctrl.svgPath.isNotBlank(), "Control ${ctrl.key} has blank svgPath")
                assertEquals(ctrl.symbol.iconName, ctrl.iconName)
                assertEquals(ctrl.symbol.svgPath, ctrl.svgPath)
            }
        }

        // Test helper query APIs
        assertEquals("🅰️", CategoryManager.getIconEmoji("A"))
        assertEquals("🎯", CategoryManager.getIconEmoji("LT"))
        assertEquals("🕹️", CategoryManager.getIconEmoji("LS"))
        assertEquals("⨂", CategoryManager.getIconEmoji("GUIDE"))
        assertEquals("⨂", CategoryManager.getIconEmoji("XBOX")) // via alias

        assertEquals(CategorySymbol.GAMEPAD, CategoryManager.getIconSymbol("A"))
        assertEquals(CategorySymbol.TRIGGER, CategoryManager.getIconSymbol("LT"))
        assertEquals(CategorySymbol.HOME, CategoryManager.getIconSymbol("GUIDE"))
        assertEquals(CategorySymbol.HOME, CategoryManager.getIconSymbol("HOME")) // via alias

        assertEquals("SportsEsports", CategoryManager.getIconName("A"))
        assertEquals("Tune", CategoryManager.getIconName("RT"))
        assertTrue(CategoryManager.getIconSvgPath("A").startsWith("M21.58"))
    }

    @Test
    fun testCategoryTypeFromIdentifier() {
        assertEquals(CategoryType.ABXY, CategoryType.fromIdentifier("BUTTON"))
        assertEquals(CategoryType.ABXY, CategoryType.fromIdentifier("BUTTONS"))
        assertEquals(CategoryType.ABXY, CategoryType.fromIdentifier("ACTION"))
        assertEquals(CategoryType.ABXY, CategoryType.fromIdentifier("A"))

        assertEquals(CategoryType.TRIGGERS, CategoryType.fromIdentifier("TRIGGER"))
        assertEquals(CategoryType.TRIGGERS, CategoryType.fromIdentifier("TRIGGERS"))
        assertEquals(CategoryType.TRIGGERS, CategoryType.fromIdentifier("LT"))
        assertEquals(CategoryType.TRIGGERS, CategoryType.fromIdentifier("L2"))

        assertEquals(CategoryType.BUMPERS, CategoryType.fromIdentifier("BUMPER"))
        assertEquals(CategoryType.BUMPERS, CategoryType.fromIdentifier("BUMPERS"))
        assertEquals(CategoryType.BUMPERS, CategoryType.fromIdentifier("LB"))
        assertEquals(CategoryType.BUMPERS, CategoryType.fromIdentifier("L1"))

        assertEquals(CategoryType.STICKS, CategoryType.fromIdentifier("JOYSTICK"))
        assertEquals(CategoryType.STICKS, CategoryType.fromIdentifier("STICKS"))
        assertEquals(CategoryType.STICKS, CategoryType.fromIdentifier("STICK"))
        assertEquals(CategoryType.STICKS, CategoryType.fromIdentifier("LS"))
        assertEquals(CategoryType.STICKS, CategoryType.fromIdentifier("L3"))

        assertEquals(CategoryType.DPAD, CategoryType.fromIdentifier("DPAD"))
        assertEquals(CategoryType.DPAD, CategoryType.fromIdentifier("CROSS"))
        assertEquals(CategoryType.DPAD, CategoryType.fromIdentifier("UP"))

        assertEquals(CategoryType.MACROS, CategoryType.fromIdentifier("MACRO"))
        assertEquals(CategoryType.MACROS, CategoryType.fromIdentifier("MACROS"))
        assertEquals(CategoryType.MACROS, CategoryType.fromIdentifier("M1"))

        assertEquals(CategoryType.SYSTEM, CategoryType.fromIdentifier("SYSTEM"))
        assertEquals(CategoryType.SYSTEM, CategoryType.fromIdentifier("HOME"))
        assertEquals(CategoryType.SYSTEM, CategoryType.fromIdentifier("GUIDE"))
    }

    @Test
    fun testCategoryManagerGetCategoryResolvesAliases() {
        assertEquals(CategoryType.ABXY, CategoryManager.getCategory("BUTTON")?.type)
        assertEquals(CategoryType.ABXY, CategoryManager.getCategory("button")?.type)
        assertEquals(CategoryType.TRIGGERS, CategoryManager.getCategory("TRIGGER")?.type)
        assertEquals(CategoryType.TRIGGERS, CategoryManager.getCategory("TRIGGERS")?.type)
        assertEquals(CategoryType.BUMPERS, CategoryManager.getCategory("BUMPER")?.type)
        assertEquals(CategoryType.BUMPERS, CategoryManager.getCategory("BUMPERS")?.type)
        assertEquals(CategoryType.STICKS, CategoryManager.getCategory("JOYSTICK")?.type)
        assertEquals(CategoryType.STICKS, CategoryManager.getCategory("STICK")?.type)
        assertEquals(CategoryType.MACROS, CategoryManager.getCategory("MACRO")?.type)
    }

    @Test
    fun testCategoryManagerGetControlsForCategoryWithAliases() {
        val buttonControls = CategoryManager.getControlsForCategory("BUTTON")
        assertEquals(listOf("A", "B", "X", "Y"), buttonControls.map { it.key })

        val triggerControls = CategoryManager.getControlsForCategory("TRIGGER")
        assertEquals(listOf("LT", "RT"), triggerControls.map { it.key })

        val stickControls = CategoryManager.getControlsForCategory("JOYSTICK")
        assertEquals(listOf("LS", "RS"), stickControls.map { it.key })
    }

    @Test
    fun testCategoryDefinitionKeysAndSubcategories() {
        val triggers = CategoryManager.getCategory(CategoryType.TRIGGERS)
        assertEquals(triggers.controls, triggers.subCategories)
        assertTrue(triggers.keys.containsAll(listOf("LT", "RT", "L2", "R2", "TRIGGER", "TRIGGERS", "ANALOG_TRIGGER")))

        val bumpers = CategoryManager.getCategory(CategoryType.BUMPERS)
        assertEquals(bumpers.controls, bumpers.subCategories)
        assertTrue(bumpers.keys.containsAll(listOf("LB", "RB", "L1", "R1", "BUMPER", "BUMPERS", "SHOULDER")))

        val sticks = CategoryManager.getCategory(CategoryType.STICKS)
        assertTrue(sticks.keys.containsAll(listOf("LS", "RS", "L3", "R3", "JOYSTICK", "STICK", "STICKS")))
    }

    @Test
    fun testNxprcCategoryBidirectionalMapping() {
        assertEquals(CategoryType.ABXY, com.sanket.tools.nexpad.nxprc.NxprcCategory.BUTTON.categoryType)
        assertEquals(CategoryType.DPAD, com.sanket.tools.nexpad.nxprc.NxprcCategory.DPAD.categoryType)
        assertEquals(CategoryType.TRIGGERS, com.sanket.tools.nexpad.nxprc.NxprcCategory.TRIGGER.categoryType)
        assertEquals(CategoryType.BUMPERS, com.sanket.tools.nexpad.nxprc.NxprcCategory.BUMPER.categoryType)
        assertEquals(CategoryType.STICKS, com.sanket.tools.nexpad.nxprc.NxprcCategory.JOYSTICK.categoryType)
        assertEquals(CategoryType.SYSTEM, com.sanket.tools.nexpad.nxprc.NxprcCategory.SYSTEM.categoryType)
        assertEquals(CategoryType.MACROS, com.sanket.tools.nexpad.nxprc.NxprcCategory.MACRO.categoryType)

        assertEquals(com.sanket.tools.nexpad.nxprc.NxprcCategory.TRIGGER, com.sanket.tools.nexpad.nxprc.NxprcCategory.fromId("TRIGGERS"))
        assertEquals(com.sanket.tools.nexpad.nxprc.NxprcCategory.JOYSTICK, com.sanket.tools.nexpad.nxprc.NxprcCategory.fromId("STICKS"))
        assertEquals(com.sanket.tools.nexpad.nxprc.NxprcCategory.BUMPER, com.sanket.tools.nexpad.nxprc.NxprcCategory.fromId("BUMPERS"))
        assertEquals(com.sanket.tools.nexpad.nxprc.NxprcCategory.TRIGGER, com.sanket.tools.nexpad.nxprc.NxprcCategory.fromId("LT"))
    }
}
