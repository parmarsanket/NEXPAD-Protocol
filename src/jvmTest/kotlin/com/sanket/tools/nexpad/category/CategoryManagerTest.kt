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
    fun testDpadCardinalDirections() {
        val dpad = CategoryManager.getCategory("DPAD")
        assertNotNull(dpad)
        val keys = dpad.controls.map { it.key }
        assertEquals(listOf("UP", "DOWN", "LEFT", "RIGHT"), keys)
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
}
