package com.metrolist.shared.xmb.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.xmb_item_unavailable
import com.metrolist.shared.xmb.nav.XmbCategory
import com.metrolist.shared.xmb.nav.XmbIcon
import com.metrolist.shared.xmb.nav.XmbItem
import com.metrolist.shared.xmb.nav.XmbItemAction
import com.metrolist.shared.xmb.nav.XmbNavigation
import com.metrolist.shared.xmb.theme.XmbMetrics
import com.metrolist.shared.xmb.theme.XmbMotion
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

/** Positions in pixels that the bar is laid out around. */
internal class XmbBarGeometry(
    val anchorX: Float,
    val barY: Float,
    val categorySpacing: Float,
    val categoryIcon: Float,
    val categoryLabelOffset: Float,
    val itemIcon: Float,
    val itemSpacing: Float,
    val focusedItemOffset: Float,
    val itemsAboveGap: Float,
    val width: Float,
    val levelFocusY: Float = barY + focusedItemOffset,
    val parentOffset: Float = itemIcon * 2f,
) {
    fun categoryX(offset: Float): Float = anchorX + offset * categorySpacing

    /**
     * Center of an item [relative] places from the focused one. Items below the focused one hang
     * under the category row; items above it jump over the row, blending smoothly across that gap.
     */
    fun itemY(relative: Float): Float = barY + focusedItemOffset + relative * itemSpacing - itemsAboveGap * (-relative).coerceIn(0f, 1f)

    /** Categories more than this many places from the focused one are off screen. */
    val visibleCategories: Int get() = (width / categorySpacing).toInt() + 2
}

private const val ITEMS_ABOVE = 6
private const val ITEMS_BELOW = 10

/**
 * The category row and, under the focused category, its item column. Animated positions are
 * read in layout, placement and draw lambdas, so scrolling doesn't recompose.
 */
@Composable
internal fun XmbBar(
    categories: List<XmbCategory>,
    navigation: XmbNavigation,
    metrics: XmbMetrics,
    geometry: XmbBarGeometry,
    fontFamily: FontFamily,
    glow: () -> Float,
    listsEnabled: Boolean = false,
) {
    val categoryPosition = remember { Animatable(navigation.category.toFloat()) }
    LaunchedEffect(navigation.category) {
        categoryPosition.animateTo(navigation.category.toFloat(), tween(XmbMotion.CATEGORY_MS, easing = XmbMotion.scrollEasing))
    }
    val itemPositions = remember(categories.size) { List(categories.size) { Animatable(navigation.items.getOrElse(it) { 0 }.toFloat()) } }
    navigation.items.forEachIndexed { index, item ->
        key(index) {
            LaunchedEffect(item) {
                itemPositions[index].animateTo(item.toFloat(), tween(XmbMotion.ITEM_MS, easing = XmbMotion.scrollEasing))
            }
        }
    }
    val density = LocalDensity.current
    val labelStyle =
        TextStyle(
            color = Color.White,
            fontFamily = fontFamily,
            fontSize = with(density) { metrics.categoryLabelSize.toSp() },
            shadow = Shadow(Color.White.copy(alpha = 0.55f), Offset.Zero, with(density) { metrics.unit.toPx() * 8f }),
        )
    categories.forEachIndexed { index, category ->
        if (abs(index - navigation.category) > geometry.visibleCategories) return@forEachIndexed
        key(category.id) {
            val distance = { abs(index - categoryPosition.value) }
            CategoryIcon(category.icon, metrics.categoryIcon, geometry, index, categoryPosition::value, distance, glow)
            BasicText(
                text = stringResource(category.title),
                style = labelStyle,
                maxLines = 1,
                softWrap = false,
                modifier =
                    Modifier
                        .placeAt(alignX = 0.5f) {
                            Offset(geometry.categoryX(index - categoryPosition.value), geometry.barY + geometry.categoryLabelOffset)
                        }.graphicsLayer { alpha = (1f - 2f * distance()).coerceIn(0f, 1f) },
            )
            if (abs(index - navigation.category) <= 1) {
                ItemColumn(
                    category = category,
                    focusedItem = navigation.items.getOrElse(index) { 0 },
                    isFocusedCategory = index == navigation.category,
                    itemPosition = itemPositions[index]::value,
                    columnX = { geometry.categoryX(index - categoryPosition.value) },
                    columnAlpha = { (1f - distance()).coerceIn(0f, 1f) },
                    metrics = metrics,
                    geometry = geometry,
                    fontFamily = fontFamily,
                    glow = glow,
                    listsEnabled = listsEnabled,
                )
            }
        }
    }
}

@Composable
private fun CategoryIcon(
    icon: XmbIcon,
    size: Dp,
    geometry: XmbBarGeometry,
    index: Int,
    position: () -> Float,
    distance: () -> Float,
    glow: () -> Float,
) {
    Box(
        Modifier
            .placeAt(alignX = 0.5f, alignY = 0.5f) { Offset(geometry.categoryX(index - position()), geometry.barY) }
            .size(size)
            .graphicsLayer {
                val focus = (1f - distance()).coerceIn(0f, 1f)
                val scale = lerp(XmbMotion.UNFOCUSED_CATEGORY_SCALE, 1f, focus)
                scaleX = scale
                scaleY = scale
                alpha = lerp(XmbMotion.UNFOCUSED_CATEGORY_ALPHA, 1f, focus)
            }.focusGlow { (1f - distance()).coerceIn(0f, 1f) * glow() },
    ) {
        XmbIconImage(icon, Modifier.size(size))
    }
}

@Composable
private fun ItemColumn(
    category: XmbCategory,
    focusedItem: Int,
    isFocusedCategory: Boolean,
    itemPosition: () -> Float,
    columnX: () -> Float,
    columnAlpha: () -> Float,
    metrics: XmbMetrics,
    geometry: XmbBarGeometry,
    fontFamily: FontFamily,
    glow: () -> Float,
    listsEnabled: Boolean,
) {
    val density = LocalDensity.current
    val titleSize = with(density) { metrics.itemTitleSize.toSp() }
    val subtitleSize = with(density) { metrics.itemSubtitleSize.toSp() }
    val glowRadius = with(density) { metrics.unit.toPx() * 8f }
    category.items.forEachIndexed { index, item ->
        if (index < focusedItem - ITEMS_ABOVE || index > focusedItem + ITEMS_BELOW) return@forEachIndexed
        key(item.id) {
            val focused = isFocusedCategory && index == focusedItem
            val relative = { index - itemPosition() }
            val focus = { (1f - abs(relative())).coerceIn(0f, 1f) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .placeAt(alignY = 0.5f) {
                            Offset(columnX() - geometry.itemIcon / 2f, geometry.itemY(relative()))
                        }.graphicsLayer { alpha = columnAlpha() * lerp(XmbMotion.UNFOCUSED_ITEM_ALPHA, 1f, focus()) },
            ) {
                Box(
                    Modifier
                        .size(metrics.itemIcon)
                        .graphicsLayer {
                            val scale = lerp(XmbMotion.UNFOCUSED_ITEM_SCALE, 1f, focus())
                            scaleX = scale
                            scaleY = scale
                        }.focusGlow { if (isFocusedCategory) focus() * glow() else 0f },
                ) {
                    XmbIconImage(item.icon, Modifier.size(metrics.itemIcon))
                }
                Spacer(Modifier.width(metrics.itemTextOffset))
                Column {
                    BasicText(
                        text = stringResource(item.title),
                        style =
                            TextStyle(
                                color = Color.White,
                                fontFamily = fontFamily,
                                fontSize = titleSize,
                                shadow = if (focused) Shadow(Color.White.copy(alpha = 0.6f), Offset.Zero, glowRadius) else null,
                            ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val available = item.action != XmbItemAction.NONE || (item.list != null && listsEnabled)
                    val subtitle = item.subtitle ?: Res.string.xmb_item_unavailable.takeIf { !available }
                    if (focused && subtitle != null) {
                        BasicText(
                            text = stringResource(subtitle),
                            style = TextStyle(color = Color.White.copy(alpha = 0.75f), fontFamily = fontFamily, fontSize = subtitleSize),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun XmbIconImage(
    icon: XmbIcon,
    modifier: Modifier = Modifier,
) {
    when (icon) {
        is XmbIcon.Drawable -> {
            Image(
                painterResource(icon.resource),
                contentDescription = null,
                modifier = modifier,
                contentScale = ContentScale.Fit,
            )
        }

        is XmbIcon.Vector -> {
            Image(icon.image, contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
        }
    }
}

/** A soft white halo behind focused icons; [strength] is read at draw time. */
internal fun Modifier.focusGlow(strength: () -> Float): Modifier =
    drawBehind {
        val alpha = strength()
        if (alpha <= 0f) return@drawBehind
        val radius = size.minDimension * 0.8f
        drawCircle(
            Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.32f * alpha), Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }

/**
 * Places content so its point at ([alignX], [alignY]) of its own size lands on [anchor] (pixels,
 * relative to the parent). The anchor is read during placement only.
 */
internal fun Modifier.placeAt(
    alignX: Float = 0f,
    alignY: Float = 0f,
    anchor: () -> Offset,
): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(placeable.width, placeable.height) {
            val point = anchor()
            placeable.place(
                (point.x - placeable.width * alignX).roundToInt(),
                (point.y - placeable.height * alignY).roundToInt(),
            )
        }
    }

internal fun lerp(
    from: Float,
    to: Float,
    fraction: Float,
): Float = from + (to - from) * fraction
