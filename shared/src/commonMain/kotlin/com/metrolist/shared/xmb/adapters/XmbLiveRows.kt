package com.metrolist.shared.xmb.adapters

import com.metrolist.shared.cast.CastConnectionStatus
import com.metrolist.shared.cast.CastDevice
import com.metrolist.shared.cast.CastProtocol
import com.metrolist.shared.cast.CastState
import com.metrolist.shared.deviceconnect.DeviceConnectManager
import com.metrolist.shared.listentogether.network.UserInfo
import com.metrolist.shared.model.LyricsEntry

/** Cast devices as rows: the connected (or connecting) one shows that as its value. */
fun castEntries(
    devices: List<CastDevice>,
    state: CastState,
    connected: String,
    connecting: String,
): List<XmbEntry> {
    val current = state.device
    // The connected device stays listed even when discovery stops reporting it.
    val all = if (current != null && devices.none { it.id == current.id }) listOf(current) + devices else devices
    return all.map { device ->
        val value =
            when {
                current?.id != device.id -> null
                state.status == CastConnectionStatus.CONNECTED -> connected
                state.status == CastConnectionStatus.CONNECTING -> connecting
                else -> null
            }
        XmbEntry(
            key = "cast:${device.id}",
            title = device.name,
            subtitle = device.protocol.label,
            artwork = null,
            kind = XmbEntryKind.DEVICE,
            value = value,
        )
    }
}

/**
 * The user's other devices (Device Connect peers that are connected) as rows after the cast devices:
 * the one playing now is marked [playingThere]; selecting another moves playback to it.
 */
fun peerEntries(
    peers: List<DeviceConnectManager.PeerDevice>,
    localDeviceId: String,
    targetId: String?,
    subtitle: String,
    playingThere: String,
): List<XmbEntry> =
    peers
        .filter { it.state == DeviceConnectManager.PeerState.CONNECTED && it.id != localDeviceId }
        .map { peer ->
            XmbEntry(
                key = "$PEER_KEY${peer.id}",
                title = peer.name,
                subtitle = peer.model.ifBlank { subtitle },
                artwork = null,
                kind = XmbEntryKind.DEVICE,
                value = playingThere.takeIf { peer.id == targetId },
            )
        }

/** The Device Connect peer a row is for, or null for a cast device. */
fun XmbEntry.peerId(): String? = key.takeIf { it.startsWith(PEER_KEY) }?.removePrefix(PEER_KEY)

private const val PEER_KEY = "peer:"

private val CastProtocol.label: String
    get() =
        when (this) {
            CastProtocol.FCAST -> "FCast"
            CastProtocol.GOOGLE_CAST -> "Google Cast"
            CastProtocol.DLNA -> "DLNA"
            CastProtocol.SONOS -> "Sonos"
        }

/** The device a cast row stands for. */
fun CastDevice.matches(entry: XmbEntry): Boolean = entry.key == "cast:$id"

/** People in the Listen Together room, the host first. */
fun memberEntries(
    users: List<UserInfo>,
    hostLabel: String,
): List<XmbEntry> =
    users.sortedByDescending { it.isHost }.map { user ->
        XmbEntry(
            key = "member:${user.userId}",
            title = user.username,
            subtitle = hostLabel.takeIf { user.isHost },
            artwork = null,
            kind = XmbEntryKind.MEMBER,
        )
    }

/** The lyric line playing at [positionMs]: the last one that has started, or -1 before the first. */
fun currentLyricIndex(
    lines: List<LyricsEntry>,
    positionMs: Long,
): Int {
    var low = 0
    var high = lines.lastIndex
    var found = -1
    while (low <= high) {
        val middle = (low + high) / 2
        if (lines[middle].time <= positionMs) {
            found = middle
            low = middle + 1
        } else {
            high = middle - 1
        }
    }
    return found
}
