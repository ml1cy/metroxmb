package com.metrolist.shared.visualizer.shader

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.metrolist.shared.utils.Logger
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.SamplingMode

internal actual class XmbShader actual constructor(
    source: String,
) {
    private val builder =
        runCatching { RuntimeShaderBuilder(RuntimeEffect.makeForShader(source)) }
            .onFailure { Logger.w("XmbShader", "Shader didn't compile: ${it.message}") }
            .getOrNull()

    actual val available: Boolean get() = builder != null

    actual fun set(
        name: String,
        value: Float,
    ) {
        builder?.uniform(name, value)
    }

    actual fun set(
        name: String,
        x: Float,
        y: Float,
    ) {
        builder?.uniform(name, x, y)
    }

    actual fun set(
        name: String,
        x: Float,
        y: Float,
        z: Float,
    ) {
        builder?.uniform(name, x, y, z)
    }

    actual fun setImage(
        name: String,
        image: ImageBitmap,
    ) {
        val skia = Image.makeFromBitmap(image.asSkiaBitmap().apply { setImmutable() })
        builder?.child(name, skia.makeShader(FilterTileMode.REPEAT, FilterTileMode.CLAMP, SamplingMode.LINEAR))
    }

    private val paint = Paint()

    // A new Skia shader per frame; the old ones are freed by their cleaners once the recorded frames
    // that use them are gone (closing them here could pull them from under a recorded frame).
    actual fun draw(scope: DrawScope) {
        val shader = builder?.makeShader() ?: return
        paint.shader = shader
        scope.drawContext.canvas.nativeCanvas
            .drawRect(Rect.makeWH(scope.size.width, scope.size.height), paint)
    }
}
