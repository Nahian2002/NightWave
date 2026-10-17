# NightWave 2.0 — Phase 5M premium-first launch

**Version:** 2.0.0-alpha5m · **versionCode:** 29  
**Branch:** `feature/nightwave-v2-phase5m-premium-default`  
**Status:** Implementation prepared as a draft PR. No APK has been built or installed here.

## Why the app used to open with the older design

Previous phases improved `NightWaveStudioActivity` but kept the Android `MAIN/LAUNCHER`
intent filter on the original `MainActivity`. The media notification also opened
that original screen. The new design was therefore incorrectly presented as an
Early Access preview even when its implementation had improved.

## What Phase 5M changes

1. The premium `NightWaveStudioActivity` is the **sole** Android launcher
   (`MAIN/LAUNCHER`). The original `MainActivity` remains installed for
   recovery but is **not exported**, so other apps cannot start it directly.
2. The public text-share target (`ACTION_SEND`, `text/plain`) moves to the
   premium Activity. A recognized public watch URL requests playback using
   the existing playback bridge; other bounded shared text is routed to native
   Search. Sharing is not replayed after screen rotation. New share intents
   are supported for an already-running top Activity.
3. The existing Media3 service's session/notification activity now opens the
   **premium** screen instead of classic NightWave. Playback service behavior,
   existing notification permissions, and queue storage are unchanged.
4. The premium header **settings** icon now selects the **You** tab rather
   than unexpectedly opening the old UI. Home, Search, Library and You remain
   premium tabs.
5. **You → Interface fallback → Open classic interface** is a clear,
   deliberate recovery route. The classic Home now shows
   **Tools and interface → Return to NightWave**, removing Early Access and
   Preview branding. It reuses an existing premium Activity where possible
   using `CLEAR_TOP | SINGLE_TOP` and finishes the classic instance.
6. Android Back continues to dismiss Up Next, expanded Now Playing, and a
   selected playlist before returning to Home. On premium Home, system Back
   exits the app instead of jumping to the older UI.
7. The existing app package (`com.nightwave.player`), Android signing identity,
   guest profiles, favorites, playlists, imported offline music files,
   saved queue, offline resume, Room preview and built-in crash diagnostics
   are preserved. There is no uninstall, database reset or migration.

## Validation added

- `tools/verify_v2_phase5m.py`: source-only checks for unique launcher,
  premium text-share entry point, private fallback, notification intent,
  correct settings navigation, shared text handling, Back semantics,
  version/build identity, and unchanged persistence.
- `NightWaveDefaultEntryInstrumentedTest.kt`: two Android instrumentation
  tests query the actual package manager on an Android emulator/device to
  confirm the single launcher, premium shared-text target and private classic
  Activity. **Authored but not executed here.**
- Historical static regression gates were adjusted to alpha5m / 29.

**Do not confuse a passing source gate with successful Android Kotlin
compilation, UI rendering or Android PackageManager behavior.** This authoring
environment has no usable Android SDK/Gradle distribution and cannot run
PowerShell, so a new APK and physical-phone tests are **pending**.

## Windows and phone acceptance

1. Download the complete Phase 5M branch ZIP into a fresh directory. Do not
   overwrite your last known working app or delete any phone data.
2. Optionally run `python tools/verify_v2_phase5m.py` for a source-only gate.
3. Run `build-windows.cmd`. A real successful result must have
   `dist/NightWave-build-status.txt` showing **alpha5m**, code **29**,
   signer digest and APK SHA-256. Failure reports remain in `dist`.
4. Connect **one** authorized device and run `check-device-update.cmd` before
   any in-place upgrade. If signer/version compatibility is inconclusive or
   mismatched, **stop**. Never uninstall to bypass an incompatible signature.
5. On an emulator/disposable profile, run `connectedDebugAndroidTest` to
   execute the new entry-point tests alongside classic/premium launch tests.
6. After a verified in-place update, tap NightWave's home-screen app icon:
   the **premium Home** must be the first screen, with premium Home/Search/
   Library/You navigation and no Early Access label.
7. Tap the top-right settings icon: the **You** profile must open, not the
   classic interface. Locate the bottom **Interface fallback** section,
   choose **Open classic interface**, then **Return to NightWave**; verify
   the original music collection remains accessible throughout.
8. Test Android Back from the expanded player, Up Next queue, selected
   playlist, Search, and You. None should unintentionally switch to classic.
9. Play a song and tap its media notification/lock-screen session:
   the new premium UI should open; audio should continue without restart.
10. Share a recognized YouTube watch URL from another app to NightWave.
    Verify premium Search opens and playback starts on an explicit share.
    Share plain text and verify premium Search handles it without classic
    NightWave appearing.
11. Verify imported offline music, explicit offline Resume, previous saved
    track, playlists, favorites, Bluetooth controls and screen-off playback.
12. If Android still reports frequent crashes, open **You → App health**
    or run `capture-nightwave-crash.cmd` and share only the sanitized
    `dist/NightWave-crash-summary.txt` after reviewing it.

**Do not merge the draft branch into stable `main` until the Windows build
and phone tests pass.** Existing installations do not change until the user
installs an APK built from this branch.
