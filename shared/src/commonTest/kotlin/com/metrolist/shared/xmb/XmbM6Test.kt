package com.metrolist.shared.xmb

import com.metrolist.shared.cast.CastConnectionStatus
import com.metrolist.shared.cast.CastDevice
import com.metrolist.shared.cast.CastProtocol
import com.metrolist.shared.cast.CastState
import com.metrolist.shared.listentogether.network.UserInfo
import com.metrolist.shared.model.LyricsEntry
import com.metrolist.shared.model.PodcastItem
import com.metrolist.shared.xmb.adapters.XmbEntryKind
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.adapters.castEntries
import com.metrolist.shared.xmb.adapters.currentLyricIndex
import com.metrolist.shared.xmb.adapters.matches
import com.metrolist.shared.xmb.adapters.memberEntries
import com.metrolist.shared.xmb.adapters.toXmbEntry
import com.metrolist.shared.xmb.nav.XmbCategoryId
import com.metrolist.shared.xmb.nav.XmbFriendsInfo
import com.metrolist.shared.xmb.nav.XmbItemAction
import com.metrolist.shared.xmb.nav.XmbText
import com.metrolist.shared.xmb.nav.xmbCategories
import com.metrolist.shared.xmb.nav.xmbFriendItems
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XmbM6Test {
    private fun column(id: XmbCategoryId) = xmbCategories().single { it.id == id }.items

    @Test
    fun podcastsVideoAndMusicColumnsOpenTheirLists() {
        assertEquals(listOf(XmbList.Podcasts, XmbList.NewEpisodes, XmbList.SavedEpisodes), column(XmbCategoryId.PODCASTS).map { it.list })
        val video = column(XmbCategoryId.VIDEO)
        assertEquals(XmbItemAction.WatchVideo, video.first().action)
        assertEquals(listOf(XmbList.LikedVideos, XmbList.FeedVideos), video.drop(1).map { it.list })
        val music = column(XmbCategoryId.MUSIC)
        assertTrue(music.any { it.action == XmbItemAction.OpenLyrics })
        assertTrue(music.any { it.list == XmbList.CastDevices })
        assertTrue(
            xmbCategories().all { category ->
                category.items.all { it.isAvailable } ||
                    category.id in setOf(XmbCategoryId.USERS, XmbCategoryId.FRIENDS, XmbCategoryId.VISUALIZER)
            },
        )
    }

    @Test
    fun podcastsOpenTheirEpisodes() {
        val podcast = PodcastItem(id = "MPSP1", title = "Show", author = null, thumbnail = null)
        assertEquals(XmbList.Podcast(podcast), podcast.toXmbEntry().open)
    }

    @Test
    fun friendsFollowListenTogethersState() {
        assertFalse(xmbFriendItems(null).single().isAvailable)
        assertEquals(listOf(XmbItemAction.EnableListenTogether), xmbFriendItems(XmbFriendsInfo(enabled = false)).map { it.action })
        val idle = xmbFriendItems(XmbFriendsInfo(enabled = true))
        assertEquals(listOf(XmbItemAction.CreateRoom, XmbItemAction.JoinRoom, XmbItemAction.EditUsername), idle.map { it.action })
        assertEquals(XmbText.Plain("Ada"), xmbFriendItems(XmbFriendsInfo(enabled = true, username = "Ada"))[2].subtitle)
        assertEquals(
            listOf(XmbItemAction.CancelJoin),
            xmbFriendItems(XmbFriendsInfo(enabled = true, waitingToJoin = true)).map { it.action },
        )
        val room =
            xmbFriendItems(
                XmbFriendsInfo(
                    enabled = true,
                    roomCode = "ABC123",
                    isHost = true,
                    members = 3,
                    joinRequests =
                        listOf("u2" to "Bo"),
                ),
            )
        assertEquals(listOf("room", "members", "request:u2", "leave"), room.map { it.id })
        assertEquals(XmbText.Plain("ABC123"), room[0].title)
        assertEquals(XmbItemAction.CopyRoomLink, room[0].action)
        assertEquals(XmbList.RoomMembers, room[1].list)
        assertEquals(XmbText.Plain("3"), room[1].subtitle)
        assertEquals(XmbItemAction.ReviewJoin("u2", "Bo"), room[2].action)
        assertEquals(XmbItemAction.LeaveRoom, room[3].action)
    }

    @Test
    fun castRowsShowTheConnectionAndKeepTheConnectedDevice() {
        val tv = CastDevice("tv", "Living room", CastProtocol.GOOGLE_CAST, "10.0.0.2", 8009)
        val speaker = CastDevice("sp", "Kitchen", CastProtocol.SONOS, "10.0.0.3", 1400)
        val rows =
            castEntries(listOf(tv, speaker), CastState(status = CastConnectionStatus.CONNECTED, device = tv), "Connected", "Connecting")
        assertEquals(listOf("Living room", "Kitchen"), rows.map { it.title })
        assertEquals(listOf("Connected", null), rows.map { it.value })
        assertEquals(listOf("Google Cast", "Sonos"), rows.map { it.subtitle })
        assertTrue(tv.matches(rows[0]))
        assertFalse(speaker.matches(rows[0]))
        val connecting =
            castEntries(listOf(speaker), CastState(status = CastConnectionStatus.CONNECTING, device = tv), "Connected", "Connecting")
        assertEquals(listOf("Living room", "Kitchen"), connecting.map { it.title }, "The device in use stays listed")
        assertEquals("Connecting", connecting[0].value)
        assertEquals(setOf(XmbEntryKind.DEVICE), rows.map { it.kind }.toSet())
    }

    @Test
    fun membersListTheHostFirst() {
        val rows = memberEntries(listOf(UserInfo("b", "Bo", isHost = false), UserInfo("a", "Ada", isHost = true)), "Host")
        assertEquals(listOf("Ada", "Bo"), rows.map { it.title })
        assertEquals(listOf("Host", null), rows.map { it.subtitle })
        assertEquals(setOf(XmbEntryKind.MEMBER), rows.map { it.kind }.toSet())
    }

    @Test
    fun theCurrentLyricIsTheLastOneStarted() {
        val lines = listOf(LyricsEntry(1_000, "one"), LyricsEntry(4_000, "two"), LyricsEntry(9_000, "three"))
        assertEquals(-1, currentLyricIndex(lines, 500))
        assertEquals(0, currentLyricIndex(lines, 1_000))
        assertEquals(0, currentLyricIndex(lines, 3_999))
        assertEquals(1, currentLyricIndex(lines, 4_000))
        assertEquals(2, currentLyricIndex(lines, 60_000))
        assertEquals(-1, currentLyricIndex(emptyList(), 5_000))
        assertNull(lines.getOrNull(currentLyricIndex(lines, 0)))
    }
}
