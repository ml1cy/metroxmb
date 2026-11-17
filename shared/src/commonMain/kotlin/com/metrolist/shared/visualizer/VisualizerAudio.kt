package com.metrolist.shared.visualizer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.metrolist.shared.equalizer.AudioSpectrumMeter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile
import kotlin.math.exp
import kotlin.math.max

/**
 * What the visualizers react to, taken from [AudioSpectrumMeter]'s 18 bands: smoothed [bass],
 * [mid] and [treble] (0..1), overall [energy], a [beat] pulse that jumps to 1 on a bass onset and
 * decays, [flow], a clock that runs faster when the music is louder, and [particleFlow], which
 * rushes much faster still. Without meter data
 * (nothing playing, or a platform path the meter can't see) everything settles to 0 and the
 * visualizer keeps its idle motion. Plain fields, read while drawing.
 */
class VisualizerAudio {
    var bass = 0f
        private set
    var mid = 0f
        private set
    var treble = 0f
        private set
    var energy = 0f
        private set
    var beat = 0f
        private set
    var flow = 0f
        private set

    /** A clock for particles: it rushes in loud passages and on beats, so they streak. */
    var particleFlow = 0f
        private set

    /** The latest meter levels, written by the polling coroutine and read by the frame loop. */
    @Volatile
    var input: FloatArray? = null

    private val bassRange = RangeFollower()
    private val midRange = RangeFollower()
    private val trebleRange = RangeFollower()
    private var slowBass = 0f

    // A beat fires on a bass rise, then waits for the rise to settle before it can fire again,
    // so a long crescendo is one beat rather than one every [MIN_BEAT_GAP].
    private var armed = true
    private var sinceBeat = MIN_BEAT_GAP

    /** Advances by [seconds] using [input]. */
    fun step(seconds: Float) = update(input, seconds)

    /** Advances by [seconds] with [levels] (the meter's bands, low to high; null for silence). */
    fun update(
        levels: FloatArray?,
        seconds: Float,
    ) {
        val dt = seconds.coerceIn(0f, MAX_STEP)
        bass = follow(bass, bassRange.normalize(levels.average(BASS), dt), dt)
        mid = follow(mid, midRange.normalize(levels.average(MID), dt), dt)
        treble = follow(treble, trebleRange.normalize(levels.average(TREBLE), dt), dt)
        energy = follow(energy, (bass + mid + treble) / 3f, dt)
        slowBass += (bass - slowBass) * (1f - exp(-dt / SLOW_BASS_SECONDS))
        sinceBeat += dt
        val rise = bass - slowBass
        if (armed && rise > BEAT_RISE && bass > BEAT_FLOOR && sinceBeat >= MIN_BEAT_GAP) {
            beat = 1f
            sinceBeat = 0f
            armed = false
        } else {
            beat *= exp(-dt * BEAT_DECAY)
            if (beat < REST) beat = 0f
        }
        if (rise < BEAT_RISE * REARM) armed = true
        flow += dt * (1f + FLOW_BOOST * energy)
        particleFlow += dt * (1f + PARTICLE_RUSH * energy + PARTICLE_BEAT_RUSH * beat)
    }

    val isSilent: Boolean get() = bass == 0f && mid == 0f && treble == 0f && beat == 0f

    private fun follow(
        current: Float,
        target: Float,
        dt: Float,
    ): Float {
        val rate = if (target > current) ATTACK else RELEASE
        val next = current + (target - current) * (1f - exp(-dt * rate))
        return if (next < REST && target == 0f) 0f else next
    }

    companion object {
        /** Meter bands 0–3 (45–125 Hz), 4–10 (180 Hz–1.4 kHz), 11–17 (2–15 kHz). */
        val BASS = 0..3
        val MID = 4..10
        val TREBLE = 11..17

        const val ATTACK = 18f
        const val RELEASE = 4f
        const val SLOW_BASS_SECONDS = 0.6f
        const val BEAT_RISE = 0.08f
        const val BEAT_FLOOR = 0.2f
        const val MIN_BEAT_GAP = 0.18f

        /** The share of [BEAT_RISE] the rise must fall under before another beat. */
        const val REARM = 0.5f
        const val BEAT_DECAY = 5f
        const val FLOW_BOOST = 1.5f
        const val PARTICLE_RUSH = 5f
        const val PARTICLE_BEAT_RUSH = 4f
        const val MAX_STEP = 0.1f
        private const val REST = 0.001f

        /** A band group's average meter level (−60..0 dB as 0..1); 0 without data or bands. */
        internal fun FloatArray?.average(bands: IntRange): Float {
            if (this == null || isEmpty()) return 0f
            val used = bands.filter { it < size }
            if (used.isEmpty()) return 0f
            return used.sumOf { this[it].toDouble() }.toFloat() / used.size
        }
    }
}

/**
 * A band group's recent range: [ceiling] jumps to each new peak and sinks back over
 * [CEILING_SECONDS]; [floor] drops to each new low and rises over [FLOOR_SECONDS]; both start
 * at the first sound after silence. [normalize]
 * places a level within that range, so a quiet recording (the console's menu music sits around
 * 0.2–0.35 on the meter) and a loud one (0.5–0.8) both move the visualizer across 0..1. A range
 * narrower than [MIN_RANGE] isn't stretched further, and levels under [SILENCE] read as 0.
 */
internal class RangeFollower {
    var floor = 0f
        private set
    var ceiling = 0f
        private set

    private var started = false

    fun normalize(
        level: Float,
        dt: Float,
    ): Float {
        // Silence ends the range: the next sound (another track, say) starts a new one from itself.
        if (level < SILENCE) {
            started = false
            return 0f
        }
        if (!started) {
            floor = level
            ceiling = level
            started = true
        }
        ceiling = if (level >= ceiling) level else ceiling + (level - ceiling) * (1f - exp(-dt / CEILING_SECONDS))
        floor = if (level <= floor) level else floor + (level - floor) * (1f - exp(-dt / FLOOR_SECONDS))
        if (level < SILENCE) return 0f
        return ((level - floor) / max(ceiling - floor, MIN_RANGE)).coerceIn(0f, 1f)
    }

    companion object {
        const val CEILING_SECONDS = 6f
        const val FLOOR_SECONDS = 3f

        /** About 7 dB: smaller swings aren't blown up to full scale. */
        const val MIN_RANGE = 0.12f

        /** About −57 dB. */
        const val SILENCE = 0.05f
    }
}

/**
 * A [VisualizerAudio] fed from [AudioSpectrumMeter] while [active]: registers as a meter consumer
 * (so it only captures while a visualizer is on screen, and Android turns audio offload off) and
 * polls `levels()` off the main thread. The caller's frame loop calls [VisualizerAudio.step].
 */
@Composable
fun rememberVisualizerAudio(active: Boolean): VisualizerAudio {
    val audio = remember { VisualizerAudio() }
    if (active) {
        DisposableEffect(audio) {
            AudioSpectrumMeter.addConsumer()
            onDispose {
                AudioSpectrumMeter.removeConsumer()
                audio.input = null
            }
        }
        LaunchedEffect(audio) {
            while (true) {
                // levels() analyzes the newest frame (and keeps the EQ bars' smoothing as is); the
                // visualizer reads that frame unsmoothed, for sharp beats.
                audio.input =
                    withContext(Dispatchers.Default) {
                        AudioSpectrumMeter.levels()
                        AudioSpectrumMeter.frameLevels()
                    }
                delay(POLL_MS)
            }
        }
    }
    return audio
}

private const val POLL_MS = 33L
