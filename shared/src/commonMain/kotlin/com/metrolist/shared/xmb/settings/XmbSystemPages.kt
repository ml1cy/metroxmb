package com.metrolist.shared.xmb.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import com.metrolist.shared.BuildConfig
import com.metrolist.shared.equalizer.EqualizerPreset
import com.metrolist.shared.equalizer.EqualizerProfiles
import com.metrolist.shared.equalizer.MAX_AUDIO_BOOST_DB
import com.metrolist.shared.equalizer.MIN_AUDIO_PREAMP_DB
import com.metrolist.shared.equalizer.bandsByFrequency
import com.metrolist.shared.equalizer.supportsGain
import com.metrolist.shared.equalizer.withAdjustedGain
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.backup_create_failed
import com.metrolist.shared.generated.resources.backup_created
import com.metrolist.shared.generated.resources.backup_sensitive_message
import com.metrolist.shared.generated.resources.cached_resolutions
import com.metrolist.shared.generated.resources.calculating
import com.metrolist.shared.generated.resources.clear_all_downloads
import com.metrolist.shared.generated.resources.clear_all_downloads_dialog
import com.metrolist.shared.generated.resources.clear_animated_artwork_cache
import com.metrolist.shared.generated.resources.clear_animated_artwork_cache_dialog
import com.metrolist.shared.generated.resources.clear_image_cache
import com.metrolist.shared.generated.resources.clear_image_cache_dialog
import com.metrolist.shared.generated.resources.clear_music_video_cache
import com.metrolist.shared.generated.resources.clear_music_video_cache_dialog
import com.metrolist.shared.generated.resources.clear_song_cache
import com.metrolist.shared.generated.resources.clear_song_cache_dialog
import com.metrolist.shared.generated.resources.create_backup
import com.metrolist.shared.generated.resources.create_backup_desc
import com.metrolist.shared.generated.resources.devmode_already_active
import com.metrolist.shared.generated.resources.devtools_enabled
import com.metrolist.shared.generated.resources.disable
import com.metrolist.shared.generated.resources.download_location
import com.metrolist.shared.generated.resources.equalizer_stream_unavailable
import com.metrolist.shared.generated.resources.error_unknown
import com.metrolist.shared.generated.resources.max_animated_artwork_cache_size
import com.metrolist.shared.generated.resources.max_image_cache_size
import com.metrolist.shared.generated.resources.max_song_cache_size
import com.metrolist.shared.generated.resources.metroeq
import com.metrolist.shared.generated.resources.metroeq_preset_bass
import com.metrolist.shared.generated.resources.metroeq_preset_flat
import com.metrolist.shared.generated.resources.metroeq_preset_rock
import com.metrolist.shared.generated.resources.metroeq_preset_treble
import com.metrolist.shared.generated.resources.metroeq_preset_v_shape
import com.metrolist.shared.generated.resources.metroeq_preset_vocal
import com.metrolist.shared.generated.resources.restore_backup
import com.metrolist.shared.generated.resources.restore_backup_desc
import com.metrolist.shared.generated.resources.restore_failed
import com.metrolist.shared.generated.resources.restore_replaces_data
import com.metrolist.shared.generated.resources.unlimited
import com.metrolist.shared.generated.resources.xmb_about_app
import com.metrolist.shared.generated.resources.xmb_about_app_description
import com.metrolist.shared.generated.resources.xmb_about_based_on
import com.metrolist.shared.generated.resources.xmb_about_based_on_description
import com.metrolist.shared.generated.resources.xmb_about_imagery
import com.metrolist.shared.generated.resources.xmb_about_imagery_description
import com.metrolist.shared.generated.resources.xmb_about_license
import com.metrolist.shared.generated.resources.xmb_about_ribbon
import com.metrolist.shared.generated.resources.xmb_about_ribbon_description
import com.metrolist.shared.generated.resources.xmb_backup_more
import com.metrolist.shared.generated.resources.xmb_backup_more_description
import com.metrolist.shared.generated.resources.xmb_eq_band
import com.metrolist.shared.generated.resources.xmb_eq_preamp
import com.metrolist.shared.generated.resources.xmb_eq_profile
import com.metrolist.shared.generated.resources.xmb_eq_reset
import com.metrolist.shared.generated.resources.xmb_eq_save
import com.metrolist.shared.generated.resources.xmb_eq_save_description
import com.metrolist.shared.generated.resources.xmb_value_none
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.settings.SettingsChoice
import com.metrolist.shared.ui.settings.SettingsControl
import com.metrolist.shared.ui.settings.SettingsItemRegistration
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.ui.utils.rememberFileSizeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** A cache or the downloads, as the Storage page lists them. */
enum class XmbStore { DOWNLOADS, SONGS, IMAGES, ANIMATED_ARTWORK, MUSIC_VIDEOS }

/**
 * What the Storage page shows: bytes used per store (music videos count cached entries in
 * [videoEntries] instead), each limit in MB (-1 is unlimited, 0 is off) and where downloads go.
 */
data class XmbStorageUsage(
    val bytes: Map<XmbStore, Long> = emptyMap(),
    val limitsMb: Map<XmbStore, Int> = emptyMap(),
    val videoEntries: Int? = null,
    val location: String? = null,
)

/** The platform's caches and downloads, for the XMB's Storage page. */
interface XmbStorage {
    /** The limits each store offers, in MB; stores without one have no limit row. */
    val limitChoices: Map<XmbStore, List<Int>>

    suspend fun usage(): XmbStorageUsage

    /** Empties [store]; false when that failed. */
    suspend fun clear(store: XmbStore): Boolean

    suspend fun setLimit(
        store: XmbStore,
        megabytes: Int,
    )
}

/** The platform's backups: each asks for a file first; [done] gets true, false on failure, or null when cancelled. */
interface XmbBackup {
    fun create(done: (Boolean?) -> Unit)

    /** Restores a backup; the app may restart to load it. */
    fun restore(done: (Boolean?) -> Unit)
}

/** The platform side of the XMB's own versions of the stock settings pages. */
class XmbSystemPages(
    val storage: XmbStorage? = null,
    val backup: XmbBackup? = null,
    /** False where the audio path can't apply MetroEQ to the stream (shown on the page). */
    val audioProcessing: () -> Boolean = { true },
) {
    /** The custom registry pages the XMB draws itself; any other custom page opens its stock screen. */
    val drawn: Set<SettingsPageId> =
        buildSet {
            add(SettingsPageId.ABOUT)
            add(SettingsPageId.EQUALIZER)
            if (storage != null) add(SettingsPageId.STORAGE)
            if (backup != null) add(SettingsPageId.BACKUP_RESTORE)
        }
}

/** What the system pages need from the XMB: a message at the top, and the stock screen of a page. */
class XmbSystemHooks(
    val toast: (String) -> Unit,
    val openStock: (SettingsPageId) -> Unit,
)

/**
 * [page]'s rows, when the XMB draws it ([XmbSystemPages.drawn]); null otherwise. [revision] changes
 * after every setting the XMB applies, so rows that read preferences are rebuilt.
 */
@Composable
fun xmbSystemRows(
    page: SettingsPageId,
    pages: XmbSystemPages,
    hooks: XmbSystemHooks,
    revision: Int,
): List<SettingsItemRegistration>? {
    if (page !in pages.drawn) return null
    return when (page) {
        SettingsPageId.ABOUT -> aboutRows(hooks)
        SettingsPageId.EQUALIZER -> equalizerRows(pages, revision)
        SettingsPageId.STORAGE -> pages.storage?.let { storageRows(it, hooks) }
        SettingsPageId.BACKUP_RESTORE -> pages.backup?.let { backupRows(it, hooks) }
        else -> null
    }
}

@Composable
private fun aboutRows(hooks: XmbSystemHooks): List<SettingsItemRegistration> {
    val uri = LocalUriHandler.current
    var presses by remember { mutableIntStateOf(0) }
    val unlocked = stringResource(Res.string.devtools_enabled)
    val alreadyUnlocked = stringResource(Res.string.devmode_already_active)

    fun open(url: String) {
        runCatching { uri.openUri(url) }
    }
    return listOf(
        SettingsItemRegistration(
            id = "xmb_about_app",
            title = stringResource(Res.string.xmb_about_app),
            description = stringResource(Res.string.xmb_about_app_description),
            control = SettingsControl.ACTION,
            valueLabel = BuildConfig.APP_VERSION,
            // Three presses unlock the developer tools, like the logo on the stock About page.
            onClick = {
                if (Settings.shared.devtoolsUnlocked) {
                    hooks.toast(alreadyUnlocked)
                } else if (++presses >= DEVTOOLS_PRESSES) {
                    presses = 0
                    Settings.shared.devtoolsUnlocked = true
                    hooks.toast(unlocked)
                }
            },
        ),
        link(
            "xmb_about_based_on",
            stringResource(Res.string.xmb_about_based_on),
            stringResource(Res.string.xmb_about_based_on_description),
        ) { open(METROLIST_URL) },
        link("xmb_about_license", stringResource(Res.string.xmb_about_license), "GPL-3.0") { open(LICENSE_URL) },
        link(
            "xmb_about_ribbon",
            stringResource(Res.string.xmb_about_ribbon),
            stringResource(Res.string.xmb_about_ribbon_description),
        ) { open(RIBBON_URL) },
        link(
            "xmb_about_imagery",
            stringResource(Res.string.xmb_about_imagery),
            stringResource(Res.string.xmb_about_imagery_description),
        ) { open(NASA_URL) },
    )
}

private fun link(
    id: String,
    title: String,
    description: String,
    open: () -> Unit,
) = SettingsItemRegistration(id = id, title = title, description = description, control = SettingsControl.ACTION, onClick = open)

@Composable
private fun equalizerRows(
    pages: XmbSystemPages,
    revision: Int,
): List<SettingsItemRegistration> {
    val presetNames = EqualizerPreset.entries.associateWith { presetName(it) }
    val profileTitle = stringResource(Res.string.xmb_eq_profile)
    val preampTitle = stringResource(Res.string.xmb_eq_preamp)
    val resetTitle = stringResource(Res.string.xmb_eq_reset)
    val saveTitle = stringResource(Res.string.xmb_eq_save)
    val saveDescription = stringResource(Res.string.xmb_eq_save_description)
    val unavailable = stringResource(Res.string.equalizer_stream_unavailable).takeUnless { pages.audioProcessing() }
    val none = stringResource(Res.string.xmb_value_none)
    val eqTitle = stringResource(Res.string.metroeq)
    val bandFormat = stringResource(Res.string.xmb_eq_band)
    // Every row reads the profiles from preferences, so they're rebuilt after each change.
    return remember(revision, presetNames, unavailable, eqTitle, bandFormat) {
        val settings = Settings.shared
        val configuration = EqualizerProfiles.selectedConfiguration
        val active = configuration.profile
        val saved = EqualizerProfiles.profiles
        val presetIds = EqualizerPreset.entries.map { it.profileId }
        buildList {
            add(
                SettingsItemRegistration(
                    id = "xmb_eq_enabled",
                    title = eqTitle,
                    description = unavailable,
                    control = SettingsControl.SWITCH,
                    checked = settings.equalizerEnabled,
                    onCheckedChange = EqualizerProfiles::setEnabled,
                ),
            )
            val choices =
                EqualizerPreset.entries.map { SettingsChoice(it.profileId, presetNames.getValue(it)) } +
                    saved.filterNot { it.id in presetIds }.map { SettingsChoice(it.id, it.name) }
            add(
                SettingsItemRegistration(
                    id = "xmb_eq_profile",
                    title = profileTitle,
                    control = SettingsControl.CHOICE,
                    choices = choices,
                    selectedChoiceId = active?.id,
                    valueLabel = active?.name ?: none,
                    onChoiceSelected = { id ->
                        val preset = EqualizerPreset.entries.firstOrNull { it.profileId == id }
                        if (preset !=
                            null
                        ) {
                            EqualizerProfiles.activatePreset(preset, presetNames.getValue(preset))
                        } else {
                            EqualizerProfiles.setActive(id)
                        }
                    },
                ),
            )
            add(
                SettingsItemRegistration(
                    id = "xmb_eq_preamp",
                    title = preampTitle,
                    control = SettingsControl.SLIDER,
                    valueLabel = decibels(configuration.preampDb.toFloat()),
                    sliderValue = configuration.preampDb.toFloat(),
                    sliderRange = MIN_AUDIO_PREAMP_DB.toFloat()..MAX_AUDIO_BOOST_DB.toFloat(),
                    sliderSteps = ((MAX_AUDIO_BOOST_DB - MIN_AUDIO_PREAMP_DB) * 2).roundToInt() - 1,
                    sliderValueFormatter = { decibels(it) },
                    onSliderChanged = { EqualizerProfiles.setPreamp(it.toDouble()) },
                ),
            )
            active?.bandsByFrequency?.forEach { (index, band) ->
                if (!band.supportsGain) return@forEach
                val title = bandFormat.replace("%1\$s", frequency(band.frequency))
                val value = band.gain.toFloat().coerceIn(-BAND_RANGE_DB, BAND_RANGE_DB)
                add(
                    SettingsItemRegistration(
                        id = "xmb_eq_band_$index",
                        title = title,
                        control = SettingsControl.SLIDER,
                        enabled = settings.equalizerEnabled,
                        valueLabel = decibels(band.gain.toFloat()),
                        sliderValue = value,
                        sliderRange = -BAND_RANGE_DB..BAND_RANGE_DB,
                        sliderSteps = (BAND_RANGE_DB * 4).roundToInt() - 1,
                        sliderValueFormatter = { decibels(it) },
                        onSliderChanged = { gain ->
                            val current = EqualizerProfiles.activeProfile ?: return@SettingsItemRegistration
                            val bands = current.bands.toMutableList()
                            bands[index] = bands[index].withAdjustedGain(gain.toDouble())
                            EqualizerProfiles.editCurrent(current.copy(bands = bands))
                        },
                    ),
                )
            }
            if (active != null && EqualizerProfiles.hasUnsavedChanges) {
                add(
                    SettingsItemRegistration(
                        id = "xmb_eq_reset",
                        title = resetTitle,
                        control = SettingsControl.ACTION,
                        onClick = EqualizerProfiles::resetAllBands,
                    ),
                )
            }
            add(
                SettingsItemRegistration(
                    id = "xmb_eq_save",
                    title = saveTitle,
                    description = saveDescription,
                    control = SettingsControl.TEXT,
                    textValue = active?.name?.takeUnless { active.id in presetIds }.orEmpty(),
                    onTextChanged = { name -> if (name.isNotBlank()) EqualizerProfiles.saveCurrent(name, saveAs = true) },
                ),
            )
        }
    }
}

@Composable
private fun storageRows(
    storage: XmbStorage,
    hooks: XmbSystemHooks,
): List<SettingsItemRegistration> {
    val formatSize = rememberFileSizeFormatter()
    val scope = rememberCoroutineScope()
    var usage by remember { mutableStateOf<XmbStorageUsage?>(null) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(storage, refresh) {
        // Sizes keep changing while songs cache, so they're read again every few seconds.
        while (true) {
            runCatching { usage = storage.usage() }.onFailure { if (it is CancellationException) throw it }
            delay(STORAGE_REFRESH_MS)
        }
    }
    val calculating = stringResource(Res.string.calculating)
    val unlimited = stringResource(Res.string.unlimited)
    val off = stringResource(Res.string.disable)
    val failed = stringResource(Res.string.error_unknown)
    val videosLabel = usage?.videoEntries?.let { stringResource(Res.string.cached_resolutions, it) }
    val titles =
        mapOf(
            XmbStore.DOWNLOADS to (Res.string.clear_all_downloads to Res.string.clear_all_downloads_dialog),
            XmbStore.SONGS to (Res.string.clear_song_cache to Res.string.clear_song_cache_dialog),
            XmbStore.IMAGES to (Res.string.clear_image_cache to Res.string.clear_image_cache_dialog),
            XmbStore.ANIMATED_ARTWORK to (Res.string.clear_animated_artwork_cache to Res.string.clear_animated_artwork_cache_dialog),
            XmbStore.MUSIC_VIDEOS to (Res.string.clear_music_video_cache to Res.string.clear_music_video_cache_dialog),
        ).mapValues { (_, keys) -> stringResource(keys.first) to stringResource(keys.second) }
    val limitTitles =
        mapOf(
            XmbStore.SONGS to stringResource(Res.string.max_song_cache_size),
            XmbStore.IMAGES to stringResource(Res.string.max_image_cache_size),
            XmbStore.ANIMATED_ARTWORK to stringResource(Res.string.max_animated_artwork_cache_size),
        )
    val locationTitle = stringResource(Res.string.download_location)

    fun limitLabel(mb: Int) =
        when {
            mb < 0 -> unlimited
            mb == 0 -> off
            else -> formatSize(mb * 1024L * 1024L)
        }

    fun perform(action: suspend () -> Boolean) {
        if (busy) return
        busy = true
        scope.launch {
            val ok =
                try {
                    action()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    false
                }
            busy = false
            if (!ok) hooks.toast(failed)
            refresh++
        }
    }
    val current = usage
    return buildList {
        XmbStore.entries.forEach { store ->
            val (title, question) = titles.getValue(store)
            val value =
                when {
                    current == null -> calculating
                    store == XmbStore.MUSIC_VIDEOS -> videosLabel ?: return@forEach
                    else -> current.bytes[store]?.let(formatSize) ?: return@forEach
                }
            add(
                SettingsItemRegistration(
                    id = "xmb_storage_clear_${store.name.lowercase()}",
                    title = title,
                    control = SettingsControl.ACTION,
                    valueLabel = value,
                    enabled = !busy && current != null,
                    confirmationMessage = question,
                    onClick = { perform { storage.clear(store) } },
                ),
            )
            val choices = storage.limitChoices[store] ?: return@forEach
            val limit = current?.limitsMb?.get(store) ?: return@forEach
            val options = (choices + limit).distinct().sortedBy { if (it < 0) Int.MAX_VALUE else it }
            add(
                SettingsItemRegistration(
                    id = "xmb_storage_limit_${store.name.lowercase()}",
                    title = limitTitles.getValue(store),
                    control = SettingsControl.CHOICE,
                    enabled = !busy,
                    choices = options.map { SettingsChoice(it.toString(), limitLabel(it)) },
                    selectedChoiceId = limit.toString(),
                    valueLabel = limitLabel(limit),
                    onChoiceSelected = { id ->
                        val megabytes = id.toIntOrNull()
                        if (megabytes != null) {
                            perform {
                                storage.setLimit(store, megabytes)
                                true
                            }
                        }
                    },
                ),
            )
        }
        current?.location?.let { location ->
            add(
                SettingsItemRegistration(
                    id = "xmb_storage_location",
                    title = locationTitle,
                    description = location,
                    control = SettingsControl.ACTION,
                    onClick = { hooks.openStock(SettingsPageId.STORAGE) },
                ),
            )
        }
    }
}

@Composable
private fun backupRows(
    backup: XmbBackup,
    hooks: XmbSystemHooks,
): List<SettingsItemRegistration> {
    val created = stringResource(Res.string.backup_created)
    val createFailed = stringResource(Res.string.backup_create_failed)
    val restoreFailed = stringResource(Res.string.restore_failed)
    return listOf(
        SettingsItemRegistration(
            id = "xmb_backup_create",
            title = stringResource(Res.string.create_backup),
            description = stringResource(Res.string.create_backup_desc),
            control = SettingsControl.ACTION,
            // The stock page warns before every backup: the file holds account credentials.
            confirmationMessage = stringResource(Res.string.backup_sensitive_message),
            onClick = { backup.create { ok -> if (ok != null) hooks.toast(if (ok) created else createFailed) } },
        ),
        SettingsItemRegistration(
            id = "xmb_backup_restore",
            title = stringResource(Res.string.restore_backup),
            description = stringResource(Res.string.restore_backup_desc),
            control = SettingsControl.ACTION,
            confirmationMessage = stringResource(Res.string.restore_replaces_data),
            onClick = { backup.restore { ok -> if (ok == false) hooks.toast(restoreFailed) } },
        ),
        SettingsItemRegistration(
            id = "xmb_backup_more",
            title = stringResource(Res.string.xmb_backup_more),
            description = stringResource(Res.string.xmb_backup_more_description),
            control = SettingsControl.ACTION,
            onClick = { hooks.openStock(SettingsPageId.BACKUP_RESTORE) },
        ),
    )
}

@Composable
private fun presetName(preset: EqualizerPreset): String =
    stringResource(
        when (preset) {
            EqualizerPreset.FLAT -> Res.string.metroeq_preset_flat
            EqualizerPreset.BASS -> Res.string.metroeq_preset_bass
            EqualizerPreset.TREBLE -> Res.string.metroeq_preset_treble
            EqualizerPreset.VOCAL -> Res.string.metroeq_preset_vocal
            EqualizerPreset.ROCK -> Res.string.metroeq_preset_rock
            EqualizerPreset.V_SHAPE -> Res.string.metroeq_preset_v_shape
        },
    )

/** "+3.5 dB", "-2 dB", "0 dB". */
internal fun decibels(value: Float): String {
    val halves = (value * 2).roundToInt()
    val sign =
        if (halves > 0) {
            "+"
        } else if (halves < 0) {
            "-"
        } else {
            ""
        }
    val whole = kotlin.math.abs(halves) / 2
    return if (kotlin.math.abs(halves) % 2 == 0) "$sign$whole dB" else "$sign$whole.5 dB"
}

/** "60 Hz", "1.2 kHz", "16 kHz". */
internal fun frequency(hertz: Double): String =
    if (hertz < 1000) {
        "${hertz.roundToInt()} Hz"
    } else {
        val tenths = (hertz / 100).roundToInt()
        if (tenths % 10 == 0) "${tenths / 10} kHz" else "${tenths / 10}.${tenths % 10} kHz"
    }

private const val DEVTOOLS_PRESSES = 3
private const val BAND_RANGE_DB = 15f
private const val STORAGE_REFRESH_MS = 3_000L
private const val METROLIST_URL = "https://github.com/MetrolistGroup/Metrolist"
private const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
private const val RIBBON_URL = "https://github.com/tsbehlman/xmb"
private const val NASA_URL = "https://earthobservatory.nasa.gov/features/BlueMarble"
