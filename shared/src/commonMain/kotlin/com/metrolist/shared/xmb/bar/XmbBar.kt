package com.metrolist.shared.xmb.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.xmb_cat_users
import com.metrolist.shared.generated.resources.xmb_item_artist
import com.metrolist.shared.generated.resources.xmb_item_unavailable
import com.metrolist.shared.xmb.nav.XmbCategory
import com.metrolist.shared.xmb.nav.XmbIcon
import com.metrolist.shared.xmb.nav.XmbItemAction
import com.metrolist.shared.xmb.nav.XmbNavigation
import com.metrolist.shared.xmb.nav.resolve
import com.metrolist.shared.xmb.theme.XmbMetrics
import com.metrolist.shared.xmb.theme.XmbMotion
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.max
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
    val itemsBelowGap: Float = 0f,
    val parentOffset: Float = itemIcon * 2f,
) {
    fun categoryX(offset: Float): Float = anchorX + offset * categorySpacing

    /** Where the focused item of a column, or the focused row of an open level, sits. */
    val focusY: Float get() = barY + focusedItemOffset

    /**
     * Center of an item [relative] places from the focused one. The focused item has extra room
     * below it for its subtitle; items above it jump over the category row. Both gaps blend in
     * over one step, so scrolling stays smooth.
     */
    fun itemY(relative: Float): Float =
        focusY + relative * itemSpacing +
            itemsBelowGap * relative.coerceIn(0f, 1f) -
            itemsAboveGap * (-relative).coerceIn(0f, 1f)

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
            shadow =
                Shadow(
                    Color.Black.copy(alpha = 0.25f),
                    Offset(0f, with(density) { metrics.unit.toPx() }),
                    with(density) {
                        metrics.unit.toPx() *
                            3f
                    },
                ),
        )
    categories.forEachIndexed { index, category ->
        if (abs(index - navigation.category) > geometry.visibleCategories) return@forEachIndexed
        key(category.id) {
            val distance = { abs(index - categoryPosition.value) }
            // Only what's in view takes taps: the icons and the focused column, at the top level (an
            // opened folder fades them out; a neighbor column is invisible at rest but still laid out).
            val atTop = navigation.stack.isEmpty()
            CategoryIcon(category.icon, metrics.categoryIcon, geometry, index, categoryPosition::value, distance, tappable = atTop)
            BasicText(
                text = stringResource(category.title),
                style = labelStyle,
                maxLines = 1,
                softWrap = false,
                modifier =
                    Modifier
                        .placeAt(alignX = 0.5f, alignY = 0.5f) {
                            Offset(geometry.categoryX(index - categoryPosition.value), geometry.barY + geometry.categoryLabelOffset)
                        }.graphicsLayer { alpha = (1f - 2f * distance()).coerceIn(0f, 1f) },
            )
            if (abs(index - navigation.category) <= 1) {
                ItemColumn(
                    category = category,
                    focusedItem = navigation.items.getOrElse(index) { 0 },
                    isFocusedCategory = index == navigation.category,
                    rowsTappable = atTop && index == navigation.category,
                    itemPosition = itemPositions[index]::value,
                    columnX = { geometry.categoryX(index - categoryPosition.value) },
                    columnAlpha = { (1f - distance()).coerceIn(0f, 1f) },
                    metrics = metrics,
                    geometry = geometry,
                    fontFamily = fontFamily,
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
    tappable: Boolean,
) {
    val taps = LocalXmbTaps.current
    Box(
        Modifier
            .placeAt(alignX = 0.5f, alignY = 0.5f) { Offset(geometry.categoryX(index - position()), geometry.barY) }
            .size(size)
            .then(if (tappable) Modifier.xmbTap { taps?.category(index) } else Modifier)
            .graphicsLayer {
                val focus = (1f - distance()).coerceIn(0f, 1f)
                val scale = lerp(XmbMotion.UNFOCUSED_CATEGORY_SCALE, 1f, focus)
                scaleX = scale
                scaleY = scale
                alpha = lerp(XmbMotion.UNFOCUSED_CATEGORY_ALPHA, 1f, focus)
            },
    ) {
        XmbIconImage(icon, Modifier.size(size))
    }
}

@Composable
private fun ItemColumn(
    category: XmbCategory,
    focusedItem: Int,
    isFocusedCategory: Boolean,
    rowsTappable: Boolean,
    itemPosition: () -> Float,
    columnX: () -> Float,
    columnAlpha: () -> Float,
    metrics: XmbMetrics,
    geometry: XmbBarGeometry,
    fontFamily: FontFamily,
    listsEnabled: Boolean,
) {
    val styles = rememberXmbRowStyles(metrics, fontFamily)
    val taps = LocalXmbTaps.current
    category.items.forEachIndexed { index, item ->
        if (index < focusedItem - ITEMS_ABOVE || index > focusedItem + ITEMS_BELOW) return@forEachIndexed
        key(item.id) {
            val focused = isFocusedCategory && index == focusedItem
            val relative = { index - itemPosition() }
            val focus = { (1f - abs(relative())).coerceIn(0f, 1f) }
            val available = item.action != XmbItemAction.None || (item.list != null && listsEnabled)
            val subtitle = item.subtitle?.resolve() ?: stringResource(Res.string.xmb_item_unavailable).takeIf { !available }
            XmbRow(
                title = item.title.resolve(),
                subtitle = subtitle.takeIf { focused },
                value = null,
                focused = focused,
                styles = styles,
                metrics = metrics,
                modifier =
                    Modifier
                        .placeAt(alignY = 0.5f) {
                            Offset(columnX() - geometry.itemIcon / 2f, geometry.itemY(relative()))
                        }.graphicsLayer { alpha = columnAlpha() * lerp(XmbMotion.UNFOCUSED_ITEM_ALPHA, 1f, focus()) }
                        .then(
                            if (rowsTappable) {
                                Modifier.xmbTap(onLongPress = { taps?.row(index, true) }) { taps?.row(index, false) }
                            } else {
                                Modifier
                            },
                        ),
                iconScale = { lerp(XmbMotion.UNFOCUSED_ITEM_SCALE, 1f, focus()) },
            ) {
                XmbArtwork(item.artwork, item.icon, metrics.itemIcon)
            }
        }
    }
}

/** Text styles shared by item rows, level rows and pickers. */
internal class XmbRowStyles(
    val title: TextStyle,
    val focusedTitle: TextStyle,
    val subtitle: TextStyle,
    val value: TextStyle,
)

@Composable
internal fun rememberXmbRowStyles(
    metrics: XmbMetrics,
    fontFamily: FontFamily,
): XmbRowStyles {
    val density = LocalDensity.current
    return remember(metrics, fontFamily, density) {
        with(density) {
            val title = TextStyle(color = Color.White, fontFamily = fontFamily, fontSize = metrics.itemTitleSize.toSp())
            XmbRowStyles(
                title = title,
                focusedTitle = title.copy(shadow = Shadow(Color.White.copy(alpha = 0.6f), Offset.Zero, metrics.unit.toPx() * 8f)),
                subtitle =
                    TextStyle(
                        color = Color.White.copy(alpha = 0.85f),
                        fontFamily = fontFamily,
                        fontSize = metrics.itemSubtitleSize.toSp(),
                    ),
                value = title.copy(color = Color.White.copy(alpha = 0.9f)),
            )
        }
    }
}

/**
 * One row: icon, then the title centered on the icon with the subtitle below it, and an optional
 * [value] in a column further right (settings show their current value there, like the console).
 * [iconScale] is read in the draw phase.
 */
@Composable
internal fun XmbRow(
    title: String,
    subtitle: String?,
    value: String?,
    focused: Boolean,
    styles: XmbRowStyles,
    metrics: XmbMetrics,
    modifier: Modifier = Modifier,
    iconScale: () -> Float = { 1f },
    icon: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            Modifier
                .size(metrics.itemIcon)
                .graphicsLayer {
                    val scale = iconScale()
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            icon()
        }
        Spacer(Modifier.width(metrics.itemTextOffset))
        XmbTitleBlock(
            title = title,
            subtitle = subtitle,
            titleStyle = if (focused) styles.focusedTitle else styles.title,
            subtitleStyle = styles.subtitle,
            maxTitleWidth = if (value != null) metrics.valueColumn - metrics.unit * 16 else null,
        )
        if (value != null) {
            // The title block is exactly this wide when there's a value, so values line up in a column.
            Spacer(Modifier.width(metrics.unit * 16))
            BasicText(
                text = value,
                style = if (focused) styles.focusedTitle else styles.value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The title, vertically centered where the row is centered (on the icon), with the subtitle
 * hanging below it, as on the console. Reserves the subtitle's height above the title too, so the
 * block stays symmetric around the title.
 */
@Composable
private fun XmbTitleBlock(
    title: String,
    subtitle: String?,
    titleStyle: TextStyle,
    subtitleStyle: TextStyle,
    maxTitleWidth: Dp?,
) {
    val density = LocalDensity.current
    Layout(
        content = {
            BasicText(title, style = titleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) BasicText(subtitle, style = subtitleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
    ) { measurables, constraints ->
        val limit = maxTitleWidth?.let { with(density) { it.roundToPx() } }
        val loose =
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxWidth =
                    limit?.coerceAtMost(constraints.maxWidth) ?: constraints.maxWidth,
            )
        val titlePlaceable = measurables[0].measure(loose)
        val subtitlePlaceable = measurables.getOrNull(1)?.measure(loose)
        val below = subtitlePlaceable?.height ?: 0
        val width = limit ?: max(titlePlaceable.width, subtitlePlaceable?.width ?: 0)
        layout(width, titlePlaceable.height + 2 * below) {
            titlePlaceable.place(0, below)
            subtitlePlaceable?.place(0, below + titlePlaceable.height)
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
            val scale = glyphScale(icon.resource)
            Image(
                painterResource(icon.resource),
                contentDescription = null,
                modifier =
                    modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentScale = ContentScale.Fit,
            )
        }

        is XmbIcon.Vector -> {
            Image(icon.image, contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
        }
    }
}

/**
 * The face icon (users, artist) is a 68 px source whose glyph fills 71% of it, against about 50 to
 * 63% for the rest of the set, so it would draw much heavier; this brings it in line.
 */
private fun glyphScale(resource: DrawableResource): Float =
    if (resource == Res.drawable.xmb_cat_users || resource == Res.drawable.xmb_item_artist) FACE_ICON_SCALE else 1f

private const val FACE_ICON_SCALE = 0.78f

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
