package com.metrolist.shared.xmb

import androidx.compose.ui.input.pointer.PointerInputEventHandler

/**
 * The XMB's whole-screen pointer handler (wheel scrolling, taking keys back on a click). It sits
 * behind everything, so a window that draws its title-bar buttons over the XMB can let presses in
 * that strip go to the window (drag, double-click to maximize) instead of counting them as the XMB's.
 */
fun interface XmbBackdropInput : PointerInputEventHandler
