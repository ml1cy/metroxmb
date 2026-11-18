package com.metrolist.shared.visualizer.shader

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * A runtime shader for the visualizers: one source, written in the subset SkSL (desktop) and AGSL
 * (Android 13+) share, like `ArtworkBackgroundShader.kt`. Its float uniforms are set by name; every
 * name must exist in the source. Where shaders can't run (Android before 13) or the source doesn't
 * compile, [available] is false and the visualizer draws its fallback.
 */
internal expect class XmbShader(
    source: String,
) {
    val available: Boolean

    fun set(
        name: String,
        value: Float,
    )

    fun set(
        name: String,
        x: Float,
        y: Float,
    )

    fun set(
        name: String,
        x: Float,
        y: Float,
        z: Float,
    )

    /**
     * An image the shader samples as `uniform shader [name]`, repeating across and clamped top to
     * bottom, with bilinear filtering. Set once; it stays.
     */
    fun setImage(
        name: String,
        image: ImageBitmap,
    )

    /** Fills the draw scope's area with the shader, with the uniforms set so far. */
    fun draw(scope: DrawScope)
}
