## Current candidate and owner preference

COMPAT01 code40 replaces code39 for future acceptance. Source upgrades are host-verified; actual devices remain NOT_RUN. Owner will check after source work, so no interim device approval/test is requested. Original preparation plan below is historical; use the updated exact candidate identity.

# NW145 — Device acceptance and re-rating

## Plan — 4 November 2026

Start from cumulative NW144 #167. Preserve all app/source/APK bytes, data, package/signer and account/consent boundaries. Check current evidence, attach the exact code40 artifact and existing prepared 219-case kit, consolidate the required device/user-study/soak/release gates, and provide a short first-session checklist. Record missing evidence honestly. No connected Android device was found by a read-only ADB inventory (zero devices); no app/device action was performed.

This is acceptance preparation, not another app build. No implementation change or new APK is needed without a demonstrated failure. Keep physical cases NOT_RUN, rating NOT_ASSESSED and release NO_GO until evidence and approvals close. Do not run tests on an unapproved personal profile, install/reset/uninstall, change language/settings/account, merge, activate feed, publish binaries or restart automation. A device profile and explicit test slot are required by the existing NW141 device runbook. Ask for the first phone model/Android version while preparing the checklist independently.

## Exact candidate

- Version40 / `2.0.0-alpha5s`, package `com.nightwave.player`.
- Build `NW-debug-3674B2BE4F6ADE0D`; canonical source `3674B2BE4F6ADE0D25EC676186A941F64809ABE0D911A4BD1E1D32914690E406`, 975 inputs.
- APK `7BACDEE3421D3E8093985D3602467295433F569C6D5FE26830EFD184B45615D3`, 28,062,382 bytes.
- Certificate `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`.
- The previously delivered owner-only Drive APK remains the candidate. No new binary is produced by these documentation changes.
- Original [139-case matrix](NW90_EXPANDED_139_CASE_MATRIX.csv), [80-case extension](quality/NW126_NW145_ACCEPTANCE.csv), and prior supplemental device cases remain authoritative. Historical host PASS/PARTIAL rows do not establish physical acceptance. The existing private code40 device kit maps all 219 cases and is PREPARED_NOT_EXECUTED.

## First owner-run session — about 15 minutes

Use the existing installed app only after its App details build identity matches this candidate. Do not uninstall or clear data to match a version. Use owned/disposable audio and a scratch playlist for writes. A recording can contain private titles/account information: keep raw evidence private and share only what you intend.

1. Record phone model, Android version, build ID, theme, font scale and audio output route. Preserve the real installed library.
2. Open Home, switch to Search, type a query, switch tabs and return. Check for the oversized blank mini-player, stale unrelated search text, clipping or a page stuck between tabs. Repeat after ordinary backgrounding; the reported long-absence issue needs a separate later reopen, not a short test labeled as a long one.
3. Play an owned/authorized track. Pause/resume, Next/Previous, open queue and return. Compare the audible song, full player, mini-player and queue. Record any mismatch.
4. Background the app. Use notification controls and return. If available, repeat with a widget and headset/Bluetooth controls. Mark unavailable hardware NOT_RUN; do not infer a pass.
5. Open a scratch playlist's Share action. Confirm the preview's actual links/count/omission text, cancel, and verify the library and queue are unchanged. Repeat with Continue, then cancel the Android chooser and verify the return route/playback state.
6. Export a scratch playlist to an owner-chosen temporary .txt destination; import that text, review, then cancel. Confirm no new playlist was created by cancellation. An approved scratch import may separately check the duplicate-name suffix; do not use a personal backup for this test.
7. Open Help and privacy and Check for updates from You. Return without downloading/installing. The stable feed remains disabled; this does not verify a newer-release or seven-day prompt.
8. Report each result as PASS, FAIL or NOT_RUN with the exact step, expected/actual behavior and timestamp/evidence. A full video is useful for visible behavior but does not establish audible timing, battery, TalkBack or every device condition.

## Full acceptance still required

Execute and attribute the original 219 cases and prior supplemental cases on both declared device classes (flagship and midrange), using [NW140 protocol](quality/NW140_DEVICE_PROTOCOL.json) and [accessibility runbook](NW141_DEVICE_RUNBOOK.md). Capture all themes, normal/large text and supported input/accessibility configurations. No reference models are approved or recorded by this run; owner selection is pending.

Before the eight-listener study begins, lock participant criteria and task order against the frozen [roadmap rubric](PRODUCT_QUALITY_ROADMAP.md#concrete-95-acceptance-standard). Include Bangla/English and accessibility participants; perform find/play, save/playlist, queue/Play Next, offline status/listening, lyrics, sound Apply/Discard, recovery and privacy/update discovery without coaching. Record every completion/failure and tap count, missing hardware/conditions and all exclusions. No participant outcomes or prior completed task-study dataset have been supplied here. Target >=95% completion is a project criterion, not proof from eight people alone.

Use the preregistered performance protocol for >=20 startup/typing samples per condition and >=100 measured local Next transitions. Run the required two-hour mixed foreground/background playback/route soak per reference, plus real offline/restore and matched battery/performance evidence. Do not substitute a stopwatch video or JVM test for audio/frame/power traces.

## Decision

Rating remains NOT_ASSESSED for this candidate. Award >=9.5 only on the frozen weighted rubric with no core area below9 and no unresolved critical/high task failure or unknown critical gate. Release remains NO_GO independently until device/data/update continuity, privacy/provider/rights/distribution and owner approvals are satisfied. Public feed remains disabled; no automated publication or merge is authorized. Continue source changes only for a demonstrated failure, with recovery and regression evidence.
