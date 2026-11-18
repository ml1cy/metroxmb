# XMB tuning notes

Constants tuned by eye and the reason for each (PLAN.md, "Working method"). Update an entry
when a value changes, and say what it was compared against. Layout values are measured from the
owner's console frames in `reference/xmb/` (`settings-red-1080p.png`, `game-purple-1080p.jpg`;
1.5 px per unit at 1080p); timings and the wave's motion are still first passes, because no
video frames exist yet.

## Layout (`xmb/theme/XmbTheme.kt`, `XmbMetrics`)

One layout unit is 1/720 of the window height, or 1/960 of the width when the window is
narrower than 4:3, so the XMB keeps its proportions at any size.

| Constant | Value | Reason |
|---|---|---|
| `ANCHOR_X` | 0.295 of width | Focused category at x = 566 of 1920 in both 1080p frames (was 0.22 from memory) |
| `BAR_Y` | 0.27 of height | Category icons centered at y = 290 of 1080 (was 0.30) |
| `categorySpacing` | 133 units | Icon centers 200 px apart (was 136) |
| `categoryIcon` | 120 units | Box size. The PS3 PNGs pad their glyphs (glyph 46–74% of the box), so a 120-unit box draws the toolbox 113 px wide focused, as in the red frame; unfocused icons shrink to 0.66 (film icon 85 px wide in the purple frame) |
| `categoryLabelOffset` | 40 units, label centered there | "Settings" and "Game" labels centered 60 px under the icon center (was 54 units to the label's top) |
| `itemIcon` | 100 units | Focused item glyph about 105–128 px wide; unfocused items shrink to 0.6 (about 70 px) |
| `focusedItemOffset` | 143 units | Focused item centered at y = 505, 215 px under the row |
| `itemSpacing` | 53 units | Rows below the focus 80 px apart |
| `itemsBelowGap` | 64 units | First row below the focus at y = 681: 176 px, so the focused item gets an extra 96 px for its subtitle |
| `itemsAboveGap` | 204 units | Rows above the focus at y = 120 and 40 in the purple frame, above the category row |
| `itemTextOffset` | 27 units after the icon box | Titles start at x = 682, 116 px right of the icon center |
| Title block | title centered on the icon, subtitle hanging below | "System Update" is level with its icon; its description sits under it |
| Text sizes | label 19, title 23, subtitle 15, clock 20 units | Cap heights in the 1080p frames |
| `valueColumn` | 440 units from the title's start | Where settings rows show their current value; leaves room for the value picker inside 1920 px |
| Clock box | top 50, height 37, width 393 units, running off the right edge; text, then the clock icon 77 units from the edge | The box at x 1330–1920, y 75–131 in both frames; border white at 33% (sampled (168, 92, 92) over (124, 0, 0)), fill black at 6% |
| Focus halos | removed | Neither frame shows a halo around the focused icon; only the focused title glows |
| Face icon | drawn at 0.78 | `users.png`/`artist.png` fill 71% of their frame against 50–63% for the rest of the set, so they looked heavier |
| Visible items | 6 above, 10 below | Enough to fill a 16:9 window; the rest aren't composed |

## Motion (`XmbMotion`)

| Constant | Value | Reason |
|---|---|---|
| `CATEGORY_MS` | 240 ms | Horizontal scroll; quick but visible, so repeated presses chain smoothly |
| `ITEM_MS` | 180 ms | Vertical scroll is a little faster than horizontal, as on the console |
| `scrollEasing` | cubic-bezier(0.25, 0.1, 0.25, 1) | Decelerating ease; the motion settles rather than stopping hard |
| Unfocused category | scale 0.66, alpha 0.45 | Measured in the 1080p frames (was 0.78 and 0.55) |
| Unfocused item | scale 0.6, alpha 0.62 | "Game Settings" rows in the red frame |
| Category label fade | alpha = 1 − 2 × distance | Label is gone halfway to the next category, so two labels never overlap |
| Title glow | white shadow, 60% alpha, 8 units blur | Focused item and category label only |

## Background (`xmb/theme/XmbPalette.kt`, `visualizer/background/XmbBackground.kt`)

| Constant | Value | Reason |
|---|---|---|
| Monthly colors | Jan `#CBCBCB` silver, Feb `#D8BF1A` gold, Mar `#6DB217` lime, Apr `#E17E9A` pink, May `#088B01` green, Jun `#9A61C8` purple, Jul `#02CDC7` cyan, Aug `#0C76C0` blue, Sep `#B444C0` magenta, Oct `#E5A708` amber, Nov `#875B1E` brown, Dec `#E3412A` red | The console's documented monthly theme colors |
| Brightness | 1.0 from 08:00 to 17:00, 0.42 from 22:00 to 05:00, smoothstep ramps between | The console dims its background in the evening; checked continuous to under 1% per minute |
| Gradient | month color × brightness × 0.375 at the top rising to × 1.625 at the bottom | RetroArch's ribbon background (via tsbehlman/xmb); matches the red frame's dark top and bright bottom (was lighter at the top) |
| Ribbon | RetroArch's XMB ribbon, ported from tsbehlman/xmb (GPL-3.0): a 50 × 50 sheet folded by cosines and value noise, drawn as a triangle mesh with `drawVertices`; each vertex's brightness is how edge-on the sheet is there, (1 − cos(e²)) × 0.75, added onto the background, so overlapping folds make the bright edges and silk lines. Its clock runs at 0.25 × real time and starts a quarter turn in, so it first rises from the lower left | Matches both 1080p frames and needs no shader, so it runs the same on every platform |
| Particles | 400 soft points from the same port: they drift both ways along the ribbon at their own speeds, bob around it, twinkle, and are 3 px mostly (a few up to 11 px at 1080p); added onto the background | The specks around the band in the frames |
| Animation | One frame clock in `XmbApp`, paused while `LocalVisualWorkActive` is false | Hidden windows do no animation work (docs/rendering.md); frames redraw without recomposing |

## Wave reactions (`visualizer/background/XmbWaveMotion.kt`)

The home-screen wave reacts to navigation, not to music; reacting to music is the visualizer's
job (owner's call). All reactions settle within about two seconds.

| Input | Reaction | Constants |
|---|---|---|
| Left / Right across categories | The ribbon's flow and the particles get a sideways push opposite to the move (moving right slides the scene left); repeated presses build up | kick 1, max 3, flow × (1 − 1.6 × push), sparkle drift + 0.07 widths/s per unit, decays at 2.4/s |
| Up / Down through items | The wave and sparkles lift (Down) or dip (Up) on a spring with a slight overshoot | kick 1.4, stiffness 32, damping 7 (about 8% overshoot), 6% of the height per unit |
| ✕ select | The ribbon brightens (opacity × 1.7 at the peak), its folds deepen and the particles flash | flash 1 decaying at 2.6/s, swell + 0.6 decaying at 1.6/s (folds × 1 + 0.9 × swell) |
| ○ back | A softer pulse | flash 0.4, swell + 0.25 |
| Error sound | A quick shiver | decays at 5/s |
| Hidden window | No frames, no motion; a long gap counts as one 0.1 s step | `MAX_STEP` |

## Drill-down levels (`xmb/bar/XmbLevelView.kt`, M2)

| Constant | Value | Reason |
|---|---|---|
| Focus | the column's focus point (y = 505 at 1080p), with the same gaps above and below | Opening a folder keeps rows where they were (was 0.42 of the height) |
| `parentOffset` | 150 units left of the list | The parent row's icon sits to the left of the list like the console's folder navigation; only the immediate parent shows |
| Rows shown | 6 above, 10 below the focus | Fills a 16:9 window; the rest aren't composed |
| Artwork | 70% of the icon box | Album art has no padding, so it would dwarf the PS3 glyphs at full box size |
| Level transition | category row fades out over `FADE_MS`; rows slide in from 40 units right | Opening reads as moving into the folder |
| Artwork | album art or photo cropped square, 6% corner radius; the XMB icon shows until it loads or if it fails | Album art replaces item icons, as the manifest notes |
| History list | latest 200 plays, one row per song | Enough to scroll; paging can come later |

## Waves visualizer (M4; `visualizer/`)

Checked with real music (`VisualizerMusicTest`): the console's menu music, played through the
meter in real time (a chunk every frame, polled every 33 ms), moves Waves (energy median 0.28,
bass up to about 0.8, about 55 beats a minute at natural spacing) and settles within 2.5 s of
silence. A synthesized 120 BPM track: 12 of 16 kicks make a beat, 33–100 ms after the kick; the
first one is missed while the range is set, and pads alone make none.

The ribbon is drawn from reused arrays through each platform's own `drawVertices`
(`RibbonMesh.desktop.kt`, `RibbonMesh.android.kt`) and the particles from one glow sprite: a
1080p frame allocates about 40 KB instead of 1.7 MB (`RibbonAllocationTest`). Android 7–9 can't
draw vertices on a hardware canvas, so there the mesh is drawn in software at half size and
scaled up.

From the owner's console videos and photos (`reference/waves/`): `video-waves-quiet.jpg`,
`video-waves-loud.jpg` and `video-waves-loud-5fps.jpg` (five frames a second through a loud
passage) for the look and reactions, `video-track-overlay-1080p.jpg` for the track overlay.

| Constant | Value | Reason |
|---|---|---|
| Band groups | bass = meter bands 0–3 (45–125 Hz), mid = 4–10 (180 Hz–1.4 kHz), treble = 11–17 (2–15 kHz) | The meter's 18 bands split into what moves the ribbon and lights it |
| Input | `AudioSpectrumMeter.frameLevels()`: the frame `levels()` just analyzed, before the EQ bars' smoothing | The bars' smoothing (35% of each new frame) blurred kicks: with it, 6 of 16 test kicks made a beat; without it, 12 |
| Level shaping | per band group, a range follower: ceiling jumps to peaks and sinks over 6 s, floor drops to lows and rises over 3 s, both start at the first sound after silence; the level's place in that range, at least 0.12 wide; under 0.05 is silence | A fixed floor (0.35) read the console's menu music (about −21 dB mean, levels 0.2–0.35) as silence: Waves didn't move at all. Following the range, quiet and loud recordings both use 0..1 |
| Smoothing | attack 18/s, release 4/s | Reacts within a few frames, falls back gently |
| Beat | bass at least 0.08 above its 0.6 s average and above 0.2, at most every 0.18 s, and only after the rise has fallen under half that since the last beat; decays at 5/s | Bass onsets, not held notes; without re-arming, a long swell fired a beat every 0.18 s |
| Flow | time × (1 + 1.5 × energy); the ribbon's clock is 0.35 × flow | The ribbon travels faster when the music is louder |
| Particle clock | time × (1 + 5 × energy + 4 × beat) | In the loud video frames the particles streak while the ribbon keeps its pace |
| Polling | `AudioSpectrumMeter.levels()` every 33 ms off the main thread, only while full screen | The meter only captures while it has consumers (CLAUDE.md) |
| Sky | fixed violet, not the month's color: (110, 40, 240) at the top, (70, 14, 175) at 35%, (35, 8, 125) at 60%, (10, 4, 45) at 85%, (5, 2, 20) at the bottom | Sampled from the video; every Waves source (videos and photos) is violet |
| Ribbon | the home ribbon at its usual height (low, as in the videos); tint (1, 0.82, 1); folds × (1 + 0.8 × bass); 4 + 5 × mid folds across its depth; opacity 0.75 × (1 + 0.5 × energy + 0.6 × beat) | The video's lavender-pink strands; loud passages turn the band into a bundle of thin silk lines |
| Particles | opacity 0.75 × (0.8 + 0.8 × treble + 0.8 × beat); size × (1 + 0.5 × energy + 0.4 × beat); a trail as long as their move in 1/20 s | Dots when quiet, bigger and streaking when loud, as in the console's loud frames |
| Track overlay | at 1080p: artwork 100 px, 118 px from the left; title 36 px and "artist / album" 28 px beside it; a 386 px block 116 px from the right with the queue position, "00:01:53 / 00:02:14" and a silver codec pill with a speaker mark, then a 9 px green bar on a dark grey track; shows for 4 s after opening or any key, fading over 0.6 s | Measured from the 1080p console capture (the Linkin Park photo's blue tones were the camera) |
| Full screen | fades in and out over 600 ms; ○ returns to the XMB | The console hides its menu while a visualizer plays |

## M6: podcasts, video, friends, lyrics, casting

| Constant | Value | Reason |
|---|---|---|
| Full-screen modes | visualizer (with optional lyrics), video, lyrics; all fade over 600 ms, hide the menu, and show the track overlay for 4 s after opening or any key | One way in and out for everything that takes over the screen; ○ steps back (lyrics off first, then the XMB) |
| Video | `setVideoMode(true)` when shown, `false` when leaving; the host's surface on black, scaled to fit | The console's Video column plays full screen; leaving turns video off so audio-only streaming resumes |
| Lyrics | current line 34 units with a white glow, others 24 units at 70%, fading with distance; 3 lines each side, 64 units apart, centred at 40% of the height on a soft dark band; slides over 400 ms; position read every 100 ms | Readable over the ribbon and clear of the track overlay |
| Cast list | discovery runs only while Play on device is open; the connected device stays listed and shows Connected or Connecting | Discovery costs network traffic |
| Friends | the column follows the room manager: off, idle (create, join, name), waiting, in a room (code, people, requests, leave) | Everything Listen Together needs, without leaving the XMB |

## Settings, value picker and keyboard (M3)

| Constant | Value | Reason |
|---|---|---|
| Settings rows | every item of every group of a registry page, flattened; current value in the value column; disabled rows at 40% | The console's settings lists are flat with the value on the right |
| Picker | opens at the value column, values 44 units apart around the row with the current one on a 14% white band, black 45% panel; other rows dim by 60% | Like the console's value lists; a question (confirmations) shows above the values |
| Slider picker | ◀ value ▶ with a bar; Left/Right move by the registry's step (or a twentieth) and snap to that grid | Applies on ✕ only, like the list picker |
| Keyboard | 10-column grid, keys 54 × 46 units with 6-unit gaps, Shift 2, Space 4, Delete 2, Done 2 cells; suggestions column 300 units; menu fades to 20% behind a 50% black scrim | No console frame of the on-screen keyboard yet; a plain readable grid until there is one |
| Typing | physical keys type straight in and move focus to Done, so Enter submits; Backspace deletes; △ (Tab) types a space | D-pad and keyboard users both get a one-key submit |

## Options panel (`xmb/screens/XmbOptionsPanelView.kt`, M2)

| Constant | Value | Reason |
|---|---|---|
| Width | 360 units, right edge | A side panel that leaves the list visible |
| Background | horizontal black gradient, 35% to 65% alpha | Darkens toward the edge so the options read over any month's color |
| Option spacing | 22 units | Roomy list like the console's option menu |
| Slide | `FADE_MS` with the scroll easing | Matches the level transition |

## Valley and Gaia (M5; `visualizer/valley`, `visualizer/gaia`)

Both are runtime shaders (`XmbShader`: Skia's `RuntimeEffect` on desktop, `RuntimeShader` on
Android 13+), from the owner's 2560×1440 recordings (`reference/valley/`, `reference/other/`). The
calm clock (`VisualizerAudio.calmFlow`, the flow with its speed smoothed over 1.5 s) drives both,
so louder music moves them faster without jitter.

| Constant | Value | Reason |
|---|---|---|
| Valley terrain | 28 / 18 / 10 layers (quality) from 8 to 1.2 units away, drawn near to far: a pixel stops once the ground in front covers it, and a layer whose highest possible ridge is below the pixel skips its noise; heights from up to 4 / 3 / 2 octaves of value noise (quintic, with a hash without sine), ×(0.85 + 0.6 × calm bass); broad mountains on the far layers | The recordings' soft ridges in depth. The near limit of 1.2 keeps the front ridges from rising over the screen; sine hashes lost precision on GPUs and showed blocks |
| Valley edges | soft by 0.004 near the horizon to 0.034 close by; crests glow over 1/55 to 1/14 of the screen, ×(0.5 + 0.5 × calm energy + 0.8 × beat); the slope below a crest darkens over 1/18 to 1/4 of the screen; haze by distance | The recordings are blurry close up, the ridges' tops are bright and the slopes dark. Lit all the way down, near ridges looked like curtains |
| Valley motion | the flight follows `calmFlow` (energy smoothed over 1.5 s) at 0.6 units a second, wrapping every 400 units | Raw levels made the peaks jitter and the speed stutter |
| Valley colors | molten, gold, night blue, crimson; 28 s each, blending over the last 30% | The recordings drift between these |
| Gaia views | close (radius 1.25 × height), globe (0.42), sunrise (0.62, sun behind), far (0.12); 40 s each, moving over the last 25% | The recording's four views, in its order |
| Gaia surface | NASA Blue Marble surface and clouds (4096×2048), water mask and Black Marble city lights (2048×1024), each with a quarter-size copy blended by on-screen size (a manual mip level) and linear filtering; clouds drift east; sun glint only on water; city lights on the night side ×(0.8 + 0.5 × beat); the limb is anti-aliased over one pixel | A real satellite look. Without the small copies, the far globe shimmered |
| Gaia while loading | the procedural Earth (3D noise, 5 / 4 / 3 octaves) until the maps are decoded, or if they fail | Decoding takes a moment on phones |
| Fallback | Android 12 and older: layered curves (Valley), a lit disc with a glow (Gaia) | `RuntimeShader` is Android 13+ |
| Auto-cycle | 45 s each, 1.5 s crossfade | |
| Quality | High / Balanced / Low also sets the ribbon's passes (8 / 4 / 1) | |
| Frame rate | 30 fps skips every other display frame (nothing redraws) | Saves power |

## Touch and portrait (M7)

| Constant | Value | Reason |
|---|---|---|
| Taps | every part takes its own taps (`xmbTap`): keys and suggestions, category icons, the focused column's and an opened level's rows, the level's parent icon, picker values and slider arrows, options, notice buttons | Taps go to what's under the finger; faded neighbor columns and the bar behind an opened folder don't take taps, since they're laid out but invisible |
| Tap rule | down and up within twice the platform's touch slop, before the long-press time; the mouse's secondary button is a long press | Fingers wobble: with the plain slop, a wobble without a step was lost |
| Row tap | a row that isn't focused is focused; the focused row is selected; a long press focuses and opens options (△) | Like moving there and pressing ✕ |
| Outside tap | closes a picker or the options; full screen, shows the track | |
| Drag step | one category spacing across, one item spacing down; the first step half a step past twice the touch slop | Same steps as the arrow keys; short drags still move |
| Direction | drag up = next item, drag left = next category | The content follows the finger |
| Flick | the release speed times 0.18 s, in steps, at most 12, from 8 steps a second up; paced 40 ms + 12 ms more per step | Long lists without long drags |
| Mouse | clicks work like taps; the wheel moves; dragging doesn't | |
| Portrait unit | min(height / 900, width / 480): about 0.86 dp on a 411 dp phone | The landscape rule (width / 960) made titles 10 sp |
| Portrait anchor | focused category at 20% of the width; values 220 units after the text | Room for the row and for values on screen |
| Portrait full screen | artwork and titles above the queue position; the time and a full-width bar below, with the card's 32-unit margins | The console's side-by-side layout doesn't fit upright |

## XMB settings and keyboard shortcuts (`xmb/settings/`)

The Settings column starts with the XMB's own pages (Theme, Date and Time, Sound, Keyboard
Shortcuts, Music Player). Their rows are `SettingsItemRegistration`s, so the XMB's settings rows
and value pickers draw them like the registry's; each writes its value to `Settings.shared`
(`xmb*` properties), whose reads recompose the XMB. `XmbPrefs.current()` reads them all.

| Setting | Values | Notes |
|---|---|---|
| Color | Original, the 12 monthly colors, Custom | Custom is `Color.hsv(hue, 0.8, 0.8)`, about as strong as the monthly colors |
| Background image | file chosen by the platform (`XmbWallpaper`) | Drawn to cover the screen, dimmed by Image dimming, faded out as Waves opens |
| Wave / sparkles off | home only | Waves always draws them; they fade in with the move |
| Wave speed | 0.5, 1, 1.75 | Scales the home wave's clock |
| Menu sound volume | 0–100% | Desktop sets each clip's gain (`EFFECT_VOLUME` × volume) |
| Visualizer when idle | off, 30 s, 1, 3, 5 min | Only while music plays and nothing is open; any menu key closes it again, media keys don't |

Shortcuts are saved by key name (`"PLAY_PAUSE=Space;NEXT_TRACK=Ctrl+Right;..."`), so a save means
the same keys on any platform; a shortcut missing from a save gets its default, an empty one
stays cleared. Defaults follow common music players (Spotify's Ctrl/Shift+arrows, Ctrl+S/R).
Space is now a shortcut rather than a fixed key, so it can be moved. Menu keys and media keys
can only be used with a modifier, and taking a key from another shortcut clears it there.
Shortcuts run before the menu but not while the on-screen keyboard is open, and show a short
message (shuffle, repeat, volume, like) under the clock.

## Now playing and the move to full screen (`xmb/screens/XmbTrackInfo.kt`)

One view draws the track in both places: the card at the bottom right (below) and, full screen,
the console's overlay (Waves section). Opening a full screen moves it from one to the other on the
same 900 ms ease as the backdrop: the artwork and titles slide left and grow, the bar grows into the
console's green one, the artist line crosses over in place, the card's time leaves (10–40%) before
the overlay's arrives (50–85%) so the two never overlap, and the queue position and codec badge
come in over the second half. The menu and clock fade over the first 60%. Going back runs it in
reverse, with the track shown even if its overlay had faded.

The backdrop (`visualizer/waves/XmbBackdrop.kt`) is one picture for home and Waves: the sky blends
from the home gradient to the Waves violet at the Waves stops, every ribbon setting blends, and
`RibbonClockBlend` hands the ribbon's clocks over without a jump, so the home wave turns into the
visualizer's instead of one fading over the other.

The ribbon mesh is drawn 8 times at an eighth of its strength, each pass offset by one of the
standard 8x multisampling positions: `drawVertices` has no anti-aliasing, and where the sheet turns
edge-on (crests and troughs) its triangles get thinner than a pixel and the silk lines stepped. Colors are 8-bit,
so each pass's share is nudged by a different fraction of a step (`ribbonPassAlpha`): plain eighths
rounded alike in every pass, kept 32 of 256 levels and showed as blocky patches in the sheet.

| Constant | Value | Reason |
|---|---|---|
| Position | bottom right, `edgeMargin` from the edges | PLAN.md: track info in the bottom right |
| Artwork | 56 units | Same as a focused item icon |
| Text column | 280 units wide; title 16, artist and times 13 units | Small enough not to compete with the menu |
| Progress bar | 3 units, white over 25% white | Thin, like the console's |
| Paused | whole panel at 70% alpha | Shows the state without another icon |
| Position polling | every 500 ms while playing, never while paused | Smooth enough for a seconds display without per-frame work |

## Status

| Constant | Value | Reason |
|---|---|---|
| Clock format | `M/D H:MM`, 24-hour | Simple and locale-neutral until a clock setting exists |
| Clock icon zoom | 4× | The clock PNG's glyph fills only 56 of its 256 pixels |
| Clock refresh | every 15 s | Minute changes show within 15 s without a per-frame clock |

## Sound (`DesktopXmbSoundPlayer`)

| Constant | Value | Reason |
|---|---|---|
| Effect volume | 0.7 (−3 dB) | Effects sit under the music |
| Repeat behavior | restart the clip | Fast navigation ticks like the console instead of piling up |

## Stock pages as XMB levels (M7; `xmb/settings/XmbSystemPages.kt`)

Custom registry pages the XMB draws itself, as rows like any settings page. Each platform gives
the parts only it has (`XmbSystemPages`: `XmbStorage`, `XmbBackup`); the rest is shared.

| Page | Rows | Notes |
|---|---|---|
| About | version (three presses unlock the developer tools, like the stock logo), Metrolist, license, ribbon and Earth imagery credits | Links open in the browser |
| MetroEQ | on/off, profile (presets and saved), preamp, one slider per band (±15 dB in 0.5 dB steps), reset, save as | `EqualizerProfiles`, as the stock page; AutoEQ search and file import stay on the stock page |
| Storage | clear downloads, song, image, animated artwork and music video caches (each with its size, after a Yes/No); song, image and animated artwork limits; the download folder (opens the stock page to change it) | Sizes refresh every 3 s |
| Backup and restore | create (after the stock warning that backups hold credentials), restore (the app restarts), playlist import and more (stock page) | System file pickers |

Other custom pages (developer tools, Android Auto, supported links, AI setup) and custom widgets
inside registry pages (previews, OAuth sign-ins) still open the stock screen.

Integrations (Discord, Last.fm, ListenBrainz, Device Connect, Listen Together) join the settings
column as one more group, from the stock integrations registry (`rememberIntegrationsRegistry`).
Their sign-ins and the Listen Together screen open the stock integrations screen.

## Links and shortcuts (M7; `XmbOpen`)

A link, app shortcut or notification becomes an `XmbOpen` (a category, and a level to open over
it), passed through `XmbInputController.open`. It waits until the XMB is shown, then closes what
was open and opens the place. Albums, playlists, artists, podcasts, downloads, liked songs,
search, settings and Listen Together open in the XMB on both platforms; sign-in, song recognition,
browse pages, playlist import and joining a room by link on Android still open stock screens.

## Messages over the XMB (M7)

| What | How | Why |
|---|---|---|
| Snackbars | shown as XMB toasts (`SnackbarsAsXmbToasts`); on desktop a snackbar with an action (an undo) stays a snackbar so the action works | Android's stock snackbar host isn't drawn in the XMB, so messages were lost; desktop's was a Material bar over the XMB |
| Expired sign-in, Listen Together guest choice (Android) | XMB notices, as desktop already did for the sign-in and playback errors | The guest choice was only composed with the stock layout, so a guest's song request did nothing |
| Row values | fade out over 24 units where they'd pass under the clock box or within 78 units of the bottom (the now-playing card) | They overlapped both |

## First-run setup (M7; `XmbOnboarding`)

The onboarding pages (privacy, permissions, migration on Android, finish) keep their platform logic
and draw as a notice band over the XMB's sky and wave: "Setup n/m", the title and text, and the
choices as buttons (wrapping in portrait). ←/→ choose, ✕/Enter/Space confirms, ○/Esc goes back a
page; buttons take taps. The language, lyrics and playback pages are left out: those settings are in
the XMB's Settings column.

## Devices (M7)

The device list (△ on the playing song, or the cast item) shows the cast devices, then the
user's other devices that Device Connect has connected (`peerEntries`). Selecting one moves
playback there; selecting the one playing hands playback back to this device. Device Connect is
switched on in Settings > Integrations > Device Connect.
