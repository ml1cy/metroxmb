package com.metrolist.shared.xmb.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.xmb_rodin_latin_regular
import org.jetbrains.compose.resources.Font

/**
 * The Rodin font. It only covers Latin; other scripts fall back to the platform fonts through
 * normal text shaping.
 */
@Composable
fun rememberXmbFontFamily(): FontFamily {
    val rodin = Font(Res.font.xmb_rodin_latin_regular)
    return remember(rodin) { FontFamily(rodin) }
}

/**
 * Layout constants, in units of [unit]: 1/720 of the window height, capped so narrow windows
 * scale by width instead. Values and reasons are logged in docs/xmb-notes.md.
 */
class XmbMetrics(
    val unit: Dp,
) {
    val categorySpacing = unit * 136
    val categoryIcon = unit * 72
    val categoryLabelOffset = unit * 54
    val categoryLabelSize = unit * 18
    val itemIcon = unit * 56
    val itemSpacing = unit * 66
    val itemTextOffset = unit * 20
    val itemTitleSize = unit * 19
    val itemSubtitleSize = unit * 14
    val focusedItemOffset = unit * 112
    val itemsAboveGap = unit * 110
    val clockSize = unit * 16
    val edgeMargin = unit * 32

    /** How far left of the list an opened level's parent icon sits. */
    val parentOffset = unit * 130

    companion object {
        /** Horizontal position of the focused category, as a fraction of the window width. */
        const val ANCHOR_X = 0.22f

        /** Vertical position of the category row, as a fraction of the window height. */
        const val BAR_Y = 0.30f

        /** Vertical position of an opened level's focused row, as a fraction of the window height. */
        const val LEVEL_FOCUS_Y = 0.42f

        fun forSize(
            width: Dp,
            height: Dp,
        ): XmbMetrics = XmbMetrics(min(height / 720f, width / 960f))
    }
}

/** Motion timings and easing. */
object XmbMotion {
    const val CATEGORY_MS = 240
    const val ITEM_MS = 180
    const val FADE_MS = 200

    /** Unfocused category icons shrink to this scale and dim to [UNFOCUSED_CATEGORY_ALPHA]. */
    const val UNFOCUSED_CATEGORY_SCALE = 0.78f
    const val UNFOCUSED_CATEGORY_ALPHA = 0.55f
    const val UNFOCUSED_ITEM_SCALE = 0.72f
    const val UNFOCUSED_ITEM_ALPHA = 0.62f

    /** Focus glow pulse period and strength range. */
    const val GLOW_PULSE_MS = 2400
    const val GLOW_MIN = 0.55f
    const val GLOW_MAX = 0.9f

    /** Decelerating ease for list and row scrolling. */
    val scrollEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
}
