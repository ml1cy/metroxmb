package com.metrolist.shared.xmb

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metrolist.shared.xmb.nav.XmbAction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Input from outside the XMB's own key and pointer handling, and what the platform needs to know
 * back. Android sends its system back (the back gesture or the TV remote's back key) as [send]
 * BACK while [canGoBack], and lets the system handle back otherwise (leaving the app).
 */
class XmbInputController(
    /** Leave the Back key to the platform's own back handling, so it isn't handled twice. */
    val systemBack: Boolean = false,
) {
    private val pending = MutableSharedFlow<XmbAction>(extraBufferCapacity = 16)
    internal val actions: SharedFlow<XmbAction> get() = pending

    fun send(action: XmbAction) {
        pending.tryEmit(action)
    }

    /** Whether BACK does something in the XMB (a level, a panel, the keyboard, full screen); false at the top. */
    var canGoBack by mutableStateOf(false)
        internal set
}
