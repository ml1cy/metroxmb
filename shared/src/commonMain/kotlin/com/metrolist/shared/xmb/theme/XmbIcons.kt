package com.metrolist.shared.xmb.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Original category icons drawn for this project on the same 128-unit canvas as the PS3 PNG set:
 * solid white, flat, rounded ends, glyph centered with similar padding and stroke weight
 * (about 9 to 11 units) to the music and friends icons. See docs/xmb-notes.md.
 */
object XmbIcons {
    /** Category icon for Podcasts: a microphone on a stand. */
    val Podcasts: ImageVector by lazy {
        xmbIcon("XmbPodcasts") {
            fill("M64,22 A17,17 0 0 1 81,39 L81,57 A17,17 0 0 1 47,57 L47,39 A17,17 0 0 1 64,22 Z")
            stroke("M39.5,50 L39.5,57 A24.5,24.5 0 0 0 88.5,57 L88.5,50", width = 9f, cap = StrokeCap.Round)
            stroke("M64,81 L64,98", width = 9f, cap = StrokeCap.Butt)
            stroke("M48.5,101.5 L79.5,101.5", width = 9f, cap = StrokeCap.Round)
        }
    }

    /** Category icon for Visualizer: five rounded bars shaped like an audio waveform. */
    val Visualizer: ImageVector by lazy {
        xmbIcon("XmbVisualizer") {
            visualizerBars.forEach { (x, height) ->
                val half = height / 2f - BAR_WIDTH / 2f
                stroke("M$x,${CENTER - half} L$x,${CENTER + half}", width = BAR_WIDTH, cap = StrokeCap.Round)
            }
        }
    }

    /** Theme Settings: a color wheel with paint dots. */
    val Theme: ImageVector by lazy {
        xmbIcon("XmbTheme") {
            stroke("M18,64 a46,46 0 1,0 92,0 a46,46 0 1,0 -92,0", 9f, StrokeCap.Round)
            for ((x, y) in listOf(64f to 38f, 42f to 52f, 86f to 52f, 48f to 80f)) {
                fill("M${x - 8},$y a8,8 0 1,0 16,0 a8,8 0 1,0 -16,0")
            }
        }
    }

    /** Date and Time Settings: a clock face. */
    val Clock: ImageVector by lazy {
        xmbIcon("XmbClock") {
            stroke("M18,64 a46,46 0 1,0 92,0 a46,46 0 1,0 -92,0", 9f, StrokeCap.Round)
            stroke("M64,64 L64,36 M64,64 L84,76", 9f, StrokeCap.Round)
        }
    }

    /** Keyboard Shortcuts: a keyboard. */
    val Keyboard: ImageVector by lazy {
        xmbIcon("XmbKeyboard") {
            stroke("M22,34 h84 a8,8 0 0 1 8,8 v44 a8,8 0 0 1 -8,8 h-84 a8,8 0 0 1 -8,-8 v-44 a8,8 0 0 1 8,-8 z", 8f, StrokeCap.Round)
            for (x in listOf(28f, 44f, 60f, 76f, 92f)) fill("M$x,46 h10 v10 h-10 z")
            for (x in listOf(36f, 52f, 68f, 84f)) fill("M$x,62 h10 v10 h-10 z")
            fill("M40,78 h48 v8 h-48 z")
        }
    }

    private const val CANVAS = 128f
    private const val CENTER = 64f
    private const val BAR_WIDTH = 11f
    private val visualizerBars = listOf(30f to 34f, 47f to 62f, 64f to 84f, 81f to 54f, 98f to 30f)

    private fun xmbIcon(
        name: String,
        block: ImageVector.Builder.() -> Unit,
    ): ImageVector =
        ImageVector
            .Builder(
                name = name,
                defaultWidth = CANVAS.dp,
                defaultHeight = CANVAS.dp,
                viewportWidth = CANVAS,
                viewportHeight = CANVAS,
            ).apply(block)
            .build()

    private fun ImageVector.Builder.fill(path: String) {
        addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.White))
    }

    private fun ImageVector.Builder.stroke(
        path: String,
        width: Float,
        cap: StrokeCap,
    ) {
        addPath(
            PathParser().parsePathString(path).toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = width,
            strokeLineCap = cap,
        )
    }
}
