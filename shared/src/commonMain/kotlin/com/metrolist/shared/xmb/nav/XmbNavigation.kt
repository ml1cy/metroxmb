package com.metrolist.shared.xmb.nav

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
 * Focus position: the focused category and, per category, its focused item. Each category keeps
 * its own item position, like the console, so moving away and back returns to the same item.
 */
data class XmbNavigation(
    val category: Int,
    val items: List<Int>,
) {
    val item: Int get() = items.getOrElse(category) { 0 }

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

/** Result of one action: the new position, its sound, and the item chosen with [XmbAction.SELECT]. */
data class XmbStep(
    val navigation: XmbNavigation,
    val feedback: XmbFeedback,
    val selected: XmbSelection? = null,
)

data class XmbSelection(
    val category: Int,
    val item: Int,
)

/**
 * Applies [action] for categories with [itemCounts] items each. Moving past either end stops
 * without wrapping or sound, as on the console.
 */
fun XmbNavigation.reduce(
    action: XmbAction,
    itemCounts: List<Int>,
): XmbStep {
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

        // Drill-down levels and the options panel arrive in M2; at the top level both do nothing.
        XmbAction.BACK, XmbAction.OPTIONS -> {
            XmbStep(this, XmbFeedback.NONE)
        }
    }
}

private fun XmbNavigation.withItem(index: Int) = copy(items = items.toMutableList().also { it[category] = index })
