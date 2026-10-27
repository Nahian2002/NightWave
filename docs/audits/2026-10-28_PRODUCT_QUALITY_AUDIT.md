# NightWave product audit — 28 October 2026

**Provisional comparative product score: 6.7/10.** The existing player, visual identity and optional backup are useful foundations. Repeated controls, uneven information density, ambiguous source/recording identity, and disconnected offline journeys keep the experience below a polished premium music app. The next target is **9.5+ within NightWave's declared scope**, assessed through real task outcomes and device evidence.

This is a demanding subjective review, not a measured Spotify score or a guarantee that every app function has been exercised. The earlier roadmap's 7.0 estimate used a different recording/rubric; subtracting these numbers would not measure regression.

Active follow-up: [product roadmap](../PRODUCT_QUALITY_ROADMAP.md), [structured backlog](../quality/NW126_NW145_BACKLOG.json), [acceptance cases](../quality/NW126_NW145_ACCEPTANCE.csv).

## Evidence and scope

- Source inspected: cumulative NW125 `61703c98238e762e59c049b6690853431f822884`, including remote NW123 accessibility and NW124 extras. The audit branch is `audit/nightwave-post-nw125-quality-roadmap`.
- Owner recording: `Screen_Recording_20261028_011340.mp4`, 131,441,674 bytes; SHA256 `c4dad9d38fa5c45983eff477500a8effbed988dd5c4c67cf07ae6b18fc90ba17`.
- Reported dimensions: 1080×2340; 16,548 frames at approximately 61.126 reported fps; frame-count duration approximately **270.72 seconds**. These are container/decoder metadata, not an app frame-rate measurement.
- Visual method: timestamp-seek review across the entire recording at one-second intervals, coarse five-second contact sheets, and full-resolution stills for key screens. This is visual sampling, not continuous frame-by-frame playback or an audio-quality audition. Timestamps below are approximate recording offsets.
- The recording shows the app's `2.0.0-alpha5s` label and NW124's playlist-document action. It does **not** expose a unique APK hash/source commit, installed signer or build provenance. Therefore the recorded binary cannot be proven identical to the inspected NW125 commit.
- Reviewed Home, Search/public fallback, creator browsing, Library/history/derived collections, You/profile, themes, backup UI, EQ, Android media notification, full/mini player, offline catalog/library/downloads, cancellation and seeking. Reviewed their production wiring and relevant policy/test boundaries.
- No agent installation, app-data reset, real Google operation, live download, phone instrumentation, acoustic A/B or background soak was performed. No private recording/frame/account data is committed. Local review images and original failure receipts are in the private audit checkpoint.

## What already works well

The midnight/mint identity is recognizable. Four persistent tabs and the mini-player give the main Compose screens continuity. The expanded player has a clear play/pause focus, track-bound seeking controls and visible shuffle/repeat state. The searched song appears among the top matches and becomes the selected player item. Search history has an observed Undo affordance. Theme changes include AMOLED, Midnight and a briefly visible Pearl theme.

At approximately **01:34**, the backup screen displays a new verification timestamp and a "Cloud backup uploaded and verified" message, later reflected as Backed up in You. This is meaningful observed feedback. It does not independently prove remote archive contents or successful restoration. At **02:09–02:14**, Android's media card shows the same playing item and advancing position while NightWave is backgrounded; this supports short background-session continuity, not a long soak or Bluetooth correctness.

The download progresses, and after the owner cancels around **04:02–04:03**, it correctly displays a cancelled/no-partial-track-saved outcome. Cancellation is not counted as a failed completed download. The empty offline library does not fabricate ready files. Source already contains strict LRC parsing, EQ identity/readback safeguards, consent-fenced backup, URL preservation and numerous playback/library regressions; these must survive visual work.

## Scorecard

Scores grade the visible product experience and available feature paths. They do not grade unobserved engine correctness. H = repeated visual evidence with source corroboration; M = limited visual sample and source; L = mainly availability/discoverability, with the full workflow untested. Weights total 100. Weighted sum is **6.655**, reported as **6.7** to avoid artificial precision.

| Area | Weight | Score / 10 | Confidence | Main reason and next improvement |
| --- | ---: | ---: | --- | --- |
| Visual identity and coherence | 8 | 7.5 | H | Good original dark/mint foundation; legacy tools and dialog surfaces need the same visual contract. |
| Compactness and useful screen space | 8 | 6.0 | H | Large repeated cards/actions and stacked chrome crowd the music; reduce duplication while retaining readable text and 48dp targets. |
| Typography and visual readability | 6 | 7.0 | M | Better hierarchy exists; truncation, image-overlaid text and secondary-copy density need screenshot/contrast acceptance. |
| Navigation and user control | 8 | 7.0 | H | Four main tabs are familiar; offline tool loops and repeated settings/history routes complicate the map. |
| Home usefulness and discovery | 8 | 6.5 | H | Real history-derived mixes are honest; generic Daily Mix labels, duplicated collection summaries and management controls dilute the listening focus. |
| Search and result decisions | 10 | 6.5 | H | Relevant Play Date results appear; duplicates are hard to distinguish, waiting is lengthy in this sample, and source wiring truncates the local corpus. |
| Music identity and artwork | 6 | 6.0 | H | Some clean titles; uploader labels/raw promotional captions and enlarged low-resolution video stills remain. |
| Now Playing experience | 10 | 8.0 | H | Strong primary controls and clear selected song; artwork quality, secondary actions and visible queue context need refinement. |
| Library and collection management | 8 | 7.0 | H | History, sort/filter, creator groups and document actions exist; empty/duplicate entry points and Saved versus Recent terminology need clarity. |
| Queue experience and discoverability | 5 | 6.5 | L | Queue entry and single-item state are visible; no multi-item add/reorder/remove/Undo demonstration, so operational correctness is unscored. |
| Offline and transfer journey | 7 | 5.0 | M | Progress/cancel truth is good; different screens, repeated navigation and restart-from-zero pause semantics fall short of premium convenience. |
| Lyrics availability and onboarding | 4 | 4.5 | M | The main online listening path offers a local-audio restriction; an actual imported lyric view is not demonstrated. Timing accuracy is unscored. |
| Sound Studio usability | 4 | 7.0 | M | Real bands/capability feedback and guided curves exist; draft/active state, duplicate preset concepts and long Apply paths need simplifying. Sound quality is unscored. |
| Account and recovery experience | 4 | 7.5 | M | Verified-looking backup feedback is a strength; tall explanation blocks, broad busy disabling and untested restoration limit confidence. |
| Personalization and preferences | 2 | 7.5 | M | Themes, motion and local profile are present; full-theme usability and preference persistence need wider acceptance. |
| Copy, empty/loading/error states | 2 | 6.0 | H | Honest copy exists, but repeated guidance and engineering vocabulary compete with primary music tasks. |

### Critical dimensions deliberately not assigned a numerical grade

| Dimension | Current evidence | Required proof |
| --- | --- | --- |
| Long-term reliability | 1,484 local JVM tests; short selected-song/background video sequence | Multi-device restart, process death, calls, headset/Bluetooth changes and long soak. |
| Android performance/battery | NW122 host microbenchmarks; roughly 01:01–01:07 loading UI in this recording | Controlled ART startup/search/next, Perfetto/frame traces, memory and matched-power battery runs. |
| Accessibility | NW123 semantics/layout source; min-target policies and JVM tests | TalkBack/Switch Access, 1×/2×/3× type and contrast review on actual rendered screens. |
| Offline integrity/resume | Worker/integrity policies and truthful cancellation | Complete authorized download/import, airplane-mode restart/seek/skip, corrupt/missing file and range/ETag tests. |
| Restoration/data safety | Existing guarded production code and isolated-client tests; displayed backup success | Disposable live backup/readback/preview/restore, merge, stale consent and low-storage trials. |
| Privacy/security/release | Narrow app components, local-first choices and prior release documents | Separate current security/privacy/dependency/history review, installed signer and applicable source/distribution approval. No new security audit is claimed. |

These unverified dimensions block a 9.5 certification. They are not evidence that each subsystem is defective.

## Feature maturity and scope decisions

| Capability | Present evidence | Roadmap decision |
| --- | --- | --- |
| Guest listening and personal library | Local profile, history and collection screens visible; persistent stores compiled/tested | Core: retain no-account access and make deliberate saved membership clear. |
| Public-reference search and playback | One successful query/play sequence plus explicit web fallback | Core: improve source/version decisions, coverage, cancellation and provider resilience; never imply a licensed complete music catalog. |
| Owned local audio and offline files | Import/integrity/player implementation present; zero ready files shown | Core: demonstrate the complete import/verification/airplane-mode listening journey. |
| Playlist creation, sorting, folders and bulk edits | Source present; history/creator sorting surfaces visible; full edits not exercised | Core: simplify the overview and execute safe edit/Undo and document roundtrips. |
| Play Next, shuffle/repeat and queue edits | Service/policies compiled with regressions; single-item captions and toggles visible | Core: verify multi-item behavior, process persistence and external controls before adding new queue modes. |
| Synchronized lyrics | Owned LRC parser/editor present; current public-reference lyric entry restricted | Core: remove the dead-end through a clearly scoped user-owned import workflow. Provider text/translation needs its own permitted integration. |
| EQ, transitions, speed and sleep timer | EQ and preferences visible; implementations and tests present | Core: make active state understandable, then prove sound/route behavior; do not label source audio lossless or enhanced without evidence. |
| Recovery and optional Google account | Displayed backup verification, guarded client/store code and isolated tests | Core: real disposable restoration and honest metadata-versus-audio scope. Metadata recovery is not automatic cross-device playback sync. |
| Listening personalization | Real local-history/saved-reference mix engines and optional memory | Core: better useful distinctions, not fabricated genre/artist knowledge. Service-scale recommendation research is separate. |
| Notifications, widgets and available car surfaces | Android media notification visible; additional integration source exists | Verify existing features in NW144 and preserve route/session authority. A widget is not a Connect-like remote session. |
| Verified albums/artist catalogs, collaborative listening, remote device handoff | Not established by this recording or this review as a complete integrated service | Separate provider/backend/product scope. Do not gate a focused player's 9.5 experience on copying every Spotify business or add placeholder features. |
| Themes, accessibility and bilingual experience | Themes/motion visible; semantics source; UI mostly English | Core: keep readable density, actual assistive-technology acceptance and resource-based Bangla/English primary flows. |

## Recording evidence timeline

| Offset | Observation | Interpretation limit |
| --- | --- | --- |
| 00:00–00:03 | Launcher, splash, then populated Home | One apparent launch; cannot classify cold/warm or derive a startup percentile. |
| 00:04–00:33 | Home/Search swiping, mood buttons, creator groups and local references | Repeated navigation and heavy empty-search hierarchy; frame splits during swipes are transitions. |
| 00:34–00:45 | Library overview and history-derived collections; 0 liked songs/0 playlists | Existing recent history does not imply a deliberately saved music collection. |
| 00:46–00:54 | You rows, local listening tools, advanced storage and diagnostics | Useful recovery tools are prominent alongside everyday preferences. |
| 01:00–01:12 | Submit Play Date; loading, two near-identical top results, finding/buffering audio, then playing | Roughly several seconds of provider waiting; one query/network condition cannot isolate CPU, provider or network cause. |
| 01:22–01:35 | Profile editor, checked account, backup busy, then verification timestamp/message | Observed UI success; no restore, merge or independent account-state inspection. |
| 01:36–01:41 | EQ waits for capability, shows Off, guided/manual curve and Apply | Preview/draft movement does not demonstrate an applied audible change. |
| 01:43–01:47 | Theme selection including a light theme | Brief render only; no complete light-theme acceptance. |
| 01:49–02:03 | Listening summary, Room/storage preview, playback health and crash summary | Displayed old process-exit reasons are not new crashes during this recording. |
| 02:06–02:17 | Home screen, notification media card, return to app | Short background playback-state continuity; no sound-route/long-duration proof. |
| 02:19–02:43 | Search/local suggestion, explicit YouTube web fallback, search-history removal/Undo | Web fallback is reachable, but introduces another surface and vocabulary. |
| 02:44–03:28 | Creator group/history browsing, sorting controls, Home mixes and mood cards | No actual playlist creation, import/export roundtrip or destructive-history confirmation is exercised. |
| 03:29–03:35 | Full player, offline-options confirmation | Secondary action creates a separate eligibility/download journey. |
| 03:36–04:08 | Downloads/catalog/offline-library navigation; progress to roughly a quarter, pause/re-entry, cancellation and dismissal | No completed offline copy; changing progress around pause/re-entry needs job-ID tracing before alleging a progress bug. |
| 04:09–04:30 | Player seeks, shuffle/repeat changes, queue captions and return to Library | Visible position/state changes; no multi-item queue or audible seek/transition validation. |

## Prioritized findings and source anchors

P0 = an actual validation blocker or release gate. P1 = important core-flow work. P2 = polish or optional depth. V = video observation; S = source finding; U = unverified acceptance. Proposed improvements are not assertions of a currently reproduced crash.

| ID | Priority / evidence | Finding and action | Source or recording anchor |
| --- | --- | --- | --- |
| Q01 | P1 V/S | Home repeats Continue, library summaries and collection entry points. Choose one primary listening action and a smaller set of useful shelves. | 00:12–00:20, 03:09–03:20; `NightWaveStudioActivity.kt` StudioHome, `NightWavePremiumChrome.kt`. |
| Q02 | P2 V/S | Persistent options/X rows beneath each recent cover turn a listening shelf into a management shelf. Move management into overflow/edit mode, retaining accessible equivalents. Existing removal has confirmation and recovery; this is not an unprotected-delete finding. | 00:18–00:20; `NightWaveEverydaySurfaces.kt` NightWaveRecentShelfTile; Activity `confirmRemoveRecent`. |
| Q03 | P1 V/S | Header, mini-player, navigation and large section actions consume substantial vertical attention. Define a compact/default density contract from available window size, not smaller text/touch targets. | 00:35, 02:50; `NightWaveChromePolicy.kt`, `NightWavePremiumDesignTokens.kt`. |
| Q04 | P1 V/S | Offline library, catalog and transfers use separate View Activities with different component styling and no equivalent persistent music shell. Consolidate user-facing navigation incrementally. | 03:36–04:08; `OfflineLibraryActivity.kt`, `CatalogDownloadsActivity.kt`, `FreeAudioCatalogActivity.kt`; their `startActivity` routes. |
| Q05 | P1 S | Actual Search caller caps saved data at 350 references, first 25 playlists, and offline entries at 400. It also deduplicates URLs before the scorer can select the strongest duplicate metadata. Pure 5,000-row policy benchmarks do not remove these caller-level gaps. Build an asynchronously updated full-corpus index/paged view with safe bounds and duplicate-metadata integration cases. | Activity lines 3186, 3195–3197 at NW125; `SavedTrackSearch.kt`, `MusicLibrary.kt`. |
| Q06 | P1 V/S | Two top results share title/creator with little version information. Add source, duration and preserved-version evidence; group alternatives without silently replacing or merging distinct URLs. | 01:08–01:15; `SearchResultPresentationPolicy.kt`, `SearchVersionGroupingPolicy.kt`, `MusicIdentity.kt`. |
| Q07 | P1 V/S | Creators/uploaders and history-derived entries can appear as artists/saved tracks. Label provenance and deliberate Save versus Recent membership separately. Do not invent verified artist/album identities. | 00:28–00:31, 02:44–02:47; `SavedCreatorBrowser.kt`, `MusicIdentity.kt`, Activity StudioSearch/StudioLibrary. |
| Q08 | P1 V/S | Enlarged artwork is visibly soft; the fallback promotes small provider images only to `hqdefault`, with no guarantee of album-quality art. Use source-aware best-available images, request sizing and graceful fallbacks. | 03:29, 04:09–04:27; `NightWaveArtworkPolicy.kt`, `StudioPlayerScreens.kt` CoverArt. |
| Q09 | P1 V | Mix captions compete with image content and some cards have visually empty lower regions. Fix artwork/title placement and measured contrast on the actual card. | 03:12–03:20; Home mix-card composables. |
| Q10 | P2 V/S | Daily Mix numbering and mood labels lack a strong explanation of their real signal. Improve useful distinctions; retain truthful local-history sourcing and opt-in memory. | 00:13–00:17; `LocalDailyMixEngine.kt`, `PersonalMixEngine.kt`, `NightWaveHomeCurationPolicy.kt`. |
| Q11 | P1 V | A listener with history but no likes/playlists sees several redundant empty/small collection surfaces. Give one purposeful onboarding action for each actual empty state. | 00:34–00:45; StudioLibrary and Home collection sections. |
| Q12 | P2 V/S | Horizontal Library pills obscure New playlist while the same action exists below. Prefer a stable primary Create action and a smaller view-filter strip. Intentional horizontal scrolling is not itself a clipping defect. | 00:35; StudioLibrary overview. |
| Q13 | P1 V/S | Recent rotation and creator groups show "Saved tracks" even when the overview reports no liked songs or playlists. Make collection origin and persistence explicit. | 02:44–02:47, 03:28; StudioLibrary selection/title mapping. |
| Q14 | P1 V/U | Queue entry is visible but a real multi-item workflow is unproven. Preserve the intentional single-selected-song search queue; test Play Next, remove/reorder, shuffled Previous and Undo. | 04:16–04:27; `NightWaveNowPlayingPresentationPolicy.kt`, `PlaybackQueue.kt`, StudioPlayerScreens queue sheet. |
| Q15 | P2 V/S | The mini-player's close control and optional Next compete with metadata space. Clarify dismissal versus transport and keep it consistent across all routes. | Persistent mini-player; `NightWaveMiniPlayerLayoutPolicy.kt`, `StudioPlayerScreens.kt`. |
| Q16 | P1 V/U | Player art/scroll/transport changes need a stable geometry and gesture review. Do not infer a layout bug from individual animation/scroll frames. | 04:09–04:27; player overlay, seek timeline and artwork-transition policies. |
| Q17 | P1 V/S | The main online-song route cannot open synchronized lyrics; users are told local audio is required. Plan user-owned LRC support with stable track identity, plus separately approved provider support if available. | 03:29–04:27; `NightWaveNowPlayingPresentationPolicy.lyricsSubtitle`, `PrivateLyricsStore.kt`, lyrics import/review/editor. |
| Q18 | P1 V/S | "Offline options" and a permission confirmation lead into multiple source tools. Present truthful source capability before the action, with direct Import owned audio and supported-source choices. | 03:33–03:40; offline activities and eligibility paths. |
| Q19 | P1 S/V | Pause/resume discards bytes and restarts. Add genuine safe partial resumption only for eligible servers/sources, with identity/ETag/range integrity; retain explicit restart fallback. | `DirectAudioDownloadWorker.kt` header; `CatalogDownloadsActivity.kt` lines 567 and 855; download explanation in recording. |
| Q20 | P1 U | Completed transfers, process interruption, storage failure and progress monotonicity for a stable job ID need real evidence. Slow visible throughput alone does not establish an engine defect. | 03:36–04:08; WorkManager progress/readback and existing integrity tests. |
| Q21 | P1 V/U | Empty offline Library prioritizes tools/filters/paging before any song exists. Offer one import action, supported-file guidance and a small example/help state. | 03:50–03:54; OfflineLibraryActivity. |
| Q22 | P1 V/S | Sound Studio mixes Off, device presets, guided curves, manual draft and Apply. Make the active engine state and unapplied draft unmistakable; preserve request/session/readback fences. | 01:36–01:41; `NightWaveSoundStudio.kt`, EQ command/capability policies. |
| Q23 | P1 V/U | Backup success is visible, but restoration/merge is not exercised. Provide smaller status hierarchy, meaningful progress and a tested disposable restore journey. | 01:26–01:35; `NightWaveAccountHub.kt`, cloud consent/access/revision and restore policies. |
| Q24 | P2 V/S | Room/DataStore preview terminology and engineering diagnostics occupy You. Put recovery/support detail behind an explicit Help/Advanced disclosure; retain the fallback internally. | 01:55–02:03; StudioProfile advanced storage, playback health, crash diagnostics. |
| Q25 | P2 V | Gear, You shortcuts and repeated management entry points need one predictable settings architecture. Preserve navigation state and keyboard/back behavior. | 00:46–00:54, 01:22–01:25. |
| Q26 | P1 S/U | English UI strings dominate; Bangla search support is not Bangla UI localization. Resource strings/plurals, locale formatting and fallback fonts need an explicit bilingual pass. | `NightWaveStudioActivity.kt` literal labels; only `res/values/strings.xml`, no `values-bn/strings.xml` in inspected tree. |
| Q27 | P1 U | Accessibility cannot be certified from semantic declarations or a recording without TalkBack. Keep NW123 improvements and verify spoken order, distinct actions, slider progress, large type and all themes. | `NightWaveAccessibilityPolicy.kt`, `NightWaveTheme.kt`, NW123 instrumentation sources. |
| Q28 | P1 V/S/U | Several seconds of search/source loading are visible. Add cancellation/phase feedback and collect controlled device traces; main-thread list assembly and creator grouping warrant profiling. | 01:01–01:12; Activity `remember(libraryRevision...)`, StudioSearch creator groups, MusicLibrary preference reads. |
| Q29 | P0 S/U | All recent waves reuse alpha5s/code35, preventing source-to-recording attribution. Add private build provenance/support identity and a disciplined release version policy. | App Gradle identity; 02:01 diagnostics; final APK/source receipts. |
| Q30 | P0 S | NW125 repository gate has an unmatched opening parenthesis at line 57 and cannot execute. Repaired in this audit; both gate modes are now included in current verification. | `tools/nw125_release_candidate_gate.py`, `tools/verify_current.py`; original failure retained privately. |
| Q31 | P0 U | Original 139-case device acceptance, installed signer, real restore and applicable privacy/source/release approvals remain unresolved. A short video and JVM pass cannot close them. | NW125 candidate and release documents; acceptance matrix. |
| Q32 | P2 S/U | Verified albums/artists, cross-device handoff and collaborative/social discovery require separate metadata/service/product decisions. Classify them explicitly instead of adding placeholder buttons or calling local sorting Spotify-scale personalization. | Product scope; local mix engines and declared feature boundaries. |

## Technical verification performed during this audit

The unchanged NW125 app source built successfully, including production Compose, debug assembly, Android instrumentation-source compilation and **1,484 JVM tests in 187 suites**, zero failures/errors/skips. Independently, the current source scan found Q30: its original result was **49/50 passing**, with the repository static audit failing on that Python parse error. The Windows builder is Python-free, which explains how compilation succeeded while a required source gate was broken.

The audit fixes only the unmatched parenthesis and adds NW125's self-test and normal JSON invocation to `verify_current.py`. Both are real executions, increasing the current runner to **52 checks**. All 52 subsequently pass. A fresh complete Windows build after this tooling repair also passes all 1,484 tests and compilation/assembly. App/package/key/data source is unchanged by the audit; this is not a UI implementation of the roadmap.

Final lint, exact artifact/source hashes and hosted-check status are recorded in [the current test report](../../TEST_REPORT.md) and the audit checkpoint. The APK's local certificate matches the saved previous development APK; the actual installed-phone certificate has not been compared. No physical instrumentation test was run. The 313-warning historical lint baseline must not be called a clean or production-certified app.

## External reference basis

Checked 28 October 2026. These first-party references establish capabilities/principles; they are not a synchronized hands-on Spotify A/B test. Features can depend on account, market, device and rollout.

- Spotify's [Now Playing](https://support.spotify.com/us/article/now-playing/) combines transport, save, shuffle/repeat, queue and device access around the selected item. NightWave should keep its strongest player focus while clarifying secondary paths.
- Spotify's [Play Queue](https://support.spotify.com/us/article/play-queue/) documents add/reorder/remove/clear workflows. Use those task expectations when testing NightWave's existing queue.
- Spotify's [offline listening](https://support.spotify.com/us/article/listen-offline/) makes successful download state visible. NightWave needs a coherent readiness journey for its own supported/owned audio scope; this reference supplies no catalog or downloading rights.
- Spotify's [February 2026 lyrics update](https://newsroom.spotify.com/2026-02-04/lyric-translations-offline-previews/) describes previews, translation and offline lyrics. This widens the product-experience gap relative to an unavailable online-song lyric action; availability and licensing remain separate work.
- Android's [accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) explain 48dp target sizing and expanded touch areas; rendered bounds and manual testing still matter. [Technical quality](https://developer.android.com/quality/technical) calls for measured startup, rendering and stability rather than inference from test counts.

The next action is **NW126: reproducible baseline, build identity and acceptance instrumentation**, followed by the ordered roadmap. No 9.5/10 claim is earned by this audit alone.
