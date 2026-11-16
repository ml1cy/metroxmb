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
    // Measured from reference/xmb/settings-red-1080p.png and game-purple-1080p.jpg (1.5 px per unit).
    val categorySpacing = unit * 133

    /** Icon box size; the PS3 PNGs leave padding around the glyph, so the box is larger than the glyph. */
    val categoryIcon = unit * 120
    val categoryLabelOffset = unit * 40
    val categoryLabelSize = unit * 19
    val itemIcon = unit * 100
    val itemSpacing = unit * 53
    val itemTextOffset = unit * 27
    val itemTitleSize = unit * 23
    val itemSubtitleSize = unit * 15

    /** Where a setting's value starts, measured from the start of its title. */
    val valueColumn = unit * 440
    val focusedItemOffset = unit * 143
    val itemsAboveGap = unit * 204
    val itemsBelowGap = unit * 64
    val clockSize = unit * 20
    val clockIcon = unit * 22
    val clockBoxTop = unit * 50
    val clockBoxHeight = unit * 37
    val clockBoxWidth = unit * 393
    val clockEndPadding = unit * 77
    val clockBorder = unit * 1.33f
    val edgeMargin = unit * 32

    /** How far left of the list an opened level's parent icon sits. */
    val parentOffset = unit * 150

    companion object {
        /** Horizontal position of the focused category, as a fraction of the window width. */
        const val ANCHOR_X = 0.295f

        /** Vertical position of the category row, as a fraction of the window height. */
        const val BAR_Y = 0.27f

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
    const val UNFOCUSED_CATEGORY_SCALE = 0.66f
    const val UNFOCUSED_CATEGORY_ALPHA = 0.45f
    const val UNFOCUSED_ITEM_SCALE = 0.6f
    const val UNFOCUSED_ITEM_ALPHA = 0.62f

    /** Album art and photos fill this share of an item's icon box, about the size of a PS3 glyph. */
    const val ARTWORK_FILL = 0.7f

    /** Decelerating ease for list and row scrolling. */
    val scrollEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
}
