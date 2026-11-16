package com.metrolist.shared.xmb.nav

import com.metrolist.shared.model.SongItem
import com.metrolist.shared.xmb.adapters.XmbEntry
import com.metrolist.shared.xmb.adapters.XmbEntryKind
import com.metrolist.shared.xmb.adapters.song

/** Actions in the △ options panel. */
enum class XmbOption {
    PLAY,
    SHUFFLE,
    PLAY_NEXT,
    ADD_TO_QUEUE,
    START_RADIO,
    LIKE,
    UNLIKE,
    DOWNLOAD,
    REMOVE_DOWNLOAD,
    VIEW_ALBUM,
    VIEW_ARTIST,
}

/**
 * The options for a row. [liked] and [downloaded] describe its song; [canDownload] is whether the
 * platform supports downloads. Kinds the XMB can't act on yet get no options.
 */
fun xmbOptionsFor(
    entry: XmbEntry,
    liked: Boolean,
    downloaded: Boolean,
    canDownload: Boolean,
): List<XmbOption> =
    when (entry.kind) {
        XmbEntryKind.SONG, XmbEntryKind.EPISODE -> {
            val song = entry.song
            buildList {
                add(XmbOption.PLAY)
                add(XmbOption.PLAY_NEXT)
                add(XmbOption.ADD_TO_QUEUE)
                if (entry.kind == XmbEntryKind.SONG) {
                    add(XmbOption.START_RADIO)
                    add(if (liked) XmbOption.UNLIKE else XmbOption.LIKE)
                }
                if (canDownload && song?.isLocal != true) add(if (downloaded) XmbOption.REMOVE_DOWNLOAD else XmbOption.DOWNLOAD)
                if (song?.albumId() != null && song.podcastId == null) add(XmbOption.VIEW_ALBUM)
                if (song?.artistId() != null) add(XmbOption.VIEW_ARTIST)
            }
        }

        XmbEntryKind.ALBUM, XmbEntryKind.PLAYLIST, XmbEntryKind.SECTION -> {
            listOf(XmbOption.PLAY, XmbOption.SHUFFLE, XmbOption.PLAY_NEXT, XmbOption.ADD_TO_QUEUE)
        }

        XmbEntryKind.ARTIST -> {
            listOf(XmbOption.PLAY, XmbOption.SHUFFLE)
        }

        XmbEntryKind.PODCAST, XmbEntryKind.BROWSE -> {
            emptyList()
        }
    }

internal fun SongItem.albumId(): String? = album?.id?.takeIf(String::isNotBlank)

internal fun SongItem.artistId(): String? = artists.firstNotNullOfOrNull { it.id?.takeIf(String::isNotBlank) }

/** The open △ panel: the row it acts on, its options and the focused option. */
data class XmbOptionsPanel(
    val entry: XmbEntry,
    val options: List<XmbOption>,
    val focus: Int = 0,
)

/** Result of input while the panel is open: the panel ([panel] null once closed), sound and chosen option. */
data class XmbOptionsStep(
    val panel: XmbOptionsPanel?,
    val feedback: XmbFeedback,
    val chosen: XmbOption? = null,
)

fun XmbOptionsPanel.reduce(action: XmbAction): XmbOptionsStep =
    when (action) {
        XmbAction.UP -> {
            if (focus > 0) XmbOptionsStep(copy(focus = focus - 1), XmbFeedback.MOVE_UP) else XmbOptionsStep(this, XmbFeedback.NONE)
        }

        XmbAction.DOWN -> {
            if (focus < options.lastIndex) {
                XmbOptionsStep(copy(focus = focus + 1), XmbFeedback.MOVE_DOWN)
            } else {
                XmbOptionsStep(this, XmbFeedback.NONE)
            }
        }

        XmbAction.SELECT -> {
            options.getOrNull(focus)?.let { XmbOptionsStep(null, XmbFeedback.SELECT, it) } ?: XmbOptionsStep(null, XmbFeedback.ERROR)
        }

        XmbAction.BACK, XmbAction.OPTIONS, XmbAction.LEFT -> {
            XmbOptionsStep(null, XmbFeedback.BACK)
        }

        XmbAction.RIGHT, XmbAction.PLAY_PAUSE, XmbAction.NEXT_TRACK, XmbAction.PREVIOUS_TRACK -> {
            XmbOptionsStep(this, XmbFeedback.NONE)
        }
    }
