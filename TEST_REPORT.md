# Current verification - NW135 owned import and offline readiness

29 October 2026. [Draft PR #152](https://github.com/Nahian2002/NightWave/pull/152), `update/nightwave-nw135-offline-journey`, stacked on NW134 #151. Protected parent `b7f0fad71604b601a5ea2e82f482229601fdf189`; built revision `fd47bcdf161427a1fabd8ae85ec15dfbc90a4212`. [Architecture/preservation](docs/NW135_OFFLINE_IMPORT_READINESS.md), [ten-case supplement](docs/quality/NW135_OFFLINE_ACCEPTANCE.csv). [Previous verification](https://github.com/Nahian2002/NightWave/blob/b7f0fad71604b601a5ea2e82f482229601fdf189/TEST_REPORT.md), earlier acceptance and frozen audits remain evidence.

Owned imports now require a real Android audio track and readable sample before the existing rename/index commit. Bounded copy/check/save stages and partial/limit/failure counts are explicit; originals, existing entries and duplicate IDs remain. Only a readable empty index simplifies the Library controls. Read-only Check audio/recovery, explicit source choices and lifecycle-bound index observation address the remaining offline journey. Metadata-only availability is labeled separately from media checks. This is not a full-file decode, checksum certificate, actual audible-playback or seek proof.

| Executed or compiled check | Result and limit |
| --- | --- |
| Complete Windows builder | PASS: production Android/Compose, debug APK and instrumentation-source compilation. First attempt halted before Gradle at the historical cache-variable marker; updated guard retains background scan requirements and checks the read-only failure overlay. |
| JVM suite | **1,688 tests / 210 suites**, zero failures/errors/skips. Six added JVM cases cover unreadable-index/empty separation, bounded stages/results, ownership/formats and real audio-track/sample conditions. Existing regressions remain. |
| Current source runner | **70/70 PASS**. New NW135 actual-caller checks include nine broken-caller fixtures. Existing guards/tests retained; no suppression. |
| Android tests | Five new isolated file-URI/WAV import/probe cases compile only: phase order/reconstructed reader, spoofed audio, same-size damaged audio with retained index/bytes, missing/empty inputs and duplicate identity. Existing file imports now use valid WAVs and isolated data. These do not exercise the actual system SAF picker. Existing theme matrix remains **612 compiled-only cases**; NW134 three queue cases remain compiled only. All device execution NOT_RUN. |
| Sequential lint | **0 errors / 313 warnings / 17 hints**, unchanged counts; no new suppression/baseline. Inherited unreachable Library description compiler warning remains. |
| Source preservation | **230 previously protected files byte-identical**, four authorized offline library/UI/recovery/view-policy changes and nine retained callback inventories. Schemas, dependencies/providers, playback/queue, Search/source identity, account/consent and data stores remain. Installed data requires separate evidence. |
| Signer | PASS against delivered NW134 16B498CC APK; same package/certificate. Installed-phone comparison NOT_RUN; no install/reset. |
| Acceptance preparation | 219-case source/APK-bound fixture pack PREPARED_NOT_EXECUTED. Original four NW135 rows and ten-case supplement NOT_RUN; prior physical gates carried forward. |
| Device/hosted evidence | No connected adb device/emulator. No system picker/grant, real storage/commit failure, airplane-mode audio/play/seek/skip/restart, full decoder, TalkBack or installed-data checks run. Actions stays disabled; no hosted CI pass, public binary or release. |

| Artifact identity | Value |
| --- | --- |
| Build ID | `NW-debug-2D35C8B666C72659` |
| Canonical source SHA256 | `2D35C8B666C72659F40B9A52B896B1BAC9AEC3F4B8BFFD5B4E861EE81AE72522` |
| Canonical inputs | `895` |
| APK SHA256 | `7F87CFD5426D4D1D421F0AC2AFB389EB1771B1F10A17C2213C09F2E2D549D6CA` |
| APK bytes | `29,026,689` |
| Embedded revision context | `fd47bcdf161427a1fabd8ae85ec15dfbc90a4212` |
| Separate NW08 manifest SHA256 | `284D3F3EEC2F0705B7FB1F25AC293A89387CDDCB28A38AAFECE0AD6D83C82125`; independent ordering/encoding retained |
| App identity | `com.nightwave.player`, `2.0.0-alpha5s`, code35; min26/compile-target36 unchanged |
| Certificate SHA256 | `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394` |

Private checkpoint `C:/Users/jayed/NightWave-checkpoints/20261029-nw135-offline-journey` retains recovery bundles/source ZIP, original failed preflight, raw JUnit/lint/build/source/signature receipts, preservation, fixtures and owner-only APK delivery proof. Drive permissions show only the verified owner; streamed download matches exact APK bytes/SHA256. Recordings, extracted media, binaries and accounts remain outside Git. Documentation commits do not change canonical build inputs.

Four themes/features, original source metadata/URLs/Details, artwork/mood/pager/Search boundaries, single-owner player transition, NW134 queue edits/Undo, existing download restrictions, lyrics/offline/cloud/account/consent, schemas and signing remain. Probe results are session-local, and later corruption/device support still needs actual checks. Latest recorded visible UI stays **8.8/10** from 030542; original whole-product 6.7 stays frozen. No new score follows from tests. **Device acceptance NOT_RUN; public release NO-GO. Next independent source batch: NW136.**
