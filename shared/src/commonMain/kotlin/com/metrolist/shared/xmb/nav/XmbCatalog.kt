@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.metrolist.shared.xmb.nav

import com.metrolist.shared.generated.resources.*
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.theme.XmbIcons
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/**
 * The XMB's categories and their items. Music items open drill-down lists (M2); the rest are
 * placeholders for later milestones (PLAN.md). The Metrolist interface item switches back to the
 * stock screens.
 */
fun xmbCategories(): List<XmbCategory> =
    listOf(
        category(
            XmbCategoryId.USERS,
            Res.string.xmb_category_users,
            drawable(Res.drawable.xmb_cat_users),
            item("account", Res.string.account, Res.drawable.xmb_item_artist),
        ),
        category(
            XmbCategoryId.SETTINGS,
            Res.string.settings,
            drawable(Res.drawable.xmb_cat_settings),
            XmbItem(
                id = "interface_style",
                title = Res.string.xmb_switch_to_metrolist,
                icon = drawable(Res.drawable.xmb_item_subsetting),
                subtitle = Res.string.xmb_switch_to_metrolist_description,
                action = XmbItemAction.SWITCH_TO_METROLIST,
            ),
            item("playback", Res.string.settings_playback, Res.drawable.xmb_item_volume),
            item("audio_quality", Res.string.audio_quality, Res.drawable.xmb_item_subsetting),
            item("metroeq", Res.string.metroeq, Res.drawable.xmb_item_equalizer),
            XmbItem("visualizer", Res.string.xmb_category_visualizer, XmbIcon.Vector(XmbIcons.Visualizer)),
            item("lyrics", Res.string.lyrics, Res.drawable.xmb_item_subsetting),
            item("cast", Res.string.settings_cast_title, Res.drawable.xmb_item_network_settings),
            item("storage", Res.string.storage, Res.drawable.xmb_item_subsetting),
            item("about", Res.string.about, Res.drawable.xmb_item_info),
        ),
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
        category(
            XmbCategoryId.SEARCH,
            Res.string.search,
            drawable(Res.drawable.xmb_cat_search),
            item("search", Res.string.search, Res.drawable.xmb_cat_search),
        ),
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
            visualizer("waves", Res.string.xmb_visualizer_waves),
            visualizer("valley", Res.string.xmb_visualizer_valley),
            visualizer("gaia", Res.string.xmb_visualizer_gaia),
            visualizer("random", Res.string.xmb_visualizer_random),
            visualizer("auto_cycle", Res.string.xmb_visualizer_auto_cycle),
        ),
    )

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

private fun visualizer(
    id: String,
    title: StringResource,
) = XmbItem("visualizer_$id", title, XmbIcon.Vector(XmbIcons.Visualizer))
