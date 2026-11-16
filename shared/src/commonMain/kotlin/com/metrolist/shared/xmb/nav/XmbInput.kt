package com.metrolist.shared.xmb.nav

import androidx.compose.ui.input.key.Key

/**
 * Maps keyboard, controller and remote keys to XMB actions. Enter, numpad Enter, the D-pad center
 * and the controller's south button select (✕); Escape, Backspace, Back and the east button go
 * back (○); Tab, Menu and the north button open options (△). Space and the media keys control
 * playback.
 */
fun xmbActionFor(key: Key): XmbAction? =
    when (key) {
        Key.DirectionUp -> XmbAction.UP
        Key.DirectionDown -> XmbAction.DOWN
        Key.DirectionLeft -> XmbAction.LEFT
        Key.DirectionRight -> XmbAction.RIGHT
        Key.Enter, Key.NumPadEnter, Key.DirectionCenter, Key.ButtonA -> XmbAction.SELECT
        Key.Escape, Key.Backspace, Key.Back, Key.ButtonB -> XmbAction.BACK
        Key.Tab, Key.Menu, Key.ButtonY -> XmbAction.OPTIONS
        Key.Spacebar, Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> XmbAction.PLAY_PAUSE
        Key.MediaNext -> XmbAction.NEXT_TRACK
        Key.MediaPrevious -> XmbAction.PREVIOUS_TRACK
        else -> null
    }

/** Mouse wheel or touchpad scrolling: vertical moves through items, horizontal through categories. */
fun xmbActionForScroll(
    deltaX: Float,
    deltaY: Float,
): XmbAction? =
    when {
        deltaY == 0f && deltaX == 0f -> null
        kotlin.math.abs(deltaY) >= kotlin.math.abs(deltaX) -> if (deltaY > 0f) XmbAction.DOWN else XmbAction.UP
        else -> if (deltaX > 0f) XmbAction.RIGHT else XmbAction.LEFT
    }
