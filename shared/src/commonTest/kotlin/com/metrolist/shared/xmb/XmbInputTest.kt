package com.metrolist.shared.xmb

import androidx.compose.ui.input.key.Key
import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.nav.XmbFeedback
import com.metrolist.shared.xmb.nav.xmbActionFor
import com.metrolist.shared.xmb.nav.xmbActionForScroll
import com.metrolist.shared.xmb.sound.XmbSound
import com.metrolist.shared.xmb.sound.sound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class XmbInputTest {
    @Test
    fun mapsKeyboardControllerAndRemoteKeys() {
        val expected =
            mapOf(
                Key.DirectionUp to XmbAction.UP,
                Key.DirectionDown to XmbAction.DOWN,
                Key.DirectionLeft to XmbAction.LEFT,
                Key.DirectionRight to XmbAction.RIGHT,
                Key.Enter to XmbAction.SELECT,
                Key.NumPadEnter to XmbAction.SELECT,
                Key.DirectionCenter to XmbAction.SELECT,
                Key.ButtonA to XmbAction.SELECT,
                Key.Escape to XmbAction.BACK,
                Key.Backspace to XmbAction.BACK,
                Key.Back to XmbAction.BACK,
                Key.ButtonB to XmbAction.BACK,
                Key.Tab to XmbAction.OPTIONS,
                Key.Menu to XmbAction.OPTIONS,
                Key.ButtonY to XmbAction.OPTIONS,
            )
        expected.forEach { (key, action) -> assertEquals(action, xmbActionFor(key), key.toString()) }
        for (key in listOf(Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause)) {
            assertEquals(XmbAction.PLAY_PAUSE, xmbActionFor(key), key.toString())
        }
        // Space plays and pauses as a keyboard shortcut (XmbShortcutsTest), so it can be remapped.
        assertNull(xmbActionFor(Key.Spacebar))
        assertEquals(XmbAction.NEXT_TRACK, xmbActionFor(Key.MediaNext))
        assertEquals(XmbAction.PREVIOUS_TRACK, xmbActionFor(Key.MediaPrevious))
        for (key in listOf(Key.A, Key.F5, Key.ShiftLeft)) assertNull(xmbActionFor(key), key.toString())
    }

    @Test
    fun scrollingFollowsTheDominantAxis() {
        assertEquals(XmbAction.DOWN, xmbActionForScroll(0f, 1f))
        assertEquals(XmbAction.UP, xmbActionForScroll(0.4f, -1f))
        assertEquals(XmbAction.RIGHT, xmbActionForScroll(2f, 1f))
        assertEquals(XmbAction.LEFT, xmbActionForScroll(-2f, 0f))
        assertNull(xmbActionForScroll(0f, 0f))
    }

    @Test
    fun feedbackPlaysTheMatchingSound() {
        assertNull(XmbFeedback.NONE.sound())
        assertEquals(XmbSound.NAV_UP, XmbFeedback.MOVE_UP.sound())
        assertEquals(XmbSound.NAV_DOWN, XmbFeedback.MOVE_DOWN.sound())
        assertEquals("nav_down.ogg", XmbFeedback.MOVE_SIDE.sound()?.file)
        assertEquals(XmbSound.SELECT, XmbFeedback.SELECT.sound())
        assertEquals("back.ogg", XmbFeedback.ERROR.sound()?.file)
        assertEquals("files/xmb/sounds/select.ogg", XmbSound.SELECT.resourcePath)
        assertEquals(false, XmbSound.BOOT in XmbSound.effects || XmbSound.MENU_BGM in XmbSound.effects)
    }
}
