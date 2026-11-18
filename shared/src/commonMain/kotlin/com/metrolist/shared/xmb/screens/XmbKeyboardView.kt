package com.metrolist.shared.xmb.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.xmb_keyboard_delete
import com.metrolist.shared.generated.resources.xmb_keyboard_done
import com.metrolist.shared.generated.resources.xmb_keyboard_shift
import com.metrolist.shared.generated.resources.xmb_keyboard_space
import com.metrolist.shared.generated.resources.xmb_keyboard_suggestions
import com.metrolist.shared.xmb.bar.LocalXmbTaps
import com.metrolist.shared.xmb.bar.xmbTap
import com.metrolist.shared.xmb.nav.XMB_KEY_COLUMNS
import com.metrolist.shared.xmb.nav.XmbKey
import com.metrolist.shared.xmb.nav.XmbKeyboard
import com.metrolist.shared.xmb.nav.startColumn
import com.metrolist.shared.xmb.nav.xmbKeyRows
import com.metrolist.shared.xmb.theme.XmbMetrics
import org.jetbrains.compose.resources.stringResource

/**
 * The on-screen keyboard over a dimmed XMB: the title, the text typed so far, the key grid with
 * the focused key lit, and suggestions in a column to the right when there are any. Physical
 * typing goes straight into the text (XmbApp handles the keys).
 */
@Composable
internal fun XmbKeyboardView(
    keyboard: XmbKeyboard,
    metrics: XmbMetrics,
    fontFamily: FontFamily,
) {
    val density = LocalDensity.current
    val unit = metrics.unit
    val taps = LocalXmbTaps.current
    val gap = unit * 6
    val keyHeight = unit * 46
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Keys shrink to fit a narrow screen (an upright phone), where the suggestions go below them.
        val stacked = metrics.portrait
        val room = maxWidth - unit * (2 * 26 + 24) - (if (stacked) 0.dp else unit * (300 + 24))
        val keyWidth = minOf(unit * 54, (room - gap * (XMB_KEY_COLUMNS - 1)) / XMB_KEY_COLUMNS)
        val keyStyle =
            TextStyle(
                color = Color.White,
                fontFamily = fontFamily,
                fontSize = with(density) { (unit * 20).toSp() },
                textAlign = TextAlign.Center,
            )
        val glow = Shadow(Color.White.copy(alpha = 0.7f), Offset.Zero, with(density) { (unit * 8).toPx() })
        val small =
            keyStyle.copy(
                fontSize =
                    with(density) {
                        (unit * 15).toSp()
                    },
                textAlign = TextAlign.Start,
                color = Color.White.copy(alpha = 0.7f),
            )
        val gridWidth = keyWidth * XMB_KEY_COLUMNS + gap * (XMB_KEY_COLUMNS - 1)

        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            Column(
                verticalArrangement = Arrangement.spacedBy(unit * 14),
                modifier =
                    Modifier
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(unit * 10))
                        .border(unit * 1.33f, Color.White.copy(alpha = 0.3f), RoundedCornerShape(unit * 10))
                        .padding(unit * 26),
            ) {
                BasicText(keyboard.title, style = small)
                val suggestions: @Composable () -> Unit = {
                    if (keyboard.suggestions.isNotEmpty()) {
                        Suggestions(
                            keyboard,
                            if (stacked) gridWidth else unit * 300,
                            keyHeight,
                            gap,
                            keyStyle,
                            small,
                            glow,
                            unit,
                        ) { taps?.suggestion(it) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(unit * 24)) {
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        TextField(keyboard, gridWidth, keyStyle, unit)
                        Spacer(Modifier.height(unit * 4))
                        xmbKeyRows.forEachIndexed { rowIndex, keys ->
                            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                keys.forEach { key ->
                                    val focused =
                                        keyboard.suggestion < 0 && keyboard.row == rowIndex && keyboard.column == startColumn(rowIndex, key)
                                    val width = keyWidth * key.span + gap * (key.span - 1)
                                    val column = startColumn(rowIndex, key)
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier =
                                            Modifier
                                                .size(width, keyHeight)
                                                .xmbTap { taps?.key(rowIndex, column) }
                                                .background(
                                                    Color.White.copy(alpha = if (focused) 0.32f else 0.08f),
                                                    RoundedCornerShape(unit * 5),
                                                ),
                                    ) {
                                        BasicText(
                                            text = keyLabel(key, keyboard.shift),
                                            style =
                                                if (focused) {
                                                    keyStyle.copy(
                                                        shadow = glow,
                                                    )
                                                } else {
                                                    keyStyle.copy(color = Color.White.copy(alpha = 0.8f))
                                                },
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (!stacked) suggestions()
                }
                if (stacked) suggestions()
            }
        }
    }
}

/** The search suggestions: a column of rows, the focused one lit; a tap picks one. */
@Composable
private fun Suggestions(
    keyboard: XmbKeyboard,
    width: Dp,
    keyHeight: Dp,
    gap: Dp,
    keyStyle: TextStyle,
    small: TextStyle,
    glow: Shadow,
    unit: Dp,
    onTap: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.width(width)) {
        BasicText(
            stringResource(Res.string.xmb_keyboard_suggestions),
            style = small,
            modifier = Modifier.height(keyHeight).padding(top = unit * 12),
        )
        keyboard.suggestions.forEachIndexed { index, suggestion ->
            val focused = index == keyboard.suggestion
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(keyHeight * 0.8f)
                        .xmbTap { onTap(index) }
                        .background(Color.White.copy(alpha = if (focused) 0.32f else 0f), RoundedCornerShape(unit * 5))
                        .padding(horizontal = unit * 10),
            ) {
                BasicText(
                    text = suggestion,
                    style =
                        (if (focused) keyStyle.copy(shadow = glow) else keyStyle.copy(color = Color.White.copy(alpha = 0.75f)))
                            .copy(textAlign = TextAlign.Start),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TextField(
    keyboard: XmbKeyboard,
    width: androidx.compose.ui.unit.Dp,
    style: TextStyle,
    unit: androidx.compose.ui.unit.Dp,
) {
    val shown = if (keyboard.secret) "•".repeat(keyboard.text.length) else keyboard.text
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier =
            Modifier
                .width(width)
                .height(unit * 52)
                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(unit * 5))
                .border(unit * 1.33f, Color.White.copy(alpha = 0.45f), RoundedCornerShape(unit * 5))
                .padding(horizontal = unit * 14),
    ) {
        // The end of the text stays visible as it grows; the caret marks where typing goes.
        BasicText(
            text = "$shown|",
            style = style.copy(textAlign = TextAlign.Start),
            maxLines = 1,
            overflow = TextOverflow.StartEllipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun keyLabel(
    key: XmbKey,
    shift: Boolean,
): String =
    when (key) {
        is XmbKey.Character -> if (shift) key.char.uppercaseChar().toString() else key.char.toString()
        XmbKey.Shift -> stringResource(Res.string.xmb_keyboard_shift) + if (shift) " ●" else ""
        XmbKey.Space -> stringResource(Res.string.xmb_keyboard_space)
        XmbKey.Delete -> stringResource(Res.string.xmb_keyboard_delete)
        XmbKey.Done -> stringResource(Res.string.xmb_keyboard_done)
    }
