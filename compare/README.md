# XMB captures

Rendered by `scripts/xmb/snap.sh` (headless, fixed clock: 16 November 14:30 unless noted, 1920×1080).
Put a console frame from `reference/` next to a capture to compare; differences still to check are
listed below. Tuned values and their reasons are in `docs/xmb-notes.md`.

| Capture | Shows |
|---|---|
| `icons.png` | Every category icon at 128 px: the six PNGs, then the two new vector icons (Podcasts, Visualizer) on a faint tile. **For the owner to approve** |
| `shell.png` | Music focused, first item focused, November brown, daytime |
| `shell-night.png` | Same at 23:30: dimmed background |
| `music-scrolled.png` | Music with Albums focused: earlier items move above the category row |
| `settings.png` | Settings column, with the "Metrolist interface" item that switches back to the stock screens |
| `visualizer-august.png` | Visualizer column in August's blue |
| `small-window.png` | 960×600 window: the layout scales by height, or by width in narrow windows |
| `music-home.png` | Music > Home opened (demo data): home shelves as folders, the parent icon on the left, now-playing in the bottom right |
| `music-section.png` | A home shelf opened: songs, the focused one with its artist and album, artwork where available |
| `music-options.png` | The △ options panel for the focused song |
| `settings-december.png` | Settings column in December's red with the registry's pages, for comparing with `reference/xmb/settings-red-1080p.png` |
| `reference-vs-settings-december.png` | That reference frame (left) next to `settings-december.png` (right) |
| `settings-page.png` | The Player and audio page as XMB rows, current values in the right-hand column |
| `settings-picker.png` | A value picker open beside the focused row |
| `search-keyboard.png` | The on-screen keyboard with typed text and suggestions |
| `users.png` | Users signed out: Log in |
| `visualizer-waves.png` | The Waves visualizer full screen (September), silent: idle motion, with the PS3-style track overlay |
| `waves-loud.png` | Waves with loud fake meter levels just after a beat: deeper folds, a brighter sheet and particles |
| `reference-vs-waves.png` | Console video frames (left: `video-waves-quiet.jpg` above, `video-waves-loud.jpg` below) next to `visualizer-waves.png` and `waves-loud.png` |
| `live-desktop-xmb.png` | The real desktop app (Linux, virtual display, 1080×720 window) right after onboarding: the XMB replaces the stock screens |
| `live-desktop-interface-setting.png` | The stock Settings → Appearance page with the new Interface style choice (before it moved to the top of the page) |

## Reference frames

From the owner, in `reference/`: `xmb/settings-red-1080p.png` and `xmb/game-purple-1080p.jpg`
(1080p screenshots, used for every layout measurement in `docs/xmb-notes.md`),
`xmb/music-green-small.jpg`, `xmb/users-blue-photo.jpg` and `xmb/network-pink-photo.jpg`
(small or photographed, for general look only), `waves/waves-purple-720p.jpg` (the Waves
visualizer, for M4; looks fan-made next to the console photos), `waves/ps3-waves-photo-linkin.jpg` and
`waves/ps3-waves-photo-nujabes.jpg` (photos of a TV playing music on the console: the Waves look
and the track overlay), stills from the owner's console videos (`waves/video-*.jpg`: quiet and
loud Waves, five frames a second through a loud passage, and the 1080p track overlay;
`valley/video-*.jpg` and `other/video-*.jpg` for M5: the blue and orange valley scenes, a warm
abstract scene and a pastel sky), and `xmb/bg_sample.png` (a flat December-red tile). The ribbon and
background follow https://github.com/tsbehlman/xmb, a GPL-3.0 WebGL port of RetroArch's XMB
ribbon, which the owner pointed to.

## Remaining differences

Matched to the 1080p frames: category row position and spacing, icon sizes, label placement,
item focus position, row pitch and gaps above and below the focus, title and subtitle placement,
text sizes, the clock box, and no halo on focused icons. Still different:

- **Background gradient.** Now dark at the top and bright at the bottom like the red frame and
  RetroArch; the purple frame is lighter at the top right, which may be a theme or time-of-day
  difference
- **Sparkle motion, wave reactions and every timing** need video frames at 2 fps or more
- The on-screen keyboard and value pickers have no reference frames yet
- **Waves**: the console's loud particles are a little bigger and brighter than here, and the speeds
  are judged from phone video, so they're approximate

## Remaining differences (M2)

The M2 captures use demo data (made-up titles; the Metrolist icon stands in for album art) so
they need no network. Still to compare against console frames of the music player:

- How the console shows an opened folder: here the category row fades out and only the immediate
  parent icon stays; the console keeps more of the path visible
- Row layout inside a folder (pitch, artwork size, where title and artist sit)
- The options panel's look (the console's is a narrower dark strip with a selection bar)
- The now-playing overlay's placement and contents
