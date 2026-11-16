package com.metrolist.shared.xmb

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.xmb_cat_friends
import com.metrolist.shared.generated.resources.xmb_cat_music
import com.metrolist.shared.generated.resources.xmb_cat_search
import com.metrolist.shared.generated.resources.xmb_cat_settings
import com.metrolist.shared.generated.resources.xmb_cat_users
import com.metrolist.shared.generated.resources.xmb_cat_video
import com.metrolist.shared.generated.resources.xmb_item_playlist
import com.metrolist.shared.generated.resources.xmb_item_volume
import com.metrolist.shared.model.AlbumTiny
import com.metrolist.shared.model.ArtistTiny
import com.metrolist.shared.model.HomeSection
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.ui.playback.PlaybackController
import com.metrolist.shared.ui.playback.PlaybackUiState
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.ui.settings.SettingsPlatform
import com.metrolist.shared.ui.settings.SharedSettingsHost
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.adapters.XmbListState
import com.metrolist.shared.xmb.adapters.XmbMusicSource
import com.metrolist.shared.xmb.adapters.toXmbEntry
import com.metrolist.shared.xmb.nav.XmbCategoryId
import com.metrolist.shared.xmb.nav.XmbIcon
import com.metrolist.shared.xmb.nav.XmbKeyboard
import com.metrolist.shared.xmb.nav.XmbLevel
import com.metrolist.shared.xmb.nav.XmbPicker
import com.metrolist.shared.xmb.theme.XmbIcons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test

/**
 * Dev capture for `scripts/xmb/snap.sh`, not a test: renders XMB screens headlessly into
 * `compare/` for side-by-side checks against reference frames. Skipped unless `XMB_SNAP_OUT` is
 * set. `XMB_SNAP` picks one screen (default: all).
 */
class XmbSnapshotCapture {
    private val output = System.getenv("XMB_SNAP_OUT")?.let(::File)

    /** A local image standing in for album art, so captures need no network. */
    private val artworkFile =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?.let { File(it, "fastlane/metadata/android/en-US/images/icon.png") }
            ?.takeIf(File::isFile)
            ?.toURI()
            ?.toString()
    private val only = System.getenv("XMB_SNAP")?.takeIf { it.isNotBlank() && it != "all" }

    @Test
    fun capture() {
        assumeTrue("Set XMB_SNAP_OUT (scripts/xmb/snap.sh) to capture XMB screens", output != null)
        val out = checkNotNull(output).apply { mkdirs() }
        val day = XmbTime(month = 11, day = 16, hour = 14, minute = 30)
        val screens =
            listOf(
                Screen("icons", 1280, 200) { CategoryIconSheet() },
                Screen("shell") { XmbApp({}, time = { day }, animate = false) },
                Screen("shell-night") { XmbApp({}, time = { day.copy(hour = 23) }, animate = false) },
                Screen("music-scrolled") { XmbApp({}, time = { day }, animate = false, initialItem = 4) },
                Screen("settings") { XmbApp({}, time = { day }, animate = false, initialCategory = XmbCategoryId.SETTINGS) },
                Screen("visualizer-august") {
                    XmbApp({}, time = { day.copy(month = 8) }, animate = false, initialCategory = XmbCategoryId.VISUALIZER)
                },
                Screen("small-window", 960, 600) { XmbApp({}, time = { day }, animate = false) },
                Screen("music-home") {
                    XmbApp({}, services = demoServices, time = { day }, animate = false, initialLevels = listOf(homeLevel))
                },
                Screen("music-section") {
                    XmbApp(
                        {},
                        services = demoServices,
                        time = { day },
                        animate = false,
                        initialLevels = listOf(homeLevel.copy(focus = 1), sectionLevel.copy(focus = 2)),
                    )
                },
                Screen("settings-december") {
                    XmbApp({
                    }, services = demoServices, time = {
                        day.copy(
                            month = 12,
                        )
                    }, animate = false, initialCategory = XmbCategoryId.SETTINGS, initialItem = 1)
                },
                Screen("settings-page") {
                    XmbApp({
                    }, services = demoServices, time = {
                        day
                    }, animate = false, initialCategory = XmbCategoryId.SETTINGS, initialLevels = listOf(playerLevel.copy(focus = 2)))
                },
                Screen("settings-picker") {
                    XmbApp(
                        {},
                        services = demoServices,
                        time = { day },
                        animate = false,
                        initialCategory = XmbCategoryId.SETTINGS,
                        initialLevels = listOf(playerLevel.copy(focus = 2)),
                        initialPicker = XmbPicker.Choice("Audio quality", listOf("Automatic", "High", "Low"), focus = 1) {},
                    )
                },
                Screen("search-keyboard") {
                    XmbApp(
                        {},
                        services = demoServices,
                        time = { day },
                        animate = false,
                        initialCategory = XmbCategoryId.SEARCH,
                        initialKeyboard =
                            XmbKeyboard(
                                title = "New search",
                                text = "midnight d",
                                row = 2,
                                column = 3,
                                suggestions = listOf("midnight drive", "midnight drive neon coast", "midnight dreams"),
                            ),
                    )
                },
                Screen("users") {
                    XmbApp({}, services = demoServices, time = { day }, animate = false, initialCategory = XmbCategoryId.USERS)
                },
                Screen("music-options") {
                    XmbApp(
                        {},
                        services = demoServices,
                        time = { day },
                        animate = false,
                        initialLevels = listOf(homeLevel.copy(focus = 1), sectionLevel.copy(focus = 2)),
                        initialOptions = true,
                    )
                },
            )
        for (screen in screens) {
            if (only != null && screen.name != only) continue
            val density = if (screen.width >= 1920) 1.5f else 1f
            val scene = ImageComposeScene(screen.width, screen.height, Density(density), content = screen.content)
            try {
                var image = scene.render(0)
                for (frame in 1..10) {
                    // Artwork decodes off the render thread; give it a moment between frames.
                    Thread.sleep(40)
                    image = scene.render(frame * 50_000_000L)
                }
                File(out, "${screen.name}.png").writeBytes(checkNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes)
            } finally {
                scene.close()
            }
            println("XMB snapshot: ${File(out, "${screen.name}.png")}")
        }
    }

    private val demoSongs =
        listOf(
            "Midnight Drive" to "Neon Coast",
            "Paper Lanterns" to "Saltwater Sky",
            "Cold Harbor" to "The Northern Lines",
            "Afterglow" to "Mira Vale",
            "Slow Satellites" to "Echo Garden",
            "Glass Rivers" to "Neon Coast",
            "Weekend Static" to "The Low Tides",
        ).mapIndexed { index, (title, artist) ->
            SongItem(
                id = "demo$index",
                title = title,
                thumbnail = artworkFile.takeIf { index == 2 },
                artists = listOf(ArtistTiny("UCdemo$index", artist)),
                album = AlbumTiny("MPREbdemo$index", "$title (Single)"),
                duration = 215,
            )
        }
    private val sections =
        listOf(
            HomeSection("Quick picks", demoSongs),
            HomeSection("Listen again", demoSongs.reversed()),
            HomeSection("New releases", demoSongs.take(4)),
            HomeSection("From your library", demoSongs.drop(2)),
        )
    private val homeLevel = XmbLevel(XmbList.Home, "Home", XmbIcon.Drawable(Res.drawable.xmb_item_playlist))
    private val sectionLevel =
        XmbLevel(XmbList.Section(sections[1], 1), sections[1].title, XmbIcon.Drawable(Res.drawable.xmb_item_playlist))
    private val demoServices =
        XmbServices(
            playback =
                object : PlaybackController {
                    override val state =
                        MutableStateFlow(
                            PlaybackUiState(currentSong = demoSongs[2], isPlaying = true, positionMs = 83_000, durationMs = 215_000),
                        )

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
                },
            music =
                XmbMusicSource { list ->
                    when (list) {
                        XmbList.Home -> flowOf(XmbListState.loaded(sections.mapIndexed { index, section -> section.toXmbEntry(index) }))
                        is XmbList.Section -> flowOf(XmbListState.loaded(list.section.items.map { it.toXmbEntry() }))
                        else -> flowOf(XmbListState.loaded(emptyList()))
                    }
                },
            settings =
                XmbSettingsHost(
                    host = {
                        SharedSettingsHost(
                            platform = SettingsPlatform.DESKTOP,
                            onClose = {},
                            availablePages =
                                setOf(
                                    SettingsPageId.STORAGE,
                                    SettingsPageId.UPDATES,
                                    SettingsPageId.ABOUT,
                                    SettingsPageId.EQUALIZER,
                                ),
                        )
                    },
                    openStockPage = {},
                ),
            account =
                object : XmbAccount {
                    override fun signIn() = Unit

                    override fun switchChannel() = Unit

                    override fun signOut() = Unit
                },
        )
    private val playerLevel =
        XmbLevel(XmbList.SettingsPage(SettingsPageId.PLAYER), "Player", XmbIcon.Drawable(Res.drawable.xmb_item_volume))

    private class Screen(
        val name: String,
        val width: Int = 1920,
        val height: Int = 1080,
        val content: @Composable () -> Unit,
    )

    /** Every category icon at 128 px on the XMB's blue, PNG set first, then the two new vectors. */
    @Composable
    private fun CategoryIconSheet() {
        val label = TextStyle(color = Color.White, fontSize = 13.sp)
        Row(
            Modifier.fillMaxSize().background(Color(0xFF0C4F8A)).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            val pngs =
                listOf(
                    "users" to Res.drawable.xmb_cat_users,
                    "settings" to Res.drawable.xmb_cat_settings,
                    "music" to Res.drawable.xmb_cat_music,
                    "search" to Res.drawable.xmb_cat_search,
                    "video" to Res.drawable.xmb_cat_video,
                    "friends" to Res.drawable.xmb_cat_friends,
                )
            for ((name, resource) in pngs) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(resource), contentDescription = null, modifier = Modifier.size(128.dp))
                    BasicText(name, style = label)
                }
            }
            for ((name, vector) in listOf("podcasts (new)" to XmbIcons.Podcasts, "visualizer (new)" to XmbIcons.Visualizer)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(128.dp).background(Color.White.copy(alpha = 0.08f))) {
                        Image(vector, contentDescription = null, modifier = Modifier.size(128.dp))
                    }
                    BasicText(name, style = label)
                }
            }
        }
    }
}
