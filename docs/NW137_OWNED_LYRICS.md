# NW137 — guided user-owned lyrics

30 October 2026. **LOCAL_VERIFIED / DEVICE_NOT_RUN** on `update/nightwave-nw137-owned-lyrics`, stacked on SEARCH02 #158. Protected parent `721ece36c9b9900cae4daa6936dd1caf77e8ac9c`; private checkpoint `20261030-nw137-lyrics` retains a baseline bundle. Weekly reserve checked before starting: 29% remaining, above the 10% stop threshold. NW138 Sound Studio follows this batch.

## Committed implementation plan

1. Preserve the existing strict UTF-8 LRC parser, review warnings, repeated-cue ordering, offset/follow/seek/editor/export behavior and local lyric/offset/cloud formats. Add a small binding policy for exact existing offline IDs and canonical public YouTube video IDs; never infer identity from a title, copied filename or signed stream URL.
2. Reuse the guided file-picker/review/explicit-confirm workflow for both supported source types. Existing offline lyrics stay in their original store. User-owned public-reference lyrics and offsets use a separate bounded device-private namespace (128 tracks, 4 MiB total); no provider catalog, scrape, transcription, automatic matching, network call or new cloud backup claim. Preserve original source URLs and account/consent boundaries.
3. Reuse editor/follow/timing/seek and user-directed LRC export through a binding-aware facade. Check selected source, lifecycle/epoch and saved-content fingerprint on preview, import, editor, offset and export paths. Serialize local writes with existing index mutation and public writes across facade instances. Reject deleted/replaced/stale input instead of binding to a different recording.
4. Retain only one bounded pending import/review in a ViewModel for configuration changes. Reconnect to the selected player before presenting a restored review; a changed source rejects it. Process death safely cancels pending import rather than saving without review. Do not persist picker addresses or LRC bytes in saved state, intent extras, logs or diagnostics.
5. Add meaningful binding/content/quota/fingerprint tests plus isolated Android store and screen cases, retain and adapt previous guards with negative fixtures, run full Windows JVM/Android/APK/instrumentation compilation and current checks, then sequential lint. Recheck weekly reserve after implementation and before validation/publishing. Verify signer/source preservation, recovery archives, a stacked draft PR and owner-only APK streamed hash.
6. Keep actual imported English/Bangla synchronization, rotation/process/picker, selected-source races, font/TalkBack/four-theme rendering, online/local seek, cloud/local preservation and long playback acceptance NOT_RUN without phone evidence. Source completion does not earn a new rating or provider rights.


## Results and source contracts

Committed plan `5df404d6`, implementation `afbb1fe7`, export-marker correction `71e79525`, final built test-fixture correction `a94328169b2cddad4669e9937296a8e90e1b0969`. A successful final frozen build is the only candidate promoted. Superseded export-marker and test-reference failures remain in the private checkpoint.

- Guided import now works for a selected indexed local song or an exact canonical public video. Both paths use the original UTF-8/64 KiB/1,200-cue parser, advisory filename/title notes, bounded preview and explicit Import/Replace confirmation. The review names the selected source; a title match never attaches words automatically. Public video IDs are namespaced independently of original local IDs, so editions/local copies cannot alias by filename.
- Existing local lyrics/offset stores and cloud lyric codecs remain byte-identical. A binding-aware facade delegates local operations to the same existing store instance. Public-reference LRC/offsets live in separate app-private `nightwave_public_lrc_v1`, bounded to 128 tracks and 4 MiB of UTF-8 lyric payload; no scraping, provider catalog, transcription, network lookup, signed URL or cloud-format change. User-directed export still goes through the existing SAF picker.
- Public saves compare the reviewed fingerprint under a shared writer monitor and reject inactive or stale review. Local writes also serialize with the existing index monitor so deleted membership cannot acquire a new saved LRC. Editor, offsets, reading and export use exact source binding; old/current source, epoch, lifecycle and fingerprint guards remain. Existing cue seeking already carries the displayed media ID and is reused for public imported lyrics.
- One pending picker ID/review is retained in a ViewModel across configuration changes. A picker callback waits up to three seconds for player connection and checks the selected source; a restored review reopens only for that exact selected source. Explicit cancel/confirm clears the retained review. Process death and an interrupted in-flight read can safely require selecting the file again; no unreviewed import is silently resumed. No LRC bytes/picker address are added to saved-state bundles, intents, logs or diagnostics.
- Existing follow/manual scroll, repeated-cue order, fine timing, title warnings, editor/Undo and explicit saved/draft export remain. The public player now says Attach your own LRC, with device-only scope, rather than falsely requiring local audio. No licensed lyrics service or new acoustic correctness claim is introduced.

## Validation

| Evidence | Final result / practical limit |
| --- | --- |
| Windows builder | Full Android/Compose/APK and instrumentation-source compilation PASS |
| JVM | **1,784 tests / 220 suites**, zero failures/errors/skips; twelve new tests |
| New host coverage | Exact local/public keys, alternate recordings, unsafe/signed references, Bangla marks/simultaneous cues, malformed/large content, fingerprints, track/UTF-8 quotas and existing offset/seek rules |
| Source checks | **77 PASS**, including owned-LRC caller-negative fixtures; prior local/import/editor/export checks retained through the facade |
| Lint | **0 errors / 322 warnings / 17 hints**, five added notices; no suppression/baseline |
| Added notice triage | Three ApplySharedPref recommendations and two UseKtx notices in the new facade. All real writes dispatch through IO; explicit commit Boolean/durability is required for truthful Save/Remove/offset results, so asynchronous apply or a Unit-returning wrapper is not substituted. |
| Existing compiler notice | Unchanged Library selectedPlaylist-null condition remains triaged; unrelated screen tail is byte-identical |
| Android additions | Five isolated store and two real-player methods compile only: separate public/local persistence, cross-instance stale fingerprint, inactive commits, cloud/local ID preservation, invalid data, public import and source-scoped cue seek; **NOT_RUN** |
| Preservation | **905 prior inputs identical**, twenty intended existing edits, five additions and nine callback inventories retained |
| Immutable boundaries | Original local LRC/offset/parser/editor/export/cloud formats, audio/index/network/source quality, package/signing/dependencies and theme tokens unchanged |
| Secrets | Implementation-stage all-ref Gitleaks **3,556 commits**, no leaks; reviewed exclusions unchanged |
| Phone/release | Eight new supplemental cases plus original NW137/prior gates remain **NOT_RUN**; no installation/reset, account action, merge, public APK or Actions change |
| Private recovery/delivery | Exact working-byte ZIP/CRC/canonical verification, baseline/final bundles, raw JVM/lint/source/signature/receipts, owner-only **33,844,842-byte** streamed SHA match |

Build **NW-debug-7027DA65E2963716**; source **7027DA65E2963716F7F8CFC39E39BB69619D6CB2B957AB25231BF65FBE27ADFD**, **930 inputs**. APK SHA256 **BB1144C07D7377CA4CF6E5F9EB0258356F7F87AB1326F1D6DCA931BB293F3E82**. Signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**, matching SEARCH02. Package/version `com.nightwave.player` / `2.0.0-alpha5s` / code35, min26/target36 unchanged. Canonical bytes match the final built revision above. Baseline 219-case pack is prepared for this exact APK, not executed acceptance.

Weekly reserve was checked before work, during implementation and before validation/delivery; the existing hourly stop rule remains active. No post-change 9.5 score is awarded. Actual English/Bangla synchronization, picker/recreation/source races, public/local seek, original cloud behavior, four-theme/font/TalkBack and long audio/route/resource checks remain required. **NW138 Sound Studio active/draft clarity is next**, then NW139–NW145.
