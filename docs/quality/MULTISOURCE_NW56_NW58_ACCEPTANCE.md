# NightWave multi-source v56–v58 acceptance matrix

Cases start as **NOT_RUN**. Current NW56 results refer to the exact [implementation and device evidence](../MULTISOURCE_NW56_IMPLEMENTATION.md). See [NW57 candidate evidence](../MULTISOURCE_NW57_IMPLEMENTATION.md). NW58 remains planned.

This matrix belongs to the new **post-v55 public release track**, not the older historical internal NW56–NW58 docs. See [master contract](../MULTISOURCE_NW56_NW58_MASTER_PLAN.md), [S24 Ultra runbook](../MULTISOURCE_S24U_DEVICE_QA.md), and [status ledger](MULTISOURCE_NW56_NW58_STATUS.json).

## Definitions and release policy

- **P0 blocking**: failure, skipped/unknown or missing evidence blocks that batch's claim of full delivery and final public release. The model must not silently redefine P0. Some provider integrations can be explicitly BLOCKED for terms/quota, in which case they must be disabled and **not claimed shipped**.
- **P1 important**: fix before a quality-complete batch; if impossible, obtain explicit scope decision rather than ignore.
- **PASS** requires a real command/result or observed device action tied to exact commit + APK hash + test date + device. **COMPILED** does not equal EXECUTED. **NOT_RUN** is not PASS.
- Test at least one representative real query per enabled provider and multiple negative/fake fixtures; do not make a provider-specific live content item part of the deterministic build suite.
- No feature gets a full score from UI text/source inspection alone. Protect existing v55 suites, apply data-loss zero tolerance, and don't alter user accounts or personal data except verified in-place-safe operations.
- For each completed case maintain `test_id | status | date | commit | APK SHA | device ID | command/scenario | outcome | evidence file/link | blocker/notes` outside this plan in `docs/quality/MULTISOURCE_NW56_NW58_EXECUTION_EVIDENCE.md` (create at implementation time).
- Where a provider is not allowed to supply a capability at zero cost, use a tested **disabled/externally-open** state with honest text. The absence of a legal integration is not a failure to be fixed via scraper or bypass.

## Cases

| ID | Priority | Area | Pass condition | Required evidence | Status |
|---|---|---|---|---|---|
| 56-01 | P0 | Source ancestry | Build code base is a verified v55-tag descendant, not divergent main snapshot. | git rev-parse / merge-base + release hash | PASS |
| 56-02 | P0 | Baseline health | Unmodified tag builds locally and documented v55 suites run; failures isolated before feature work. | builder receipts + exact hash | PASS |
| 56-03 | P0 | Provider terms | For every source rights, free-tier, permitted playback/offline, attribution, credentials and limits recorded with dated links. | provider contract ledger | PASS FOR READ-ONLY DISCOVERY |
| 56-04 | P0 | No charge | No paid API, paid CI, paid proxy, billable backend, or automatic billing requirement. | gradle+network+account audit | PASS |
| 56-05 | P0 | Secrets hygiene | No bearer secret, refresh token or credential embedded in repo/APK/log. | source/config and APK scan | PASS TARGETED SCAN |
| 56-06 | P0 | Stable source ID | Two identical titles from different sources never collide; canonical URL preserved. | JVM fixtures | PASS |
| 56-07 | P0 | Capability defaults | Unknown/forbidden Offline, Stream or Play never enables action. | permission matrix unit tests | PASS |
| 56-08 | P1 | Audius real | Current approved read-only discovery shows genuine tracks, attribution, valid stream if authorized; quotas handled. | live response + S24 playback | DISCOVERY PASS · IN-APP PLAYBACK BLOCKED |
| 56-09 | P1 | PeerTube real | Genuine indexed videos; instance permissions, HTTPS and failure status respected. | two permitted instance fixtures + live | PASS DISCOVERY AND EXTERNAL HANDOFF |
| 56-10 | P1 | Radio Browser | Searches station directory; live station metadata/streams differentiated from tracks; no recording. | station selection S24 + network | DISCOVERY PASS · IN-APP RADIO BLOCKED |
| 56-11 | P1 | Jamendo conditional | Either licensed noncommercial discovery/streaming verified OR accurately marked BLOCKED; offline not exposed. | terms record + UI screenshot | BLOCKED AS DESIGNED |
| 56-12 | P0 | YouTube isolation | No expanded extraction, prohibited downloads/background, cross-provider API aggregation or mixed unlicensed streams. | source audit + UI navigation | PASS NO EXPANSION |
| 56-13 | P1 | Provider failure isolation | Malformed payload, 429, 503, DNS, timeout, zero matches affect only that source. | mocked integration matrix | PASS HOST FIXTURES |
| 56-14 | P1 | Cancellation | Rapid query replace cancels stale requests; only current generation renders. | delay/fast-query simulator | PASS HOST FIXTURES |
| 56-15 | P1 | Fast partial UI | One slow source does not block completed permitted sources; bounded concurrency. | timed mocks + Compose | PARTIAL HOST PASS · COMPOSE TIMING NOT RUN |
| 56-16 | P0 | Local regressions | Existing local imports, local search and original public references remain correct. | old tests + S24 golden corpus | PASS SCOPED · LOCAL/PUBLIC SEARCH, OWNED IMPORT, OFFLINE RESUME AND PAUSE |
| 56-17 | P1 | Correct provenance | Every card clearly names source, type, actual canonical destination and availability. | screenshot + link click test | PASS SCOPED |
| 56-18 | P1 | Consent/privacy | Only enabled providers receive query; third-party destinations and retained metadata documented. | traffic logging + settings | PARTIAL HOST ROUTING AND DEVICE UI · WIRE CAPTURE OPEN |
| 57-01 | P0 | Filter behavior | All supported provider tabs/Local/Radio distinguish source; unsupported sources not deceptively enabled. | Compose/navigation tests | PASS SCOPED S24 |
| 57-02 | P1 | Real result identity | Title, artist, artwork and duration only from actual provider or explicitly marked unknown. | response fixture vs UI | PASS SCOPED LIVE DISPLAY |
| 57-03 | P0 | No wrong-song plays | Duplicate titles across sources/versions remain distinct, exact selected ID plays. | cross-source same-title cases | PARTIAL EXACT LINKS AND HOST IDENTITY · PROVIDER PLAYBACK OPEN |
| 57-04 | P1 | Bangla/English search | Mixed scripts and repeated/typo terms never break local search; source changes reset requests correctly. | frozen query corpus | PASS HOST BANGLA FIXTURE · DEVICE OPEN |
| 57-05 | P1 | Stable pagination | No item duplication/omission on source page changes, filter switches or rotation. | paging + lifecycle tests | PARTIAL BOUNDED EXPANSION · PROVIDER PAGING NOT RUN |
| 57-06 | P0 | Library migration | v55 backup/import/library IDs survive upgrade; no loss of saved references, profile, playlists, history. | before-after byte+semantic comparison | PARTIAL 33 FILES MATCHED · RESTORE NOT RUN |
| 57-07 | P0 | Queue continuity | Background/resume, repeat, shuffle, Play Next, skip and process restart preserve selected source identity. | JVM + device queue cases | PARTIAL EXISTING OFFLINE RESUME · PROVIDER QUEUE OPEN |
| 57-08 | P1 | Radio queue semantics | Radio isn't treated as a finite track; stop/Next/notifications truthful and responsive. | instrumented/player session | BLOCKED · RADIO EXTERNAL ONLY |
| 57-09 | P0 | Offline capability | Only owned or explicitly permitted recordings can download; Jamendo no-offline constraint respected. | negative rights/unit/UI tests | PASS NO NEW THIRD-PARTY DOWNLOAD |
| 57-10 | P0 | Offline truth | Saved reference is not labeled downloaded; airplane mode plays only available files. | offline S24 test | PASS SCOPED LINK LABEL AND STORAGE · AIRPLANE NOT RUN |
| 57-11 | P1 | Download robustness | Authorized transfers pause/retry/resume only with server support, file hash/integrity verified. | forced disconnect/range fixtures | NOT_RUN |
| 57-12 | P1 | Performance | Search and scrolling not worse than measured v55 baselines beyond documented justified variance. | same device benchmark receipts | NOT_RUN |
| 57-13 | P1 | Accessibility | 48dp targets, TalkBack labels, contrast, large font and reduced motion on primary search/play views. | screen reader/device evidence | PARTIAL 48DP CONTROLS · DEVICE ACCESSIBILITY OPEN |
| 57-14 | P1 | Dark/light/theme | New provider UI remains consistent across NightWave themes and mini-player layouts. | 4-theme screenshot matrix | PARTIAL S24 AMOLED · OTHER THEMES OPEN |
| 57-15 | P1 | Lifecycle correctness | No stale results after app background/foreground/rotation or killed request. | instrumentation cases | PARTIAL HOST LIFECYCLE GUARDS AND SAVED LINK RELAUNCH |
| 57-16 | P0 | No credential collision | No provider account login hijacks Google Drive guest/account consent or backup scopes. | app-only auth/backup checks | PASS NO NEW CREDENTIAL OR ACCOUNT SCOPE |
| 58-01 | P0 | Windows build | Fresh branch builds via build-windows.cmd; resulting APK provenance and SHA captured. | dist/build status and fingerprints | NOT_RUN |
| 58-02 | P0 | Full host suite | All relevant JVM/source checks and lint pass with exact counts and source hash. | raw reports, no test disable | NOT_RUN |
| 58-03 | P0 | Android execution | Instrumentation tests actually run on S24, not merely compile; failures and skipped tests reported. | runner output and serial | NOT_RUN |
| 58-04 | P0 | Safe in-place update | Installed vs candidate signer match, backup verified, no data loss after install/restart. | check-device-update + before/after | NOT_RUN |
| 58-05 | P0 | Core S24 journeys | Home/Search/Library/You/Now Playing/queue/local/offline player work without regression. | scenario recording/screenshots | NOT_RUN |
| 58-06 | P0 | Provider E2E | Search/open/play flow genuinely verified for each enabled provider (or clearly marked blocked). | device result log with IDs | NOT_RUN |
| 58-07 | P0 | Graceful outages | Offline, DNS, 429/503, wrong JSON and dead radio do not crash or substitute wrong source. | controlled failure tests | NOT_RUN |
| 58-08 | P1 | Soak/perf | Replay/search/pause/resume >30 minutes with recorded crashes, jank and memory vs v55. | Perfetto/logcat/baselines | NOT_RUN |
| 58-09 | P0 | Privacy & rights | Updated third-party notice, source attribution, no prohibited caching or claim of full popular catalogs. | manual policy review | NOT_RUN |
| 58-10 | P0 | Release integrity | Exact version, signer, SHA, published notes and update feed action verified separately; no accidental feed flip. | pre-release audit receipts | NOT_RUN |
| 58-11 | P0 | Remaining gaps | All FAIL/BLOCKED/NOT_RUN listed honestly; no '100%' for untested results. | status/QA report | NOT_RUN |
| 58-12 | P0 | Owner go/no-go | No public APK release/update-channel activation without explicit release approval. | release checklist | NOT_RUN |

## S24 Ultra scenario seeds

**Search:** `c418`, `LilyPad Minecraft`, `blue yung kai`, a Bangla title, a rare exact artist, a same-title remix/live duplicate, zero results, very long input. Results must preserve their true provider and version. Do not promise these exact recordings exist on each source.

**Offline:** one local MP3 imported by user, one playlist reference that has *no file*, one disabled third-party download, and one separately verifiable permitted audio source. Confirm labels, play controls and airplane-mode behavior without losing files.

**Playback/recovery:** rapid Play/Next/Previous, pause/lock/screen-off, incoming media controls, shuffle/repeat, activity recreation, termination/reopen (after baseline snapshot), queue restoration and no random new tracks.

**Privacy/security:** select a source filter and confirm network requests are scoped to that permitted provider; inspect no auth tokens in logs/screenshot/Git; never auto-save non-permitted audio.

**Device:** owner has a Samsung S24 Ultra connected to Windows; connectivity, USB debugging authorization and target package must be observed. No permission to wipe/reset, re-sign under a new key, remove existing app, change system animation scale or alter Google accounts. Before installed-app replacement run exact signed-candidate preflight, preserve a verified owner-approved backup and stop if any signer mismatch.

## Acceptance progression

- NW56 can close only when all its P0s pass and every enabled provider meets NW56-08/09/10/11 or is precisely documented as BLOCKED/unsupported.
- NW57 can close only when its P0s pass and the merged search/queue/library changes survive phone regression (otherwise *source complete / device pending*).
- NW58 cannot close or ship with any unresolved blocking P0, unknown signer, unpublished privacy/rights problem, unverified on-device compatibility or missing owner release approval.
- **Absolute target:** 100% of **executed, defined, applicable tests** passing. No claim of universal accuracy, permission, uptime or zero bugs without evidence.
