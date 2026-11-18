package com.metrolist.android.xmb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.metrolist.android.listentogether.AndroidListenTogetherManager
import com.metrolist.android.playback.ListenTogetherPlaybackRequest
import com.metrolist.android.playback.PlayerConnection
import com.metrolist.shared.auth.YouTubeSessionState
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.action_login
import com.metrolist.shared.generated.resources.action_not_now
import com.metrolist.shared.generated.resources.cancel
import com.metrolist.shared.generated.resources.listen_together_guest_choice_message
import com.metrolist.shared.generated.resources.listen_together_guest_choice_title
import com.metrolist.shared.generated.resources.listen_together_guest_queue
import com.metrolist.shared.generated.resources.listen_together_guest_solo
import com.metrolist.shared.generated.resources.youtube_session_expired_message
import com.metrolist.shared.generated.resources.youtube_session_expired_title
import com.metrolist.shared.xmb.nav.XmbNotice
import com.metrolist.shared.xmb.nav.XmbNoticeRequest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Notices the stock layout showed as dialogs, as XMB notices: a guest in a Listen Together room
 * choosing what playing a song does, and an expired YouTube sign-in.
 */
@Composable
internal fun rememberAndroidXmbNotice(
    playerConnection: PlayerConnection,
    signIn: () -> Unit,
): XmbNoticeRequest? {
    var request by remember { mutableStateOf<ListenTogetherPlaybackRequest?>(null) }
    LaunchedEffect(playerConnection) { playerConnection.listenTogetherPlaybackRequests.collect { request = it } }
    val scope = rememberCoroutineScope()
    val guestTitle = stringResource(Res.string.listen_together_guest_choice_title)
    val guestMessage = stringResource(Res.string.listen_together_guest_choice_message)
    val queue = stringResource(Res.string.listen_together_guest_queue)
    val solo = stringResource(Res.string.listen_together_guest_solo)
    val cancel = stringResource(Res.string.cancel)
    val expiredTitle = stringResource(Res.string.youtube_session_expired_title)
    val expiredMessage = stringResource(Res.string.youtube_session_expired_message)
    val logIn = stringResource(Res.string.action_login)
    val notNow = stringResource(Res.string.action_not_now)
    val pending = request
    return when {
        pending != null -> {
            XmbNoticeRequest(
                XmbNotice(
                    id = "listen_together_guest:${pending.song.id}",
                    title = guestTitle,
                    message = guestMessage,
                    buttons = listOf(queue, solo, cancel),
                    cancel = 2,
                ),
            ) { chosen ->
                request = null
                when (chosen) {
                    0 -> {
                        AndroidListenTogetherManager.suggestTrack(pending.song)
                    }

                    1 -> {
                        scope.launch {
                            // As the stock dialog: leave the room, then play what was asked for.
                            if (AndroidListenTogetherManager.leaveRoomAndWait()) {
                                if (pending.queue.isNotEmpty()) {
                                    playerConnection.playQueue(
                                        pending.queue,
                                        pending.startIndex,
                                        pending.queueTitle,
                                        shuffleModeEnabled = pending.shuffleModeEnabled,
                                    )
                                } else {
                                    playerConnection.playSong(pending.song, "")
                                }
                            }
                        }
                    }
                }
            }
        }

        YouTubeSessionState.showExpiredSessionNotice -> {
            XmbNoticeRequest(
                XmbNotice(
                    id = "session_expired",
                    title = expiredTitle,
                    message = expiredMessage,
                    buttons = listOf(logIn, notNow),
                    cancel = 1,
                ),
            ) { chosen ->
                YouTubeSessionState.dismissExpiredSessionNotice()
                if (chosen == 0) signIn()
            }
        }

        else -> {
            null
        }
    }
}
