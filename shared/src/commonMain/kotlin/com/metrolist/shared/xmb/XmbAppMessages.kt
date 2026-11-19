@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.metrolist.shared.xmb

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.metrolist.shared.api.lyrics.LyricsTranslationFailure
import com.metrolist.shared.auth.YouTubeSessionState
import com.metrolist.shared.auth.YouTubeSyncState
import com.metrolist.shared.generated.resources.*
import com.metrolist.shared.listentogether.ListenTogetherEvent
import com.metrolist.shared.listentogether.ListenTogetherManager
import com.metrolist.shared.listentogether.ListenTogetherText
import com.metrolist.shared.settings.HiddenHomeItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * The app's background messages, posted to [snackbars] (which the XMB shows as toasts): a YouTube
 * sync that failed (retried with the action), an item hidden from Home (undone with the action),
 * a lyrics translation that failed, and Listen Together's host changes.
 */
@Composable
fun XmbAppMessages(
    snackbars: SnackbarHostState,
    lyricsFailures: Flow<LyricsTranslationFailure>? = null,
    listenTogether: ListenTogetherManager? = null,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(snackbars) {
        YouTubeSyncState.failures.map { it.values.firstOrNull() }.distinctUntilChanged().collectLatest { current ->
            val failure = current ?: return@collectLatest
            if (YouTubeSessionState.showExpiredSessionNotice) return@collectLatest
            val result =
                snackbars.showSnackbar(
                    message = getString(Res.string.youtube_sync_failed),
                    actionLabel = getString(Res.string.try_again),
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
            if (result == SnackbarResult.ActionPerformed) {
                scope.launch {
                    YouTubeSyncState.succeeded(failure.operation)
                    failure.retry()
                }
            }
        }
    }
    LaunchedEffect(snackbars) {
        HiddenHomeItems.hidden.collectLatest { item ->
            val result =
                snackbars.showSnackbar(
                    message = getString(Res.string.hidden_from_home, item.title),
                    actionLabel = getString(Res.string.undo),
                    duration = SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) HiddenHomeItems.restore(item.key)
        }
    }
    if (lyricsFailures != null) {
        LaunchedEffect(snackbars, lyricsFailures) {
            lyricsFailures.collect { snackbars.showSnackbar(getString(it.messageResource()), duration = SnackbarDuration.Short) }
        }
    }
    if (listenTogether != null) {
        val autoApproved by rememberUpdatedState(stringResource(Res.string.listen_together_suggestion_auto_approved))
        val hostChanged by rememberUpdatedState(stringResource(Res.string.listen_together_log_host_changed))
        val becameHost by rememberUpdatedState(stringResource(Res.string.listen_together_became_host))
        LaunchedEffect(listenTogether) {
            listenTogether.events.collect { event ->
                val message =
                    when {
                        event is ListenTogetherEvent.SuggestionAutoApproved -> autoApproved
                        event is ListenTogetherEvent.Notice && event.key == ListenTogetherText.HOST_CHANGED -> hostChanged
                        event is ListenTogetherEvent.Notice && event.key == ListenTogetherText.BECAME_HOST -> becameHost
                        else -> null
                    }
                if (message != null) snackbars.showSnackbar(message)
            }
        }
    }
}

private fun LyricsTranslationFailure.messageResource(): StringResource =
    when (this) {
        LyricsTranslationFailure.DATA_POLICY -> Res.string.ai_translation_failed_privacy
        LyricsTranslationFailure.AUTHENTICATION -> Res.string.ai_translation_failed_key
        LyricsTranslationFailure.CREDIT_LIMIT -> Res.string.ai_translation_failed_credit
        LyricsTranslationFailure.RATE_LIMIT -> Res.string.ai_translation_failed_rate
        LyricsTranslationFailure.FREE_DAILY_LIMIT -> Res.string.ai_translation_failed_daily
        LyricsTranslationFailure.FREE_MINUTE_LIMIT -> Res.string.ai_translation_failed_minute
        LyricsTranslationFailure.MODEL_BUSY -> Res.string.ai_translation_failed_busy
        LyricsTranslationFailure.MODEL_UNAVAILABLE -> Res.string.ai_translation_failed_model
        LyricsTranslationFailure.NETWORK, LyricsTranslationFailure.TIMEOUT -> Res.string.ai_translation_failed_network
        LyricsTranslationFailure.EMPTY_RESPONSE -> Res.string.ai_translation_failed_empty
        LyricsTranslationFailure.INVALID_RESPONSE -> Res.string.ai_translation_failed_invalid
        LyricsTranslationFailure.INCOMPLETE_RESPONSE -> Res.string.ai_translation_failed_incomplete
        LyricsTranslationFailure.REQUEST_FAILED -> Res.string.ai_translation_failed
    }
