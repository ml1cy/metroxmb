package com.metrolist.android.xmb

import com.metrolist.android.ui.IncomingDestination
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.album
import com.metrolist.shared.generated.resources.artist
import com.metrolist.shared.generated.resources.downloads
import com.metrolist.shared.generated.resources.liked_songs
import com.metrolist.shared.generated.resources.playlist
import com.metrolist.shared.generated.resources.podcast
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.nav.XmbCategoryId
import com.metrolist.shared.xmb.nav.XmbOpen
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.getString

/**
 * Links, app shortcuts and notifications for the XMB. `MetrolistApp` turns each into a place the
 * XMB shows ([xmbOpenFor]); `XmbAndroidHost` passes it to the XMB. The player opens the XMB's
 * full-screen visualizer.
 */
internal object AndroidXmbLinks {
    val pending = MutableStateFlow<XmbOpen?>(null)

    fun open(target: XmbOpen) {
        pending.value = target
    }
}

/** A full-screen page over the XMB: Google's sign-in in an XMB frame. */
internal sealed interface AndroidXmbOverlay {
    data class SignIn(
        val switchingChannel: Boolean = false,
    ) : AndroidXmbOverlay
}

internal object AndroidXmbOverlays {
    val current = MutableStateFlow<AndroidXmbOverlay?>(null)

    fun show(overlay: AndroidXmbOverlay) {
        current.value = overlay
    }

    fun close() {
        current.value = null
    }
}

/** Where the XMB shows [destination]; null for those `MetrolistApp` handles itself (playing, sign-in, joining a room). */
internal suspend fun xmbOpenFor(destination: IncomingDestination): XmbOpen? =
    when (destination) {
        IncomingDestination.Recognition -> {
            XmbOpen.identifySong()
        }

        is IncomingDestination.Browse -> {
            XmbOpen.browse(destination.id, destination.params, destination.title.orEmpty())
        }

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
