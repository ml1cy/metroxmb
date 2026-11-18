package com.metrolist.android.xmb

import com.metrolist.android.ui.IncomingDestination
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.album
import com.metrolist.shared.generated.resources.artist
import com.metrolist.shared.generated.resources.downloads
import com.metrolist.shared.generated.resources.liked_songs
import com.metrolist.shared.generated.resources.playlist
import com.metrolist.shared.generated.resources.podcast
import com.metrolist.shared.ui.navigation.Screen
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.nav.XmbCategoryId
import com.metrolist.shared.xmb.nav.XmbOpen
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.getString

/**
 * Links, app shortcuts and notifications for the XMB. `MetrolistApp` hands over each place the
 * XMB can show ([xmbOpenFor]) and goes to its top level; `XmbAndroidHost` passes it to the XMB.
 * The player opens the XMB's full-screen visualizer. The rest (sign-in, song recognition, browse
 * pages, playlist import, joining a room by link) still open their stock screens.
 */
internal object AndroidXmbLinks {
    val pending = MutableStateFlow<XmbOpen?>(null)

    fun open(target: XmbOpen) {
        pending.value = target
    }
}

/** Where the XMB shows [destination], or null when only a stock screen can. */
internal suspend fun xmbOpenFor(destination: IncomingDestination): XmbOpen? =
    when (destination) {
        IncomingDestination.Player -> {
            XmbOpen.nowPlaying()
        }

        IncomingDestination.Home, IncomingDestination.Library -> {
            XmbOpen(XmbCategoryId.MUSIC)
        }

        IncomingDestination.Downloads -> {
            XmbOpen.downloads(getString(Res.string.downloads))
        }

        IncomingDestination.ListenTogetherRoom -> {
            XmbOpen(XmbCategoryId.FRIENDS)
        }

        IncomingDestination.Settings, IncomingDestination.Updates -> {
            XmbOpen.settings()
        }

        IncomingDestination.BackupRestore -> {
            XmbOpen.settings(SettingsPageId.BACKUP_RESTORE)
        }

        is IncomingDestination.Search -> {
            destination.query?.takeIf(String::isNotBlank)?.let(XmbOpen::search)
                ?: XmbOpen(XmbCategoryId.SEARCH)
        }

        is IncomingDestination.Album -> {
            XmbOpen.album(destination.id, getString(Res.string.album))
        }

        is IncomingDestination.Artist -> {
            XmbOpen.artist(destination.id, getString(Res.string.artist))
        }

        is IncomingDestination.Playlist -> {
            XmbOpen.playlist(destination.id, getString(Res.string.playlist))
        }

        is IncomingDestination.Podcast -> {
            XmbOpen.podcast(destination.id, getString(Res.string.podcast))
        }

        is IncomingDestination.AutoPlaylist -> {
            autoPlaylist(destination.type)
        }

        else -> {
            null
        }
    }

private suspend fun autoPlaylist(type: String): XmbOpen =
    when (type.lowercase()) {
        "liked" -> XmbOpen(XmbCategoryId.MUSIC, XmbList.Liked, getString(Res.string.liked_songs))
        "downloaded", "downloads" -> XmbOpen.downloads(getString(Res.string.downloads))
        else -> XmbOpen(XmbCategoryId.MUSIC)
    }

/**
 * Where the XMB shows a stock route, or null for one it doesn't (sign-in, settings pages it doesn't
 * draw, recognition, browse...). `MetrolistApp` redirects its navigator with this, so a borrowed
 * screen that links to an album, an artist or the downloads opens them in the XMB. Titles are left
 * to the XMB's generic words.
 */
internal fun xmbOpenForScreen(screen: Screen): XmbOpen? =
    when (screen) {
        is Screen.Album -> XmbOpen.album(screen.id, "")
        is Screen.Artist, is Screen.AboutArtist -> XmbOpen.artist((screen as? Screen.Artist)?.id ?: (screen as Screen.AboutArtist).id, "")
        is Screen.Playlist -> XmbOpen.playlist(screen.id, "")
        is Screen.Podcast -> XmbOpen.podcast(screen.id, "")
        is Screen.AutoPlaylist -> autoPlaylistOpen(screen.type)
        Screen.Downloads -> XmbOpen(XmbCategoryId.MUSIC, XmbList.Downloads)
        Screen.History -> XmbOpen(XmbCategoryId.MUSIC, XmbList.History)
        is Screen.HomeSection -> XmbOpen(XmbCategoryId.MUSIC)
        Screen.Settings, Screen.Updates, Screen.UpdateDetails -> XmbOpen.settings()
        Screen.About -> XmbOpen.settings(SettingsPageId.ABOUT)
        Screen.Storage -> XmbOpen.settings(SettingsPageId.STORAGE)
        Screen.BackupRestore -> XmbOpen.settings(SettingsPageId.BACKUP_RESTORE)
        else -> null
    }

private fun autoPlaylistOpen(type: String): XmbOpen =
    when (type.lowercase()) {
        "liked" -> XmbOpen(XmbCategoryId.MUSIC, XmbList.Liked)
        "downloaded", "downloads" -> XmbOpen(XmbCategoryId.MUSIC, XmbList.Downloads)
        else -> XmbOpen(XmbCategoryId.MUSIC)
    }
