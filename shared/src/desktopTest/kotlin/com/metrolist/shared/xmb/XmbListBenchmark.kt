package com.metrolist.shared.xmb

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.xmb_item_playlist
import com.metrolist.shared.model.AlbumTiny
import com.metrolist.shared.model.ArtistTiny
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.ui.playback.PlaybackController
import com.metrolist.shared.ui.playback.PlaybackUiState
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.adapters.XmbListState
import com.metrolist.shared.xmb.adapters.toXmbEntry
import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.nav.XmbIcon
import com.metrolist.shared.xmb.nav.XmbLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assume.assumeTrue
import kotlin.test.Test

/**
 * Frame times while scrolling a long list, against a short one (dev check, not a test: set
 * XMB_BENCH=1). Playback position moves every frame, as it does while music plays.
 */
class XmbListBenchmark {
    private fun songs(count: Int) =
        List(count) { index ->
            SongItem(
                id = "song$index",
                title = "Song number $index",
                thumbnail = null,
                artists = listOf(ArtistTiny("UC$index", "Artist ${index % 50}")),
                album = AlbumTiny("MPREb$index", "Album ${index % 80}"),
                duration = 200,
            )
        }

    private fun playback(queue: List<SongItem>) =
        object : PlaybackController {
            override val state =
                MutableStateFlow(PlaybackUiState(currentSong = queue.first(), isPlaying = true, durationMs = 200_000, queue = queue))

            override fun play(
                song: SongItem,
                queue: List<SongItem>,
                songsOnly: Boolean,
            ) = Unit

            override fun shuffleQueue(
                songs: List<SongItem>,
                queueTitle: String?,
                songsOnly: Boolean,
            ) = Unit

            override fun togglePlayPause() = Unit

            override fun seekTo(positionMs: Long) = Unit

            override fun skipToNext() = Unit

            override fun skipToPrevious() = Unit

            override fun setPlaybackSpeed(speed: Float) = Unit

            override fun startRadio(song: SongItem) = Unit

            override fun playNext(songs: List<SongItem>) = Unit

            override fun addToQueue(songs: List<SongItem>) = Unit
        }

    private fun run(
        name: String,
        count: Int,
        list: XmbList,
    ) {
        val all = songs(count)
        val controller = playback(all)
        val input = XmbInputController()
        val services =
            XmbServices(
                playback = controller,
                music = { flowOf(XmbListState.loaded(all.map { it.toXmbEntry() })) },
            )
        val level = XmbLevel(list, name, XmbIcon.Drawable(Res.drawable.xmb_item_playlist))
        val scene =
            ImageComposeScene(1280, 720, Density(1f)) {
                XmbApp(services = services, input = input, initialLevels = listOf(level))
            }
        try {
            var time = 0L
            repeat(30) {
                time += 16_666_667L
                scene.render(time)
            }
            val frames = 240
            val start = System.nanoTime()
            var worst = 0L
            repeat(frames) { frame ->
                if (frame % 4 == 0) input.send(XmbAction.DOWN)
                controller.state.value = controller.state.value.copy(positionMs = frame * 16L)
                time += 16_666_667L
                val t = System.nanoTime()
                scene.render(time)
                worst = maxOf(worst, System.nanoTime() - t)
            }
            val average = (System.nanoTime() - start) / frames / 1_000_000.0
            println("XMB bench $name ($count rows): ${"%.2f".format(average)} ms/frame, worst ${worst / 1_000_000} ms")
        } finally {
            scene.close()
        }
    }

    @Test
    fun scrollLongLists() {
        assumeTrue("Set XMB_BENCH=1 to time list scrolling", System.getenv("XMB_BENCH") != null)
        run("warmup", 20, XmbList.Liked)
        run("songs-short", 20, XmbList.Liked)
        run("songs-long", 5000, XmbList.Liked)
        run("queue-short", 20, XmbList.Queue)
        run("queue-long", 5000, XmbList.Queue)
    }
}
