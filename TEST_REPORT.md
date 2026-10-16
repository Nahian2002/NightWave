# NightWave v0.5 — Build & QA status

Date: 2026-10-17

## Existing, user-confirmed baseline
- A previous NightWave debug APK built on 64-bit Windows using \`build-windows.cmd\`.
- All five initial S24 Ultra playback checks passed (including screen-off and Bluetooth play/pause).
- These prior tests do not establish that new v0.5 behavior has passed.

## Newly implemented in v0.5
- Home/Search/Library navigation, track rows and now-playing dialog.
- Favorites, recent history, named playlists, save/remove actions stored on-device.
- Playing queue, per-track jump, next/previous, shuffle/repeat and queue snapshot recovery.
- Service-owned auto-advance after \`Player.STATE_ENDED\`, resolved on a worker thread with request-generation invalidation.
- Bluetooth NEXT/PREVIOUS key handling, preserving existing Media3 play/pause handling.
- Version bump to 0.5.0 (versionCode 5) and preserved Android application ID.

## Completed checks
- Kotlin 1.9 JVM compilation of the queue engine: PASS after fixing an unsupported deque append call.
- Kotlin smoke harness: PASS (38 transitions/edge cases, including shuffle, repeat one/all/off, history, jump bounds, 100-item cap).
- GitHub verification: all new Kotlin files, queue persistence, UI and resources confirmed uploaded.
- ViewBinding static check: 30 layout IDs, all referenced UI bindings present. PASS.
- MediaSessionService, audio stream resolver and foreground media service remain installed.

## Not yet verified
- Full Android Gradle APK build from current v0.5 source: **PENDING WINDOWS BUILD**.
- Android instrumentation / on-device UI smoke test: **PENDING**.
- Live YouTube search following v0.4/v0.5 modifications: **PENDING**.
- Playlist auto-next across multiple songs while screen locked: **PENDING**.
- Media notification/Bluetooth NEXT/PREVIOUS behavior across manufacturers: **PENDING**.
- Saved library upgrade/data persistence across reinstall: **NOT PROMISED**.

## User validation required
1. Download a fresh ZIP from GitHub and build with \`build-windows.cmd\`.
2. Install the new \`dist\NightWave-debug.apk\` over the old app without uninstalling.
3. Search \`Adele Hello\` in Songs and Videos, then tap a result.
4. Like it; create a playlist and add 3–4 results. Switch between Home and Library.
5. Play the playlist, lock the display, and let two consecutive songs finish.
6. Test next/previous with Bluetooth and the queue dialog. Reopen the app and verify saved favorites/queue.
7. Report the first compiler error if the build fails; don't remove the working app until validated.

No GitHub-hosted runner is used for the free local build.
