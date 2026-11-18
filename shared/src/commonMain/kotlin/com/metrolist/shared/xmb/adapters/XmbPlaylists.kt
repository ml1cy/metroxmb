package com.metrolist.shared.xmb.adapters

import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.playlist.addSongToStoredPlaylist
import com.metrolist.shared.playlist.createStoredLocalPlaylist
import com.metrolist.shared.state.GlobalYouTubeRepository
import kotlinx.coroutines.flow.first

/** The playlists a song can be added to, for the XMB's Add to Playlist option. */
interface XmbPlaylists {
    /** Editable playlists, as id to name, oldest first. */
    suspend fun editable(): List<Pair<String, String>>

    suspend fun add(
        playlistId: String,
        song: SongItem,
    ): Boolean

    /** Creates a local playlist named [name]; its id, or null when that failed. */
    suspend fun create(name: String): String?
}

/** Through the library database and the shared playlist mutations, as the stock add-to-playlist dialog. */
class DatabaseXmbPlaylists(
    private val database: MusicDatabase,
) : XmbPlaylists {
    override suspend fun editable(): List<Pair<String, String>> =
        database
            .dao()
            .editablePlaylistsByCreateDateAsc()
            .first()
            .map { it.playlist.id to it.playlist.name }

    override suspend fun add(
        playlistId: String,
        song: SongItem,
    ): Boolean = addSongToStoredPlaylist(database, GlobalYouTubeRepository.instance, playlistId, song, allowDuplicate = true).isSuccess

    override suspend fun create(name: String): String? = createStoredLocalPlaylist(database, name).getOrNull()?.id
}
