package com.metrolist.shared.xmb.settings

import androidx.compose.runtime.Composable
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.xmb_background_gradient
import com.metrolist.shared.generated.resources.xmb_background_image
import com.metrolist.shared.generated.resources.xmb_color_amber
import com.metrolist.shared.generated.resources.xmb_color_blue
import com.metrolist.shared.generated.resources.xmb_color_brown
import com.metrolist.shared.generated.resources.xmb_color_custom
import com.metrolist.shared.generated.resources.xmb_color_cyan
import com.metrolist.shared.generated.resources.xmb_color_gold
import com.metrolist.shared.generated.resources.xmb_color_green
import com.metrolist.shared.generated.resources.xmb_color_lime
import com.metrolist.shared.generated.resources.xmb_color_magenta
import com.metrolist.shared.generated.resources.xmb_color_original
import com.metrolist.shared.generated.resources.xmb_color_pink
import com.metrolist.shared.generated.resources.xmb_color_purple
import com.metrolist.shared.generated.resources.xmb_color_red
import com.metrolist.shared.generated.resources.xmb_color_silver
import com.metrolist.shared.generated.resources.xmb_date_day_month
import com.metrolist.shared.generated.resources.xmb_date_hidden
import com.metrolist.shared.generated.resources.xmb_date_month_day
import com.metrolist.shared.generated.resources.xmb_frame_rate_30
import com.metrolist.shared.generated.resources.xmb_frame_rate_60
import com.metrolist.shared.generated.resources.xmb_idle_1m
import com.metrolist.shared.generated.resources.xmb_idle_30s
import com.metrolist.shared.generated.resources.xmb_idle_3m
import com.metrolist.shared.generated.resources.xmb_idle_5m
import com.metrolist.shared.generated.resources.xmb_idle_off
import com.metrolist.shared.generated.resources.xmb_item_song
import com.metrolist.shared.generated.resources.xmb_item_volume
import com.metrolist.shared.generated.resources.xmb_overlay_always
import com.metrolist.shared.generated.resources.xmb_overlay_brief
import com.metrolist.shared.generated.resources.xmb_overlay_never
import com.metrolist.shared.generated.resources.xmb_quality_balanced
import com.metrolist.shared.generated.resources.xmb_quality_high
import com.metrolist.shared.generated.resources.xmb_quality_low
import com.metrolist.shared.generated.resources.xmb_setting_background
import com.metrolist.shared.generated.resources.xmb_setting_color
import com.metrolist.shared.generated.resources.xmb_setting_custom_hue
import com.metrolist.shared.generated.resources.xmb_setting_custom_hue_description
import com.metrolist.shared.generated.resources.xmb_setting_date_format
import com.metrolist.shared.generated.resources.xmb_setting_dim_at_night
import com.metrolist.shared.generated.resources.xmb_setting_dim_at_night_description
import com.metrolist.shared.generated.resources.xmb_setting_frame_rate
import com.metrolist.shared.generated.resources.xmb_setting_frame_rate_description
import com.metrolist.shared.generated.resources.xmb_setting_idle_visualizer
import com.metrolist.shared.generated.resources.xmb_setting_idle_visualizer_description
import com.metrolist.shared.generated.resources.xmb_setting_menu_sound_volume
import com.metrolist.shared.generated.resources.xmb_setting_menu_sounds
import com.metrolist.shared.generated.resources.xmb_setting_now_playing_card
import com.metrolist.shared.generated.resources.xmb_setting_now_playing_card_description
import com.metrolist.shared.generated.resources.xmb_setting_show_clock
import com.metrolist.shared.generated.resources.xmb_setting_sparkles
import com.metrolist.shared.generated.resources.xmb_setting_time_format
import com.metrolist.shared.generated.resources.xmb_setting_track_overlay
import com.metrolist.shared.generated.resources.xmb_setting_visualizer_quality
import com.metrolist.shared.generated.resources.xmb_setting_visualizer_quality_description
import com.metrolist.shared.generated.resources.xmb_setting_wallpaper
import com.metrolist.shared.generated.resources.xmb_setting_wallpaper_description
import com.metrolist.shared.generated.resources.xmb_setting_wallpaper_dim
import com.metrolist.shared.generated.resources.xmb_setting_wave
import com.metrolist.shared.generated.resources.xmb_setting_wave_reacts
import com.metrolist.shared.generated.resources.xmb_setting_wave_reacts_description
import com.metrolist.shared.generated.resources.xmb_setting_wave_speed
import com.metrolist.shared.generated.resources.xmb_settings_date_time
import com.metrolist.shared.generated.resources.xmb_settings_date_time_description
import com.metrolist.shared.generated.resources.xmb_settings_music
import com.metrolist.shared.generated.resources.xmb_settings_music_description
import com.metrolist.shared.generated.resources.xmb_settings_shortcuts
import com.metrolist.shared.generated.resources.xmb_settings_shortcuts_description
import com.metrolist.shared.generated.resources.xmb_settings_sound
import com.metrolist.shared.generated.resources.xmb_settings_sound_description
import com.metrolist.shared.generated.resources.xmb_settings_theme
import com.metrolist.shared.generated.resources.xmb_settings_theme_description
import com.metrolist.shared.generated.resources.xmb_shortcut_like
import com.metrolist.shared.generated.resources.xmb_shortcut_lyrics
import com.metrolist.shared.generated.resources.xmb_shortcut_media_keys
import com.metrolist.shared.generated.resources.xmb_shortcut_mute
import com.metrolist.shared.generated.resources.xmb_shortcut_next
import com.metrolist.shared.generated.resources.xmb_shortcut_not_set
import com.metrolist.shared.generated.resources.xmb_shortcut_play_pause
import com.metrolist.shared.generated.resources.xmb_shortcut_previous
import com.metrolist.shared.generated.resources.xmb_shortcut_repeat
import com.metrolist.shared.generated.resources.xmb_shortcut_reset
import com.metrolist.shared.generated.resources.xmb_shortcut_reset_confirm
import com.metrolist.shared.generated.resources.xmb_shortcut_search
import com.metrolist.shared.generated.resources.xmb_shortcut_seek_back
import com.metrolist.shared.generated.resources.xmb_shortcut_seek_forward
import com.metrolist.shared.generated.resources.xmb_shortcut_shuffle
import com.metrolist.shared.generated.resources.xmb_shortcut_visualizer
import com.metrolist.shared.generated.resources.xmb_shortcut_volume_down
import com.metrolist.shared.generated.resources.xmb_shortcut_volume_up
import com.metrolist.shared.generated.resources.xmb_speed_fast
import com.metrolist.shared.generated.resources.xmb_speed_normal
import com.metrolist.shared.generated.resources.xmb_speed_slow
import com.metrolist.shared.generated.resources.xmb_time_12
import com.metrolist.shared.generated.resources.xmb_time_24
import com.metrolist.shared.generated.resources.xmb_value_none
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.settings.SettingsChoice
import com.metrolist.shared.ui.settings.SettingsControl
import com.metrolist.shared.ui.settings.SettingsItemRegistration
import com.metrolist.shared.visualizer.VisualizerQuality
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.nav.XmbIcon
import com.metrolist.shared.xmb.nav.XmbItem
import com.metrolist.shared.xmb.theme.XmbIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

/** The XMB's own settings pages, at the top of the Settings column. */
enum class XmbSettingsPageId {
    THEME,
    DATE_TIME,
    SOUND,
    SHORTCUTS,
    MUSIC,
}

/** The Settings column's XMB items, above the app's own settings pages. */
fun xmbOwnSettingsItems(): List<XmbItem> =
    listOf(
        page(
            XmbSettingsPageId.THEME,
            Res.string.xmb_settings_theme,
            XmbIcon.Vector(XmbIcons.Theme),
            Res.string.xmb_settings_theme_description,
        ),
        page(
            XmbSettingsPageId.DATE_TIME,
            Res.string.xmb_settings_date_time,
            XmbIcon.Vector(XmbIcons.Clock),
            Res.string.xmb_settings_date_time_description,
        ),
        page(
            XmbSettingsPageId.SOUND,
            Res.string.xmb_settings_sound,
            XmbIcon.Drawable(Res.drawable.xmb_item_volume),
            Res.string.xmb_settings_sound_description,
        ),
        page(
            XmbSettingsPageId.SHORTCUTS,
            Res.string.xmb_settings_shortcuts,
            XmbIcon.Vector(XmbIcons.Keyboard),
            Res.string.xmb_settings_shortcuts_description,
        ),
        page(
            XmbSettingsPageId.MUSIC,
            Res.string.xmb_settings_music,
            XmbIcon.Drawable(Res.drawable.xmb_item_song),
            Res.string.xmb_settings_music_description,
        ),
    )

private fun page(
    id: XmbSettingsPageId,
    title: StringResource,
    icon: XmbIcon,
    subtitle: StringResource,
) = XmbItem("xmb_settings:${id.name}", title, icon, subtitle, list = XmbList.XmbSettings(id))

/** What the pages need from the XMB: choosing a background image (when the platform can) and recording a shortcut. */
class XmbSettingsHooks(
    val pickWallpaper: (() -> Unit)?,
    val recordShortcut: (XmbShortcut) -> Unit,
)

/** The shortcut's name, as the Keyboard Shortcuts page and the key prompt show it. */
fun XmbShortcut.title(): StringResource =
    when (this) {
        XmbShortcut.PLAY_PAUSE -> Res.string.xmb_shortcut_play_pause
        XmbShortcut.NEXT_TRACK -> Res.string.xmb_shortcut_next
        XmbShortcut.PREVIOUS_TRACK -> Res.string.xmb_shortcut_previous
        XmbShortcut.SEEK_FORWARD -> Res.string.xmb_shortcut_seek_forward
        XmbShortcut.SEEK_BACK -> Res.string.xmb_shortcut_seek_back
        XmbShortcut.SHUFFLE -> Res.string.xmb_shortcut_shuffle
        XmbShortcut.REPEAT -> Res.string.xmb_shortcut_repeat
        XmbShortcut.VOLUME_UP -> Res.string.xmb_shortcut_volume_up
        XmbShortcut.VOLUME_DOWN -> Res.string.xmb_shortcut_volume_down
        XmbShortcut.MUTE -> Res.string.xmb_shortcut_mute
        XmbShortcut.LIKE -> Res.string.xmb_shortcut_like
        XmbShortcut.VISUALIZER -> Res.string.xmb_shortcut_visualizer
        XmbShortcut.LYRICS -> Res.string.xmb_shortcut_lyrics
        XmbShortcut.SEARCH -> Res.string.xmb_shortcut_search
    }

/**
 * [page]'s rows, as settings-registry items so the XMB's settings rows and value pickers draw
 * them. Each writes its value to [Settings.shared] straight away.
 */
@Composable
fun xmbSettingsRows(
    page: XmbSettingsPageId,
    prefs: XmbPrefs,
    hooks: XmbSettingsHooks,
): List<SettingsItemRegistration> {
    val settings = Settings.shared
    return when (page) {
        XmbSettingsPageId.THEME -> {
            val colors =
                listOf(
                    Res.string.xmb_color_silver,
                    Res.string.xmb_color_gold,
                    Res.string.xmb_color_lime,
                    Res.string.xmb_color_pink,
                    Res.string.xmb_color_green,
                    Res.string.xmb_color_purple,
                    Res.string.xmb_color_cyan,
                    Res.string.xmb_color_blue,
                    Res.string.xmb_color_magenta,
                    Res.string.xmb_color_amber,
                    Res.string.xmb_color_brown,
                    Res.string.xmb_color_red,
                ).mapIndexed { index, name -> "${index + 1}" to stringResource(name) }
            val image = prefs.wallpaper.takeIf(String::isNotBlank)
            val pickImage = hooks.pickWallpaper
            listOf(
                choice(
                    "xmb_color",
                    stringResource(Res.string.xmb_setting_color),
                    listOf(XmbPrefs.ORIGINAL_COLOR to stringResource(Res.string.xmb_color_original)) + colors +
                        (XmbPrefs.CUSTOM_COLOR to stringResource(Res.string.xmb_color_custom)),
                    prefs.color,
                ) { settings.xmbColor = it },
                slider(
                    "xmb_custom_hue",
                    stringResource(Res.string.xmb_setting_custom_hue),
                    stringResource(Res.string.xmb_setting_custom_hue_description),
                    prefs.customHue,
                    0f..350f,
                    steps = 34,
                    label = { "${it.roundToInt()}°" },
                    enabled = prefs.color == XmbPrefs.CUSTOM_COLOR,
                ) { settings.xmbCustomHue = it },
                switch(
                    "xmb_dim_at_night",
                    stringResource(Res.string.xmb_setting_dim_at_night),
                    stringResource(Res.string.xmb_setting_dim_at_night_description),
                    prefs.dimAtNight,
                ) { settings.xmbDimAtNight = it },
                choice(
                    "xmb_background",
                    stringResource(Res.string.xmb_setting_background),
                    listOf(
                        XmbPrefs.GRADIENT_BACKGROUND to stringResource(Res.string.xmb_background_gradient),
                        XmbPrefs.IMAGE_BACKGROUND to stringResource(Res.string.xmb_background_image),
                    ),
                    prefs.background,
                ) {
                    // Picking Image with no image chosen yet asks for one first.
                    if (it == XmbPrefs.IMAGE_BACKGROUND && image == null && pickImage != null) pickImage() else settings.xmbBackground = it
                },
                SettingsItemRegistration(
                    id = "xmb_wallpaper",
                    title = stringResource(Res.string.xmb_setting_wallpaper),
                    description = stringResource(Res.string.xmb_setting_wallpaper_description),
                    control = SettingsControl.ACTION,
                    valueLabel = image?.substringAfterLast('/')?.substringAfterLast('\\') ?: stringResource(Res.string.xmb_value_none),
                    enabled = pickImage != null,
                    onClick = pickImage,
                ),
                slider(
                    "xmb_wallpaper_dim",
                    stringResource(Res.string.xmb_setting_wallpaper_dim),
                    null,
                    prefs.wallpaperDim,
                    0f..0.8f,
                    steps = 7,
                    label = ::percent,
                    enabled = prefs.wallpaperPath != null,
                ) { settings.xmbWallpaperDim = it },
                switch("xmb_wave", stringResource(Res.string.xmb_setting_wave), null, prefs.wave) { settings.xmbWave = it },
                switch("xmb_sparkles", stringResource(Res.string.xmb_setting_sparkles), null, prefs.sparkles) { settings.xmbSparkles = it },
                switch(
                    "xmb_wave_reacts",
                    stringResource(Res.string.xmb_setting_wave_reacts),
                    stringResource(Res.string.xmb_setting_wave_reacts_description),
                    prefs.waveReacts,
                    enabled = prefs.wave,
                ) { settings.xmbWaveReacts = it },
                choice(
                    "xmb_frame_rate",
                    stringResource(Res.string.xmb_setting_frame_rate),
                    listOf("60" to stringResource(Res.string.xmb_frame_rate_60), "30" to stringResource(Res.string.xmb_frame_rate_30)),
                    prefs.frameRate.toString(),
                    description = stringResource(Res.string.xmb_setting_frame_rate_description),
                ) { id -> id.toIntOrNull()?.let { settings.xmbFrameRate = it } },
                choice(
                    "xmb_wave_speed",
                    stringResource(Res.string.xmb_setting_wave_speed),
                    WAVE_SPEEDS.map { (speed, name) -> speed.toString() to stringResource(name) },
                    WAVE_SPEEDS.minBy { abs(it.first - prefs.waveSpeed) }.first.toString(),
                ) { id -> id.toFloatOrNull()?.let { settings.xmbWaveSpeed = it } },
            )
        }

        XmbSettingsPageId.DATE_TIME -> {
            listOf(
                switch("xmb_clock", stringResource(Res.string.xmb_setting_show_clock), null, prefs.clock) { settings.xmbClock = it },
                choice(
                    "xmb_time_format",
                    stringResource(Res.string.xmb_setting_time_format),
                    listOf("24" to stringResource(Res.string.xmb_time_24), "12" to stringResource(Res.string.xmb_time_12)),
                    if (prefs.hour24) "24" else "12",
                    enabled = prefs.clock,
                ) { settings.xmb24HourClock = it == "24" },
                choice(
                    "xmb_date_format",
                    stringResource(Res.string.xmb_setting_date_format),
                    listOf(
                        XmbDateFormat.MONTH_DAY.id to stringResource(Res.string.xmb_date_month_day),
                        XmbDateFormat.DAY_MONTH.id to stringResource(Res.string.xmb_date_day_month),
                        XmbDateFormat.HIDDEN.id to stringResource(Res.string.xmb_date_hidden),
                    ),
                    prefs.dateFormat.id,
                    enabled = prefs.clock,
                ) { settings.xmbDateFormat = it },
            )
        }

        XmbSettingsPageId.SOUND -> {
            listOf(
                switch("xmb_menu_sounds", stringResource(Res.string.xmb_setting_menu_sounds), null, prefs.menuSounds) {
                    settings.xmbMenuSounds = it
                },
                slider(
                    "xmb_menu_sound_volume",
                    stringResource(Res.string.xmb_setting_menu_sound_volume),
                    null,
                    prefs.menuSoundVolume,
                    0f..1f,
                    steps = 9,
                    label = ::percent,
                    enabled = prefs.menuSounds,
                ) { settings.xmbMenuSoundVolume = it },
            )
        }

        XmbSettingsPageId.SHORTCUTS -> {
            val notSet = stringResource(Res.string.xmb_shortcut_not_set)
            val mediaKeys = stringResource(Res.string.xmb_shortcut_media_keys)
            XmbShortcut.entries.map { shortcut ->
                SettingsItemRegistration(
                    id = "xmb_shortcut_${shortcut.name}",
                    title = stringResource(shortcut.title()),
                    description = mediaKeys.takeIf { shortcut in MEDIA_KEY_SHORTCUTS },
                    control = SettingsControl.ACTION,
                    valueLabel = prefs.shortcuts[shortcut]?.label ?: notSet,
                    onClick = { hooks.recordShortcut(shortcut) },
                )
            } +
                SettingsItemRegistration(
                    id = "xmb_shortcuts_reset",
                    title = stringResource(Res.string.xmb_shortcut_reset),
                    control = SettingsControl.ACTION,
                    confirmationMessage = stringResource(Res.string.xmb_shortcut_reset_confirm),
                    onClick = { settings.xmbShortcuts = "" },
                )
        }

        XmbSettingsPageId.MUSIC -> {
            listOf(
                switch(
                    "xmb_now_playing_card",
                    stringResource(Res.string.xmb_setting_now_playing_card),
                    stringResource(Res.string.xmb_setting_now_playing_card_description),
                    prefs.nowPlayingCard,
                ) { settings.xmbNowPlayingCard = it },
                choice(
                    "xmb_track_overlay",
                    stringResource(Res.string.xmb_setting_track_overlay),
                    listOf(
                        XmbTrackOverlay.BRIEF.id to stringResource(Res.string.xmb_overlay_brief),
                        XmbTrackOverlay.ALWAYS.id to stringResource(Res.string.xmb_overlay_always),
                        XmbTrackOverlay.NEVER.id to stringResource(Res.string.xmb_overlay_never),
                    ),
                    prefs.trackOverlay.id,
                ) { settings.xmbTrackOverlay = it },
                choice(
                    "xmb_visualizer_quality",
                    stringResource(Res.string.xmb_setting_visualizer_quality),
                    listOf(
                        VisualizerQuality.HIGH.id to stringResource(Res.string.xmb_quality_high),
                        VisualizerQuality.BALANCED.id to stringResource(Res.string.xmb_quality_balanced),
                        VisualizerQuality.LOW.id to stringResource(Res.string.xmb_quality_low),
                    ),
                    prefs.visualizerQuality.id,
                    description = stringResource(Res.string.xmb_setting_visualizer_quality_description),
                ) { settings.xmbVisualizerQuality = it },
                choice(
                    "xmb_idle_visualizer",
                    stringResource(Res.string.xmb_setting_idle_visualizer),
                    IDLE_TIMES.map { (seconds, name) -> seconds.toString() to stringResource(name) },
                    (IDLE_TIMES.firstOrNull { it.first == prefs.idleVisualizerSeconds } ?: IDLE_TIMES.first()).first.toString(),
                    description = stringResource(Res.string.xmb_setting_idle_visualizer_description),
                ) { id -> id.toIntOrNull()?.let { settings.xmbIdleVisualizer = it } },
            )
        }
    }
}

private val MEDIA_KEY_SHORTCUTS = setOf(XmbShortcut.PLAY_PAUSE, XmbShortcut.NEXT_TRACK, XmbShortcut.PREVIOUS_TRACK)

private val WAVE_SPEEDS =
    listOf(0.5f to Res.string.xmb_speed_slow, 1f to Res.string.xmb_speed_normal, 1.75f to Res.string.xmb_speed_fast)

private val IDLE_TIMES =
    listOf(
        0 to Res.string.xmb_idle_off,
        30 to Res.string.xmb_idle_30s,
        60 to Res.string.xmb_idle_1m,
        180 to Res.string.xmb_idle_3m,
        300 to Res.string.xmb_idle_5m,
    )

private fun percent(value: Float): String = "${(value * 100).roundToInt()}%"

private fun switch(
    id: String,
    title: String,
    description: String?,
    checked: Boolean,
    enabled: Boolean = true,
    set: (Boolean) -> Unit,
) = SettingsItemRegistration(
    id = id,
    title = title,
    description = description,
    control = SettingsControl.SWITCH,
    enabled = enabled,
    checked = checked,
    onCheckedChange = set,
)

private fun choice(
    id: String,
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    enabled: Boolean = true,
    description: String? = null,
    set: (String) -> Unit,
) = SettingsItemRegistration(
    id = id,
    title = title,
    description = description,
    control = SettingsControl.CHOICE,
    enabled = enabled,
    choices = options.map { (choiceId, label) -> SettingsChoice(choiceId, label) },
    selectedChoiceId = selected,
    onChoiceSelected = set,
)

private fun slider(
    id: String,
    title: String,
    description: String?,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    label: (Float) -> String,
    enabled: Boolean = true,
    set: (Float) -> Unit,
) = SettingsItemRegistration(
    id = id,
    title = title,
    description = description,
    control = SettingsControl.SLIDER,
    enabled = enabled,
    valueLabel = label(value),
    sliderValue = value.coerceIn(range.start, range.endInclusive),
    sliderRange = range,
    sliderSteps = steps,
    sliderValueFormatter = { label(it) },
    onSliderChanged = set,
)
