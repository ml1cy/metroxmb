package com.metrolist.shared.xmb.adapters

import com.metrolist.shared.db.HistoryStatGroup
import com.metrolist.shared.db.HistoryStatisticsSnapshot
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.db.historyStatisticsSnapshot
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.n_album
import com.metrolist.shared.generated.resources.n_artist
import com.metrolist.shared.generated.resources.n_month
import com.metrolist.shared.generated.resources.n_time
import com.metrolist.shared.generated.resources.n_week
import com.metrolist.shared.generated.resources.n_year
import com.metrolist.shared.generated.resources.statistics_duration_hours_minutes
import com.metrolist.shared.generated.resources.statistics_duration_minutes
import com.metrolist.shared.generated.resources.statistics_lifetime
import com.metrolist.shared.generated.resources.statistics_play_count_duration
import com.metrolist.shared.generated.resources.statistics_top_albums
import com.metrolist.shared.generated.resources.statistics_top_artists
import com.metrolist.shared.generated.resources.statistics_top_tracks
import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.AlbumTiny
import com.metrolist.shared.model.ArtistItem
import com.metrolist.shared.model.ArtistTiny
import com.metrolist.shared.model.SongItem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/** The periods the XMB's Statistics folder offers, each counted back from now. */
enum class XmbStatsPeriod(
    val days: Int?,
) {
    WEEK(7),
    MONTH(30),
    YEAR(365),
    ALL(null),
    ;

    /** The epoch-millisecond range this period covers, ending at [nowMs]. */
    fun range(nowMs: Long): Pair<Long, Long> = (days?.let { nowMs - it.days.inWholeMilliseconds } ?: 0L) to nowMs
}

enum class XmbStatsKind { TRACKS, ARTISTS, ALBUMS }

/**
 * Builds the Statistics levels from the play history (the same snapshot the stock statistics
 * screen used). A period's snapshot is kept for [CACHE_MS] so opening its folders doesn't count
 * the history again.
 */
internal class XmbStatistics(
    private val database: MusicDatabase,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<XmbStatsPeriod, Pair<Long, HistoryStatisticsSnapshot>>()

    private suspend fun snapshot(period: XmbStatsPeriod): HistoryStatisticsSnapshot =
        mutex.withLock {
            val time = now()
            cache[period]?.takeIf { time - it.first < CACHE_MS }?.second
                ?: period.range(time).let { (since, until) -> database.historyStatisticsSnapshot(since, until) }.also {
                    cache[period] = time to it
                }
        }

    suspend fun periods(): List<XmbEntry> =
        XmbStatsPeriod.entries.map { period ->
            XmbEntry(
                key = "statistics:${period.name}",
                title = period.label(),
                subtitle = null,
                artwork = null,
                kind = XmbEntryKind.SECTION,
                open = XmbList.StatisticsPeriod(period),
            )
        }

    suspend fun period(period: XmbStatsPeriod): List<XmbEntry> {
        val snapshot = snapshot(period)
        val plays = getPluralString(Res.plurals.n_time, snapshot.totals.playCount, snapshot.totals.playCount)
        val tracks =
            if (snapshot.totals.playTimeMs > 0) {
                getString(Res.string.statistics_play_count_duration, plays, duration(snapshot.totals.playTimeMs))
            } else {
                plays
            }

        fun folder(
            kind: XmbStatsKind,
            title: String,
            subtitle: String,
            artwork: String?,
        ) = XmbEntry(
            key = "statistics:${period.name}:${kind.name}",
            title = title,
            subtitle = subtitle,
            artwork = artwork,
            kind = XmbEntryKind.SECTION,
            open = XmbList.StatisticsTop(period, kind),
        )
        return listOf(
            folder(XmbStatsKind.TRACKS, getString(Res.string.statistics_top_tracks), tracks, snapshot.songs.firstOrNull()?.thumbnailUrl),
            folder(
                XmbStatsKind.ARTISTS,
                getString(Res.string.statistics_top_artists),
                getPluralString(Res.plurals.n_artist, snapshot.artists.size, snapshot.artists.size),
                snapshot.artists.firstOrNull()?.thumbnailUrl,
            ),
            folder(
                XmbStatsKind.ALBUMS,
                getString(Res.string.statistics_top_albums),
                getPluralString(Res.plurals.n_album, snapshot.albums.size, snapshot.albums.size),
                snapshot.albums.firstOrNull()?.thumbnailUrl,
            ),
        )
    }

    suspend fun top(
        period: XmbStatsPeriod,
        kind: XmbStatsKind,
    ): List<XmbEntry> {
        val snapshot = snapshot(period)
        return when (kind) {
            XmbStatsKind.TRACKS -> {
                val items = snapshot.songItems.associateBy(SongItem::id)
                snapshot.songs.take(TOP_ROWS).mapIndexed { index, group ->
                    val entry = (items[group.songId] ?: group.toSongItem()).toXmbEntry()
                    entry.copy(
                        title = "${index + 1}. ${entry.title}",
                        subtitle =
                            listOfNotNull(
                                group.artists.ifBlank { null },
                                playCount(group.playCount, group.playTimeMs),
                            ).joinToString(" · "),
                    )
                }
            }

            XmbStatsKind.ARTISTS -> {
                snapshot.artists.take(TOP_ROWS).mapIndexed { index, artist ->
                    val item = artist.id?.let { ArtistItem(id = it, title = artist.name, thumbnail = artist.thumbnailUrl) }
                    XmbEntry(
                        key = "statistics:artist:${artist.id ?: artist.name}",
                        title = "${index + 1}. ${artist.name}",
                        subtitle = playCount(artist.playCount, artist.playTimeMs),
                        artwork = artist.thumbnailUrl,
                        kind = XmbEntryKind.ARTIST,
                        item = item,
                        open = item?.let(XmbList::Artist),
                    )
                }
            }

            XmbStatsKind.ALBUMS -> {
                snapshot.albums.take(TOP_ROWS).mapIndexed { index, album ->
                    val item =
                        album.id?.let {
                            AlbumItem(
                                id = it,
                                title = album.name,
                                thumbnail = album.thumbnailUrl,
                                artists =
                                    album.artists.split(", ").filter(String::isNotBlank).map { name ->
                                        ArtistTiny(id = null, name = name)
                                    },
                            )
                        }
                    XmbEntry(
                        key = "statistics:album:${album.id ?: album.name}",
                        title = "${index + 1}. ${album.name}",
                        subtitle =
                            listOfNotNull(
                                album.artists.ifBlank { null },
                                playCount(album.playCount, album.playTimeMs),
                            ).joinToString(" · "),
                        artwork = album.thumbnailUrl,
                        kind = XmbEntryKind.ALBUM,
                        item = item,
                        open = item?.let(XmbList::Album),
                    )
                }
            }
        }
    }

    private suspend fun playCount(
        count: Int,
        timeMs: Long,
    ): String {
        val plays = getPluralString(Res.plurals.n_time, count, count)
        return if (timeMs > 0) getString(Res.string.statistics_play_count_duration, plays, duration(timeMs)) else plays
    }

    private suspend fun duration(ms: Long): String {
        val minutes = ms / 60_000
        return if (minutes >= 60) {
            getString(Res.string.statistics_duration_hours_minutes, (minutes / 60).toInt(), (minutes % 60).toInt())
        } else {
            getString(Res.string.statistics_duration_minutes, minutes.toInt())
        }
    }

    private companion object {
        const val CACHE_MS = 60_000L
        const val TOP_ROWS = 100
    }
}

internal suspend fun XmbStatsPeriod.label(): String =
    when (this) {
        XmbStatsPeriod.WEEK -> getPluralString(Res.plurals.n_week, 1, 1)
        XmbStatsPeriod.MONTH -> getPluralString(Res.plurals.n_month, 1, 1)
        XmbStatsPeriod.YEAR -> getPluralString(Res.plurals.n_year, 1, 1)
        XmbStatsPeriod.ALL -> getString(Res.string.statistics_lifetime)
    }

private fun HistoryStatGroup.toSongItem(): SongItem =
    SongItem(
        id = songId,
        title = title,
        thumbnail = thumbnailUrl,
        artists = artists.split(", ").filter(String::isNotBlank).map { ArtistTiny(id = null, name = it) },
        album = album?.let { AlbumTiny(id = albumId, name = it) },
        duration = duration,
    )
