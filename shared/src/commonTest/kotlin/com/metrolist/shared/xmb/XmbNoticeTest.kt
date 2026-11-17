package com.metrolist.shared.xmb

import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.nav.XmbFeedback
import com.metrolist.shared.xmb.nav.XmbNotice
import com.metrolist.shared.xmb.nav.reduce
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class XmbNoticeTest {
    private val notice = XmbNotice(id = "n", message = "Signed out", buttons = listOf("Log in", "Not now"), cancel = 1)

    @Test
    fun focusMovesAcrossButtonsWithoutWrapping() {
        assertEquals(1, notice.reduce(XmbAction.RIGHT, 0).focus)
        assertEquals(XmbFeedback.MOVE_SIDE, notice.reduce(XmbAction.DOWN, 0).feedback)
        val end = notice.reduce(XmbAction.RIGHT, 1)
        assertEquals(1, end.focus)
        assertEquals(XmbFeedback.NONE, end.feedback)
        assertEquals(0, notice.reduce(XmbAction.LEFT, 1).focus)
        assertEquals(0, notice.reduce(XmbAction.UP, 0).focus)
    }

    @Test
    fun selectChoosesTheFocusedButton() {
        assertEquals(0, notice.reduce(XmbAction.SELECT, 0).chosen)
        assertEquals(1, notice.reduce(XmbAction.SELECT, 1).chosen)
    }

    @Test
    fun backChoosesCancelOrErrorsWithoutOne() {
        assertEquals(1, notice.reduce(XmbAction.BACK, 0).chosen)
        val stuck = notice.copy(cancel = null).reduce(XmbAction.BACK, 0)
        assertNull(stuck.chosen)
        assertEquals(XmbFeedback.ERROR, stuck.feedback)
    }

    @Test
    fun otherKeysAreSwallowed() {
        for (action in listOf(XmbAction.OPTIONS, XmbAction.PLAY_PAUSE, XmbAction.NEXT_TRACK, XmbAction.PREVIOUS_TRACK)) {
            val step = notice.reduce(action, 1)
            assertEquals(1, step.focus)
            assertNull(step.chosen)
        }
    }
}
