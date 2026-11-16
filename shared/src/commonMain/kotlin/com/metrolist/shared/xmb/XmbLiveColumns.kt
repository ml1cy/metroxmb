package com.metrolist.shared.xmb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.loading
import com.metrolist.shared.generated.resources.xmb_cast_connected
import com.metrolist.shared.generated.resources.xmb_cast_connecting
import com.metrolist.shared.generated.resources.xmb_friends_approve
import com.metrolist.shared.generated.resources.xmb_friends_host
import com.metrolist.shared.generated.resources.xmb_friends_leave_confirm
import com.metrolist.shared.generated.resources.xmb_friends_name
import com.metrolist.shared.generated.resources.xmb_friends_reject
import com.metrolist.shared.generated.resources.xmb_friends_room_code
import com.metrolist.shared.generated.resources.xmb_lyrics_none
import com.metrolist.shared.generated.resources.xmb_lyrics_nothing_playing
import com.metrolist.shared.listentogether.ListenTogetherRoomRole
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.xmb.nav.XmbFriendsInfo
import org.jetbrains.compose.resources.stringResource

/** Listen Together's state as the Friends column needs it; the manager's flows and the setting are observed. */
@Composable
internal fun rememberXmbFriendsInfo(together: XmbListenTogether): XmbFriendsInfo {
    val manager = together.manager
    val room by manager.roomState.collectAsState()
    val role by manager.role.collectAsState()
    val name by manager.username.collectAsState()
    val pending by manager.joinRequestPending.collectAsState()
    val submitted by manager.joinRequestSubmitted.collectAsState()
    val requests by manager.pendingJoinRequests.collectAsState()
    return XmbFriendsInfo(
        enabled = Settings.shared.listenTogetherEnabled,
        username = name,
        roomCode = room?.roomCode,
        isHost = role == ListenTogetherRoomRole.HOST,
        members = room?.users?.size ?: 0,
        waitingToJoin = room == null && (pending || submitted),
        joinRequests = requests.map { it.userId to it.username },
    )
}

/** Words the M6 screens need while handling input (resolved in composition). */
internal class XmbM6Text(
    val connected: String,
    val connecting: String,
    val host: String,
    val roomCode: String,
    val yourName: String,
    val leaveQuestion: String,
    val approve: String,
    val reject: String,
    val nothingPlaying: String,
    val loading: String,
    val noLyrics: String,
)

@Composable
internal fun rememberXmbM6Text() =
    XmbM6Text(
        connected = stringResource(Res.string.xmb_cast_connected),
        connecting = stringResource(Res.string.xmb_cast_connecting),
        host = stringResource(Res.string.xmb_friends_host),
        roomCode = stringResource(Res.string.xmb_friends_room_code),
        yourName = stringResource(Res.string.xmb_friends_name),
        leaveQuestion = stringResource(Res.string.xmb_friends_leave_confirm),
        approve = stringResource(Res.string.xmb_friends_approve),
        reject = stringResource(Res.string.xmb_friends_reject),
        nothingPlaying = stringResource(Res.string.xmb_lyrics_nothing_playing),
        loading = stringResource(Res.string.loading),
        noLyrics = stringResource(Res.string.xmb_lyrics_none),
    )
