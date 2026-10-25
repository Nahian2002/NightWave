# NW48 — Downloads & Offline 2.0

**Parent:** `feature/nightwave-nw47-home-library-you`  
**Branch:** `feature/nightwave-nw48-downloads-offline2`  
**Status:** SOURCE IMPLEMENTED, source-contract inspected. **Full Gradle build, JUnit execution, device QA and end-to-end network performance are NOT VERIFIED.**

## Downloads center
- **All / Active / Ready / Issues** tabs with 48dp targets. These are display-only selections; no files or worker records are deleted.
- WorkManager **SUCCEEDED** does **not** imply Ready: the saved song ID is checked once in a batched read against the actual private library. Completed transfers whose file has since gone missing surface as Issues, not as playable.
- Unified foreground counter summarizes active, verified ready and failed/cancelled/missing-file items. Filtering occurs before the 60-card display cap; newest/current user request stays ahead of historical transfer records.
- Progress percent calculation uses bounded doubles to avoid integer multiplication overflow on malformed large counters; unknown length remains indeterminate.
- Throughput estimator keeps a baseline when progress events are closer than 250ms; it hides speeds older than eight seconds, and shows **No new bytes · Waiting for source** instead of silently displaying an old speed.
- A foreground-only five-second UI refresh detects stalled transfers without a background forever-loop. It is unregistered on Activity pause and preserves the download scroll location.
- Selected download tab survives rotation.
- Search-track Pause remains an explicit **stop-and-restart**, not real HTTP Range resume. Search retry still requires permission and validates the exact canonical recording; it does not save signed/expiring audio URLs in tags. Historical hide only hides a card, never deletes offline audio.
- Catalog and direct-link jobs retain Cancel and their existing safe link/catalog recovery flows. No new unsupported automatic resume promise is added.

## Retry and transfer integrity
- Catalog, direct and search workers immediately publish **zero bytes / unknown total** at the beginning of each new WorkManager attempt.
- The same workers report progress resets immediately when internal downloader retries restart from zero, rather than retaining a previously high percentage after failed network reads.
- The transfer engine remains unchanged: 128 KiB sequential buffer, HTTPS only, strict redirect+DNS safeguards, supported audio MIME/magic validation, MediaExtractor audio-only verification, private staging with cleanup, size and storage limits, catalog hash/byte checks where supplied, final private-library commit only on successful validation.
- There is **no persisted partial-file or byte-range continuation** in this batch. Source/network conditions primarily determine speed; we cannot promise a universal throughput increase.

## Offline Library
- **All / Ready offline / Needs attention** filter applies only to the already indexed health report; missing, malformed or altered files remain indexed for explicit recovery. All filters are combined safely with existing local search and sorting.
- Health-specific empty states distinguish no imports, missing playable audio, and no entries requiring attention.
- Increased Play, Drive upload and Remove controls to 48dp targets. Remove continues to require confirmation, and unverified files cannot be deleted through that option.
- Secondary sources, personal Drive music and offline transport controls are reachable through a collapsed **More sources & playback** section. This saves vertical space on compact screens while preserving all destinations and media actions.
- The selected health filter survives rotation. Song health checks remain read-only; offline resume still checks its selected file and never depends on the UI filter.

## Tests and verification scope
- New pure policy tests: `DownloadCenterViewPolicyTest` and `OfflineLibraryViewPolicyTest`, covering offline-ready proof vs WorkManager status, active and issue groups, Long.MAX_VALUE progress, unavailable bytes, stale transfer speeds, each file-health classification and empty-state messaging.
- `tools/verify_nw48_downloads.py --self-test` guards the original media/audio validation path, pause semantics, progress reset, source callbacks and health filters with five negative mutation fixtures.
- The Android CI workflow includes both source verification and the existing Gradle compile/tests. **A GitHub Actions failure before job steps are reported is NOT a compiler verdict.**
- Existing NW18/NW21/NW23/NW35 source checks were reviewed for relevant integration constraints; no claim of end-to-end execution.

## Combined APK acceptance after NW52
1. Run the standard Windows build on the final stacked branch and require successful Kotlin/Android compilation, JUnit suite and instrumentation test-source compilation.
2. Start an eligible searched-track save on Wi-Fi; verify accurate percent/speed, leave/reopen Downloads, disconnect the network and retry when allowed. Ensure no stale percentages survive a clean transfer restart.
3. Pause a searched track: it must stop and present **Resume restarts from zero**. Resume requires another explicit approved request and must not invent saved partial bytes.
4. Test catalog and approved direct audio downloads. Try an invalid type, oversized source, wrong checksum, redirect and lost connectivity: no corrupt file should be registered as ready.
5. Test completed songs, manually removed files and damaged-entry safe simulations on disposable data. Ready filters must reflect current real file status; Issues must not delete or repair anything automatically.
6. In Offline Library test local search, every health filter, sort order, pagination, Resume session, Play All, Shuffle, multi-file import, Drive shortcut, and explicitly confirmed removal. Confirm the extra tools panel opens and closes without hiding the song list permanently.
7. Rotate or split screen on 320/360/390dp layouts with large text; all buttons must remain reachable and touch-sized. Check screen readers, background audio and library persistence after a normal update (do **not** uninstall or clear app data).
8. Check real-world throughput with known authorized endpoints; speed depends on server/network, so compare actual sustained MB/s rather than assuming an app-side increase.

**Next:** NW49 Audio & Lyrics Studio. This batch does not remove legal/source restrictions and does not grant arbitrary YouTube offline download rights.
