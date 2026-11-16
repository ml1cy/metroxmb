@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.metrolist.shared.xmb.nav

import com.metrolist.shared.generated.resources.*
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.ui.settings.SettingsUiRegistry
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.theme.XmbIcons
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/**
 * The XMB's categories and their items. Users, Settings and Search take their items from live
 * data ([xmbUserItems], [xmbSettingsItems], [xmbSearchItems]); the defaults are what the XMB shows
 * without a host (dev snapshots; Settings is empty without a settings registry). Podcasts, Video, Friends and Visualizer are placeholders for later
 * milestones (PLAN.md).
 */
fun xmbCategories(
    users: List<XmbItem> = xmbUserItems(null),
    settings: List<XmbItem> = emptyList(),
    search: List<XmbItem> = xmbSearchItems(emptyList()),
): List<XmbCategory> =
    listOf(
        XmbCategory(XmbCategoryId.USERS, Res.string.xmb_category_users, drawable(Res.drawable.xmb_cat_users), users),
        XmbCategory(XmbCategoryId.SETTINGS, Res.string.settings, drawable(Res.drawable.xmb_cat_settings), settings),
        category(
            XmbCategoryId.MUSIC,
            Res.string.xmb_category_music,
            drawable(Res.drawable.xmb_cat_music),
            item("home", Res.string.home, Res.drawable.xmb_item_playlist, XmbList.Home),
            item("quick_picks", Res.string.quick_picks, Res.drawable.xmb_item_song, XmbList.QuickPicks),
            item("liked", Res.string.liked_songs, Res.drawable.xmb_item_liked, XmbList.Liked),
            item("playlists", Res.string.playlists, Res.drawable.xmb_item_playlist, XmbList.Playlists),
            item("albums", Res.string.albums, Res.drawable.xmb_item_album, XmbList.Albums),
            item("artists", Res.string.artists, Res.drawable.xmb_item_artist, XmbList.Artists),
            item("history", Res.string.menu_history, Res.drawable.xmb_item_history, XmbList.History),
            item("downloads", Res.string.downloads, Res.drawable.xmb_item_playlist, XmbList.Downloads),
        ),
        XmbCategory(XmbCategoryId.SEARCH, Res.string.search, drawable(Res.drawable.xmb_cat_search), search),
        category(
            XmbCategoryId.PODCASTS,
            Res.string.podcasts,
            XmbIcon.Vector(XmbIcons.Podcasts),
            item("subscriptions", Res.string.xmb_item_subscriptions, Res.drawable.xmb_item_playlist),
            item("episodes", Res.string.filter_episodes, Res.drawable.xmb_item_song),
        ),
        category(
            XmbCategoryId.VIDEO,
            Res.string.video,
            drawable(Res.drawable.xmb_cat_video),
            item("music_videos", Res.string.xmb_item_music_videos, Res.drawable.xmb_item_music_video),
            item("live", Res.string.xmb_item_live, Res.drawable.xmb_item_music_video),
        ),
        category(
            XmbCategoryId.FRIENDS,
            Res.string.xmb_category_friends,
            drawable(Res.drawable.xmb_cat_friends),
            item("listen_together", Res.string.listen_together, Res.drawable.xmb_item_listen_together),
            item("lastfm_friends", Res.string.xmb_item_lastfm_friends, Res.drawable.xmb_item_artist),
        ),
        category(
            XmbCategoryId.VISUALIZER,
            Res.string.xmb_category_visualizer,
            XmbIcon.Vector(XmbIcons.Visualizer),
            visualizer("waves", Res.string.xmb_visualizer_waves, XmbVisualizerKind.WAVES),
            visualizer("valley", Res.string.xmb_visualizer_valley),
            visualizer("gaia", Res.string.xmb_visualizer_gaia),
            visualizer("random", Res.string.xmb_visualizer_random),
            visualizer("auto_cycle", Res.string.xmb_visualizer_auto_cycle),
        ),
    )

/** The signed-in account, as the Users column shows it. */
data class XmbAccountInfo(
    val signedIn: Boolean,
    val name: String? = null,
    val detail: String? = null,
    val thumbnail: String? = null,
)

/**
 * Users: Log in when signed out; when signed in, the account (selecting it switches channel) and
 * Log out. Without [account] (no host) the column shows an unavailable Account row.
 */
fun xmbUserItems(account: XmbAccountInfo?): List<XmbItem> {
    val userIcon = drawable(Res.drawable.xmb_cat_users)
    return when {
        account == null -> {
            listOf(XmbItem("account", Res.string.account, userIcon))
        }

        !account.signedIn -> {
            listOf(
                XmbItem("sign_in", Res.string.action_login, userIcon, Res.string.xmb_users_sign_in_description, XmbItemAction.SignIn),
            )
        }

        else -> {
            listOf(
                XmbItem(
                    id = "account",
                    title = account.name?.takeIf(String::isNotBlank)?.let(XmbText::Plain) ?: XmbText.Resource(Res.string.account),
                    icon = userIcon,
                    subtitle =
                        account.detail?.takeIf(String::isNotBlank)?.let(XmbText::Plain) ?: XmbText.Resource(Res.string.xmb_users_signed_in),
                    action = XmbItemAction.SwitchChannel,
                    artwork = account.thumbnail?.takeIf(String::isNotBlank),
                ),
                XmbItem(
                    "switch_channel",
                    Res.string.xmb_users_switch_channel,
                    drawable(Res.drawable.xmb_item_listen_together),
                    Res.string.xmb_users_switch_channel_description,
                    XmbItemAction.SwitchChannel,
                ),
                XmbItem(
                    "sign_out",
                    Res.string.action_logout,
                    drawable(Res.drawable.xmb_item_remove),
                    Res.string.xmb_users_sign_out_description,
                    XmbItemAction.SignOut,
                ),
            )
        }
    }
}

/** Search: a new search on the on-screen keyboard, then the recent searches, newest first. */
fun xmbSearchItems(history: List<String>): List<XmbItem> =
    listOf(
        XmbItem(
            "new_search",
            Res.string.xmb_search_new,
            drawable(Res.drawable.xmb_cat_search),
            Res.string.xmb_search_new_description,
            XmbItemAction.OpenSearch,
        ),
    ) +
        history.distinct().map { query ->
            XmbItem(
                id = "recent:$query",
                title = XmbText.Plain(query),
                icon = drawable(Res.drawable.xmb_item_history),
                subtitle = XmbText.Resource(Res.string.xmb_search_recent),
                list = XmbList.SearchResults(query),
            )
        }

/**
 * Settings pages from the shared settings registry, in the stock root order. Pages the XMB draws
 * as rows open a level; custom pages (storage, backup, updates, MetroEQ...) open their stock screen.
 */
fun xmbSettingsItems(registry: SettingsUiRegistry): List<XmbItem> =
    registry.rootGroups
        .flatMap { it.pages }
        .distinct()
        .mapNotNull { id -> registry[id] }
        .map { page ->
            XmbItem(
                id = "settings:${page.id.name}",
                title = XmbText.Plain(page.title),
                icon = drawable(settingsPageIcon(page.id)),
                subtitle = page.subtitle?.takeIf(String::isNotBlank)?.let(XmbText::Plain),
                action = if (page.custom) XmbItemAction.StockSettings(page.id) else XmbItemAction.None,
                list = if (page.custom) null else XmbList.SettingsPage(page.id),
            )
        }

internal fun settingsPageIcon(page: SettingsPageId): DrawableResource =
    when (page) {
        SettingsPageId.PLAYER, SettingsPageId.PLAYER_QUEUE -> Res.drawable.xmb_item_volume
        SettingsPageId.EQUALIZER -> Res.drawable.xmb_item_equalizer
        SettingsPageId.ABOUT -> Res.drawable.xmb_item_info
        SettingsPageId.LISTEN_TOGETHER -> Res.drawable.xmb_item_listen_together
        SettingsPageId.DEVICE_CONNECT -> Res.drawable.xmb_item_network_settings
        SettingsPageId.CONTENT, SettingsPageId.CONTENT_HOME -> Res.drawable.xmb_item_playlist
        else -> Res.drawable.xmb_item_subsetting
    }

private fun category(
    id: XmbCategoryId,
    title: StringResource,
    icon: XmbIcon,
    vararg items: XmbItem,
) = XmbCategory(id, title, icon, items.toList())

private fun drawable(resource: DrawableResource) = XmbIcon.Drawable(resource)

private fun item(
    id: String,
    title: StringResource,
    icon: DrawableResource,
    list: XmbList? = null,
) = XmbItem(id, title, drawable(icon), list = list)

/** Visualizer items; those without a [kind] yet (Valley, Gaia, random, auto-cycle) arrive in M5. */
private fun visualizer(
    id: String,
    title: StringResource,
    kind: XmbVisualizerKind? = null,
) = XmbItem(
    "visualizer_$id",
    title,
    XmbIcon.Vector(XmbIcons.Visualizer),
    action = kind?.let(XmbItemAction::OpenVisualizer) ?: XmbItemAction.None,
)
