# NW141 accessibility and bilingual primary flows

## Committed implementation plan — 30 October 2026

Protected cumulative parent: `6f767c92452a22394396081c9c47b17863cc393c` (NW140 draft #162). Branch: `update/nightwave-nw141-accessibility`. Private verified baseline bundle and exact working-byte ZIP: `20261030-nw141-accessibility`, outside Git.

The owner explicitly requested starting this update now despite the scheduled five-hour admission estimate. Retain periodic allowance checks, the weekly 10% stop, safe recovery, complete validation and truthful pending evidence. This manual run does not lower the saved estimate for subsequent scheduled runs.

1. Introduce Android resource-backed English/Bangla primary interface copy, parameterized announcements, quantity resources and locale-aware numeric/time presentation. Preserve user text, Unicode, URLs, persistent keys, stored English identifiers and account/consent contracts. Resources follow the Android application locale; no new account or locale preference is needed.
2. Wire primary shell/Home/Search/Library/You, playback/queue, lyrics guidance, Sound Studio and account recovery actions into the shared resources. Retain every action, callback, theme and data path. Record any remaining technical/legacy copy explicitly rather than claiming complete application translation.
3. Improve heading/state/independent-action semantics, remove title truncation from accessible queue removal, retain 48dp independent controls and font-responsive layouts. Protect Bangla combining marks through platform font padding and script-appropriate spacing. Preserve reduced-motion static states.
4. Add resource parity/format/coverage regressions, locale/Unicode and actual Compose caller tests. Run the current checks and full Windows builder, then sequential lint. Compare notices, canonical changes, APK provenance and the actual prior signer. Keep Android execution and rendered contrast/TalkBack/Switch Access/1x–3x/all-theme acceptance NOT_RUN until performed.
5. Save exact identities, private recovery and owner-only APK with byte/hash readback, cumulative draft PR and next NW142. No install/reset/uninstall, account action, merge, Actions/settings change or public binary. No new product rating follows from builds or translation counts.

## Acceptance limits

Source/local verification, Android compilation and physical acceptance are separate. The original NW141 cases and prior physical/performance/recovery gates remain open. Native provider errors, raw diagnostic details and user/provider-authored content must retain their original meaning and identity.

## Locally verified result

Primary Compose interface resources and accessibility readiness are complete locally. This is not full-application translation or physical certification. The device/application resource locale selects English or Bangla automatically; unsupported locales retain the complete English fallback. No new persisted language setting was introduced.

- Added 442 string resources and four quantity resources with matched Bangla counterparts; the six previous resources also have Bangla counterparts. Primary shell, search, collection actions/counts, player/queue, lyrics guidance/follow, EQ actions and recovery controls use Android resources. Parameter order belongs to the translator; presentation-only digits, durations and account dates use the active resource locale.
- Full queue/remove and lyric announcements retain complete Unicode titles/cues. Navigation keeps its Tab role and selected state without adding an English “tab” suffix. Search, Library and reusable section headings expose heading semantics. Queue numbering can grow rather than being squeezed into a fixed width. Existing independent row/options/remove, bounded EQ steps, track-scoped seek and static reduced-motion states remain.
- Bangla typography uses neutral tracking, platform font padding and untrimmed line extents. English metrics and all four palettes are retained. This prepares glyph/layout behavior; only actual rendering can prove no clipping or contrast failures.
- Old literal-copy checks now inspect the actual resource-backed caller. Their original negative fixtures remain; the new checker rejects missing resources in both locales, parity/placeholder/quantity corruption and missing actual caller fences. Fixed a Windows checker-output Unicode failure so one failed check cannot prevent later results from being reported.

## Exact validation and artifact

Protected parent `6f767c92452a22394396081c9c47b17863cc393c`; plan `7c454a77`; implementation `959a4efcf9b94882b56107fa6bd2efa237b353a6`; quantity/reference repair and actual build revision `ba709e2ea5b6a00e8c43c773835430b0399566a2`.

| Check | Result |
| --- | --- |
| Full Windows builder | PASS: Android/Compose/APK and Android instrumentation sources |
| JVM | 1,808 tests / 223 suites; zero failures/errors/skips; five new locale/duration boundary tests |
| Current source checks | 82 PASS_SOURCE_ONLY; eleven new invalid resource/caller fixtures rejected |
| Android UI tests | Four new actual-component/resource methods compile only; NOT_RUN |
| Sequential lint | 0 errors / 322 warnings / 17 hints; identical ID/severity/message/path inventory to NW140, ignoring line shifts; no suppression |
| Preservation | 332 prior app/src/main files byte-identical; only twelve prior UI files changed. Native Activity/account prefix unchanged except R import; engines, sources, stores, build/dependency/manifest/package/signer contracts unchanged |
| Secrets | Implementation-stage all-ref Gitleaks 3,574 commits; no leaks, reviewed exclusions unchanged |
| Device gates | Original 219-case pack prepared for this artifact; ten new cases and every prior physical/performance/account gate NOT_RUN |

Build `NW-debug-C4D430224921F123`; canonical source `C4D430224921F123F14D2E5ED80E7E9724D82242BC763F103DD46CF77D227C35`, 951 inputs. APK SHA256 `AC566E55078292380D3EB6405EDF5AB5BF6D77E1A99C0436F54C21FBAAA46F4C`, 33,844,842 bytes. Package `com.nightwave.player`, version `2.0.0-alpha5s` / code35. Signer SHA256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`, verified against the actual NW140 APK. Generated dex/resources change, and promoted APK equals the actual Gradle output; source identity is independently verified.

Owner-only Drive permissions and streamed remote size/SHA readback match the private APK. Private baseline/final bundles, working-byte ZIPs, JUnit/lint/build/signature/provenance/source/delivery receipts are retained outside Git. The initial missing song-quantity compilation failure is retained and repaired with a regression that catches missing referenced resources even when absent from both locales; only the final successful artifact is delivered.

## Remaining acceptance and next batch

[Ten supplemental physical cases](quality/NW141_ACCESSIBILITY_ACCEPTANCE.csv), original NW141 cases and all prior gates remain NOT_RUN. Follow [the device runbook](NW141_DEVICE_RUNBOOK.md). Actual TalkBack/Switch Access, all-theme 1x/2x/3x layout/contrast/touch bounds, locale recreation, native-speaker review and installed-data/signer continuity are required. No fresh visual rating or 9.5 certification is inferred.

Legacy native import/recovery previews, advanced diagnostic policy details, source/provider messages and user-authored content retain their original text/meaning. This batch does not claim complete application translation. Carry the native/failure-copy review into NW143 and the actual responsive layout work into NW142. No feature, theme, source URL/quality, data format or account boundary was removed or hidden.

**Next independent source batch: NW142**, then NW143–NW145. Source work is separate from physical acceptance. No automatic device install/reset/uninstall, account action, merge, Actions/settings change or public binary occurred.

Implementation references: [Android localization](https://developer.android.com/guide/topics/resources/localization), [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics), [paragraph/glyph spacing](https://developer.android.com/develop/ui/compose/text/style-paragraph). These inform implementation; they do not supply physical acceptance evidence.
