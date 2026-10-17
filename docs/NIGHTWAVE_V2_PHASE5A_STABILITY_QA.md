# NightWave 2.0 — Phase 5A stability, background playback and crash diagnostics

**Branch**: feature/nightwave-v2-phase5a-stability
**Version**: 2.0.0-alpha5a (code 17)
**Status**: source implementation committed. Android Gradle compilation, Android instrumentation and real-phone crash reproduction are **NOT verified here**.

## Reported condition
A Samsung Galaxy S24 Ultra reported NightWave was "crashing frequently" after the Alpha 4 development installation. The user later liked the cleaner premium UI. No Android FATAL EXCEPTION stack trace has yet been provided. Therefore **the actual cause of that warning remains unconfirmed**. Do not treat a passing source review or build as proof that the recurring Android crash is fixed.

## Source findings and fixes
1. **Unrequested network resolution during Media3 creation**: Previously MediaSessionService.onCreate restored the last queue AND invoked NewPipe YouTube audio extraction immediately, even if no Play command was requested. The service can be recreated simply by a UI binding or Android recovery. Phase 5A now restores only metadata and local queue state at creation. Only an explicit Play (button, headset play, RESUME_SAVED) resolves a fresh online stream.
2. **Periodic progress work when idle**: The background service previously scheduled the progress writer every 2.5 seconds whenever it existed. It now schedules writes only while playback is active, persists on pause and service destruction, and skips unsafe bookmark writes. Local offline files are not saved as signed streaming bookmarks.
3. **Executor shutdown race**: The async YouTube resolver now checks shutdown/release state and catches task rejection instead of crashing if a request arrives while Android destroys the service.
4. **Corrupt saved-position types**: The QueueStateStore bookmark reader catches incompatible preference types, returning no bookmark instead of crashing the Activity or Media3 service. The original raw preference is not deleted.
5. **Corrupt legacy guest profile settings**: GuestProfileStore reads cautiously using typed values from SharedPreferences.all. Wrong-typed fields safely use UI defaults. If the local identity itself is corrupted, it is preserved, shown as a recovery profile, and prevented from being overwritten by a normal Save.
6. **Excessive Room mirror checks**: The premium UI no longer launches a Room/DataStore reconciliation on each routine music-library refresh or playback change. Sync still happens on Resume or an explicit Advanced storage recheck.
7. **Offline-file failures**: A missing/corrupted offline library index yields an unavailable file instead of propagating an exception into the music service. Android service start failure from the offline screen is reported as a retryable error.
8. **First play while disconnected**: The classic Play control requests an explicit service resume even if MediaController has not connected yet. Bluetooth play and media headset resume are also handled when no media item is prepared.
9. **Device-local crash summary**: A bounded crash recorder installs before NewPipe initialization and keeps up to five UTC crash timestamps, exception class names and NightWave-only stack frames. It intentionally stores **no exception messages, song URLs, user files, emails, tokens or app content**. It always delegates to Android's previous uncaught-exception handler; no crash suppression/restart loop. Classic Home → **Diagnostics · Share recent crash summary** lets the user choose a share target. Nothing is uploaded automatically.

## What Phase 5A intentionally does not do
- It does not promise every playback or crash scenario is solved. A logcat FATAL EXCEPTION / Android ANR trace is still required to confirm the source of the earlier "crashing frequently" notification.
- It does not make an untested Compose UI the only launcher; classic startup remains accessible and the premium preview stays opt-in.
- It does not modify or clear favorite/playlist preferences, the guest backup, Room rows, imported offline files or Google accounts.
- It does not implement Google sign-in, cloud recovery, offline YouTube downloading or a new search service.

## Testing and safety
Run \`build-windows.cmd\` in a freshly extracted Phase 5A ZIP. It runs Kotlin/Gradle unit tests and assembles \`dist/NightWave-debug.apk\` on the user's Windows PC without paid GitHub Actions. **The build has not been run in this execution environment.**

Optional source checks: \`python tools/verify_v2_phase5a.py\`. The previous 3B/3C/3D/4 and Alpha4 offline source checks are also version-compatible.

Tests added or extended:
- Three Android guest-profile tests: wrong-typed values, corrupt identity without overwrite, normal profile save.
- Two additional Android bookmark tests: corrupt position and corrupt URL value. Prior five bookmark tests remain in place.
- One additional Android offline-file test: malformed index does not crash local-file lookup; existing offline tests remain in place.

**Never run Android instrumentation tests against personal music data.** Several earlier tests deliberately clear test preferences. Use a disposable emulator or test device/profile and run \`:app:connectedDebugAndroidTest\` only there.

Real-phone acceptance:
- Launch classic and premium preview repeatedly. Test return from Android home, lock screen, notification and Bluetooth; note any system crash warning.
- Leave the app paused with a saved previous song; close UI; re-open the app. It should display saved title/art/bookmark without starting network activity merely to draw the UI.
- Tap Play and verify that only then does a fresh YouTube stream resolve and resume from approximately the last position. Test unavailable/network-offline stream without crashing.
- Verify play/pause, previous/next and automatic queue progression with screen locked for 15 minutes.
- Import a personally owned MP3, switch Airplane mode on and play it; verify user audio files survive process stop and the original selected document is not touched.
- Explore premium Library/Profile; make likes/playlists and verify no disappearance. Technical Room tests remain behind Advanced.
- From classic Home choose **Diagnostics · Share recent crash summary** after a crash. Share the small summary if available; otherwise connect USB and run \`capture-nightwave-crash.cmd\` and provide NightWave's \`FATAL EXCEPTION\`/ANR stack. Review any ADB log for unrelated private information before sharing.
- Force stop via Android Settings only if NightWave continues crashing or draining battery. Do not uninstall or clear data.

## Release gate
Keep this pull request DRAFT and stable main unchanged. Require a successful local build, passing JVM/unit tests, disposable Android instrumentation, repeated real-device playback and startup QA, and a matching crash/ANR trace demonstrating that the original recurring failure no longer reproduces before declaring it fixed or promoting the premium UI to the launcher.
