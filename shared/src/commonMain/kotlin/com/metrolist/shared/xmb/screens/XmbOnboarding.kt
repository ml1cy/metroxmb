package com.metrolist.shared.xmb.screens

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.loading
import com.metrolist.shared.generated.resources.xmb_onboarding_step
import com.metrolist.shared.generated.resources.xmb_onboarding_welcome
import com.metrolist.shared.ui.onboarding.OnboardingPage
import com.metrolist.shared.ui.onboarding.OnboardingPageContent
import com.metrolist.shared.visualizer.VisualizerAudio
import com.metrolist.shared.visualizer.background.XmbWaveMotion
import com.metrolist.shared.visualizer.waves.XmbBackdrop
import com.metrolist.shared.xmb.XmbTime
import com.metrolist.shared.xmb.bar.LocalXmbTaps
import com.metrolist.shared.xmb.bar.XmbTaps
import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.nav.XmbNotice
import com.metrolist.shared.xmb.nav.reduce
import com.metrolist.shared.xmb.nav.xmbActionFor
import com.metrolist.shared.xmb.settings.XmbPrefs
import com.metrolist.shared.xmb.theme.XmbMetrics
import com.metrolist.shared.xmb.theme.rememberXmbFontFamily
import org.jetbrains.compose.resources.stringResource

/** One more choice on an onboarding page, after its main one (Log in, Restore a backup on the last page). */
class XmbOnboardingAction(
    val label: String,
    val run: () -> Unit,
)

/**
 * The first-run setup the console way: the XMB's sky and wave behind a notice band with the step,
 * the page's title and text, and its choices as buttons. ←/→ (or a D-pad) choose, ✕/Enter/Space
 * confirms, ○/Esc goes back a page; buttons take taps and clicks. The pages and what each choice
 * does are the platform's ([content]); this only draws them.
 */
@Composable
fun XmbOnboarding(
    content: OnboardingPageContent,
    actions: List<XmbOnboardingAction> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val buttons =
        buildList {
            add(XmbFrameButton(content.primaryLabel, content.onPrimary))
            actions.forEach { add(XmbFrameButton(it.label, it.run)) }
            val secondary = content.onSecondary
            if (content.secondaryLabel != null && secondary != null) add(XmbFrameButton(content.secondaryLabel, secondary))
        }
    val title = if (content.page == OnboardingPage.WELCOME) stringResource(Res.string.xmb_onboarding_welcome) else content.title
    XmbNoticeScreen(
        id = "onboarding:${content.page}",
        title = stringResource(Res.string.xmb_onboarding_step, content.step + 1, content.stepCount),
        message = "$title\n\n${content.description}",
        details = if (content.isWorking) stringResource(Res.string.loading) else content.error,
        buttons = if (content.isWorking) emptyList() else buttons,
        onBack = { if (!content.isWorking) content.onBack?.invoke() },
        modifier = modifier,
    )
}

/**
 * A wait the console way, over whatever is shown: the notice band with [message] and no buttons
 * (signing in reloads the app's data, for one).
 */
@Composable
fun XmbBusyOverlay(
    message: String,
    modifier: Modifier = Modifier,
) {
    val fontFamily = rememberXmbFontFamily()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val metrics = remember(maxWidth, maxHeight) { XmbMetrics.forSize(maxWidth, maxHeight) }
        XmbNoticeView(
            XmbNotice(id = "busy", message = message, buttons = emptyList()),
            focus = 0,
            metrics = metrics,
            fontFamily = fontFamily,
        )
    }
}
