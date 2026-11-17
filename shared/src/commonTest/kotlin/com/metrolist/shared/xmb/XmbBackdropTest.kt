package com.metrolist.shared.xmb

import androidx.compose.ui.graphics.Color
import com.metrolist.shared.visualizer.waves.RibbonClockBlend
import com.metrolist.shared.visualizer.waves.WAVES_SKY
import com.metrolist.shared.visualizer.waves.blendedSky
import com.metrolist.shared.xmb.screens.crossfade
import com.metrolist.shared.xmb.theme.XmbBackgroundColors
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XmbBackdropTest {
    @Test
    fun ribbonClockHandsOverWithoutAJump() {
        val clock = RibbonClockBlend()
        // Home alone: the home clock shows as is.
        assertEquals(12f, clock.blend(home = 12f, waves = 400f, t = 0f))
        // The fade starts where the home clock was, though the visualizer's clock is far away.
        assertTrue(abs(clock.blend(home = 12.01f, waves = 400.02f, t = 0.05f) - 12.01f) < 0.01f)
        // At the end the visualizer's clock runs at its own pace from there.
        val shown = clock.blend(home = 12.2f, waves = 400.5f, t = 1f)
        assertEquals(shown + 1f, clock.blend(home = 12.2f, waves = 401.5f, t = 1f), 0.0001f)
        // Going back starts from the visualizer's time, not the home clock's.
        val back = clock.blend(home = 12.3f, waves = 402f, t = 1f)
        assertTrue(abs(clock.blend(home = 12.31f, waves = 402f, t = 0.95f) - back) < 0.01f)
    }

    @Test
    fun skyBlendsFromTheHomeGradientToTheWavesViolet() {
        val colors = XmbBackgroundColors(top = Color.Red, bottom = Color.Blue, base = Color.Red)
        val home = blendedSky(colors, 0f)
        assertEquals(Color.Red, home.first().second)
        assertEquals(Color.Blue, home.last().second)
        assertEquals(WAVES_SKY.toList(), blendedSky(colors, 1f).toList())
    }

    @Test
    fun trackInfoCrossfadeHoldsAtTheEnds() {
        assertEquals(0f, crossfade(0f))
        assertEquals(0f, crossfade(0.2f))
        assertEquals(0.5f, crossfade(0.5f), 0.0001f)
        assertEquals(1f, crossfade(0.8f))
        assertEquals(1f, crossfade(1f))
    }
}
