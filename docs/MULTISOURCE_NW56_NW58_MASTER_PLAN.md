# NightWave v55 -> v56/v57/v58: zero-billing multi-source execution contract

**Status:** PLANNED / NOT IMPLEMENTED / NOT TESTED. **Owner request:** three high-capacity *mega-batches*, all quality gates retained; absolutely no paid hosting, paid APIs, subscriptions, cloud runners, or unattended purchases.

> **Important version collision:** Here `NW56`, `NW57` and `NW58` mean the **new public-release version track following [NightWave v55](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55)**. The existing historical files `docs/NW56_THEME_CONTRAST.md`, `NW57_HOME_FIRST_FOLD.md` and `NW58_PLAYER_OVERLAY.md` describe **older internal development batches**, and MUST NOT be treated as these deliverables or overwritten.

## Agent start instructions (read in this order)

1. Read this entire contract, [acceptance gates](quality/MULTISOURCE_NW56_NW58_ACCEPTANCE.md), [Samsung workflow](MULTISOURCE_S24U_DEVICE_QA.md) and [machine-readable status](quality/MULTISOURCE_NW56_NW58_STATUS.json).
2. Inspect current Git refs, existing open PRs, release, `AGENTS.md`, `TEST_REPORT.md`, build scripts and Android tests. **Never rely on old "next NW133" text:** it belongs to the historical track. Do not assume `main` contains the release APK source.
3. **Source identity:** public v55 tag `nightwave-v55` points at `c569de64cdd618fb6e5593e9b6bc778903a7588e`, matching release asset `NightWave-v55.apk` (SHA-256 `dd0ee037e8949c7325323de3d2ba797f50703937e5b73494f92cd7065befcede`; published 2026-11-06). At roadmap creation `main` was `8417cf770aee48bad15756ccb3e066f3888b7e22`; the two histories **diverge**. Resolve the latest authoritative release/source state again before modifying source. Start product work on a **fresh branch from the verified v55 tag**, or from a verified descendant of that tag if a later release exists. Do NOT blindly cherry-pick/merge `main`, historical NW56–58, or stacked PR branches over the v55 implementation.
4. Preserve `com.nightwave.player`, signing continuity, application data, guest accounts, existing playlists/history/imported audio, Drive scope/consent, user settings, themes, mini-player, background playback behavior where lawful, queue ownership, lyrics, search URLs, update integrity and published release identities. Do not delete/rewrite historical docs or unrelated files.
5. Complete NW56, then NW57, then NW58. Each mega-batch may have **internal implementation commits** and tests; do not create 10 separate public deliveries. One reviewable PR per mega-batch (or cumulative, explicitly documented PR stack) is sufficient. **NEVER trade verification for batch size.**
6. Update the status JSON + acceptance evidence after each step, with exact source SHA, APK SHA, Android device identity, test commands and actual PASS/FAIL/NOT_RUN. Run no paid GitHub Actions; use Windows laptop locally.
7. Provider functionality is conditional upon **documented legal/API permission and empirically tested availability**; unsupported capability is a clearly labeled disabled/open-in-provider action, NEVER a fake download/play button. If permissions cannot be established at zero cost, mark BLOCKED, retain UX and continue with other providers. Ask the owner only for indispensable account/consent actions.

## Promise and limits

Goal: a polished local-first NightWave search/listening app discovering music across **permitted free providers** with a single consistent entry point. **Not** a promise of Spotify/YouTube/SoundCloud catalog parity; **not** all popular platforms as unrestricted in-app streams; **not** 100% uptime or guaranteed bug freedom. Strict build and test pass + physical acceptance are measurable standards. There must be **zero mandatory recurring developer expenditure**. A provider free tier can have quotas or outages; disable gracefully when exhausted and never auto-enroll in a paid plan.

## Provider capability/rights matrix — recheck official terms before coding

| Source | Discovery | In-app playback | Offline | Decision / hard gate |
| --- | --- | --- | --- | --- |
| Existing local/imported audio | YES | YES | YES | Preserve SAF/import permissions and local media ownership. |
| Existing YouTube/reference path | Preserve existing only pending rights review | Follow allowed official player behavior | **NO new YouTube download/cache/audio-extraction flow** | Do not expand existing extractor or background/download mechanisms without provider/rights review. YouTube API policies restrict audiovisual download, audio separation, background play and API data aggregation; treat other platform results as **separate labeled sections**, not YouTube API data blending, without approval. |
| Audius | Candidate YES (free read-only API) | Candidate YES (documented stream API) | NO until explicit permission and technical evidence | Check free request allowance and app-key restrictions. No secret in APK/repo. Do not assume unlimited requests; a free key/account may require owner setup. |
| PeerTube | Candidate YES | Candidate YES for playable instance/video | Only if content license + instance rules + API actually allow | Instance-specific availability, safe HTTPS links, instance/global index consent and failure. Respect instance controls, no unrestricted crawls. |
| Jamendo | Candidate YES for noncommercial project with registered client ID | Candidate if current terms and project use qualify | **DISABLED by default** | Current API terms prohibit apps specifically designed for caching/offline access, regardless of per-track `audiodownload_allowed`. No offline integration without a written rights/terms reconciliation. No ads/monetization under noncommercial plan. |
| Radio Browser / stations | Candidate YES | Candidate live stream subject to station rights and codec | **NO recording/offline radio** | Community stations are live streams, not normal track catalog; separate Radio view, correct User-Agent, no uptime guarantees. |
| SoundCloud | External open-in-provider only | **NO aggregate in-app player** without separate permission | NO | Current API integration may require Artist Pro and terms restrict mixed on-demand services. Do not scrape, impersonate, extract or hide this limitation. |
| Spotify / Bandcamp | External open only where official links available | No unauthorized embedded catalog | NO | Metadata and streaming entitlements are not interchangeable. No fake provider tabs that imply unsupported integration. |
| MusicBrainz (optional) | Metadata/enrichment only | NO | NO | Respect rate limits, attribution, noncommercial policy and data license. Never synthesize an audio stream from metadata. |

Official starting points (verify again on implementation day):
- Audius: https://docs.audius.co/api/ and https://docs.audius.co/sdk/
- Jamendo: https://devportal.jamendo.com/api_terms_of_use and https://developer.jamendo.com/v3.0/tracks
- PeerTube: https://docs.joinpeertube.org/api-rest-reference.html and https://docs.joinpeertube.org/use/search
- Radio Browser: https://docs.radio-browser.info/ and https://api.radio-browser.info/
- YouTube: https://developers.google.com/youtube/terms/developer-policies
- SoundCloud: https://developers.soundcloud.com/docs/api/terms-of-use and https://developers.soundcloud.com/docs/api/

**Never place client secrets, Google tokens, OAuth refresh tokens, user audio, app backups, screenshots, private logs or APKs into Git.** A public client identifier is not a license to bypass server-side security. Respect provider rate/retention policies and attribute original services.

## Architectural contract (NW56 foundation)

Implement only after reading the actual v55 app's Kotlin/Compose/Media3 call sites, especially `app/src/main/java/com/nightwave/player/net/`, `PublicMusicSearchRunner`, `YouTubeGateway`, `CommunityVideoSearch`, offline source-capability policies, queue/state stores, and `app/src/test` / `app/src/androidTest`. No arbitrary wholesale rewrite.

Proposed *conceptual* abstractions (adapt naming to real code):
- `SourceId` / stable `SourceItemId(provider, remoteId, canonicalUrl)`, with collision-proof identity; local ID never reused for remote results.
- `SourceResult`: title, creator, artwork attribution, duration if verified, source URL, result type (TRACK/VIDEO/RADIO/LOCAL), source provenance and timestamp.
- `SourceCapabilities`: independent tri-state SEARCH / PLAY_IN_APP / OPEN_EXTERNALLY / DOWNLOAD_FOR_OFFLINE (each AVAILABLE/UNAVAILABLE/UNKNOWN plus reason). **Default UNKNOWN must never enable Download.**
- `SourceSearchAdapter`: bounded suspend/cancellable search; total and per-source timeouts; explicit `Success/Empty/Quota/Unavailable/Unsupported/Auth/Permission/Network` states; never one provider exception crashing global search.
- `SourcePlaybackResolver`: fresh per-source capability and playable URL only after supported permissions checks; no audio extraction from prohibited sources.
- `SearchCoordinator`: bounded parallelism, cancellation when query changes, stale-result suppression with generation token, source-scoped pagination/caching and endpoint fallback only when allowed.
- `SourceLink` / provenance metadata persisted through playlists, history, queue, share and backup/restore with explicit schema migration and backwards read/write compatibility.
- No global dedupe by title alone; preserve variants (live/cover/remix), creators and exact original links.

Resource budget: no own backend, no GitHub runner billing, no external analytics, no paywall, no paid provider, no unbounded fanout. Use phone-side cache for *permitted metadata* with TTL, bounded concurrency, request limits, exponential or provider-guided retry with jitter, and data-saver support. Keep material user information local. Cloud-free must remain functional offline.

## NW56 — Multi-Source Engine (merges original 56–60)

**Deliver as one integrated batch.**
1. Audit v55 API/rights paths and build baseline; create a provider contract ledger with official docs, costs, allowed features, credential handling, rate limits and tested endpoints.
2. Implement source model, adapters, coordinator, typed result/error/capability contracts, robust cancellation and safe URL handling; preserve exact v55 local and public searches.
3. Add and validate Audius; PeerTube; Radio Browser; Jamendo **only if its terms are compatible with this noncommercial experience**. Integration with unapproved source remains disabled/externally linked instead of simulated. Respect free API quota. Where required obtain an account/key via a **manual owner setup checkpoint**, not a paid workaround.
4. Expose labeled per-provider results/source status and an accessible Radio section; no invented titles/audio/artwork. Partial results render promptly, missing provider remains visibly unavailable. Do not combine YouTube API data in a prohibited way.
5. Unit tests: models/IDs, response decoding, timeout, cancellation, 429/offline/malformed/SSL/redirect, negative-rights matrix, source isolation, zero-charge assertions. Device build and targeted smoke check.
**Exit:** baseline build PASS, new tests PASS, permitted provider(s) live smoke PASS or explicitly BLOCKED with evidence, old playback/local search unchanged on device. Do not call a blocked provider "implemented."

## NW57 — Unified Experience (merges original 61–63)

**Deliver as one integrated batch, after NW56 gates.**
1. One search entry with "All permitted", Local, Audius, PeerTube, Radio and conditionally Jamendo chips; active filters persist correctly, accessible horizontally, clear loading/empty/fail states, responsive Bangla/English input and screen readers. Underlying disallowed data-combinations remain grouped/isolated.
2. Relevance: local/owned relevance, multilingual normalization, robust query matching, source-specific ranking, type-aware dedupe; group only *verified* duplicate tracks and preserve clickable source/versions. Never auto-substitute or play the wrong song. Explicit selection of source on playback when needed.
3. Playlist/library/queue/Now Playing/mini-player: correct stable IDs and provenance, local radio stream behavior, correct end/skip/Next/repeat semantics, refusal of unsupported continuous/radio queues, and reliable restoration after process death. Existing guest/Drive backup format preserved through versioned migrations.
4. Offline: continue local import and already-authorized offline functionality without regressions; source-specific Download/Save controls only when **both provider permission and recording rights** are verified. "Save to playlist" means reference unless verified audio is locally available. Jamendo API flags do not override its no-offline-app limitation. No generic "save any track" promise.
5. UI: reuse NightWave identity, theme, compact mini-player, light/dark contrast, 48dp targets, large fonts, motion reduction; avoid turning features into overgrown panels.
6. Tests: search correctness/cancellation, no bleed across provider filters, ID migration/restart, backups and playlists, offline capability gates, queue and player regression, accessibility/Compose instrumentation.
**Exit:** integrated tests PASS; stable result source/open/play/status truthful; no old-user library loss; phone E2E routes PASS or marked NOT_RUN.

## NW58 — Reliability, Security & Public Release Gate (merges original 64–65)

**Deliver as one integrated batch, after NW57 gates.**
1. Run host unit/integration suite, source checks, Android lint, Android instrumentation (ACTUALLY EXECUTED, not just compiled), release build and signature/provenance checks. Compare coverage to the v55 **1,870 JVM + 94 source checks** baseline without treating counts alone as quality.
2. S24 Ultra: exact device discovery, signer preflight, preserving app data, in-place install only when safe/authorized; search each available source, fallback, query cancellation, first result correctness, player ownership, background and lock-screen where lawful, offline/airplane tests for owned audio, queue/restart, themes, Drive guest/optional consent, update path, error UX and restore. Avoid modifying user settings/accounts. See [device runbook](MULTISOURCE_S24U_DEVICE_QA.md).
3. Network adversarial matrix: zero connection, DNS/timeout/429/503, stale results, provider removal, invalid JSON, slow transfer, metered Wi-Fi, duplicate titles and provider-specific takedowns. Ensure no cross-provider query leakage beyond explicit requests; no secret leakage.
4. Measure startup/search latency, frame jank and RAM/battery on same build/device conditions; improve only on grounded traces, no invented "Spotify-level" percentages. Soak test pause/background/resume/rotation and no fatal crashes.
5. Audit third-party license/terms, user privacy text/data destinations, user consent and accessibility. Document remaining provider gaps. No release until blockers close.
6. Release candidate: versionName/versionCode migration, update channel and signing key verification, staged APK receipt, README/release notes, SHA256. **Do NOT publish new release or flip signed feed as a side effect**; public release only after explicitly authorized final go/no-go and all blocking gates.
**Exit:** acceptance matrix blocking cases PASS with verified evidence; unresolved cases remain transparently NOT_RUN/BLOCKED/FAIL and prevent "100% complete" claim.

## Working rules for a high-capability coding agent

- Spend compute on implementation **and** test feedback, not inflated test-count claims. Favor small internal checkpoints inside each mega-batch; unit boundaries may be separate commits without separate "updates".
- Inspect code shape and data migrations before editing; run tests after each subsystem integration; fix causes, not weaken assertions. Keep baseline data/signing, no reset/uninstall on the owner's device.
- If a provider becomes paid, rate-limited beyond practical use, or disallows feature: **do not subscribe or bypass**. Disable provider and note gap/possible external deep-link alternative.
- Never say "100% accurate" without defining the tested corpus; use an evidence ledger (source commit, real endpoint responses, exact playback ID, run time, proof on S24 Ultra). Device-connected is not device-tested.
- Report exact changed files, build path/hash, install/app version, tests PASS/FAIL/NOT_RUN, screenshots/logs kept privately, unverified restrictions, and next state.
- If unexpected missing repo setup/credentials/device authorization, implement every safe independent part and clearly record the blocked checks rather than inventing success.

**Scope freeze:** this roadmap is documentation only; until someone implements/validates the batches, none of NW56–NW58's multi-source features should be advertised as shipped.
