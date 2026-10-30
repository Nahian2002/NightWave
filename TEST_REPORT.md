# Current verification — NW141 accessible bilingual primary flows

30 October 2026. Branch `update/nightwave-nw141-accessibility`, stacked on [NW140 draft #162](https://github.com/Nahian2002/NightWave/pull/162). Protected parent `6f767c92452a22394396081c9c47b17863cc393c`; plan `7c454a77`; implementation `959a4efcf9b94882b56107fa6bd2efa237b353a6`; final quantity/reference repair and build revision `ba709e2ea5b6a00e8c43c773835430b0399566a2`. [Results](docs/NW141_ACCESSIBILITY_BILINGUAL.md), [device runbook](docs/NW141_DEVICE_RUNBOOK.md), [ten cases](docs/quality/NW141_ACCESSIBILITY_ACCEPTANCE.csv), [schedule](docs/quality/UPCOMING_UPDATES.md), [previous report](https://github.com/Nahian2002/NightWave/blob/6f767c92452a22394396081c9c47b17863cc393c/TEST_REPORT.md).

Primary Compose resource/accessibility readiness only. 442 string and four quantity resources have matched Bangla counterparts; original six resources also localized. Resource locale drives presentation-only digits/durations/account dates. Full Unicode queue/cue/remove descriptions, heading/Tab/selected/action semantics and Bangla font extents are wired. Features, themes, original URLs/quality, persistent formats, local data and account/consent authority remain. Legacy native import/recovery previews, advanced/provider diagnostics and user text retain their original meaning; full-application translation is not claimed. NW143 carries native/failure-copy review.

| Check | Final evidence |
| --- | --- |
| Full Windows builder | Android/Compose/APK and Android instrumentation-source compilation PASS |
| JVM | **1,808 tests / 223 suites**, zero failures/errors/skips; all original 1,803 tests retained plus five locale/duration boundaries |
| Current source | **82 PASS_SOURCE_ONLY**, eleven new rejected resource/caller negatives; old negative fixtures retained and exact resource callers followed |
| New Android tests | Four methods compile only: locale/Unicode resources, quantities/dates, real navigation recreation/selection and separate large-Bangla Apply/Discard; **NOT_RUN** |
| Sequential lint | **0 errors / 322 warnings / 17 hints**, unchanged ID/severity/message/path inventory; no suppression |
| Preservation | **332** prior main files byte-identical; only twelve prior UI files changed. Native Activity/account prefix unchanged except resource import; engines/stores/sources/build/dependencies/manifest unchanged |
| Secrets | Implementation-stage all-ref Gitleaks **3,574 commits**, no leaks; exclusions unchanged |
| Identity/delivery | Embedded canonical source independently matched; actual NW140 signer matches; owner-only Drive remote size/SHA readback verified |
| Device/runtime | Original 219-case pack prepared only; four Android methods, ten supplemental/original/prior phone/performance/account/release cases **NOT_RUN** |
| Consequential actions | No install/reset/uninstall, device test/launch, app account operation, merge, Actions/settings change or public binary |

Build **NW-debug-C4D430224921F123**; source **C4D430224921F123F14D2E5ED80E7E9724D82242BC763F103DD46CF77D227C35**, **951 inputs**. APK **AC566E55078292380D3EB6405EDF5AB5BF6D77E1A99C0436F54C21FBAAA46F4C**, **33,844,842 bytes**. Signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**; package `com.nightwave.player`, `2.0.0-alpha5s` / code35 unchanged. Actual Gradle output equals promoted APK; dex/resources changed. The initial missing quantity compilation failure is preserved privately, repaired and covered by a missing-reference regression; only the successful final build is delivered.

Private baseline/final bundles and exact working-byte ZIPs, raw JUnit/lint/build/provenance/signature/source/delivery/remote receipts remain outside Git. Actual TalkBack/Switch Access, all-theme 1x/2x/3x clipping/contrast/touch bounds, native-speaker review, locale recreation, installed-data/signer, original performance and recovery acceptance are still required. No new numerical rating or 9.5 certification is awarded. **Next independent source batch: NW142**, then NW143–NW145. Saved schedule guards remain; this manual run was explicitly owner-authorized despite the earlier scheduled admission estimate.
