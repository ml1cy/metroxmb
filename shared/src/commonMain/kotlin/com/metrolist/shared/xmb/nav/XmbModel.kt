package com.metrolist.shared.xmb.nav

import androidx.compose.ui.graphics.vector.ImageVector
import com.metrolist.shared.xmb.adapters.XmbList
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/** The horizontal categories, in display order. */
enum class XmbCategoryId {
    USERS,
    SETTINGS,
    MUSIC,
    SEARCH,
    PODCASTS,
    VIDEO,
    FRIENDS,
    VISUALIZER,
}

sealed interface XmbIcon {
    data class Drawable(
        val resource: DrawableResource,
    ) : XmbIcon

    data class Vector(
        val image: ImageVector,
    ) : XmbIcon
}

/**
 * What selecting an item does besides opening its [XmbItem.list]. Items with neither play the
 * error sound.
 */
enum class XmbItemAction {
    NONE,
    SWITCH_TO_METROLIST,
}

data class XmbItem(
    val id: String,
    val title: StringResource,
    val icon: XmbIcon,
    val subtitle: StringResource? = null,
    val action: XmbItemAction = XmbItemAction.NONE,
    val list: XmbList? = null,
) {
    val isAvailable: Boolean get() = action != XmbItemAction.NONE || list != null
}

data class XmbCategory(
    val id: XmbCategoryId,
    val title: StringResource,
    val icon: XmbIcon,
    val items: List<XmbItem>,
)
