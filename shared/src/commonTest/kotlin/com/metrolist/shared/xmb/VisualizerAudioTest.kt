package com.metrolist.shared.xmb

import com.metrolist.shared.visualizer.RangeFollower
import com.metrolist.shared.visualizer.VisualizerAudio
import com.metrolist.shared.visualizer.VisualizerAudio.Companion.average
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

    /** Music: every band group pulses between [low] and [high] twice a second, the high part first. */
    private fun VisualizerAudio.pulse(
        low: Float,
        high: Float,
        seconds: Float,
    ) {
        var time = 0f
        while (time < seconds) {
            val loud = time % 0.5f < 0.13f
            update(levels(if (loud) high else low, if (loud) high else low, if (loud) high else low), 1f / 60f)
            time += 1f / 60f
        }
    }

    @Test
    fun bandGroupsAverageTheirBands() {
        assertEquals(0.6f, levels(0.6f).average(VisualizerAudio.BASS), 0.001f)
        assertEquals(0f, null.average(VisualizerAudio.BASS))
        assertEquals(0f, FloatArray(2).average(VisualizerAudio.TREBLE), "Missing bands read as silence")
    }

    @Test
    fun quietAndLoudRecordingsBothUseTheWholeRange() {
        // A pulse twice a second, quiet (0.2–0.32, like the console's menu music) or loud (0.6–0.8).
        for ((low, high) in listOf(0.2f to 0.32f, 0.6f to 0.8f)) {
            val audio = VisualizerAudio()
            var lowest = 1f
            var highest = 0f
            for (step in 0 until 600) {
                val inBeat = step % 30
                audio.update(levels(if (inBeat < 8) high else low), 1f / 60f)
                if (step >= 300) {
                    lowest = minOf(lowest, audio.bass)
                    highest = maxOf(highest, audio.bass)
                }
            }
            assertTrue(highest > 0.8f && lowest < 0.3f, "$low–$high swings only $lowest–$highest")
        }
    }

    @Test
    fun aSmallWobbleIsNotBlownUp() {
        val follower = RangeFollower()
        var top = 0f
        for (step in 0 until 600) {
            val level = 0.5f + if (step % 20 < 10) 0.01f else -0.01f
            val value = follower.normalize(level, 1f / 60f)
            if (step > 400) top = maxOf(top, value)
        }
        assertTrue(top < 0.4f, "a ±0.01 wobble reached $top")
        assertEquals(0f, follower.normalize(RangeFollower.SILENCE / 2f, 1f / 60f), "Near-silence reads as 0")
    }

    @Test
    fun followsTheMusicQuicklyUpAndSlowlyDown() {
        val audio = VisualizerAudio()
        audio.pulse(0.4f, 0.8f, 2f)
        audio.run(levels(0.8f, 0.8f, 0.8f), 0.2f)
        assertTrue(audio.bass > 0.85f && audio.mid > 0.85f && audio.treble > 0.85f, "Attack within a fifth of a second")
        audio.run(levels(0.4f, 0.4f, 0.4f), 0.25f)
        assertTrue(audio.bass in 0.2f..0.6f, "Release is slower: ${audio.bass}")
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
        audio.pulse(0.3f, 0.3f, 1f)
        audio.run(levels(0.95f), 0.05f)
        val first = audio.beat
        audio.run(levels(0.3f), 0.03f)
        audio.run(levels(0.95f), 0.03f)
        assertTrue(audio.beat < first, "A second onset within ${VisualizerAudio.MIN_BEAT_GAP}s doesn't re-fire")
    }

    @Test
    fun flowRunsFasterWhenLoud() {
        val quiet = VisualizerAudio().apply { run(null, 2f) }
        val loud = VisualizerAudio().apply { pulse(0.3f, 0.9f, 2f) }
        assertEquals(2f, quiet.flow, 0.01f)
        assertTrue(loud.flow > 2.6f, "flow ${loud.flow}")
    }

    @Test
    fun particlesRushInLoudPassages() {
        val quiet = VisualizerAudio().apply { run(null, 2f) }
        val loud = VisualizerAudio().apply { pulse(0.3f, 0.9f, 2f) }
        assertEquals(2f, quiet.particleFlow, 0.01f)
        assertTrue(loud.particleFlow > 5f, "Particles rush much faster than the ribbon: ${loud.particleFlow}")
        assertTrue(loud.particleFlow > loud.flow * 2f)
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
