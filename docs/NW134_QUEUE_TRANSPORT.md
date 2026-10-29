# NW134 - queue editing and transport acceptance

29 October 2026. [Draft PR #151](https://github.com/Nahian2002/NightWave/pull/151), cumulative branch `update/nightwave-nw134-queue-finish`, protected parent `b04248cd519a7dcc313e3a31443103c0e821a7be` (DESIGN06 / draft PR #150). See [current verification](../TEST_REPORT.md), [roadmap](PRODUCT_QUALITY_ROADMAP.md) and [focused phone supplement](quality/NW134_FINISH_ACCEPTANCE.csv). This batch resumes the original roadmap; it does not replace pending phone acceptance with a visual score.

## Latest recorded visual baseline

Owner recording `Screen_Recording_20261029_030542.mp4`: 62,624,545 bytes, SHA256 `3418f738f91caae4bd73432229843298596def67f49741224fe2117b90a0ea33`, 1080x2340. Sequential inspection decoded 8,639 frames through 130.4267 seconds with monotonic presentation timestamps; review used 131 one-second samples, overview sheets and six detailed frames. Exact recorded build identity is unavailable; encoding frame rate is not app performance.

The same five equally weighted visual dimensions score **8.8/10**: identity 8.5, color 9, hierarchy 9, observed coherence 9, perceived readability 8.5. Appearance diagnostics: AMOLED 9.2, Midnight 9.0, Pearl 8.8, Aurora 8.6 (briefer coverage). No lasting blank/stuck page is observed in sampled navigation; this does not certify full frame timing or gesture recovery. Original whole-product 6.7 remains frozen. No new score is inferred from the following source changes.

## Finishing the observed UI issues

Statistics' short controls use a measured FlowRow, avoiding the general action group's 360dp stacking threshold. Their independent 48dp targets and callbacks remain; long labels or large system text wrap naturally. The global layout policy is unchanged. Recent creator tracks, saved playlists and trend sessions use singular at exactly one. Repeated statistics explanations are shorter while retaining active-playback/30-second qualification, consent, device-only privacy, UTC, incompleteness and transfer limitations.

Automatic collections use distinct history, likes, long-listen and creator category icons at the existing 52dp artwork size, avoiding repeated first-track artwork and smaller mosaics. Saved playlists and actual song artwork remain. The save guidance card has less padding and a shorter heading; Find music retains its target and route. Selected mix fill is the principal visual cue, backed by selected semantics and the expand/collapse glyph; an extra accent border and accent glyph are removed. Four theme choices and normal/error roles remain.

## Service-owned queue edits

Connected public queues with Shuffle off expose Edit order and Clear Up Next. Edit order adds Move earlier/later inside a song's options; swipe removal, its button outside edit mode and Undo remain. Moves stay within the explicit Play Next section or ordinary Up Next section. Crossing that boundary is rejected so ordinary songs cannot overtake manually prioritized choices. This does not pretend the shuffle pool has a fixed order. Shuffle explains how to enable linear editing. Offline playback retains its existing private collection controls; no public queue command edits an offline file or collection.

The UI passes the exact reviewed list of public URLs, current identity and manual priorities through the bridge. Both bridge and service reject a stale snapshot; the queue validates it again before any move/clear. No optimistic queue order is written by Compose. Successful edits persist through the existing QueueStateStore, with no schema or bookmark change and no playback restart, pause, resolver call or offline reset. Clear asks for confirmation and keeps the snapshot reviewed when the dialog opened, even if the live queue changes underneath it. It removes upcoming entries only; earlier/current songs, saved collections, files, repeat and transport state remain.

Inspection also found and corrected a concrete Undo defect: removing a manually prioritized song discarded its priority. Undo now restores the original priority order and checks that the remaining queue still matches. Existing one-shot service tokens and invalidation by intervening playback/queue commands remain. Queue capacity stays 100 and a single-song Search selection gains no unrelated tracks.

## Evidence and acceptance boundaries

Ten new executed JVM scenarios cover moves in both directions, current/past/no-op bounds, stale snapshots, priority sections, clear/repeat/end behavior, shuffle/empty rejection, priority-preserving Undo, changed-remainder rejection, reconstruction and a single-song queue. A plural regression is added. The source guard checks actual queue/service/bridge/UI wiring with seven broken-caller fixtures; earlier checks remain.

Three Android UI tests cover confirmation retaining its reviewed snapshot, actual reorder callbacks and shuffle restriction. The four-theme/width/font matrix adds one measured-statistics-actions method, making **17 x 36 = 612 cases compiled only**. No device execution, audible timing, background/process/reconnect, real notification/headset/Bluetooth/call/audio-focus, TalkBack, installed-data continuity or soak is claimed. The original four NW134 acceptance rows remain NOT_RUN until their physical evidence exists. Independent NW135 source work may follow while these explicit gates remain open; final certification cannot.

Source preservation retains 231 previously protected files byte-for-byte and nine callback inventories; three queue/service/bridge files are intentionally changed for this authorized batch. Schemas, dependencies/providers, consent/accounts, source URLs/metadata, library/history stores, lyrics, Search ownership, artwork containment, player transition and all themes/features remain. Private recordings, frames, APKs, receipts and recovery bundles stay outside Git. Actions remains disabled and no public binary/release is authorized.

Build evidence: implementation `8a5a480f23308c772f5fcbf60e5d8af3f3cca2fc`, build `NW-debug-16B498CC3E7811BB`; 1,682 JVM tests / 209 suites and 69 source checks PASS. Full Android/Compose/APK/instrumentation-source build PASS; sequential lint 0 errors / 313 warnings / 17 hints; signer matches previous owner APK. Device acceptance NOT_RUN. NW135 is next for independent source work.
