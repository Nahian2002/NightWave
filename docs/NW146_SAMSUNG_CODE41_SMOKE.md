# NW146 — Owner-approved code41 phone checks

4 November 2026. Owner explicitly approved **“Install version41 and test”** on the connected Samsung SM-S928B, which reports Android17/API37. This session covers the in-place replacement and targeted non-destructive queue/update checks. It does not authorize any subsequent APK, reset/uninstall/clear-data, account/consent or system-settings change. Raw screenshots, UI trees, package data, APKs and saved-file hashes stay private outside Git.

## Identity and preservation

The actual installed code40 was pulled again and matched its prior APK hash. Its signer matched the verified code41 candidate. Installation with data retained succeeded. The installed code41 APK was pulled back and matched **38D00A4111655583BB782480D472C51F9B5453455257DF71504051AF220DF615**, 29,080,179 bytes. Package `com.nightwave.player`, signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**; exact build/source identity remains in [NW146 source evidence](quality/NW146_SOURCE_EVIDENCE.json).

All **29 collected preference/database file hashes were byte-identical** immediately before/after installation, before first launch, with the app stopped. This is a scoped snapshot, not a complete music-file, restoration or future-migration certification. Normal launch and play/pause operations can subsequently update runtime state.

## Observed results

| Check | Result | Evidence and limits |
| --- | --- | --- |
| In-place upgrade and installed identity | PASS | Actual old/new signer continuity, installation success, installed APK readback and 29 unchanged saved-data hashes |
| Cold opening | PASS for this launch | Home and compact mini-player visible with saved selection; ADB reports 1,387ms for one cold launch, not fully-drawn latency or a percentile |
| Paused queue | PASS | Current track header and Paused badge agree with full-player Paused state |
| Playing queue | PASS for UI state | Briefly resumed the existing selected track; queue badge changes to Playing; no audible-quality/timing claim |
| Pause again | PASS | Full-player Pause followed by queue shows Paused; selected item and 24 upcoming count retained; no queue edit/remove/clear action |
| Manual update check | PASS for unavailable-state messaging | You → App health → Check for updates opens directly and displays the specific unavailable/not-ready message with Retry; no false Disabled or up-to-date claim |
| Retry and return | PASS for tested path | Retry returns to the same unavailable state; Back returns to You; Home remains usable |
| Current-process crash-buffer observation | No fatal record found | Only the current app PID's crash buffer was inspected; not an all-process or historical crash guarantee |

The launcher hierarchy capture initially returned no successful dump marker; its screenshot showed the launcher. It was not reused as a valid UI tree and is not evidence of an app failure. Fresh app captures all completed. One updater-label lookup occurred before the row entered the viewport; it performed no tap. After scrolling, the observed row was used successfully.

The actual HTTP root cause on the phone was not captured. The unavailable message verifies the UI classification, not successful update delivery. No public feed, signed release, download, installer handoff or seven-day prompt was activated or exercised.

## Remaining acceptance

Device acceptance is **PARTIAL**. Buffering/resolving/disconnected/error phases, live signed disabled/current/available/invalid-metadata and offline states, automatic seven-day timing, real in-app download/install, spoken TalkBack output, other themes/fonts, long absence/process death and older Android devices remain NOT_RUN. The isolated Android queue semantics test remains COMPILED_NOT_RUN; manual checks do not count as its execution. Original219-case and supplemental matrices, audio/performance/battery/route/soak/study gates remain unpromoted. No new rating or release approval follows.

Final phone state: **NightWave Home, existing selection paused**. No library edit/import/export/restore, account or Google consent action, system setting change, reset/uninstall or release activation occurred. Source/APK unchanged by this documentation-only follow-up; prior host results were not rerun. Next source batch **NW147**.

[Draft review #172](https://github.com/Nahian2002/NightWave/pull/172) remains unmerged.
