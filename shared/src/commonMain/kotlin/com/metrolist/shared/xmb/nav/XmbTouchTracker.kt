package com.metrolist.shared.xmb.nav

import kotlin.math.abs

/** What a touch that didn't drag was. */
enum class XmbTouchGesture { TAP, LONG_PRESS }

/**
 * Touch for the XMB, as steps: dragging moves one item (vertically) or one category
 * (horizontally) per [stepY] or [stepX] of travel, like the arrow keys; the axis is chosen once
 * the finger has moved [slop]. Dragging up moves to the next item and dragging left to the next
 * category, as the content follows the finger. A quick flick carries on for a few more steps
 * ([flingSteps]). Taps on the XMB's parts are theirs (`xmbTap`); this sees only what they leave:
 * a touch that never made a step is a tap, or a long press when held [longPressMs].
 * Coordinates are window pixels.
 */
class XmbTouchTracker(
    private val stepX: Float,
    private val stepY: Float,
    private val slop: Float,
    private val longPressMs: Long = LONG_PRESS_MS,
) {
    private var startX = 0f
    private var startY = 0f
    private var downAt = 0L
    private var axis: Axis? = null
    private var consumed = 0
    private var active = false

    private enum class Axis { X, Y }

    fun down(
        x: Float,
        y: Float,
        timeMs: Long,
    ) {
        startX = x
        startY = y
        downAt = timeMs
        axis = null
        consumed = 0
        active = true
    }

    /** The steps a move to ([x], [y]) adds, in order. */
    fun move(
        x: Float,
        y: Float,
    ): List<XmbAction> {
        if (!active) return emptyList()
        val dx = x - startX
        val dy = y - startY
        if (axis == null) {
            if (abs(dx) < slop && abs(dy) < slop) return emptyList()
            axis = if (abs(dx) > abs(dy)) Axis.X else Axis.Y
        }
        // The first step half a step past the slop, so a short drag still moves; then one per spacing.
        val travel = if (axis == Axis.X) -dx else -dy
        val step = if (axis == Axis.X) stepX else stepY
        val steps = ((abs(travel) - slop) / step + 0.5f).toInt().coerceAtLeast(0) * if (travel < 0) -1 else 1
        val actions = mutableListOf<XmbAction>()
        while (consumed != steps) {
            val forward = steps > consumed
            consumed += if (forward) 1 else -1
            actions += action(forward)
        }
        return actions
    }

    /**
     * Lifting the finger: a tap or long press when it made no step (a wobble past the slop
     * still counts as a tap), null after a drag.
     */
    fun up(timeMs: Long): XmbTouchGesture? {
        if (!active) return null
        active = false
        if (consumed != 0) return null
        return if (timeMs - downAt >= longPressMs) XmbTouchGesture.LONG_PRESS else XmbTouchGesture.TAP
    }

    /**
     * Extra steps for a flick that ends with [velocity] (pixels a second along the drag's axis,
     * signed like the finger's motion): the distance it would coast in [FLING_SECONDS], in steps,
     * at most [MAX_FLING]. None after a slow release or a tap.
     */
    fun flingSteps(velocity: Float): List<XmbAction> {
        val dragAxis = axis ?: return emptyList()
        if (consumed == 0) return emptyList()
        val step = if (dragAxis == Axis.X) stepX else stepY
        val count = (abs(velocity) * FLING_SECONDS / step).toInt().coerceAtMost(MAX_FLING)
        if (count < 1 || abs(velocity) < step * MIN_FLING_STEPS_PER_SECOND) return emptyList()
        // Finger moving up or left (negative) is a forward flick, as with the drag.
        return List(count) { action(forward = velocity < 0f) }
    }

    /** The drag's axis for velocity: true when vertical. */
    val vertical: Boolean get() = axis == Axis.Y

    fun cancel() {
        active = false
    }

    private fun action(forward: Boolean): XmbAction =
        when (axis) {
            Axis.X -> if (forward) XmbAction.RIGHT else XmbAction.LEFT
            else -> if (forward) XmbAction.DOWN else XmbAction.UP
        }

    private companion object {
        const val LONG_PRESS_MS = 500L

        /** How far a flick coasts, and how fast it must be to coast at all (steps a second). */
        const val FLING_SECONDS = 0.18f
        const val MIN_FLING_STEPS_PER_SECOND = 8f
        const val MAX_FLING = 12
    }
}
