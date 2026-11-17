package com.metrolist.shared.visualizer

import com.metrolist.shared.equalizer.AudioSpectrumMeter
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.xmb.sound.decodeXmbSound
import kotlinx.coroutines.runBlocking
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Waves against music rather than made-up band levels: audio goes through [AudioSpectrumMeter]
 * in real time the way playback feeds it (a chunk every 60 fps frame, the meter polled every
 * 33 ms) into [VisualizerAudio]. The real track is the console's menu music from the owner's
 * assets: quiet (about −21 dB mean), which a fixed level floor read as silence.
 */
class VisualizerMusicTest {
    @BeforeTest
    fun start() = AudioSpectrumMeter.addConsumer()

    @AfterTest
    fun stop() = AudioSpectrumMeter.removeConsumer()

    @Test
    fun theMenuMusicMovesWaves() {
        val pcm = decodeXmbSound(runBlocking { Res.readBytes("files/xmb/sounds/menu_bgm.ogg") })
        val samples =
            ShortArray(pcm.data.size / 2) { i -> ((pcm.data[2 * i + 1].toInt() shl 8) or (pcm.data[2 * i].toInt() and 0xff)).toShort() }
        val seconds = samples.size / pcm.channels / pcm.sampleRate.toDouble()
        val run = play(seconds + SILENCE_SECONDS, pcm.sampleRate, pcm.channels) { frame -> samples.getOrNull(frame) ?: 0 }
        val music = run.during(1.0, seconds)
        val bpm = run.onsets.count { it < seconds } / seconds * 60
        val gaps = run.onsets.zipWithNext { a, b -> b - a }.sorted()
        val report = "energy ${music.percentiles { it.energy }} bass ${music.percentiles { it.bass }} beats ${"%.0f".format(bpm)}/min"
        assertTrue(music.median { it.energy } > 0.15f, "barely moves: $report")
        assertTrue(music.percentile(0.9) { it.bass } > 0.6f, "bass never swells: $report")
        assertTrue(bpm in 5.0..150.0, "beats: $report")
        assertTrue(
            gaps.isEmpty() || gaps[gaps.size / 2] > 0.3,
            "beats fire in bursts (median gap ${gaps.getOrNull(gaps.size / 2)}): $report",
        )
        val after = run.during(seconds + 2.5, seconds + SILENCE_SECONDS)
        assertTrue(after.all { it.energy < 0.05f } && run.onsets.none { it > seconds + 0.3 }, "doesn't settle after the music: $report")
    }

    @Test
    fun drumsMakeBeatsAndPadsDont() {
        val random = Random(7)
        val drums = 8.0
        val pads = 4.0
        val run =
            play(drums + pads + SILENCE_SECONDS, SYNTH_RATE, 2) { frame ->
                val t = (frame / 2) / SYNTH_RATE.toDouble()
                (synth(t, drums, pads, random).coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }
        val kicks = (0 until (drums / BEAT).toInt()).map { it * BEAT }
        val drumBeats = run.onsets.filter { it < drums }
        val hits = kicks.count { kick -> drumBeats.any { it - kick in 0.0..0.2 } }
        assertTrue(hits >= kicks.size * 3 / 4, "only $hits of ${kicks.size} kicks made a beat: $drumBeats")
        assertTrue(drumBeats.size <= kicks.size + 2, "extra beats: $drumBeats")
        assertTrue(run.onsets.count { it in drums + 0.5..drums + pads } <= 2, "beats during pads: ${run.onsets}")
        assertTrue(run.onsets.none { it > drums + pads + 0.3 }, "beats in silence: ${run.onsets}")
    }

    private class Frame(
        val time: Double,
        val bass: Float,
        val energy: Float,
    )

    private class Run(
        val onsets: List<Double>,
        val frames: List<Frame>,
    ) {
        fun during(
            from: Double,
            to: Double,
        ) = frames.filter { it.time in from..to }
    }

    private fun List<Frame>.percentile(
        share: Double,
        value: (Frame) -> Float,
    ): Float = map(value).sorted().let { it[((it.size - 1) * share).toInt()] }

    private fun List<Frame>.median(value: (Frame) -> Float) = percentile(0.5, value)

    private fun List<Frame>.percentiles(value: (Frame) -> Float) =
        listOf(0.1, 0.5, 0.9).joinToString("/") { "%.2f".format(percentile(it, value)) }

    /** Plays [sample] (interleaved, by index) for [seconds] through the meter into a [VisualizerAudio]. */
    private fun play(
        seconds: Double,
        rate: Int,
        channels: Int,
        sample: (Int) -> Short,
    ): Run {
        val audio = VisualizerAudio()
        val onsets = mutableListOf<Double>()
        val frames = mutableListOf<Frame>()
        var position = 0
        var nextPoll = 0.0
        var lastBeat = 0f
        for (frame in 0 until (seconds * FPS).toInt()) {
            val now = frame / FPS
            val end = ((frame + 1) / FPS * rate).toInt()
            val chunk = ShortArray((end - position) * channels) { sample(position * channels + it) }
            AudioSpectrumMeter.accept(chunk, chunk.size, channels, rate)
            position = end
            if (now >= nextPoll) {
                AudioSpectrumMeter.levels()
                audio.input = AudioSpectrumMeter.frameLevels()
                nextPoll += POLL_SECONDS
            }
            audio.step((1.0 / FPS).toFloat())
            if (audio.beat == 1f && lastBeat < 1f) onsets += now
            lastBeat = audio.beat
            frames += Frame(now, audio.bass, audio.energy)
        }
        return Run(onsets, frames)
    }

    /** A 120 BPM track: kick, bass line, off-beat hi-hat and chords, then chords alone swelling, then silence. */
    private fun synth(
        t: Double,
        drums: Double,
        pads: Double,
        random: Random,
    ): Double {
        if (t >= drums + pads) return 0.0
        val chord = listOf(220.0, 277.2, 329.6).map { it * if ((t / 2).toInt() % 2 == 0) 1.0 else 0.89 }
        if (t >= drums) {
            val swell = 0.5 + 0.5 * sin(2 * PI * (t - drums) / 4)
            return chord.sumOf { sin(2 * PI * it * t) + 0.3 * sin(4 * PI * it * t) } * 0.06 * swell
        }
        val inBeat = t % BEAT
        val kick = sin(2 * PI * (50 + 70 * exp(-inBeat * 30)) * inBeat) * exp(-inBeat * 20) * 0.45
        val bass = sin(2 * PI * listOf(55.0, 55.0, 82.4, 73.4)[((t / (BEAT / 2)).toInt()) % 4] * t) * 0.12
        val offBeat = (t + BEAT / 2) % BEAT
        val hat = if (offBeat < 0.05) (random.nextDouble() * 2 - 1) * exp(-offBeat * 80) * 0.15 else 0.0
        return kick + bass + hat + chord.sumOf { sin(2 * PI * it * t) } * 0.04
    }

    private companion object {
        const val FPS = 60.0
        const val POLL_SECONDS = 0.033
        const val SILENCE_SECONDS = 4.0
        const val SYNTH_RATE = 44_100
        const val BEAT = 0.5
    }
}
