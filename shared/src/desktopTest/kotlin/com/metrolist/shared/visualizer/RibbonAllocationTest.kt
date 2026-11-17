package com.metrolist.shared.visualizer

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.metrolist.shared.visualizer.background.RibbonFrame
import com.metrolist.shared.visualizer.background.drawRibbon
import com.metrolist.shared.visualizer.background.drawRibbonParticles
import com.sun.management.ThreadMXBean
import org.jetbrains.skia.Surface
import java.lang.management.ManagementFactory
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The ribbon draws every frame, on phones too: it must not churn the garbage collector. Built from
 * lists, a 1080p frame allocated about 1.7 MB (100 MB a second at 60 fps); from reused arrays it's
 * a few tens of KB. Drawn onto a 1×1 surface so only the preparation is measured.
 */
class RibbonAllocationTest {
    @Test
    fun aRibbonFrameAllocatesLittle() {
        ImageComposeScene(1, 1) {}.close() // registers Compose's graphics on this thread
        val canvas = Surface.makeRasterN32Premul(1, 1).canvas.asComposeCanvas()
        val scope = CanvasDrawScope()

        fun frame(index: Int) =
            scope.draw(Density(1f), LayoutDirection.Ltr, canvas, Size(1920f, 1080f)) {
                val ribbon = RibbonFrame(time = index * 0.01f, streak = 0.02f)
                drawRibbon(ribbon)
                drawRibbonParticles(ribbon)
            }
        repeat(100, ::frame)
        val threads = ManagementFactory.getThreadMXBean() as ThreadMXBean
        val thread = Thread.currentThread().threadId()
        val before = threads.getThreadAllocatedBytes(thread)
        repeat(FRAMES, ::frame)
        val perFrame = (threads.getThreadAllocatedBytes(thread) - before) / FRAMES
        assertTrue(perFrame < 200 * 1024, "a ribbon frame allocates ${perFrame / 1024} KB")
    }

    private companion object {
        const val FRAMES = 100
    }
}
