package com.metrolist.desktop.xmb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.metrolist.desktop.ui.theme.MetrolistTheme
import com.metrolist.desktop.youtube.DesktopPageBoundTokenHost
import com.metrolist.desktop.youtube.DesktopPageBoundTokenRuntime
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.xmb.InterfaceStyle
import com.metrolist.shared.xmb.XmbApp
import com.metrolist.shared.xmb.sound.DesktopXmbSoundPlayer
import com.metrolist.shared.xmb.sound.LocalXmbSoundPlayer
import org.koin.core.context.GlobalContext

/**
 * Desktop glue for the XMB: shown by `App()` in Main.kt instead of the stock content when the
 * interface style is XMB and onboarding is done. Keeps the page-bound token host the stock
 * content runs, so playback that starts from media keys can still resolve streams.
 */
@Composable
internal fun XmbDesktopHost(checkDarkMode: (Boolean) -> Unit) {
    SideEffect { checkDarkMode(true) }
    val sounds = remember { DesktopXmbSoundPlayer() }
    DisposableEffect(sounds) { onDispose { sounds.close() } }
    MetrolistTheme {
        DesktopPageBoundTokenHost(GlobalContext.get().get<DesktopPageBoundTokenRuntime>())
        CompositionLocalProvider(LocalXmbSoundPlayer provides sounds) {
            XmbApp(onSwitchToStock = { Settings.shared.interfaceStyle = InterfaceStyle.METROLIST })
        }
    }
}

/** Whether `App()` shows the XMB. Onboarding always runs in the stock interface first. */
internal fun showsXmb(settings: Settings = Settings.shared): Boolean =
    settings.interfaceStyle == InterfaceStyle.XMB && settings.desktopOnboardingComplete
