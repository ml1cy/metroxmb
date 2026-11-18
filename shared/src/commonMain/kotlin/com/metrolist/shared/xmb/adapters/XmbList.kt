package com.metrolist.shared.xmb.adapters

import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.ArtistItem
import com.metrolist.shared.model.HomeSection
import com.metrolist.shared.model.PlaylistItem
import com.metrolist.shared.model.PodcastItem
import com.metrolist.shared.model.SearchSummary
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.xmb.settings.XmbSettingsPageId

/** A list the XMB can open as a drill-down level. [key] identifies it for animation and state. */
sealed interface XmbList {
    val key: String

    data object Home : XmbList {
        override val key = "home"
    }

    data object QuickPicks : XmbList {
        override val key = "quick_picks"
    }

    data object Liked : XmbList {
        override val key = "liked"
    }

    data object Playlists : XmbList {
        override val key = "playlists"
    }

    data object Albums : XmbList {
        override val key = "albums"
    }

    data object Artists : XmbList {
        override val key = "artists"
    }

    data object History : XmbList {
        override val key = "history"
    }

    data object Downloads : XmbList {
        override val key = "downloads"
    }

    /** One home feed shelf, by its position in the feed. */
    data class Section(
        val section: HomeSection,
        val index: Int,
    ) : XmbList {
        override val key = "section:$index:${section.title}"
    }

    data class Album(
        val album: AlbumItem,
    ) : XmbList {
        override val key = "album:${album.id}"
    }

    data class Playlist(
        val playlist: PlaylistItem,
    ) : XmbList {
        override val key = "playlist:${playlist.id}"
    }

    data class Artist(
        val artist: ArtistItem,
    ) : XmbList {
        override val key = "artist:${artist.id}"
    }

    /** Podcasts in the library (subscriptions). */
    data object Podcasts : XmbList {
        override val key = "podcasts"
    }

    /** New episodes of subscribed podcasts. */
    data object NewEpisodes : XmbList {
        override val key = "new_episodes"
    }

    /** Episodes saved for later. */
    data object SavedEpisodes : XmbList {
        override val key = "saved_episodes"
    }

    /** One podcast's episodes. */
    data class Podcast(
        val podcast: PodcastItem,
    ) : XmbList {
        override val key = "podcast:${podcast.id}"
    }

    /** Liked songs that are music videos. */
    data object LikedVideos : XmbList {
        override val key = "liked_videos"
    }

    /** Music videos from the home feed's shelves. */
    data object FeedVideos : XmbList {
        override val key = "feed_videos"
    }

    /** A browse page (a mood, a genre, a chart): its shelves, each opening as a [Section]. */
    data class Browse(
        val browseId: String,
        val params: String? = null,
        val title: String? = null,
    ) : XmbList {
        override val key = "browse:$browseId:$params"
    }

    /** The play queue, built from the playback state. */
    data object Queue : XmbList {
        override val key = "queue"
    }

    /** Cast devices found on the network; the XMB builds these rows from the playback state. */
    data object CastDevices : XmbList {
        override val key = "cast_devices"
    }

    /** The people in the Listen Together room; built from the room state. */
    data object RoomMembers : XmbList {
        override val key = "room_members"
    }

    /** A settings page from the settings registry, drawn as XMB rows with value pickers. */
    data class SettingsPage(
        val page: SettingsPageId,
    ) : XmbList {
        override val key = "settings:${page.name}"
    }

    /** One of the XMB's own settings pages (Theme, Date and Time...), built by XmbApp in composition. */
    data class XmbSettings(
        val page: XmbSettingsPageId,
    ) : XmbList {
        override val key = "xmb_settings:${page.name}"
    }

    /** Search results for [query]: one folder per result shelf (top result, songs, albums...). */
    data class SearchResults(
        val query: String,
    ) : XmbList {
        override val key = "search:$query"
    }

    /** One shelf of a search's results. */
    data class SearchSection(
        val query: String,
        val index: Int,
        val summary: SearchSummary,
    ) : XmbList {
        override val key = "search:$query:$index"
    }
}
