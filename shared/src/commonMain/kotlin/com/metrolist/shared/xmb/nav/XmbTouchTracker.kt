package com.metrolist.shared.xmb.nav

import kotlin.math.abs

/**
 * Touch for the XMB, as steps: dragging moves one item (vertically) or one category
 * (horizontally) per [stepY] or [stepX] of travel, like the arrow keys; the axis is chosen once
 * the finger has moved [slop]. Dragging up moves to the next item and dragging left to the next
 * category, as the content follows the finger. A tap selects the focused row, or steps toward
 * the tapped spot when that's elsewhere (a tap on the category row moves along the row); a press
 * held [longPressMs] without moving opens the options (△). Coordinates are window pixels.
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
        // Steps so far: the first after half a step past the slop, so a short drag still moves.
        val travel = if (axis == Axis.X) -dx else -dy
        val step = if (axis == Axis.X) stepX else stepY
        val steps = ((abs(travel) - slop) / step + 0.5f).toInt().coerceAtLeast(0) * if (travel < 0) -1 else 1
        val actions = mutableListOf<XmbAction>()
        while (consumed != steps) {
            val forward = steps > consumed
            consumed += if (forward) 1 else -1
            actions +=
                when (axis) {
                    Axis.X -> if (forward) XmbAction.RIGHT else XmbAction.LEFT
                    else -> if (forward) XmbAction.DOWN else XmbAction.UP
                }
        }
        return actions
    }

    /**
     * What lifting the finger at ([x], [y]) does, when it never dragged: a long press, or a tap.
     * [focusX], [focusY] is the focused item's center and [rowY] the category row's.
     */
    fun up(
        x: Float,
        y: Float,
        timeMs: Long,
        focusX: Float,
        focusY: Float,
        rowY: Float,
    ): XmbAction? {
        if (!active) return null
        active = false
        if (axis != null) return null
        if (timeMs - downAt >= longPressMs) return XmbAction.OPTIONS
        val dx = x - focusX
        return when {
            abs(y - rowY) <= stepX / 2f -> {
                if (abs(dx) <= stepX / 2f) {
                    null
                } else if (dx > 0) {
                    XmbAction.RIGHT
                } else {
                    XmbAction.LEFT
                }
            }

            abs(y - focusY) <= stepY * TAP_ROW -> {
                XmbAction.SELECT
            }

            y > focusY -> {
                XmbAction.DOWN
            }

            else -> {
                XmbAction.UP
            }
        }
    }

    fun cancel() {
        active = false
    }

    private companion object {
        const val LONG_PRESS_MS = 500L

        /** A tap within this many item steps of the focused row selects it. */
        const val TAP_ROW = 0.75f
    }
}
