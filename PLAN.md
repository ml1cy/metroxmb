# XMB Music: PS3-Style Front End + Visualizers for Metrolist-KMP — Build Plan

Project rules are in [CLAUDE.md](CLAUDE.md). This is a separate, private project built on
Metrolist's source; Metrolist's `AGENTS.md` describes their repo's process and doesn't apply here.

## Goal
Build on **Metrolist-KMP**'s source code to make a **PS3 XrossMediaBar (XMB)-style interface**,
with a faithful replica of the PS3's built-in music visualizers (made by Q-Games) as the live
background. Metrolist's backend (streaming, playback, library, lyrics, casting, MetroEQ,
Listen Together) stays as is. The stock Metrolist UI keeps working as a selectable interface style.

**Visualizers for the 1:1 core:** Waves (XMB silk ribbon), Valley (terrain), Gaia (Earth).

**Target order:** Desktop (fastest to iterate and screenshot) → Android / Android TV. iOS isn't
a target: it keeps the stock Metrolist interface.

## What already exists in Metrolist (don't rebuild it)
- **Audio data:** `shared/.../equalizer/AudioSpectrumMeter` is an 18-band meter fed on every
  platform (Android `EqualizerAudioProcessor`, desktop FFmpeg `AudioRenderer`, iOS
  `IosAudioProcessingTap`). It only captures while it has consumers.
- **Shaders:** `ArtworkBackgroundShader.kt` and `PlatformLiquidBackgroundCanvas.*` show the
  shader setup, per-platform actuals and Android API gating.
- **Focus/D-pad:** `FocusOutlineProvider`, the Android TV patterns, and "Input and focus" in
  `docs/development.md`.
- **Settings:** `PreferenceKeys.kt` + `Settings.shared` + `MetrolistSettingsRegistry`.
- **Docs to read before touching each area:** `docs/development.md`, `docs/audio-processing.md`,
  `docs/rendering.md`, `docs/playback.md`, `docs/strings.md`, `docs/checks.md`.

## Assets
PS3 assets are committed in `assets-ps3/`, and `assets-ps3/ASSETS_MANIFEST.md` lists each one:
- **Font:** SCE-PS3 Rodin LATIN Regular (Latin only, no bold)
- **Icons:** 128×128 white PNGs (PS3-style "systematic" set), sorted into category, item and UI slots
- **Sounds:** nav up/down, select, back, notice in/out, boot, unlock, optional menu BGM
- **Gaps:** podcast and visualizer category icons (made in M1), left/right nav sound and
  error sound (reuse existing sounds until the owner adds files)

Earth textures for Gaia come from public-domain NASA Blue Marble imagery (added in M5).

## XMB layout for a music app
Horizontal category row; each category opens a vertical item list:

| Category | Items |
|---|---|
| **Users** | Account / sign-in, profile switch |
| **Settings** | Interface style, playback, audio quality, MetroEQ, visualizer, lyrics, casting, storage, about |
| **Music** | Home feed, Quick picks, Liked songs, Playlists, Albums, Artists, History, Downloads |
| **Search** | Search field (on-screen keyboard on TV/controller), recent searches, results |
| **Podcasts** | Subscriptions, episodes |
| **Video** | Music videos, live |
| **Friends** | Listen Together rooms, invite, join |
| **Visualizer** | Waves, Valley, Gaia, random, auto-cycle |

- **Options panel (the △ menu):** slide-in panel on the right with the existing song menu
  actions (`SongMenuActions`)
- **Now playing:** music keeps playing while you browse; track info and progress sit in the
  bottom-right; "full screen" hides the XMB and shows only the visualizer
- **Drill-down:** nested vertical lists with the parent icon shifting left, like PS3 folder navigation
- **Clock and status** in the top-right

## Code layout (inside `shared`, per CLAUDE.md)
```
shared/src/commonMain/kotlin/com/metrolist/shared/
  xmb/
    XmbApp.kt               root when InterfaceStyle = XMB: background + bar + overlays
    nav/                    XmbState (category, item, depth stack), input mapping
    bar/                    CategoryRow, ItemColumn, OptionsPanel, focus animations
    screens/                search keyboard, now-playing info, dialogs, settings pages
    theme/                  font, sizes, glow, monthly palette, motion specs
    sound/                  XmbSounds (plays the committed oggs)
    adapters/               existing ViewModels → XMB items
  visualizer/
    Visualizer.kt           interface: shader source + uniforms(time, audio, settings)
    VisualizerAudio.kt      AudioSpectrumMeter consumer → bass/mid/treble/beat, smoothing
    background/  waves/  valley/  gaia/
shared/src/commonMain/composeResources/
  drawable/xmb_*.png  font/xmb_rodin_latin_regular.ttf  files/xmb/sounds/*.ogg
```
Platform entry points (Android activity, desktop window) choose `XmbApp` or the stock UI from
`Settings.shared.interfaceStyle`; iOS always shows the stock UI. Log every upstream file edit
in `UPSTREAM_TOUCHPOINTS.md`.

**Input:** keyboard/controller/D-pad first (arrows, Enter = ✕, Esc/Back = ○, Menu/Tab = △).
Touch: swipe horizontally across categories and vertically through items; tap = select,
long-press = options.

## Working method
- One milestone per session, on its own branch (see CLAUDE.md → Git).
- Reference frames from the real console go in `reference/` (`xmb/`, `waves/`, `valley/`,
  `gaia/`). Claude Code can't watch video, so the owner exports stills
  (`ffmpeg -i clip.mp4 -vf fps=2 out/%04d.png`).
- Visual check: `scripts/xmb/snap.sh <screen>` captures the desktop app. Put the capture next
  to the reference in `compare/` and list remaining differences. This is a dev script, not a test.
- Log tuned constants (timings, easing, sizes, colors) and the reason for each in `docs/xmb-notes.md`.
- Run the relevant checks from `docs/checks.md` and `ktlint --format` on changed Kotlin.

## Milestones

### M0: Map the codebase (no code changes)
- Build desktop + Android from this checkout
- Write `docs/xmb-architecture.md`: each platform's UI entry point, navigation host, the
  ViewModels each XMB category needs, the settings registry, the song menu actions, and the
  AudioSpectrumMeter consumer API
- Map every stock screen to a spot in the XMB layout; flag any screen that doesn't fit
- Propose the exact upstream touchpoints (entry-point switch, preference key) and wait for the owner's OK

### M1: XMB shell, assets and background (desktop)
- `InterfaceStyle` preference (`METROLIST` / `XMB`, fork default `XMB`) and the entry-point switch
- Move `assets-ps3/` into Compose resources with `git mv` (`drawable/xmb_*`,
  `font/xmb_rodin_latin_regular.ttf`, `files/xmb/sounds/`), update `ASSETS_MANIFEST.md`
  (moved to `docs/xmb-assets.md`) with the new paths, and add a backend test that every
  manifest entry resolves to a file
- **Two original category icons:** `xmb_cat_podcasts` (a microphone) and `xmb_cat_visualizer`
  (an audio waveform or spectrum bars), drawn as `ImageVector`s to match the set: solid white,
  flat, rounded, same weight and padding as music/settings. Show them beside the existing
  category icons in `compare/icons.png` for the owner to approve
- Monthly XMB background colors + time-of-day brightness, with a simple wave
- Category row + item column with real XMB focus behavior (enlarge + glow, label fade, eased
  scroll, list centered on focus), keyboard/controller input, nav sounds
- All labels in `strings.xml` with `xmb_` keys; Rodin font with normal fallback for non-Latin text

### M2: Wire up Music
- Adapters from existing ViewModels: home feed, library, playlists, albums, artists, history, downloads
- Selecting a song plays it through `PlaybackController`; bottom-right now-playing info;
  drill-down into albums/playlists; options panel with real actions

### M3: Search, Settings, Users
- Search with an XMB-style on-screen keyboard (plus physical keyboard and mobile IME)
- Every setting reachable in the XMB Settings column through the existing settings registry,
  with XMB-style value pickers
- Account sign-in through the existing flow

### M4: Waves for real
- `VisualizerAudio` consumes `AudioSpectrumMeter` only while visible; extend the meter in shared
  code (with tests) if more data is needed, keeping MetroEQ's page unchanged
- Full Waves shader: layered translucent ribbons, soft edges, glossy crests, drifting sparkles,
  subtle reactivity; idle motion when no meter data is available
- Full-screen visualizer mode (hide the XMB, show a fading track overlay)

### M5: Valley and Gaia
- Valley: raymarched heightfield, fog, gliding camera
- Gaia: day/night Earth (NASA textures), oceans, atmosphere, clouds, city lights, starfield

### M6: Remaining categories
- Podcasts, Video (music videos via the existing player), Friends (Listen Together),
  XMB-styled lyrics view, casting picker

### M7: Android, Android TV, touch
- Touch gestures, phone portrait layout, tablet landscape, TV D-pad
- Quality settings and FPS cap; pause when hidden (per `docs/rendering.md`); shader fallback
  below the API level the existing shaders require

## Extra features (after the core is done)
- Theme picker: monthly colors, fixed colors, custom, or album-art tint
- Lyrics floating over the waves; beat-synced color shifts; party mode
- Listen Together: everyone in a room sees the same visualizer and colors
- Gamepad stick moves the sparkles, like the SIXAXIS did
- Desktop "wallpaper mode": borderless, always-on-bottom window on Windows
- Custom XMB themes (wallpaper + icon set + colors) importable as a zip
- More PS3-style visualizers; screenshot/record button
- TV mode that boots straight into the XMB

## Known risks
- **Scope:** the front-end rewrite is the biggest part. Metrolist-KMP is alpha, so upstream
  ViewModels may change. Keep adapters thin and touchpoints logged.
- The meter is unavailable during Android offload. The visualizer falls back to idle motion
  there.
- **Private repo only:** the committed PS3 font and sounds are Sony's. Keep `origin` private
  and don't share builds.

## Definition of done (per milestone)
- Screens compared with reference frames in `compare/`, with remaining differences listed
- 60 fps at 1080p on a mid-range Android phone and on integrated graphics
- Fully usable with keyboard/controller alone and with touch alone
- Stock interface style still builds and works
