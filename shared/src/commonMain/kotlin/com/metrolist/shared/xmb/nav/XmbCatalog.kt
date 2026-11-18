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
    friends: List<XmbItem> = xmbFriendItems(null),
    canRecognize: Boolean = false,
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
            XmbItem(
                "lyrics",
                Res.string.lyrics,
                drawable(Res.drawable.xmb_item_song),
                Res.string.xmb_lyrics_description,
                XmbItemAction.OpenLyrics,
            ),
            item("play_on_device", Res.string.xmb_cast_title, Res.drawable.xmb_item_network_settings, XmbList.CastDevices),
            XmbItem(
                "play_queue",
                Res.string.xmb_queue,
                drawable(Res.drawable.xmb_item_playlist),
                Res.string.xmb_queue_description,
                list = XmbList.Queue,
            ),
            *listOfNotNull(
                XmbItem(
                    "identify_song",
                    Res.string.recognize_music,
                    drawable(Res.drawable.xmb_item_song),
                    Res.string.xmb_recognize_description,
                    XmbItemAction.IdentifySong,
                ).takeIf { canRecognize },
            ).toTypedArray(),
            XmbItem(
                "sleep_timer",
                Res.string.sleep_timer,
                drawable(Res.drawable.xmb_item_history),
                Res.string.xmb_sleep_timer_description,
                XmbItemAction.SleepTimer,
            ),
        ),
        XmbCategory(XmbCategoryId.SEARCH, Res.string.search, drawable(Res.drawable.xmb_cat_search), search),
        category(
            XmbCategoryId.PODCASTS,
            Res.string.podcasts,
            XmbIcon.Vector(XmbIcons.Podcasts),
            item("subscriptions", Res.string.xmb_item_subscriptions, Res.drawable.xmb_item_playlist, XmbList.Podcasts),
            item("new_episodes", Res.string.new_episodes, Res.drawable.xmb_item_song, XmbList.NewEpisodes),
            item("saved_episodes", Res.string.episodes_for_later, Res.drawable.xmb_item_history, XmbList.SavedEpisodes),
        ),
        category(
            XmbCategoryId.VIDEO,
            Res.string.video,
            drawable(Res.drawable.xmb_cat_video),
            XmbItem(
                "watch_video",
                Res.string.xmb_video_watch,
                drawable(Res.drawable.xmb_item_music_video),
                Res.string.xmb_video_watch_description,
                XmbItemAction.WatchVideo,
            ),
            item("liked_videos", Res.string.xmb_video_liked, Res.drawable.xmb_item_liked, XmbList.LikedVideos),
            item("feed_videos", Res.string.xmb_item_music_videos, Res.drawable.xmb_item_music_video, XmbList.FeedVideos),
        ),
        XmbCategory(XmbCategoryId.FRIENDS, Res.string.xmb_category_friends, drawable(Res.drawable.xmb_cat_friends), friends),
        category(
            XmbCategoryId.VISUALIZER,
            Res.string.xmb_category_visualizer,
            XmbIcon.Vector(XmbIcons.Visualizer),
            visualizer("waves", Res.string.xmb_visualizer_waves, XmbItemAction.OpenVisualizer(XmbVisualizerKind.WAVES)),
            visualizer("valley", Res.string.xmb_visualizer_valley, XmbItemAction.OpenVisualizer(XmbVisualizerKind.VALLEY)),
            visualizer("gaia", Res.string.xmb_visualizer_gaia, XmbItemAction.OpenVisualizer(XmbVisualizerKind.GAIA)),
            visualizer("random", Res.string.xmb_visualizer_random, XmbItemAction.OpenRandomVisualizer),
            visualizer("auto_cycle", Res.string.xmb_visualizer_auto_cycle, XmbItemAction.CycleVisualizers),
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

/** Listen Together as the Friends column sees it. */
data class XmbFriendsInfo(
    val enabled: Boolean,
    val username: String = "",
    val roomCode: String? = null,
    val isHost: Boolean = false,
    val members: Int = 0,
    val waitingToJoin: Boolean = false,
    val joinRequests: List<Pair<String, String>> = emptyList(),
)

/**
 * Friends (Listen Together): turn it on; outside a room, create or join one and set your name;
 * waiting for a host, cancel; in a room, its code (selecting copies the invite link), the people in
 * it, join requests for the host to approve, and Leave room. Without [info] (no host) the column
 * shows an unavailable Listen Together row.
 */
fun xmbFriendItems(info: XmbFriendsInfo?): List<XmbItem> {
    val together = drawable(Res.drawable.xmb_item_listen_together)
    return when {
        info == null -> {
            listOf(XmbItem("listen_together", Res.string.listen_together, together))
        }

        !info.enabled -> {
            listOf(
                XmbItem(
                    "enable",
                    Res.string.xmb_friends_enable,
                    together,
                    Res.string.xmb_friends_enable_description,
                    XmbItemAction.EnableListenTogether,
                ),
            )
        }

        info.roomCode != null -> {
            buildList {
                add(
                    XmbItem(
                        id = "room",
                        title = XmbText.Plain(info.roomCode),
                        icon = together,
                        subtitle =
                            XmbText.Resource(
                                if (info.isHost) Res.string.xmb_friends_room_host else Res.string.xmb_friends_room_guest,
                            ),
                        action = XmbItemAction.CopyRoomLink,
                    ),
                )
                add(
                    XmbItem(
                        id = "members",
                        title = XmbText.Resource(Res.string.xmb_friends_members),
                        icon = drawable(Res.drawable.xmb_cat_users),
                        subtitle = XmbText.Plain(info.members.toString()),
                        list = XmbList.RoomMembers,
                    ),
                )
                info.joinRequests.forEach { (userId, name) ->
                    add(
                        XmbItem(
                            id = "request:$userId",
                            title = XmbText.Plain(name),
                            icon = drawable(Res.drawable.xmb_cat_users),
                            subtitle = XmbText.Resource(Res.string.xmb_friends_join_request),
                            action = XmbItemAction.ReviewJoin(userId, name),
                        ),
                    )
                }
                add(XmbItem("leave", Res.string.leave_room, drawable(Res.drawable.xmb_item_remove), action = XmbItemAction.LeaveRoom))
            }
        }

        info.waitingToJoin -> {
            listOf(
                XmbItem(
                    "waiting",
                    Res.string.xmb_friends_waiting,
                    together,
                    Res.string.xmb_friends_waiting_description,
                    XmbItemAction.CancelJoin,
                ),
            )
        }

        else -> {
            listOf(
                XmbItem("create", Res.string.create_room, together, Res.string.xmb_friends_create_description, XmbItemAction.CreateRoom),
                XmbItem("join", Res.string.join_room, together, Res.string.xmb_friends_join_description, XmbItemAction.JoinRoom),
                XmbItem(
                    id = "name",
                    title = XmbText.Resource(Res.string.xmb_friends_name),
                    icon = drawable(Res.drawable.xmb_cat_users),
                    subtitle =
                        info.username.takeIf(String::isNotBlank)?.let(XmbText::Plain)
                            ?: XmbText.Resource(Res.string.xmb_friends_name_missing),
                    action = XmbItemAction.EditUsername,
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
 * as rows open a level: registry pages, and the custom pages in [drawn] (About, MetroEQ, Storage,
 * Backup); other custom pages open their stock screen.
 */
fun xmbSettingsItems(
    registry: SettingsUiRegistry,
    drawn: Set<SettingsPageId> = emptySet(),
    hidden: Set<SettingsPageId> = emptySet(),
): List<XmbItem> =
    registry.rootGroups
        .flatMap { it.pages }
        .distinct()
        .filterNot { it in hidden }
        .mapNotNull { id -> registry[id] }
        .map { page ->
            XmbItem(
                id = "settings:${page.id.name}",
                title = XmbText.Plain(page.title),
                icon = drawable(settingsPageIcon(page.id)),
                subtitle = page.subtitle?.takeIf(String::isNotBlank)?.let(XmbText::Plain),
                action = if (page.custom && page.id !in drawn) XmbItemAction.StockSettings(page.id) else XmbItemAction.None,
                list = if (page.custom && page.id !in drawn) null else XmbList.SettingsPage(page.id),
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

/** A Visualizer item: one visualizer, a random one, or all in turn. */
private fun visualizer(
    id: String,
    title: StringResource,
    action: XmbItemAction,
) = XmbItem(
    "visualizer_$id",
    title,
    XmbIcon.Vector(XmbIcons.Visualizer),
    action = action,
)
