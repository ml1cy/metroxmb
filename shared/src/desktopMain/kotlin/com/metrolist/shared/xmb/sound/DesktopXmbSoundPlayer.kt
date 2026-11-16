package com.metrolist.shared.xmb.sound

import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.playback.ffmpeg.AudioDecoder
import com.metrolist.shared.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.log10

/** Interleaved signed 16-bit little-endian PCM. */
internal class XmbPcm(
    val data: ByteArray,
    val sampleRate: Int,
    val channels: Int,
) {
    val durationSeconds: Double get() = data.size / 2.0 / channels / sampleRate
}

/** Decodes a whole Ogg (or any FFmpeg-readable) sound into PCM with the bundled FFmpeg. */
internal fun decodeXmbSound(bytes: ByteArray): XmbPcm {
    val decoder = AudioDecoder(ByteArrayInputStream(bytes), maximumSize = bytes.size)
    try {
        val pcm = ByteArrayOutputStream()
        while (true) pcm.write(decoder.readPCMFrame() ?: break)
        check(decoder.sampleRate > 0 && decoder.audioChannels > 0) { "Sound has no audio stream" }
        return XmbPcm(pcm.toByteArray(), decoder.sampleRate, decoder.audioChannels)
    } finally {
        decoder.close()
    }
}

/**
 * Desktop sound effects: each effect is decoded once in the background and kept as a `Clip`.
 * Playing an effect again restarts it, like the console's rapid navigation ticks. Effects that
 * haven't loaded yet, or that fail to load, are skipped silently.
 */
class DesktopXmbSoundPlayer :
    XmbSoundPlayer,
    AutoCloseable {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val audio = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + audio)
    private val clips = HashMap<String, Clip>()

    init {
        scope.launch {
            val failed = mutableListOf<String>()
            var firstError: Throwable? = null
            for (file in XmbSound.effects.map { it.file }.distinct()) {
                runCatching { clips[file] = openClip(decodeXmbSound(Res.readBytes("files/xmb/sounds/$file"))) }
                    .onFailure {
                        failed += file
                        if (firstError == null) firstError = it
                    }
            }
            // A machine without an audio device fails every clip the same way; say so once.
            if (failed.isNotEmpty()) Logger.w(TAG, "XMB sounds unavailable (${failed.joinToString()}): $firstError")
        }
    }

    override fun play(sound: XmbSound) {
        scope.launch {
            val clip = clips[sound.file] ?: return@launch
            clip.stop()
            clip.framePosition = 0
            clip.start()
        }
    }

    override fun close() {
        scope
            .launch {
                clips.values.forEach { runCatching { it.close() } }
                clips.clear()
            }.invokeOnCompletion { scope.cancel() }
    }

    private fun openClip(pcm: XmbPcm): Clip {
        val format = AudioFormat(pcm.sampleRate.toFloat(), 16, pcm.channels, true, false)
        return AudioSystem.getClip().apply {
            open(format, pcm.data, 0, pcm.data.size)
            if (isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gain = getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                gain.value = (20f * log10(EFFECT_VOLUME)).coerceIn(gain.minimum, gain.maximum)
            }
        }
    }

    private companion object {
        const val TAG = "XmbSounds"

        /** Effects play a little under full scale so they sit below the music. */
        const val EFFECT_VOLUME = 0.7f
    }
}
