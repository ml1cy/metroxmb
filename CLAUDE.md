# Project guide: XMB Music

XMB Music is a **separate, private project** built on Metrolist-KMP's source code (GPL-3.0).
It gives the app a PS3 XMB-style interface and PS3-style music visualizers. Nothing from this
repo goes back to MetrolistGroup. The work plan is in [PLAN.md](PLAN.md).

**This file is the only agent guide for this project.**

## About `AGENTS.md` and Metrolist's docs
- `AGENTS.md` is Metrolist's guide for **their** repository: their PR process, review rules,
  rejected-features list, maintainer approvals, issue references and commit-message policy.
  **None of it applies here.** Use it, and `docs/`, only as a description of how the
  codebase works (module layout, where code lives, how subsystems behave).
- Don't open issues, PRs, comments or discussions on MetrolistGroup repositories. Don't push to
  the `upstream` remote.
- Metrolist's docs describe real technical constraints: some things break if you ignore them.
  Those are listed under **Technical facts to respect**. Everything else in their guides is
  their policy and optional here.

## Git
- Remotes: `origin` = the owner's private repo, `upstream` = MetrolistGroup/Metrolist (read only,
  used to pull new releases).
- One branch per PLAN.md milestone: `feat/xmb-m<N>-<short-description>`. Commit as you go and
  push the branch to `origin`, both pre-approved.
- Ask the owner before merging into `main`, rebasing onto upstream, rewriting history or
  force pushing.
- **Keep `origin` private.** The repo contains Sony PS3 assets (font, sounds) for personal use.
  Never make it public or share builds that include them.
- Upstream's GitHub Actions workflows (`.github/workflows/`) are for their infrastructure
  (Blacksmith runners, signing secrets, release publishing). Don't rely on them and don't edit
  them. The owner keeps Actions disabled in the repo settings.

## Technical facts to respect
These come from how the code actually works, not from upstream policy:
- **Strings:** UI text lives in `shared/src/commonMain/composeResources/values/strings.xml`.
  Android generates a resource subset from the keys it references (`docs/strings.md`), so text
  placed elsewhere won't reach Android. Put XMB strings there with an `xmb_` key prefix.
  English only. Locale-specific text in other files is up to the owner.
- **Preferences:** keys go in `PreferenceKeys.kt`, exposed and read through `Settings.shared`.
  The settings screens are generated from `MetrolistSettingsRegistry`.
- **Listen Together:** keep the schema (`metroproto` submodule, don't modify), JVM generation,
  shared models and both codecs (incl. `PortableMessageCodec`) in sync, or rooms break
  between devices (`docs/listen-together.md`).
- **Audio:** MetroEQ, tempo/pitch and silence skipping run in each platform's PCM path
  (`docs/audio-processing.md`). Don't reorder that chain.
- **Rendering:** hidden windows/backgrounded apps suspend animation work (`docs/rendering.md`).
  Shader code is API-gated on Android.
- **Desktop resources** go in `desktopApp/src/desktopMain/resources/`, even though desktop Kotlin
  is under `desktopApp/src/jvmMain/kotlin/`.
- **Build and checks:** `docs/development.md` (setup) and `docs/checks.md` (what to run per
  platform). Format changed Kotlin with `ktlint --format` so the build's lint step passes.

## Project rules
- **Keep stock Metrolist working.** The XMB is a selectable interface style: an `InterfaceStyle`
  preference (`METROLIST` / `XMB`, default `XMB`). Don't delete the stock screens; that keeps
  upstream merges easy and gives a fallback while the XMB is unfinished.
- **Code location:** XMB and visualizer code go in `shared`, under the packages
  `com.metrolist.shared.xmb` and `com.metrolist.shared.visualizer`; platform glue goes in the
  platform source sets. New Gradle modules only with the owner's OK.
- **Audio data:** don't write a new PCM tap. Every platform already feeds
  `shared/.../equalizer/AudioSpectrumMeter` (Android `EqualizerAudioProcessor`, desktop FFmpeg
  `AudioRenderer`, iOS `IosAudioProcessingTap`). Call `addConsumer()` while a visualizer is
  visible, `removeConsumer()` on dispose, and read `levels()`. Extend the meter in shared code if
  more data is needed, keeping the MetroEQ page's behavior the same. Where the meter has no data
  (iOS HLS fallback, Android offload), the visualizer keeps its idle motion.
- **Shaders:** follow the existing pattern in `ArtworkBackgroundShader.kt` and
  `PlatformLiquidBackgroundCanvas.*`.
- **Input:** XMB navigation must work with keyboard, controller/D-pad (including Android TV) and
  touch. See "Input and focus" in `docs/development.md`.
- **Assets:** PS3 assets are committed. Their manifest is the source of truth:
  `assets-ps3/ASSETS_MANIFEST.md` until PLAN.md M1 moves them into Compose resources, then
  `docs/xmb-assets.md`. Every slot is required; a missing file fails a test. Don't download or
  search the web for Sony assets; only use files the owner adds.
- **Font:** the Rodin font is Latin-only. Non-Latin titles must render through normal font
  fallback to the app's existing fonts.
- **Tests:** add backend tests for logic you add or change (adapters, meter extensions, asset
  manifest). Reference-image comparison is a dev script under `scripts/xmb/`, not a test.
- **Upstream sync:** log every edit to a file that exists upstream in
  `UPSTREAM_TOUCHPOINTS.md` (file, change, reason) so pulling new Metrolist releases stays manageable.
- **Don't bump `appVersion`** unless the owner asks. Upstream's tooling reads it, and changing it
  causes merge conflicts.
