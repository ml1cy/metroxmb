package com.metrolist.android.xmb

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.metrolist.android.LocalMiniPlayerBottomInset
import com.metrolist.android.discord.DiscordLoginFlow
import com.metrolist.android.playback.LocalPlayerConnection
import com.metrolist.shared.discord.DiscordRpcManager
import com.metrolist.shared.discord.openDiscordUrl
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.copied
import com.metrolist.shared.ui.settings.IntegrationPlaybackState
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.ui.settings.SettingsPlatform
import com.metrolist.shared.ui.settings.SharedIntegrationsHost
import com.metrolist.shared.ui.settings.SharedIntegrationsScreen
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * The integrations registry's Android host (Discord, Last.fm, ListenBrainz, Device Connect), for
 * the XMB's settings column. Discord signs in through the browser ([DiscordLoginFlow]).
 */
@Composable
internal fun rememberAndroidIntegrationsHost(): SharedIntegrationsHost {
    val copiedMessage = stringResource(Res.string.copied)
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val currentSong by playerConnection?.currentSong?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }
    val duration by playerConnection?.duration?.collectAsState(initial = 0L) ?: remember { mutableLongStateOf(0L) }
    val playbackSpeed by playerConnection?.playbackSpeedFlow?.collectAsState() ?: remember { mutableFloatStateOf(1f) }
    val isPlaying by playerConnection?.isPlaying?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }
    val discordLoginInProgress = DiscordLoginFlow.isActive

    LaunchedEffect(discordLoginInProgress) {
        if (discordLoginInProgress) {
            delay(120_000L)
            if (DiscordLoginFlow.isActive && !DiscordLoginFlow.exchanging) {
                DiscordLoginFlow.clear()
                DiscordRpcManager.setError("discord_error_timeout")
            }
        }
    }

    return SharedIntegrationsHost(
        platform = SettingsPlatform.ANDROID,
        onClose = {},
        discordLoginInProgress = discordLoginInProgress,
        playbackPosition = playerConnection?.currentPosition,
        currentPositionMs = { playerConnection?.currentPositionMs() ?: 0L },
        playback =
            currentSong?.let { song ->
                IntegrationPlaybackState(
                    song = song,
                    artworkUrl = song.thumbnail,
                    artistArtworkUrl = song.artists.firstOrNull()?.thumbnail,
                    durationMs = duration,
                    isPlaying = isPlaying,
                    playbackSpeed = playbackSpeed,
                )
            },
        onStartDiscordLogin = { session ->
            DiscordLoginFlow.start(session)
            runCatching { context.openDiscordUrl(session.authorizeUrl) }
                .onFailure {
                    DiscordLoginFlow.clear()
                    DiscordRpcManager.setError("discord_error_no_browser")
                }
        },
        deviceConnect = rememberDeviceConnectSettingsHost(),
        onOpenUrl = { url -> runCatching { context.openDiscordUrl(url) } },
        onCopyText = { label, text -> context.copyToClipboard(label, text, copiedMessage) },
        onMessage = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
    )
}

private fun Context.copyToClipboard(
    label: String,
    text: String,
    copiedMessage: String,
) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(this, copiedMessage, Toast.LENGTH_SHORT).show()
}
