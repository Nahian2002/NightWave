# Current verification - DESIGN04 editorial and gesture finish

29 October 2026. [Draft PR #148](https://github.com/Nahian2002/NightWave/pull/148), `design/nightwave-editorial-finish`, stacked on #147. Protected parent `e7890f957fbb96c0b70a8d5dd69ea498eb9b37c6`; implementation `684cf0cbf0ec543b1037a9431056871e97d9bcce`. [Architecture, recorded baseline and preservation](docs/design/EDITORIAL_FINISH.md), [24-case manual supplement](docs/design/EDITORIAL_FINISH_ACCEPTANCE.csv). [Previous verification](https://github.com/Nahian2002/NightWave/blob/e7890f957fbb96c0b70a8d5dd69ea498eb9b37c6/TEST_REPORT.md) remains historical evidence.

The latest owner recording (002945) supplies a subjective **8.3/10 visible-UI score**: identity 8.5, color 9, hierarchy 8, coherence 8, legibility 8. Larger artwork and mood containment are improved in that sample; wide-art backplates, promotional captions and repetitive Home emphasis remain. Partial Home/Search frames recover and include touches; they do not prove an idle freeze. The recording does not identify an exact binary. This is the pre-change review, not a score for build 24139708. The original 6.7 whole-product audit remains frozen.

The player now sizes its frame using actual resolved-source proportions while retaining complete-edge Fit containment and same-source guards. Music UI conservatively shortens clear promotional captions; a visible Details dialog exposes complete original title/creator/source. Core MusicIdentity and Videos mode remain unchanged. Home gives one mix emphasis, differentiates real purposes and reports only facts available for its unique tracks. Shelf touch ownership prevents the outer pager from acquiring a held shelf gesture and releases on up/cancellation/disposal; idle recovery waits for the touch. Physical gesture/rendering acceptance remains separate.

| Executed or compiled check | Result and limit |
| --- | --- |
| Complete Windows builder | PASS: production Android/Compose, debug APK and instrumentation-source compilation. |
| JVM suite | **1,649 tests / 207 suites**, zero failures/errors/skips. Seventeen new methods cover title/edition/Bangla safety, mix facts/purpose, art geometry and balanced gesture owners. |
| Current source runner | **65/65 PASS**. Ten new broken caller fixtures; prior checks retained. |
| Android theme/layout matrix | **13 x 36 = 468 compiled cases, NOT_RUN**. Existing methods additionally check source-aware frame geometry, full original Details and press/release ownership before shelf edges, page swipes and theme changes. Stronger assertions do not increase executed cases. |
| Sequential lint | **0 errors / 313 warnings / 17 hints**, unchanged. No suppression/baseline added. |
| Feature preservation | **234 protected nonpresentation source/schema/build files byte-identical**; nine callback inventories, exact six shortcut definitions and core MusicIdentity retained. Installed data is not certified by this comparison. |
| Signer | PASS against protected delivered 006E APK; same certificate and package. No installed-phone comparison or installation. |
| Acceptance | Fresh source/APK-bound 219-case fixture pack PREPARED_NOT_EXECUTED. New 24-case four-theme supplement NOT_RUN; previous matrices retained. |
| Device/publication | Current adb inventory has no connected device/emulator. Actions remains disabled; no public binary, release, install/reset or account/data operation. No hosted CI pass claimed. |

| Artifact identity | Value |
| --- | --- |
| Build ID | `NW-debug-241397081D965B18` |
| Canonical source SHA256 | `241397081D965B1863FE75599F6CCFE189C2192B2364803DE13863AD709D6C02` |
| Source inputs | `879` |
| APK SHA256 | `CFE5B6C6F65E5ACFA8C40C4EF2F2C004FA6E59DD626AD75C32087D0548313A14` |
| APK bytes | `28,999,287` |
| Embedded revision context | `684cf0cbf0ec543b1037a9431056871e97d9bcce` |
| Separate NW08 manifest SHA256 | `C1FECDFCA304587AC6E5F15DD79066E42937B9BDB51167CE49FA5F6476A2D0D9`; retained independent encoding/ordering contract |
| App identity | `com.nightwave.player`, `2.0.0-alpha5s`, code35; min26/compile-target36 unchanged |
| Signing certificate SHA256 | `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394` |

Private checkpoint `C:\Users\jayed\NightWave-checkpoints\20261029-editorial-finish` retains protected recovery, raw receipts, source/APK evidence and owner-only delivery verification. Recordings, frames, accounts, credentials and binaries remain outside Git. Documentation-only follow-up does not alter canonical build inputs.

All four themes, every feature, source URLs and stored metadata, Search modes/prefill ownership, complete mood art/caption lanes, normal/error roles, native/classic choice, queues, playlists/library, local music, account/consent, recovery, dependencies and package/signing identity remain. Phone acceptance must confirm art, long/edition titles and Details, mix actions, shelf hold/release/cancellation, ordinary page/vertical scrolling, rapid/reselected tabs, theme/rotation/window/font combinations and accessibility. Earlier data/audio/performance/recovery/release gates carry forward. **No post-change 9.5-10 rating is claimed. Device acceptance NOT_RUN; public release NO-GO. NW134 remains next.**
