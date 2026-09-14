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
        assertTrue(abxy.isGroupCluster)
    }

    @Test
    fun testDpadCrossAndDirections() {
        val dpad = CategoryManager.getCategory("DPAD")
        assertNotNull(dpad)
        val keys = dpad.controls.map { it.key }
        assertTrue(keys.containsAll(listOf("DPAD", "UP", "DOWN", "LEFT", "RIGHT")))
        
        val cross = CategoryManager.getControl("DPAD")
        assertNotNull(cross)
        assertEquals(140, cross.defaultWidthDp)
        assertEquals(140, cross.defaultHeightDp)
        assertEquals(ComponentType.DPAD, cross.componentType)
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
}
