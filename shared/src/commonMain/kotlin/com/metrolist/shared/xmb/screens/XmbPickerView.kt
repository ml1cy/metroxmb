package com.metrolist.shared.xmb.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import com.metrolist.shared.xmb.adapters.plainNumber
import com.metrolist.shared.xmb.bar.LocalXmbTaps
import com.metrolist.shared.xmb.bar.XmbBarGeometry
import com.metrolist.shared.xmb.bar.XmbRowStyles
import com.metrolist.shared.xmb.bar.placeAt
import com.metrolist.shared.xmb.bar.xmbTap
import com.metrolist.shared.xmb.nav.XmbPicker
import com.metrolist.shared.xmb.theme.XmbMetrics

/**
 * The value picker, opened at the focused row's value column: the values stacked around the
 * row with the current one glowing on a highlight band, or a slider with its value. [rowTitle] is
 * the focused row's title; a picker with a different title (a confirmation question) shows it
 * above the values.
 */
@Composable
internal fun XmbPickerView(
    picker: XmbPicker,
    rowTitle: String?,
    metrics: XmbMetrics,
    geometry: XmbBarGeometry,
    styles: XmbRowStyles,
) {
    val density = LocalDensity.current
    val unit = with(density) { metrics.unit.toPx() }
    // Text of the picker lines up with the rows' value column.
    val inset = metrics.unit * 18
    val x = geometry.anchorX + geometry.itemIcon / 2f + with(density) { (metrics.itemTextOffset + metrics.valueColumn - inset).toPx() }
    val width = metrics.unit * 380
    when (picker) {
        is XmbPicker.Choice -> {
            val pitch = unit * 44f
            val question = picker.title.takeIf { it != rowTitle }
            val top = geometry.focusY - (picker.focus + 0.5f) * pitch - (if (question != null) pitch * 1.4f else 0f) - unit * 10f
            val height = picker.labels.size * pitch + (if (question != null) pitch * 1.4f else 0f) + unit * 20f
            Box(
                Modifier
                    .placeAt { Offset(x, top) }
                    .size(width, with(density) { height.toDp() })
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(metrics.unit * 6)),
            )
            if (question != null) {
                BasicText(
                    text = question,
                    style = styles.subtitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .placeAt(alignY = 0.5f) { Offset(x + unit * 18f, top + unit * 10f + pitch * 0.7f) }
                            .width(width - inset),
                )
            }
            val taps = LocalXmbTaps.current
            picker.labels.forEachIndexed { index, label ->
                val focused = index == picker.focus
                val y = geometry.focusY + (index - picker.focus) * pitch
                Box(
                    Modifier
                        .placeAt(alignY = 0.5f) { Offset(x, y) }
                        .size(width, with(density) { pitch.toDp() })
                        .xmbTap { taps?.pickerValue(index) },
                )
                if (focused) {
                    Box(
                        Modifier
                            .placeAt(alignY = 0.5f) { Offset(x + unit * 6f, y) }
                            .size(width - metrics.unit * 12, with(density) { (pitch * 0.9f).toDp() })
                            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(metrics.unit * 4)),
                    )
                }
                BasicText(
                    text = label,
                    style = if (focused) styles.focusedTitle else styles.title.copy(color = Color.White.copy(alpha = 0.6f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .placeAt(alignY = 0.5f) { Offset(x + unit * 18f, y) }
                            .width(width - inset * 2),
                )
            }
        }

        is XmbPicker.Slider -> {
            val label = picker.format?.invoke(picker.value) ?: plainNumber(picker.value)
            val fraction = ((picker.value - picker.range.start) / (picker.range.endInclusive - picker.range.start)).coerceIn(0f, 1f)
            Column(
                verticalArrangement = Arrangement.spacedBy(metrics.unit * 10),
                modifier =
                    Modifier
                        .placeAt(alignY = 0.5f) { Offset(x, geometry.focusY) }
                        .width(width)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(metrics.unit * 6))
                        .padding(horizontal = inset, vertical = metrics.unit * 14),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(metrics.unit * 14)) {
                    val taps = LocalXmbTaps.current
                    val arrow = Modifier.padding(horizontal = metrics.unit * 8)
                    BasicText("◀", style = styles.subtitle, modifier = Modifier.xmbTap { taps?.sliderStep(-1) }.then(arrow))
                    BasicText(label, style = styles.focusedTitle, maxLines = 1, modifier = Modifier.xmbTap { taps?.confirm() })
                    BasicText("▶", style = styles.subtitle, modifier = Modifier.xmbTap { taps?.sliderStep(1) }.then(arrow))
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(metrics.unit * 6)
                        .drawBehind {
                            val radius = CornerRadius(size.height / 2f)
                            drawRoundRect(Color.White.copy(alpha = 0.2f), cornerRadius = radius)
                            drawRoundRect(
                                Color.White.copy(alpha = 0.85f),
                                size = Size(size.width * fraction, size.height),
                                cornerRadius = radius,
                            )
                        },
                )
            }
        }
    }
}
