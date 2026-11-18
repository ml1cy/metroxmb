package com.metrolist.shared.xmb.nav

/** A key of the on-screen keyboard. Character keys print [char]; the others edit or submit. */
sealed interface XmbKey {
    /** Grid cells the key covers. */
    val span: Int

    data class Character(
        val char: Char,
    ) : XmbKey {
        override val span = 1
    }

    data object Shift : XmbKey {
        override val span = 2
    }

    data object Space : XmbKey {
        override val span = 4
    }

    data object Delete : XmbKey {
        override val span = 2
    }

    data object Done : XmbKey {
        override val span = 2
    }
}

/** The layout: four rows of ten characters, then Shift, Space, Delete and Done. */
val xmbKeyRows: List<List<XmbKey>> =
    listOf("1234567890", "qwertyuiop", "asdfghjkl'", "zxcvbnm,.-").map { row -> row.map(XmbKey::Character) } +
        listOf(listOf(XmbKey.Shift, XmbKey.Space, XmbKey.Delete, XmbKey.Done))

const val XMB_KEY_COLUMNS = 10
const val XMB_KEYBOARD_MAX_LENGTH = 200

/**
 * The on-screen keyboard: the [text] typed so far, the focused grid cell ([row], [column]), Shift,
 * and [suggestions] shown in a column to the right of the keys ([suggestion] is the focused one
 * while focus is there, else -1). [secret] hides the text (passwords and keys in settings).
 */
data class XmbKeyboard(
    val title: String,
    val text: String = "",
    val row: Int = 1,
    val column: Int = 0,
    val shift: Boolean = false,
    val secret: Boolean = false,
    val suggestions: List<String> = emptyList(),
    val suggestion: Int = -1,
    /** The longest text it takes; sign-in tokens are pasted and run to thousands of characters. */
    val maxLength: Int = XMB_KEYBOARD_MAX_LENGTH,
) {
    val focusedKey: XmbKey? get() = if (suggestion >= 0) null else keyAt(row, column)

    /** Physical typing: adds [char] and moves focus to Done, so Enter submits what was typed. */
    fun type(char: Char): XmbKeyboard {
        if (text.length >= maxLength || char.isISOControl()) return this
        return copy(
            text = text + char,
            row = xmbKeyRows.lastIndex,
            column = startColumn(xmbKeyRows.lastIndex, XmbKey.Done),
            suggestion = -1,
        )
    }

    fun deleteLast(): XmbKeyboard = copy(text = text.dropLast(1))

    /** Pasting: adds [pasted] (line breaks as spaces, other control characters dropped) up to [maxLength]. */
    fun paste(pasted: String): XmbKeyboard {
        val clean = pasted.map { if (it == '\n' || it == '\r' || it == '\t') ' ' else it }.filterNot(Char::isISOControl).joinToString("")
        if (clean.isEmpty()) return this
        return copy(
            text = (text + clean).take(maxLength),
            row = xmbKeyRows.lastIndex,
            column = startColumn(xmbKeyRows.lastIndex, XmbKey.Done),
            suggestion = -1,
        )
    }

    /** Keeps focus valid when the suggestion list changes under it. */
    fun withSuggestions(list: List<String>): XmbKeyboard =
        copy(suggestions = list, suggestion = if (suggestion >= 0) suggestion.coerceAtMost(list.lastIndex) else -1)
}

/** One keyboard action's result: the keyboard (null once closed), the sound, and submitted text. */
data class XmbKeyboardStep(
    val keyboard: XmbKeyboard?,
    val feedback: XmbFeedback,
    val submitted: String? = null,
)

fun XmbKeyboard.reduce(action: XmbAction): XmbKeyboardStep {
    if (suggestion >= 0) return reduceSuggestions(action)
    return when (action) {
        XmbAction.UP -> {
            if (row > 0) moved(row - 1, column, XmbFeedback.MOVE_UP) else XmbKeyboardStep(this, XmbFeedback.NONE)
        }

        XmbAction.DOWN -> {
            if (row < xmbKeyRows.lastIndex) moved(row + 1, column, XmbFeedback.MOVE_DOWN) else XmbKeyboardStep(this, XmbFeedback.NONE)
        }

        XmbAction.LEFT -> {
            val start = startColumn(row, checkNotNull(keyAt(row, column)))
            if (start > 0) moved(row, start - 1, XmbFeedback.MOVE_SIDE) else XmbKeyboardStep(this, XmbFeedback.NONE)
        }

        XmbAction.RIGHT -> {
            val key = checkNotNull(keyAt(row, column))
            val next = startColumn(row, key) + key.span
            when {
                next < XMB_KEY_COLUMNS -> {
                    moved(row, next, XmbFeedback.MOVE_SIDE)
                }

                suggestions.isNotEmpty() -> {
                    XmbKeyboardStep(
                        copy(suggestion = row.coerceAtMost(suggestions.lastIndex)),
                        XmbFeedback.MOVE_SIDE,
                    )
                }

                else -> {
                    XmbKeyboardStep(this, XmbFeedback.NONE)
                }
            }
        }

        XmbAction.SELECT -> {
            press(checkNotNull(keyAt(row, column)))
        }

        XmbAction.OPTIONS -> {
            XmbKeyboardStep(if (text.length < maxLength) copy(text = "$text ") else this, XmbFeedback.SELECT)
        }

        XmbAction.BACK -> {
            XmbKeyboardStep(null, XmbFeedback.BACK)
        }

        XmbAction.PLAY_PAUSE, XmbAction.NEXT_TRACK, XmbAction.PREVIOUS_TRACK -> {
            XmbKeyboardStep(this, XmbFeedback.NONE)
        }
    }
}

private fun XmbKeyboard.press(key: XmbKey): XmbKeyboardStep =
    when (key) {
        is XmbKey.Character -> {
            val char = if (shift) key.char.uppercaseChar() else key.char
            if (text.length <
                maxLength
            ) {
                XmbKeyboardStep(copy(text = text + char, shift = false), XmbFeedback.SELECT)
            } else {
                XmbKeyboardStep(this, XmbFeedback.ERROR)
            }
        }

        XmbKey.Shift -> {
            XmbKeyboardStep(copy(shift = !shift), XmbFeedback.SELECT)
        }

        XmbKey.Space -> {
            if (text.length <
                maxLength
            ) {
                XmbKeyboardStep(copy(text = "$text "), XmbFeedback.SELECT)
            } else {
                XmbKeyboardStep(this, XmbFeedback.ERROR)
            }
        }

        XmbKey.Delete -> {
            if (text.isNotEmpty()) XmbKeyboardStep(deleteLast(), XmbFeedback.BACK) else XmbKeyboardStep(this, XmbFeedback.ERROR)
        }

        XmbKey.Done -> {
            XmbKeyboardStep(null, XmbFeedback.SELECT, submitted = text)
        }
    }

private fun XmbKeyboard.reduceSuggestions(action: XmbAction): XmbKeyboardStep =
    when (action) {
        XmbAction.UP -> {
            val previous = suggestion - 1
            if (previous >=
                0
            ) {
                XmbKeyboardStep(copy(suggestion = previous), XmbFeedback.MOVE_UP)
            } else {
                XmbKeyboardStep(this, XmbFeedback.NONE)
            }
        }

        XmbAction.DOWN -> {
            val next = suggestion + 1
            if (next <=
                suggestions.lastIndex
            ) {
                XmbKeyboardStep(copy(suggestion = next), XmbFeedback.MOVE_DOWN)
            } else {
                XmbKeyboardStep(this, XmbFeedback.NONE)
            }
        }

        XmbAction.LEFT -> {
            val back = suggestion.coerceAtMost(xmbKeyRows.lastIndex)
            XmbKeyboardStep(copy(suggestion = -1, row = back, column = XMB_KEY_COLUMNS - 1), XmbFeedback.MOVE_SIDE)
        }

        XmbAction.SELECT -> {
            XmbKeyboardStep(null, XmbFeedback.SELECT, submitted = suggestions[suggestion])
        }

        XmbAction.BACK -> {
            XmbKeyboardStep(null, XmbFeedback.BACK)
        }

        XmbAction.RIGHT, XmbAction.OPTIONS, XmbAction.PLAY_PAUSE, XmbAction.NEXT_TRACK, XmbAction.PREVIOUS_TRACK -> {
            XmbKeyboardStep(this, XmbFeedback.NONE)
        }
    }

private fun XmbKeyboard.moved(
    row: Int,
    column: Int,
    feedback: XmbFeedback,
): XmbKeyboardStep {
    val key = checkNotNull(keyAt(row, column))
    return XmbKeyboardStep(copy(row = row, column = startColumn(row, key)), feedback)
}

/** The key covering grid cell ([row], [column]). */
fun keyAt(
    row: Int,
    column: Int,
): XmbKey? {
    var start = 0
    for (key in xmbKeyRows.getOrNull(row) ?: return null) {
        if (column < start + key.span) return key
        start += key.span
    }
    return null
}

/** The first grid column [key] covers in [row]. */
fun startColumn(
    row: Int,
    key: XmbKey,
): Int {
    var start = 0
    for (candidate in xmbKeyRows[row]) {
        if (candidate == key) return start
        start += candidate.span
    }
    error("$key isn't in row $row")
}
