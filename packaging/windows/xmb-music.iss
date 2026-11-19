; XMB Music Windows installer (Inno Setup 6).
; Built by .github/workflows/build.yml on a Windows runner:
;   ISCC.exe /DAppVersion=14.0.0 /DSourceDir=<app folder> /DOutputDir=<dist> /DIconFile=<logo.ico> xmb-music.iss
; SourceDir is the jpackage app folder (Metrolist.exe, app\, runtime\) that
; :desktopApp:assembleWindowsRelease also zips as the portable build.
; Per-user install: no administrator rights, like the portable ZIP.

#ifndef AppVersion
  #define AppVersion "0.0.0"
#endif
#ifndef SourceDir
  #error SourceDir must point at the Windows app folder
#endif
#ifndef OutputDir
  #define OutputDir "."
#endif

#define AppName "XMB Music"
#define AppExe "Metrolist.exe"

[Setup]
; Keep this AppId: upgrades and the uninstaller find earlier installs by it.
AppId={{6C1D3F52-8E4B-4C57-9A0E-5B2F7D1C9A41}
AppName={#AppName}
AppVersion={#AppVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher=XMB Music
DefaultDirName={localappdata}\Programs\XMB Music
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=10.0
OutputDir={#OutputDir}
OutputBaseFilename=XMB-Music-Setup-x64-{#AppVersion}
Compression=lzma2/ultra64
SolidCompression=yes
LZMAUseSeparateProcess=yes
WizardStyle=modern
UninstallDisplayName={#AppName}
UninstallDisplayIcon={app}\{#AppExe}
CloseApplications=yes
RestartApplications=no
#ifdef IconFile
SetupIconFile={#IconFile}
#endif

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; The whole app folder; the portable ZIP's install manifest isn't part of it.
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#AppName}"; Filename: "{app}\{#AppExe}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Registry]
; The app registers metrolist:// (links) and metrolist-auth:// (Discord sign-in) for itself on
; launch, per user; remove them on uninstall.
Root: HKCU; Subkey: "Software\Classes\metrolist"; Flags: uninsdeletekey dontcreatekey
Root: HKCU; Subkey: "Software\Classes\metrolist-auth"; Flags: uninsdeletekey dontcreatekey

[Run]
Filename: "{app}\{#AppExe}"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
; Files the app writes next to itself (caches, CDS archive); user data in the profile stays.
Type: filesandordirs; Name: "{app}"
