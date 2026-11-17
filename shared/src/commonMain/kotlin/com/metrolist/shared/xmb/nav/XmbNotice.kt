package com.metrolist.shared.xmb.nav

/**
 * A console-style notice: a message band across the screen with buttons, for things the platform
 * needs to tell the user (a playback error, an expired sign-in). [cancel] is the button ○
 * chooses; null means ○ does nothing.
 */
data class XmbNotice(
    val id: String,
    val message: String,
    val buttons: List<String>,
    val title: String? = null,
    val details: String? = null,
    val focus: Int = 0,
    val cancel: Int? = null,
)

/** A notice from the host, and what to do with the chosen button's index. */
class XmbNoticeRequest(
    val notice: XmbNotice,
    val onChoice: (Int) -> Unit,
)

/** One notice action's result: the focused button, the sound, and the button chosen, if any. */
data class XmbNoticeStep(
    val focus: Int,
    val feedback: XmbFeedback,
    val chosen: Int? = null,
)

fun XmbNotice.reduce(
    action: XmbAction,
    focus: Int,
): XmbNoticeStep =
    when (action) {
        XmbAction.LEFT, XmbAction.UP -> {
            if (focus > 0) XmbNoticeStep(focus - 1, XmbFeedback.MOVE_SIDE) else XmbNoticeStep(focus, XmbFeedback.NONE)
        }

        XmbAction.RIGHT, XmbAction.DOWN -> {
            if (focus < buttons.lastIndex) XmbNoticeStep(focus + 1, XmbFeedback.MOVE_SIDE) else XmbNoticeStep(focus, XmbFeedback.NONE)
        }

        XmbAction.SELECT -> {
            XmbNoticeStep(focus, XmbFeedback.NONE, chosen = focus)
        }

        XmbAction.BACK -> {
            val cancel = cancel
            if (cancel != null) XmbNoticeStep(focus, XmbFeedback.NONE, chosen = cancel) else XmbNoticeStep(focus, XmbFeedback.ERROR)
        }

        XmbAction.OPTIONS, XmbAction.PLAY_PAUSE, XmbAction.NEXT_TRACK, XmbAction.PREVIOUS_TRACK -> {
            XmbNoticeStep(focus, XmbFeedback.NONE)
        }
    }
