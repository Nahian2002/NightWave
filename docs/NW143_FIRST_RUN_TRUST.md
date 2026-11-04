# NW143 — First run, failures and trust

Owner started this batch on 4 November 2026 from cumulative reopen fix #165. Preserve all features, themes, sources/quality, data, identity and account/consent boundaries. Baseline private bundle is outside Git. Version37 phone acceptance is pending independently; do not restart the automation or activate the stable feed.

Plan: extend the existing first-use Home hero rather than add redundant empty cards. Explain public search versus imported local audio and optional Google backup; retain direct Search/Offline callbacks. Review primary search/loading/empty/offline/failure/success and native import/recovery messages, retaining query/work and raw diagnostic access. Localize changed consumer copy in English/Bangla. Keep advanced storage, diagnostics, exports and recovery available, using understandable everyday labels. Review actual privacy/support data flows and document unresolved provider/distribution approvals without claiming legal clearance.

Before claiming completion: meaningful caller/resource regressions, current source checks, full Windows builder then sequential lint, exact source/APK/signer evidence and private verified delivery, draft PR, roadmap/backlog update. Physical state journeys, native-speaker review and all previous device gates remain NOT_RUN until executed. Next original batch NW144 only after NW143 source validation; no source-only 9.5 rating.

Status: LOCAL_VERIFIED source implementation; physical/release acceptance remains pending. Historical checkpoint below retains the state before this completion.

## State and trust review

| Journey | Existing behavior retained / targeted improvement | Runtime evidence |
| --- | --- | --- |
| First use / empty Home | Single hero remains; localized scope guidance and existing Find music / Offline callbacks. No forced sign-in or new persisted onboarding flag. | NOT_RUN |
| Search loading / saved matches | Existing request-owned loading, local corpus and cancellation retained. | NOT_RUN |
| Search failure / prior results | Localized next step; previous results and original retry query retained. Raw error and provider details remain in a scrollable explicit details dialog, including error-only cases. | NOT_RUN |
| Search empty / unsupported link / success | Existing no-match hint, supported-URL gate, individual recordings and selectable source callbacks retained. No provider or ranking changes. | NOT_RUN |
| Google permission cancelled | Localized explanation that local library is unchanged and Drive can be reconnected. Existing consent/activity-result boundary retained. | NOT_RUN |
| Backup import failure | Localized NightWave-backup retry guidance; existing transaction/validation and retained-library paths unchanged. | NOT_RUN |
| Recovery / diagnostics | Added bilingual Help and privacy entry in App health. Existing playback recovery, preview-before-share diagnostics, export, advanced storage and classic interface remain reachable. | NOT_RUN |

Static data-flow review: network/provider/search engines, account authorization, Drive backup formats and scope checks, crash filtering, storage stores, manifest permissions and feed trust key are unchanged by this batch. Crash summary still previews exactly the report supplied to the explicit share chooser. Consumer help explains public-source requests/network address, optional Drive and separate audio copies; it does not claim a provider catalog or guaranteed service availability. Existing SECURITY.md private reporting contact is reused, without changing its policy. PRIVACY_POLICY_DRAFT.md remains a draft; legal review, stable policy publication, provider/music-rights approvals, public binary/feed activation and installed-data evidence remain separate unresolved release gates. No privacy or release certification is implied by this source review. Existing inherited English diagnostics/user/provider text are preserved; full-app translation is not claimed.

Checkpoint: `96abf6e3` adds English/Bangla first-use hero guidance and clearer storage action copy. NW141 bilingual and NW131 Home positive/negative source regressions pass. Android compilation, full checks, lint, native/failure/trust review and APK delivery remain pending. This is not a completed NW143 release; retain code37 as the last verified delivery.

## NW143 verified source result — 4 November 2026

Source implementation locally verified. Clear first-use Home guidance, bilingual Help and privacy, resource-backed search failure/previous-results steps, error-only scrollable diagnostics, native import/permission/optional-restore copy and storage labels. All original actions, query/source/quality, data formats, themes, account/consent and recovery boundaries remain. Changed app inputs are the UI Activity, two localized resource files and version code38; engines/stores/manifest/dependencies/feed trust anchor unchanged.

Full Windows builder PASS; 1,817 JVM / 226 suites, zero failures/errors/skips; Android test-source compilation PASS. All 85 current checks PASS_SOURCE_ONLY, including new NW143 caller-negative fixtures. Sequential lint PASS: 0 errors / 324 warnings / 17 hints; unchanged ID/severity/message/path inventory. Gitleaks implementation-stage 3,597 commits, no leaks. Initial missing scroll import and two obsolete literal-label guards repaired; failed/interim logs retained privately. Final build revision `9438858e68d18dc2b1ea73fe9e57643fffad9d7b`.

Build **NW-debug-515F3474BA50AE1F**; canonical source **515F3474BA50AE1F2BC903CFE9BDDCB766734E7F21632C93E47647472E7FD82E**, 971 inputs. APK **61E0F43FC9B2CA5130EE9C199E368A1505B415AF1091A4844EE524FA8B033EAC**, 26,730,266 bytes. Package `com.nightwave.player`, version `2.0.0-alpha5s`, code38. Signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**, compared with the actual previous code37 APK. Owner-only Drive permission and complete remote byte/hash readback verified. Raw receipts and recovery remain outside Git.

Original 219-case pack PREPARED_NOT_EXECUTED for this APK. All new state journeys, native-speaker/large-font/all-theme review, physical cold reopening, seven-day/manual updater, live provider/Drive/permission/install/data-preservation and performance cases remain NOT_RUN. Full-app translation, legal/privacy/distribution clearance and a new 9.5/10 score are not claimed. Stable feed remains disabled; no main merge/public binary, device action, account operation, repository setting change or automation restart. Next independent source batch NW144; NW145 still requires actual device evidence.

[Draft NW143 review #166](https://github.com/Nahian2002/NightWave/pull/166), stacked on #165; unmerged.
