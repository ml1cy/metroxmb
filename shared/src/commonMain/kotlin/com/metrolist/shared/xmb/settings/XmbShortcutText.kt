package com.metrolist.shared.xmb.settings

import androidx.compose.runtime.Composable
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.xmb_search_new
import com.metrolist.shared.generated.resources.xmb_shortcut_hint
import com.metrolist.shared.generated.resources.xmb_shortcut_press
import com.metrolist.shared.generated.resources.xmb_shortcut_reserved
import com.metrolist.shared.generated.resources.xmb_toast_liked
import com.metrolist.shared.generated.resources.xmb_toast_muted
import com.metrolist.shared.generated.resources.xmb_toast_repeat_all
import com.metrolist.shared.generated.resources.xmb_toast_repeat_off
import com.metrolist.shared.generated.resources.xmb_toast_repeat_one
import com.metrolist.shared.generated.resources.xmb_toast_shuffle_off
import com.metrolist.shared.generated.resources.xmb_toast_shuffle_on
import com.metrolist.shared.generated.resources.xmb_toast_unliked
import com.metrolist.shared.generated.resources.xmb_toast_volume
import org.jetbrains.compose.resources.stringResource

/** The words shortcuts show: the key prompt and the short messages after a shortcut. */
class XmbShortcutText(
    val press: String,
    val hint: String,
    val reserved: String,
    val search: String,
    val shuffleOn: String,
    val shuffleOff: String,
    val repeatOff: String,
    val repeatAll: String,
    val repeatOne: String,
    val muted: String,
    val liked: String,
    val unliked: String,
    private val volumeFormat: String,
) {
    fun volume(percent: Int): String = volumeFormat.replace("%1\$d", percent.toString())
}

@Composable
fun rememberXmbShortcutText(): XmbShortcutText =
    XmbShortcutText(
        press = stringResource(Res.string.xmb_shortcut_press),
        hint = stringResource(Res.string.xmb_shortcut_hint),
        reserved = stringResource(Res.string.xmb_shortcut_reserved),
        search = stringResource(Res.string.xmb_search_new),
        shuffleOn = stringResource(Res.string.xmb_toast_shuffle_on),
        shuffleOff = stringResource(Res.string.xmb_toast_shuffle_off),
        repeatOff = stringResource(Res.string.xmb_toast_repeat_off),
        repeatAll = stringResource(Res.string.xmb_toast_repeat_all),
        repeatOne = stringResource(Res.string.xmb_toast_repeat_one),
        muted = stringResource(Res.string.xmb_toast_muted),
        liked = stringResource(Res.string.xmb_toast_liked),
        unliked = stringResource(Res.string.xmb_toast_unliked),
        volumeFormat = stringResource(Res.string.xmb_toast_volume),
    )
