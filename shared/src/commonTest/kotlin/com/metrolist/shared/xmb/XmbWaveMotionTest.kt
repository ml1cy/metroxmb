package com.metrolist.shared.xmb

import com.metrolist.shared.visualizer.background.XmbWaveMotion
import com.metrolist.shared.visualizer.background.XmbWaveNudge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XmbWaveMotionTest {
    private fun XmbWaveMotion.run(
        seconds: Float,
        frame: Float = 1f / 60f,
    ) {
        var left = seconds
        while (left > 0f) {
            advance(minOf(frame, left))
            left -= frame
        }
    }

    @Test
    fun restsAndFlowsSteadilyWithoutInput() {
        val motion = XmbWaveMotion()
        assertTrue(motion.isSettled)
        motion.run(2f)
        assertEquals(2f, motion.flow, 0.001f)
        assertEquals(2f * XmbWaveMotion.BASE_DRIFT, motion.drift, 0.0001f)
        assertTrue(motion.isSettled)
    }

    @Test
    fun movingRightPushesTheSceneLeftThenSettles() {
        val resting = XmbWaveMotion().apply { run(0.3f) }
        val pushed = XmbWaveMotion().apply { nudge(XmbWaveNudge.RIGHT) }
        pushed.run(0.3f)
        assertTrue(pushed.flow < resting.flow, "The flow slows or reverses")
        assertTrue(pushed.drift > resting.drift, "Sparkles travel further left")
        pushed.run(5f)
        assertTrue(pushed.isSettled)
        val left = XmbWaveMotion().apply { nudge(XmbWaveNudge.LEFT) }
        left.run(0.3f)
        assertTrue(left.flow > resting.flow)
        assertTrue(left.drift < resting.drift)
    }

    @Test
    fun repeatedMovesBuildUpToALimit() {
        val motion = XmbWaveMotion()
        repeat(10) { motion.nudge(XmbWaveNudge.RIGHT) }
        assertEquals(XmbWaveMotion.MAX_PUSH, motion.push)
    }

    @Test
    fun movingDownLiftsTheWaveWithABounceThatSettles() {
        val motion = XmbWaveMotion()
        motion.nudge(XmbWaveNudge.DOWN)
        motion.run(0.1f)
        assertTrue(motion.lift < 0f, "Negative lift raises the wave")
        var crossed = false
        repeat(60) {
            motion.run(1f / 60f)
            if (motion.lift > 0f) crossed = true
        }
        assertTrue(crossed, "It overshoots a little, like a spring")
        motion.run(5f)
        assertTrue(motion.isSettled)
    }

    @Test
    fun selectPulsesAndSwellsAndBackIsSofter() {
        val select = XmbWaveMotion().apply { nudge(XmbWaveNudge.SELECT) }
        assertEquals(1f, select.flash)
        assertEquals(0f, select.pulseAge)
        val back = XmbWaveMotion().apply { nudge(XmbWaveNudge.BACK) }
        assertTrue(back.flash < select.flash)
        assertTrue(back.swell < select.swell)
        select.run(0.5f)
        assertTrue(select.flash in 0f..0.5f)
        assertEquals(0.5f, select.pulseAge, 0.01f)
        select.run(5f)
        assertTrue(select.isSettled)
    }

    @Test
    fun errorsShiverBrieflyAndLongFramesAreCapped() {
        val motion = XmbWaveMotion().apply { nudge(XmbWaveNudge.ERROR) }
        assertEquals(1f, motion.shiver)
        motion.advance(30f)
        assertEquals(XmbWaveMotion.MAX_STEP, motion.flow, 0.0001f)
        motion.run(3f)
        assertTrue(motion.isSettled)
    }
}
