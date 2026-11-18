package com.metrolist.shared.xmb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.cancel
import com.metrolist.shared.generated.resources.close
import com.metrolist.shared.generated.resources.ok
import com.metrolist.shared.generated.resources.play
import com.metrolist.shared.generated.resources.recognition_error
import com.metrolist.shared.generated.resources.recognition_listening_hint
import com.metrolist.shared.generated.resources.recognition_match_found
import com.metrolist.shared.generated.resources.recognition_notification_no_match
import com.metrolist.shared.generated.resources.recognize_music
import com.metrolist.shared.generated.resources.xmb_recognition_identifying
import com.metrolist.shared.recognition.RecognitionStatus
import com.metrolist.shared.recognition.toSongItem
import com.metrolist.shared.ui.playback.PlaybackController
import com.metrolist.shared.xmb.nav.XmbNotice
import com.metrolist.shared.xmb.nav.XmbNoticeRequest
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

/** Identifying the song playing nearby through the microphone (Android). */
interface XmbRecognition {
    val status: StateFlow<RecognitionStatus>

    /** Listens, asking for the microphone first when needed. */
    fun start()

    /** Stops listening, or dismisses the result. */
    fun reset()
}

/**
 * Identify Song as an XMB notice: listening (Cancel), identifying, then the match with Play, or
 * why nothing matched. Null while nothing is going on.
 */
@Composable
internal fun rememberXmbRecognitionNotice(
    recognition: XmbRecognition?,
    playback: PlaybackController?,
): XmbNoticeRequest? {
    recognition ?: return null
    val status by recognition.status.collectAsState()
    val title = stringResource(Res.string.recognize_music)
    val cancel = stringResource(Res.string.cancel)
    val ok = stringResource(Res.string.ok)
    return when (val current = status) {
        RecognitionStatus.Ready -> {
            null
        }

        RecognitionStatus.Listening, RecognitionStatus.Processing -> {
            val message =
                stringResource(
                    if (current ==
                        RecognitionStatus.Listening
                    ) {
                        Res.string.recognition_listening_hint
                    } else {
                        Res.string.xmb_recognition_identifying
                    },
                )
            XmbNoticeRequest(
                XmbNotice(id = "recognition:$current", title = title, message = message, buttons = listOf(cancel), cancel = 0),
            ) {
                recognition.reset()
            }
        }

        is RecognitionStatus.Success -> {
            val result = current.result
            val song = result.toSongItem()
            val play = stringResource(Res.string.play)
            val close = stringResource(Res.string.close)
            XmbNoticeRequest(
                XmbNotice(
                    id = "recognition:${result.trackId}:${song != null}",
                    title = stringResource(Res.string.recognition_match_found),
                    message = "${result.title}\n${result.artist}",
                    details = result.album,
                    buttons = if (song != null && playback != null) listOf(play, close) else listOf(close),
                    cancel = if (song != null && playback != null) 1 else 0,
                ),
            ) { chosen ->
                if (chosen == 0 && song != null && playback != null) playback.play(song, listOf(song))
                recognition.reset()
            }
        }

        is RecognitionStatus.NoMatch -> {
            XmbNoticeRequest(
                XmbNotice(
                    id = "recognition:no_match",
                    title = title,
                    message = current.message.ifBlank { stringResource(Res.string.recognition_notification_no_match) },
                    buttons = listOf(ok),
                    cancel = 0,
                ),
            ) { recognition.reset() }
        }

        is RecognitionStatus.Error -> {
            XmbNoticeRequest(
                XmbNotice(
                    id = "recognition:error",
                    title = stringResource(Res.string.recognition_error),
                    message = current.message,
                    buttons = listOf(ok),
                    cancel = 0,
                ),
            ) { recognition.reset() }
        }
    }
}
