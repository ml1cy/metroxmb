package com.metrolist.shared.xmb.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.xmb.adapters.XmbList
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The horizontal categories, in display order. */
enum class XmbCategoryId {
    USERS,
    SETTINGS,
    MUSIC,
    SEARCH,
    PODCASTS,
    VIDEO,
    FRIENDS,
    VISUALIZER,
}

sealed interface XmbIcon {
    data class Drawable(
        val resource: DrawableResource,
    ) : XmbIcon

    data class Vector(
        val image: ImageVector,
    ) : XmbIcon
}

/** The visualizers the Visualizer column opens full screen. */
enum class XmbVisualizerKind {
    WAVES,
    VALLEY,
    GAIA,
}

/** What fills the screen instead of the menu. */
sealed interface XmbFullScreen {
    /** A visualizer, with the lyrics over it when [lyrics] is on (△ toggles them). */
    data class Visualizer(
        val kind: XmbVisualizerKind,
        val lyrics: Boolean = false,
        /** Auto-cycle: moves on to the next visualizer every so often. */
        val cycle: Boolean = false,
    ) : XmbFullScreen

    /** The playing music video. */
    data object Video : XmbFullScreen

    /** The playing song's lyrics over the home background. */
    data object Lyrics : XmbFullScreen
}

/** Text from the string catalog, or plain text from data (settings titles, queries, account names). */
sealed interface XmbText {
    data class Resource(
        val resource: StringResource,
    ) : XmbText

    data class Plain(
        val text: String,
    ) : XmbText
}

@Composable
fun XmbText.resolve(): String =
    when (this) {
        is XmbText.Resource -> stringResource(resource)
        is XmbText.Plain -> text
    }

/**
 * What selecting a top-level item does besides opening its [XmbItem.list]. Items with neither
 * play the error sound.
 */
sealed interface XmbItemAction {
    data object None : XmbItemAction

    /** Opens the on-screen keyboard for a new search. */
    data object OpenSearch : XmbItemAction

    data object SignIn : XmbItemAction

    data object SwitchChannel : XmbItemAction

    data object SignOut : XmbItemAction

    /** Shows a visualizer full screen, hiding the XMB. */
    data class OpenVisualizer(
        val kind: XmbVisualizerKind,
    ) : XmbItemAction

    /** Shows a visualizer chosen at random (not the last one). */
    data object OpenRandomVisualizer : XmbItemAction

    /** Shows the visualizers in turn. */
    data object CycleVisualizers : XmbItemAction

    /** Shows the playing song's music video full screen. */
    data object WatchVideo : XmbItemAction

    /** Shows the playing song's lyrics full screen. */
    data object OpenLyrics : XmbItemAction

    /** Friends: turns Listen Together on. */
    data object EnableListenTogether : XmbItemAction

    /** Friends: creates a room (asking for a name first if there's none). */
    data object CreateRoom : XmbItemAction

    /** Friends: joins a room by code, typed on the keyboard. */
    data object JoinRoom : XmbItemAction

    /** Friends: changes the name others see. */
    data object EditUsername : XmbItemAction

    /** Friends: copies the room's invite link. */
    data object CopyRoomLink : XmbItemAction

    /** Friends: leaves the room (after a Yes/No). */
    data object LeaveRoom : XmbItemAction

    /** Friends: stops waiting for the host to let us in. */
    data object CancelJoin : XmbItemAction

    /** Friends: lets someone in, or not (the host's Approve/Reject picker). */
    data class ReviewJoin(
        val userId: String,
        val username: String,
    ) : XmbItemAction

    /** A settings page the XMB doesn't draw itself; the host shows its stock screen. */
    data class StockSettings(
        val page: SettingsPageId,
    ) : XmbItemAction
}

data class XmbItem(
    val id: String,
    val title: XmbText,
    val icon: XmbIcon,
    val subtitle: XmbText? = null,
    val action: XmbItemAction = XmbItemAction.None,
    val list: XmbList? = null,
    val artwork: String? = null,
) {
    constructor(
        id: String,
        title: StringResource,
        icon: XmbIcon,
        subtitle: StringResource? = null,
        action: XmbItemAction = XmbItemAction.None,
        list: XmbList? = null,
    ) : this(id, XmbText.Resource(title), icon, subtitle?.let(XmbText::Resource), action, list)

    val isAvailable: Boolean get() = action != XmbItemAction.None || list != null
}

data class XmbCategory(
    val id: XmbCategoryId,
    val title: StringResource,
    val icon: XmbIcon,
    val items: List<XmbItem>,
)
