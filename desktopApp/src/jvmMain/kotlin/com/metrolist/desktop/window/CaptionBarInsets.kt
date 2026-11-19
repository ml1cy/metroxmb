package com.metrolist.desktop.window

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/** The window frame's caption strip, which the XMB's sky fills (the caption buttons sit on it). */
val LocalCaptionBarInsets = staticCompositionLocalOf { WindowInsets(0) }

/** The height of the window's caption strip on Windows. */
internal val DesktopTitleBarHeight = 48.dp
