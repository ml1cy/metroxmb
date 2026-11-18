package com.metrolist.shared.visualizer.shader

import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.metrolist.shared.utils.Logger

internal actual class XmbShader actual constructor(
    source: String,
) {
    // RuntimeShader is Android 13+; below that the visualizer draws its fallback.
    private val runtime: Any? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { make(source) }.onFailure { Logger.w("XmbShader", "Shader didn't compile: ${it.message}") }.getOrNull()
        } else {
            null
        }

    actual val available: Boolean get() = runtime != null

    actual fun set(
        name: String,
        value: Float,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) (runtime as? RuntimeShader)?.setFloatUniform(name, value)
    }

    actual fun set(
        name: String,
        x: Float,
        y: Float,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) (runtime as? RuntimeShader)?.setFloatUniform(name, x, y)
    }

    actual fun set(
        name: String,
        x: Float,
        y: Float,
        z: Float,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) (runtime as? RuntimeShader)?.setFloatUniform(name, x, y, z)
    }

    actual fun setImage(
        name: String,
        image: ImageBitmap,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setInput(name, image)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setInput(
        name: String,
        image: ImageBitmap,
    ) {
        val bitmap = BitmapShader(image.asAndroidBitmap(), Shader.TileMode.REPEAT, Shader.TileMode.CLAMP)
        bitmap.filterMode = BitmapShader.FILTER_MODE_LINEAR
        (runtime as? RuntimeShader)?.setInputShader(name, bitmap)
    }

    private val paint = Paint()

    actual fun draw(scope: DrawScope) {
        val shader = runtime as? Shader ?: return
        paint.shader = shader
        scope.drawContext.canvas.nativeCanvas
            .drawRect(0f, 0f, scope.size.width, scope.size.height, paint)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun make(source: String): Any = RuntimeShader(source)
}
