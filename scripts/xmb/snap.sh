#!/usr/bin/env bash
# Renders XMB screens headlessly into compare/ for side-by-side checks against reference frames
# in reference/ (PLAN.md, "Working method"). A dev script, not a test.
#
#   scripts/xmb/snap.sh            # every screen
#   scripts/xmb/snap.sh shell      # one screen: icons, shell, shell-night, music-scrolled,
#                                  # settings, visualizer-august, small-window, music-home,
#                                  # music-section, settings-december, settings-page,
#                                  # settings-picker, search-keyboard, users, visualizer-waves,
#                                  # waves-loud, lyrics, cast-devices, video-column, video-full,
#                                  # podcasts, music-options
set -euo pipefail
cd "$(dirname "$0")/../.."
export XMB_SNAP_OUT="$PWD/compare"
export XMB_SNAP="${1:-all}"
# testServerWithoutReflection is a dependency of desktopTest and isn't needed for captures.
./gradlew :shared:desktopTest --tests 'com.metrolist.shared.xmb.XmbSnapshotCapture' \
    -x :shared:testServerWithoutReflection --rerun --console=plain -q
ls -1 compare/*.png
