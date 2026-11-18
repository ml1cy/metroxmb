package com.metrolist.shared.xmb.nav

import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.ArtistItem
import com.metrolist.shared.model.PlaylistItem
import com.metrolist.shared.model.PodcastItem
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.xmb.adapters.XmbList

/**
 * A place the platform asks the XMB to show: an app shortcut, a shared link, a notification. It
 * opens [category], with [list] as an opened level titled [title] when there is one. Titles of
 * linked items are a generic word ("Album") until the platform knows better: a link carries only
 * an id.
 */
data class XmbOpen(
    val category: XmbCategoryId,
    val list: XmbList? = null,
    val title: String = "",
) {
    companion object {
        fun album(
            id: String,
            title: String,
        ) = XmbOpen(XmbCategoryId.MUSIC, XmbList.Album(AlbumItem(id = id, title = title, thumbnail = null, artists = emptyList())), title)

        fun artist(
            id: String,
            title: String,
        ) = XmbOpen(XmbCategoryId.MUSIC, XmbList.Artist(ArtistItem(id = id, title = title, thumbnail = null)), title)

        fun playlist(
            id: String,
            title: String,
        ) = XmbOpen(XmbCategoryId.MUSIC, XmbList.Playlist(PlaylistItem(id = id, title = title, thumbnail = null)), title)

        fun podcast(
            id: String,
            title: String,
        ) = XmbOpen(XmbCategoryId.PODCASTS, XmbList.Podcast(PodcastItem(id = id, title = title, thumbnail = null)), title)

        fun downloads(title: String) = XmbOpen(XmbCategoryId.MUSIC, XmbList.Downloads, title)

        fun search(query: String) = XmbOpen(XmbCategoryId.SEARCH, XmbList.SearchResults(query), query)

        fun settings(
            page: SettingsPageId? = null,
            title: String = "",
        ) = XmbOpen(XmbCategoryId.SETTINGS, page?.let(XmbList::SettingsPage), title)
    }
}

/**
 * The navigation that shows [target]: its category, focused on the item that opens [XmbOpen.list]
 * when the column has one (so back lands on it), with the level opened over it. Anything open
 * before is closed.
 */
fun XmbNavigation.opening(
    target: XmbOpen,
    columns: List<List<XmbItem>>,
    icon: XmbIcon,
): XmbNavigation {
    val category = target.category.ordinal.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val column = columns.getOrNull(category).orEmpty()
    val focus = target.list?.let { list -> column.indexOfFirst { it.list == list } }?.takeIf { it >= 0 }
    val updatedItems = items.toMutableList().also { if (focus != null && category in it.indices) it[category] = focus }
    // A level the column already lists is opened from that item, as if it were selected.
    val level = target.list?.let { XmbLevel(it, target.title, column.getOrNull(focus ?: -1)?.icon ?: icon) }
    return copy(category = category, items = updatedItems, stack = listOfNotNull(level))
}
