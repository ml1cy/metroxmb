package com.metrolist.shared.xmb

import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.nav.XmbFeedback
import com.metrolist.shared.xmb.nav.XmbNavigation
import com.metrolist.shared.xmb.nav.XmbSelection
import com.metrolist.shared.xmb.nav.reduce
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class XmbNavigationTest {
    private val counts = listOf(1, 9, 8, 0)

    private fun XmbNavigation.run(vararg actions: XmbAction): Pair<XmbNavigation, List<XmbFeedback>> {
        var navigation = this
        val feedback =
            actions.map { action ->
                val step = navigation.reduce(action, counts)
                navigation = step.navigation
                step.feedback
            }
        return navigation to feedback
    }

    @Test
    fun startsOnTheRequestedCategoryWithEveryListAtItsTop() {
        val navigation = XmbNavigation.initial(counts, category = 2)
        assertEquals(2, navigation.category)
        assertEquals(listOf(0, 0, 0, 0), navigation.items)
        assertEquals(3, XmbNavigation.initial(counts, category = 99).category)
        assertFailsWith<IllegalArgumentException> { XmbNavigation.initial(emptyList(), 0) }
    }

    @Test
    fun movesThroughItemsAndStopsAtEitherEndWithoutSound() {
        val (navigation, feedback) = XmbNavigation.initial(counts, 2).run(XmbAction.UP, XmbAction.DOWN, XmbAction.DOWN, XmbAction.UP)
        assertEquals(1, navigation.item)
        assertEquals(listOf(XmbFeedback.NONE, XmbFeedback.MOVE_DOWN, XmbFeedback.MOVE_DOWN, XmbFeedback.MOVE_UP), feedback)

        val atBottom = XmbNavigation(2, listOf(0, 0, 7, 0))
        assertEquals(XmbFeedback.NONE, atBottom.reduce(XmbAction.DOWN, counts).feedback)
        assertEquals(atBottom, atBottom.reduce(XmbAction.DOWN, counts).navigation)
    }

    @Test
    fun movesBetweenCategoriesWithoutWrapping() {
        val (left, leftFeedback) = XmbNavigation.initial(counts, 1).run(XmbAction.LEFT, XmbAction.LEFT)
        assertEquals(0, left.category)
        assertEquals(listOf(XmbFeedback.MOVE_SIDE, XmbFeedback.NONE), leftFeedback)

        val (right, rightFeedback) = XmbNavigation.initial(counts, 2).run(XmbAction.RIGHT, XmbAction.RIGHT)
        assertEquals(3, right.category)
        assertEquals(listOf(XmbFeedback.MOVE_SIDE, XmbFeedback.NONE), rightFeedback)
    }

    @Test
    fun eachCategoryRemembersItsOwnItem() {
        val (navigation, _) =
            XmbNavigation
                .initial(counts, 1)
                .run(XmbAction.DOWN, XmbAction.DOWN, XmbAction.RIGHT, XmbAction.DOWN, XmbAction.LEFT)
        assertEquals(1, navigation.category)
        assertEquals(2, navigation.item)
        assertEquals(listOf(0, 2, 1, 0), navigation.items)
    }

    @Test
    fun selectReportsTheFocusedItemOrAnErrorForEmptyCategories() {
        val step = XmbNavigation(1, listOf(0, 4, 0, 0)).reduce(XmbAction.SELECT, counts)
        assertEquals(XmbFeedback.SELECT, step.feedback)
        assertEquals(XmbSelection(1, 4), step.selected)

        val empty = XmbNavigation(3, listOf(0, 0, 0, 0)).reduce(XmbAction.SELECT, counts)
        assertEquals(XmbFeedback.ERROR, empty.feedback)
        assertNull(empty.selected)
        assertEquals(XmbFeedback.NONE, empty.navigation.reduce(XmbAction.DOWN, counts).feedback)
    }

    @Test
    fun backAndOptionsDoNothingAtTheTopLevel() {
        val navigation = XmbNavigation(2, listOf(0, 0, 3, 0))
        for (action in listOf(XmbAction.BACK, XmbAction.OPTIONS)) {
            val step = navigation.reduce(action, counts)
            assertEquals(navigation, step.navigation)
            assertEquals(XmbFeedback.NONE, step.feedback)
        }
    }
}
