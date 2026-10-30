# Current verification — NW140 Android measurement readiness

30 October 2026. `update/nightwave-nw140-performance`, [draft PR #162](https://github.com/Nahian2002/NightWave/pull/162), stacked on [NW139 #161](https://github.com/Nahian2002/NightWave/pull/161). Protected parent `923d7f8179efe1c6445e68dc199522504de50657`; plan `90acd22bf6d306d1c53b7dfe6fe462befa00f6dc`; implementation `3c9f7781153ce126d5117029b39e15374e775581`. [Evidence](docs/NW140_ANDROID_PERFORMANCE.md), [device runbook](docs/NW140_DEVICE_RUNBOOK.md), [frozen targets](docs/quality/NW140_DEVICE_PROTOCOL.json), [ten supplemental cases](docs/quality/NW140_PERFORMANCE_ACCEPTANCE.csv), [hourly schedule/budget guards](docs/quality/UPCOMING_UPDATES.md), [previous report](https://github.com/Nahian2002/NightWave/blob/923d7f8179efe1c6445e68dc199522504de50657/TEST_REPORT.md). **NW141 source is next**, then NW142–NW145.

**Measurement readiness only; Android performance remains NOT_RUN.** No production optimization, baseline profile or speedup is claimed. The explicit opt-in diagnostics use real ART query-runner and actual Search fixture callers over isolated synthetic 10k stores. Wall-clock ART query timing is separate from Compose's auto clock/automation/scroll/debug overhead. Bounded copied frame data keeps first-draw exclusions, drops, missing deadlines and partial/low-count status explicit. Strict private analysis binds raw samples, app/test hashes and matched environment/harness, but cannot authenticate device execution or award performance/rating acceptance.

| Evidence | Final result / limit |
| --- | --- |
| Windows builder | Android/Compose/APK/instrumentation-source compilation PASS |
| JVM | **1,803 tests / 222 suites**, zero failures/errors/skips; unchanged inventory |
| Current source | **81 PASS_SOURCE_ONLY**; new evidence math/identity/sample/environment fixtures reject twenty invalid receipts, plus caller-negative checks |
| Diagnostic artifact | Test APK separately assembled; package, exact target and signer verified; not installed or executed |
| Sequential lint | **0 errors / 322 warnings / 17 hints**; unchanged notice inventory, no suppression |
| Preservation | **939 prior canonical inputs identical**, check runner only edited, three additions; all **344 app/src/main files** byte-identical |
| Original contracts | Player/features/themes, sources/quality, data, account/consent, dependencies/build/manifest/package/signing unchanged |
| Secrets | Implementation-stage all-ref Gitleaks **3,569 commits**, no leaks; reviewed exclusions unchanged |
| Recovery/delivery | Source/APK/test identities, prior NW139 signer, private recovery and owner-only app/kit streamed SHA/bytes verified |
| Android/physical | Two new diagnostics compile/assemble only; ten supplemental/original/prior performance/device/account gates **NOT_RUN** |
| Hosted/release | Actions disabled; no hosted pass, install/reset/uninstall, device launch/test, account action, merge or public binary |

Build **NW-debug-2B3F3E0DD6C787B1**; source **2B3F3E0DD6C787B1EFF0071979C60F5033BF6A4CF974A6316AB1FA9AF1943EBE**, **943 inputs**. App APK **82ABC29DB2D91B46D31C0CBD801E464E7F4E52BD46AAC98ABB6B1019FBC12F87**, **33,844,842 bytes**. Test APK **55B9187A957AA37160C601F87663AE6EAE2D5C14B5530974A54D637F1F54A652**, **1904636 bytes**, package `com.nightwave.player.test` targeting `com.nightwave.player`. Both signer SHA256 **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**, same as the actual previous NW139 app. App version `2.0.0-alpha5s` / code35 unchanged.

Private eight-entry kit **0DF9CB0A50C15C0DAC2E6BB614EEA08D8700FC2522E0CAEAE79171541F7D78FE**, **27,093,544 bytes**; every entry/CRC and remote streamed size/hash verified. The measurement identity and original 219-case pack are PREPARED_NOT_EXECUTED. Real startup TTID/TTFD/warm, actual input-to-display, 100 local Next-to-independent-audible-start, production frame/Perfetto/allocation/retained-memory, matched-power battery and 60-minute integration/final two-hour soaks remain required. No new 9.5 rating is inferred; existing recorded scores retain their historical limits.

Required owner input is an approved physical device/test profile and explicit measurement slot. The kit does not install or run anything automatically. Receipt JSON is structural evidence only; debug diagnostic distributions are not app-wide certification. Original data/update/cloud/account/accessibility/source-rights/release acceptance remains open. Weekly reserve and five-hour completion-budget admission remain active; allowance observations stay private.
