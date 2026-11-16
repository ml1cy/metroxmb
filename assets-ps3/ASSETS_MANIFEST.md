# assets-ps3 manifest
Committed to this private fork for personal use; keep the repo private (see CLAUDE.md).
Every slot below is required: a missing file is a test failure, not a placeholder fallback.
Swap any file by replacing it with the same name. PLAN.md M1 moves these into Compose
resources and updates this list with the new paths.

## Font
| Slot | File | Notes |
|---|---|---|
| UI font | `fonts/SCE-PS3-Rodin-LATIN-Regular.ttf` | Latin only, regular weight. Need a fallback font for CJK/Arabic titles and a bold weight (synthesize or add a file) |

## Category icons (`icons/category/`, 128×128 white PNG)
| Category | File | Source icon |
|---|---|---|
| Users | `users.png` | menu_user |
| Settings | `settings.png` | settings (toolbox) |
| Music | `music.png` | music |
| Search | `search.png` | image (network + magnifier) |
| Podcasts | `podcasts.png` | menu_stream, temporary until M1 replaces it with an original microphone icon |
| Video | `video.png` | movie |
| Friends | `friends.png` | netplay (speech bubble) |
| Visualizer | `visualizer.png` | menu_audio, temporary until M1 replaces it with an original waveform icon |

## Item icons (`icons/items/`)
| Use | File | Source icon |
|---|---|---|
| Song row | `song.png` | musics |
| Playlist | `playlist.png` | folder |
| Album | `album.png` | disc (album art replaces this when loaded) |
| Artist | `artist.png` | menu_user (artist photo replaces this when loaded) |
| Liked songs | `liked.png` | favorites |
| History | `history.png` | history |
| Like / unlike | `like.png` | add-favorite |
| Add to playlist | `add_to_playlist.png` | menu_add |
| New playlist | `new_playlist.png` | add (dashed tile) |
| Info | `info.png` | menu_info |
| Remove | `remove.png` | menu_exit |
| Help | `help.png` | menu_help |
| Listen Together room | `listen_together.png` | menu_room |
| Music video | `music_video.png` | movies |
| Network settings | `network_settings.png` | menu_network |
| Other sub-settings | `subsetting.png` | subsetting |
| MetroEQ | `equalizer.png` | menu_mixer |
| Volume / audio | `volume.png` | menu_volume_max |

## UI (`icons/ui/`)
arrow (back/drill indicator), cursor, toggle_on/off, check, clock, wifi, bluetooth,
battery_20/40/60/80/full/charging, dialog_slice (9-slice dialog background),
key/key_hover (on-screen keyboard).

## Sounds (`sounds/`, OGG)
| Slot | File | Length |
|---|---|---|
| Move up | `nav_up.ogg` | 0.05 s |
| Move down (also left/right) | `nav_down.ogg` | 0.05 s |
| Select (✕) | `select.ogg` | 0.31 s |
| Back (○) | `back.ogg` | 0.05 s |
| Notification in | `notice.ogg` | 0.82 s |
| Notification out | `notice_close.ogg` | 0.31 s |
| Boot / startup | `boot.ogg` | 10 s |
| Unlock (achievement-style) | `unlock.ogg` | 1.0 s |
| Menu background music | `menu_bgm.ogg` | 2:14. Optional; off by default, and muted while music plays |

## Background
`background/bg_sample.png` is a static sample only. The real background is the
procedural monthly gradient + Waves shader.

## Extras
`icons/extra/` holds every original icon from the pack, unrenamed, for picking alternates.

## Gaps (not in this pack)
- Left/right nav sound (reuses `nav_down.ogg` until the owner adds one)
- Error/buzzer sound (reuses `back.ogg` until the owner adds one)
- Dedicated podcast and visualizer category icons (Claude Code makes these in M1)
- Bold font weight
