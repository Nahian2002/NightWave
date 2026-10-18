# NightWave 6V4D — Integrated reliability gate

**Baseline:** 6V4A (identity-safe public queue restore) + 6V4B (non-destructive
offline audio health audit) + 6V4C (bounded playback recovery and Media3
controller reconnect). This branch is an **integration/build-gate upgrade**,
not a new streaming or Google Drive feature.

## How to build (Windows)
1. Download the **full source archive** for this branch or pinned commit
   to a fresh folder; keep the previous known-good NightWave APK installed.
2. Extract the archive. Do not mix source files from multiple phase ZIPs.
3. In the extracted project folder, run `build-windows.cmd`.
4. Its existing preflight runs first, then
   `tools/check-phase6v4-integration.ps1` (Windows PowerShell 5.1,
   no Python, Java, Android SDK or internet required for this gate).
5. If that passes, the unchanged real Gradle pipeline must complete:
   `testDebugUnitTest assembleDebug compileDebugAndroidTestKotlin`.
6. An APK is copied into `dist/NightWave-debug.apk` only after
   `BUILD SUCCESSFUL`, APK identity and signature verification.
7. Run `check-device-update.cmd` before an in-place phone update.
   **Never uninstall or clear app data** to work around an update mismatch.

If the build fails, **do not install** an older or partly generated APK.
Run `send-build-report.cmd` and share
`dist/NightWave-build-diagnostic.txt`. The script preserves any previous
verified package at `dist/NightWave-debug-previous.apk`.

## CI
The GitHub workflow also invokes
`python3 tools/verify_v2_phase6v4d.py --self-test`
and `python3 tools/verify_v2_phase6v4d.py`
before its existing Android Gradle tasks. The self-test builds a synthetic
minimal source tree and verifies that missing runtime protections or
regression tests cause the checker to fail. PR target filters now include
6V4C and 6V4D. CI is not automatically claimed to have run by committing
these files; **no CI result is implied without an actual workflow run**.

## Protecting users and data
- No changes to existing playlist schemas, OAuth scopes, user accounts,
  imported file bytes, or cloud backup/restore protocol in this phase.
- The saved public queue is restored by exact video identity. No transient
  signed streaming URL is persisted.
- Offline file health reports never silently rewrite a corrupt index or
  delete a missing/mismatched song. An absent song remains visible for
  user-directed recovery.
- Automatic streaming retry is bounded by the same selected song, expected
  generation and play intent; pause, offline switching and sleep timer stop it.
- Rebinding a disconnected controller does **not** auto-resume audio.
- Do not run Android instrumentation tests against a personal phone; their
  test fixtures deliberately mutate app-private test preferences. Use an
  isolated emulator/disposable installation instead.

## Release acceptance — separate gates
| Stage | Requirement | Status before a real build |
| --- | --- | --- |
| Source registration | 6V4A/B/C hooks and regression files wired | Committed; gate added |
| Gate self-test | Detect removed hook and missing tests | Added; runtime result pending |
| Windows PowerShell gate | Exit 1 on missing required source | Added; Windows result pending |
| Kotlin & JVM tests | Gradle testDebugUnitTest successful | NOT VERIFIED |
| APK compile | Gradle assembleDebug successful | NOT VERIFIED |
| Instrumentation source compile | compileDebugAndroidTestKotlin successful | NOT VERIFIED |
| APK signer & update compatibility | identity/signing scripts pass | NOT VERIFIED |
| Android emulator scenarios | pause/retry, corrupted offline media, reconnect | NOT VERIFIED |
| Personal-phone install/update | only after all previous gates pass | NOT ATTEMPTED |

## Manual smoke tests after successful verified build
1. Play 3 public songs, select the second, close/reopen NightWave. Confirm the
   same song and queue metadata are shown. Playback should wait for explicit
   Resume and should not start automatically.
2. Resume, pause and use Next/Previous. Shuffle/repeat should retain the
   selected public track. Bluetooth/lock-screen controls must work.
3. Import a short personally owned audio file. Verify offline playback in
   airplane mode; relaunch and explicitly resume its saved checkpoint.
4. On a disposable emulator install only, truncate/delete a test-owned imported
   file. The app must show a warning, must not play/upload it, and must keep
   its index record. **Do not damage music files on the personal phone.**
5. While an online song is buffering, tap another song. Old requests must
   not replace the new selection. Pause during a temporary network failure;
   no delayed callback should restart playback.
6. Confirm sleep timer stops playback and cannot be undone by auto-retry.
7. Temporarily disconnect/reconnect the emulator media controller. Status
   must not be overwritten by stale events and playback must not auto-start.
8. Confirm Google Drive vault can still list files, and only run upload/
   download of a small personally owned test track. Existing cloud library
   and playlist data must remain unchanged.
