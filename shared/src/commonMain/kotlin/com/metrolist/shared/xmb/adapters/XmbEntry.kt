package com.metrolist.shared.xmb.adapters

import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.xmb_cat_users
import com.metrolist.shared.generated.resources.xmb_item_album
import com.metrolist.shared.generated.resources.xmb_item_artist
import com.metrolist.shared.generated.resources.xmb_item_music_video
import com.metrolist.shared.generated.resources.xmb_item_network_settings
import com.metrolist.shared.generated.resources.xmb_item_playlist
import com.metrolist.shared.generated.resources.xmb_item_song
import com.metrolist.shared.generated.resources.xmb_item_subsetting
import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.ArtistItem
import com.metrolist.shared.model.ArtistTiny
import com.metrolist.shared.model.BrowseItem
import com.metrolist.shared.model.EpisodeItem
import com.metrolist.shared.model.HomeSection
import com.metrolist.shared.model.PlaylistItem
import com.metrolist.shared.model.PodcastItem
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.model.YTItem
import com.metrolist.shared.model.toSongItem
import com.metrolist.shared.ui.settings.SettingsItemRegistration
import com.metrolist.shared.xmb.nav.XmbIcon
import com.metrolist.shared.xmb.theme.XmbIcons

enum class XmbEntryKind {
    SONG,
    ALBUM,
    PLAYLIST,
    ARTIST,
    SECTION,
    PODCAST,
    EPISODE,

    /** Browse and mood pages; they open as levels of shelves. */
    BROWSE,

    /** A row of a settings page; [XmbEntry.setting] holds the registry item. */
    SETTING,

    /** A cast device; [XmbEntry.key] carries its id. */
    DEVICE,

    /** Someone in the Listen Together room. */
    MEMBER,
}

/**
 * One row of a drill-down list. [open] is the list SELECT opens; rows without one play
 * ([song]) or, for kinds the XMB doesn't handle yet, play the error sound.
 */
data class XmbEntry(
    val key: String,
    val title: String,
    val subtitle: String?,
    val artwork: String?,
    val kind: XmbEntryKind,
    val item: YTItem? = null,
    val open: XmbList? = null,
    val setting: SettingsItemRegistration? = null,
    val value: String? = null,
    val enabled: Boolean = true,
)

/** The playable song behind this row, if any. Episodes play as songs, like the stock app. */
val XmbEntry.song: SongItem?
    get() =
        when (val source = item) {
            is SongItem -> source
            is EpisodeItem -> source.toSongItem()
            else -> null
        }

/** Fallback icon while artwork loads, or when there's none. */
val XmbEntry.icon: XmbIcon
    get() =
        when (kind) {
            XmbEntryKind.SONG -> {
                if ((item as? SongItem)?.musicVideoType != null) {
                    XmbIcon.Drawable(Res.drawable.xmb_item_music_video)
                } else {
                    XmbIcon.Drawable(Res.drawable.xmb_item_song)
                }
            }

            XmbEntryKind.EPISODE -> {
                XmbIcon.Drawable(Res.drawable.xmb_item_song)
            }

            XmbEntryKind.ALBUM -> {
                XmbIcon.Drawable(Res.drawable.xmb_item_album)
            }

            XmbEntryKind.ARTIST -> {
                XmbIcon.Drawable(Res.drawable.xmb_item_artist)
            }

            XmbEntryKind.PLAYLIST, XmbEntryKind.SECTION, XmbEntryKind.BROWSE -> {
                XmbIcon.Drawable(Res.drawable.xmb_item_playlist)
            }

            XmbEntryKind.PODCAST -> {
                XmbIcon.Vector(XmbIcons.Podcasts)
            }

            XmbEntryKind.SETTING -> {
                XmbIcon.Drawable(Res.drawable.xmb_item_subsetting)
            }

            XmbEntryKind.DEVICE -> {
                XmbIcon.Drawable(Res.drawable.xmb_item_network_settings)
            }

            XmbEntryKind.MEMBER -> {
                XmbIcon.Drawable(Res.drawable.xmb_cat_users)
            }
        }

fun YTItem.toXmbEntry(): XmbEntry =
    when (this) {
        is SongItem -> {
            XmbEntry(
                key = "song:$id",
                title = customTitle ?: title,
                subtitle = listOfNotNull(customArtist ?: artists.joinNames(), album?.name).joinToString(" · ").ifBlank { null },
                artwork = thumbnail.asArtwork(),
                kind = XmbEntryKind.SONG,
                item = this,
            )
        }

        is AlbumItem -> {
            XmbEntry(
                key = "album:$id",
                title = title,
                subtitle = listOfNotNull(artists.joinNames(), year).joinToString(" · ").ifBlank { null },
                artwork = thumbnail.asArtwork(),
                kind = XmbEntryKind.ALBUM,
                item = this,
                open = XmbList.Album(this),
            )
        }

        is PlaylistItem -> {
            XmbEntry(
                key = "playlist:$id",
                title = title,
                subtitle = listOfNotNull(author, songCount).joinToString(" · ").ifBlank { subtitle },
                artwork = (customThumbnail ?: thumbnail).asArtwork() ?: artworkThumbnails.firstOrNull().asArtwork(),
                kind = XmbEntryKind.PLAYLIST,
                item = this,
                open = XmbList.Playlist(this),
            )
        }

        is ArtistItem -> {
            XmbEntry(
                key = "artist:$id",
                title = title,
                subtitle = subscribers ?: monthlyListeners,
                artwork = thumbnail.asArtwork(),
                kind = XmbEntryKind.ARTIST,
                item = this,
                open = XmbList.Artist(this),
            )
        }

        is EpisodeItem -> {
            XmbEntry(
                key = "episode:$id",
                title = title,
                subtitle = listOfNotNull(author?.name, publishDateText).joinToString(" · ").ifBlank { null },
                artwork = thumbnail.asArtwork(),
                kind = XmbEntryKind.EPISODE,
                item = this,
            )
        }

        is PodcastItem -> {
            XmbEntry(
                key = "podcast:$id",
                title = title,
                subtitle = author,
                artwork = thumbnail.asArtwork(),
                kind = XmbEntryKind.PODCAST,
                item = this,
                open = XmbList.Podcast(this),
            )
        }

        is BrowseItem -> {
            XmbEntry(
                key = "browse:$id",
                title = title,
                subtitle = subtitle,
                artwork = thumbnail.asArtwork(),
                kind = XmbEntryKind.BROWSE,
                item = this,
                open = XmbList.Browse(id, params, title),
            )
        }
    }

fun HomeSection.toXmbEntry(index: Int): XmbEntry =
    XmbEntry(
        key = "section:$index:$title",
        title = title.ifBlank { items.firstOrNull()?.title.orEmpty() },
        subtitle = subtitle,
        artwork = thumbnail.asArtwork() ?: items.firstOrNull()?.thumbnail.asArtwork(),
        kind = XmbEntryKind.SECTION,
        open = XmbList.Section(this, index),
    )

/**
 * The queue for playing the song at [index] of a list: every playable row, starting position
 * mapped past non-playable rows (albums or artists mixed into a home shelf).
 */
fun List<XmbEntry>.playQueueFrom(index: Int): Pair<List<SongItem>, Int>? {
    val selected = getOrNull(index)?.song ?: return null
    val songs = mapNotNull { it.song }.filterNot { it.isUnavailable }
    val start = songs.indexOfFirst { it.id == selected.id }
    return if (start < 0) listOf(selected) to 0 else songs to start
}

private fun List<ArtistTiny>.joinNames(): String? = map { it.name }.filter(String::isNotBlank).joinToString(", ").ifBlank { null }

/**
 * Only real images: built-in playlists put bare icon names in their thumbnail field, while URLs,
 * `file:` URIs and local paths (including Windows ones) all contain a slash or colon.
 */
private fun String?.asArtwork(): String? = this?.takeIf { value -> value.any { it == '/' || it == '\\' || it == ':' } }
