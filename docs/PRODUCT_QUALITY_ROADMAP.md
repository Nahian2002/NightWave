# Active NightWave quality roadmap — NW126–NW145

Established **28 October 2026**, after the owner's post-NW125 recording review. **This is the active plan for future upgrade batches.** NW111–NW125 implementation history is retained in [the previous roadmap](NW111_NW125_SPOTIFY_QUALITY_ROADMAP.md); unresolved device/release gates carry forward.

## Start here in every future chat

1. Read [the detailed audit and scorecard](audits/2026-10-28_PRODUCT_QUALITY_AUDIT.md), [the structured batch backlog](quality/NW126_NW145_BACKLOG.json), and [current verification](../TEST_REPORT.md).
2. Confirm the actual branch, newest cumulative remote and latest batch status. The original audit baseline was NW125 `61703c98238e762e59c049b6690853431f822884`. `main` now holds reviewed cumulative source after public-source promotion; NW133 is locally verified on `update/nightwave-nw133-player-artwork`, pending review/merge.
3. **Next source batch: NW134.** NW126–NW133 are locally verified with device acceptance pending. On "next", select the first eligible incomplete batch, respecting dependencies and carrying pending phone checks visibly.
4. Update backlog status/evidence, this roadmap's progress ledger and the current test report after each batch. A source implementation is not device acceptance. Re-rate only with fresh product evidence.

Root [AGENTS.md](../AGENTS.md) makes these continuity instructions discoverable to future coding sessions. The roadmap is persisted in Git/GitHub; it is not a background automation or a promise that a future model remembers private chat text.

## Outcome and product boundaries

Current provisional score is **6.7/10** on the fixed, weighted [16-area rubric](audits/2026-10-28_PRODUCT_QUALITY_AUDIT.md#scorecard). The strongest area is Now Playing at 8.0; visual identity and account/personalization are around 7.5. Compactness, result/source identity, offline convenience and lyric availability are the main gaps.

The goal is **9.5+ for a coherent, original NightWave music player**: immediate and predictable listening, readable compact screens, useful collections, complete local search, trustworthy offline readiness, capable lyrics, accessible controls and tested recovery. Preserve the midnight identity, owned/local-first strengths, optional account and manual control over consequential changes.

Spotify provides a task-quality reference. Its commercial catalog, artist/album identity systems, recommendation infrastructure, cross-device network and collaboration services require separate product/provider work. Those cannot be created through UI polish or assumed permission. No placeholder social, Connect, lossless, AI recommendation or lyric-provider claim belongs in the core app.

## Design direction

- **Home:** a purposeful Continue state, concise real shelves and useful personal collections. Recent song options belong in overflow/edit mode. Empty listeners get one next action rather than several empty dashboard cards.
- **Search:** input first; useful saved matches immediately; clear Local / Public video / Supported catalog provenance. Keep explicit provider fallback. Distinguish versions with actual duration/source evidence and preserve each original selectable URL. Index the entire supported local corpus asynchronously; don't hide songs behind the current 350/400-row caller caps.
- **Library:** one predictable place for liked songs, playlists, recent history and owned offline music. Separate intentional Save membership from listening history. Keep a visible Create action and compact, meaningful filters.
- **Player:** preserve the strong play/pause center, stable transport geometry and readable title/creator. Improve source-aware art and queue/lyrics entry points; keep the selected song stable during loading, route changes and transitions.
- **Offline:** the same navigation and visual language, a direct import path, explicit source capability, per-item progress/readiness and recovery. A saved reference is not an offline audio file. Genuine resume retains safe partial bytes only where the source/server contract supports it.
- **You/settings:** profile, appearance, sound, recovery and Help. Place Room/storage preview and detailed diagnostic tooling behind an explicit advanced disclosure while preserving their recovery purpose.
- **Compact means less duplication and clearer hierarchy.** Keep minimum 48dp independent touch targets; preserve system font scaling, readable supporting text and reflow. Do not achieve density by shrinking all controls or truncating every title.

## Ordered implementation batches

Detailed scope, dependencies, finding IDs, source areas and four acceptance criteria per batch are authoritative in [the backlog](quality/NW126_NW145_BACKLOG.json). [The acceptance CSV](quality/NW126_NW145_ACCEPTANCE.csv) starts all new cases NOT_RUN; the original 139 cases remain untouched.

| Batch | Priority | Delivery | Depends on | Required outcome |
| --- | --- | --- | --- | --- |
| NW126 | P0 | Reproducible baseline and build identity | — | Exact APK/source/device attribution, executable gates, mapped old/new acceptance and a safe test fixture. |
| NW127 | P1 | Compact visual system and hierarchy | NW126 | Shared density/type/spacing contract, default/narrow/large-text comparison and readable card overlays. |
| NW128 | P1 | Unified shell and offline navigation | NW127 | Coherent offline/catalog/download routes, predictable Back and persistent selected-song controls. |
| NW129 | P1 | Complete local search corpus | NW126, NW128 | Find items beyond 350/400 rows and 25 playlists, with async snapshots, cancellation and measured updates. |
| NW130 | P1 | Decidable public music results | NW129 | Source/duration/version evidence, preserved URLs, correct Bangla/alias/repeated-word ranking. |
| NW131 | P1 | Home that earns its screen space | NW127, NW130 | Distinct real shelves, useful empty state, compact recent actions and truthful mix descriptions. |
| NW132 | P1 | Clear Library and collection identity | NW128, NW129 | Save versus Recent clarity, stable Create/filter paths, tested playlist/bulk/Undo and document roundtrip. |
| NW133 | P1 | Player, artwork and metadata finish | NW127, NW130 | Best-available legitimate art, stable controls, clean identity and explicit loading/secondary paths. |
| NW134 | P1 | Queue and transport acceptance | NW132, NW133 | Multi-item Play Next/reorder/remove/Undo, shuffle history, restart and external transport verified. |
| NW135 | P1 | Offline import/readiness journey | NW128, NW132 | Owned/eligible music import to verified ready state, airplane-mode playback and truthful repair states. |
| NW136 | P1 | Resumable, recoverable transfers | NW135 | Safe eligible-source partial retention, validated Range/ETag behavior and process/storage/network recovery. |
| NW137 | P1 | Useful user-owned lyrics | NW133, NW135 | Guided LRC import and readable synchronized view tied to a stable selected source identity. |
| NW138 | P1 | Sound Studio active/draft clarity | NW133, NW134 | Simple presets plus explicit advanced bands, sticky Apply/readback and real route A/B evidence. |
| NW139 | P1 | Complete optional recovery journey | NW126, NW132 | Disposable live backup/readback/preview/restore/merge, precise access/busy status and smaller account UI. |
| NW140 | P1 | Measured Android performance | NW129, NW134, NW136 | ART startup/search/next, jank, memory and battery receipts; baseline profile/IO work justified by traces. |
| NW141 | P1 | Accessibility and Bangla/English | NW127, NW133, NW137, NW138, NW139 | Real TalkBack/Switch Access, font/contrast acceptance and resource-based bilingual primary flows. |
| NW142 | P1 | Adaptive layouts and input | NW128, NW141 | Narrow, landscape, tablet/split-screen, keyboard and insets behave without unreachable controls. |
| NW143 | P1 | First-run, failures and trust | NW135, NW136, NW139, NW141 | Clear onboarding/recovery, understandable disabled states, privacy/support disclosures and reviewed data boundaries. |
| NW144 | P2 | Finish external controls and sharing | NW134, NW142, NW143 | Reliable existing notifications/widgets/available car controls and safe playlist/share roundtrips. |
| NW145 | P0 | Cross-device certification and re-rating | NW126–NW144 | All blocking cases closed, independent task study, long soaks and a justified final score/release decision. |

P2 extras cannot delay or replace the core listening fixes. Existing widgets/car integrations should be verified before expanding them. New cross-device/social/catalog services require a separately scoped, approved project and evidence; unused buttons do not count as feature delivery.

## Concrete 9.5 acceptance standard

These are **NightWave project targets**, not measured Spotify specifications. NW126 records the actual reference phones and establishes comparable runs; material changes to targets need a written reason and retained original baseline.

### Product and usability

- Same frozen weighted rubric scores at least 9.5, with no core listening category below 9.0 and no unresolved critical/high-severity task failure. An unknown critical dimension cannot be replaced by an optimistic rating.
- At least eight representative listeners, including Bangla/English and accessibility participants, perform the preregistered core tasks. Target at least 95% task completion without coaching; report sample size and every failure, not statistical certainty from a small study.
- Per-task navigation budgets are agreed before the redesign: once the song/collection is visible, Play, Like, Play Next, Queue and offline-status discovery should take at most three intentional taps. File pickers, consent and recovery confirmation keep necessary safety steps and receive their own budgets.
- Screenshots at 320/360/412dp, 1×/2×/3× type, landscape and a tablet/split-screen profile show reachable controls and intentional text wrapping. Default density shows more useful music content through less duplication; independent actions retain 48dp targets.
- Adopt measured contrast of at least 4.5:1 for ordinary required text, 3:1 for qualifying large text and 3:1 for meaningful control/state cues, with the applicable inactive/decorative exceptions. See W3C's [text contrast](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html) and [non-text contrast](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html); evaluate rendered composition and intended colors, not compressed-video pixels alone.
- Every primary flow has empty/loading/error/offline/unavailable/success states with an actionable next step. No invented artists, album identities, listening counts, readiness, cloud health or recommendation claims.

### Performance and playback

- On the declared flagship reference, project targets are cold fully drawn p95 ≤2s, warm resume p95 ≤300ms and 10,000-item local keystroke-to-correct-result p95 ≤250ms including debounce. Midrange targets are ≤3s, ≤500ms and ≤400ms respectively. Record at least 20 launches/type runs per condition and actual distributions; one recording cannot establish these.
- Warm local Next intent-to-audible-start p95 target ≤250ms over at least 100 transitions, with no wrong-track metadata. Separate normal Next, natural gapless transition, cold decode, output route and provider/network wait. Do not infer sound timing from a progress counter.
- Frame-deadline misses target ≤1% across the defined core interactions at each device's actual refresh rate; no unexplained ≥100ms main-thread stalls. Keep raw Perfetto/frame evidence and profiler overhead context.
- A 60-minute playback test is required before cross-feature integration acceptance; the final candidate needs a two-hour mixed background/foreground/route test on both reference classes. No crash, ANR, unexpected stop, wrong queue transition or accumulating resource leak is acceptable.
- CPU/memory/battery comparisons use matched route, volume, power, network and temperature. A >10% relative regression against the same-device baseline requires investigation; do not turn one absolute battery-percent reading into a universal claim.

### Data, capability and release

- Installed signer matches before any approved in-place update; profiles, account boundaries, settings, queue, playlists/history and owned audio survive. No uninstall/data reset as an upgrade strategy.
- Authorized/imported audio completes verification, survives process stop and plays/seeks/skips in airplane mode. Missing/corrupt/unsupported files do not appear Ready. Retained partial files are source-bound, bounded, checked and cleaned safely; restart fallback is explicit.
- A disposable backup/readback/restore/merge run proves actual contents, stale-local/account/consent rejection and recovery after low storage/cancellation. Metadata backup and audio-file backup have distinct scope/status.
- Fresh complete Gradle JVM, Android/Compose, instrumentation-source, APK/signer, lint and current source checks use the same final source; required device tests actually execute. Warnings are triaged, not globally suppressed. Hosted infrastructure failure remains separate from local results.
- Applicable privacy, security/dependency/history, provider/source and distribution approvals are recorded. Public release stays NO-GO while required gates remain open. The 9.5 product target does not waive these conditions.

## Definition of done for a batch

Record: exact protected parent and commit; user-visible before/after; relevant Q finding IDs; changed files and preserved data contracts; meaningful deterministic positive/negative tests; actual local build/source/lint/signature receipts where affected; required device/task evidence and what remains unrun; rollback checkpoint; PR; updated next batch. Keep code completion and physical acceptance as separate fields.

Each batch should be a reviewable change with a clear user problem. Refactor screen/state/repository seams incrementally, especially the large Studio Activity; avoid a simultaneous navigation, persistence and player-engine rewrite. Do not delete linked history, schemas or checks as cosmetic cleanup.

## Progress ledger

| Date | Work | Status | Evidence / remaining |
| --- | --- | --- | --- |
| 2026-10-28 | Post-NW125 visual/source audit and next plan | AUDIT_COMPLETE; IMPLEMENTATION_NOT_STARTED | Entire recording sampled at 1s, 16-area rubric, 32 findings, 20 batches and 80 new acceptance cases. Q30 tooling parse error repaired; NW125 gate modes added to current runner. Fresh build: 1,484 JVM tests/187 suites and 52 checks pass. Physical acceptance/re-rating remains pending. |
| 2026-10-28 | NW126 build identity and acceptance baseline | LOCAL_VERIFIED / DEVICE_NOT_RUN | Packaged source ID, installed APK/certificate support receipt, independent promotion gate, 219-case disposable fixture plan; 1,496 JVM tests and 54 checks pass. See [NW126 evidence](NW126_BUILD_IDENTITY_BASELINE.md). |
| 2026-10-28 | NW127 compact visual hierarchy | LOCAL_VERIFIED / DEVICE_NOT_RUN | Shared density/reflow contract, single Continue, recent overflow, opaque captions and stable Library Create; 1,502 JVM tests / 189 suites and 54 checks pass. See [NW127 evidence](NW127_COMPACT_VISUAL_HIERARCHY.md); full-screen/physical acceptance pending. |
| 2026-10-28 | NW128 shared offline shell and navigation | LOCAL_VERIFIED / DEVICE_NOT_RUN | Four retained offline screens share navigation/selected-song controls, bounded main handoff, browsing/draft restoration and native recovery; 1,515 JVM tests and 55 source checks pass. See [NW128 evidence](NW128_UNIFIED_OFFLINE_SHELL.md); 30 real route/Back cycles, picker/recreation and audible acceptance pending. |
| 2026-10-28 | NW129 complete local search corpus | LOCAL_VERIFIED / DEVICE_NOT_RUN | Complete observed primary/offline snapshots, prepared background matching, synchronous freshness/provenance and result paging; 1,532 JVM tests and 56 source checks pass. Two final host runs measured 10k public/5k offline metadata. See [NW129 evidence](NW129_COMPLETE_LOCAL_SEARCH.md); actual screen/Android timing/heap acceptance pending. |
| 2026-10-28 | NW130 decidable public music results | LOCAL_VERIFIED / DEVICE_NOT_RUN | Source/uploader/duration/title labels and collision IDs, full selectable versions, bounded cancellable HTTP and phase/retry/fallback; 1,546 JVM tests / 195 suites, 57 source checks and frozen 36-case corpus with zero top errors. Implementation `09114486009742d2fdef2d0af2a037e5405d5e37`, [PR #133](https://github.com/Nahian2002/NightWave/pull/133). See [NW130 evidence](NW130_DECIDABLE_PUBLIC_RESULTS.md); actual UI/source selection/provider/device acceptance pending. |
| 2026-10-28 | NW131 purposeful Home | LOCAL_VERIFIED / DEVICE_NOT_RUN | Single real primary action, truthful local mix captions/explanations, distinct shelves and compact reflow; 1,559 JVM tests / 196 suites and 58 source checks pass. Implementation `f2267fa4d4cd24fccf49228d6823527f08a479e5`. [PR #135](https://github.com/Nahian2002/NightWave/pull/135). See [NW131 evidence](NW131_PURPOSEFUL_HOME.md); phone rendering, source/queue, overflow/recovery and audible acceptance pending. |
| 2026-10-28 | NW132 clear Library and collection identity | LOCAL_VERIFIED / DEVICE_NOT_RUN | Truthful saved/history/automatic origins, compact options, complete 300-song playlist contents filter and canonical bounded selection; 1,573 JVM tests / 197 suites and 59 source checks pass. Implementation `26cd79236662c3b20f3e2961b3066ed232f44118`. [PR #136](https://github.com/Nahian2002/NightWave/pull/136). See [NW132 evidence](NW132_CLEAR_LIBRARY.md); actual screen/store/picker/restart/recovery acceptance pending. |
| 2026-10-28 | NW132 maintenance and cleanup | LOCAL_VERIFIED / DEVICE_NOT_RUN | Library collection scroll repair; active information refresh; 354 disposable files / 60.79 MiB removed with checkpoint protection; 1,573 JVM tests and 59 source checks pass. [PR #137](https://github.com/Nahian2002/NightWave/pull/137); see [maintenance evidence](REPOSITORY_MAINTENANCE_20261028.md). This is not NW133. |
| 2026-10-28 | NW133 player, artwork and metadata finish | LOCAL_VERIFIED / DEVICE_NOT_RUN | Measured artwork requests with bounded same-source fallback, shared status, clear Save and identity-bound seeking; 1,587 JVM tests / 198 suites and 60 source checks pass. Implementation `d2828b9dd1e90a33e2d608b3a3b8289e25b8d54e`. [PR #144](https://github.com/Nahian2002/NightWave/pull/144). See [NW133 evidence](NW133_PLAYER_ARTWORK_METADATA.md); actual art/cache/geometry/touch/notification/audible acceptance remains pending. |
| 2026-10-28 | DESIGN01 owner-requested matte redesign | LOCAL_VERIFIED / DEVICE_NOT_RUN | Coordinated four-theme surfaces and actions, adaptive Home, selected-art player atmosphere and native styling; see [design system](design/MATTE_DESIGN_SYSTEM.md) and its separate acceptance ledger. NW134 is not claimed implemented. |
| 2026-10-28 | DESIGN02 expressive theme finish | LOCAL_VERIFIED / DEVICE_NOT_RUN | Recorded Midnight theme rated 6.0/10 on a theme-only rubric; differentiated palettes, colored control/section roles, four native dialog styles and bounded singleton art. [Review](design/2026-10-28_THEME_RECORDING_REVIEW.md), [implementation](design/EXPRESSIVE_THEME_FINISH.md), [PR #146](https://github.com/Nahian2002/NightWave/pull/146). Initial correction awaiting the subsequent owner review recorded below. |
| 2026-10-28 | DESIGN02 attributable owner review and boundary fix | LOCAL_VERIFIED / PARTIAL_RECORDED_D5 / NEW_DEVICE_NOT_RUN | Primary Midnight theme 7.6/10 versus 6.0 on the same rubric; all four choices briefly observed, compact singleton confirmed. Material container roles and API29+ navigation scrim corrected in B017. [Review and limits](design/2026-10-28_THEME_VALIDATION_202916.md). Overall 6.7 unchanged; formal device matrix unrun. |
| 2026-10-28 | DESIGN02 four-theme composition and Search input follow-up | LOCAL_VERIFIED / NEW_DEVICE_NOT_RUN | [Recorded ratings and exact scope](design/2026-10-28_THEME_POLISH_211137.md): Midnight/Aurora 7.6, Pearl/AMOLED 7.4. Flat rail, quieter player/actions/separators; explicit prefill tokens prevent stale query replay. 1,614 JVM tests / 201 suites, 62 source checks, Android/Compose and instrumentation-source compile pass; lint 0/313/17, signer retained. New phone acceptance pending; no overall re-rating. |
| 2026-10-28 | DESIGN02 mood containment and composition finish | LOCAL_VERIFIED / NEW_DEVICE_NOT_RUN | [Latest recorded review](design/2026-10-28_MOOD_THEME_FINISH_230959.md): 8.2 appearance only. Separate art/caption lanes, six complete bounded motifs and one selection cue; 1,620 JVM tests / 202 suites, 62 source checks, Android/Compose/instrumentation-source compile and signer pass; lint 0/313/17. Phone acceptance pending; original overall score unchanged. |
| 2026-10-28 | DESIGN03 complete content hierarchy and artwork finish | LOCAL_VERIFIED / DEVICE_NOT_RUN | Owner authorized all six recommendations from the latest 8.4 appearance review. [Architecture and preservation](design/CONTENT_HIERARCHY_FINISH.md), [96-case supplement](design/CONTENT_FINISH_ACCEPTANCE.csv). [PR #147](https://github.com/Nahian2002/NightWave/pull/147): 1,625 JVM tests / 203 suites, 63 source checks, Android/Compose and 432 instrumentation cases compiled; lint 0/313/17, signer retained. Music-first Search, bounded Home art, source-aware previews, concise copy, consistent roles and snapping/spacing. New phone acceptance pending. |
| 2026-10-29 | DESIGN03 artwork/pager acceptance correction | LOCAL_VERIFIED / DEVICE_NOT_RUN | [Owner regression review](design/2026-10-29_ARTWORK_PAGER_REVIEW.md): 7.8 recorded UI, tiny images and split destinations. Fit artwork, balanced mosaics, shelf boundaries, settled/reselected tabs and idle recovery. 1,632 JVM tests / 204 suites, 64 source checks, 468 Android cases compiled only; lint 0/313/17, signer retained. PR #147; new phone validation pending. |
| Next | NW134 queue and transport acceptance | PLANNED | Continue the cumulative `design/nightwave-content-finish` branch after DESIGN03 and carry all earlier physical/release blockers forward. Actions remains disabled; no APK publication. |

All NW126–NW145 acceptance rows originally began NOT_RUN; the current CSV records executed evidence and remaining limits. Update this ledger and the structured backlog after real work. Do not interpret the audit's repaired gate or new documents as completion of twenty upgrade batches.
