package com.metrolist.shared.xmb

import androidx.compose.ui.platform.Clipboard

internal actual suspend fun xmbClipboardText(clipboard: Clipboard): String? =
    runCatching {
        clipboard
            .getClipEntry()
            ?.clipData
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.text
            ?.toString()
    }.getOrNull()
