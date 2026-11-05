# NW150 — Samsung code46 scoped phone result

5 November2026. **PARTIAL_SCOPED_PASS_PRESERVED**. Owner explicitly approved **Install version46 and test**; [committed scoped plan](NW150_SAMSUNG_CODE46_TEST_PLAN.md). Cumulative draft review [#176](https://github.com/Nahian2002/NightWave/pull/176); exact source/APK/signer and host checks remain in [NW150_SOURCE_EVIDENCE.json](quality/NW150_SOURCE_EVIDENCE.json). No app/source/APK change was needed for this follow-up.

## Installation and preservation

Physical Samsung SM-S928B reports Android17/API37,1440×3120,font0.8,AMOLED/System. Initial USB inventory was empty; owner reconnected and the same authorized phone on43 was confirmed. This was not an app failure. Freshly pulled43 matched its known hash; actual43/candidate46 signer continuity passed. A private source recovery bundle and committed approval plan preceded installation. The default Windows PowerShell launch rejected the verifier under its execution policy; the existing Codex PowerShell runtime ran the same verifier successfully, without changing policy or settings.

NightWave alone was stopped for a consistent private TAR backup. Every archived file matched its collected SHA256. In-place43→46 passed: **all29 collected preference/database files retained identical hashes before first launch**. Pulled installed46 is29,311,242 bytes/SHA256 `11258AE163BCBE357D7E890C6093FB6D70AEF4C0ED92D4BF810BBA7953FE9BC0`, matching the verified delivered APK. Package/signer unchanged. Build `NW-debug-D3D3F6A4CD0C40D5`, built revision `1bd431d5a235e055fe076b83a32346e033ecbb1b`, canonical source `D3D3F6A4CD0C40D5420D8D1AFA310E00D49B11F0534B949BB005A771E8387C7D`/1008 inputs.

After the checks, **six core preference files** match the original bytes: guest profile, music library, on-device index, saved queue, offline session and search history. The complete guest-profile field map also matches. OriginalAMOLED/System, Android animator scale1 and font0.8 are unchanged. Temporary submitted query removed through its exact in-app history action; original zero-entry history list and original file bytes restored. No pre-existing history was evicted or removed. Other normal runtime files were not claimed byte-identical after app use; all29 were verified before first launch.

## Observed checks

| Check | Result and practical limit |
| --- | --- |
| Cold launch | Normal Home, paused selection; ADB TotalTime1,310ms is one sample, not fully-drawn/p95 performance. |
| Navigation | Normal remote taps reach Home/Search/Library/You. Clear query → Library/You → Search returns empty; no stale auto-filled text observed. |
| Cancel search | Observed live loading then tapped Cancel. Neutral Search cancelled, Retry and Open YouTube search visible; no false Online search unavailable label. |
| Background return | Retry starts loading; Home/background then return shows Search paused while away, Retry and web fallback. Playback stays paused. |
| Public retry | One temporary generic query completed with20 online results and Search details; uploader/source-kind/duration visible on result rows. No public audio played or downloaded. Identical-title/video-ID selection is still a fixture/device gap. |
| Matching refresh/cancel | Matching20 results visible, Refresh starts, then Cancel preserves all20 and Previous results are kept guidance. No extra history submission from typing/refresh. |
| Saved-only query | Existing saved caption and Recent origin appear while typing, without online loading; prior public-result count disappears for the different query. No track tapped/queue changed. |
| On-device destination | Existing single indexed entry found through its extension. Selected-entry navigation shows the new exact-entry banner, corresponding title and Local file present · Size checked. Duplicate/missing/renamed physical fixtures were not manufactured. This is not a full seek/decode/integrity test. |
| Owned-LRC guide | Guide, UTF-8/64KB/review/ownership/exact-source instructions and Import action reachable. No LRC attached to current selection; no import/editor-save/export/remove performed. Follow/large-font/import cases remain NOT_RUN. |
| Full-player reopen | First full player and three normal mini-player reopen cycles show Paused/Play/Minimize. Artwork crop is pixel-identical across the three captures and nonblank. No frame/haptic/continuous-gesture certification inferred. |
| Final state | Home, paused, original profile/settings/history preserved. Current app-PID crash buffer contains zero FATAL EXCEPTION records; this does not certify all past/future stability. |

## Inspection limitations and follow-ups

Some early PNG captures contained incompletely drawn regions despite the full semantics tree; later captures, full-player captures and a short independent screen recording show complete content. The static recording has two variable-rate idle frames; sequential decoding succeeds, a fixed7-second seek was unavailable. Its metadata must not be called app FPS. Raw recording byte/hash readback matched; only its verified task-owned remote copy was removed. Cause remains unknown; do not call the earlier Samsung43 anomaly resolved or a persistent app blank screen reproduced. Raw screenshots/video/accounts/media/file hashes remain private outside Git.

After a background return the focused Search field reopened the keyboard, reducing result space. It could be dismissed and no stuck page was observed in the checked routes. Add a submit/resume focus review to NW151; this is a bounded UX observation, not a new crash or a claim all Android versions behave identically.

Nine new Android fixture cases remain **COMPILED_NOT_RUN**; no test APK or full suite was installed on personal data. Duplicate/renamed/missing-file fixtures, actual LRC follow/editor/import, large-font/other themes, older Android, audio/real transfer/soak/study/long-absence and original219/full NW145 gates remain pending. No automatic9.5/100% or full functional rating. Source host1854 JVM/233 suites,92 checks/full builder/sequential lint0/327/17 remains valid; no unnecessary app rebuild. No reset/uninstall/clear-data, account/consent/EQ/system-setting change, public binary/release/feed/main merge or automation activation. **Next independent source batch NW151.**
