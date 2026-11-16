package com.metrolist.shared.visualizer.waves

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Android: the canvas fallback for now. The XMB reaches Android in M7, which adds the
 * `RuntimeShader` path (API 33+) with this fallback below it, like `PlatformLiquidBackgroundCanvas`.
 */
@Composable
internal actual fun PlatformWavesCanvas(
    colors: WavesColors,
    frame: () -> WavesFrame,
    modifier: Modifier,
) {
    Canvas(modifier) { drawWavesFallback(colors, frame()) }
}
