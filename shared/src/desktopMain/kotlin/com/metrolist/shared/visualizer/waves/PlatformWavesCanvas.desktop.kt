package com.metrolist.shared.visualizer.waves

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

/** Desktop: the ribbons as a Skia runtime shader, like `PlatformLiquidBackgroundCanvas`. */
@Composable
internal actual fun PlatformWavesCanvas(
    colors: WavesColors,
    frame: () -> WavesFrame,
    modifier: Modifier,
) {
    val renderer = remember { WavesShaderRenderer() }
    DisposableEffect(renderer) { onDispose { renderer.close() } }
    Canvas(modifier) {
        val current = frame()
        if (!renderer.draw(this, colors, current)) drawWavesFallback(colors, current)
    }
}

private class WavesShaderRenderer {
    private val effect = runCatching { RuntimeEffect.makeForShader(WAVES_SHADER) }.getOrNull()
    private val builder = effect?.let(::RuntimeShaderBuilder)
    private val paint = Paint()
    private var available = builder != null

    fun draw(
        scope: androidx.compose.ui.graphics.drawscope.DrawScope,
        colors: WavesColors,
        frame: WavesFrame,
    ): Boolean {
        val builder = builder ?: return false
        if (!available || scope.size.width <= 0f || scope.size.height <= 0f) return false
        return try {
            builder.uniform("resolution", scope.size.width, scope.size.height)
            builder.uniform("time", frame.time)
            builder.uniform("audio", frame.bass, frame.mid, frame.treble, frame.beat)
            builder.uniform("base", colors.base.red, colors.base.green, colors.base.blue)
            builder.uniform("glow", colors.glow.red, colors.glow.green, colors.glow.blue)
            builder.makeShader().use { shader ->
                paint.shader = shader
                scope.drawIntoCanvas { it.nativeCanvas.drawRect(Rect.makeWH(scope.size.width, scope.size.height), paint) }
                paint.shader = null
            }
            true
        } catch (_: Exception) {
            available = false
            false
        }
    }

    fun close() {
        paint.close()
        builder?.close()
        effect?.close()
    }
}
