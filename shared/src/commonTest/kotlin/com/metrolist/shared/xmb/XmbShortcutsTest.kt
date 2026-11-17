package com.metrolist.shared.xmb

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import com.metrolist.shared.xmb.settings.XmbDateFormat
import com.metrolist.shared.xmb.settings.XmbKeyChord
import com.metrolist.shared.xmb.settings.XmbPrefs
import com.metrolist.shared.xmb.settings.XmbShortcut
import com.metrolist.shared.xmb.settings.XmbShortcutCapture
import com.metrolist.shared.xmb.settings.XmbShortcuts
import com.metrolist.shared.xmb.settings.xmbClockText
import com.metrolist.shared.xmb.settings.xmbKeyFromName
import com.metrolist.shared.xmb.settings.xmbKeyName
import com.metrolist.shared.xmb.settings.xmbShortcutCapture
import com.metrolist.shared.xmb.theme.XmbPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XmbShortcutsTest {
    @Test
    fun chordsSaveAndReadBackByName() {
        val chord = XmbKeyChord(Key.DirectionRight, ctrl = true, shift = true)
        assertEquals("Ctrl+Shift+Right", chord.label)
        assertEquals(chord, XmbKeyChord.parse(chord.label))
        assertEquals(XmbKeyChord(Key.Semicolon), XmbKeyChord.parse("Semicolon"))
        assertNull(XmbKeyChord.parse("Hyper+A"))
        assertNull(XmbKeyChord.parse("NoSuchKey"))
        // Keys without a name keep their code.
        val odd = Key(987654L)
        assertEquals(odd, xmbKeyFromName(xmbKeyName(odd)))
    }

    @Test
    fun everyDefaultSurvivesSavingAndNoKeyIsUsedTwice() {
        assertEquals(XmbShortcuts.Default, XmbShortcuts.decode(XmbShortcuts.Default.encode()))
        assertEquals(XmbShortcuts.Default, XmbShortcuts.decode(""))
        val chords = XmbShortcuts.defaults.values
        assertEquals(chords.size, chords.toSet().size)
        assertTrue(chords.none { it.isReserved })
        assertEquals(XmbShortcut.PLAY_PAUSE, XmbShortcuts.Default.match(XmbKeyChord(Key.Spacebar)))
    }

    @Test
    fun bindingMovesAKeyAndClearingSticks() {
        val v = XmbKeyChord(Key.V)
        val moved = XmbShortcuts.Default.bind(XmbShortcut.SHUFFLE, v)
        assertEquals(XmbShortcut.SHUFFLE, moved.match(v))
        assertNull(moved[XmbShortcut.VISUALIZER])
        val saved = XmbShortcuts.decode(moved.encode())
        assertEquals(moved, saved)
        // A cleared shortcut stays cleared rather than falling back to its default.
        assertNull(saved[XmbShortcut.VISUALIZER])
        // An older save without a newer shortcut gets that shortcut's default.
        assertEquals(XmbShortcuts.defaults[XmbShortcut.SEARCH], XmbShortcuts.decode("SHUFFLE=V")[XmbShortcut.SEARCH])
    }

    @Test
    fun recordingWaitsForTheKeyAndKeepsMenuKeysFree() {
        assertEquals(XmbShortcutCapture.Wait, xmbShortcutCapture(XmbKeyChord(Key.CtrlLeft, ctrl = true)))
        assertEquals(XmbShortcutCapture.Cancel, xmbShortcutCapture(XmbKeyChord(Key.Escape)))
        assertEquals(XmbShortcutCapture.Clear, xmbShortcutCapture(XmbKeyChord(Key.Backspace)))
        assertEquals(XmbShortcutCapture.Reserved, xmbShortcutCapture(XmbKeyChord(Key.DirectionUp)))
        assertEquals(XmbShortcutCapture.Reserved, xmbShortcutCapture(XmbKeyChord(Key.Enter)))
        val chord = XmbKeyChord(Key.DirectionUp, alt = true)
        assertEquals(XmbShortcutCapture.Bind(chord), xmbShortcutCapture(chord))
        assertEquals(XmbShortcutCapture.Bind(XmbKeyChord(Key.Spacebar)), xmbShortcutCapture(XmbKeyChord(Key.Spacebar)))
    }

    @Test
    fun colorChoicesPickTheBackground() {
        val day = 12f
        val night = 23f
        val original = XmbPrefs().backgroundColors(month = 3, hour = day)
        assertEquals(XmbPalette.backgroundColors(3, day), original)
        // A fixed month keeps its color in any month.
        assertEquals(XmbPalette.backgroundColors(8, day), XmbPrefs(color = "8").backgroundColors(month = 3, hour = day))
        // Dim at night off keeps daytime brightness.
        assertEquals(XmbPalette.backgroundColors(3, day), XmbPrefs(dimAtNight = false).backgroundColors(3, night))
        assertNotEquals(original, XmbPrefs().backgroundColors(3, night))
        // A custom hue: red-ish at 0, blue-ish at 220.
        val red = XmbPrefs(color = XmbPrefs.CUSTOM_COLOR, customHue = 0f).backgroundColors(3, day).base
        val blue = XmbPrefs(color = XmbPrefs.CUSTOM_COLOR, customHue = 220f).backgroundColors(3, day).base
        assertTrue(red.red > red.blue && blue.blue > blue.red)
        assertNotEquals(Color.Unspecified, red)
        // Only an image background with a file shows one.
        assertNull(XmbPrefs(wallpaper = "/a.png").wallpaperPath)
        assertEquals("/a.png", XmbPrefs(background = XmbPrefs.IMAGE_BACKGROUND, wallpaper = "/a.png").wallpaperPath)
    }

    @Test
    fun clockFormats() {
        assertEquals("11/17 21:05", xmbClockText(11, 17, 21, 5, hour24 = true, XmbDateFormat.MONTH_DAY))
        assertEquals("17/11 9:05 PM", xmbClockText(11, 17, 21, 5, hour24 = false, XmbDateFormat.DAY_MONTH))
        assertEquals("12:00 AM", xmbClockText(11, 17, 0, 0, hour24 = false, XmbDateFormat.HIDDEN))
        assertEquals("12:30 PM", xmbClockText(11, 17, 12, 30, hour24 = false, XmbDateFormat.HIDDEN))
    }
}
