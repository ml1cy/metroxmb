package com.metrolist.shared.visualizer.background

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/** Navigation the home-screen wave reacts to. */
enum class XmbWaveNudge {
    LEFT,
    RIGHT,
    UP,
    DOWN,
    SELECT,
    BACK,
    ERROR,
}

/**
 * How the background wave and its sparkles move: a steady flow plus short reactions to
 * navigation. Moving across categories pushes the wave and sparkles sideways, moving through
 * items lifts or lowers them with a little bounce, ✕ sends a bright pulse along the strands and
 * swells them, ○ does a softer version, and the error sound makes the wave shiver. Everything
 * settles back within a couple of seconds. The wave doesn't follow the music; that's the
 * visualizer's job. Plain fields, read while drawing, so frames don't recompose.
 */
class XmbWaveMotion {
    /** Wave phase, in seconds of resting flow. */
    var flow = 0f
        private set

    /** How far the sparkles have travelled to the left, in screen widths. */
    var drift = 0f
        private set

    /** Sideways push: positive after moving right. */
    var push = 0f
        private set

    /** Vertical offset: negative lifts the wave. */
    var lift = 0f
        private set
    private var liftVelocity = 0f

    /** Extra strand spread and glow, 0..1. */
    var swell = 0f
        private set

    /** Brightness pulse, 0..1. */
    var flash = 0f
        private set

    /** Seconds since the last pulse started travelling along the strands. */
    var pulseAge = RESTING_PULSE_AGE
        private set

    /** Error shiver, 0..1. */
    var shiver = 0f
        private set

    val isSettled: Boolean
        get() = push == 0f && lift == 0f && liftVelocity == 0f && swell == 0f && flash == 0f && shiver == 0f

    fun nudge(nudge: XmbWaveNudge) {
        when (nudge) {
            XmbWaveNudge.LEFT -> {
                push = (push - SIDE_KICK).coerceIn(-MAX_PUSH, MAX_PUSH)
            }

            XmbWaveNudge.RIGHT -> {
                push = (push + SIDE_KICK).coerceIn(-MAX_PUSH, MAX_PUSH)
            }

            // Focus moving down scrolls the list up, so the wave rises with it, and the reverse.
            XmbWaveNudge.DOWN -> {
                liftVelocity = (liftVelocity - LIFT_KICK).coerceIn(-MAX_LIFT_VELOCITY, MAX_LIFT_VELOCITY)
            }

            XmbWaveNudge.UP -> {
                liftVelocity = (liftVelocity + LIFT_KICK).coerceIn(-MAX_LIFT_VELOCITY, MAX_LIFT_VELOCITY)
            }

            XmbWaveNudge.SELECT -> {
                flash = 1f
                swell = (swell + SELECT_SWELL).coerceAtMost(1f)
                pulseAge = 0f
            }

            XmbWaveNudge.BACK -> {
                flash = max(flash, BACK_FLASH)
                swell = (swell + BACK_SWELL).coerceAtMost(1f)
                pulseAge = 0f
            }

            XmbWaveNudge.ERROR -> {
                shiver = 1f
            }
        }
    }

    /** Advances by [seconds] of frame time; long gaps (a hidden window) are capped. */
    fun advance(seconds: Float) {
        val dt = seconds.coerceIn(0f, MAX_STEP)
        // Moving right slides the scene left, so a positive push slows or reverses the flow.
        flow += dt * (1f - push * PUSH_FLOW)
        drift += dt * (BASE_DRIFT + push * PUSH_DRIFT)
        push = settle(push * exp(-dt * PUSH_DECAY))
        val acceleration = -LIFT_STIFFNESS * lift - LIFT_DAMPING * liftVelocity
        liftVelocity += acceleration * dt
        lift += liftVelocity * dt
        if (abs(lift) < REST && abs(liftVelocity) < REST) {
            lift = 0f
            liftVelocity = 0f
        }
        swell = settle(swell * exp(-dt * SWELL_DECAY))
        flash = settle(flash * exp(-dt * FLASH_DECAY))
        shiver = settle(shiver * exp(-dt * SHIVER_DECAY))
        pulseAge = (pulseAge + dt).coerceAtMost(RESTING_PULSE_AGE)
    }

    private fun settle(value: Float) = if (abs(value) < REST) 0f else value

    companion object {
        const val SIDE_KICK = 1f
        const val MAX_PUSH = 3f
        const val PUSH_FLOW = 1.6f
        const val PUSH_DECAY = 2.4f
        const val BASE_DRIFT = 0.006f
        const val PUSH_DRIFT = 0.07f
        const val LIFT_KICK = 1.4f
        const val MAX_LIFT_VELOCITY = 4f
        const val LIFT_STIFFNESS = 32f
        const val LIFT_DAMPING = 7f
        const val SELECT_SWELL = 0.6f
        const val BACK_SWELL = 0.25f
        const val BACK_FLASH = 0.4f
        const val SWELL_DECAY = 1.6f
        const val FLASH_DECAY = 2.6f
        const val SHIVER_DECAY = 5f
        const val MAX_STEP = 0.1f
        const val RESTING_PULSE_AGE = 10f
        private const val REST = 0.0005f
    }
}
