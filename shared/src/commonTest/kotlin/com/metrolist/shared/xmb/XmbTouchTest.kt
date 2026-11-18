package com.metrolist.shared.xmb

import androidx.compose.ui.unit.dp
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.nav.XmbIcon
import com.metrolist.shared.xmb.nav.XmbLevel
import com.metrolist.shared.xmb.nav.XmbNavigation
import com.metrolist.shared.xmb.nav.XmbTouchGesture
import com.metrolist.shared.xmb.nav.XmbTouchTracker
import com.metrolist.shared.xmb.nav.focusCategory
import com.metrolist.shared.xmb.nav.focusRow
import com.metrolist.shared.xmb.theme.XmbIcons
import com.metrolist.shared.xmb.theme.XmbMetrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XmbTouchTest {
    private fun tracker() = XmbTouchTracker(stepX = 100f, stepY = 50f, slop = 10f)

    @Test
    fun draggingUpStepsDownTheListOnePerItem() {
        val touch = tracker()
        touch.down(500f, 500f, 0L)
        assertEquals(emptyList(), touch.move(500f, 495f), "inside the slop")
        // Half a step past the slop is the first step, then one per item spacing.
        assertEquals(listOf(XmbAction.DOWN), touch.move(500f, 460f))
        assertEquals(emptyList(), touch.move(500f, 450f))
        assertEquals(listOf(XmbAction.DOWN, XmbAction.DOWN), touch.move(500f, 350f))
        // Back the other way undoes steps.
        assertEquals(listOf(XmbAction.UP, XmbAction.UP), touch.move(500f, 450f))
        assertNull(touch.up(300L), "a drag isn't a tap")
    }

    @Test
    fun draggingLeftMovesToTheNextCategoryAndTheAxisStaysPut() {
        val touch = tracker()
        touch.down(500f, 500f, 0L)
        assertEquals(listOf(XmbAction.RIGHT), touch.move(430f, 505f))
        // Vertical wobble after the axis is set doesn't move items.
        assertEquals(emptyList(), touch.move(420f, 560f))
        // Dragging back toward the start undoes the step.
        assertEquals(listOf(XmbAction.LEFT), touch.move(480f, 560f))
    }

    @Test
    fun aTouchThatMadeNoStepIsATapEvenIfItWobbled() {
        val still = tracker().apply { down(500f, 500f, 0L) }
        assertEquals(XmbTouchGesture.TAP, still.up(120L))
        // Past the slop but short of a step: still a tap, not a lost touch.
        val wobbly = tracker().apply { down(500f, 500f, 0L) }
        assertEquals(emptyList(), wobbly.move(500f, 485f))
        assertEquals(XmbTouchGesture.TAP, wobbly.up(150L))
        val held = tracker().apply { down(500f, 500f, 0L) }
        assertEquals(XmbTouchGesture.LONG_PRESS, held.up(700L))
    }

    @Test
    fun aFlickCoastsOnButASlowReleaseStops() {
        val flick = tracker().apply { down(500f, 500f, 0L) }
        flick.move(500f, 440f)
        assertTrue(flick.vertical)
        // 2,000 px/s upward over 0.18 s is 360 px: 7 more items down.
        assertEquals(List(7) { XmbAction.DOWN }, flick.flingSteps(-2000f))
        assertEquals(List(12) { XmbAction.DOWN }, flick.flingSteps(-20000f), "capped")
        assertEquals(emptyList(), flick.flingSteps(-200f), "a slow release")
        val tap = tracker().apply { down(500f, 500f, 0L) }
        assertEquals(emptyList(), tap.flingSteps(-2000f), "no drag, no flick")
    }

    @Test
    fun tapsFocusRowsAndCategoriesDirectly() {
        val start = XmbNavigation.initial(listOf(3, 5, 9), 1)
        assertEquals(2, start.focusCategory(2).category)
        assertEquals(1, start.focusCategory(7).category, "no such category")
        assertEquals(4, start.focusRow(4).item)
        val level = XmbLevel(XmbList.Home, "Home", XmbIcon.Vector(XmbIcons.Theme))
        val opened = start.push(level)
        assertEquals(6, opened.focusRow(6).level?.focus)
        assertEquals(1, opened.focusCategory(0).category, "categories don't change inside a folder")
    }

    @Test
    fun uprightPhonesGetLegibleSizes() {
        val phone = XmbMetrics.forSize(411.dp, 891.dp)
        assertTrue(phone.portrait)
        assertTrue(phone.itemTitleSize > 17.dp, "titles ${phone.itemTitleSize}")
        assertEquals(XmbMetrics.PORTRAIT_ANCHOR_X, phone.anchorX)
        // Values start where they still fit on screen.
        val valueStart = 411.dp * phone.anchorX + phone.itemIcon / 2 + phone.itemTextOffset + phone.valueColumn
        assertTrue(valueStart < 411.dp - 40.dp, "values start at $valueStart")
        val desktop = XmbMetrics.forSize(1280.dp, 720.dp)
        assertFalse(desktop.portrait)
        assertEquals(XmbMetrics.ANCHOR_X, desktop.anchorX)
    }
}
