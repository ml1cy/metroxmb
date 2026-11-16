XMB Music - portable Windows x64 build
======================================

PRIVATE BUILD. It contains Sony PS3 assets (font, sounds) for personal use only.
Don't share it.

Run
---
1. Unzip anywhere (for example C:\Apps\Metrolist-XMB). Keep the folder together.
2. Double-click Metrolist.cmd.
   If nothing appears, run Metrolist-console.cmd to see the log.

Windows may warn that the script comes from the internet: choose "More info" > "Run anyway".
The app bundles its own Java runtime; nothing else needs installing. YouTube's
background token check uses the Microsoft Edge WebView2 runtime, which Windows 10 and 11
normally have.

Where things are stored
-----------------------
Settings: %USERPROFILE%\.config\metrolist
Crash logs: %USERPROFILE%\.local\state\metrolist\logs
Deleting the folder removes the app; your settings stay in your user profile.

Using the XMB
-------------
The app opens in the stock Metrolist screens until first-run setup is done, then switches to
the XMB.
  Arrow keys        move
  Enter             select (cross)
  Esc / Backspace   back (circle)
  Tab               options (triangle)
  Mouse wheel       move up/down
Settings > "Metrolist interface" switches to the standard screens. Switch back from
Settings > Appearance > Interface style.
In this milestone the XMB is the menu shell only: its items don't open anything yet. Music
playback, search and the rest are in the standard screens.

Known limits of this build
--------------------------
- Built on Linux, not on Windows, and not yet run on a Windows machine. Under Wine the
  bundled Java runtime, launcher and classpath all start, but Wine stops the JDK's own
  network-interface lookup, so the window was never seen there.
- No YouTube sign-in: the Windows login helper (metro-wv2-helper, a .NET program) isn't
  included. The standard screens work signed out.
- Launches through a .cmd file instead of an .exe, so the taskbar shows the Java icon.
