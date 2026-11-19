package com.metrolist.shared.xmb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.metrolist.shared.deviceconnect.DeviceConnectManager
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.approve
import com.metrolist.shared.generated.resources.device_connect_pair_code_message
import com.metrolist.shared.generated.resources.device_connect_pair_same_account_message
import com.metrolist.shared.generated.resources.device_connect_pair_title
import com.metrolist.shared.generated.resources.reject
import com.metrolist.shared.xmb.nav.XmbNotice
import com.metrolist.shared.xmb.nav.XmbNoticeRequest
import org.jetbrains.compose.resources.stringResource

/**
 * Another device asking to pair over Device Connect, as an XMB notice: its name, the code to
 * compare when it uses another account, and Approve or Reject (○).
 */
@Composable
fun rememberXmbPairingNotice(manager: DeviceConnectManager?): XmbNoticeRequest? {
    manager ?: return null
    val state by manager.state.collectAsState()
    val request = state.pairingRequests.firstOrNull { it.requiresApproval } ?: return null
    val name = request.deviceName.ifBlank { request.deviceId }
    val title = stringResource(Res.string.device_connect_pair_title, name)
    val message =
        if (request.sas.isBlank()) {
            stringResource(Res.string.device_connect_pair_same_account_message, name)
        } else {
            stringResource(Res.string.device_connect_pair_code_message, name)
        }
    val approve = stringResource(Res.string.approve)
    val reject = stringResource(Res.string.reject)
    return XmbNoticeRequest(
        XmbNotice(
            id = "pairing:${request.requestId}",
            title = title,
            message = message,
            details =
                request.sas
                    .takeIf(String::isNotBlank)
                    ?.toCharArray()
                    ?.joinToString(" "),
            buttons = listOf(approve, reject),
            cancel = 1,
        ),
    ) { chosen -> if (chosen == 0) manager.approvePairing(request.requestId) else manager.rejectPairing(request.requestId) }
}
