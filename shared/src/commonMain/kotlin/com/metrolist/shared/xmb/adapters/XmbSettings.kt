package com.metrolist.shared.xmb.adapters

import com.metrolist.shared.ui.settings.SettingsControl
import com.metrolist.shared.ui.settings.SettingsItemRegistration
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.ui.settings.SettingsPageRegistration
import com.metrolist.shared.ui.settings.SettingsUiRegistry
import com.metrolist.shared.xmb.nav.XmbPicker
import kotlin.math.abs
import kotlin.math.roundToInt

/** The few words settings rows need from the string catalog. */
data class XmbSettingLabels(
    val on: String,
    val off: String,
    val yes: String,
    val no: String,
)

/**
 * A settings page as XMB rows: every item of every group, in order. [sliderLabels] holds the
 * formatted value of each slider by item id (formatters are composable, so the caller runs them).
 */
fun SettingsPageRegistration.toXmbEntries(
    labels: XmbSettingLabels,
    sliderLabels: Map<String, String> = emptyMap(),
): List<XmbEntry> =
    groups.flatMap { group ->
        group.items.map { item -> item.toXmbEntry(labels, sliderLabels[item.id], groupEnabled = group.enabled) }
    }

fun SettingsItemRegistration.toXmbEntry(
    labels: XmbSettingLabels,
    sliderLabel: String? = null,
    groupEnabled: Boolean = true,
): XmbEntry =
    XmbEntry(
        key = "setting:$id",
        title = title,
        subtitle = (description ?: annotatedDescription?.text)?.takeIf(String::isNotBlank),
        artwork = null,
        kind = XmbEntryKind.SETTING,
        setting = this,
        value = valueText(labels, sliderLabel),
        enabled = enabled && groupEnabled,
    )

/** What a row shows as its current value, at the right of the title. */
internal fun SettingsItemRegistration.valueText(
    labels: XmbSettingLabels,
    sliderLabel: String?,
): String? =
    when (control) {
        SettingsControl.SWITCH, SettingsControl.MAIN_SWITCH -> {
            if (checked == true) labels.on else labels.off
        }

        SettingsControl.CHOICE -> {
            choices.firstOrNull { it.id == selectedChoiceId }?.label ?: valueLabel
        }

        SettingsControl.SLIDER -> {
            sliderLabel ?: valueLabel ?: sliderValue?.let(::plainNumber)
        }

        SettingsControl.TEXT -> {
            val text = textValue.orEmpty()
            when {
                text.isBlank() -> null
                secret -> SECRET_MASK
                else -> text.lineSequence().first()
            }
        }

        SettingsControl.NAVIGATION, SettingsControl.ACTION, SettingsControl.CUSTOM -> {
            valueLabel
        }
    }

/** What selecting a settings row does. */
sealed interface XmbSettingStep {
    /** The row is disabled. */
    data object Unavailable : XmbSettingStep

    data class Pick(
        val picker: XmbPicker,
    ) : XmbSettingStep

    /** Edit text on the on-screen keyboard; [apply] receives the result. */
    data class Type(
        val title: String,
        val text: String,
        val secret: Boolean,
        val apply: (String) -> Unit,
    ) : XmbSettingStep

    /** Open another registry page as an XMB level. */
    data class Open(
        val page: SettingsPageId,
    ) : XmbSettingStep

    /** Open a page outside the app (Android's link settings). */
    data class External(
        val page: SettingsPageId,
    ) : XmbSettingStep

    data class Run(
        val action: () -> Unit,
    ) : XmbSettingStep
}

/**
 * Decides what selecting [entry] (a row of settings [page]) does. The XMB's own pages have no
 * registry [page]: their rows always have handlers, and anything else is unavailable there.
 * Custom pages in [drawn] open as XMB levels like registry pages.
 */
fun xmbSettingStep(
    entry: XmbEntry,
    page: SettingsPageId?,
    registry: SettingsUiRegistry?,
    labels: XmbSettingLabels,
    drawn: Set<SettingsPageId> = emptySet(),
): XmbSettingStep {
    val item = entry.setting ?: return XmbSettingStep.Unavailable
    val stock = XmbSettingStep.Unavailable
    if (!entry.enabled) return XmbSettingStep.Unavailable
    return when (item.control) {
        SettingsControl.SWITCH, SettingsControl.MAIN_SWITCH -> {
            val change = item.onCheckedChange ?: return stock
            XmbSettingStep.Pick(
                XmbPicker.Choice(item.title, listOf(labels.on, labels.off), focus = if (item.checked == true) 0 else 1) { change(it == 0) },
            )
        }

        SettingsControl.CHOICE -> {
            val select = item.onChoiceSelected
            if (select == null || item.choices.isEmpty()) return stock
            val focus = item.choices.indexOfFirst { it.id == item.selectedChoiceId }.coerceAtLeast(0)
            XmbSettingStep.Pick(XmbPicker.Choice(item.title, item.choices.map { it.label }, focus) { select(item.choices[it].id) })
        }

        SettingsControl.SLIDER -> {
            val change = item.onSliderChanged
            val range = item.sliderRange
            if (change == null || range == null) return stock
            val value = (item.sliderValue ?: range.start).coerceIn(range.start, range.endInclusive)
            XmbSettingStep.Pick(XmbPicker.Slider(item.title, value, range, item.sliderSteps, item.sliderValueFormatter, change))
        }

        SettingsControl.TEXT -> {
            val change = item.onTextChanged ?: return stock
            XmbSettingStep.Type(item.title, item.textValue.orEmpty(), item.secret, change)
        }

        SettingsControl.NAVIGATION -> {
            val destination = item.destination ?: return XmbSettingStep.Unavailable
            val target = registry?.get(destination)
            if (target != null &&
                (!target.custom || destination in drawn)
            ) {
                XmbSettingStep.Open(destination)
            } else {
                XmbSettingStep.External(destination)
            }
        }

        SettingsControl.ACTION -> {
            val click = item.onClick ?: return XmbSettingStep.Unavailable
            val confirmation = item.confirmationMessage
            if (confirmation == null) {
                XmbSettingStep.Run(click)
            } else {
                XmbSettingStep.Pick(XmbPicker.Choice(confirmation, listOf(labels.yes, labels.no), focus = 1) { if (it == 0) click() })
            }
        }

        SettingsControl.CUSTOM -> {
            stock
        }
    }
}

private const val SECRET_MASK = "••••••••"

internal fun plainNumber(value: Float): String {
    val hundredths = (value * 100).roundToInt()
    val sign = if (hundredths < 0) "-" else ""
    val whole = abs(hundredths) / 100
    val fraction = abs(hundredths) % 100
    return if (fraction == 0) "$sign$whole" else "$sign$whole.${fraction.toString().padStart(2, '0').trimEnd('0')}"
}
