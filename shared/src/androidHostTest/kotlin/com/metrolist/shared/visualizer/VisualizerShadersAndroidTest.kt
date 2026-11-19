package com.metrolist.shared.visualizer

import com.metrolist.shared.visualizer.gaia.GAIA_SHADER
import com.metrolist.shared.visualizer.shader.XmbShader
import com.metrolist.shared.visualizer.valley.VALLEY_SHADER
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Valley and Gaia's shaders compile as AGSL on Android 13+, with every uniform the visualizers
 * set, and Android 12 reports them unavailable (the visualizers draw their fallbacks there).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisualizerShadersAndroidTest {
    @Test
    @Config(sdk = [34])
    fun shadersCompileWithTheirUniforms() {
        val valley = XmbShader(VALLEY_SHADER)
        assertTrue("Valley's AGSL didn't compile", valley.available)
        valley.set("iResolution", 1080f, 2340f)
        listOf("iTime", "bass", "energy", "beat", "layers", "octaves").forEach { valley.set(it, 1f) }
        listOf("haze", "ground", "glow").forEach { valley.set(it, 0.5f, 0.4f, 0.3f) }
        val gaia = XmbShader(GAIA_SHADER)
        assertTrue("Gaia's AGSL didn't compile", gaia.available)
        gaia.set("iResolution", 1080f, 2340f)
        gaia.set("center", 500f, 900f)
        gaia.set("sun", 0.3f, 0.4f, 0.8f)
        listOf("iTime", "radius", "energy", "beat", "octaves").forEach { gaia.set(it, 1f) }
    }

    @Test
    @Config(sdk = [32])
    fun olderAndroidUsesTheFallbacks() {
        assertFalse(XmbShader(VALLEY_SHADER).available)
    }
}
