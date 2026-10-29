# Current verification - NETWORK01 media transport and I/O

29 October 2026. [Draft PR #153](https://github.com/Nahian2002/NightWave/pull/153), `fix/nightwave-network-throughput`, stacked on NW135 #152. Protected parent `f11329b5e7eeec652dbb853f3d016ea725d220b5`; implementation `83a150098b768a3c89ff5491576e26fa13b8c469`, final built revision `2f61ec2f2f6d03fc36a90a979c28308501d370b5`. [Recording/architecture/evidence](docs/NETWORK_TRANSPORT_THROUGHPUT.md), [phone supplement](docs/quality/NETWORK_THROUGHPUT_ACCEPTANCE.csv), [previous verification](https://github.com/Nahian2002/NightWave/blob/f11329b5e7eeec652dbb853f3d016ea725d220b5/TEST_REPORT.md). NW136 genuine resumption remains PLANNED and next.

Recording 180628 shows roughly 14-19 KB/s during a searched-audio save and a concurrent Buffering audio indication. Sequential inspection decoded 7,567 frames through 134.4538s, with 135 one-second samples, overviews and details. Exact recorded build and actual phone line throughput are unverified; encoding rate is not app timing. These observations do not identify the limiting source/route/device/app. No fixed bandwidth cap was found in the inspected paths; NW134-to-NW135 did not alter their network transport.

Playback now uses a reusable HTTP/2-capable OkHttp client through the pinned Media3 adapter, with HTTP/1.1 fallback and existing local routing. Read-ahead is at most 120s with a 16 MiB allocation target and size priority; pinned 1s/2s startup/rebuffer defaults remain. Guarded public downloads reuse their client without cancelling other calls. Small reads accumulate in a 256 KiB output buffer, flush before fd.sync and retain original verification/commit order. Sequential local copy/hash buffers also increase. Source quality/selection, original identities, source/DNS/redirect/cancel/storage checks and all features remain.

| Executed or compiled check | Result and limit |
| --- | --- |
| Complete Windows builder | Final Android/Compose/APK/instrumentation-source build PASS. Initial passing candidate was superseded after verifying the pinned startup defaults; its log is retained privately. Final source rebuilt completely, not patched into an old APK. |
| JVM suite | **1,697 tests / 212 suites**, zero failures/errors/skips. Nine new network/I/O cases execute; previous regressions remain. |
| Network/I/O fixtures | Real loopback connection reuse, isolated cancellation and complete-body SHA readback PASS. Controlled 16 MiB in 8 KiB writes: 2,048 downstream writes become 64, identical SHA256. This is an operation-count fixture, not a measured phone/Internet speed multiplier. |
| Current source runner | **71/71 PASS**. Eight new broken-caller fixtures protect actual pooled transport/local routing/read-ahead/flush/cancel/source/audio wiring. |
| Android tests | One new DefaultDataSource local-file subrange/read/seek case compiles only. Earlier 612 theme, five NW135 import and three NW134 queue cases remain compilation only. No device execution. |
| Sequential lint/dependencies | **0 errors / 314 warnings / 17 hints**. One new warning is a newer-version notice for the adapter intentionally aligned with Media3 1.6.1. No suppression/baseline. Runtime report resolves adapter1.6.1 / OkHttp4.12.0. Existing Kotlin unreachable-description warning remains. |
| Source preservation | **230 previously protected files byte-identical**, four intentional transport/library/build changes and nine callback inventories retained. Gateway/bitrate selection and the exact public-address policy remain. Schemas, stored library/history/queue, themes, accounts/consent, providers and NW135 import checks remain. |
| Signer | PASS against delivered NW135 2D35C8B6 APK; same package/certificate. Installed-phone comparison NOT_RUN; no installation/reset. |
| Acceptance preparation | 219-case source/APK-bound fixture pack PREPARED_NOT_EXECUTED. Ten new physical cases and original NW134/NW135 acceptance remain NOT_RUN. |
| Device/hosted evidence | No connected adb device/emulator. No phone Wi-Fi/mobile A/B, actual server/CDN speed, audible rebuffer, route/background, heap/data/battery or soak run. Actions disabled; no hosted CI pass, public binary/release or merge. |

| Artifact identity | Value |
| --- | --- |
| Build ID | `NW-debug-3733A8B004D88749` |
| Canonical source SHA256 | `3733A8B004D887490F2A6241912256658385093D18E97B46D993CB41A289BB20` |
| Canonical inputs | `901` |
| APK SHA256 | `4682D5D43E4A6435386AAF835D9B5386FC20823D9E9B6B9F5D4C745BB7A39FD7` |
| APK bytes | `33,844,842` |
| Embedded revision | `2f61ec2f2f6d03fc36a90a979c28308501d370b5` |
| Separate NW08 manifest SHA256 | `1E530CBE24E366FB1BE8FA08735C221F3F58200AC89BBD1644952BB1872B6C05`; independent ordering/encoding retained |
| App identity | `com.nightwave.player`, `2.0.0-alpha5s`, code35; min26/compile-target36 unchanged |
| Certificate SHA256 | `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394` |

Private checkpoint `C:/Users/jayed/NightWave-checkpoints/20261029-network-throughput` retains recording frames, recovery/source ZIP, initial/final builder logs, raw JUnit/lint/dependencies, exact pinned constants, source preservation, fixtures, signer/provenance and owner-only APK delivery proof. Streamed Drive download matches the exact APK bytes/SHA; permissions show only the verified owner. Media, binaries, tokens and account details remain outside Git. Documentation follow-up does not change canonical inputs.

The only deliberate dependency addition is `media3-datasource-okhttp:1.6.1`, matching existing Media3 and declaring Apache-2.0 in its POM. Historical advisory/publication inventory remains historical; no dependency-security remediation is claimed. Original metadata/URLs/Details, all formats/features/themes, queue/recovery/consent, source restrictions and data schemas remain. No bitrate reduction, proxy, fixed-network binding or media disk cache is introduced. No new rating or guaranteed line-rate speed is inferred. **Phone throughput/audio/resource acceptance NOT_RUN; public release NO-GO. NW136 remains next.**
