# NW129 — complete, current local search

28 October 2026. Branch `update/nightwave-nw129-complete-local-search`, protected NW128 parent `684efcfbb3fd3eacf2f8a2ad4d94aff4e8592092` (PR #131). Default `main` remains the old v0.8 snapshot.

## Delivered behavior

The actual main Search screen now loads the complete supported primary-library snapshot and offline metadata in the background. Its old first-25-playlist, 350-public-reference, 400-offline-entry and first-5,000-suggestion boundaries are removed. Metadata preparation and creator grouping happen once per accepted snapshot, rather than during composition or repeatedly for each keystroke.

The supported storage contract is unchanged: at most 500 likes, 100 recent references, 50 playlists × 300 entries (15,600 public references including duplicates), plus 5,000 offline entries within the existing offline-index size bound. This batch does not increase storage limits or activate a new writer. An unreadable/unsupported source produces an explicit unavailable state and Retry; it is not silently presented as a complete empty library. Public provider search and the existing native recovery routes remain available.

Search ranks every duplicate variant before choosing the best-scoring original source object per URL. Equal scores preserve the original stable order. No stored title, uploader, artwork, URL, file ID, collection membership or playback queue is rewritten. The public preview compatibility API still returns at most 24 matches, while the main screen retains complete ranked membership and offers additional result pages (24 public / 60 offline rows per expansion). Creator browsing remains a disclosed 48-group/100-track preview; full song/creator query matching does not inherit those preview limits.

Each local public row identifies its actual Liked, Playlist and/or Recent membership. History-only entries are labeled Recent and never become likes or playlist members. Creator descriptions refer to liked songs, playlists and history, with no verified-artist claim. Offline results now describe metadata entries and direct the user to On device for availability; index membership alone is not a verified playable file.

## Snapshot and cancellation contract

- `MusicLibrary.searchSnapshot()` captures one primary preference map under the existing writer lock, then uses the existing strict snapshot parser. Parsing happens on IO; the lock does not enclose the full parse. Search uses the primary source even while the parity-checked read-only Room preview is enabled elsewhere; it does not make Room a writer or bypass its migration safeguards.
- Actual public/offline preference listeners invalidate the visible corpus synchronously, including background offline completion, deletion, playlist rename and history changes. Listeners are explicitly unregistered on disposal. Only the active, started Search screen builds an index; there is no polling loop or personal-data cache on disk.
- One conflated controller cancels and joins superseded construction before beginning newer work. Repeated multi-key commits are coalesced for 80ms. Strict parsing has bounded non-suspending steps; cancellation waits for them, then discards the obsolete result. Preparation/scoring check cancellation at regular row intervals. No unbounded queue of whole-library builds is created.
- Accepted index state carries the source revision and Activity refresh token. A changed refresh token gates the old index in that same composition. Query results carry the exact query and index identity; a different query or snapshot immediately rejects them before the old producer has received cancellation.
- The production typing runner applies the existing 120ms debounce and executes scoring on Default. Public strict → curated alias → typo fallback precedence, original URLs, repeated-token and Bangla vowel distinctions remain. Offline ranking retains its existing normalization and ordering, with immutable prepared labels.
- Canonical creator keys retain dependent vowel marks. Complete creator/suggestion metadata is prepared once, with small display limits applied only after matching. Blank-query discovery previews do not define corpus membership.
- Local loading, empty and unavailable states are separate. There is no local-source network lookup while typing, no auto-import/repair/download, and no automatic playback command.

See the first-party [latest-flow cancellation contract](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/collect-latest.html) and [preference listener API](https://developer.android.com/reference/android/content/SharedPreferences.OnSharedPreferenceChangeListener). The lifecycle/revision guards and tests supplement these APIs; they do not prove phone scheduling by themselves.

## Memory and compatibility

The index has linear, storage-bounded membership, prepared labels and provenance. A query keeps complete ranked references for paging; the ordinary 24-item compatibility helper uses a bounded top-result heap. Index building has at most one active snapshot/preparation path plus the newest pending request. Exact Android peak heap and GC behavior still require measurement; supported membership is not a universal memory-performance guarantee.

The existing snapshot digest now streams the same sorted-key/length/UTF-8/null-separator contract instead of assembling another whole-library string and byte array. An independent test reconstructs the original algorithm for empty/missing values, Unicode metadata and a complete 10,000-item source. All existing snapshot/parity/recovery tests remain. No digest format, persisted schema, writer, backup policy or recovery fingerprint is migrated.

The Compose test-host manifest dependency is debug-only, following [Android's test setup](https://developer.android.com/develop/ui/compose/testing). It makes `createComposeRule`'s host available; it does not mean instrumentation executed. The release app manifest and its exported surfaces are unchanged.

Home curation budgets, general Library/Smart collection work, public provider ranking, offline transfer/readiness and engine behavior are separate scope. Some non-search work still needs main-thread/startup profiling. This batch does not claim that the entire app has no main-thread IO, or close the remaining Library terminology/organization work assigned to NW132.

## Executed tests and host timing

Seventeen new JVM cases cover real parsed 10k membership and playlist 26, saved positions after 350, offline positions after 400 and at 4,999, complete paging membership, later best duplicate metadata, source provenance, edits/removal, Bangla/alias/repetition/typo parity, late creators, supported bounds, cancellation, streamed digest parity, joined obsolete reads, inactive sessions, failures and refresh/query acceptance. Existing first-5,000 suggestion expectations now test discovery beyond that old boundary. Three new Android cases compile for the actual screen → isolated primary preferences → production parser/index → original result tap path, including observed rename and rapid query change. They have not run on a device.

Current source guards retain the no-autoqueue, multilingual, media, account, data, packaging and recovery protections. Guards that encoded the old private Search boundary, per-query preparation and 5k cutoff now check the actual internal testable screen, complete preparation and query/revision tags. A new positive/negative guard rejects reintroduced caller truncation, missing source cleanup, a stale-result bypass and navigation/storage writes in the index path. Passing these guards is not runtime proof.

Two fresh JVM runs measured the production typing runner on 10,000 unique public rows / 10,600 public references / 34 playlists and 5,000 offline metadata entries. Each of seven strict/creator/Bangla-alias/repeated/typo/offline/miss queries has 20 measured samples per run, after two warmups. Both receipts have identical output digests and exact current measured-source hashes.

- Query p95 across the two runs: approximately **138.4–153.4ms**, including the 120ms debounce.
- Five parse/preparation samples per JVM: cold first samples approximately **1,032.2ms / 1,145.0ms**; subsequent samples approximately **161.3–303.8ms**.
- These are host measurements. They exclude Android preference disk IO, IME/input scheduling, Compose layout/rendering, ART/thermal effects and device acceptance. They are not a synchronized before/after app comparison or proof of the project's phone p95 targets.

Reproduce after the complete build, in sequence with other Gradle work: `gradle -I tools/nw129-benchmark.init.gradle :app:benchmarkFullLocalSearch -Pnw129Runs=2`. The source ZIP/checkpoint retains the raw JSON receipts and source/harness hashes. Do not run separate Gradle build/lint/benchmark writers concurrently in one checkout.

## Pending physical acceptance

No device was connected; no install, account action, real import, library mutation or device seeding was performed. The Android tests use unique synthetic preference files, never clear the real stores and do not play media or contact providers. Preparation of the separate 219-case plan is not execution.

1. On an authorized disposable/reference target, first compare the installed signer and perform only a permitted in-place update. Keep the NW126–NW128 preservation and acceptance blockers visible; never uninstall/reset to make validation pass.
2. Run the three actual-screen tests and manually repeat saved item 451, a track in playlist 26+, offline item 401/5,000, the 10,000th public song and broad-query Show more paths. Verify the original selected URL/ID and correct Liked/Playlist/Recent labels, without invented readiness or autoqueued results.
3. Add, rename and remove entries while typing; complete an offline import/download in the background. Record the next accepted source revision and reject old-query/old-library results during loading, error, pause/resume and screen disposal. Exercise the real preference listeners and Room-preview fallback.
4. Trace snapshot reads, preparation, queries and rendering on Android. Collect at least 20 matched keystroke-to-correct-render runs per declared reference profile, with the same corpus and 120ms debounce, and report p50/p95/max plus failures. Check heap/GC/jank and cancellation under rapid edits. Host timings cannot substitute for this.
5. Recheck Bangla vowel pairs, repeated words, curated Romanized aliases, typo fallbacks, stronger later duplicates, long captions, large type and TalkBack in every theme. Preserve exact originals and the intentional single-song Play behavior.

NW129 source/local completion may enable **NW130 — decidable public music results** while these checks remain open. The provisional score remains **6.7/10**, and public release remains **NO-GO**.

## Final build evidence

- Complete Windows builder PASS; 1,532 JVM tests / 192 suites, zero failures/errors/skips; Android app/test-source compilation and debug assembly PASS.
- 56/56 current source checks PASS. Three actual-screen cases compile only. Earlier obsolete-guard failures are preserved separately from the final passing receipt.
- Sequential final lint PASS: 0 errors, 313 warnings, 17 hints, matching NW128. No new suppression/baseline; the initial two paging-state hints were fixed.
- `NW-debug-CE8EF36E8FCD696F`; canonical source SHA256 `CE8EF36E8FCD696F0AE0FE0191D90F107C2E0950741E02A8432EA4B2F959FD1C` (826 inputs).
- APK SHA256 `6961547D81A2959A3F8BE6112678372BF367EDB90AC2305AA2E725B470F7CFCA`; retained NW08 manifest fingerprint `C0CC48925D7325A75D08FED4CA53F15C3A784D6DF8F64C7BEAB8A09ABD2D028E`.
- Unchanged package/version/SDK and certificate; protected NW128 APK signer comparison MATCH. Actual installed-phone comparison NOT_RUN.
- Two final host timing receipts match outputs and current measured-source bytes. APK/source-bound 219-case plan prepared without app seeding. No device connected; all four NW129 rows remain PARTIAL with device status NOT_RUN.

Private recovery checkpoints retain exact source/APK receipts, 192 JUnit reports, timing JSON/hashes, earlier/final logs and Git bundles. Generated/private artifacts are outside Git. Hosted CI execution is recorded independently on the PR.

Implementation commit: `808a89ecce377d615e479d89bfdd60fccda32ffd`. [Draft PR #132](https://github.com/Nahian2002/NightWave/pull/132). The following ledger-only commit changes no compiled source inputs. The embedded revision is protected-parent context; the canonical source digest identifies the actual built snapshot.
