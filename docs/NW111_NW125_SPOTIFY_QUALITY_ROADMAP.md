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
| NW111 Music-first search | IN_PROGRESS | New regression and source scorer planned on this branch |
| NW112–NW125 | PLANNED | No new work executed; status does not imply functionality |

After every wave update this ledger with commit, automated receipts, remaining blockers, actual device acceptance and next planned wave. Avoid recalculating a "Spotify score" based on static code checks.

## Source references

- [Spotify Premium benefits](https://support.spotify.com/us/article/your-premium-benefits/) (lossless on eligible devices and settings)
- [Spotify transitions](https://support.spotify.com/kr-en/article/tracks-transitions/) (gapless, crossfade, Automix)
- [YouTube API developer policies](https://developers.google.com/youtube/terms/developer-policies) (audiovisual-content downloading/offline restrictions)
- [YouTube policy compliance guide](https://developers.google.com/youtube/terms/developer-policies-guide)

The roadmap defines future turns and test requirements; it does not schedule autonomous background coding.
