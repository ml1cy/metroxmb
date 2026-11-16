package com.metrolist.shared.xmb

import com.metrolist.shared.visualizer.VisualizerAudio
import com.metrolist.shared.visualizer.VisualizerAudio.Companion.shaped
import com.metrolist.shared.xmb.nav.XmbCategoryId
import com.metrolist.shared.xmb.nav.XmbItemAction
import com.metrolist.shared.xmb.nav.XmbVisualizerKind
import com.metrolist.shared.xmb.nav.xmbCategories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VisualizerAudioTest {
    private fun levels(
        bass: Float,
        mid: Float = 0f,
        treble: Float = 0f,
    ) = FloatArray(18) { band ->
        when (band) {
            in VisualizerAudio.BASS -> bass
            in VisualizerAudio.MID -> mid
            else -> treble
        }
    }

    private fun VisualizerAudio.run(
        levels: FloatArray?,
        seconds: Float,
    ) {
        var left = seconds
        while (left > 0f) {
            update(levels, minOf(1f / 60f, left))
            left -= 1f / 60f
        }
    }

    @Test
    fun bandGroupsStretchTheMeterRange() {
        assertEquals(0f, levels(0.2f).shaped(VisualizerAudio.BASS))
        assertEquals(1f, levels(0.95f).shaped(VisualizerAudio.BASS))
        assertEquals(0.5f, levels(0.625f).shaped(VisualizerAudio.BASS), 0.001f)
        assertEquals(0f, null.shaped(VisualizerAudio.BASS))
        assertEquals(0f, FloatArray(2).shaped(VisualizerAudio.TREBLE), "Missing bands read as silence")
    }

    @Test
    fun followsTheMusicQuicklyUpAndSlowlyDown() {
        val audio = VisualizerAudio()
        audio.run(levels(0.9f, 0.8f, 0.7f), 0.25f)
        assertTrue(audio.bass > 0.85f, "Attack within a quarter second")
        assertTrue(audio.mid in 0.7f..0.85f)
        assertTrue(audio.treble in 0.55f..0.7f)
        audio.run(null, 0.25f)
        assertTrue(audio.bass in 0.2f..0.6f, "Release is slower")
        audio.run(null, 5f)
        assertTrue(audio.isSilent)
    }

    @Test
    fun aBassOnsetIsABeatButASteadyToneIsNot() {
        val audio = VisualizerAudio()
        audio.run(levels(0.4f), 2f)
        assertEquals(0f, audio.beat)
        audio.run(levels(0.95f), 0.05f)
        assertTrue(audio.beat > 0.7f, "Onset fires a beat")
        audio.run(levels(0.95f), 3f)
        assertEquals(0f, audio.beat, "A held level stops beating")
    }

    @Test
    fun beatsDontRepeatFasterThanTheMinimumGap() {
        val audio = VisualizerAudio()
        audio.run(levels(0.95f), 0.05f)
        val first = audio.beat
        audio.run(levels(0.3f), 0.03f)
        audio.run(levels(0.95f), 0.03f)
        assertTrue(audio.beat < first, "A second onset within ${VisualizerAudio.MIN_BEAT_GAP}s doesn't re-fire")
    }

    @Test
    fun flowRunsFasterWhenLoud() {
        val quiet = VisualizerAudio().apply { run(null, 1f) }
        val loud = VisualizerAudio().apply { run(levels(0.95f, 0.95f, 0.95f), 1f) }
        assertEquals(1f, quiet.flow, 0.01f)
        assertTrue(loud.flow > 1.8f)
    }

    @Test
    fun longFramesAreCapped() {
        val audio = VisualizerAudio()
        audio.update(levels(0.95f), 30f)
        assertEquals(VisualizerAudio.MAX_STEP, audio.flow, 0.05f)
    }

    @Test
    fun onlyWavesOpensUntilTheOtherVisualizersExist() {
        val items = xmbCategories().single { it.id == XmbCategoryId.VISUALIZER }.items
        assertEquals(XmbItemAction.OpenVisualizer(XmbVisualizerKind.WAVES), items.first().action)
        assertEquals(listOf("visualizer_waves"), items.filter { it.isAvailable }.map { it.id })
    }
}
