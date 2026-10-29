# Current verification - NW134 queue edits and compact UI finish

29 October 2026. [Draft PR #151](https://github.com/Nahian2002/NightWave/pull/151), `update/nightwave-nw134-queue-finish`, stacked on DESIGN06 #150. Protected parent `b04248cd519a7dcc313e3a31443103c0e821a7be`; implementation/built revision `8a5a480f23308c772f5fcbf60e5d8af3f3cca2fc`. [Architecture/latest review](docs/NW134_QUEUE_TRANSPORT.md), [12-case supplement](docs/quality/NW134_FINISH_ACCEPTANCE.csv). [Previous verification](https://github.com/Nahian2002/NightWave/blob/b04248cd519a7dcc313e3a31443103c0e821a7be/TEST_REPORT.md), earlier matrices and the original audit remain evidence.

Latest recording 030542 scores **8.8/10 visible UI** on the same five equal dimensions: identity 8.5, color 9, hierarchy 9, observed coherence 9, perceived readability 8.5. Sequential decoding read 8,639 frames through 130.4267s with monotonic timestamps; review used 131 one-second samples, two overviews and six details. Exact recorded build is unknown; encoding rate is not app performance. No lasting stuck/blank page is observed in samples. No new candidate score is assigned; original whole-product 6.7 remains frozen.

Statistics short actions now wrap to measured content; singular/plural labels, automatic collection category icons, compact save guidance and quieter selection address observed finish issues. NW134 adds service-owned snapshot-checked reordering within Play Next/Up Next and confirmed Clear Up Next, preserving current/earlier songs, stored music and transport. Undo restores original manual priority and rejects a changed remainder. Shuffle restrictions are explained; offline collections retain existing controls. All themes/features remain.

| Executed or compiled check | Result and limit |
| --- | --- |
| Complete Windows builder | PASS: production Android/Compose, debug APK assembly and Android instrumentation-source compilation from final implementation. |
| JVM suite | **1,682 tests / 209 suites**, zero failures/errors/skips. Ten new queue scenarios plus one plural regression execute; existing shuffle/repeat/history, queue/transport and source-selection regressions remain. |
| Current source runner | **69/69 PASS**. Adds NW134 actual UI/bridge/service/queue checks with seven broken-caller fixtures. Earlier checks remain; no guard weakened. |
| Android tests | **17 x 36 = 612 theme/width/font cases compiled only**, adding measured short-action bounds/callback coverage. Three new queue UI tests compile only: confirmation snapshot, absolute reorder identities and shuffle restriction. Device execution NOT_RUN. |
| Sequential lint | **0 errors / 313 warnings / 17 hints**, unchanged counts; no suppression/baseline. Inherited Kotlin compiler warning for an unreachable Library description branch remains. |
| Source preservation | **231 previously protected files byte-identical**, three intentional queue/service/bridge changes, nine callback inventories retained. Schemas, providers/dependencies, core identity, library/history storage and consent/accounts remain. Installed-data behavior is separate. |
| Signer | PASS against delivered DESIGN06 0195B157 APK; same package/certificate. Installed-phone comparison NOT_RUN; no installation/reset. |
| Acceptance preparation | 219-case source/APK-bound fixture pack PREPARED_NOT_EXECUTED. Original four NW134 rows and new 12-case supplement remain NOT_RUN. |
| Device/hosted evidence | adb inventory has no connected device/emulator. No real audio/service/restart/notification/headset/Bluetooth/call/audio-focus tests performed. Actions stays disabled; no hosted CI pass or public binary/release. |

| Artifact identity | Value |
| --- | --- |
| Build ID | `NW-debug-16B498CC3E7811BB` |
| Canonical source SHA256 | `16B498CC3E7811BB3CA673A72007539865C5B43DFD898FA808F3D84786E2505B` |
| Canonical inputs | `889` |
| APK SHA256 | `5AFFF3445F6091CA1C6A9404575E3AB48CE9D766F357BF7A9B238C4B06B91D36` |
| APK bytes | `29,026,015` |
| Embedded revision context | `8a5a480f23308c772f5fcbf60e5d8af3f3cca2fc` |
| Separate NW08 manifest SHA256 | `9281FFD5997BB3FFC0CF015D85F387DF6DCB40FF8DFC42370C84706332F55DF4`; independent ordering/encoding retained |
| App identity | `com.nightwave.player`, `2.0.0-alpha5s`, code35; min26/compile-target36 unchanged |
| Certificate SHA256 | `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394` |

Private checkpoint `C:/Users/jayed/NightWave-checkpoints/20261029-nw134-queue-finish` retains recovery bundles/source ZIP, raw JUnit/lint/build/source/signature receipts, source preservation, fixture pack and owner-only APK delivery proof. Drive permissions show only the verified owner; streamed download matches exact APK bytes/SHA256. Private media, binaries and account information stay outside Git. Documentation commits do not change canonical build inputs.

Four themes, original metadata/URLs/Details, source art sizing/Fit containment, mood caption lanes, single-owner player transition, Search input ownership, shelf/pager recovery, classic/native, local music, queue/lyrics/offline, account/consent/recovery and signing remain. Source artwork limitations remain. Real rendering/contrast/large-text/TalkBack, audio/transport/restart, retained consent/data, performance/soak/recovery and release acceptance are pending. **No 9.5-10 score is claimed. Device acceptance NOT_RUN; public release NO-GO. Next independent source batch: NW135.**
