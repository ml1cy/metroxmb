package com.metrolist.shared.xmb.sound

import com.metrolist.shared.generated.resources.Res
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XmbSoundDecodeTest {
    /** Lengths from docs/xmb-assets.md (ffprobe), in seconds. */
    private val expected =
        mapOf(
            "nav_up.ogg" to 0.049,
            "nav_down.ogg" to 0.049,
            "select.ogg" to 0.313,
            "back.ogg" to 0.049,
            "notice.ogg" to 0.817,
            "notice_close.ogg" to 0.313,
            "unlock.ogg" to 1.009,
        )

    @Test
    fun everyEffectLoadsFromComposeResourcesAndDecodesToAudiblePcm() {
        assertEquals(expected.keys, XmbSound.effects.map { it.file }.toSet())
        for ((file, seconds) in expected) {
            val pcm = decodeXmbSound(runBlocking { Res.readBytes("files/xmb/sounds/$file") })
            assertEquals(48_000, pcm.sampleRate, file)
            assertTrue(pcm.channels in 1..2, "$file has ${pcm.channels} channels")
            assertTrue(abs(pcm.durationSeconds - seconds) < 0.03, "$file decoded to ${pcm.durationSeconds} s, expected $seconds s")
            val samples = ByteBuffer.wrap(pcm.data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val peak = (0 until samples.limit()).maxOf { abs(samples.get(it).toInt()) }
            assertTrue(peak > 500, "$file is silent (peak $peak)")
        }
    }
}
