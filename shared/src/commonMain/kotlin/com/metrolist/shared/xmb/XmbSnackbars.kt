package com.metrolist.shared.xmb

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filterNotNull

/**
 * The app's snackbars as XMB toasts while the XMB is shown (playback errors, Listen Together
 * events, playlist additions...). With [keepActions], a snackbar with an action (an undo, say) is
 * left to the platform's own snackbar host, so the action still works; without one, it's shown as
 * a toast too, since nothing else would show it.
 */
@Composable
fun SnackbarsAsXmbToasts(
    snackbars: SnackbarHostState,
    input: XmbInputController,
    keepActions: Boolean = false,
) {
    LaunchedEffect(snackbars, input, keepActions) {
        snapshotFlow { snackbars.currentSnackbarData }.filterNotNull().collect { data ->
            if (keepActions && data.visuals.actionLabel != null) return@collect
            input.toast(data.visuals.message)
            data.dismiss()
        }
    }
}
