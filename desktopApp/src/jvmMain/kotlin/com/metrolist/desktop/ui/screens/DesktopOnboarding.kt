@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.backup.DesktopBackupRestoreManager
import com.metrolist.desktop.state.DesktopLocaleManager
import com.metrolist.desktop.state.appLanguageChoices
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.app_name
import com.metrolist.shared.generated.resources.back
import com.metrolist.shared.generated.resources.continue_action
import com.metrolist.shared.generated.resources.finish
import com.metrolist.shared.generated.resources.log_in
import com.metrolist.shared.generated.resources.logo
import com.metrolist.shared.generated.resources.no
import com.metrolist.shared.generated.resources.onboarding_backup_picker_error
import com.metrolist.shared.generated.resources.onboarding_backup_picker_title
import com.metrolist.shared.generated.resources.onboarding_backup_restore_failed
import com.metrolist.shared.generated.resources.onboarding_begin_setup
import com.metrolist.shared.generated.resources.onboarding_crash_reports_description
import com.metrolist.shared.generated.resources.onboarding_crash_reports_title
import com.metrolist.shared.generated.resources.onboarding_finish_description
import com.metrolist.shared.generated.resources.onboarding_finish_title
import com.metrolist.shared.generated.resources.onboarding_language_lyrics_description
import com.metrolist.shared.generated.resources.onboarding_playback_description
import com.metrolist.shared.generated.resources.onboarding_sentry_diagnostics
import com.metrolist.shared.generated.resources.onboarding_sentry_opt_in
import com.metrolist.shared.generated.resources.onboarding_sentry_private
import com.metrolist.shared.generated.resources.onboarding_settings_hint
import com.metrolist.shared.generated.resources.onboarding_step_of
import com.metrolist.shared.generated.resources.onboarding_welcome_privacy_description
import com.metrolist.shared.generated.resources.privacy_policy
import com.metrolist.shared.generated.resources.restore_from_backup
import com.metrolist.shared.generated.resources.settings_localization
import com.metrolist.shared.generated.resources.settings_playback
import com.metrolist.shared.generated.resources.side_menu_welcome
import com.metrolist.shared.generated.resources.yes
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.components.ContainedLoadingIndicator
import com.metrolist.shared.ui.components.GroupedListItem
import com.metrolist.shared.ui.icons.MetrolistIcons
import com.metrolist.shared.ui.onboarding.OnboardingPage
import com.metrolist.shared.ui.onboarding.OnboardingPageContent
import com.metrolist.shared.ui.onboarding.OnboardingPreferences
import com.metrolist.shared.ui.onboarding.OnboardingTransition
import com.metrolist.shared.ui.settings.SettingsHostChoice
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.ui.settings.SettingsPlatform
import com.metrolist.shared.ui.settings.SharedSettingsHost
import com.metrolist.shared.ui.utils.verticalScrollWithDrag
import com.metrolist.shared.utils.isCrashReportingAvailable
import com.metrolist.shared.xmb.screens.XmbOnboarding
import com.metrolist.shared.xmb.screens.XmbOnboardingAction
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopOnboarding(onComplete: (sendCrashReports: Boolean, openLogin: Boolean) -> Unit) {
    val pages =
        remember {
            buildList {
                add(OnboardingPage.WELCOME)
                if (isCrashReportingAvailable()) add(OnboardingPage.CRASH_REPORTS)
                // XMB Music: language, lyrics and playback are in the XMB's Settings column.
                add(OnboardingPage.FINISH)
            }
        }
    var pageIndex by rememberSaveable { mutableStateOf(0) }
    var sendCrashReports by rememberSaveable { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val restorePickerTitle = stringResource(Res.string.onboarding_backup_picker_title)

    fun nextPage() {
        error = null
        if (pageIndex < pages.lastIndex) {
            pageIndex++
        } else {
            onComplete(sendCrashReports, false)
        }
    }

    fun restoreBackup() {
        if (isWorking) return
        isWorking = true
        scope.launch {
            try {
                error = null
                val selected =
                    runCatching {
                        FileKit.openFilePicker(
                            type = FileKitType.File(listOf("zip", "backup")),
                            dialogSettings = FileKitDialogSettings(title = restorePickerTitle),
                        )
                    }.getOrElse {
                        if (it is CancellationException) throw it
                        error = getString(Res.string.onboarding_backup_picker_error, it.message ?: "")
                        return@launch
                    } ?: return@launch

                withContext(Dispatchers.IO) {
                    DesktopBackupRestoreManager.stageRestoreAndRestart(selected.file) {
                        Settings.shared.desktopOnboardingComplete = true
                        if (isCrashReportingAvailable()) {
                            Settings.shared.desktopPendingOnboardingSentryConsent = sendCrashReports.toString()
                        }
                    }
                }.onFailure {
                    Settings.shared.desktopOnboardingComplete = false
                    Settings.shared.desktopPendingOnboardingSentryConsent = ""
                    error = getString(Res.string.onboarding_backup_restore_failed, it.message ?: "")
                }
            } finally {
                isWorking = false
            }
        }
    }

    @Composable
    fun pageContent(index: Int): OnboardingPageContent {
        val page = pages[index]
        return OnboardingPageContent(
            page = page,
            title =
                when (page) {
                    OnboardingPage.WELCOME -> stringResource(Res.string.side_menu_welcome)
                    OnboardingPage.CRASH_REPORTS -> stringResource(Res.string.onboarding_crash_reports_title)
                    OnboardingPage.LANGUAGE_LYRICS -> stringResource(Res.string.settings_localization)
                    OnboardingPage.PLAYBACK -> stringResource(Res.string.settings_playback)
                    OnboardingPage.FINISH -> stringResource(Res.string.onboarding_finish_title)
                    else -> error("Unsupported desktop onboarding page")
                },
            description =
                when (page) {
                    OnboardingPage.WELCOME -> {
                        stringResource(Res.string.onboarding_welcome_privacy_description)
                    }

                    OnboardingPage.CRASH_REPORTS -> {
                        stringResource(Res.string.onboarding_crash_reports_description)
                    }

                    OnboardingPage.LANGUAGE_LYRICS -> {
                        stringResource(Res.string.onboarding_language_lyrics_description)
                    }

                    OnboardingPage.PLAYBACK -> {
                        stringResource(Res.string.onboarding_playback_description)
                    }

                    OnboardingPage.FINISH -> {
                        stringResource(Res.string.onboarding_finish_description)
                    }
                },
            icon =
                when (page) {
                    OnboardingPage.WELCOME -> MetrolistIcons.Outlined.MusicNote
                    OnboardingPage.CRASH_REPORTS -> MetrolistIcons.Outlined.Security
                    OnboardingPage.LANGUAGE_LYRICS -> MetrolistIcons.Outlined.Language
                    OnboardingPage.PLAYBACK -> MetrolistIcons.Outlined.PlayArrow
                    OnboardingPage.FINISH -> MetrolistIcons.Filled.Check
                },
            step = index + 1,
            stepCount = pages.size,
            primaryLabel =
                when (page) {
                    OnboardingPage.WELCOME -> stringResource(Res.string.onboarding_begin_setup)
                    OnboardingPage.CRASH_REPORTS -> stringResource(Res.string.yes)
                    OnboardingPage.LANGUAGE_LYRICS, OnboardingPage.PLAYBACK -> stringResource(Res.string.continue_action)
                    OnboardingPage.FINISH -> stringResource(Res.string.finish)
                },
            secondaryLabel = if (page == OnboardingPage.CRASH_REPORTS) stringResource(Res.string.no) else null,
            isWorking = isWorking,
            error = error.takeIf { index == pageIndex },
            backLabel = stringResource(Res.string.back),
            onPrimary = {
                if (page == OnboardingPage.CRASH_REPORTS) sendCrashReports = true
                nextPage()
            },
            onSecondary =
                if (page == OnboardingPage.CRASH_REPORTS) {
                    {
                        sendCrashReports = false
                        nextPage()
                    }
                } else {
                    null
                },
            onBack =
                if (index > 0) {
                    {
                        error = null
                        pageIndex--
                    }
                } else {
                    null
                },
        )
    }
    val content = pageContent(pageIndex)

    // XMB Music: the pages draw the XMB way.
    XmbOnboarding(
        content = content,
        actions =
            if (content.page == OnboardingPage.FINISH && !isWorking) {
                listOf(
                    XmbOnboardingAction(stringResource(Res.string.log_in)) { onComplete(sendCrashReports, true) },
                    XmbOnboardingAction(stringResource(Res.string.restore_from_backup), ::restoreBackup),
                )
            } else {
                emptyList()
            },
    )
}
