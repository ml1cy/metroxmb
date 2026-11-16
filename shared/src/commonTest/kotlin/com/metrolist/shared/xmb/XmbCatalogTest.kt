package com.metrolist.shared.xmb

import com.metrolist.shared.constants.InterfaceStyleKey
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.settings.SettingsGroupRegistration
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.ui.settings.SettingsPlatform
import com.metrolist.shared.ui.settings.prioritized
import com.metrolist.shared.xmb.nav.XmbCategoryId
import com.metrolist.shared.xmb.nav.XmbItemAction
import com.metrolist.shared.xmb.nav.xmbCategories
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XmbCatalogTest {
    @Test
    fun listsTheCategoriesInPlanOrderEachWithItems() {
        val categories = xmbCategories()
        assertEquals(XmbCategoryId.entries.toList(), categories.map { it.id })
        categories.forEach { category ->
            assertTrue(category.items.isNotEmpty(), "${category.id} has no items")
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
    fun settingsOffersTheWayBackToTheStockInterface() {
        val settings = xmbCategories().single { it.id == XmbCategoryId.SETTINGS }
        assertEquals(XmbItemAction.SWITCH_TO_METROLIST, settings.items.first().action)
    }

    @Test
    fun interfaceStyleDefaultsToXmbAndIsStoredByOrdinal() {
        assertEquals(InterfaceStyle.XMB, Settings(MapSettings()).interfaceStyle)
        val store = MapSettings()
        Settings(store).interfaceStyle = InterfaceStyle.METROLIST
        assertEquals(InterfaceStyle.METROLIST.ordinal, store.getInt(InterfaceStyleKey, -1))
        assertEquals(InterfaceStyle.METROLIST, Settings(MapSettings(InterfaceStyleKey to 0)).interfaceStyle)
        assertEquals(listOf("METROLIST", "XMB"), InterfaceStyle.entries.map { it.name }, "Stored ordinals must not change")
    }

    @Test
    fun interfaceGroupLeadsTheAppearancePage() {
        val groups =
            listOf("theme", "interface", "displayDensity").map { id ->
                SettingsGroupRegistration(id = id, title = id, items = emptyList())
            }
        assertEquals(listOf("interface", "theme", "displayDensity"), groups.prioritized(SettingsPageId.APPEARANCE).map { it.id })
    }

    @Test
    fun onlyPlatformsWithAnXmbHostOfferIt() {
        assertTrue(SettingsPlatform.DESKTOP.supportsXmb())
        assertFalse(SettingsPlatform.ANDROID.supportsXmb())
        assertFalse(SettingsPlatform.IOS.supportsXmb())
    }
}
