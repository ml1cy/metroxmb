package com.metrolist.shared.xmb.adapters

import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.ArtistItem
import com.metrolist.shared.model.HomeSection
import com.metrolist.shared.model.PlaylistItem

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
}
