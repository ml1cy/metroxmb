package com.metrolist.android.xmb

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.xmb.XmbApp
import com.metrolist.shared.xmb.XmbTime
import com.metrolist.shared.xmb.nav.XmbFullScreen
import com.metrolist.shared.xmb.nav.XmbVisualizerKind
import com.russhwolf.settings.MapSettings
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * The XMB through Android's own drawing (the ribbon mesh through the framework canvas, the particle
 * sprite, text), on a phone-sized screen: it must compose and draw something other than black.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(manifest = Config.NONE, sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class XmbAndroidRenderTest {
    @Before
    fun settings() = Settings.replaceSharedForTest(Settings(MapSettings()))

    @Test
    fun theMenuDraws() = assertDraws { XmbApp(time = { XmbTime(11, 18, 14, 30) }, animate = false) }

    @Test
    fun wavesDraws() =
        assertDraws {
            XmbApp(
                time = { XmbTime(11, 18, 14, 30) },
                animate = false,
                initialFullScreen = XmbFullScreen.Visualizer(XmbVisualizerKind.WAVES),
            )
        }

    private fun assertDraws(content: @androidx.compose.runtime.Composable () -> Unit) {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent(content = content)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        val view = activity.window.decorView
        assertTrue("laid out ${view.width}x${view.height}", view.width > 0 && view.height > 0)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val pixels =
            IntArray(
                bitmap.width * bitmap.height,
            ).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
        val lit = pixels.count { (it shr 16 and 0xff) + (it shr 8 and 0xff) + (it and 0xff) > 60 }
        assertTrue("only $lit of ${pixels.size} pixels are not black", lit > pixels.size / 4)
    }
}
