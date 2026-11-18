package com.metrolist.android.xmb

import com.metrolist.android.ui.IncomingDestination
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.nav.XmbCategoryId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidXmbLinksTest {
    @Test
    fun shortcutsOpenTheirXmbPlaces() =
        runTest {
            assertTrue(xmbOpenFor(IncomingDestination.Recognition)!!.identifySong)
            assertTrue(xmbOpenFor(IncomingDestination.Player)!!.nowPlaying)
            assertEquals(XmbCategoryId.MUSIC, xmbOpenFor(IncomingDestination.Home)?.category)
            assertEquals(XmbCategoryId.FRIENDS, xmbOpenFor(IncomingDestination.ListenTogetherRoom)?.category)
            assertEquals(XmbCategoryId.SETTINGS, xmbOpenFor(IncomingDestination.Settings)?.category)
            assertEquals(XmbList.SettingsPage(SettingsPageId.BACKUP_RESTORE), xmbOpenFor(IncomingDestination.BackupRestore)?.list)
        }

    @Test
    fun searchAndBrowseLinksOpenTheirLevels() =
        runTest {
            assertEquals(XmbList.SearchResults("abba"), xmbOpenFor(IncomingDestination.Search("abba"))?.list)
            assertEquals(XmbCategoryId.SEARCH, xmbOpenFor(IncomingDestination.Search(null))?.category)
            assertNull(xmbOpenFor(IncomingDestination.Search(null))?.list)
            assertEquals(
                XmbList.Browse("FEmusic_moods", "p", "Moods"),
                xmbOpenFor(IncomingDestination.Browse("FEmusic_moods", "p", "Moods"))?.list,
            )
        }

    @Test
    fun destinationsTheAppHandlesItselfHaveNoXmbPlace() =
        runTest {
            // Playing, sign-in and joining a room are handled by MetrolistApp.
            assertNull(xmbOpenFor(IncomingDestination.Login))
            assertNull(xmbOpenFor(IncomingDestination.PlaySearch("abba")))
            assertNull(xmbOpenFor(IncomingDestination.ListenTogether("ABCD1234")))
        }
}
