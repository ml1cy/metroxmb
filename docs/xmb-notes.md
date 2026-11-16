# XMB tuning notes

Constants tuned by eye and the reason for each (PLAN.md, "Working method"). Update an entry
when a value changes, and say what it was compared against. Until the owner adds console frames
under `reference/`, every value here is a first pass from memory of the console, checked only
against the captures in `compare/`.

## Layout (`xmb/theme/XmbTheme.kt`, `XmbMetrics`)

One layout unit is 1/720 of the window height, or 1/960 of the width when the window is
narrower than 4:3, so the XMB keeps its proportions at any size.

| Constant | Value | Reason |
|---|---|---|
| `ANCHOR_X` | 0.22 of width | The focused category sits left of center like the console, leaving room for two categories on its left (the leftmost is partly cut off at the edge, as on the console) |
| `BAR_Y` | 0.30 of height | Category row in the upper third, with room above for items that scroll up past it |
| `categorySpacing` | 136 units | Wide gaps between category icons; eight categories span the screen from the anchor |
| `categoryIcon` | 72 units | Focused category icon size; unfocused ones shrink to 0.78 |
| `categoryLabelOffset` | 54 units | Label sits just under the focused category's icon |
| `itemIcon` | 56 units | Focused item icon; unfocused shrink to 0.72 |
| `itemSpacing` | 66 units | Item pitch, a little more than the icon so glows don't touch |
| `focusedItemOffset` | 112 units | Focused item hangs under the category label |
| `itemsAboveGap` | 110 units | Items above the focused one jump over the category row and label, so the first one sits just above the row |
| `itemTextOffset` | 20 units | Gap between item icon and title |
| Text sizes | label 18, title 19, subtitle 14, clock 16 units | Rodin at these sizes matches the console's proportions at 720p |
| Visible items | 6 above, 10 below | Enough to fill a 16:9 window; the rest aren't composed |

## Motion (`XmbMotion`)

| Constant | Value | Reason |
|---|---|---|
| `CATEGORY_MS` | 240 ms | Horizontal scroll; quick but visible, so repeated presses chain smoothly |
| `ITEM_MS` | 180 ms | Vertical scroll is a little faster than horizontal, as on the console |
| `scrollEasing` | cubic-bezier(0.25, 0.1, 0.25, 1) | Decelerating ease; the motion settles rather than stopping hard |
| Unfocused category | scale 0.78, alpha 0.55 | Focused category clearly larger and brighter |
| Unfocused item | scale 0.72, alpha 0.62 | Same idea for items |
| Category label fade | alpha = 1 − 2 × distance | Label is gone halfway to the next category, so two labels never overlap |
| Glow pulse | 2.4 s period, strength 0.55 to 0.9 | Slow breathing glow on the focused icon; a radial white halo at 32% peak alpha. Works on every platform (no blur effect needed) |
| Title glow | white shadow, 60% alpha, 8 units blur | Focused item and category label only |

## Background (`xmb/theme/XmbPalette.kt`, `visualizer/background/XmbBackground.kt`)

| Constant | Value | Reason |
|---|---|---|
| Monthly colors | Jan `#CBCBCB` silver, Feb `#D8BF1A` gold, Mar `#6DB217` lime, Apr `#E17E9A` pink, May `#088B01` green, Jun `#9A61C8` purple, Jul `#02CDC7` cyan, Aug `#0C76C0` blue, Sep `#B444C0` magenta, Oct `#E5A708` amber, Nov `#875B1E` brown, Dec `#E3412A` red | The console's documented monthly theme colors |
| Brightness | 1.0 from 08:00 to 17:00, 0.42 from 22:00 to 05:00, smoothstep ramps between | The console dims its background in the evening; checked continuous to under 1% per minute |
| Gradient top | base lightened 12% toward white, × 0.92 × brightness | Lighter top like the console |
| Gradient bottom | base × 0.6 × brightness | Started at 0.38, softened after comparing with `reference/xmb/bg_sample.png` (a December-red tile only ~10% darker at the bottom). Needs real console frames to settle |
| Highlight | white radial at (0.2 W, 0.08 H), radius 0.75 × the longer side, 10% + 8% × brightness alpha | Soft sheen in the upper left |
| Wave | 3 translucent ribbons around 60–66% of the height, 0.05–0.11 cycles/s, 16% peak alpha, 1.2-unit crest line | Simple stand-in until M4's Waves shader |
| Animation | One frame clock in `XmbApp`, paused while `LocalVisualWorkActive` is false | Hidden windows do no animation work (docs/rendering.md); frames redraw without recomposing |

## Drill-down levels (`xmb/bar/XmbLevelView.kt`, M2)

| Constant | Value | Reason |
|---|---|---|
| `LEVEL_FOCUS_Y` | 0.42 of height | With the category row faded out, an opened list centers a little above the middle, leaving room below for now-playing |
| `parentOffset` | 130 units left of the list | The parent row's icon sits to the left of the list like the console's folder navigation; only the immediate parent shows |
| Rows shown | 7 above, 10 below the focus | Fills a 16:9 window; the rest aren't composed |
| Level transition | category row fades out over `FADE_MS`; rows slide in from 40 units right | Opening reads as moving into the folder |
| Artwork | album art or photo cropped square, 6% corner radius; the XMB icon shows until it loads or if it fails | Album art replaces item icons, as the manifest notes |
| History list | latest 200 plays, one row per song | Enough to scroll; paging can come later |

## Options panel (`xmb/screens/XmbOptionsPanelView.kt`, M2)

| Constant | Value | Reason |
|---|---|---|
| Width | 360 units, right edge | A side panel that leaves the list visible |
| Background | horizontal black gradient, 35% to 65% alpha | Darkens toward the edge so the options read over any month's color |
| Option spacing | 22 units | Roomy list like the console's option menu |
| Slide | `FADE_MS` with the scroll easing | Matches the level transition |

## Now playing (`xmb/screens/XmbNowPlaying.kt`, M2)

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
