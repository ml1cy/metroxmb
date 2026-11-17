package com.metrolist.shared.xmb.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.metrolist.shared.xmb.theme.XmbMetrics

/** A short message after a shortcut ("Shuffle on", "Volume 70%"), in the clock's style. */
@Composable
internal fun XmbToast(
    message: String,
    metrics: XmbMetrics,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(metrics.unit * 6)
    BasicText(
        message,
        style =
            TextStyle(
                color = Color.White,
                fontFamily = fontFamily,
                fontSize = with(LocalDensity.current) { (metrics.unit * 18).toSp() },
            ),
        modifier =
            modifier
                .background(Color.Black.copy(alpha = 0.35f), shape)
                .border(metrics.clockBorder, Color.White.copy(alpha = 0.33f), shape)
                .padding(horizontal = metrics.unit * 18, vertical = metrics.unit * 8),
    )
}
