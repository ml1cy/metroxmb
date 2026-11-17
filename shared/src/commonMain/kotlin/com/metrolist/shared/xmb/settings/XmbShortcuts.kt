package com.metrolist.shared.xmb.settings

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import com.metrolist.shared.xmb.nav.xmbActionFor

/** What a keyboard shortcut does, in the Keyboard Shortcuts page's order. */
enum class XmbShortcut {
    PLAY_PAUSE,
    NEXT_TRACK,
    PREVIOUS_TRACK,
    SEEK_FORWARD,
    SEEK_BACK,
    SHUFFLE,
    REPEAT,
    VOLUME_UP,
    VOLUME_DOWN,
    MUTE,
    LIKE,
    VISUALIZER,
    LYRICS,
    SEARCH,
}

/**
 * A key and the modifiers held with it. Saved and shown by name ("Ctrl+Right"), so a saved
 * shortcut means the same key on every platform. Cmd counts as Ctrl on a Mac.
 */
data class XmbKeyChord(
    val key: Key,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
) {
    val hasModifier: Boolean get() = ctrl || shift || alt

    /** "Ctrl+Shift+Right": how it's shown and saved. */
    val label: String
        get() =
            buildString {
                if (ctrl) append("Ctrl+")
                if (shift) append("Shift+")
                if (alt) append("Alt+")
                append(xmbKeyName(key))
            }

    /** Keys the menu moves with (arrows, Enter, Esc...) and media keys can't be taken on their own. */
    val isReserved: Boolean get() = !hasModifier && xmbActionFor(key) != null

    companion object {
        fun of(event: KeyEvent): XmbKeyChord =
            XmbKeyChord(
                event.key,
                ctrl = event.isCtrlPressed || event.isMetaPressed,
                shift = event.isShiftPressed,
                alt = event.isAltPressed,
            )

        fun parse(text: String): XmbKeyChord? {
            val parts = text.split('+')
            val key = xmbKeyFromName(parts.last()) ?: return null
            val modifiers = parts.dropLast(1).map(String::lowercase)
            if (modifiers.any { it !in MODIFIER_NAMES }) return null
            return XmbKeyChord(key, ctrl = "ctrl" in modifiers, shift = "shift" in modifiers, alt = "alt" in modifiers)
        }

        private val MODIFIER_NAMES = setOf("ctrl", "shift", "alt")
    }
}

/**
 * Each shortcut's key, or none. Saved as "NAME=Chord" pairs joined by ';' (no key name contains
 * either); a shortcut missing from the text keeps its default, an empty chord means none.
 */
class XmbShortcuts(
    private val bindings: Map<XmbShortcut, XmbKeyChord?>,
) {
    operator fun get(shortcut: XmbShortcut): XmbKeyChord? = bindings[shortcut]

    /** The shortcut on [chord], if any. */
    fun match(chord: XmbKeyChord): XmbShortcut? = XmbShortcut.entries.firstOrNull { bindings[it] == chord }

    /** [shortcut] on [chord] (none when null); a shortcut that had [chord] loses it. */
    fun bind(
        shortcut: XmbShortcut,
        chord: XmbKeyChord?,
    ): XmbShortcuts =
        XmbShortcuts(
            XmbShortcut.entries.associateWith { other ->
                when {
                    other == shortcut -> chord
                    chord != null && bindings[other] == chord -> null
                    else -> bindings[other]
                }
            },
        )

    fun encode(): String = XmbShortcut.entries.joinToString(";") { "${it.name}=${bindings[it]?.label.orEmpty()}" }

    override fun equals(other: Any?): Boolean = other is XmbShortcuts && other.bindings == bindings

    override fun hashCode(): Int = bindings.hashCode()

    companion object {
        /** Familiar media-player keys (Ctrl+arrows to skip and change volume, Shift+arrows to seek). */
        val defaults: Map<XmbShortcut, XmbKeyChord> =
            mapOf(
                XmbShortcut.PLAY_PAUSE to XmbKeyChord(Key.Spacebar),
                XmbShortcut.NEXT_TRACK to XmbKeyChord(Key.DirectionRight, ctrl = true),
                XmbShortcut.PREVIOUS_TRACK to XmbKeyChord(Key.DirectionLeft, ctrl = true),
                XmbShortcut.SEEK_FORWARD to XmbKeyChord(Key.DirectionRight, shift = true),
                XmbShortcut.SEEK_BACK to XmbKeyChord(Key.DirectionLeft, shift = true),
                XmbShortcut.SHUFFLE to XmbKeyChord(Key.S, ctrl = true),
                XmbShortcut.REPEAT to XmbKeyChord(Key.R, ctrl = true),
                XmbShortcut.VOLUME_UP to XmbKeyChord(Key.DirectionUp, ctrl = true),
                XmbShortcut.VOLUME_DOWN to XmbKeyChord(Key.DirectionDown, ctrl = true),
                XmbShortcut.MUTE to XmbKeyChord(Key.M),
                XmbShortcut.LIKE to XmbKeyChord(Key.H),
                XmbShortcut.VISUALIZER to XmbKeyChord(Key.V),
                XmbShortcut.LYRICS to XmbKeyChord(Key.L),
                XmbShortcut.SEARCH to XmbKeyChord(Key.F, ctrl = true),
            )

        val Default = XmbShortcuts(defaults)

        fun decode(text: String): XmbShortcuts {
            val saved =
                text
                    .split(';')
                    .mapNotNull { pair ->
                        val name = pair.substringBefore('=', "")
                        val shortcut = XmbShortcut.entries.firstOrNull { it.name == name } ?: return@mapNotNull null
                        shortcut to pair.substringAfter('=').takeIf(String::isNotEmpty)?.let(XmbKeyChord::parse)
                    }.toMap()
            return XmbShortcuts(XmbShortcut.entries.associateWith { if (it in saved) saved[it] else defaults[it] })
        }
    }
}

/** What a key pressed while waiting for a new shortcut does. */
sealed interface XmbShortcutCapture {
    /** A modifier on its own: keep waiting for the key. */
    data object Wait : XmbShortcutCapture

    /** Esc: leave the shortcut as it was. */
    data object Cancel : XmbShortcutCapture

    /** Backspace: no key for this shortcut. */
    data object Clear : XmbShortcutCapture

    /** A menu key without a modifier: not allowed. */
    data object Reserved : XmbShortcutCapture

    data class Bind(
        val chord: XmbKeyChord,
    ) : XmbShortcutCapture
}

fun xmbShortcutCapture(chord: XmbKeyChord): XmbShortcutCapture =
    when {
        chord.key in MODIFIER_KEYS -> XmbShortcutCapture.Wait
        !chord.hasModifier && chord.key == Key.Escape -> XmbShortcutCapture.Cancel
        !chord.hasModifier && chord.key == Key.Backspace -> XmbShortcutCapture.Clear
        chord.isReserved -> XmbShortcutCapture.Reserved
        else -> XmbShortcutCapture.Bind(chord)
    }

private val MODIFIER_KEYS =
    setOf(Key.CtrlLeft, Key.CtrlRight, Key.ShiftLeft, Key.ShiftRight, Key.AltLeft, Key.AltRight, Key.MetaLeft, Key.MetaRight)

/** A key's name for display and saving; keys without one are saved by code ("#123"). */
fun xmbKeyName(key: Key): String = KEY_NAMES[key] ?: "#${key.keyCode}"

fun xmbKeyFromName(name: String): Key? =
    KEYS_BY_NAME[name.lowercase()] ?: name
        .removePrefix("#")
        .takeIf { name.startsWith("#") }
        ?.toLongOrNull()
        ?.let(::Key)

private val KEY_NAMES: Map<Key, String> by lazy {
    val letters =
        listOf(
            Key.A,
            Key.B,
            Key.C,
            Key.D,
            Key.E,
            Key.F,
            Key.G,
            Key.H,
            Key.I,
            Key.J,
            Key.K,
            Key.L,
            Key.M,
            Key.N,
            Key.O,
            Key.P,
            Key.Q,
            Key.R,
            Key.S,
            Key.T,
            Key.U,
            Key.V,
            Key.W,
            Key.X,
            Key.Y,
            Key.Z,
        ).mapIndexed { index, key -> key to ('A' + index).toString() }
    val digits =
        listOf(Key.Zero, Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)
            .mapIndexed { index, key -> key to index.toString() }
    val numPad =
        listOf(
            Key.NumPad0,
            Key.NumPad1,
            Key.NumPad2,
            Key.NumPad3,
            Key.NumPad4,
            Key.NumPad5,
            Key.NumPad6,
            Key.NumPad7,
            Key.NumPad8,
            Key.NumPad9,
        ).mapIndexed { index, key -> key to "Num $index" }
    val functions =
        listOf(Key.F1, Key.F2, Key.F3, Key.F4, Key.F5, Key.F6, Key.F7, Key.F8, Key.F9, Key.F10, Key.F11, Key.F12)
            .mapIndexed { index, key -> key to "F${index + 1}" }
    (
        letters + digits + numPad + functions +
            listOf(
                Key.Spacebar to "Space",
                Key.Enter to "Enter",
                Key.NumPadEnter to "Num Enter",
                Key.Escape to "Esc",
                Key.Tab to "Tab",
                Key.Backspace to "Backspace",
                Key.Delete to "Delete",
                Key.Insert to "Insert",
                Key.MoveHome to "Home",
                Key.MoveEnd to "End",
                Key.PageUp to "Page Up",
                Key.PageDown to "Page Down",
                Key.DirectionUp to "Up",
                Key.DirectionDown to "Down",
                Key.DirectionLeft to "Left",
                Key.DirectionRight to "Right",
                Key.Comma to "Comma",
                Key.Period to "Period",
                Key.Slash to "Slash",
                Key.Backslash to "Backslash",
                Key.Semicolon to "Semicolon",
                Key.Apostrophe to "Apostrophe",
                Key.LeftBracket to "Left Bracket",
                Key.RightBracket to "Right Bracket",
                Key.Minus to "Minus",
                Key.Equals to "Equals",
                Key.Grave to "Backquote",
                Key.NumPadAdd to "Num Plus",
                Key.NumPadSubtract to "Num Minus",
                Key.NumPadMultiply to "Num Star",
                Key.NumPadDivide to "Num Slash",
                Key.NumPadDot to "Num Period",
                Key.MediaPlayPause to "Play/Pause",
                Key.MediaNext to "Next Track",
                Key.MediaPrevious to "Previous Track",
                Key.MediaStop to "Stop",
                Key.VolumeUp to "Volume Up",
                Key.VolumeDown to "Volume Down",
                Key.VolumeMute to "Mute",
            )
    ).toMap()
}

private val KEYS_BY_NAME: Map<String, Key> by lazy { KEY_NAMES.entries.associate { (key, name) -> name.lowercase() to key } }
