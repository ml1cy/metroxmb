package com.metrolist.desktop.state.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metrolist.shared.ui.navigation.Navigator
import com.metrolist.shared.ui.navigation.Screen

object DesktopNavigation {
    var listenTogetherRoomCode by mutableStateOf<String?>(null)

    // XMB Music: no stock screens are shown, so nothing binds a navigator in the app; the content
    // states still record where they'd go, harmlessly. Tests bind one.
    private var boundNavigator: Navigator? = null

    /** The bound navigator's current route, or null when none is bound. */
    val currentRoute: Screen? get() = boundNavigator?.state?.currentRoute as? Screen

    fun bind(navigator: Navigator) {
        boundNavigator = navigator
    }

    fun unbind(navigator: Navigator) {
        if (boundNavigator === navigator) boundNavigator = null
    }

    fun navigate(route: Screen) {
        boundNavigator?.navigate(route)
    }

    fun openListenTogetherInvitation(
        roomCode: String,
        joinRoom: (String) -> Boolean,
    ) {
        listenTogetherRoomCode = roomCode
        navigate(Screen.ListenTogether)
        if (joinRoom(roomCode)) listenTogetherRoomCode = null
    }

    fun goBack(): Boolean {
        val navigator = boundNavigator ?: return false
        return performNavigationBack(navigator.canGoBack, navigator::goBack)
    }
}

internal fun performNavigationBack(
    canGoBack: Boolean,
    onBack: () -> Unit,
): Boolean {
    if (!canGoBack) return false
    onBack()
    return true
}
