package com.metrolist.shared.xmb

import com.metrolist.shared.xmb.nav.XMB_KEYBOARD_MAX_LENGTH
import com.metrolist.shared.xmb.nav.XMB_KEY_COLUMNS
import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.nav.XmbFeedback
import com.metrolist.shared.xmb.nav.XmbKey
import com.metrolist.shared.xmb.nav.XmbKeyboard
import com.metrolist.shared.xmb.nav.XmbKeyboardStep
import com.metrolist.shared.xmb.nav.keyAt
import com.metrolist.shared.xmb.nav.reduce
import com.metrolist.shared.xmb.nav.xmbKeyRows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class XmbKeyboardTest {
    private val keyboard = XmbKeyboard(title = "Search")

    private fun XmbKeyboard.press(vararg actions: XmbAction): XmbKeyboardStep {
        var step = XmbKeyboardStep(this, XmbFeedback.NONE)
        actions.forEach { step = checkNotNull(step.keyboard).reduce(it) }
        return step
    }

    @Test
    fun everyRowFillsTheGrid() {
        xmbKeyRows.forEach { row -> assertEquals(XMB_KEY_COLUMNS, row.sumOf { it.span }) }
    }

    @Test
    fun startsOnQAndTypesTheFocusedKey() {
        assertEquals(XmbKey.Character('q'), keyboard.focusedKey)
        val step = keyboard.press(XmbAction.RIGHT, XmbAction.SELECT)
        assertEquals("w", step.keyboard?.text)
        assertEquals(XmbFeedback.SELECT, step.feedback)
    }

    @Test
    fun wideKeysTakeOneStepAcrossAndKeepTheColumnGoingUp() {
        // Down from 'f' (column 3) lands on Shift's row: column 3 is under Space, which starts at 2.
        val bottom =
            keyboard
                .press(
                    XmbAction.DOWN,
                    XmbAction.RIGHT,
                    XmbAction.RIGHT,
                    XmbAction.RIGHT,
                    XmbAction.DOWN,
                    XmbAction.DOWN,
                ).keyboard!!
        assertEquals(XmbKey.Space, bottom.focusedKey)
        assertEquals(2, bottom.column)
        assertEquals(XmbKey.Delete, bottom.reduce(XmbAction.RIGHT).keyboard!!.focusedKey)
        assertEquals(XmbKey.Shift, bottom.reduce(XmbAction.LEFT).keyboard!!.focusedKey)
        assertEquals(XmbKey.Character('c'), bottom.reduce(XmbAction.UP).keyboard!!.focusedKey)
        assertEquals(XmbKey.Space, keyAt(4, 5))
    }

    @Test
    fun shiftCapitalisesOneLetter() {
        val shifted = XmbKeyboard(title = "", row = 4, column = 0).reduce(XmbAction.SELECT).keyboard!!
        val typed = shifted.copy(row = 1, column = 0).reduce(XmbAction.SELECT).keyboard!!
        assertEquals("Q", typed.text)
        assertEquals("Qq", typed.reduce(XmbAction.SELECT).keyboard!!.text)
    }

    @Test
    fun physicalTypingMovesFocusToDoneSoEnterSubmits() {
        val typed = keyboard.type('a').type('b')
        assertEquals("ab", typed.text)
        assertEquals(XmbKey.Done, typed.focusedKey)
        val done = typed.reduce(XmbAction.SELECT)
        assertNull(done.keyboard)
        assertEquals("ab", done.submitted)
        assertEquals("a", typed.deleteLast().text)
        assertEquals("ab", typed.type('\n').text)
    }

    @Test
    fun deleteOnEmptyTextIsAnError() {
        assertEquals(XmbFeedback.ERROR, XmbKeyboard(title = "", row = 4, column = 6).reduce(XmbAction.SELECT).feedback)
    }

    @Test
    fun textStopsAtTheMaximumLength() {
        val full = XmbKeyboard(title = "", text = "a".repeat(XMB_KEYBOARD_MAX_LENGTH))
        assertEquals(XMB_KEYBOARD_MAX_LENGTH, full.type('b').text.length)
        assertEquals(XmbFeedback.ERROR, full.reduce(XmbAction.SELECT).feedback)
    }

    @Test
    fun rightFromTheLastColumnEntersSuggestionsAndSelectingOneSubmitsIt() {
        val withSuggestions = XmbKeyboard(title = "", row = 1, column = 9, suggestions = listOf("alpha", "beta"))
        val inside = withSuggestions.reduce(XmbAction.RIGHT).keyboard!!
        assertEquals(1, inside.suggestion)
        assertNull(inside.focusedKey)
        val submitted = inside.reduce(XmbAction.UP).keyboard!!.reduce(XmbAction.SELECT)
        assertEquals("alpha", submitted.submitted)
        val back = inside.reduce(XmbAction.LEFT).keyboard!!
        assertEquals(-1, back.suggestion)
        assertEquals(XmbKey.Character('p'), back.focusedKey)
        assertEquals(XmbFeedback.NONE, XmbKeyboard(title = "", row = 1, column = 9).reduce(XmbAction.RIGHT).feedback)
    }

    @Test
    fun suggestionFocusSurvivesAShorterList() {
        val inside = XmbKeyboard(title = "", suggestions = listOf("a", "b", "c"), suggestion = 2)
        assertEquals(0, inside.withSuggestions(listOf("a")).suggestion)
        assertEquals(-1, keyboard.withSuggestions(listOf("a")).suggestion)
    }

    @Test
    fun backClosesWithoutSubmitting() {
        val step = keyboard.type('x').reduce(XmbAction.BACK)
        assertNull(step.keyboard)
        assertNull(step.submitted)
    }
}
