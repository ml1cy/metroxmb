package com.metrolist.shared.xmb.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.metrolist.shared.api.lyrics.AiProviderPresets
import com.metrolist.shared.api.lyrics.LyricsTranslationClient
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.ai_test_connection_failed
import com.metrolist.shared.generated.resources.ai_test_missing_credentials
import com.metrolist.shared.generated.resources.brand_better_lyrics
import com.metrolist.shared.generated.resources.brand_kugou
import com.metrolist.shared.generated.resources.brand_lrclib
import com.metrolist.shared.generated.resources.brand_lrcred
import com.metrolist.shared.generated.resources.brand_lyprio
import com.metrolist.shared.generated.resources.brand_paxsenix
import com.metrolist.shared.generated.resources.brand_unison
import com.metrolist.shared.generated.resources.openrouter_auth_code
import com.metrolist.shared.generated.resources.openrouter_auth_instruction
import com.metrolist.shared.generated.resources.openrouter_sign_in_description
import com.metrolist.shared.generated.resources.openrouter_signed_in_description
import com.metrolist.shared.generated.resources.restore_hidden_item_message
import com.metrolist.shared.generated.resources.unblock_artist_message
import com.metrolist.shared.generated.resources.xmb_ai_test_passed
import com.metrolist.shared.generated.resources.xmb_lyrics_offset_value
import com.metrolist.shared.generated.resources.xmb_lyrics_provider_position
import com.metrolist.shared.generated.resources.xmb_openrouter_sign_out
import com.metrolist.shared.generated.resources.xmb_value_hidden
import com.metrolist.shared.generated.resources.xmb_value_off
import com.metrolist.shared.model.LyricsProvider
import com.metrolist.shared.settings.BlockedContent
import com.metrolist.shared.settings.HiddenHomeItems
import com.metrolist.shared.settings.HiddenLibraryItems
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.state.SongLikeState
import com.metrolist.shared.ui.settings.OpenRouterAuthController
import com.metrolist.shared.ui.settings.SettingsChoice
import com.metrolist.shared.ui.settings.SettingsControl
import com.metrolist.shared.ui.settings.SettingsItemRegistration
import com.metrolist.shared.ui.settings.SettingsPageId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Settings pages that only set up the stock screens (their theme, player and mini player styles,
 * navigation tabs, fonts) or that are off in this project (updates). The XMB leaves them out; its
 * own Theme Settings set its look.
 */
val XMB_HIDDEN_SETTINGS_PAGES: Set<SettingsPageId> =
    setOf(
        SettingsPageId.APPEARANCE,
        SettingsPageId.APPEARANCE_NAVIGATION,
        SettingsPageId.PLAYER_STYLE,
        SettingsPageId.PLAYER_ARTWORK,
        SettingsPageId.MINI_PLAYER_STYLE,
        SettingsPageId.FONTS,
        SettingsPageId.UPDATES,
        SettingsPageId.UPDATE_DETAILS,
        // The AI setup wizard's steps are all on the AI translation page.
        SettingsPageId.LYRICS_AI_SETUP,
        // A developer benchmark with its own screen.
        SettingsPageId.PLAYBACK_BENCHMARK,
    )

/**
 * The XMB's versions of the settings rows the registry draws with its own widgets
 * ([SettingsControl.CUSTOM]): the lyrics offset as a slider, the lyrics sources as one row each,
 * the AI provider and models as choices and text, OpenRouter's browser sign-in, the connection
 * test, and hidden or blocked items as rows that restore them. Rows that only preview something
 * for the stock screens (the Discord card, theme colors) have no XMB version and are left out.
 * Returns the rows to show in place of [item]; empty to leave it out.
 */
@Composable
internal fun xmbCustomRows(
    item: SettingsItemRegistration,
    hooks: XmbSystemHooks,
): List<SettingsItemRegistration> {
    val settings = Settings.shared
    val id = item.id
    return when {
        id == "globalLyricsOffset" -> {
            listOf(lyricsOffsetRow(item))
        }

        id == "providerOrder" -> {
            lyricsProviderRows()
        }

        id == "provider" -> {
            listOf(aiProviderRow(item))
        }

        id == "openRouterSignIn" -> {
            openRouterRows(item)
        }

        id == "translationModel" -> {
            listOf(modelRow(item, settings.lyricsAiModel, allowBlank = false) { settings.lyricsAiModel = it })
        }

        id == "detectorModel" -> {
            listOf(
                modelRow(item, settings.lyricsAiDetectorModel, allowBlank = true) {
                    settings.lyricsAiDetectorModel =
                        it
                },
            )
        }

        id == "aiConnectionTest" -> {
            listOf(connectionTestRow(item, hooks))
        }

        id.startsWith("hiddenHome:") -> {
            listOfNotNull(hiddenHomeRow(item))
        }

        id.startsWith("hidden:") -> {
            listOfNotNull(hiddenLibraryRow(item))
        }

        id.startsWith("blockedArtist:") -> {
            listOf(unblockArtistRow(item))
        }

        id.startsWith("blockedSong:") -> {
            listOf(unblockSongRow(item))
        }

        else -> {
            emptyList()
        }
    }
}

@Composable
private fun lyricsOffsetRow(item: SettingsItemRegistration): SettingsItemRegistration {
    val settings = Settings.shared
    val format = stringResource(Res.string.xmb_lyrics_offset_value)
    val label = { ms: Float -> lyricsOffsetLabel(ms.roundToInt(), format) }
    val value = settings.globalLyricsOffsetMs.toFloat().coerceIn(-LYRICS_OFFSET_MS, LYRICS_OFFSET_MS)
    return item.copy(
        control = SettingsControl.SLIDER,
        valueLabel = label(value),
        sliderValue = value,
        sliderRange = -LYRICS_OFFSET_MS..LYRICS_OFFSET_MS,
        sliderSteps = (LYRICS_OFFSET_MS * 2 / LYRICS_OFFSET_STEP_MS).roundToInt() - 1,
        sliderValueFormatter = { label(it) },
        onSliderChanged = { settings.globalLyricsOffsetMs = it.roundToInt() },
        customContent = null,
    )
}

/** "+0.3 s", "-1.25 s", "0 s": the offset, positive when the lyrics show later. */
internal fun lyricsOffsetLabel(
    ms: Int,
    format: String,
): String {
    val sign =
        if (ms > 0) {
            "+"
        } else if (ms < 0) {
            "-"
        } else {
            ""
        }
    val hundredths = (abs(ms) + 5) / 10
    val seconds = hundredths / 100
    val fraction = (hundredths % 100).toString().padStart(2, '0').trimEnd('0')
    return format.replace("%1\$s", if (fraction.isEmpty()) "$sign$seconds" else "$sign$seconds.$fraction")
}

/**
 * The lyrics sources in the order they're tried, one row each: Off, or a place in the order.
 * Moving a source to a place shifts the others; turning one off keeps it in the order.
 */
@Composable
private fun lyricsProviderRows(): List<SettingsItemRegistration> {
    val settings = Settings.shared
    val order = LyricsProvider.parseUserOrderFromStoredCsv(settings.lyricsProviderOrder)
    val enabled = LyricsProvider.parseEnabledFromStoredCsv(settings.lyricsEnabledProviders)
    val off = stringResource(Res.string.xmb_value_off)
    val position = stringResource(Res.string.xmb_lyrics_provider_position)
    val labels = order.associateWith { providerLabel(it) }
    return order.mapIndexed { index, provider ->
        val choices =
            listOf(SettingsChoice("off", off)) +
                order.indices.map { SettingsChoice(it.toString(), position.replace("%1\$d", (it + 1).toString())) }
        val selected = if (provider in enabled) index.toString() else "off"
        SettingsItemRegistration(
            id = "xmb_lyrics_provider_${provider.name}",
            title = labels.getValue(provider),
            control = SettingsControl.CHOICE,
            choices = choices,
            selectedChoiceId = selected,
            valueLabel = choices.first { it.id == selected }.label,
            onChoiceSelected = { choice ->
                val nowEnabled = if (choice == "off") enabled - provider else enabled + provider
                val target = choice.toIntOrNull()
                val reordered =
                    if (target == null) order else order.toMutableList().apply { add(target, removeAt(index)) }
                settings.lyricsProviderOrder = reordered.joinToString(",") { it.name }
                settings.lyricsEnabledProviders =
                    if (nowEnabled.isEmpty()) "NONE" else reordered.filter { it in nowEnabled }.joinToString(",") { it.name }
            },
        )
    }
}

@Composable
private fun providerLabel(provider: LyricsProvider): String =
    when (provider) {
        LyricsProvider.LYPRIO -> stringResource(Res.string.brand_lyprio)
        LyricsProvider.BETTERLYRICS -> stringResource(Res.string.brand_better_lyrics)
        LyricsProvider.LRCRED -> stringResource(Res.string.brand_lrcred)
        LyricsProvider.UNISON -> stringResource(Res.string.brand_unison)
        LyricsProvider.LRCLIB -> stringResource(Res.string.brand_lrclib)
        LyricsProvider.KUGOU -> stringResource(Res.string.brand_kugou)
        LyricsProvider.PAXSENIX -> stringResource(Res.string.brand_paxsenix)
        else -> provider.name
    }

/** The AI provider, applied as the stock picker does: its endpoint and default model come with it. */
private fun aiProviderRow(item: SettingsItemRegistration): SettingsItemRegistration {
    val settings = Settings.shared
    val choices = AiProviderPresets.all.map { SettingsChoice(it.id, it.id) }
    return item.copy(
        control = SettingsControl.CHOICE,
        choices = choices,
        selectedChoiceId = settings.lyricsAiProvider,
        valueLabel = settings.lyricsAiProvider,
        customContent = null,
        onChoiceSelected = { id ->
            val selected = AiProviderPresets.all.firstOrNull { it.id == id } ?: return@copy
            val previous = settings.lyricsAiProvider
            settings.setLyricsAiApiKey(previous, settings.lyricsAiApiKeyFor(previous))
            settings.lyricsAiProvider = selected.id
            if (selected.id != "OpenRouter") settings.lyricsOpenRouterOAuthSession = ""
            if (selected.endpoint.isNotBlank()) settings.lyricsAiBaseUrl = selected.endpoint
            settings.lyricsAiModel = selected.defaultModel
            settings.lyricsAiDetectorModel = ""
            // Direct translators (no AI) translate literally, as the stock picker sets.
            if (selected.id == "DeepL (keyless)" || selected.id == "Google Translate") settings.lyricsAiTranslationMode = "Literal"
        },
    )
}

/**
 * OpenRouter's browser sign-in: Sign In opens the browser; the code it shows is then typed on the
 * keyboard; once signed in, the row signs out.
 */
@Composable
private fun openRouterRows(item: SettingsItemRegistration): List<SettingsItemRegistration> {
    val settings = Settings.shared
    val controller = remember { OpenRouterAuthController(settings) }
    DisposableEffect(controller) { onDispose { controller.dispose() } }
    val signedIn = settings.lyricsOpenRouterOAuthKey.isNotBlank()
    if (signedIn) {
        return listOf(
            item.copy(
                title = stringResource(Res.string.xmb_openrouter_sign_out),
                description = stringResource(Res.string.openrouter_signed_in_description),
                control = SettingsControl.ACTION,
                customContent = null,
                onClick = controller::disconnect,
            ),
        )
    }
    val signIn =
        item.copy(
            description = stringResource(Res.string.openrouter_sign_in_description),
            control = SettingsControl.ACTION,
            enabled = !controller.busy,
            customContent = null,
            onClick = controller::start,
        )
    if (!controller.awaitingCode) return listOf(signIn)
    return listOf(
        signIn,
        SettingsItemRegistration(
            id = "xmb_openrouter_code",
            title = stringResource(Res.string.openrouter_auth_code),
            description = stringResource(Res.string.openrouter_auth_instruction),
            control = SettingsControl.TEXT,
            textValue = "",
            enabled = !controller.busy,
            onTextChanged = { code -> if (code.isNotBlank()) controller.exchange(code) },
        ),
    )
}

/** A model name, typed on the keyboard ([allowBlank]: blank means the provider's default). */
private fun modelRow(
    item: SettingsItemRegistration,
    model: String,
    allowBlank: Boolean,
    set: (String) -> Unit,
) = item.copy(
    control = SettingsControl.TEXT,
    textValue = model,
    customContent = null,
    onTextChanged = { text -> if (allowBlank || text.isNotBlank()) set(text.trim()) },
)

/** Runs the stock connection test and reports the result as a toast. */
@Composable
private fun connectionTestRow(
    item: SettingsItemRegistration,
    hooks: XmbSystemHooks,
): SettingsItemRegistration {
    val settings = Settings.shared
    val scope = rememberCoroutineScope()
    val passed = stringResource(Res.string.xmb_ai_test_passed)
    val failed = stringResource(Res.string.ai_test_connection_failed)
    val missing = stringResource(Res.string.ai_test_missing_credentials)
    return item.copy(
        control = SettingsControl.ACTION,
        customContent = null,
        onClick = {
            val provider = settings.lyricsAiProvider
            val (apiKey, baseUrl) = settings.lyricsAiCredentials(provider)
            val canRun =
                when (provider) {
                    "DeepL" -> settings.lyricsDeepLApiKey.isNotBlank()
                    "DeepL (keyless)", "Google Translate" -> true
                    else -> settings.lyricsOpenRouterOAuthKey.isNotBlank() || apiKey.isNotBlank()
                }
            if (!canRun) {
                hooks.toast(missing)
            } else {
                scope.launch {
                    val ok =
                        try {
                            KoinPlatform
                                .getKoin()
                                .get<LyricsTranslationClient>()
                                .probeLyricsAiConnection(
                                    provider = provider,
                                    targetLanguage = settings.lyricsAiTargetLanguage,
                                    translationMode = settings.lyricsAiTranslationMode,
                                    activationMode = settings.lyricsAiActivationMode,
                                    apiKey = apiKey,
                                    baseUrl = baseUrl,
                                    translationModel = settings.lyricsAiModel,
                                    detectorModel = settings.lyricsAiDetectorModel,
                                    systemPrompt = settings.lyricsAiSystemPrompt,
                                    deepLApiKey = settings.lyricsDeepLApiKey,
                                    deepLFormality = settings.lyricsDeepLFormality,
                                )
                            true
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            false
                        }
                    hooks.toast(if (ok) passed else failed)
                }
            }
        },
    )
}

@Composable
private fun hiddenHomeRow(item: SettingsItemRegistration): SettingsItemRegistration? {
    val hidden = HiddenHomeItems.all().firstOrNull { "hiddenHome:${it.type}:${it.id}" == item.id } ?: return null
    return restoreRow(item, stringResource(Res.string.restore_hidden_item_message).replace("%1\$s", hidden.title)) {
        HiddenHomeItems.restore(hidden.key)
    }
}

@Composable
private fun hiddenLibraryRow(item: SettingsItemRegistration): SettingsItemRegistration? {
    val hidden = HiddenLibraryItems.all().firstOrNull { "hidden:${it.type}:${it.id}" == item.id } ?: return null
    return restoreRow(item, stringResource(Res.string.restore_hidden_item_message).replace("%1\$s", hidden.title)) {
        HiddenLibraryItems.restore(hidden.key)
    }
}

@Composable
private fun unblockArtistRow(item: SettingsItemRegistration): SettingsItemRegistration {
    val artistId = item.id.removePrefix("blockedArtist:")
    return restoreRow(item, stringResource(Res.string.unblock_artist_message).replace("%1\$s", item.title)) {
        BlockedContent.unblockArtist(artistId)
    }
}

@Composable
private fun unblockSongRow(item: SettingsItemRegistration): SettingsItemRegistration {
    val songId = item.id.removePrefix("blockedSong:")
    return restoreRow(item, stringResource(Res.string.restore_hidden_item_message).replace("%1\$s", item.title)) {
        if (!SongLikeState.removeDislike(songId)) BlockedContent.unblockSong(songId)
    }
}

/** A hidden or blocked item: selecting it asks, then shows or allows it again. */
@Composable
private fun restoreRow(
    item: SettingsItemRegistration,
    question: String,
    restore: () -> Unit,
) = item.copy(
    control = SettingsControl.ACTION,
    valueLabel = stringResource(Res.string.xmb_value_hidden),
    confirmationMessage = question,
    customContent = null,
    onClick = restore,
)

private const val LYRICS_OFFSET_MS = 5_000f
private const val LYRICS_OFFSET_STEP_MS = 50f
