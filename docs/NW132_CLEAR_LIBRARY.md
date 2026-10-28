# NW132 — clear Library and collection identity

28 October 2026. Branch `update/nightwave-nw132-clear-library`; protected NW131 parent `e80378f1e5c0bc1f81970afd5d4a70c9ce42f2d9` (PR #135). Implementation through `26cd79236662c3b20f3e2961b3066ed232f44118` (initial source `d85be670a4c82a07b9ebfbfe12ea47239776bcb8`). Exact build and review evidence: [TEST_REPORT](../TEST_REPORT.md).

## Listener-visible change

Library distinguishes liked songs, playlists, listening history, automatic collections and imported offline music. A history-only listener sees actual history, a concise explanation of how to keep favorites, and the existing Create/Find/Offline actions. The empty liked tile, Recently liked heading and duplicate empty-track prompt no longer fill that overview. Empty collections remain actionable when opened directly.

History counts distinguish stored events from distinct canonical songs. Liked counts and visible liked/history rows use distinct original canonical references, without rewriting or deleting stored data. Automatic collections use Collection songs, not Saved tracks. Their descriptions count selected songs actually present in likes, playlists and history; these memberships overlap and are not additive totals. Metadata-derived creator group titles remain source metadata, not verified artists. No new genre, offline-ready or provider recommendation claim is made.

Automatic collections are behind a Show/Hide disclosure, preserving every generated destination. Their explanations prepare source-membership sets once per library update and are remembered using real source revisions, rather than scanning every playlist on each player update. This is a caching contract, not measured phone performance.

New playlist remains visible in the adaptive primary action group. Library options contains the existing playlist import, folder organization and management actions. Playlist options keeps Share, Export, Pin, Change folder and the existing Rename/Delete manager, replacing four tall tool rows and a duplicate Manage button. Existing folder/pin state remains visible. Opening or dismissing a menu performs no edit, starts no player and opens no picker.

“Search playlists or their songs” now inspects all 300 supported songs per playlist, matching the existing writer capacity instead of hiding songs after item 100. This is a bounded view filter; it does not change persistence limits, order or membership. Bulk selection rejects malformed/noncanonical public references even if they appear in an old/restored selection, while preserving the existing 20-song bound and valid selections hidden by a filter. A prior cap-test fixture now uses valid canonical IDs; the cap assertion and all existing tests remain.

## Preserved behavior and findings

Existing native create/rename/delete, pin/folder stores, temporary view sorts, original song URLs, exact bounded bulk remove/copy/reorder and post-edit snapshot-guarded Undo remain. Actual removal and deletion confirmations remain in the Activity. The composable tests inject in-memory callbacks; they do not prove a real confirmation dialog or preference write.

The system document picker still exports/imports the existing public-link text format. Cancellation returns without a write, reads are bounded/strict UTF-8, malformed text is rejected, and valid imports require preview/explicit confirmation. Name conflicts create a Shared-suffixed playlist rather than overwriting. No audio bytes, private accounts, tokens or backups enter this format. Native picker/provider, process recreation, restart persistence and data-preserving upgrade remain physical acceptance tasks.

Addresses Q07/Q11/Q12/Q13 at source level. The broader Q24 You/advanced-diagnostic disclosure remains follow-up work under NW143; it is not claimed fixed by this Library batch. Playback/offline engines and workers, consent/Google/backup boundaries, manifests, dependencies, signing identity, schemas and original 139-case matrix are unchanged. Repository-only publication controls from PR #134 remain; visibility is not changed by this batch.

## Executed and compiled verification

Fourteen new executed JVM methods exercise the actual presentation/browsing/selection/mutation/document policies. Cases cover empty/history-only/mature membership, invalid/duplicate rows, original metadata, overlapping provenance, prepared-label refresh, item 300 and unsupported overflow bounds, create/rename/folder/pin/view sort, exact repeated document import, conflict-safe import, malformed input, hidden selection remove/Undo and stale remove/reorder/Undo rejection. Existing storage/parser/engine suites remain. These are synthetic immutable-policy journeys, not an actual Android store/restart.

Five new actual StudioLibrary Android cases compile: history-only overview/Create; automatic history origin; Library import-menu dispatch; exact selected playlist export dispatch; and filtered-hidden selection with the real mutation/Undo policies in a disposable in-memory harness. No actual player, account, installed data or system picker is accessed. Device execution **NOT_RUN**.

The current runner includes `verify_nw132_library.py --self-test`, with broken caller/origin/export/Undo/document/browse/selection fixtures. Existing Search/Kotlin screen-isolation guards now follow StudioLibrary's internal visibility for real-screen tests. NW59 retains its bounded-scan check using the existing 300-song writer capacity. No safeguards, assertions, warnings or baselines are suppressed. See the current report for exact totals, digest and lint result.

## Required phone acceptance

All four NW132 acceptance rows remain **PARTIAL**, device **NOT_RUN**:

1. Attribute the installed build and signer before any permitted in-place update. Review new, history-only, liked-only, empty-playlist and mature libraries; compare stored events, unique songs, saved membership and derived-group descriptions against the fixture.
2. Open Create/Import/options/folder/sort/rename/pin from the real screen. Check 320/360/412dp, 1×/2×/3× text, themes, missing/bright art, TalkBack/Switch Access and reachable 48dp targets. Verify saved membership/order across Back/recreation/restart, with no unintended playback.
3. Select rows across filters, verify hidden counts and the 20-song cap, remove/cancel/copy/reorder/Undo, inject a stale edit, and compare all unrelated collections/history/likes/audio before and after.
4. Export/import through a disposable system document provider; test cancel, unavailable/zero-read provider, malformed/truncated/oversized input, Bangla text, name conflict and preview cancellation. Actual audio/private data must remain excluded and original sources preserved.

No Android device was connected; no install/reset/account/data operation occurred. The 219-case pack is prepared only. The provisional product score stays **6.7/10**, and app release remains **NO-GO** with prior physical/recovery/performance/rights gates carried forward. Source-only publication is a separate decision. Next source batch: **NW133 — player, artwork and metadata finish**.
