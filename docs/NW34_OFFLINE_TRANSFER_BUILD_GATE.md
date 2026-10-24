# NightWave NW34 — Offline Transfer Storage Safety and Build Preflight

**Release status: SOURCE CHANGE / Android APK and device acceptance NOT VERIFIED.**

NW34 deliberately avoids adding another user-facing feature before NW30-NW33
has passed a full local Windows Android build and real device testing.

## Problems addressed

1. **Unknown-size audio transfers:** NW31's storage policy checked free space
   before transfer, but a server without Content-Length could exhaust the
   device during a large download before a second private-library copy.
2. **New offline source regressions not covered by Windows preflight:** the
   existing NW27/NW07 checks predated NW30-NW33 additions. Removing a critical
   offline verification call could previously pass the older fast checks.

## Changes in NW34

- `OfflineSaveStoragePolicy.canContinue(free, staged)` rejects invalid staged
  sizes and requires free storage >= staged bytes + 24 MiB. This reserves enough
  space for a complete second, private-library copy of downloaded audio.
- `PermittedAudioDownloader.transferOnce` checks Android `StatFs` after
  each ~4 MiB of **searched-audio** transfer, whenever known-size delivery
  finishes, and at end-of-stream (even if Content-Length is unknown). If
  free-space measurement fails it rejects rather than guessing.
- The existing staging `.pending` file is deleted on any failure in the
  downloader's `finally` block. Other offline music, old library IDs, import
  metadata and user files are not touched.
- The user-facing failure category now accurately reports storage exhausted
  *during* transfer instead of saying the track is unavailable.
- The Windows builder now executes `tools/check-nw34-offline-integrity.ps1`
  self-test and source preflight **before Gradle**. It checks 16 required
  offline source/test/manifest contracts, and fails the build if missing.
- `tools/check-builder-syntax.ps1` now includes the new ASCII-only
  PowerShell 5.1 script in syntax/encoding validation.
- Added JVM unit tests for periodic unknown-length storage validation and
  private, nonsensitive error reporting.

## Retained functionality

NW28 Google Drive sign-in diagnostics, NW29 offline discovery, NW30 searched
audio saving pilot, NW31 verified offline-first playback, NW32 actual-file
readiness checks and NW33 same-track retry history remain in the branch.
No Android application ID, versionCode, signing policy, Media3 service,
persistent library format or existing song ID has been changed.

NW34 does **not** increase the number of YouTube songs savable offline and
does **not** bypass DRM, access controls or provider permissions.
YouTube's third-party developer rules restrict offline content storage
without prior written approval; this source pilot is not cleared for public
distribution just because a user clicks Save.

## Verification performed

- An isolated local Kotlin/JVM harness matching the pure NW34 storage and
  failure policies compiled and passed **15/15** assertions. This does not
  prove the Android app compiles.
- NW34's GitHub committed source files were read back and new marker/call
  sites compared. PowerShell syntax checks are authored to run on Windows.
- Windows PowerShell and Android SDK are NOT available here; the full
  Android Gradle build, Kotlin unit-test task, Android instrumentation
  compilation, APK signature compatibility and real Samsung playback
  **have not been tested in this source session**.
- GitHub Actions was NOT triggered.

## Windows acceptance (do not uninstall existing NightWave)

1. Download the full NW34 ZIP at the immutable commit, extract into a fresh
   Windows directory.
2. Run `build-windows.cmd`. It should perform the new NW34 offline gate
   before the source fingerprint and full Gradle tasks.
3. Check `dist/NightWave-build-status.txt`, errors and full build log.
   A fast preflight PASS is *not* an Android compile PASS.
4. Compare signing identity with `check-device-update.cmd` prior to
   installing over the old NightWave app. Do not clear user data.
5. Save a recording from an authorized source. Test a deliberate transfer
   interruption, a missing Content-Length response, a nearly-full test
   device, and automatic deletion of incomplete staged files.
6. Verify offline playback with both Wi-Fi and mobile data turned off.
   Delete a disposable downloaded test track and confirm the old job no
   longer reports Ready Offline.
7. Verify search, online playback, lyrics, playlists, storage, Google
   authorization, direct-link downloads and existing imports still behave.
