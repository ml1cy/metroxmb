package com.metrolist.shared.xmb

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.metrolist.shared.ui.settings.SettingsChoice
import com.metrolist.shared.ui.settings.SettingsControl
import com.metrolist.shared.ui.settings.SettingsGroupRegistration
import com.metrolist.shared.ui.settings.SettingsItemRegistration
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.ui.settings.SettingsPageRegistration
import com.metrolist.shared.ui.settings.SettingsRootGroupRegistration
import com.metrolist.shared.ui.settings.SettingsTone
import com.metrolist.shared.ui.settings.SettingsUiRegistry
import com.metrolist.shared.xmb.adapters.XmbEntryKind
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.adapters.XmbSettingLabels
import com.metrolist.shared.xmb.adapters.XmbSettingStep
import com.metrolist.shared.xmb.adapters.plainNumber
import com.metrolist.shared.xmb.adapters.toXmbEntries
import com.metrolist.shared.xmb.adapters.toXmbEntry
import com.metrolist.shared.xmb.adapters.xmbSettingStep
import com.metrolist.shared.xmb.nav.XmbItemAction
import com.metrolist.shared.xmb.nav.XmbPicker
import com.metrolist.shared.xmb.nav.XmbText
import com.metrolist.shared.xmb.nav.xmbSettingsItems
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class XmbSettingsAdapterTest {
    private val labels = XmbSettingLabels(on = "On", off = "Off", yes = "Yes", no = "No")
    private val icon = ImageVector.Builder("test", 1.dp, 1.dp, 1f, 1f).build()
    private val changes = mutableListOf<Any>()

    private fun item(
        control: SettingsControl,
        id: String = control.name.lowercase(),
        enabled: Boolean = true,
        builder: SettingsItemRegistration.() -> SettingsItemRegistration = { this },
    ) = SettingsItemRegistration(id = id, title = id, control = control, enabled = enabled).builder()

    private fun page(
        id: SettingsPageId,
        custom: Boolean = false,
        vararg items: SettingsItemRegistration,
    ) = SettingsPageRegistration(
        id,
        id.name,
        icon = icon,
        tone = SettingsTone.BLUE,
        groups = listOf(SettingsGroupRegistration("g", "G", items = items.toList())),
        custom = custom,
    )

    private val registry =
        SettingsUiRegistry(
            pages =
                listOf(
                    page(SettingsPageId.PLAYER),
                    page(SettingsPageId.PLAYER_QUEUE),
                    page(SettingsPageId.STORAGE, custom = true),
                ),
            rootGroups =
                listOf(
                    SettingsRootGroupRegistration("Main", listOf(SettingsPageId.PLAYER, SettingsPageId.STORAGE, SettingsPageId.PLAYER)),
                ),
        )

    private fun step(item: SettingsItemRegistration) = xmbSettingStep(item.toXmbEntry(labels), SettingsPageId.PLAYER, registry, labels)

    @Test
    fun rowsShowTheCurrentValue() {
        assertEquals("On", item(SettingsControl.SWITCH) { copy(checked = true) }.toXmbEntry(labels).value)
        assertEquals("Off", item(SettingsControl.MAIN_SWITCH) { copy(checked = false) }.toXmbEntry(labels).value)
        val choice =
            item(
                SettingsControl.CHOICE,
            ) { copy(choices = listOf(SettingsChoice("a", "Alpha"), SettingsChoice("b", "Beta")), selectedChoiceId = "b") }
        assertEquals("Beta", choice.toXmbEntry(labels).value)
        assertEquals("1.25x", item(SettingsControl.SLIDER) { copy(sliderValue = 1.25f) }.toXmbEntry(labels, sliderLabel = "1.25x").value)
        assertEquals("1.25", item(SettingsControl.SLIDER) { copy(sliderValue = 1.25f) }.toXmbEntry(labels).value)
        assertEquals("••••••••", item(SettingsControl.TEXT) { copy(textValue = "key", secret = true) }.toXmbEntry(labels).value)
        assertNull(item(SettingsControl.TEXT) { copy(textValue = " ") }.toXmbEntry(labels).value)
        assertEquals("first", item(SettingsControl.TEXT) { copy(textValue = "first\nsecond", multiline = true) }.toXmbEntry(labels).value)
    }

    @Test
    fun pagesFlattenTheirGroupsAndDisabledGroupsDisableTheirRows() {
        val disabled = SettingsGroupRegistration("off", "Off", items = listOf(item(SettingsControl.SWITCH, "a")), enabled = false)
        val entries =
            SettingsPageRegistration(
                SettingsPageId.PLAYER,
                "Player",
                icon = icon,
                tone = SettingsTone.BLUE,
                groups = listOf(SettingsGroupRegistration("on", "On", items = listOf(item(SettingsControl.SWITCH, "b"))), disabled),
            ).toXmbEntries(labels)
        assertEquals(listOf("b", "a"), entries.map { it.title })
        assertEquals(listOf(true, false), entries.map { it.enabled })
        assertEquals(setOf(XmbEntryKind.SETTING), entries.map { it.kind }.toSet())
    }

    @Test
    fun switchesPickOnOrOffAndApplyOnlyTheChoice() {
        val picked =
            assertIs<XmbSettingStep.Pick>(
                step(
                    item(SettingsControl.SWITCH) {
                        copy(checked = false, onCheckedChange = {
                            changes +=
                                it
                        })
                    },
                ),
            )
        val choice = assertIs<XmbPicker.Choice>(picked.picker)
        assertEquals(listOf("On", "Off"), choice.labels)
        assertEquals(1, choice.focus)
        choice.apply(0)
        assertEquals(listOf<Any>(true), changes)
    }

    @Test
    fun choicesStartOnTheSelectedValue() {
        val options = listOf(SettingsChoice("a", "Alpha"), SettingsChoice("b", "Beta"))
        val picked =
            assertIs<XmbSettingStep.Pick>(
                step(
                    item(SettingsControl.CHOICE) {
                        copy(choices = options, selectedChoiceId = "b", onChoiceSelected = {
                            changes +=
                                it
                        })
                    },
                ),
            )
        val choice = assertIs<XmbPicker.Choice>(picked.picker)
        assertEquals(1, choice.focus)
        choice.apply(0)
        assertEquals(listOf<Any>("a"), changes)
        assertIs<XmbSettingStep.Stock>(step(item(SettingsControl.CHOICE) { copy(onChoiceSelected = {}) }))
    }

    @Test
    fun slidersTextActionsAndCustomRows() {
        val slider =
            assertIs<XmbSettingStep.Pick>(
                step(
                    item(SettingsControl.SLIDER) {
                        copy(sliderValue = 9f, sliderRange = 0f..5f, sliderSteps = 4, onSliderChanged = {})
                    },
                ),
            )
        assertEquals(5f, assertIs<XmbPicker.Slider>(slider.picker).value)
        val text =
            assertIs<XmbSettingStep.Type>(
                step(
                    item(SettingsControl.TEXT) {
                        copy(textValue = "abc", secret = true, onTextChanged = {
                            changes +=
                                it
                        })
                    },
                ),
            )
        assertEquals("abc", text.text)
        text.apply("new")
        assertEquals(listOf<Any>("new"), changes)
        assertIs<XmbSettingStep.Run>(step(item(SettingsControl.ACTION) { copy(onClick = {}) }))
        val confirm =
            assertIs<XmbSettingStep.Pick>(
                step(
                    item(SettingsControl.ACTION) {
                        copy(confirmationMessage = "Sure?", onClick = {
                            changes +=
                                "clicked"
                        })
                    },
                ),
            )
        val question = assertIs<XmbPicker.Choice>(confirm.picker)
        assertEquals("Sure?", question.title)
        assertEquals(1, question.focus, "Confirmations start on No")
        question.apply(1)
        assertEquals(listOf<Any>("new"), changes)
        question.apply(0)
        assertEquals("clicked", changes.last())
        assertEquals(XmbSettingStep.Stock(SettingsPageId.PLAYER), step(item(SettingsControl.CUSTOM)))
        assertEquals(XmbSettingStep.Unavailable, step(item(SettingsControl.SWITCH, enabled = false) { copy(onCheckedChange = {}) }))
    }

    @Test
    fun navigationOpensRegistryPagesAndHandsCustomOrMissingPagesToTheStockScreen() {
        assertEquals(
            XmbSettingStep.Open(SettingsPageId.PLAYER_QUEUE),
            step(
                item(SettingsControl.NAVIGATION) {
                    copy(destination = SettingsPageId.PLAYER_QUEUE)
                },
            ),
        )
        assertEquals(
            XmbSettingStep.Stock(SettingsPageId.STORAGE),
            step(
                item(SettingsControl.NAVIGATION) {
                    copy(destination = SettingsPageId.STORAGE)
                },
            ),
        )
        assertEquals(
            XmbSettingStep.Stock(SettingsPageId.FONTS),
            step(
                item(SettingsControl.NAVIGATION) {
                    copy(destination = SettingsPageId.FONTS)
                },
            ),
        )
    }

    @Test
    fun settingsColumnListsRootPagesOnceAndCustomPagesOpenStock() {
        val items = xmbSettingsItems(registry)
        assertEquals(listOf("settings:PLAYER", "settings:STORAGE"), items.map { it.id })
        assertEquals(XmbText.Plain("PLAYER"), items[0].title)
        assertEquals(XmbList.SettingsPage(SettingsPageId.PLAYER), items[0].list)
        assertEquals(XmbItemAction.StockSettings(SettingsPageId.STORAGE), items[1].action)
        assertNull(items[1].list)
        assertFalse(items.any { !it.isAvailable })
    }

    @Test
    fun numbersWithoutAFormatterDropTrailingZeros() {
        assertEquals("1", plainNumber(1f))
        assertEquals("1.5", plainNumber(1.5f))
        assertEquals("0.05", plainNumber(0.05f))
        assertEquals("-2.25", plainNumber(-2.25f))
    }
}
