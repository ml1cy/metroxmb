package com.metrolist.shared.xmb.nav

import com.metrolist.shared.xmb.adapters.XmbList

/** Input actions, named after the console's buttons where they differ from directions. */
enum class XmbAction {
    UP,
    DOWN,
    LEFT,
    RIGHT,

    /** ✕ */
    SELECT,

    /** ○ */
    BACK,

    /** △ */
    OPTIONS,

    /** Media keys and Space; handled by the app, not the menu. */
    PLAY_PAUSE,
    NEXT_TRACK,
    PREVIOUS_TRACK,
}

/** What the user should hear after an action. */
enum class XmbFeedback {
    NONE,
    MOVE_UP,
    MOVE_DOWN,
    MOVE_SIDE,
    SELECT,
    BACK,
    ERROR,
}

/**
 * An opened drill-down level: the list it shows, the focused row, and how its parent row
 * looked (shown to the left of the list, like the console's folder navigation).
 */
data class XmbLevel(
    val list: XmbList,
    val title: String,
    val icon: XmbIcon,
    val artwork: String? = null,
    val focus: Int = 0,
)

/**
 * Focus position: the focused category, per category its focused item, and the stack of opened
 * levels. Each category keeps its own item position, like the console, so moving away and back
 * returns to the same item.
 */
data class XmbNavigation(
    val category: Int,
    val items: List<Int>,
    val stack: List<XmbLevel> = emptyList(),
) {
    val item: Int get() = items.getOrElse(category) { 0 }

    val level: XmbLevel? get() = stack.lastOrNull()

    fun push(level: XmbLevel): XmbNavigation = copy(stack = stack + level)

    fun pop(): XmbNavigation = copy(stack = stack.dropLast(1))

    /** Keeps the open level's focus inside a list that changed size (rows loaded or removed). */
    fun clampLevel(size: Int): XmbNavigation {
        val top = level ?: return this
        val focus = top.focus.coerceIn(0, (size - 1).coerceAtLeast(0))
        return if (focus == top.focus) this else copy(stack = stack.dropLast(1) + top.copy(focus = focus))
    }

    companion object {
        fun initial(
            itemCounts: List<Int>,
            category: Int,
        ): XmbNavigation {
            require(itemCounts.isNotEmpty()) { "The XMB needs at least one category" }
            return XmbNavigation(category.coerceIn(itemCounts.indices), List(itemCounts.size) { 0 })
        }
    }
}

/**
 * Result of one action: the new position, its sound, the item or row chosen with
 * [XmbAction.SELECT], and whether [XmbAction.OPTIONS] asked for the focused row's options.
 */
data class XmbStep(
    val navigation: XmbNavigation,
    val feedback: XmbFeedback,
    val selected: XmbSelection? = null,
    val optionsRequested: Boolean = false,
)

/** A top-level item ([depth] 0) or a row of the open level ([depth] = stack size, [item] = row). */
data class XmbSelection(
    val category: Int,
    val item: Int,
    val depth: Int = 0,
)

/**
 * Applies [action]. At the top level, categories have [itemCounts] items each; inside a level,
 * the open list has [levelSize] rows. Moving past either end stops without wrapping or sound, as
 * on the console. Back and Left close a level.
 */
fun XmbNavigation.reduce(
    action: XmbAction,
    itemCounts: List<Int>,
    levelSize: Int = 0,
): XmbStep {
    if (stack.isNotEmpty()) return reduceLevel(action, levelSize)
    val count = itemCounts.getOrElse(category) { 0 }
    return when (action) {
        XmbAction.LEFT -> {
            if (category > 0) XmbStep(copy(category = category - 1), XmbFeedback.MOVE_SIDE) else XmbStep(this, XmbFeedback.NONE)
        }

        XmbAction.RIGHT -> {
            if (category < itemCounts.lastIndex) {
                XmbStep(copy(category = category + 1), XmbFeedback.MOVE_SIDE)
            } else {
                XmbStep(this, XmbFeedback.NONE)
            }
        }

        XmbAction.UP -> {
            if (item > 0) XmbStep(withItem(item - 1), XmbFeedback.MOVE_UP) else XmbStep(this, XmbFeedback.NONE)
        }

        XmbAction.DOWN -> {
            if (item < count - 1) XmbStep(withItem(item + 1), XmbFeedback.MOVE_DOWN) else XmbStep(this, XmbFeedback.NONE)
        }

        XmbAction.SELECT -> {
            if (count > 0) XmbStep(this, XmbFeedback.SELECT, XmbSelection(category, item)) else XmbStep(this, XmbFeedback.ERROR)
        }

        // Top-level items have no options; Back has nowhere to go.
        XmbAction.BACK, XmbAction.OPTIONS,
        XmbAction.PLAY_PAUSE, XmbAction.NEXT_TRACK, XmbAction.PREVIOUS_TRACK,
        -> {
            XmbStep(this, XmbFeedback.NONE)
        }
    }
}

private fun XmbNavigation.reduceLevel(
    action: XmbAction,
    size: Int,
): XmbStep {
    val top = checkNotNull(level)
    return when (action) {
        XmbAction.UP -> {
            if (top.focus > 0) XmbStep(withFocus(top.focus - 1), XmbFeedback.MOVE_UP) else XmbStep(this, XmbFeedback.NONE)
        }

        XmbAction.DOWN -> {
            if (top.focus < size - 1) XmbStep(withFocus(top.focus + 1), XmbFeedback.MOVE_DOWN) else XmbStep(this, XmbFeedback.NONE)
        }

        XmbAction.LEFT, XmbAction.BACK -> {
            XmbStep(pop(), XmbFeedback.BACK)
        }

        XmbAction.SELECT -> {
            if (size > 0) {
                XmbStep(this, XmbFeedback.SELECT, XmbSelection(category, top.focus, depth = stack.size))
            } else {
                XmbStep(this, XmbFeedback.ERROR)
            }
        }

        XmbAction.OPTIONS -> {
            if (size > 0) XmbStep(this, XmbFeedback.NONE, optionsRequested = true) else XmbStep(this, XmbFeedback.ERROR)
        }

        XmbAction.RIGHT, XmbAction.PLAY_PAUSE, XmbAction.NEXT_TRACK, XmbAction.PREVIOUS_TRACK -> {
            XmbStep(this, XmbFeedback.NONE)
        }
    }
}

private fun XmbNavigation.withItem(index: Int) = copy(items = items.toMutableList().also { it[category] = index })

private fun XmbNavigation.withFocus(index: Int) = copy(stack = stack.dropLast(1) + checkNotNull(level).copy(focus = index))
