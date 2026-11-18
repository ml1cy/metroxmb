package com.metrolist.shared.xmb.adapters

import com.metrolist.shared.playlistimport.ImportProgressState
import com.metrolist.shared.playlistimport.PlaylistImportManager
import com.metrolist.shared.playlistimport.PlaylistImportStatusManager
import com.metrolist.shared.playlistimport.UnifiedPlaylistImporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/** What an XMB playlist import says when it ends. */
class XmbImportMessages(
    val started: String,
    /** "%1$s": the playlist's title; "%2$d" and "%3$d": songs added of the total. */
    val finished: String,
    val empty: String,
    val failed: String,
)

/**
 * Playlist import from a link (Spotify, Apple Music, Deezer, Tidal, Amazon Music or YouTube), as
 * the stock import screen does it: the playlist is read, its songs matched on YouTube Music and
 * saved as a new local playlist. It runs on its own scope (an import outlives the settings page)
 * and reports through [messages], which the XMB shows as toasts; the app's import status shows it
 * meanwhile, as for any import.
 */
class XmbPlaylistImport(
    private val manager: PlaylistImportManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val reports = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> get() = reports

    fun importLink(
        link: String,
        text: XmbImportMessages,
    ) {
        val input = link.trim()
        if (input.isEmpty()) return
        reports.tryEmit(text.started)
        scope.launch {
            val data =
                try {
                    UnifiedPlaylistImporter.parseFromInput(input).getOrThrow()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    reports.emit(text.failed)
                    return@launch
                }
            if (data.tracks.isEmpty()) {
                reports.emit(text.empty)
                return@launch
            }
            val statusId = PlaylistImportStatusManager.startImport(data.title, data.source)
            UnifiedPlaylistImporter
                .executeImport(
                    manager = manager,
                    title = data.title,
                    tracks = data.tracks,
                    destination = PlaylistImportManager.Destination.NewLocal,
                    provider = data.provider,
                    sourceYouTubePlaylistId = data.sourceYouTubePlaylistId,
                    coverUrl = data.coverUrl,
                ) { state -> PlaylistImportStatusManager.updateProgress(statusId, state) }
                .onSuccess { completed ->
                    PlaylistImportStatusManager.complete(statusId, completed)
                    reports.emit(
                        text.finished
                            .replace("%1\$s", data.title)
                            .replace("%2\$d", completed.importedTracks.toString())
                            .replace("%3\$d", data.tracks.size.toString()),
                    )
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    PlaylistImportStatusManager.fail(statusId, ImportProgressState.Failed(message = error.message.orEmpty()))
                    reports.emit(text.failed)
                }
        }
    }
}
