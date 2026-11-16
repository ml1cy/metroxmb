# Upstream touchpoints

Every edit this project makes to a file that exists upstream, so pulling new Metrolist
releases stays manageable. Project-only files (PLAN.md, `docs/xmb-*.md`, `reference/`,
`compare/`, `scripts/xmb/`, the `xmb_` Compose resources, the `xmb` and `visualizer` packages and
the `desktop/xmb` host) aren't listed.

| File | Change | Why |
|---|---|---|
| `CLAUDE.md` | Upstream symlink to `AGENTS.md` replaced with this project's own guide. `AGENTS.md` is untouched | Metrolist's agent rules are for their repo; this project needs its own. On upstream merges, keep ours |
| `README.md` | Replaced with this project's README (status, roadmap, controls, builds, docs) | The project's front page. On upstream merges, keep ours |
| `.github/workflows/`, `.github/actions/`, `.github/scripts/`, `.github/tests/`, `.github/actionlint.yaml` | Deleted. Issue templates, PR template and `FUNDING.yml` are untouched | The owner removed GitHub Actions; upstream's CI needs their runners and secrets. On upstream merges, keep these deleted and delete any new files upstream adds there |
| `scripts/test_build_appimage.py`, `scripts/test_build_ios.py`, `scripts/test_desktop_smoke.py` | Removed the assertions that read workflow and action files; the build-script, Xcode-project and `gradle.properties` checks stay | The workflow files they inspected are deleted. On upstream merges, drop any new workflow assertions |
| `shared/src/commonMain/kotlin/com/metrolist/shared/constants/PreferenceKeys.kt` | Appended `InterfaceStyleKey` under an "XMB Music project keys" comment at the end | Preference keys live here (CLAUDE.md) |
| `shared/src/commonMain/kotlin/com/metrolist/shared/settings/Settings.kt` | One import and `var interfaceStyle by preference(InterfaceStyleKey, InterfaceStyle.XMB)` after `appLayout` | The interface-style switch; stored by ordinal, so `InterfaceStyle` values are append-only |
| `shared/src/commonMain/kotlin/com/metrolist/shared/ui/settings/AppearanceSettingsRegistration.kt` | Two imports and an "Interface" group with one choice at the top of the Appearance page, only on platforms with an XMB host (`supportsXmb()`) | Lets the stock settings switch to the XMB |
| `shared/src/commonMain/kotlin/com/metrolist/shared/ui/settings/SettingsOrdering.kt` | Added `"interface"` first in the Appearance group order | Unlisted groups sort to the end of the page; this keeps the interface switch at the top |
| `shared/src/commonMain/composeResources/values/strings.xml` | Appended an XMB block of `xmb_` strings before `</resources>` | UI text must live in this catalog (docs/strings.md). On upstream merges, keep the block at the end |
| `desktopApp/src/jvmMain/kotlin/com/metrolist/desktop/Main.kt` | Two imports; in `App()`, `if (showsXmb()) XmbDesktopHost(checkDarkMode) else AppContent(...)` | The desktop entry-point switch. The XMB replaces `AppContent`, so desktop's global playback shortcuts (installed inside it) don't take the arrow keys |
