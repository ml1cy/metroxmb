package com.metrolist.shared.xmb.settings

import androidx.compose.ui.graphics.Color
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.visualizer.VisualizerQuality
import com.metrolist.shared.xmb.theme.XmbBackgroundColors
import com.metrolist.shared.xmb.theme.XmbPalette

/**
 * The XMB's own settings as the menu uses them. Read from [Settings.shared], whose reads are
 * snapshot state, so a change anywhere recomposes the XMB. [color] is [ORIGINAL_COLOR], a month
 * "1".."12" (that month's color, kept all year) or [CUSTOM_COLOR] ([customHue]).
 */
data class XmbPrefs(
    val color: String = ORIGINAL_COLOR,
    val customHue: Float = 210f,
    val dimAtNight: Boolean = true,
    val background: String = GRADIENT_BACKGROUND,
    val wallpaper: String = "",
    val wallpaperDim: Float = 0.3f,
    val wave: Boolean = true,
    val sparkles: Boolean = true,
    val waveReacts: Boolean = true,
    val waveSpeed: Float = 1f,
    val clock: Boolean = true,
    val hour24: Boolean = true,
    val dateFormat: XmbDateFormat = XmbDateFormat.MONTH_DAY,
    val menuSounds: Boolean = true,
    val menuSoundVolume: Float = 1f,
    val shortcuts: XmbShortcuts = XmbShortcuts.Default,
    val nowPlayingCard: Boolean = true,
    val trackOverlay: XmbTrackOverlay = XmbTrackOverlay.BRIEF,
    val idleVisualizerSeconds: Int = 0,
    val visualizerQuality: VisualizerQuality = VisualizerQuality.HIGH,
    /** Animation frames a second: 60, or 30 to save power. */
    val frameRate: Int = 60,
) {
    /** The background image to show, or null for the color gradient. */
    val wallpaperPath: String? get() = wallpaper.takeIf { background == IMAGE_BACKGROUND && it.isNotBlank() }

    /** The background's colors for [month] (1..12) at [hour] of the day. */
    fun backgroundColors(
        month: Int,
        hour: Float,
    ): XmbBackgroundColors {
        val base =
            when (color) {
                ORIGINAL_COLOR -> XmbPalette.monthColor(month)
                CUSTOM_COLOR -> Color.hsv(customHue.mod(360f), CUSTOM_SATURATION, CUSTOM_VALUE)
                else -> color.toIntOrNull()?.takeIf { it in 1..12 }?.let(XmbPalette::monthColor) ?: XmbPalette.monthColor(month)
            }
        return XmbPalette.colorsFor(base, if (dimAtNight) XmbPalette.brightness(hour) else 1f)
    }

    companion object {
        const val ORIGINAL_COLOR = "original"
        const val CUSTOM_COLOR = "custom"
        const val GRADIENT_BACKGROUND = "gradient"
        const val IMAGE_BACKGROUND = "image"

        // A custom hue gets the monthly colors' typical strength.
        private const val CUSTOM_SATURATION = 0.8f
        private const val CUSTOM_VALUE = 0.8f

        fun current(): XmbPrefs {
            val settings = Settings.shared
            return XmbPrefs(
                color = settings.xmbColor,
                customHue = settings.xmbCustomHue,
                dimAtNight = settings.xmbDimAtNight,
                background = settings.xmbBackground,
                wallpaper = settings.xmbWallpaper,
                wallpaperDim = settings.xmbWallpaperDim.coerceIn(0f, 1f),
                wave = settings.xmbWave,
                sparkles = settings.xmbSparkles,
                waveReacts = settings.xmbWaveReacts,
                waveSpeed = settings.xmbWaveSpeed.coerceIn(0.1f, 4f),
                clock = settings.xmbClock,
                hour24 = settings.xmb24HourClock,
                dateFormat = XmbDateFormat.entries.firstOrNull { it.id == settings.xmbDateFormat } ?: XmbDateFormat.MONTH_DAY,
                menuSounds = settings.xmbMenuSounds,
                menuSoundVolume = settings.xmbMenuSoundVolume.coerceIn(0f, 1f),
                shortcuts = XmbShortcuts.decode(settings.xmbShortcuts),
                nowPlayingCard = settings.xmbNowPlayingCard,
                trackOverlay = XmbTrackOverlay.entries.firstOrNull { it.id == settings.xmbTrackOverlay } ?: XmbTrackOverlay.BRIEF,
                idleVisualizerSeconds = settings.xmbIdleVisualizer.coerceAtLeast(0),
                visualizerQuality = VisualizerQuality.of(settings.xmbVisualizerQuality),
                frameRate = if (settings.xmbFrameRate <= 30) 30 else 60,
            )
        }
    }
}

enum class XmbDateFormat(
    val id: String,
) {
    MONTH_DAY("md"),
    DAY_MONTH("dm"),
    HIDDEN("none"),
}

/** How the track info shows over a full-screen visualizer, video or lyrics. */
enum class XmbTrackOverlay(
    val id: String,
) {
    /** After opening and after each key, then it fades. */
    BRIEF("brief"),
    ALWAYS("always"),
    NEVER("never"),
}

/** The clock's text: "11/17 21:05", "17/11 9:05 PM" or just the time. */
fun xmbClockText(
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    hour24: Boolean,
    dateFormat: XmbDateFormat,
): String {
    val minutes = minute.toString().padStart(2, '0')
    val time = if (hour24) "$hour:$minutes" else "${(hour % 12).takeIf { it != 0 } ?: 12}:$minutes ${if (hour < 12) "AM" else "PM"}"
    return when (dateFormat) {
        XmbDateFormat.MONTH_DAY -> "$month/$day $time"
        XmbDateFormat.DAY_MONTH -> "$day/$month $time"
        XmbDateFormat.HIDDEN -> time
    }
}
