# NightWave — Spotify-quality roadmap, NW111–NW125

Established: 27 October 2026. Status: ACTIVE PLAN, NOT RELEASE CERTIFICATION.
Protected parent: 7011ad2609e5074f72e5ea750148b0cf625d8f94 (NW110 and Windows-build corrections).
Development branch: feature/nightwave-nw111-spotify-quality-roadmap-search-relevance.
Prior plans: [NW76–NW90](CURRENT_CUMULATIVE_ROADMAP.md), [NW43–NW52](NW43_NW52_PREMIUM_PRODUCT_ROADMAP.md). Do not overwrite their historical gates.

## Goal and current evidence

Aim for Spotify-level reliability, speed, clarity and polish while preserving an **original NightWave identity** (dark/midnight styling, restrained mint accent, local-first and no required server). Spotify is a quality reference, not a UI template or music license. A 9.5+/10 rating is a **target, not a claim**.

The owner's 27 October 2026 Samsung video (3m36s) supports subjective overall score 7.0/10, design 8.0, navigation 8.3, search 6.1, player 7.9, queue 7.1 (unverified), offline 5.0 (unverified), transfers 6.0, library 7.5, lyrics 5.3 (unverified), EQ 6.9, backup 6.0, reliability 6.7. These are not automated benchmarks.

Observed problems: "Jhum Cultivation" appeared as a Best Match for a music-intent "Jhum" search; a transfer showed roughly 1% at 36 KB/s and stated resume restarts from zero; backup was pending; complete offline playback/backup restore/EQ effect were not demonstrated. Screen captures do not verify music rights, playback under low network, audio-route behavior, signed APK or phone acceptance.

Latest Windows evidence: production Android Kotlin and debug APK assembly compiled; 1,283 JVM tests ran with 1 failure in ShuffleRecoveryPersistenceTest. The test received a fail-closed correction at the protected parent SHA, but that exact revision has **not yet been rerun on Windows**. Signer, release APK, Google Drive roundtrip, rights and on-device reliability are unverified.

## Guardrails — all waves

- Preserve application ID com.nightwave.player, signature compatibility, saved library, playlists, downloaded files, Drive account, settings and queue. Never uninstall/clear app data to force upgrades. Keep backups and work only on isolated review branches.
- Keep all shipped features. Fix actual failing tests/implementation without weakening quality gates or deleting behavior. Add positive, negative and adversarial tests. Fail-closed diagnostics, no false "passed" reports.
- Keep source categories separate: public video metadata/search vs locally owned/permitted/appropriately licensed playable offline audio. YouTube API policies disallow storing audiovisual content/offline playback without prior approval. Legacy YouTube extraction routes are a **public-release rights NO-GO** unless redesigned/authorized. Do not bypass restrictions or promise every YouTube song can be saved.
- Preserve original video URLs, no silent autoqueue/URL rewrite or false verified-artist/official claims; SONGS ranking may reorder, VIDEOS remains provider-native.
- No mandatory custom backend, billing, subscriptions, premium build runners or added account requirements; optional Drive uses only user consent and supported OAuth.
- Run available tests after each source batch; user prefers **one consolidated Samsung test cycle after implementation**. Never mark Android/JVM/phone tests passed without independent receipts.
- For each wave retain GitHub branch, commit SHA, exact changed files, feature and regression count, risks, automated receipts, build signature gate and unresolved blockers. Complete full-project GitHub ZIP, not a hotfix-only ZIP.

## Ordered upgrade waves

| Wave | Priority | Code goal | Acceptance proof |
| --- | --- | --- | --- |
| NW111 — Music-first search | P0 | Re-rank obvious nonmusic incidental matches in SONGS while preserving all candidates; user requesting the nonmusic topic overrides penalty. No playback/network change. | Regression for Jhum cultivation, requested cultivation, genuine music, VIDEO mode and identity-preserving result set |
| NW112 — Romanized and multilingual search | P0 | Conservative Jhum/Jhoom transliteration, Bangla native spelling, artist/title exactness, duplicate and version grouping, without "Alo"/"Alone" false hits. | 40+ fixed queries, documented top-3 precision and false positives; Bangla vowel identity preserved |
| NW113 — Offline truth/eligibility | P0 | Honest online-only/eligible/verified-unavailable labels, supported source permission, no success status until playable verified index. | Missing/zero/tampered file, low space, provider rejection, restart and Airplane Mode fixtures |
| NW114 — Transfer reliability | P0 | WorkManager state transitions, accurate speed/ETA, retry/queue, cancel/pause truth, resume only if source supports byte ranges, integrity. | 0/partial/complete/timeout/reboot/low storage and deterministic checksums |
| NW115 — Playback/queue correctness | P0 | No random bulk-queued search results, ordered Play Next, shuffle history, repeat, linear/playlist mode, MediaSession and crash recovery. | Exact selection, stress state machine, corrupted histories, headset/lock screen and Android compilation |
| NW116 — Premium player | P1 | Balanced artwork/text/transport/lyrics/queue and compact mini-player, visible control state, interruption-safe motion. | 360/412dp, long song names, 1.0–2.0x font, safe animations and Compose builds |
| NW117 — Authentic Home | P1 | Strong resume hero, varied listening shelves, no duplicated cards or imaginary metrics. | Empty/new/existing accounts, recent/liked recency tests, layout and scroll stability |
| NW118 — Library polish | P1 | Playlist artwork/details/folders, recent removals, efficient sorting, multiselect, offline badges, safe undo. | No unintended writes, rollback, 5k-file library, inaccessible-file recovery |
| NW119 — Lyrics | P1 | Accurate imported LRC timing, offsets, editing and full-screen follow with Bangla and TalkBack. | Timestamp, wrong-song rejection, large lyric, seek-follow and device latency |
| NW120 — Audio/EQ | P1 | Real audio-session readback for EQ, reliable speaker/Bluetooth/headphones, gapless, crossfade, normalization. | Audible A/B plus readback, route changes, no fake enabled status |
| NW121 — Drive/guest backup | P1 | OAuth errors, durable connection status, backup/readback verification, preview/restore with no silent overwrite. | Disposable profile roundtrip, account switch, cancellation and signing SHA |
| NW122 — Performance | P1 | Measured cold/warm start, search p95, playback-next latency, caching, allocations, jank and battery. | Before/after measured baselines and 30-min playback soak |
| NW123 — Accessibility | P1 | Consistent typography/touch areas, TalkBack, reduced motion, high contrast, Bangla/English. | Device screen-reader, 360dp/large-font checks and touch coverage |
| NW124 — Useful optional extras | P2 | Refine sleep timer, smarter offline discovery, user-owned playlist export/import; device/social only when real architecture permits. | User opt-in, honest fallbacks, privacy, power and offline regression |
| NW125 — Final certification | RELEASE | Clean Windows Gradle build, full JVM/Android tests, signer match, device 139-case matrix, rights/security/OAuth/privacy audits. | Release decision GO only if *all* critical gates passed with exact-commit receipts |

Dependencies: NW111→NW112; NW113→NW114; NW115 before motion-sensitive NW116; NW125 cannot start release acceptance until all critical tests are green. Urgent data-loss/security defects take precedence, but are documented and never silently skipped.

## Core end-to-end behavior to prove

1. Query "Jhum" shows likely music above cultivation; query "Jhum cultivation" can still find nonmusic; Videos mode unchanged, all provider URLs remain; Bangla vowel marks and repeated tokens stay distinct.
2. Tapping one Search track queues that exact song; Play Next works; Previous/Next in shuffle retrace the visited tracks; restart does not change selected song.
3. Save legally eligible/owned audio, verify file integrity, disable data/Wi-Fi, force-stop/reopen and play, seek and skip without internet. An unsupported source must show a truthful explanation rather than fake 100%.
4. Drive account stays connected; disposable backup/readback/preview/restore passes with no unrequested data deletion. Existing accounts and guest paths survive upgrade.
5. Player is unclipped at small/big displays with 2x font; controls are responsive, TalkBack accessible, EQ changes reflect real active sessions, no background playback interruptions.
6. Real build report from same commit: Kotlin compile, 100% required JVM tests, AndroidTest compilation, signature identity and no blocker; separate phone and public-rights checks remain mandatory.

## Evidence and phase status ledger

Status vocabulary: PLANNED / IN_PROGRESS / CODE_COMPLETE_UNVERIFIED / GRADLE_VERIFIED / DEVICE_VERIFIED / BLOCKED. "Source gate pass" is not Android compilation.

| Milestone | Initial status | Reason |
| --- | --- | --- |
| NW110 Windows build recovery | CODE_COMPLETE_UNVERIFIED | 1 of 1,283 tests failed on former commit; follow-up test fix unverified on Windows |
| NW111 Music-first search | CODE_COMPLETE_UNVERIFIED | Bounded nonmusic context demotion committed; 5 new JUnit tests; Windows Gradle/JVM/device tests pending |
| NW112 Multilingual search | CODE_COMPLETE_UNVERIFIED | Curated 3-group Romanized/Bangla fallback and 135 JUnit query/title fixtures; Android/JVM test build pending |
| NW113 Offline truth/eligibility | CODE_COMPLETE_UNVERIFIED | Index-unreadable is not treated as deleted files; read-only per-file health and truthful file-size labels; 6 new JVM tests; full Gradle/device pending |
| NW114 Transfers | IN_PROGRESS | Partial HTTP 206/contradictory Content-Range fail-closed, honest byte/s and ETA, transparent queued retries; 6 new JVM tests. Safe persistent Range resume NOT implemented; full Gradle and device checks pending |
| NW115 Playback/queue fidelity | IN_PROGRESS | Shuffle history, same-track replay, headset controls, transient HTTP recovery and safe IDLE-session re-resolution; 33 new JUnit methods across five batches; full Gradle/device tests pending |
| NW116 Premium player | IN_PROGRESS | Responsive Now Playing transport splits primary/secondary controls on narrow or large-text windows; Offline/Lyrics actions stack when needed; 6 new layout JUnit methods; Compose/device QA pending |
| NW117–NW125 | PLANNED | Not started; status does not imply functionality |

After every wave update this ledger with commit, automated receipts, remaining blockers, actual device acceptance and next planned wave. Avoid recalculating a "Spotify score" based on static code checks.

## Source references

- [Spotify Premium benefits](https://support.spotify.com/us/article/your-premium-benefits/) (lossless on eligible devices and settings)
- [Spotify transitions](https://support.spotify.com/kr-en/article/tracks-transitions/) (gapless, crossfade, Automix)
- [YouTube API developer policies](https://developers.google.com/youtube/terms/developer-policies) (audiovisual-content downloading/offline restrictions)
- [YouTube policy compliance guide](https://developers.google.com/youtube/terms/developer-policies-guide)

The roadmap defines future turns and test requirements; it does not schedule autonomous background coding.

## NW111 implementation ledger — 2026-10-27

- Parent: 7011ad2609e5074f72e5ea750148b0cf625d8f94; isolated branch as above.
- Production change: SearchMusicIntentPolicy.incidentalNonMusicPenalty() uses exact normalized word boundaries, a bounded -230 nonmusic penalty, -55 if explicit music markers exist, and 0 when the user requested the contextual topic. Read-only metadata; no URLs/playback/files modified.
- Wiring: SearchRelevancePolicy.publicScore() invokes this advisory penalty during SONGS ranking. VIDEOS still bypasses the music ranker; result counts and original ordering on equal scores unchanged.
- Added tests: SearchRelevancePolicyTest has five new JUnit methods, with 16 new assertion calls and every one of the original 22 test methods retained. Covers Jhum cultivation vs Jhum music, explicit cultivation intent, music-tag exception, Videos-mode preservation, unchanged ordinary results.
- Static source verification: all new method references exist, 27 SearchRelevancePolicyTest methods detected, old Alo/Alone and Bangla vowel tests still present. These are source checks, NOT JUnit passes.
- No permissions, manifest, backend, download/DRM/source-rights, OAuth, offline files or queue code changed.
- Windows Gradle (new branch): NOT RUN. Device proof: NOT RUN. Fresh APK/identity: NOT VERIFIED. Rights decision: NO-GO unchanged. Next: NW112 native/Romanized multilingual search precision with a stricter gold-fixture evaluation.

## NW112 implementation ledger — 2026-10-27

- Starting point: NW111 at commit ce7ce69db251789c314b9164365fb7e815794049, with earlier NW110/Windows fixes unchanged.
- New pure policy: SearchMultilingualAliasPolicy.kt. Curated whole-token spelling groups: jhum/jhoom/ঝুম, bhalobasha/valobasha/ভালোবাসা and bhalobashi/valobashi/ভালোবাসি. Maximum 8 words and 15 alternates per query. The original normalized query and video URLs are NEVER modified.
- Online Songs ranking: SearchRelevancePolicy.publicScore evaluates original spelling first; alternate queries receive a fixed score penalty to keep exact matching ahead of equivalent aliases. Existing NW111 incidental-nonmusic demotion remains; Videos mode uses original provider ordering.
- Saved tracks: SavedTrackSearch uses direct matches first, curated aliases only when no direct hits, typo fallback last. Saved URLs, media, playlists and files remain unchanged.
- Tests: SearchMultilingualAliasPolicyTest.kt adds nine @Test methods, including 135 deterministic query/title cross-script fixtures (3 spelling groups x 3 typed forms x 3 source forms x 5 query suffixes), independent nonmusic rejection, Bangla vowel contrasts, repeated-token identity, literal-vs-alias, saved fallback, Videos mode unchanged, numeric versions and Alo/Alone. Existing NW82 and NW111 source tests retained.
- Independent Kotlin CLI policy smoke: Kotlin 1.9.0/JDK21 compiled the new bounded-alias algorithm with the existing token-splitting contract and exercised 33 assertions PASS. This is a local focused policy smoke, **NOT** Gradle, Android, the entire scorer, or the 135 new JUnit cases.
- Remaining: no complete NW112 Gradle/JVM execution, signed APK, physical-device search benchmark or 40-query measured precision receipt. NW112 source is CODE_COMPLETE_UNVERIFIED and public release remains NO-GO pending rights and all real acceptance checks.
- Next phase: NW113 offline truth/eligibility, only for owned/licensed/authorized sources. Do not infer blanket YouTube offline rights.

## NW113 implementation ledger — 2026-10-27

- Protected parent: NW112 commit 180ff5cdca091c9e3bf927198ed028b32dc556d2; isolated NW113 branch retains NW110, NW111 and NW112 history.
- Discovered defect: the existing OfflineAudioLibrary.availableIds() caught index parsing failures and returned an empty Set. Download Center then treated a failed index read as if every completed recording was deleted. That is misleading, even though the UI correctly did not delete files.
- New read-only OfflineAvailabilitySnapshot(indexReadable, healthById) separates UNKNOWN-index-failure from MISSING, SIZE_CHANGED, UNREADABLE, UNSAFE_METADATA and AVAILABLE file states. Does not rewrite any index or file. Existing availableIds(requested) public API and its fail-closed semantics remain intact.
- Download Center now queries OfflineAudioLibrary.availabilitySnapshot() on the existing single off-UI-thread executor, keeps the generation check, refuses to claim Ready if index parsing fails, and renders individual file-condition hints for completed transfers.
- Honesty improvement: 'file present / size checked' replaces the former claim 'verified local file'. The status and the Offline Library availability row explicitly avoid asserting SHA or decoder proof from a size-only read. New in-app statuses retain the indexed entry on failure; no automatic repair/deletion.
- Tests added: 3 DownloadStatus + 3 OfflineLibraryHealth tests, including corrupted/unreadable index, missing file, stale file size, unreadable file, unsafe metadata and false availability after previous WorkManager success. All preexisting tests preserved.
- Static checks: 12/12 source contract invariants verified including original availableIds signature, path/symlink safety, UI off-thread checks, generation guard and old tests.
- **NOT EXECUTED:** full Android Gradle/JUnit suites on this commit, complete phone offline playback, read/write low-storage or same-size corruption proof. Size equality is NOT a cryptographic hash or successful Media3 decode. Public music-source rights remain NO-GO until verified.
- Next: NW114 Transfers & Resumability — true retry state and byte-range continuation only when an authorized source supports it, otherwise truthful restart semantics.

## NW114 implementation ledger — 2026-10-27

- Protected cumulative baseline: NW113 commit 8dffa59b7fc77290b88a0ebefca781eae628895a. Work performed on isolated feature/nightwave-nw114-transfer-truth-and-retry. Existing code, offline library and Android package/signing remain intact.
- Complete-response safety: AudioDownloadNetworkPolicy.isFullStartResponse() only accepts HTTP 200 with no Content-Range. PermittedAudioDownloader still issues normal GET without Range and rejects 206 or contradictory 200/Content-Range, with a safe text error; never appends response bytes or registers corrupt partial downloads. All pre-existing URL whitelist/DNS/private-storage/media validation checks retained.
- Throughput and ETA: OfflineTransferRatePolicy now reports actual B/s under 1 KiB/s and one-decimal KiB/s below 10 KiB/s; ETA always rounds a positive remainder upward and never invents a known duration when server total is unavailable.
- Queued retries: WorkManager ENQUEUED with prior run attempts says "Retry scheduled · Waiting for network/backoff · Starts from zero"; first attempt remains "Queued · Waiting for network". Pause remains a cancellation request, not a byte-range resume.
- Public UI errors: both general and search offline failure policies classify unsupported partial audio separately without revealing raw signed media URLs or response internals. No new media provider, API key, login, elevated permission, network bypass or source capability is introduced.
- Regression declaration: 6 additional JUnit methods across AudioDownloadNetworkPolicyTest (1), OfflineTransferRatePolicyTest (2), DownloadRecoveryPolicyTest (2), SearchOfflineFailurePolicyTest (1). Existing tests retained; real JUnit invocation and Android runtime are NOT VERIFIED.
- Source/diff inspection: only the four source policies, downloader, Downloads UI and four corresponding existing JUnit test files changed (10 files); no library, auth, playback, app manifest, Gradle configuration or signer files touched.
- **Deferral / remaining scope:** HTTP byte-range resume is NOT implemented. Current downloader intentionally deletes incomplete staging files on cancellation/error; even a source advertising Accept-Ranges is NOT enough to safely resume. True persistent resume needs a stable source identity, strong ETag/validator, Content-Range verification, authenticated private checkpoint persistence, bounded bytes, final full-file hash plus decoder validation, cleanup and lifecycle tests. Never claim existing Pause/Resume retains partial bytes. Additional full JVM/Gradle, reboot, network failure, low-storage, 139-case device and source-rights tests remain mandatory.
- **NW114 status IN_PROGRESS**, source improvements committed but end-to-end completeness and safe resume unverified; NW115 planned only, not claimed. Public release still NO-GO.

## NW115 first implementation batch — 2026-10-27

- Parent NW114 SHA: 86092de7bf2117025f53eb5ed4733e3404f8661f. Isolated branch: feature/nightwave-nw115-shuffle-jump-history. All NW110–NW114 changes preserved, no merge into older main.
- Detected behavior: PlaybackQueue.next() correctly marks the current song as heard in shuffled cycles, but PlaybackQueue.jump(to) failed to mark the departed selected track as heard. After listener manually jumps A→B, the following shuffle Next could immediately select A again, even though the listener just played it. Recovery snapshots also lost that played-state fact.
- Minimal fix: add shuffledSeen.add(index) alongside the existing visited.add(index) ONLY for a valid Shuffle-mode jump to a different track. Order matters: store old index before assigning index=to. No changes to linear navigation, explicit user jump back to an already heard song, shuffle cycle reset, repeat, Play Next, queue persistence schema, URL format, private audio or selected song.
- Test declaration: a new PlaybackQueueShuffleJumpCycleTest.kt with 7 new JUnit test methods and 36 assertion calls: no early replay after explicit jump, restart recovery, invalid/same-song jump, deliberate manual replay, toggling Shuffle, manual Play Next priority, exhausted two-track Repeat OFF and Repeat ALL.
- Existing suite including ShuffleRecoveryPersistenceTest and PlaybackQueueNavigationModeTest is unchanged. No prior tests removed or modified.
- Verification: code and regression assertions inspected against actual PlaybackQueue.next()/jump()/restoreShuffleRecovery()/hasNext(). GitHub source-only check, NOT JVM or Android tests. The owner must run the full Gradle/JUnit suite, MediaSession, screen-off and queue persistence phone tests to accept this wave.
- Remaining NW115 scope: end-to-end exact search→single-song queue, Media3 Session commands, lockscreen/headset, previous threshold, Repeat One, failure recoveries and device stress. **State remains IN_PROGRESS**, not fully certified.
- NW114 remains IN_PROGRESS for safe persistent byte-range resume, pending a source-authorized resumable protocol and end-to-end validator/checkpoint QA. No pseudo-resume claims, no unverified online source extraction or offline-rights promises. NW125 release remains NO-GO.

## NW115 second implementation batch — 2026-10-27

- Parent first NW115 batch: 7e79a62a9f792eb3598194b6bee5995b14acbf4f. Isolated cumulative branch: feature/nightwave-nw115-loaded-jump-replay, preserving NW110–NW114 and the NW115 Shuffle jump fix.
- Concrete UI latency defect: PlaybackService ACTION_JUMP always called resolveAndPlay() after any valid public-queue jump, including the current selected song already loaded in Media3. This unconditionally stopped and cleared healthy playback to trigger another public-stream extraction, delaying replay and risking an otherwise avoidable network error.
- New pure PlaybackQueueJumpPolicy.canRestartLoaded() requires a canonical public selected URL, equality between the **old selected**, the **requested** and the **loaded Media3 mediaId**, plus no player error and a non-IDLE player. It cannot authorize replay of signed media URLs or an unrelated loaded item.
- Service behavior: for an explicit same-song tap with a healthy loaded item, preserve saved queue, cancel any crossfade, seek the current loaded Media3 item to zero, prepare only if it had ended, call Play, save the zero bookmark and publish widgets. New/different/unloaded/errored selections retain the existing resolveAndPlay() path and security checks. No playlist, queue dedup, Play Next, Shuffle, Repeat, downloads, offline files or OAuth changed.
- Added PlaybackQueueJumpPolicyTest.kt with **7 new JUnit methods and 27 assertion calls**: accepted exact loaded replay, different jump, stale loaded item, idle/player error, null/noncanonical/signed identity, same-row Shuffle and Play Next preservation, and a distinct-track queue jump.
- Cumulative NW115 new test count: 7 previous Shuffle jump regression methods + 7 loaded-replay regression methods = **14 new JUnit test methods**. None of the older tests removed or modified.
- Static review: 13/13 source integration checks found the new guard wired into ACTION_JUMP, original navigation/MediaSession/auth checks intact, queue persistence and bookmark retained, and the fallback resolver preserved. No Android Gradle/JVM or device execution of the new tests has occurred.
- Still IN_PROGRESS: real MediaSession/controller notification, car/headset lockscreen, stream resolution/race testing, full clean signed Android build, NW114 byte-range resume feasibility, and legal music-source permissions. NW125 GO remains blocked.

## NW115 third implementation batch — 2026-10-27

- Protected parent NW115 Batch 2 commit: 0ca9727a0bd998221a3fa1940a5bf7c33b152303. Isolated branch: feature/nightwave-nw115-media-button-next-truth. All earlier cumulative code, app ID, library files and Google OAuth unchanged.
- Concrete MediaSession mismatch: notification custom Next already rejected absent Next targets, but PlaybackService.onMediaButtonEvent() always returned true after a headset MEDIA_NEXT key even for an exhausted queue/Repeat OFF or no selected song. Previous likewise returned true on an empty selection. A false success is misleading to Android/Bluetooth controllers and hides a no-op.
- Add read-only MediaSessionQueueControlPolicy.canAcknowledgeNext(): requires the currently active public/offline selection and hasManualNext() computed without consuming Shuffle, Play Next or Media3 timeline. Wire that same check into both the custom notification Next command and the raw headset MEDIA_NEXT handler. For headset MEDIA_PREVIOUS, require a selected track before acknowledging; on a valid selection preserve existing >3-second restart and ordinary previous navigation.
- Returning false for unavailable transport leaves Media3/platform fallback available. All loaded-media Play/Pause/headset-toggle actions remain delegated to Media3. Reject key-repeat and action-up as before; preserve audio focus and becoming-noisy behavior.
- Extend MediaSessionQueueControlPolicyTest.kt by 5 JUnit methods and 14 assertion calls: exhausted public, offline gapless end, wrong selected mode, valid selected target and non-mutating repeated availability probes with manually prioritized Shuffle. Original 8 MediaSessionQueueControlPolicyTest methods untouched; cumulative NW115 = **19 new JUnit methods** across three batches.
- Source review: custom and hardware Next use the same policy, previous/no-selection check added, no download/network/source rights/permissions/signer/queue order/metadata schema changed. UI, Google Drive, history and offline files preserved. Real JVM full suite, Android MediaSession and Bluetooth headset/lock screen tests NOT YET VERIFIED.
- Focused independent Kotlin 1.9.0 / JDK 21 compiler-and-run smoke of the pure MediaSessionQueueControlPolicy decision matrix: **20/20 cases passed**. This checks boolean conditions and existing controller-name rules only; it is NOT the Android Gradle/JUnit suite or the new 5 JUnit test methods.
- NW115 remains IN_PROGRESS. Source-only tests are not proof of device playback. NW114 byte-range resume is still blocked until authenticated persistent checkpoints and independent full-file verification exist. NW125 public GO stays NO-GO.

## NW115 fourth implementation batch — 2026-10-27

- Protected parent NW115 Batch 3 commit: b051ae7a2a688ef19cc2f24b20c6132e88cc63f2. Isolated branch: feature/nightwave-nw115-http-recovery-fence. NW110–NW114 and all NW115 earlier branches' code is retained. No merge to default main.
- Detected error-recovery flaw: PlaybackService.onPlayerError() classified every ERROR_CODE_IO_BAD_HTTP_STATUS as transient. Permanent HTTP 401/403/404/416, provider source refusal and nonexistent audio could incur an unnecessary second extractor lookup and network delay. A genuine user Retry remains available; automatic recovery must be narrower.
- New PlaybackRecoveryPolicy.retryableHttpStatus() accepts only 408, 425, 429, 500, 502, 503, 504; null/unrecognized/permanent HTTP codes are fail-closed. Existing intermittent network timeout/connection failure remains eligible for ONE already-guarded refresh.
- PlaybackService inspects typed androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException within a bounded eight-link Throwable cause chain ONLY for ERROR_CODE_IO_BAD_HTTP_STATUS. It uses the typed integer code, **never raw failure messages or signed CDN URLs**, before applying the existing PlaybackRecoveryPolicy.mayAutoRetry() and acceptsPostedRetry() fences. Existing pause, timer, selected song, loaded Media3 identity, offline mode, pending lookup, generation and retry-once protections remain unmodified.
- Five extra JUnit methods added to PlaybackRecoveryPolicyTest.kt with 7 transient, 20 permanent and 7 unknown status-code fixtures, as well as pause/timer/selection/previous-attempt/stale-callback checks. All nine original PlaybackRecoveryPolicyTest methods retained. Cumulative NW115 tests: **24 added JUnit methods** over four batches; these new cases have NOT RUN via Gradle/JVM on Windows.
- Existing error UI still provides a redacted explicit Retry path; no new provider, source extraction, download rights, files, Google Drive, Android permissions, signer, media queue, EQ or Compose changes.
- Static review only until full compilation and tests. Source-level progress is not a release, runtime-success or Spotify-parity certificate. NW115 remains IN_PROGRESS pending MediaSession/headset/Bluetooth/screen-off, Repeat One, process death/restart and full clean Android/JUnit device matrix; NW114 persistent safe resume remains blocked pending legally authorized range/validator/checkpoint support.

## NW115 fifth implementation batch — 2026-10-27

- Baseline/source parent: `6615979e6116a73dd71212f925773ced87ea9a25` (NW115 Batch 4), isolated cumulative branch `feature/nightwave-nw115-idle-session-refresh`. All prior NW110–NW114 code and four NW115 batches preserved.
- Verified source-level interruption risk: a saved public Media3 MediaItem with a matching ID but STATE_IDLE is not evidence that its temporary signed stream URI is still valid. The old explicit Resume path re-prepared such an IDLE item rather than resolving its selected public watch ID; same-queue PLAY_QUEUE could do the same, or fall through to replacing the queue after a process/service restart.
- Added PlaybackResolutionPolicy.canResumeLoadedPublic(): reuse an existing Media3 item only if not IDLE, no playback error, the requested song is canonical and its public ID matches the exact loaded ID. Existing reusesLoadedTrack() remains unchanged for other callers.
- Added pure PlaybackResolutionPolicy.shouldResolveSavedSelection() for user-explicit, same-selected-queue PLAY_QUEUE commands that have neither reusable loaded media nor matching in-flight resolution. No automatic stream lookup on service creation, background attach or queue-only commands.
- PlaybackService now calls these policies in RESUME_SAVED and same-queue PLAY_QUEUE. A fresh resolution from the public ID retains the current selected queue, Shuffle history, manual Play Next priority, repeat settings, and existing bookmark; no signed expired URI reuse. Pending duplicate lookup continues to be deduplicated and playing valid loaded media remains instant.
- Added 9 deterministic PlaybackIdleSessionRecoveryPolicyTest JUnit methods (32 assertions) covering healthy same-ID resume, IDLE/empty loaded item, error/wrong-ID/signed-media denial, pending duplicate, queue-only false Play, no unrequested autoplay, preserved shuffle history/Play Next and linear Previous order. Source review 18/18 explicit checks passed; Android Gradle/JVM tests NOT RUN.
- Cumulative NW115 test declarations: previous 24 new methods + new 9 = **33 added JUnit tests** across five batches, not a claim those 33 have executed. All original tests preserved.
- Remaining acceptance: full clean Windows build, full JVM regression suite, real headset/Bluetooth, MediaSession notification, screen-off process death/restart, bookmark seek accuracy, old/new source expiry, imported-file offline playback and signature continuity. NW115 stays IN_PROGRESS, NW114 true Range resume still blocked pending source-authorized persistent checkpoints and integrity, NW125 release GO still NO-GO.

## NW116 first implementation batch — 2026-10-27

- Protected cumulative baseline: NW115 fifth-batch commit `3a3696dd6e3245697f3c4b7c7c3871aa6b627df7`. Isolated development branch `feature/nightwave-nw116-responsive-player-controls`; all prior NW110–NW115 features, tests, Google Drive, media sources, media permissions, signing and storage remain unchanged.
- Specific UI defect: full-screen Now Playing placed all 5 primary/secondary transport buttons (Shuffle, Previous, Play/Pause, Next, Repeat) in one fixed row. Narrow split-screen windows and very large system text settings can leave inadequate horizontal breathing room for its five accessible targets. Offline and Lyrics side-by-side labels can also truncate badly.
- Production UI: NightWaveNowPlayingLayoutPolicy gains read-only `splitTransportControls` (usable content width below 284dp or font scale >=1.8) and `stackQuickActions` (usable content width below 320dp or font scale >=1.6) decisions. Uses the established 14/20/28dp padding scheme; no player state or network use.
- Compose: standard-width Now Playing retains familiar five-control row. Split layout centers Previous/Play/Next on the primary row and places Shuffle/Repeat on a secondary spaced row; every button preserves its existing 48dp target and exact callback, semantics, state tint and Play icon. Compact/large-font layout stacks existing Offline and Lyrics cards full-width; normal phone still shows paired cards. Queue action, lyrics/editor, seek, controls and playback logic are unchanged.
- Test declarations: extend NightWaveNowPlayingLayoutPolicyTest by 6 new JUnit methods, 25 direct assertion calls; test normal 360/390/412/700dp, narrow 240–300dp, 320dp intermediate, font scales 1/1.6/2, exact width thresholds, NaN/Infinity and invalid measurements. All five previous LayoutPolicyTest methods retained, so 11 total in that suite.
- Independent exact-file policy smoke: copied NightWaveNowPlayingLayoutPolicy.kt from GitHub and matched its **git blob hash f5f3f024ca634617152b4e4dfbda0f0c633332a6** before compiling with Kotlin 1.9.0 / Java 21; all **145 independent policy assertions PASSED**. Initial external smoke detected a wrong new breakpoint expectation, corrected both the GitHub JUnit declaration and the smoke, and reran successfully. This confirms the pure policy only; it is NOT Android Compose compilation, a full JUnit pass or a device screenshot review.
- Remaining: Gradle/Compose compilation, full JVM tests, actual 240/320/360/390/412dp layouts at 1.0/1.6/2.0 system font scale, text truncation, TalkBack focus, reduced motion and physical device navigation. No claims of screenshot/performance/Spotify equivalence without device evidence.
- Status NW116 **IN_PROGRESS** and this batch **CODE_COMPLETE_UNVERIFIED**. NW115 real MediaSession/process-recovery tests and NW114 genuine authorized resume remain outstanding; NW125 public certification and source-rights sign-off remain NO-GO.
