package com.metrolist.desktop.xmb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.metrolist.desktop.state.DesktopLocaleManager
import com.metrolist.desktop.state.app.UiState
import com.metrolist.desktop.state.appLanguageChoices
import com.metrolist.desktop.state.auth.AuthState
import com.metrolist.desktop.state.content.ArtistState
import com.metrolist.desktop.state.content.HomeState
import com.metrolist.desktop.state.content.SearchState
import com.metrolist.desktop.state.playback.ListenTogetherState
import com.metrolist.desktop.state.playback.PlayerState
import com.metrolist.desktop.utils.AppLogger
import com.metrolist.desktop.utils.SentryManager
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.app_language_system
import com.metrolist.shared.generated.resources.device_language
import com.metrolist.shared.generated.resources.settings_artist_source_custom
import com.metrolist.shared.generated.resources.settings_artist_source_itunes
import com.metrolist.shared.generated.resources.settings_artist_source_lastfm
import com.metrolist.shared.generated.resources.settings_artist_source_youtube
import com.metrolist.shared.generated.resources.settings_playback_speed_locked_listen_together
import com.metrolist.shared.model.ArtistSource
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.state.GlobalYouTube
import com.metrolist.shared.state.applyContentRegion
import com.metrolist.shared.state.contentCountryCodes
import com.metrolist.shared.ui.settings.SettingsChangeEffect
import com.metrolist.shared.ui.settings.SettingsChoice
import com.metrolist.shared.ui.settings.SettingsHostChoice
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.ui.settings.SettingsPlatform
import com.metrolist.shared.ui.settings.SharedSettingsHost
import com.metrolist.shared.ui.settings.settingsHomeSections
import org.jetbrains.compose.resources.stringResource
import java.util.Locale

/**
 * The settings registry's host for the XMB on desktop. It mirrors the registry-facing parts of
 * the stock `SettingsOverlay` host (choices, change effects, Listen Together's speed lock); pages
 * whose content the stock overlay draws itself (storage, backup, updates, about, devtools, fonts,
 * MetroEQ) open that overlay instead. Exhaustive `when`s over the shared enums make an upstream
 * addition fail to compile here, so it can't be missed on merge.
 */
@Composable
internal fun rememberDesktopXmbSettingsHost(): SharedSettingsHost {
    val isInListenTogetherRoom by ListenTogetherState.listenTogetherManager.isInRoom
        .collectAsState()
    val systemDefaultLabel = stringResource(Res.string.app_language_system)
    val systemLocale = Locale.getDefault()
    val appLanguageChoices = appLanguageChoices()
    val deviceLanguageLabel = stringResource(Res.string.device_language)
    val lyricsTargetLanguageChoices =
        listOf(SettingsChoice("device", deviceLanguageLabel)) + appLanguageChoices.filterNot { it.id == GlobalYouTube.SYSTEM }
    val artistSourceChoices =
        ArtistSource.entries.map { source ->
            SettingsChoice(
                id = source.name,
                label =
                    stringResource(
                        when (source) {
                            ArtistSource.YOUTUBE -> Res.string.settings_artist_source_youtube
                            ArtistSource.ITUNES -> Res.string.settings_artist_source_itunes
                            ArtistSource.LASTFM -> Res.string.settings_artist_source_lastfm
                            ArtistSource.CUSTOM -> Res.string.settings_artist_source_custom
                        },
                    ),
            )
        }
    val contentCountryChoices =
        listOf(SettingsChoice(GlobalYouTube.SYSTEM, systemDefaultLabel)) +
            contentCountryCodes
                .map { code ->
                    SettingsChoice(
                        code,
                        Locale
                            .Builder()
                            .setRegion(code)
                            .build()
                            .getDisplayCountry(systemLocale)
                            .ifBlank { code },
                    )
                }.sortedBy(SettingsChoice::label)
    val lockedSpeed = stringResource(Res.string.settings_playback_speed_locked_listen_together)
    return SharedSettingsHost(
        platform = SettingsPlatform.DESKTOP,
        onClose = {},
        availablePages = xmbDesktopSettingsPages(Settings.shared.devtoolsUnlocked),
        onSwitchYouTubeChannel = if (AuthState.isSignedIn) ({ UiState.showSwitchChannel = true }) else null,
        onSettingChanged = { effect, value ->
            when (effect) {
                SettingsChangeEffect.SENTRY -> value?.let(SentryManager::setEnabled)

                SettingsChangeEffect.RESTART -> UiState.restartRequested = true

                SettingsChangeEffect.LOGGING -> value?.let(AppLogger::setEnabled)

                SettingsChangeEffect.VIDEO_PLAYBACK -> if (value == false) PlayerState.closeVideoViewer()

                SettingsChangeEffect.AUDIO,
                SettingsChangeEffect.CONTENT,
                SettingsChangeEffect.AUTO_DOWNLOAD_ON_LIKE,
                SettingsChangeEffect.PLAYBACK_SPEED,
                SettingsChangeEffect.PLAYBACK_PITCH,
                -> Unit
            }
        },
        onFloatSettingChanged = { effect, value ->
            when (effect) {
                SettingsChangeEffect.PLAYBACK_SPEED -> PlayerState.setPlaybackSpeed(value)
                SettingsChangeEffect.PLAYBACK_PITCH -> PlayerState.setPlaybackPitch(value)
                else -> Unit
            }
        },
        hostChoices =
            mapOf(
                SettingsHostChoice.CONTENT_LANGUAGE to appLanguageChoices,
                SettingsHostChoice.CONTENT_COUNTRY to contentCountryChoices,
                SettingsHostChoice.APP_LANGUAGE to appLanguageChoices,
                SettingsHostChoice.LYRICS_TARGET_LANGUAGE to lyricsTargetLanguageChoices,
                SettingsHostChoice.ARTIST_ICON_SOURCE to artistSourceChoices,
                SettingsHostChoice.ARTIST_BANNER_SOURCE to artistSourceChoices,
            ),
        onHostChoiceSelected = { choice, value ->
            when (choice) {
                SettingsHostChoice.CONTENT_LANGUAGE -> {
                    Settings.shared.contentLanguage = value
                }

                SettingsHostChoice.CONTENT_COUNTRY -> {
                    Settings.shared.contentCountry = value
                }

                SettingsHostChoice.APP_LANGUAGE -> {
                    Settings.shared.appLanguage = value
                    UiState.restartRequested = true
                }

                SettingsHostChoice.LYRICS_TARGET_LANGUAGE -> {
                    Settings.shared.lyricsAiTargetLanguage = value
                }

                SettingsHostChoice.APP_ICON -> {
                    Unit
                }

                SettingsHostChoice.ARTIST_ICON_SOURCE -> {
                    ArtistState.updateArtistIconSource(ArtistSource.valueOf(value))
                }

                SettingsHostChoice.ARTIST_BANNER_SOURCE -> {
                    ArtistState.updateArtistBannerSource(ArtistSource.valueOf(value))
                }
            }
            if (choice == SettingsHostChoice.CONTENT_LANGUAGE || choice == SettingsHostChoice.CONTENT_COUNTRY) {
                GlobalYouTube.applyContentRegion(
                    contentCountry = Settings.shared.contentCountry,
                    contentLanguage = Settings.shared.contentLanguage,
                    systemLocale = DesktopLocaleManager.systemLocale,
                )
                HomeState.fetchHomeData()
            }
        },
        playbackSpeedEnabled = !isInListenTogetherRoom,
        playbackSpeedOverride = 1f.takeIf { isInListenTogetherRoom },
        playbackSpeedDescription = lockedSpeed.takeIf { isInListenTogetherRoom },
        homeSections = settingsHomeSections(HomeState.uiState),
        onHomeSectionVisibilityChanged = { section, visible -> Settings.shared.toggleHomeSectionVisibility(section, visible) },
        onClearSearchHistory = SearchState::clearSearchHistory,
    )
}

/**
 * The pages the stock desktop overlay offers, plus MetroEQ (the shared settings screen adds it).
 * Custom pages show as items that open the stock overlay; Fonts has no actions here, so the
 * registry leaves its page out and its row opens the stock overlay too.
 */
internal fun xmbDesktopSettingsPages(devtoolsUnlocked: Boolean): Set<SettingsPageId> =
    buildSet {
        add(SettingsPageId.STORAGE)
        add(SettingsPageId.BACKUP_RESTORE)
        add(SettingsPageId.UPDATES)
        add(SettingsPageId.ABOUT)
        if (devtoolsUnlocked) add(SettingsPageId.DEVTOOLS)
        add(SettingsPageId.EQUALIZER)
        addAll(
            listOf(
                SettingsPageId.FONTS,
                SettingsPageId.HIDDEN_HOME_ITEMS,
                SettingsPageId.HIDDEN_LIBRARY_ITEMS,
                SettingsPageId.BLOCKED_ARTISTS,
                SettingsPageId.BLOCKED_SONGS,
                SettingsPageId.LYRICS_SOURCES,
                SettingsPageId.LYRICS_AI_TRANSLATION,
                SettingsPageId.LYRICS_ROMANIZATION,
                SettingsPageId.PLAYER_ARTWORK,
                SettingsPageId.PLAYER_STYLE,
            ),
        )
    }
