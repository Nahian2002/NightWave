# Upcoming NightWave updates

Replanned 29 October 2026 from the [latest owner recording review](../audits/2026-10-29_RECORDING_230548_REVIEW.md). The original NW126–NW145 IDs, audit, dependencies and physical cases remain evidence. This execution order adds focused follow-ups before the remaining original batches; it does not repeat a broad redesign.

## Order and target slots

| Order | Batch | Target slot | Reviewable outcome |
| --- | --- | --- | --- |
| 1 | FINISH02 | Start now, 29 Oct | Clean saved identity in Resume/options/Downloads; clear search and Show all; completed transfer duration/rate. |
| 2 | ART02 | 30 Oct | Existing saved artwork continuity using known identity and existing cached images, with bounded background work and deletion fences. |
| 3 | SEARCH02 | 31 Oct | Measured public-search phases, safe bounded result continuity, cancellation/retry and source/version preservation. |
| 4 | NW137 | 1 Nov | Guided owned-LRC import, explicit binding and readable synchronized lyrics. |
| 5 | NW138 | 2 Nov | Clear active versus draft sound, accessible advanced controls and reachable Apply. |
| 6 | NW139 | 3 Nov | Compact optional recovery and disposable backup/restore plan; live account operations require the owner's explicit scope. |
| 7 | NW140 | 4 Nov | Android measurement harness, trace-based improvements and real startup/search/playback evidence. |
| 8 | NW141 | 5 Nov | Accessibility and Bangla/English primary flows; actual TalkBack/font/contrast acceptance. |
| 9 | NW142 | 6 Nov | Narrow/large-text/landscape/tablet/input coverage and targeted layout fixes. |
| 10 | NW143 | 7 Nov | Clear first-run/failure/privacy/recovery journeys with retained capabilities. |
| 11 | NW144 | 8 Nov | Existing notification/widget/external controls and share roundtrips. |
| 12 | NW145 | After blocking evidence closes | Consolidated physical acceptance, task study, two-hour soaks and defensible re-rating. |

These are planned daily slots, not deadlines or automatic acceptance. One bounded eligible batch runs at a time; a build failure or unfinished batch moves the later slots. Independent source work can continue with recorded device checks pending. Phone/account gates wait for actual evidence and appropriate user authorization. No automatic install, reset, merge, public APK or repository setting change.

## Follow-up schedule

Codex heartbeat `nightwave-sequential-upgrades` is active in this chat, daily at **10:00 AM Asia/Dhaka**. This is the proposed default cadence while the owner's cadence question remains open. The local Codex app/host must be available. Each run reads the current backlog and checkout, skips overlapping active work, completes one eligible source batch and records its next step. Report completed work, meaningful failures or required input; remain quiet on unchanged state. The backlog carries the actual status; target dates never mark a batch complete.

## FINISH02 committed implementation plan

Protected parent: `c932daaa1b3ac2e3da87730b3d726659f1e74171`. Branch: `fix/nightwave-everyday-finish`, stacked on #155. Private checkpoint: `20261029-finish02` outside Git.

1. Reuse conservative known identity for Resume, saved download headings and offline dialogs/accessibility descriptions. Preserve original stored title, IDs, URLs, audio files, queue and cloud schemas; do not infer an artist from an uncorroborated title.
2. Add a persistent 48dp Clear search action while a query exists and a filtered-empty Show all action. Clear only local query/availability filters and reset paging; preserve songs, sorting, history and selection.
3. Persist a small numeric completion receipt on successful guarded transfers: network bytes and monotonic elapsed interval. Show duration and average network rate only after existing file verification says Ready. Exclude retained bytes, distinguish transfer from queue/import, omit unknown/legacy/reused receipts. Do not parse human diagnostic text or alter the successful transfer algorithm.
4. Add meaningful tests for unit conversion, invalid/zero/overflow inputs, resumed-byte semantics and filtered-state recovery, plus caller guards. Run the full builder, current source checks, then lint; verify signer/provenance/recovery and save a cumulative draft PR and owner-only APK.
5. Keep physical theme/font/input, fresh-versus-reused/resumed transfer, deletion, airplane-mode and previous acceptance cases NOT_RUN until performed. No score increase follows from test counts.

ART02, SEARCH02 and NW137 onward retain separate implementation plans and checks; unfinished artwork/search work is explicit rather than claimed by this first batch.
