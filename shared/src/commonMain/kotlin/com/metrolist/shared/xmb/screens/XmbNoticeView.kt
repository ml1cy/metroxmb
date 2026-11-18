package com.metrolist.shared.xmb.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import com.metrolist.shared.xmb.bar.LocalXmbTaps
import com.metrolist.shared.xmb.bar.xmbTap
import com.metrolist.shared.xmb.nav.XmbNotice
import com.metrolist.shared.xmb.theme.XmbMetrics

/**
 * A notice the console way: the XMB dims, and a dark band across the middle carries the title,
 * the message, optional details and the buttons, the focused one lit.
 */
@Composable
internal fun XmbNoticeView(
    notice: XmbNotice,
    focus: Int,
    metrics: XmbMetrics,
    fontFamily: FontFamily,
) {
    val density = LocalDensity.current
    val unit = metrics.unit

    fun style(
        size: Float,
        alpha: Float = 1f,
    ) = TextStyle(
        color = Color.White.copy(alpha = alpha),
        fontFamily = fontFamily,
        fontSize =
            with(density) {
                (unit * size).toSp()
            },
        textAlign = TextAlign.Center,
    )
    val glow = Shadow(Color.White.copy(alpha = 0.6f), Offset.Zero, with(density) { (unit * 8).toPx() })
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.72f)),
        ) {
            Box(Modifier.fillMaxWidth().height(unit * 1.33f).background(Color.White.copy(alpha = 0.35f)))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(unit * 14),
                modifier = Modifier.widthIn(max = unit * 900).padding(vertical = unit * 34, horizontal = unit * 40),
            ) {
                notice.title?.let { BasicText(it, style = style(16f, 0.7f)) }
                BasicText(notice.message, style = style(22f), maxLines = 4, overflow = TextOverflow.Ellipsis)
                notice.details?.let { BasicText(it, style = style(14f, 0.65f), maxLines = 6, overflow = TextOverflow.Ellipsis) }
                Row(horizontalArrangement = Arrangement.spacedBy(unit * 24), modifier = Modifier.padding(top = unit * 10)) {
                    val taps = LocalXmbTaps.current
                    notice.buttons.forEachIndexed { index, label ->
                        val focused = index == focus
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier
                                    .xmbTap { taps?.noticeButton(index) }
                                    .widthIn(min = unit * 160)
                                    .background(Color.White.copy(alpha = if (focused) 0.18f else 0f), RoundedCornerShape(unit * 4))
                                    .padding(horizontal = unit * 18, vertical = unit * 6),
                        ) {
                            BasicText(label, style = if (focused) style(20f).copy(shadow = glow) else style(20f, 0.6f))
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(unit * 1.33f).background(Color.White.copy(alpha = 0.35f)))
        }
    }
}
