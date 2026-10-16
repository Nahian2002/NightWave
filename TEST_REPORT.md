# NightWave v0.2 — Verification report

Date: 2026-10-17

## Passed locally

- Kotlin parser smoke runner compiled with `kotlinc` and executed: **PASS**.
- 10 accepted YouTube video links: **PASS**.
- 9 invalid/deceptive link samples rejected: **PASS**.
- 4 duration-formatting cases: **PASS**.
- 10 Android XML documents parse: **PASS**.
- 14 view IDs exist; all 13 referenced view properties are declared: **PASS**.
- Media session/service declaration, required permissions, no-cleartext flag: **PASS**.
- Build configuration pinned to `com.github.TeamNewPipe:NewPipeExtractor:v0.26.5` (upstream case): **PASS static check only**.
- GitHub Actions workflow includes static checks, Android unit tests, debug APK build, and artifact upload: **PASS static check only**.

## Not verified

- **APK has not been built**: Android SDK and Gradle are not installed here and outbound access to install them is unavailable.
- JitPack and Google Maven dependencies have **not** been remotely resolved.
- No live YouTube search or stream playback could be tested in this runtime.
- No Android phone/emulator was connected; lock-screen and Bluetooth behavior unverified.
- The private GitHub repository **exists** at `Nahian2002/NightWave`, and the source plus Windows no-billing local build scripts were uploaded.
- GitHub Actions jobs failed **before startup** with an account-level billing/spending limit restriction; the Android project was not compiled by those runs.
- The Windows build bootstrap is untested on Windows because this environment cannot access Gradle/SDK downloads or execute Windows batch/PowerShell.

The checks above are not a replacement for a successful GitHub Actions run and physical Android validation.

## No-billing local build route

- `build-windows.cmd` invokes `tools/build-windows.ps1`.
- On a 64-bit Windows PC the script checks for JDK 17+, installs build tools from Google/Gradle/Adoptium, accepts SDK licenses interactively, runs `:app:testDebugUnitTest` and `:app:assembleDebug`, and copies a successful artifact to `dist/NightWave-debug.apk`.
- The script has been reviewed statically; **actual successful APK compilation is still pending**. GitHub Actions was changed to manual-only to avoid known-blocked automatic attempts.
