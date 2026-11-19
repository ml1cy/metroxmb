package com.metrolist.shared.xmb

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.nav.XmbOpen
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

    private val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    internal val toasts: SharedFlow<String> get() = messages

    /** A short message across the top of the XMB, like its own "Shuffle on" (the platform's snackbars). */
    fun toast(message: String) {
        messages.tryEmit(message)
    }

    /**
     * A place to show (a link, an app shortcut, a notification). It waits here until the XMB is
     * shown and takes it, so a link that starts the app isn't lost.
     */
    var pendingOpen by mutableStateOf<XmbOpen?>(null)
        private set

    fun open(target: XmbOpen) {
        pendingOpen = target
    }

    internal fun takeOpen(): XmbOpen? = pendingOpen.also { pendingOpen = null }

    /** Whether BACK does something in the XMB (a level, a panel, the keyboard, full screen); false at the top. */
    var canGoBack by mutableStateOf(false)
        internal set
}
