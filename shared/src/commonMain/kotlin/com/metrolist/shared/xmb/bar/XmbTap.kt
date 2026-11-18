package com.metrolist.shared.xmb.bar

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Where taps on the XMB's parts go: a category icon, a row of the focused column or of an opened
 * level, a level's parent icon, an on-screen keyboard key or suggestion, a picker value, a slider
 * arrow, an option in the △ panel, a notice button. `XmbApp` provides them; [row] and [levelRow]
 * get `long = true` for a long press (or a right-click), which opens the row's options.
 */
internal class XmbTaps(
    val category: (Int) -> Unit,
    val row: (index: Int, long: Boolean) -> Unit,
    val levelRow: (index: Int, long: Boolean) -> Unit,
    val parent: () -> Unit,
    val key: (row: Int, column: Int) -> Unit,
    val suggestion: (Int) -> Unit,
    val pickerValue: (Int) -> Unit,
    val sliderStep: (Int) -> Unit,
    val confirm: () -> Unit,
    val option: (Int) -> Unit,
    val noticeButton: (Int) -> Unit,
)

internal val LocalXmbTaps = staticCompositionLocalOf<XmbTaps?> { null }

/**
 * A tap on this element: the pointer goes down and up again without travelling more than a
 * generous slop (twice the platform's touch slop: fingers wobble, and a wobble isn't a drag).
 * Held past the long-press time, or pressed with the secondary mouse button, it's a long press
 * instead ([onLongPress], or [onTap] when there's none). Drags are left to the XMB's own touch
 * handling, which steps through the menu. Works for touch, stylus and the mouse.
 */
internal fun Modifier.xmbTap(
    onLongPress: (() -> Unit)? = null,
    onTap: () -> Unit,
): Modifier =
    composed {
        val tap by rememberUpdatedState(onTap)
        val longPress by rememberUpdatedState(onLongPress)
        pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val secondary = currentEvent.buttons.isSecondaryPressed
                val slop = viewConfiguration.touchSlop * TAP_SLOP
                var moved = false
                val up: PointerInputChange? =
                    withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull null
                            if ((change.position - down.position).getDistance() > slop) moved = true
                            if (change.changedToUp()) return@withTimeoutOrNull change
                        }
                        @Suppress("UNREACHABLE_CODE")
                        null
                    }
                if (moved) return@awaitEachGesture
                if (up != null) {
                    up.consume()
                    if (secondary) (longPress ?: tap)() else tap()
                    return@awaitEachGesture
                }
                // Held: a long press, as soon as it's long enough; then the rest of the press is ours.
                if (longPress == null && down.isConsumed) return@awaitEachGesture
                (longPress ?: tap)()
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    change.consume()
                    if (change.changedToUp()) break
                }
            }
        }
    }

private const val TAP_SLOP = 2f
