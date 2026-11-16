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
| `live-desktop-xmb.png` | The real desktop app (Linux, virtual display, 1080×720 window) right after onboarding: the XMB replaces the stock screens |
| `live-desktop-interface-setting.png` | The stock Settings → Appearance page with the new Interface style choice (before it moved to the top of the page) |

## Remaining differences (M1)

Only one reference exists so far: `reference/xmb/bg_sample.png`, a flat December-red tile. Its
top color (190, 80, 69) is close to the December gradient's top here; its bottom is only ~10%
darker, so the gradient's bottom factor was softened from 0.38 to 0.6. Everything else needs
console frames in `reference/xmb/` to compare against:

- Category spacing, icon sizes and the row's vertical position
- Item pitch, focus scale, and how far items above the focus sit from the row
- Glow shape: here a radial halo; the console's follows the glyph outline
- Text sizes, the focused title's glow, and subtitle placement
- Monthly colors under the console's own gradient and brightness curve
- The wave: a simple three-ribbon stand-in until M4's Waves shader
- Clock format and placement (the console adds a line under the clock)
- Motion timings (240 ms across, 180 ms down) need video frames at 2 fps or more

## Remaining differences (M2)

The M2 captures use demo data (made-up titles; the Metrolist icon stands in for album art) so
they need no network. Still to compare against console frames of the music player:

- How the console shows an opened folder: here the category row fades out and only the immediate
  parent icon stays; the console keeps more of the path visible
- Row layout inside a folder (pitch, artwork size, where title and artist sit)
- The options panel's look (the console's is a narrower dark strip with a selection bar)
- The now-playing overlay's placement and contents
