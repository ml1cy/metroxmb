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
import com.metrolist.shared.xmb.nav.XmbCategoryId
import com.metrolist.shared.xmb.theme.XmbIcons
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
            )
        for (screen in screens) {
            if (only != null && screen.name != only) continue
            val density = if (screen.width >= 1920) 1.5f else 1f
            val scene = ImageComposeScene(screen.width, screen.height, Density(density), content = screen.content)
            try {
                var image = scene.render(0)
                for (frame in 1..10) image = scene.render(frame * 50_000_000L)
                File(out, "${screen.name}.png").writeBytes(checkNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes)
            } finally {
                scene.close()
            }
            println("XMB snapshot: ${File(out, "${screen.name}.png")}")
        }
    }

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
