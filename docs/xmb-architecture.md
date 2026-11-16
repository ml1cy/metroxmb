# XMB architecture map (M0)

Where the XMB front end plugs into Metrolist-KMP, mapped from upstream `5b2fc2b` plus this
project's commits. Written for PLAN.md M0. No production code changed in M0. Line numbers are
as of that base and will drift with upstream merges; the symbol names are the stable part.

## 1. Build status

Desktop compiles, and the shared desktop tests pass except for 18 failures caused by the build
container (no IPv6, running as root). The Android debug APK builds and all 872 Android unit
tests pass. iOS wasn't built (needs macOS). Details, setup and upstream tooling issues are in
[§14](#14-m0-build-log).

## 2. UI entry points

| Platform | Entry | Root composable | Notes |
|---|---|---|---|
| Android | `androidApp/.../MainActivity.kt` `showContent()` (l.273) | `MetrolistApp(...)` (`androidApp/.../ui/MetrolistApp.kt:395`), called at `MainActivity.kt:312` | Shows `OnboardingScreen` first until `onboardingManager.isComplete`. `MetrolistApp` sets theme, auth restore, nav state, the player sheet, then `NavigationContentProvider` (l.893) supplies the shared locals listed below |
| Desktop | `desktopApp/.../Main.kt` `main()` → `runDesktopApp()` → `application { Window { WindowFrame { App(...) } } }` (l.1229, l.1544) | `WindowScope.App(...)` (l.259) → `AppContent(...)` (l.283) | `App` supplies `LocalNavigator`, `LocalPlaybackController` (`DesktopPlaybackController`), `LocalDownloadedSongIds`. `AppContent` holds `MetrolistTheme`, the desktop onboarding gate, the image loader, `DesktopShortcutLayer` (l.503) and `AppContentHost` |
| iOS | `shared/src/iosMain/.../MainViewController.kt` `MainViewController()` (l.574) | `MobileApp(...)` (`shared/src/commonMain/.../ui/mobile/MobileApp.kt:302`), called at l.438 | iOS's whole UI is the **common** `MobileApp`. Its parameter list is the model for what a cross-platform `XmbApp` needs from a host: database, `PlaybackController`, `ListenTogetherManager`, `PlayerPlatformServices`, download callbacks, settings page content, login content |

Shared (`commonMain`) locals the stock roots provide, which XMB code can read directly:
`LocalPlaybackController`, `LocalNavigator`, `LocalDownloadedSongIds`, `LocalIsTv`,
`LocalPlayerSession`, `LocalPlaybackKeyFocus`. Android adds its own `LocalPlayerConnection`,
`LocalPlayerPlatform`, `LocalMenuState`, `LocalPlaylistAdder` and `LocalListenTogetherManager`
(the last is Android-only, in `androidApp/.../LocalComposition.kt`), so the XMB takes the
`ListenTogetherManager` as a host parameter, like `MobileApp` does.

## 3. Navigation hosts

- Shared model: `ui/navigation/Screen.kt` (`sealed class Screen : NavKey`, Navigation3) and
  `Navigator`/`NavigationState` (`ui/navigation/NavigationState.kt:169`): per-tab back stacks,
  forward history, `navigate`, `goBack`, `goForward`, `navigateToRoot`.
- Android: `MetrolistApp` builds `rememberNavigationState(...)` with top-level `Home`, `Search`,
  `Library`, `EasterEgg` and maps entries to screens inside the file. ViewModels for detail
  routes are scoped to entries (`docs/development.md` → Navigation).
- Desktop: same `Navigator`; routes render in `desktopApp/.../ui/layouts/AppContentHost.kt`
  (`when (route)`). Settings, sign-in, channel switch, token login, device connect and log viewer
  are overlays toggled through `UiState`, not routes.
- iOS: `MobileApp` + `ui/mobile/MobileRoutes.kt`.

**XMB plan:** the XMB doesn't use `Screen` routes for its own structure. `xmb/nav/XmbState` holds
category, item and a drill-down stack of XMB levels. Album, playlist, artist, podcast and browse
pages become drill-down levels fed by the same loaders the stock screens use. Stock-only screens
with no XMB equivalent open as an overlay panel (see §10), so `LocalNavigator` isn't needed by
XMB code.

## 4. Data per XMB category

All shared ViewModels are plain classes taking a `CoroutineScope` (not AndroidX ViewModels);
Android wraps several in `androidApp/.../viewmodels/` for `koinViewModel()` scoping. The
database is a Koin `single<MusicDatabase>` on every platform, and `koin-core` is a shared
`commonMain` dependency (`KoinPlatform.getKoin()` is already used in common code), so XMB
adapters in `xmb/adapters/` can construct the shared classes from common code.

| Category | Items | Shared source (`shared/.../viewmodels/` unless noted) | Platform-specific parts |
|---|---|---|---|
| Users | Account, sign in, switch channel, token login | `auth/AuthManager.kt` (`accountName`, `accountThumbnail`, session restore); `Settings.shared.accountEmail` etc. | Login is a WebView flow per platform: Android `Screen.Login`, desktop `LoginDialog` via `UiState.showSignIn`, iOS `MobileApp(loginContent = ...)` |
| Settings | Interface style, playback, audio quality, MetroEQ, visualizer, lyrics, casting, storage, about | `ui/settings/MetrolistSettingsRegistry.kt` → `SettingsUiRegistry` (§6) | `SharedSettingsHost` is built per platform: Android `ui/screens/SettingsScreen.kt`, desktop `ui/screens/settings/SettingsOverlay.kt`, iOS inside `MobileApp.kt` |
| Music | Home feed, Quick picks, Liked, Playlists, Albums, Artists, History, Downloads | `HomeViewModel(database, scope, ...)` (`uiState`: sections, chips, speed dial; `loadMoreSections`, `refresh`); `LibraryViewModel(scope, database, downloadedSongIds)` with `LibraryFilterType` (`TRACKS`, `PLAYLISTS`, `ARTISTS`, `ALBUMS`, `PODCASTS`); `AlbumViewModel`, `ArtistViewModel`, `BrowseViewModel`, `PlaylistPageLoader` (internal); auto playlists `liked`, `downloaded`, `top100` (`Screen.AutoPlaylist`) | History: Android `HistoryViewModel`, desktop `history/HistoryRepository.kt` and `state/history/HistoryScreenState.kt`, shared `ui/mobile/MobileHistoryLoader.kt`. Downloads: `LocalDownloadedSongIds` everywhere; download actions are platform services (Android `DownloadService`, desktop `LibraryState`, iOS callbacks into `MobileApp`) |
| Search | Field, recent searches, results | `SearchViewModel(scope)`: `onQueryChange`, `search`, `searchFiltered(SearchFilter)`, `loadMoreFilteredResults`, history add/remove/clear, recent played songs | Keyboard focus rules: Android TV (`docs/development.md` → Android TV), desktop `PlaybackKeyFocus.setTextInputActive` |
| Podcasts | Subscriptions, episodes | `LibraryFilterType.PODCASTS`; `PodcastPager` (`loadMore`, `retry`); `podcastMediaMenuModel`; `loadMediaItemSongs(PodcastItem)` | None beyond playback |
| Video | Music videos, live | `PlaybackController.setVideoMode/toggleVideoMode`, `PlaybackUiState.isVideoAvailable/isVideoModeActive`; `supportsVideoPlayback` | The video surface is per platform (Android player view, desktop `ui/components/VideoRenderer.kt`). iOS has no video playback |
| Friends | Listen Together rooms, invite, join; Last.fm friends | `listentogether/ListenTogetherManager.kt` (passed in by each host); `FriendsViewModel(scope)` is **Last.fm friends' activity** (needs a Last.fm username and API key) | Android `AndroidListenTogetherManager` notifications |
| Visualizer | Waves, Valley, Gaia, random, auto-cycle | New `visualizer/` package; audio from §8 | Shader availability per platform (§9) |

Now playing (bottom-right) and the △ panel read `LocalPlaybackController.state`
(`PlaybackUiState`: `currentSong`, `isPlaying`, `positionMs`, `durationMs`, `queue`,
`repeatMode`, `isShuffled`, `castState`, `sleepTimerState`, ...). Lyrics come from
`PlayerViewModel` (`LyricsUiState`), which needs `PlayerPlatformServices`; that is per platform
(Android `AndroidPlayerPlatformServices`, iOS `IosPlayerPlatformServices`, desktop player state).

## 5. Playback

`ui/playback/PlaybackController.kt` is the shared interface (`play`, `playQueue`,
`shuffleQueue`, `togglePlayPause`, `seekTo`, `skipToNext/Previous`, `startRadio`, `playNext`,
`addToQueue`, `playQueueIndex`, `toggleShuffle/Repeat`, sleep timer, video mode, cast discovery
and `castTo`). Implementations: `AndroidPlaybackController` (built in `MetrolistApp.kt:638` from
the `PlaybackViewModel`'s `PlayerConnection`), `DesktopPlaybackController` (created in `App()`, `Main.kt`),
`IosPlaybackController`. XMB song rows call `LocalPlaybackController.current`; no new playback
path is needed for M2.

## 6. Settings

- **Keys:** `constants/PreferenceKeys.kt` (`val XxxKey = "xxx"`).
- **Values:** `settings/Settings.kt` delegates, e.g.
  `var playerBackgroundStyle by preference(PlayerBackgroundStyle.SOLID)`. A keyed overload
  `preference(XxxKey, default)` exists; without a key the property name is the stored key.
  Every delegate is registered in `settingTypes`/`preferenceDefaults` and so takes part in
  portable backup (`PortableSettings`) automatically.
- **Enums are stored by ordinal** (`settings.putInt(name, value.ordinal)`). New enum values must
  only be appended, never reordered.
- **Screens:** `rememberMetrolistSettingsRegistry(host, revision)` returns `SettingsUiRegistry`:
  pages (`SettingsPageId`) → groups → `SettingsItemRegistration` with a `SettingsControl` of
  `SWITCH`, `MAIN_SWITCH`, `CHOICE`, `SLIDER`, `TEXT`, `NAVIGATION`, `ACTION` or `CUSTOM`, plus
  callbacks (`onCheckedChange`, `onChoiceSelected`, `onSliderChanged`, ...). This is a data
  model, so the XMB Settings column can render it with XMB value pickers and stay in sync with
  stock settings. `CUSTOM` items and the custom pages (Storage, Backup/Restore, Updates, About,
  Devtools, Fonts, hidden/blocked lists, Discord, Last.fm, ListenBrainz, Device Connect, Listen
  Together, Equalizer) render platform Compose through `host.pageContent`; the XMB shows those
  in an overlay panel.
- The XMB needs the same `SharedSettingsHost` each platform already builds. Plan: each platform
  host passes a host factory into `XmbApp` (§11).

## 7. Song menu actions (△ options panel)

`ui/menu/MediaMenuModel.kt`: `MediaMenuModel(title, subtitle, thumbnail, headerActions,
primaryActions, actionGroups: List<List<PlayerMenuAction>>)` built by `songMediaMenuModel(...)`,
`albumMediaMenuModel`, `artistMediaMenuModel`, `playlistMediaMenuModel`,
`podcastMediaMenuModel`. `SongMenuActions.kt` adds `songPrimaryActions`, dislike, block artist,
go to album, suggest to host, equalizer and tempo/pitch actions. `PlayerMenuAction`
(`ui/components/PlayerMenuComponents.kt:38`) carries icon, label, enabled and click handlers.

The models are shared, but the callbacks are wired per platform: Android
`ui/menu/SongMenu.kt`, desktop `ui/components/ItemContextMenu.kt`, iOS/mobile
`ui/mobile/MobileItemMenu.kt`. Plan: the △ panel renders any `MediaMenuModel` as an XMB list,
and each platform host provides a `(YTItem) -> MediaMenuModel` builder through a local that
reuses those existing wiring functions. No menu logic is duplicated.

## 8. AudioSpectrumMeter

`shared/.../equalizer/AudioSpectrumMeter.kt` (`object`):

- **Feeds:** Android `EqualizerAudioProcessor` (PCM16), desktop FFmpeg `AudioRenderer`,
  iOS `IosAudioProcessingTap` (float), all post-processing. `accept()` returns immediately
  unless a consumer is registered.
- **API:** `addConsumer()` / `removeConsumer()` (counted; `removeConsumer()` with no consumers
  throws), `hasConsumers: StateFlow<Boolean>`, `levels(): FloatArray` (analyzes the pending
  frame if one is ready, else decays the last levels by 0.98), `snapshotLevels()` (no
  analysis), `frequencies` and `bandCount` (18 bands: 45 Hz to 15 kHz), `sampleRate`,
  `gainReductionDb` (limiter).
- **Analysis:** one 2048-sample Hann frame per `levels()` call, Goertzel power per band, mapped
  to 0..1 over 60 dB, smoothed 0.65 rising / 0.88 falling. At 48 kHz a frame fills every
  ~43 ms, so at most ~23 fresh analyses per second. A 60 fps visualizer must interpolate.
- **Only consumer today:** `MetroEqScreen.kt:231-246`, which adds a consumer while shown and
  polls `levels()` on `Dispatchers.Default`.
- **Shared frame:** `levels()` consumes the single pending frame, so two pollers (the MetroEQ
  page open over a visible visualizer) each see about half the analyses. Smoothing hides it,
  but `VisualizerAudio` should poll from one place and fan out.
- **Android offload:** `AudioOffloadEligibility.meterActive`
  (`androidApp/.../playback/AudioOffloadEligibility.kt`) disables offload while any consumer
  exists, and `ExoPlaybackPlayer` re-applies audio preferences when `hasConsumers` changes.
  So a visible visualizer always gets data on Android, but it keeps the CPU path busy, and
  showing or hiding it reconfigures audio. The visualizer must release its consumer whenever it
  isn't visible (backgrounded, PiP, full-screen video) and shouldn't toggle it rapidly. This
  corrects PLAN.md's "meter unavailable during Android offload": offload is off while the
  visualizer listens.
- For M4: bass/mid/treble and beat can be derived from the 18 bands in `VisualizerAudio`
  without touching the meter. If more is needed (waveform, faster updates), extend the meter in
  shared code with tests and keep MetroEQ's output identical.

## 9. Shaders

`ui/components/ArtworkBackgroundShader.kt` keeps one SkSL/AGSL string in common code
(`artworkBackgroundShader`). Platform actuals in `PlatformLiquidBackgroundCanvas.{android,desktop,ios}.kt`:

- Android: `android.graphics.RuntimeShader`, used only on **API 33+**
  (`Build.VERSION.SDK_INT >= 33`), with a blur or bitmap fallback below.
- Desktop and iOS: Skia `RuntimeEffect.makeForShader(...)` wrapped in `runCatching`, so a
  compile failure falls back instead of crashing.

The visualizers follow the same shape: SkSL source in `visualizer/<name>/`, a common
`Visualizer` interface for uniforms, platform actuals for the canvas, and the API 33 gate with
a gradient fallback on older Android (minSdk is 24).

## 10. Input and focus

- Shared `ui/components/FocusOutline.kt` (`FocusOutlineProvider`, `Modifier.focusOutline`),
  `ui/tv/TvFocus.kt`, `LocalIsTv` (`ui/PlatformLocals.kt`).
- **Desktop global shortcuts:** `desktopApp/.../input/DesktopShortcuts.kt` installs an AWT key
  dispatcher that consumes Space (play/pause), ←/→ (seek), ↑/↓ (volume when the playback
  background has focus), `/`, `D`, digits and more, unless `PlaybackKeyFocus` says a text field
  or focused control has the keys. It is installed by `DesktopShortcutLayer` inside the stock
  `AppContent`, so an XMB host that replaces `AppContent` doesn't get it, and XMB keeps arrows,
  Enter, Esc and Tab for itself. The XMB should still offer media keys (e.g. Space and
  hardware media keys) itself.
- Android TV: D-pad and Back. The stock app requires a double Back to exit from the start
  destination; the XMB keeps that rule at the top level.

## 11. Upstream touchpoints (approved)

Approved by the owner. M1 landed 1, 2, 3, 5 and 7, plus a one-word addition to
`SettingsOrdering.kt` so the interface group sits at the top of Appearance. Android (4) switches
over in M7, once the XMB handles touch there; until then `xmbPlatforms` lists only desktop, so
Android neither shows the setting nor switches. iOS (6) is dropped: iOS isn't a target and keeps
the stock interface.
`UPSTREAM_TOUCHPOINTS.md` is the log of what actually changed.

The XMB sits beside the stock UI. Each platform switches at the point where its shared locals
already exist, so the XMB reuses the platform's `PlaybackController`, player session, Listen
Together manager, auth restore and onboarding without re-creating them.

| # | File | Change |
|---|---|---|
| 1 | `shared/.../constants/PreferenceKeys.kt` | Add `val InterfaceStyleKey = "interfaceStyle"` |
| 2 | `shared/.../settings/Settings.kt` | Add `var interfaceStyle by preference(InterfaceStyleKey, InterfaceStyle.XMB)` and its import. `InterfaceStyle { METROLIST, XMB }` lives in the project-only file `shared/.../xmb/InterfaceStyle.kt`, in that order since enums are stored by ordinal |
| 3 | `shared/.../ui/settings/AppearanceSettingsRegistration.kt` | One `CHOICE` item "Interface style" (XMB / Metrolist) at the top of Appearance, so the stock settings on every platform can switch back to the XMB. Strings `xmb_interface_style*` in `values/strings.xml` |
| 4 | `androidApp/.../ui/MetrolistApp.kt` | Inside `NavigationContentProvider` (l.893): `if (Settings.shared.interfaceStyle == InterfaceStyle.XMB) { XmbAndroidHost(...); return }` before the stock scaffold. `XmbAndroidHost` is a project-only file in `androidApp/.../xmb/` |
| 5 | `desktopApp/.../Main.kt` | In `App()` (l.278): call `XmbDesktopHost(...)` instead of `AppContent(...)` when the style is XMB. The host keeps `MetrolistTheme`, the onboarding gate and the image loader setup from `AppContent` and adds XMB input. Project-only file in `desktopApp/.../xmb/` |
| 6 | `shared/src/iosMain/.../MainViewController.kt` | **Dropped.** iOS keeps the stock `MobileApp(...)`; no edit |
| 7 | `shared/src/commonMain/composeResources/values/strings.xml` | XMB strings with the `xmb_` prefix (upstream file; appended block only) |

Notes:

- Onboarding stays stock on every platform. The switch runs after it completes.
- `assets-ps3/` → `composeResources/{drawable,font,files}` is a `git mv` into new
  project-only names (`xmb_*`), not an upstream edit.
- An upstream merge touching these lines conflicts in small, predictable places; each will be
  logged in `UPSTREAM_TOUCHPOINTS.md` when it lands.
- Alternative for 4 and 5: switch earlier (in `MainActivity.showContent`, `Window`) with fully
  separate hosts. That means fewer lines inside upstream root
  composables, but the hosts would duplicate player connection, controller, Listen Together and
  theme setup that drifts with upstream. Not recommended.

## 12. Stock screens → XMB

| Stock screen / route | XMB location | Fit |
|---|---|---|
| `Home`, `HomeSection`, `Browse` | Music → Home feed; shelves as drill-down levels | Fits. Chips (moods/genres) become a sub-list |
| Quick picks / Speed dial | Music → Quick picks (home section) | Fits |
| `Library` (all filters) | Music → Playlists / Albums / Artists / Songs; Podcasts → Subscriptions | Fits |
| `AutoPlaylist(liked/downloaded/top100)` | Music → Liked songs, Downloads, Top 100 | Fits |
| `Album`, `Artist`, `Playlist`, `Podcast` | Drill-down levels with track lists | Fits. Artist pages have mixed shelves (songs, albums, singles, videos, related): one sub-list per shelf |
| `AboutArtist` | Artist level → Info item, text panel | Fits as a panel |
| `History` | Music → History | Fits |
| `Search` | Search category | Fits; needs the XMB keyboard (M3) |
| `Settings`, `SettingsPage(*)` | Settings category | Registry pages fit. **Custom pages** (Storage, Backup/Restore, Updates, About, Devtools, Fonts, hidden/blocked lists, integrations, Device Connect, Equalizer) open as overlay panels with the stock content first; XMB-native versions later where worth it |
| MetroEQ (`EQUALIZER` page, `MetroEqScreen`) | Settings → MetroEQ | **Flag:** a graphical editor. Overlay panel with the stock screen |
| `Login`, `TokenLogin`, `SwitchChannel` | Users | **Flag:** WebView flows; overlay with the platform's existing login UI |
| `ListenTogether*` (+ settings, server, blocked users, logs) | Friends | Rooms/join/invite fit; logs and server config go under Settings → Listen Together |
| `Integrations`, `Discord`, `LastFm`, `ListenBrainz` | Settings → Integrations | Overlay panels (custom pages) |
| `Statistics`, `StatisticsDetail` | Music → Statistics | **Flag:** charts and date ranges don't map to a list. Overlay panel |
| `Recognition`, `RecognitionHistory` (Android only) | Music → Recognize | **Flag:** microphone UI. Overlay panel; Android only |
| `PlaylistImport`, `BackupRestore` | Settings → Backup | Overlay panels |
| `Downloads` (Android) | Music → Downloads | Fits |
| `AndroidAuto`, `PlaybackBenchmark`, `Devtools` | Settings (Android only / dev) | Overlay panels |
| `EasterEgg` (Android top-level route) | Not mapped | **Flag:** left out of the XMB |
| Expanded player, queue, lyrics | Now-playing info + △ menu → Queue / Lyrics; full screen = visualizer | Queue as an XMB list; lyrics view is M6 |
| Video player | Video category; plays over the visualizer | **Flag:** needs a video surface layer above the background (M6). iOS has no video |
| Cast picker | △ menu → Cast | Fits (device list from `PlaybackUiState.castDevices`) |
| Sleep timer, tempo/pitch dialogs | △ menu entries → XMB value pickers | Fits |
| Desktop mini-player window, tray | Unchanged | Separate windows; keep stock |
| Android widgets, notifications, Android Auto | Unchanged | Not part of the in-app UI |

## 13. Asset findings (for M1)

- **Icon sizes vary.** The manifest specifies 128×128 for category icons, but
  `category/users.png` is 68×68, as is `items/artist.png` (both from `menu_user`); they'll look
  soft when enlarged on focus. Ten item icons and all 15 UI icons are 256×256, the rest 128×128.
  M1 draws every icon at a fixed dp size, and the manifest test enforces a minimum of 128 px.
  The owner may want sharper `users`/`artist` sources.
- **Sounds are Ogg Vorbis** (48 kHz). Android (`SoundPool`) and desktop (bundled FFmpeg) can
  play them. iOS AVFoundation can't decode Vorbis, which doesn't matter since iOS keeps the
  stock interface.
- Font: `SCE-PS3-Rodin-LATIN-Regular.ttf`, TrueType, regular weight only. Compose synthesizes
  bold; non-Latin falls back through the app's existing font stack.

## 14. M0 build log

Environment: Linux x64 cloud container, 4 cores, 15 GB RAM, running as root, no IPv6.

**Setup used** (local only, nothing committed):

- Temurin 25.0.5+8 from Adoptium's GitHub release, checksum-verified, in `/opt/jdks`;
  `~/.gradle/gradle.properties` points `org.gradle.java.installations.paths` at it with
  auto-download off. The build's toolchains require vendor `ADOPTIUM` and version 25.
- Android SDK in `/opt/android-sdk`: command-line tools 16111833, `platform-tools`,
  `build-tools;36.0.0`, `platforms;android-35`, `platforms;android-37.1`, plus `sdk.dir` in
  the untracked `local.properties`.
- Submodules `metroproto` and `libopus` initialized.

**Upstream tooling issues found** (left unchanged; they're upstream files):

- `scripts/deps.sh` checks `java -version | head -n1` for version 25. When `JAVA_TOOL_OPTIONS`
  is set, the first line is the JVM's "Picked up JAVA_TOOL_OPTIONS" notice, so the check fails
  and the script tries to install SDKMAN.
- `scripts/deps.sh` installs `platforms;android-37`, but `compileSdk` is
  `release(37) { minorApiLevel = 1 }`, which needs `platforms;android-37.1`.
- `docs/checks.md` lists `scripts/test_tester_release_version.py`, which doesn't exist.

**Maven Central rate limits.** With the project's `org.gradle.parallel=true` and
configuration cache, the first resolution sends bursts that Maven Central answers with HTTP 429
from this network (6 of 40 concurrent requests in a probe; sequential requests all succeed).
A cold cache needed one pass with `--max-workers=1 --no-parallel --no-configuration-cache`.
Warm builds run normally.

**Desktop**

| Command | Result |
|---|---|
| `./gradlew :desktopApp:compileKotlinJvm` | Passed |
| `./gradlew :shared:desktopTest -x :shared:testServerWithoutReflection` | 3,589 tests, 9 failed, 5 skipped. All 9 are environmental, see below |
| `./gradlew :shared:testServerWithoutReflection` | 78 tests, 9 failed, 1 skipped. All environmental |
| Repository scripts from `docs/checks.md` (escapes, translation mechanics, hooks, iOS/AppImage/DMG/distribution/AltStore/metromem/APK collection/Android strings tests) | All passed |

Environmental failures:

- `LocalDeviceServerTest`, `DeviceConnectManagerIntegrationTest`,
  `DeviceConnectManagerShutdownTest`: `LocalDeviceServer` binds `::` (dual-stack), and this
  container's kernel has no IPv6 (`UnsupportedAddressTypeException`, "Address family not
  supported"). Expected to pass on a normal desktop.
- `DesktopSettingsCorruptionTest` (2): makes files unreadable with POSIX permissions, which root
  bypasses. Expected to pass as a normal user.

**Android**

| Command | Result |
|---|---|
| `./gradlew :androidApp:assembleDebug` | Passed: `androidApp-debug.apk` (47 MB). The first run installed NDK 30.0.16248370 and CMake 4.1.2 for `opusDecoder`'s `buildOpusJni` |
| `./gradlew :androidApp:testDebugUnitTest` | 872 tests, all passed |

Robolectric's `nativeruntime-dist-compat` jar is redirected by Maven Central to
`gcp.mirrors.maven.org`, so that host must be reachable as well as `repo.maven.apache.org`.

**Network hosts the build needs** beyond the default package registries: `dl.google.com`
(Google Maven, Android SDK), `jitpack.io` (InnerTubeX, QuackJS), `jogamp.org`,
`maven.firstdark.dev`, `maven.aliyun.com` (declared in `settings.gradle.kts`),
`gcp.mirrors.maven.org` (Central's redirect target) and `github.com` (Temurin download,
submodules). The owner's `metroxmb` cloud environment allows these and has a setup script that
installs the JDK and SDK as above.

**Not built in M0:** iOS (needs macOS and Xcode), release/packaging tasks, and the
desktop app launch (no display in this container; `scripts/xmb/snap.sh` in M1 needs a virtual
display).

## 15. M3: Settings, Search and Users

The three columns take their items from live data (`xmbSettingsItems`, `xmbSearchItems`,
`xmbUserItems` in `nav/XmbCatalog.kt`); the host passes what they need in `XmbServices`.

- **Settings.** `XmbSettingsHost` carries the platform's `SharedSettingsHost` and a way to show
  a stock settings page. `XmbApp` builds the registry with `rememberMetrolistSettingsRegistry`,
  exactly as the stock screen does, and rebuilds it after each change (a revision counter, like
  `SharedSettingsRegistryScreen`). Root pages become column items; a page opens as a level whose
  rows are its items (`adapters/XmbSettings.kt`). Selecting a row goes through `xmbSettingStep`:
  switches and choices open a value list, sliders a slider, text the on-screen keyboard, actions
  run (after a Yes/No when the registry asks for confirmation), and navigation rows open the next
  registry page. Custom pages (storage, backup, updates, about, devtools, MetroEQ), custom rows
  and pages the registry doesn't build (fonts) open the stock screen over the XMB. The registry's
  own callbacks write the settings and fire the host's change effects, so behaviour matches the
  stock screen.
- **Desktop settings host.** `desktop/xmb/DesktopXmbSettings.kt` mirrors the registry-facing
  parts of the stock `SettingsOverlay` host (language, country and artist-source choices, change
  effects, the Listen Together speed lock, home sections). It's a copy rather than a refactor of
  the overlay, so upstream merges don't conflict there; its `when`s over `SettingsChangeEffect`
  and `SettingsHostChoice` are exhaustive, so an enum value added upstream fails to compile and
  can't be missed. Check the overlay's host when merging a new Metrolist release.
- **Stock screens over the XMB.** The desktop host draws `SettingsOverlay` (opened at a page
  through `UiState.selectedSettingsPage`) and the sign-in `LoginDialog` itself, since the stock
  content that normally draws them isn't composed. While either is open `XmbApp` is `suspended`
  and leaves keys alone; focus returns when they close.
- **Search.** `adapters/XmbSearch.kt` uses the shared `SearchViewModel` for recent searches
  (`Settings.shared.searchHistory`) and suggestions, and the repository's search summary for
  results, filtered for blocked content like the stock screen. Results open as one folder per shelf.
  The XMB's view model is its own instance, so the stock search screen sees new history after a
  restart, not live.
- **Users.** The account comes from `Settings.shared` (name, email, picture, cookie), which is
  snapshot state, so signing in or out updates the column. `XmbAccount` supplies the platform's
  flows: on desktop, the stock sign-in and channel dialogs and `AuthState.signOut()`.
- **Wave.** The background wave and sparkles react to navigation through `XmbWaveMotion`
  (`docs/xmb-notes.md`), not to audio; the meter stays for the visualizers.

