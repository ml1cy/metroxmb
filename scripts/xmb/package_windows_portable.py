#!/usr/bin/env python3
"""Assemble a portable Windows x64 ZIP of the desktop app from a Linux build.

jpackage can't target Windows from Linux, so this takes the Linux `createDistributable` output
(built with `-Dorg.bytedeco.javacpp.platform=windows-x86_64`, so FFmpeg's natives are already
Windows ones), swaps the remaining host-specific jars for Windows ones, adds a Windows JRE, and
writes .cmd launchers that pass the same JVM options the packaged app uses.

Run through scripts/xmb/package_windows_portable.sh, which builds and downloads the inputs.
A dev script, not part of the build. The result contains the PS3 assets: keep it private.
"""
import argparse
import os
import shutil
import tempfile
import zipfile
from pathlib import Path

MAIN_CLASS = "com.metrolist.desktop.MainKt"
# Linux-only or launcher-specific options from the Linux .cfg; the .cmd sets the last two itself.
DROP_PREFIXES = (
    "--add-opens=java.desktop/sun.awt.X11=",
    "-XX:TrimNativeHeapInterval=",
    "-Djava.locale.providers=",
    "-Dwebkit.disable.dmabuf.renderer=",
    "-Dskiko.library.path=",
    "-Dorg.slf4j.simpleLogger.log.org.freedesktop.dbus=",
    "-Dcompose.application.resources.dir=",
    "-XX:ErrorFile=",
)
SQLITE_WINDOWS = "org/sqlite/native/Windows/x86_64/"
SIGNATURES = (".SF", ".RSA", ".DSA")


def read_cfg(dist: Path) -> tuple[list[str], list[str]]:
    cfg = next((dist / "lib/app").glob("*.cfg"))
    classpath, options = [], []
    for line in cfg.read_text().splitlines():
        if line.startswith("app.classpath="):
            classpath.append(line.split("$APPDIR/", 1)[1])
        elif line.startswith("java-options="):
            option = line.split("=", 1)[1]
            if not option.startswith(DROP_PREFIXES) and option not in options:
                options.append(option)
    return classpath, options


def windows_sqlite_jar(source: Path, target: Path) -> None:
    """Keep the driver classes and only the Windows x64 native, like the build does per platform."""
    with zipfile.ZipFile(source) as src, zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as out:
        for item in src.infolist():
            name = item.filename
            if name.startswith("META-INF/") and name.endswith(SIGNATURES):
                continue
            if name.startswith("org/sqlite/native/") and not (name.startswith(SQLITE_WINDOWS) or name.endswith("/")):
                continue
            out.writestr(item, src.read(name))


def crlf(text: str) -> str:
    return text.replace("\r\n", "\n").replace("\n", "\r\n")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dist", type=Path, required=True, help="createDistributable output (…/app/Metrolist)")
    parser.add_argument("--jre-zip", type=Path, required=True, help="Temurin Windows x64 JRE zip")
    parser.add_argument("--skiko-runtime", type=Path, required=True, help="skiko-awt-runtime-windows-x64 jar")
    parser.add_argument("--sqlite-jdbc", type=Path, required=True, help="full sqlite-jdbc jar (all natives)")
    parser.add_argument("--logo", type=Path, required=True, help="desktop logo.png")
    parser.add_argument("--readme", type=Path, required=True, help="README.txt to include")
    parser.add_argument("--out", type=Path, required=True, help="output .zip")
    parser.add_argument("--folder", default="Metrolist-XMB", help="top-level folder inside the zip")
    args = parser.parse_args()

    classpath, options = read_cfg(args.dist)
    app_dir = args.dist / "lib/app"
    with tempfile.TemporaryDirectory() as temporary:
        root = Path(temporary) / args.folder
        (root / "app").mkdir(parents=True)
        (root / "resources/windows").mkdir(parents=True)
        jars = []
        for jar in classpath:
            if jar.startswith("sqlite-jdbc-") and jar.endswith(".jar"):
                name = f"sqlite-jdbc-{args.sqlite_jdbc.stem.removeprefix('sqlite-jdbc-')}-win.jar"
                windows_sqlite_jar(args.sqlite_jdbc, root / "app" / name)
                jars.append(name)
                continue
            shutil.copy2(app_dir / jar, root / "app" / jar)
            jars.append(jar)
            if jar.startswith("skiko-awt-runtime-all-"):
                shutil.copy2(args.skiko_runtime, root / "app" / args.skiko_runtime.name)
                jars.append(args.skiko_runtime.name)
        missing = [jar for jar in jars if not (root / "app" / jar).is_file()]
        if missing:
            raise SystemExit(f"Missing jars: {missing}")

        with zipfile.ZipFile(args.jre_zip) as jre:
            prefix = jre.namelist()[0].split("/", 1)[0] + "/"
            for item in jre.infolist():
                relative = item.filename.removeprefix(prefix)
                if not relative:
                    continue
                target = root / "runtime" / relative
                if item.is_dir():
                    target.mkdir(parents=True, exist_ok=True)
                else:
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_bytes(jre.read(item))
        if not (root / "runtime/bin/javaw.exe").is_file():
            raise SystemExit("The JRE zip has no bin/javaw.exe")

        shutil.copy2(args.logo, root / "resources/windows/logo.png")
        arguments = options + ["-cp", ";".join(f"app\\{jar}" for jar in jars)]
        (root / "app/metrolist.args").write_text(crlf("\n".join(arguments) + "\n"))
        dynamic = (
            '"-Dcompose.application.resources.dir=%~dp0resources" '
            '"-XX:ErrorFile=%USERPROFILE%\\.local\\state\\metrolist\\logs\\hs_err_pid%%p.log"'
        )
        (root / "Metrolist.cmd").write_text(
            crlf(
                "@echo off\n"
                "setlocal\n"
                'cd /d "%~dp0"\n'
                f'start "" "runtime\\bin\\javaw.exe" @app\\metrolist.args {dynamic} {MAIN_CLASS} %*\n',
            ),
        )
        (root / "Metrolist-console.cmd").write_text(
            crlf(
                "@echo off\n"
                "rem Same as Metrolist.cmd, but keeps a console open with the app's log output.\n"
                "setlocal\n"
                'cd /d "%~dp0"\n'
                f'"runtime\\bin\\java.exe" @app\\metrolist.args {dynamic} {MAIN_CLASS} %*\n'
                "pause\n",
            ),
        )
        shutil.copy2(args.readme, root / "README.txt")
        (root / "README.txt").write_text(crlf((root / "README.txt").read_text()))

        args.out.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(args.out, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as out:
            for path in sorted(root.rglob("*")):
                out.write(path, path.relative_to(root.parent).as_posix())
    print(f"{args.out} ({os.path.getsize(args.out) / 1e6:.0f} MB, {len(jars)} jars, {len(options)} JVM options)")


if __name__ == "__main__":
    main()
