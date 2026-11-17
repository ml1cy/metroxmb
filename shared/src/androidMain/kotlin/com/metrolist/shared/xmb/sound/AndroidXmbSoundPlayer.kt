package com.metrolist.shared.xmb.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.Volatile

/**
 * Android sound effects through a SoundPool: each effect is copied out of the app's resources
 * once (SoundPool loads files) and loaded in the background. Effects that haven't loaded yet, or
 * that fail to load, are skipped silently.
 */
class AndroidXmbSoundPlayer(
    context: Context,
) : XmbSoundPlayer,
    AutoCloseable {
    private val pool =
        SoundPool
            .Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).build()
    private val ids = ConcurrentHashMap<String, Int>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var volume = 1f

    init {
        val directory = File(context.cacheDir, "xmb-sounds")
        scope.launch {
            directory.mkdirs()
            for (file in XmbSound.effects.map { it.file }.distinct()) {
                runCatching {
                    val target = File(directory, file)
                    if (!target.exists()) target.writeBytes(Res.readBytes("files/xmb/sounds/$file"))
                    ids[file] = pool.load(target.absolutePath, 1)
                }.onFailure { Logger.w(TAG, "XMB sound $file unavailable: $it") }
            }
        }
    }

    override fun play(sound: XmbSound) {
        val id = ids[sound.file] ?: return
        val level = EFFECT_VOLUME * volume
        if (level > 0f) pool.play(id, level, level, 1, 0, 1f)
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }

    override fun close() {
        scope.cancel()
        pool.release()
    }

    private companion object {
        const val TAG = "XmbSounds"
        const val MAX_STREAMS = 4

        /** Effects play a little under full scale so they sit below the music, as on desktop. */
        const val EFFECT_VOLUME = 0.7f
    }
}
