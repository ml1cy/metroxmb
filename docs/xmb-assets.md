# XMB assets

The source of truth for the PS3 assets this private project ships (see CLAUDE.md: keep the repo
private). Paths are relative to `shared/src/commonMain/composeResources/`. Compose generates an
accessor for each file (`Res.drawable.xmb_cat_music`, `Res.font.xmb_rodin_latin_regular`) and
`files/` entries load with `Res.readBytes("files/xmb/sounds/nav_up.ogg")`.

Every slot below is required. `XmbAssetManifestTest` (shared desktop tests) fails when a listed
file is missing, isn't the listed format or size, or when an `xmb_` asset exists that this file
doesn't list. To swap an asset, replace the file with the same name and update its size here.

## Font

| Slot | File | Notes |
|---|---|---|
| UI font | `font/xmb_rodin_latin_regular.ttf` | SCE-PS3 Rodin LATIN Regular. Latin only, regular weight only. Non-Latin text falls back to the platform's fonts; bold is synthesized |

## Category icons (white on transparent)

| Category | File | Size | Source icon |
|---|---|---|---|
| Users | `drawable/xmb_cat_users.png` | 68×68 | menu_user. Small: looks soft when enlarged on focus; a 128 px or larger source would help |
| Settings | `drawable/xmb_cat_settings.png` | 128×128 | settings (toolbox) |
| Music | `drawable/xmb_cat_music.png` | 128×128 | music |
| Search | `drawable/xmb_cat_search.png` | 128×128 | image (network + magnifier) |
| Video | `drawable/xmb_cat_video.png` | 128×128 | movie |
| Friends | `drawable/xmb_cat_friends.png` | 128×128 | netplay (speech bubble) |

Podcasts and Visualizer use original vector icons drawn for this project, defined in code as
`XmbIcons.Podcasts` (microphone) and `XmbIcons.Visualizer` (waveform bars) in
`shared/.../xmb/theme/XmbIcons.kt`, on the same 128-unit canvas as the PNGs. The temporary
PNGs they replace were copies of `reference/icons-extra/menu_stream.png` and `menu_audio.png`.

## Item icons

| Use | File | Size | Source icon |
|---|---|---|---|
| Song row | `drawable/xmb_item_song.png` | 128×128 | musics |
| Playlist | `drawable/xmb_item_playlist.png` | 256×256 | folder |
| Album | `drawable/xmb_item_album.png` | 256×256 | disc (album art replaces this when loaded) |
| Artist | `drawable/xmb_item_artist.png` | 68×68 | menu_user (artist photo replaces this when loaded). Small, like Users |
| Liked songs | `drawable/xmb_item_liked.png` | 256×256 | favorites |
| History | `drawable/xmb_item_history.png` | 256×256 | history |
| Like / unlike | `drawable/xmb_item_like.png` | 256×256 | add-favorite |
| Add to playlist | `drawable/xmb_item_add_to_playlist.png` | 256×256 | menu_add |
| New playlist | `drawable/xmb_item_new_playlist.png` | 256×256 | add (dashed tile) |
| Info | `drawable/xmb_item_info.png` | 128×128 | menu_info |
| Remove | `drawable/xmb_item_remove.png` | 256×256 | menu_exit |
| Help | `drawable/xmb_item_help.png` | 128×128 | menu_help |
| Listen Together room | `drawable/xmb_item_listen_together.png` | 128×128 | menu_room |
| Music video | `drawable/xmb_item_music_video.png` | 128×128 | movies |
| Network settings | `drawable/xmb_item_network_settings.png` | 128×128 | menu_network |
| Other sub-settings | `drawable/xmb_item_subsetting.png` | 128×128 | subsetting |
| MetroEQ | `drawable/xmb_item_equalizer.png` | 256×256 | menu_mixer |
| Volume / audio | `drawable/xmb_item_volume.png` | 256×256 | menu_volume_max |

## UI

| Use | File | Size |
|---|---|---|
| Back / drill indicator | `drawable/xmb_ui_arrow.png` | 256×256 |
| Cursor | `drawable/xmb_ui_cursor.png` | 128×128 |
| Toggle on | `drawable/xmb_ui_toggle_on.png` | 256×256 |
| Toggle off | `drawable/xmb_ui_toggle_off.png` | 256×256 |
| Check | `drawable/xmb_ui_check.png` | 256×256 |
| Clock | `drawable/xmb_ui_clock.png` | 256×256 |
| Wi-Fi | `drawable/xmb_ui_wifi.png` | 128×128 |
| Bluetooth | `drawable/xmb_ui_bluetooth.png` | 256×256 |
| Battery 20% | `drawable/xmb_ui_battery_20.png` | 256×256 |
| Battery 40% | `drawable/xmb_ui_battery_40.png` | 256×256 |
| Battery 60% | `drawable/xmb_ui_battery_60.png` | 256×256 |
| Battery 80% | `drawable/xmb_ui_battery_80.png` | 256×256 |
| Battery full | `drawable/xmb_ui_battery_full.png` | 256×256 |
| Battery charging | `drawable/xmb_ui_battery_charging.png` | 256×256 |
| Dialog background (9-slice) | `drawable/xmb_ui_dialog_slice.png` | 256×256 |
| On-screen key | `drawable/xmb_ui_key.png` | 256×256 |
| On-screen key, focused | `drawable/xmb_ui_key_hover.png` | 256×256 |

## Sounds (Ogg Vorbis, 48 kHz)

| Slot | File | Length |
|---|---|---|
| Move up | `files/xmb/sounds/nav_up.ogg` | 0.05 s |
| Move down (also left/right) | `files/xmb/sounds/nav_down.ogg` | 0.05 s |
| Select (✕) | `files/xmb/sounds/select.ogg` | 0.31 s |
| Back (○), also the error sound | `files/xmb/sounds/back.ogg` | 0.05 s |
| Notification in | `files/xmb/sounds/notice.ogg` | 0.82 s |
| Notification out | `files/xmb/sounds/notice_close.ogg` | 0.31 s |
| Boot / startup | `files/xmb/sounds/boot.ogg` | 10 s |
| Unlock (achievement-style) | `files/xmb/sounds/unlock.ogg` | 1.0 s |
| Menu background music | `files/xmb/sounds/menu_bgm.ogg` | 2:14. Optional; off by default, and muted while music plays |

Desktop decodes these with the bundled FFmpeg and Android's `SoundPool` plays Vorbis. iOS
can't decode Vorbis natively, so iOS needs converted copies (M8).

## Not shipped

- `reference/xmb/bg_sample.png`: static sample of the console background, for comparison only.
  The real background is the procedural monthly gradient and wave.
- `reference/icons-extra/`: every original icon from the pack, unrenamed, for picking
  alternates. Not in Compose resources (several names aren't valid resource names, and unused
  icons would add size). Copy one into `drawable/` with an `xmb_` name and list it above to use it.

## Gaps

- Left/right nav sound: reuses `nav_down.ogg` until the owner adds one.
- Error/buzzer sound: reuses `back.ogg` until the owner adds one.
- Bold font weight.
