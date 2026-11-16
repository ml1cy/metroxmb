package com.metrolist.shared.xmb

import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.nav.XmbFeedback
import com.metrolist.shared.xmb.nav.XmbPicker
import com.metrolist.shared.xmb.nav.reduce
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class XmbPickerTest {
    private val applied = mutableListOf<Any>()
    private val choice = XmbPicker.Choice("Theme", listOf("Light", "Dark", "System"), focus = 1) { applied += it }

    @Test
    fun choiceMovesWithinItsValuesWithoutWrapping() {
        val up = choice.reduce(XmbAction.UP)
        assertEquals(0, (up.picker as XmbPicker.Choice).focus)
        assertEquals(XmbFeedback.MOVE_UP, up.feedback)
        val top = (up.picker as XmbPicker.Choice).reduce(XmbAction.UP)
        assertSame(up.picker, top.picker)
        assertEquals(XmbFeedback.NONE, top.feedback)
        val down = choice.reduce(XmbAction.DOWN).picker!!.reduce(XmbAction.DOWN)
        assertEquals(XmbFeedback.NONE, down.feedback)
        assertEquals(2, (down.picker as XmbPicker.Choice).focus)
    }

    @Test
    fun nothingChangesUntilConfirmed() {
        val moved = choice.reduce(XmbAction.DOWN).picker!!
        assertEquals(emptyList(), applied)
        val confirmed = moved.reduce(XmbAction.SELECT)
        assertNull(confirmed.picker)
        assertEquals(XmbFeedback.SELECT, confirmed.feedback)
        confirmed.commit!!.invoke()
        assertEquals(listOf<Any>(2), applied)
    }

    @Test
    fun backAndLeftCancel() {
        listOf(XmbAction.BACK, XmbAction.LEFT).forEach { action ->
            val step = choice.reduce(action)
            assertNull(step.picker)
            assertNull(step.commit)
            assertEquals(XmbFeedback.BACK, step.feedback)
        }
    }

    @Test
    fun sliderStepsSnapAndStopAtTheEnds() {
        val slider = XmbPicker.Slider("Speed", 1f, 0.5f..2f, steps = 5, format = null) { applied += it }
        assertEquals(0.25f, slider.step)
        val right = slider.reduce(XmbAction.RIGHT)
        assertEquals(1.25f, (right.picker as XmbPicker.Slider).value, 0.0001f)
        assertEquals(XmbFeedback.MOVE_SIDE, right.feedback)
        val atEnd = XmbPicker.Slider("Speed", 2f, 0.5f..2f, steps = 5, format = null) {}
        assertEquals(XmbFeedback.NONE, atEnd.reduce(XmbAction.RIGHT).feedback)
        val down = slider.reduce(XmbAction.DOWN)
        assertEquals(0.75f, (down.picker as XmbPicker.Slider).value, 0.0001f)
        down.picker!!
            .reduce(XmbAction.SELECT)
            .commit!!
            .invoke()
        assertEquals(0.75f, applied.single() as Float, 0.0001f)
    }

    @Test
    fun continuousSlidersMoveInTwentiethsAndSnapToThatGrid() {
        val slider = XmbPicker.Slider("Volume", 0.33f, 0f..1f, steps = 0, format = null) {}
        assertEquals(0.05f, slider.step, 0.0001f)
        assertEquals(0.4f, (slider.reduce(XmbAction.RIGHT).picker as XmbPicker.Slider).value, 0.0001f)
    }
}
