#!/usr/bin/env bash
# Builds a portable Windows x64 ZIP of the desktop app on Linux: jpackage can't target Windows from
# here, so this builds the Linux distributable with Windows FFmpeg natives, downloads the Windows
# JRE and Skiko runtime (checksum-verified) and hands everything to package_windows_portable.py.
# A dev script. The ZIP contains the PS3 assets: keep it private.
#
#   scripts/xmb/package_windows_portable.sh [output.zip]
set -euo pipefail
cd "$(dirname "$0")/../.."
out="${1:-dist/Metrolist-XMB-windows-x64.zip}"
jre_tag="jdk-25.0.5+8"
jre_file="OpenJDK25U-jre_x64_windows_hotspot_25.0.5_8.zip"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

./gradlew -Dorg.bytedeco.javacpp.platform=windows-x86_64 -Pmetrolist.debug=false \
    :desktopApp:createDistributable --console=plain -q
dist=desktopApp/build/compose/binaries/main/app/Metrolist

skiko_version="$(ls "$dist/lib/app" | sed -n 's/^skiko-awt-\([0-9.]*\)-[0-9a-f]*\.jar$/\1/p')"
# Downloads are kept in $XMB_WINDOWS_CACHE once their checksums pass, so later builds don't fetch
# them again (the hosts rate-limit repeated downloads).
cache="${XMB_WINDOWS_CACHE:-$HOME/.cache/xmb-windows}"
mkdir -p "$cache"
skiko_jar="$cache/skiko-awt-runtime-windows-x64-$skiko_version.jar"
skiko_url="https://repo1.maven.org/maven2/org/jetbrains/skiko/skiko-awt-runtime-windows-x64/$skiko_version/$(basename "$skiko_jar")"
if [ ! -f "$skiko_jar" ]; then
    curl -fsSL -o "$work/skiko.jar" "$skiko_url"
    echo "$(curl -fsSL "$skiko_url.sha1")  $work/skiko.jar" | sha1sum --check --status
    mv "$work/skiko.jar" "$skiko_jar"
fi

jre_url="https://github.com/adoptium/temurin25-binaries/releases/download/${jre_tag/+/%2B}/$jre_file"
if [ ! -f "$cache/$jre_file" ]; then
    curl -fsSL -o "$work/$jre_file" "$jre_url"
    echo "$(curl -fsSL "$jre_url.sha256.txt" | cut -d' ' -f1)  $work/$jre_file" | sha256sum --check --status
    mv "$work/$jre_file" "$cache/$jre_file"
fi

sqlite_version="$(sed -n 's/^sqliteJdbc = "\(.*\)"/\1/p' gradle/libs.versions.toml)"
sqlite_jar="$(find "${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2" -name "sqlite-jdbc-$sqlite_version.jar" | head -n 1)"
[ -n "$sqlite_jar" ] || { echo "sqlite-jdbc $sqlite_version isn't in the Gradle cache" >&2; exit 1; }

python3 scripts/xmb/package_windows_portable.py \
    --dist "$dist" --jre-zip "$cache/$jre_file" --skiko-runtime "$skiko_jar" --sqlite-jdbc "$sqlite_jar" \
    --logo desktopApp/src/desktopMain/resources/logo.png \
    --readme scripts/xmb/windows-portable-README.txt --out "$out"
