package com.metrolist.android.xmb

import com.metrolist.shared.ui.navigation.Screen
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.nav.XmbCategoryId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidXmbLinksTest {
    @Test
    fun collectionsOpenInTheirXmbCategory() {
        val album = xmbOpenForScreen(Screen.Album("MPREb_1"))
        assertEquals(XmbCategoryId.MUSIC, album?.category)
        assertEquals("MPREb_1", (album?.list as XmbList.Album).album.id)
        assertEquals(XmbCategoryId.PODCASTS, xmbOpenForScreen(Screen.Podcast("p1"))?.category)
        assertEquals(XmbList.Downloads, xmbOpenForScreen(Screen.AutoPlaylist("downloaded"))?.list)
        assertEquals(XmbList.Liked, xmbOpenForScreen(Screen.AutoPlaylist("liked"))?.list)
    }

    @Test
    fun settingsTheXmbDrawsOpenThere() {
        assertEquals(XmbList.SettingsPage(SettingsPageId.ABOUT), xmbOpenForScreen(Screen.About)?.list)
        assertEquals(XmbCategoryId.SETTINGS, xmbOpenForScreen(Screen.Updates)?.category)
    }

    @Test
    fun screensOnlyTheStockUiHasStay() {
        // Sign-in and stock settings pages, and the top-level routes (the XMB itself).
        assertNull(xmbOpenForScreen(Screen.Login))
        assertNull(xmbOpenForScreen(Screen.SettingsPage(SettingsPageId.DEVTOOLS)))
        assertNull(xmbOpenForScreen(Screen.Home))
        assertNull(xmbOpenForScreen(Screen.Library))
    }
}
