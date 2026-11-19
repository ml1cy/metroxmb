package com.metrolist.shared.visualizer.background

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Build
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Canvas as FrameworkCanvas

private fun additivePaint(filter: Boolean = false) =
    Paint().apply {
        isFilterBitmap = filter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            blendMode = BlendMode.PLUS
        } else {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        }
    }

private val additive = additivePaint()
private val passColors = IntArray(RIBBON_RESOLUTION * RIBBON_RESOLUTION)

internal actual fun DrawScope.drawRibbonMesh(
    positions: FloatArray,
    levels: IntArray,
    rgb: Int,
    indices: ShortArray,
) {
    val canvas = drawContext.canvas.nativeCanvas
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || !canvas.isHardwareAccelerated) {
        ribbonSampleOffsets.forEachIndexed { pass, offset ->
            ribbonPassColors(levels, rgb, pass, passColors)
            canvas.save()
            canvas.translate(offset.x, offset.y)
            canvas.drawVertices(
                FrameworkCanvas.VertexMode.TRIANGLES,
                positions.size,
                positions,
                0,
                null,
                0,
                passColors,
                0,
                indices,
                0,
                indices.size,
                additive,
            )
            canvas.restore()
        }
    } else {
        SoftwareRibbon.draw(canvas, size.width, size.height, positions, levels, rgb, indices)
    }
}

/**
 * Android 7–9 hardware canvases can't draw vertices, so there the mesh is drawn in software
 * into a half-size bitmap (one pass: scaling the bitmap up smooths its edges) and added on.
 */
private object SoftwareRibbon {
    private var bitmap: Bitmap? = null
    private val canvas = FrameworkCanvas()
    private val colors = IntArray(RIBBON_RESOLUTION * RIBBON_RESOLUTION)
    private val upscale = additivePaint(filter = true)
    private val bounds = RectF()

    fun draw(
        target: FrameworkCanvas,
        width: Float,
        height: Float,
        positions: FloatArray,
        levels: IntArray,
        rgb: Int,
        indices: ShortArray,
    ) {
        val w = (width / 2f).toInt().coerceAtLeast(1)
        val h = (height / 2f).toInt().coerceAtLeast(1)
        val image =
            bitmap?.takeIf { it.width == w && it.height == h } ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bitmap = it }
        image.eraseColor(Color.TRANSPARENT)
        canvas.setBitmap(image)
        canvas.save()
        canvas.scale(w / width, h / height)
        for (index in levels.indices) colors[index] = (levels[index] shl 24) or rgb
        canvas.drawVertices(
            FrameworkCanvas.VertexMode.TRIANGLES,
            positions.size,
            positions,
            0,
            null,
            0,
            colors,
            0,
            indices,
            0,
            indices.size,
            additive,
        )
        canvas.restore()
        bounds.set(0f, 0f, width, height)
        target.drawBitmap(image, null, bounds, upscale)
    }
}
