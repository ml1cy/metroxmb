package com.metrolist.shared.xmb.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Colors the background is drawn from: a vertical gradient from [top] to [bottom], and the month's
 * [base] color at the time of day's brightness (the visualizer's gradient starts from it).
 */
data class XmbBackgroundColors(
    val top: Color,
    val bottom: Color,
    val base: Color,
)

/**
 * The XMB's monthly theme colors and the time-of-day brightness curve. Values and reasons are
 * logged in docs/xmb-notes.md.
 */
object XmbPalette {
    /** January to December. */
    val monthlyColors: List<Color> =
        listOf(
            Color(0xFFCBCBCB), // January: silver
            Color(0xFFD8BF1A), // February: gold
            Color(0xFF6DB217), // March: lime
            Color(0xFFE17E9A), // April: pink
            Color(0xFF088B01), // May: green
            Color(0xFF9A61C8), // June: purple
            Color(0xFF02CDC7), // July: cyan
            Color(0xFF0C76C0), // August: blue
            Color(0xFFB444C0), // September: magenta
            Color(0xFFE5A708), // October: amber
            Color(0xFF875B1E), // November: brown
            Color(0xFFE3412A), // December: red
        )

    const val TOP_FACTOR = 0.375f
    const val BOTTOM_FACTOR = 1.625f
    const val NIGHT_BRIGHTNESS = 0.42f
    const val DAY_BRIGHTNESS = 1f

    // Hours (local time) where the background reaches full brightness and where it is fully dimmed.
    private const val DAWN_START = 5f
    private const val DAY_START = 8f
    private const val DUSK_START = 17f
    private const val NIGHT_START = 22f

    /** [month] is 1 (January) to 12 (December); out-of-range values wrap. */
    fun monthColor(month: Int): Color = monthlyColors[(month - 1).mod(12)]

    /**
     * Brightness multiplier for a local time of day, [hour] in 0 until 24 (fractional). Full
     * brightness from 08:00 to 17:00, dimmed from 22:00 to 05:00, with smooth ramps between.
     */
    fun brightness(hour: Float): Float {
        val h = ((hour % 24f) + 24f) % 24f
        return when {
            h < DAWN_START || h >= NIGHT_START -> NIGHT_BRIGHTNESS
            h < DAY_START -> mix(NIGHT_BRIGHTNESS, DAY_BRIGHTNESS, smoothstep((h - DAWN_START) / (DAY_START - DAWN_START)))
            h < DUSK_START -> DAY_BRIGHTNESS
            else -> mix(DAY_BRIGHTNESS, NIGHT_BRIGHTNESS, smoothstep((h - DUSK_START) / (NIGHT_START - DUSK_START)))
        }
    }

    fun backgroundColors(
        month: Int,
        hour: Float,
    ): XmbBackgroundColors = colorsFor(monthColor(month), brightness(hour))

    /** The background for [base] at brightness [light] (1 by day). */
    fun colorsFor(
        base: Color,
        light: Float,
    ): XmbBackgroundColors =
        // RetroArch's ribbon background: the color at 0.375 at the top rising to 1.625 at the bottom.
        XmbBackgroundColors(
            top = base.scaled(light * TOP_FACTOR),
            bottom = base.scaled(light * BOTTOM_FACTOR),
            base = base.scaled(light),
        )

    private fun Color.scaled(factor: Float): Color =
        Color(
            red = (red * factor).coerceIn(0f, 1f),
            green = (green * factor).coerceIn(0f, 1f),
            blue = (blue * factor).coerceIn(0f, 1f),
            alpha = alpha,
        )

    private fun smoothstep(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    private fun mix(
        from: Float,
        to: Float,
        t: Float,
    ): Float = from + (to - from) * t
}
