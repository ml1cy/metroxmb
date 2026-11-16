package com.metrolist.shared.xmb

import com.metrolist.shared.ui.settings.SettingsPlatform

/**
 * Which front end the app shows. Stored by ordinal through `Settings.interfaceStyle`, so new
 * values must only be appended.
 */
enum class InterfaceStyle {
    METROLIST,
    XMB,
}

/** Platforms whose entry point can show the XMB. Others always use the stock interface. */
val xmbPlatforms: Set<SettingsPlatform> = setOf(SettingsPlatform.DESKTOP)

fun SettingsPlatform.supportsXmb(): Boolean = this in xmbPlatforms
