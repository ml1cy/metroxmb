package com.metrolist.shared.xmb

import androidx.compose.ui.unit.dp
import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.nav.XmbTouchTracker
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
        assertNull(touch.up(500f, 450f, 300L, 500f, 500f, 300f), "a drag isn't a tap")
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
    fun tapsSelectOrStepTowardTheTap() {
        fun tap(
            x: Float,
            y: Float,
            heldMs: Long = 80L,
        ) = tracker().run {
            down(x, y, 0L)
            up(x, y, heldMs, focusX = 300f, focusY = 500f, rowY = 300f)
        }
        assertEquals(XmbAction.SELECT, tap(700f, 520f), "anywhere along the focused row")
        assertEquals(XmbAction.DOWN, tap(300f, 600f))
        assertEquals(XmbAction.UP, tap(300f, 420f))
        assertEquals(XmbAction.RIGHT, tap(450f, 310f), "on the category row: along the row")
        assertEquals(XmbAction.LEFT, tap(150f, 290f))
        assertNull(tap(320f, 300f), "the focused category itself")
        assertEquals(XmbAction.OPTIONS, tap(300f, 500f, heldMs = 700L), "a long press")
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
