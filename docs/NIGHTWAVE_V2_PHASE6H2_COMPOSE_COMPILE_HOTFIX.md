# NightWave 6H2 — fix real Android Kotlin compiler errors

**Base:** complete Phase 6H1; no modifications to installed NightWave data, media files, playlists, Google Drive, Kotlin package ID or APK signing settings.

## Actual Windows Gradle evidence

The provided 6H1 Windows output reached `:app:compileDebugKotlin` and reported three errors, all in `app/src/main/java/com/nightwave/player/v2/ui/StudioComponents.kt`:

- Line 164 column 26: unresolved `fillMaxSize` in `ListeningSpotlight`'s main Row.
- Line 198 column 49: unresolved `fillMaxSize` in the artwork `AsyncImage` modifier.
- Line 203 column 41: unresolved `fillMaxSize` in the empty-art placeholder `Canvas` modifier.

The file used `Modifier.fillMaxSize()` and `Modifier.fillMaxSize(.78f)` but imported `androidx.compose.foundation.layout.fillMaxWidth` **without** the required Kotlin extension `androidx.compose.foundation.layout.fillMaxSize`.

**Fix:** add the missing Compose foundation layout import in that file. No removal of artwork, changes to card dimensions, changes to playback behavior or UI redesign were needed.

## Prevention

- Extend the Windows no-Python Kotlin preflight `tools/check-kotlin-compat.ps1` to reject Kotlin source files that call the extension without its import.
- Extend the repository-wide Python preflight `tools/verify_v2_phase6f.py` with the same source contract.
- Add `tools/verify_v2_phase6h2.py`, checking the exact three reported sites, the required import, and that the preflight would fail if the import were deleted.
- Stage the gate under the existing build-only GitHub Actions workflow, with a trigger for 6H1-based hotfix PRs. No device install or user-data modification.

## Verification and safe retry

The uploaded 6H1 logs are **real Kotlin compiler evidence of these three errors only**. Fixing them may reveal later compiler, JVM-test or instrumentation-source issues after `compileDebugKotlin` passes. Do not represent the APK as verified until **all** build tasks and signing checks complete successfully.

1. Download and extract the **complete 6H2 branch ZIP** into a *new* directory. Do not overlay an old extraction.
2. Run `build-windows.cmd`. The Windows preflight should pass, then Gradle will reattempt `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`.
3. If it fails again, run `send-build-report.cmd` and share the **reviewed** `dist/NightWave-build-diagnostic.txt`.
4. If it builds, run the included `check-device-update.cmd` for a **read-only package/signature comparison** before attempting any in-place update. Do not uninstall NightWave or clear saved music to work around a signature mismatch.

**Status:** GitHub import fix and source guard authored. An actual 6H2 Windows/Android compilation and install-signing comparison are still pending.
