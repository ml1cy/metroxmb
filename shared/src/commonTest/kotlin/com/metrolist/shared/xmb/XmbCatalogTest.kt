package com.metrolist.shared.xmb

import com.metrolist.shared.model.SearchSummary
import com.metrolist.shared.model.SearchSummaryPage
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.adapters.toXmbEntries
import com.metrolist.shared.xmb.nav.XmbAccountInfo
import com.metrolist.shared.xmb.nav.XmbCategoryId
import com.metrolist.shared.xmb.nav.XmbItemAction
import com.metrolist.shared.xmb.nav.XmbNavigation
import com.metrolist.shared.xmb.nav.XmbText
import com.metrolist.shared.xmb.nav.xmbCategories
import com.metrolist.shared.xmb.nav.xmbSearchItems
import com.metrolist.shared.xmb.nav.xmbUserItems
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class XmbCatalogTest {
    @Test
    fun listsTheCategoriesInPlanOrderEachWithItems() {
        val categories = xmbCategories()
        assertEquals(XmbCategoryId.entries.toList(), categories.map { it.id })
        categories.forEach { category ->
            // Settings lists the settings registry's pages, so it's empty without a host.
            if (category.id != XmbCategoryId.SETTINGS) assertTrue(category.items.isNotEmpty(), "${category.id} has no items")
            assertEquals(
                category.items.size,
                category.items
                    .map { it.id }
                    .toSet()
                    .size,
                "${category.id} has duplicate item ids",
            )
        }
    }

    @Test
    fun usersOffersSignInOrTheAccountWithSwitchAndLogOut() {
        assertEquals(listOf("account"), xmbUserItems(null).map { it.id })
        assertFalse(xmbUserItems(null).single().isAvailable)
        val signedOut = xmbUserItems(XmbAccountInfo(signedIn = false))
        assertEquals(listOf(XmbItemAction.SignIn), signedOut.map { it.action })
        val signedIn =
            xmbUserItems(XmbAccountInfo(signedIn = true, name = "Ada", detail = "ada@example.com", thumbnail = "https://img/a.jpg"))
        assertEquals(listOf("account", "switch_channel", "sign_out"), signedIn.map { it.id })
        assertEquals(XmbText.Plain("Ada"), signedIn[0].title)
        assertEquals(XmbText.Plain("ada@example.com"), signedIn[0].subtitle)
        assertEquals("https://img/a.jpg", signedIn[0].artwork)
        assertEquals(XmbItemAction.SignOut, signedIn[2].action)
        val unnamed = xmbUserItems(XmbAccountInfo(signedIn = true, name = " ", thumbnail = ""))
        assertIs<XmbText.Resource>(unnamed[0].title)
        assertNull(unnamed[0].artwork)
    }

    @Test
    fun searchStartsWithANewSearchThenRecentQueries() {
        val items = xmbSearchItems(listOf("daft punk", "abba", "daft punk"))
        assertEquals(XmbItemAction.OpenSearch, items.first().action)
        assertEquals(listOf("new_search", "recent:daft punk", "recent:abba"), items.map { it.id })
        assertEquals(XmbList.SearchResults("abba"), items[2].list)
    }

    @Test
    fun dynamicColumnsKeepTheirPlace() {
        val categories = xmbCategories(search = xmbSearchItems(listOf("q")))
        assertEquals(2, categories.single { it.id == XmbCategoryId.SEARCH }.items.size)
        assertEquals(0, categories.single { it.id == XmbCategoryId.SETTINGS }.items.size)
    }

    @Test
    fun shrinkingColumnsClampTheirFocus() {
        val navigation = XmbNavigation(category = 3, items = listOf(0, 0, 0, 5))
        assertEquals(listOf(0, 0, 0, 2), navigation.clampItems(listOf(1, 1, 1, 3)).items)
        assertEquals(listOf(0, 0, 0, 0), navigation.clampItems(listOf(0, 0, 0, 0)).items)
        assertSame(navigation, navigation.clampItems(listOf(1, 1, 1, 9)))
    }

    @Test
    fun searchResultsBecomeShelfFolders() {
        val song = SongItem(id = "s", title = "Song", thumbnail = "https://img/s.jpg", artists = emptyList(), album = null)
        val page =
            SearchSummaryPage(
                listOf(
                    SearchSummary("Top result", listOf(song), isTopResult = true),
                    SearchSummary("Empty", emptyList()),
                    SearchSummary("", listOf(song, song.copy(id = "t", title = "Other"))),
                ),
            )
        val entries = page.toXmbEntries("q")
        assertEquals(listOf("Top result", "Song"), entries.map { it.title })
        assertEquals(listOf("search:q:0", "search:q:2"), entries.map { it.key })
        assertEquals("Song, Other", entries[1].subtitle)
        assertEquals("https://img/s.jpg", entries[0].artwork)
        assertEquals(XmbList.SearchSection("q", 2, page.summaries[2]), entries[1].open)
    }
}
