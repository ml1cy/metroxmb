package com.metrolist.shared.xmb

import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

internal actual suspend fun xmbClipboardText(clipboard: Clipboard): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val contents = Toolkit.getDefaultToolkit().systemClipboard.getContents(null)
            if (contents?.isDataFlavorSupported(DataFlavor.stringFlavor) ==
                true
            ) {
                contents.getTransferData(DataFlavor.stringFlavor) as? String
            } else {
                null
            }
        }.getOrNull()
    }
