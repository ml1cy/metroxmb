package com.metrolist.shared.visualizer.background

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Paint
import org.jetbrains.skia.VertexMode

// Vertex colors are combined with the paint's color by MODULATE, so the paint is white; PLUS adds
// the result onto the canvas.
private val additive =
    Paint().apply {
        color = 0xFFFFFFFF.toInt()
        blendMode = BlendMode.PLUS
    }

private val passColors = IntArray(RIBBON_RESOLUTION * RIBBON_RESOLUTION)

internal actual fun DrawScope.drawRibbonMesh(
    positions: FloatArray,
    levels: IntArray,
    rgb: Int,
    indices: ShortArray,
) {
    val canvas = drawContext.canvas.nativeCanvas
    RIBBON_SAMPLE_OFFSETS.forEachIndexed { pass, offset ->
        ribbonPassColors(levels, rgb, pass, passColors)
        canvas.save()
        canvas.translate(offset.x, offset.y)
        canvas.drawVertices(VertexMode.TRIANGLES, positions, passColors, null, indices, BlendMode.MODULATE, additive)
        canvas.restore()
    }
}
