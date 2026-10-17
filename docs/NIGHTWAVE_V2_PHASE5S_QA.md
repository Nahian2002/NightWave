# NightWave Phase 5S — Android build recovery & crash hardening

**Version:** `2.0.0-alpha5s` · **Build:** 35  
**Branch:** `feature/nightwave-v2-phase5s-build-recovery`  
**Release status:** Draft. Stable `main` and existing installed Android data remain untouched.

## Findings

- The latest GitHub Actions runs for the preceding Phase 5R PR returned **failure**, but GitHub's job details had **no steps or logs**, and job-log downloads returned **404 BlobNotFound**. There is no evidence tying those failures to a particular Kotlin compiler line. They might be runner provisioning/account restrictions, or something else; a build cannot be called successful.
- Historical Windows logs from early alpha builds confirmed `Unresolved reference keySet` in `LibraryBackupCodec`/`LegacyLibrarySnapshot`, and `Unresolved reference Surface` in a previous Compose screen. These specific source mistakes are **not present** in the Phase 5S source: the JSON code uses `.keys()` and the premium Compose screen imports Material3 `Surface`. Those historical logs do not explain a newer build failure.
- The Phase 5R Windows builder compiled app/JVM code but did **not** compile `:app:compileDebugAndroidTestKotlin`, although later phases had added more than ten Android instrumentation source files.
- The Up Next `LazyColumn` had `key = { _, video -> video.url }`. Duplicate restored songs with the same URL can cause Compose's duplicate-key runtime exception. It now keys rows by **unique queue position**, leaving playback and saved queue contents unchanged.

## What Phase 5S implements

1. **A more complete Windows build:** `build-windows.cmd` now requires real Gradle tasks `:app:testDebugUnitTest`, `:app:assembleDebug` and `:app:compileDebugAndroidTestKotlin`, a Gradle `BUILD SUCCESSFUL` result, and the existing APK-signature/version verification before publishing a new candidate APK.
2. **No stale full logs:** At the start of every build, the previous `dist/NightWave-build-full.log` is preserved as `dist/NightWave-build-full-previous.log`. The current full log starts as a fresh pre-Gradle placeholder. If SDK/JDK setup fails, no prior compiler failure is misrepresented as a current one. A previous verified APK is likewise archived, not handed out as new.
3. **One-click redacted diagnosis:** `diagnose-nightwave-build.cmd` reads only the generated `dist` logs/status, extracts up to 65 likely build/compiler/dependency/test failure lines, redacts Windows username paths and printed Basic/Bearer authorization credentials, and writes `dist/NightWave-build-diagnostic.txt`. The tool never connects to Android, installs software, modifies playlists, or alters music files.
4. **Self-test before compilation:** The builder runs `tools/diagnose-build.ps1 -SelfTest` with synthetic compiler errors and a fabricated secret. The Windows preflight PowerShell parser also parses the new diagnostic script. A failed test blocks candidate publication.
5. **Scoped CI:** The Android workflow follows the Phase 5R branch as its PR base and runs both Phase 5R and Phase 5S source checks; its APK remains explicitly **UNVERIFIED-FOR-DEVICE** until the installed app signature is checked.
6. **No library migration:** No changes to offline audio, guest profile, playlist storage, queue serialization, media playback actions, app package or signing configuration.

## What we know and what we don't

- Source wiring was verified by reading the GitHub branch; `tools/verify_v2_phase5s.py` adds a deterministic source gate for the diagnostic path and duplicate-key regression.
- **The new PowerShell scripts have not been run on Windows** here. The available execution environment has Kotlin/JDK, but no Android SDK, Gradle, Windows PowerShell, connected phone or emulator.
- No real Android compilation, signed APK, instrumentation execution, upgrade-signature check or on-device startup/performance QA is established by source checks.
- GitHub-hosted workflow failures with no recorded job steps might require GitHub Actions/repository account troubleshooting. A code patch cannot resolve account-level runner limitations.

## Windows acceptance — next actionable step

1. Download the Phase 5S complete source ZIP into a new folder. **Do not uninstall NightWave or clear Android app data.**
2. Run `build-windows.cmd`. Allow official first-time SDK/JDK/Gradle downloads and Android SDK license acceptance. Compilation now covers instrumentation sources as well.
3. On **failure**, double-click `diagnose-nightwave-build.cmd`. Share `dist/NightWave-build-diagnostic.txt` plus `dist/NightWave-build-errors.txt` if needed. The shorter summary contains the current stage and leading failures; avoid uploading historical full logs when the current build stopped before Gradle.
4. On **success**, check `dist/NightWave-build-status.txt`: **SUCCESS**, package `com.nightwave.player`, version `2.0.0-alpha5s`, code **35**, APK SHA-256 and signer identity.
5. Before installing over the current app, connect exactly one authorized phone and run `check-device-update.cmd`. A signing mismatch, older version, missing APK or inconclusive result means **do not install**.
6. Run Android `connectedDebugAndroidTest` on an emulator/disposable profile, then verify the real phone's premium launch, page swipes, Up Next/Undo with duplicate queue songs, saved/offline music, media notification, Search fallback, profile at 1.5× font and crash diagnostics.
7. Preserve this draft branch until real Android compile and device QA succeed. Stable `main` remains unchanged.

[Previous failed GitHub Actions run](https://github.com/Nahian2002/NightWave/actions/runs/39034842269) — failure cause **not established** by available logs.
