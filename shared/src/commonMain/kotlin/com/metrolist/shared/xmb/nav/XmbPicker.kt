package com.metrolist.shared.xmb.nav

import androidx.compose.runtime.Composable
import kotlin.math.roundToInt

/**
 * The value picker that opens beside a setting, like the console's: a short list of values
 * ([Choice]) or a slider ([Slider]). Nothing changes until ✕ confirms; ○ cancels.
 */
sealed interface XmbPicker {
    val title: String

    /** [labels] to pick from, [focus] on the current value; [apply] receives the chosen index. */
    data class Choice(
        override val title: String,
        val labels: List<String>,
        val focus: Int,
        val apply: (Int) -> Unit,
    ) : XmbPicker

    /** A number in [range]; Left/Right (or Up/Down) move by [step]. */
    data class Slider(
        override val title: String,
        val value: Float,
        val range: ClosedFloatingPointRange<Float>,
        val steps: Int,
        val format: (@Composable (Float) -> String)?,
        val apply: (Float) -> Unit,
    ) : XmbPicker {
        /** Registry sliders with `steps` have steps + 1 intervals; continuous ones move in twentieths. */
        val step: Float get() = (range.endInclusive - range.start) / (if (steps > 0) steps + 1 else CONTINUOUS_STEPS)

        fun moved(direction: Int): Slider {
            val raw = value + direction * step
            val snapped = range.start + ((raw - range.start) / step).roundToInt() * step
            return copy(value = snapped.coerceIn(range.start, range.endInclusive))
        }
    }

    private companion object {
        const val CONTINUOUS_STEPS = 20
    }
}

/** One picker action's result: the picker (null once closed), the sound, and the change to commit. */
data class XmbPickerStep(
    val picker: XmbPicker?,
    val feedback: XmbFeedback,
    val commit: (() -> Unit)? = null,
)

fun XmbPicker.reduce(action: XmbAction): XmbPickerStep =
    when (action) {
        XmbAction.BACK -> {
            XmbPickerStep(null, XmbFeedback.BACK)
        }

        XmbAction.SELECT -> {
            when (this) {
                is XmbPicker.Choice -> XmbPickerStep(null, XmbFeedback.SELECT) { apply(focus) }
                is XmbPicker.Slider -> XmbPickerStep(null, XmbFeedback.SELECT) { apply(value) }
            }
        }

        XmbAction.UP, XmbAction.DOWN, XmbAction.LEFT, XmbAction.RIGHT -> {
            when (this) {
                is XmbPicker.Choice -> moveChoice(action)
                is XmbPicker.Slider -> moveSlider(action)
            }
        }

        XmbAction.OPTIONS, XmbAction.PLAY_PAUSE, XmbAction.NEXT_TRACK, XmbAction.PREVIOUS_TRACK -> {
            XmbPickerStep(this, XmbFeedback.NONE)
        }
    }

private fun XmbPicker.Choice.moveChoice(action: XmbAction): XmbPickerStep {
    val up = focus > 0
    val down = focus < labels.lastIndex
    return when {
        action == XmbAction.UP && up -> XmbPickerStep(copy(focus = focus - 1), XmbFeedback.MOVE_UP)

        action == XmbAction.DOWN && down -> XmbPickerStep(copy(focus = focus + 1), XmbFeedback.MOVE_DOWN)

        // Left closes the picker like Back, matching how Left leaves a folder.
        action == XmbAction.LEFT -> XmbPickerStep(null, XmbFeedback.BACK)

        else -> XmbPickerStep(this, XmbFeedback.NONE)
    }
}

private fun XmbPicker.Slider.moveSlider(action: XmbAction): XmbPickerStep {
    val direction = if (action == XmbAction.RIGHT || action == XmbAction.UP) 1 else -1
    val next = moved(direction)
    return if (next.value == value) XmbPickerStep(this, XmbFeedback.NONE) else XmbPickerStep(next, XmbFeedback.MOVE_SIDE)
}
