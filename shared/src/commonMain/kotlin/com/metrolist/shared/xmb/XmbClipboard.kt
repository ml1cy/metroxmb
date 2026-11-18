package com.metrolist.shared.xmb

import androidx.compose.ui.platform.Clipboard

/** The clipboard's text, for pasting into the on-screen keyboard; null when it holds none. */
internal expect suspend fun xmbClipboardText(clipboard: Clipboard): String?
