# NW145 — First owner-approved phone session, code40

4 November 2026. Source remains COMPAT01 `3674B2BE4F6ADE0D25EC676186A941F64809ABE0D911A4BD1E1D32914690E406` (975 inputs), build `NW-debug-3674B2BE4F6ADE0D`; no app code, APK, data schema or signing change in this evidence branch. Private raw screenshots, UI trees, APKs, preference/database hashes, logs and evidence inventory remain outside Git. No private track titles, serial, account data or raw device content is published here.

## Authorization and identity

Owner connected and authorized USB debugging, then explicitly approved “Install version 40 and test.” This approval covered the verified version40 in-place upgrade and non-destructive NightWave checks; it does not authorize reset/uninstall/clear-data, new APK installations, account/consent actions or repository publication changes.

Connected device reports Samsung SM-S928B, Android17/API37, physical1440×3120, density560 override (physical600), font scale0.8. These are observed device properties, not a claim about official firmware or coverage of other phones. The installed APK was code36/source4085DB54 before the session. Its actual pulled signer matched code40. Explicit `adb install -r` succeeded; the pulled installed code40 bytes exactly match **7BACDEE3421D3E8093985D3602467295433F569C6D5FE26830EFD184B45615D3**. All29 collected preference/database file hashes were byte-identical immediately before/after replacement, before first launch. This snapshot does not certify every audio file or future migration/restore behavior.

## Actual scoped results

| Check | Observation | Scope |
| --- | --- | --- |
| In-place update / identity | PASS | Actual old/new signer match, install success, installed APK hash readback and29 unchanged collected data-file hashes |
| Cold open / oversized mini-player | PASS for this launch | Saved selection visible, compact mini-player and Home content visible; reported ADB cold launch1496ms, one sample |
| Fresh navigation | PASS for tested sequence | Home → Search → Library → Search → You and later Home; fresh screenshots/trees confirm intended screens |
| Unexpected search text | PASS for empty-query return | Search field empty before and after Library return; typed-query, keyboard, submit/history and all-theme behavior not exercised |
| Help and privacy | PASS | Opens from App health; Back returns to the same settings area |
| Manual update check | PARTIAL / REQUEST UNAVAILABLE | Screen opens immediately despite automatic cooldown; displays failure/retry UI and Back returns. Delivery is not a pass: stable feed is still disabled/unpublished, and the phone request's HTTP root cause was not captured |
| Play/pause controls | PASS for UI state | Existing selected track switches Resume → Pause → Resume. Left paused; no audible quality, timing or headset claim |
| Short background return | PASS for10 seconds | Existing task returns to Search with empty field and paused control; ADB HOT return177ms, one sample |
| Full-player expansion / Back | PASS | Artwork tap opens full player with selected source paused; Back returns to Search; Home restored afterward |
| Crash-buffer observation | No fatal record in current PID | Filtered current-process crash buffer only; does not prove absence of all historical/process-restart/other-process failures |

Initial reused UI-hierarchy captures did not consistently match the screenshots. Those navigation observations were not counted. The private test helper was corrected to use a unique dump file and require an explicit successful dump on every read, then the navigation sequence was repeated. No app bug was inferred from stale automation state. The original oversized blank panel was not reproduced in the cold/short-return checks; a true long-absence reproduction remains pending.

Final phone state: NightWave Home, selected track paused. No library import/export/delete/restore, new playlist, Google connection/consent, system setting change or reset/uninstall was invoked. Ordinary app startup/playback state may update normal runtime preferences; only the pre-first-launch29-file snapshot is claimed byte-identical.

## Still pending

NW145 is **PARTIAL**, not certified. Original219 cases and supplemental matrices remain unpromoted by these smoke checks. Android6/7 and other devices/OEMs, all themes/fonts/accessibility, typed search/history, public-source/download throughput, native file/playlist roundtrips, Bluetooth/headset/call/audio focus, automatic7-day/new-release flow, audible quality/timing, long absence/process death, data restoration, eight-listener study and two-hour soaks/performance/battery remain NOT_RUN. No 9.5 rating or public-release GO follows. Release feed activation/public APK/main merge/settings changes remain separately gated. Future non-destructive checks on this approved phone can continue without repeating version40 permission; any other installation remains outside this specific approval.

[Draft phone evidence review #170](https://github.com/Nahian2002/NightWave/pull/170), stacked on COMPAT01 #169; unmerged.
