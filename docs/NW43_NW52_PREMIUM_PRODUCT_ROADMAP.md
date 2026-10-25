# NightWave Premium Roadmap — NW43 through NW52

**Planning baseline:** NW42 branch, 25 October 2026. **Planning document only**; does not implement these batches or certify releases.

## Objective and constraints

Move NightWave from the user's observed ~5.6/10 overall experience (visual ~6.7/10) toward a dependable, original, premium Android audio player. This is a **quality aspiration**, not an objective or guaranteed numerical result. Spotify is a standard for interaction predictability and refinement, not a UI asset kit to copy.

Hard constraints:
- Android-first; preserve the existing application ID, app signing, local user data, favorite songs, playlists, previous listening position, legal offline audio, and stable background playback.
- No required private server or recurring cloud infrastructure, no new billing requirement. An optional, user-authorized Google Drive backup may need Google Cloud OAuth registration.
- Public video search does not imply a license to download every result. Do not implement circumvention of provider restrictions or misrepresent what can play without network access.
- Do not hide real offline-file problems, silently re-import or clear files, or rewrite users' playlists in a visual upgrade.
- Build/package/test increments **one after another** and treat every failed compiler/preflight result as a blocker. User may defer physical phone tests, but code cannot be labeled release-verified until device validation.

## Gate 0 (required before NW43): make the exact NW42 patch green

- Run `build-windows.cmd` from the latest NW42 SHA and verify `:app:testDebugUnitTest`, `:app:assembleDebug` and `:app:compileDebugAndroidTestKotlin`; fix compiler/test failures rather than masking source-check scripts.
- Run `check-device-update.cmd` before updating the currently installed app; match signing identity. Never uninstall/reset app to force an update.
- Take screenshot/recording baselines for Home, Search, Now Playing, Queue, Library, Downloads and You; preserve build artifacts and diagnostic logs.
- Accept only a fresh successful build, not `NightWave-debug-previous.apk`.

## Batch roadmap

| Batch | Scope | Deliverables | Acceptance gate |
|---|---|---|---|
| **NW43 — Queue correctness** | Fix first-order trust defect | Searching and tapping one track queues **only that track**; explicit playlists/mixes enqueue their actual sequence; manual Add to Queue and Play Next have distinct ordering; optional Autoplay similar music is **OFF by default** and visibly labeled; queue shows Now Playing, Next from collection, Added by you, and Recommendations only where actually present | Test single search result, playlist, shuffle, repeat, Play Next, manual queue, offline tracks, restore, back/next and queue editing. Zero unexpected bulk search-result queues |
| **NW44 — Search relevance & clarity** | Make discovery feel intentional | Rank exact title/artist matches first; handle punctuation, aliases and “Pt. II/2”; downrank cover, instrumental, sped-up, live and unrelated literal matches by default; one search action, clear filters, recent queries, local/offline matches, useful error and empty states | 40-query evaluation set including “Alone 2”; publish top-3 precision and examples. Never auto-queue full results or promise unavailable providers |
| **NW45 — Google account / Drive repair** | Resolve ongoing connection failure, preserve guest data | Read-only signing-fingerprint diagnosis, verify Cloud Android OAuth client/package/SHA-1, Google Drive API, consent audience and Play Services; distinguish error **8 INTERNAL_ERROR** from **10 DEVELOPER_ERROR**; bounded retry; concise error UI and advanced diagnostics; explicit backup/preview/restore comparisons with no silent overwrites | Account connection demonstrably works on a real test device; backup + restore round-trip with disposable data passes. If external Cloud configuration isn't available, report blocker rather than claiming a fix |
| **NW46 — Now Playing 2.0** | Highest-visibility premium screen | Artwork-led layout, smart legible real-art-derived palette with graceful fallback, cleaner title/artist/actions, restrained queue/lyrics/sound entry points, coordinated mini → full player transitions, accessible gestures, no persistent tutorial labels | Video diff against NW42 on 360/412dp and high font scale; transport, gestures, queue, lock screen, offline service remain correct |
| **NW47 — Home, Library & You IA** | Stop stacking generic cards | Home with a clear resume hero and varied authentic artwork shelves; compact Liked/Recent/Offline/Playlists and creation actions; artwork-led playlist details; selected source cues; editor-style sections; relocate engineering settings under Advanced/Diagnostics | Key tasks found in <=2 taps where practical; zero duplicate primary actions or redundant marketing descriptions; no hidden essential settings |
| **NW48 — Downloads & offline reliability** | Complete predictable offline journey | Distinguish queued/downloading/paused/failed/verified/unavailable; real bytes/total/speed/ETA only when measurable; pause vs cancel accurately communicated; byte-range resume **only for sources that support it**; bounded retry and integrity checks; offline collections, storage, playable-only filters and gentle undo for history | Airplane-mode song playback after force close, interruption/restart, low storage, stale file and checksum tests; no false “saved offline” reports |
| **NW49 — Playback depth, EQ, lyrics** | Functional premium audio | Audit shuffle/repeat/crossfade/gapless transitions and state rollback; confirm audio effect sliders truly change sound on supported output; make unsupported paths explicit; synchronized/imported lyrics with correct offsets and readable layout | Audio matrix: screen-off, Bluetooth, headphones, offline, network change, seek, next/previous, queue restore; no phantom EQ settings |
| **NW50 — Motion & accessibility system** | Polish without jank | One typography/spacing/radius/elevation/color system; page, card, mini-player, bottom-sheet transitions; tasteful haptics; stateful skeletons; Reduced Motion and battery-friendly behavior; 48dp minimum touch targets, TalkBack semantics, dynamic type and device insets | QA at 360dp/412dp/tablet and 1.0×/1.5×/2.0× text scale; reduced-motion path has no decorative springs; transitions stay interruption-safe |
| **NW51 — Performance, recovery and security** | Engineering-grade reliability | Profiling actual cold/warm start and frame jank, baseline profiles where warranted, artwork loading/cache, memory/battery budget, ANR/crash tests, recovery after process death, data migration/undo, network/offline boundaries, privacy-sensitive logging review | Measured improvements relative to NW42 baseline, no P0/P1 regressions, full local Gradle/unit/instrumentation + device stress plan; no evidence-free performance guarantees |
| **NW52 — Public release gate** | Make release defensible | Store page, honest features/rights language, permissions, privacy policy, source/licensing disclosures, release signing identity, Google testing vs production OAuth, versioned update/rollback plans, external beta + final device checklist | A release candidate can ship only after build, legal, auth, crash, data durability, storage, and end-to-end manual tests pass; otherwise remain beta |

## Design language to lock before NW46

- **Personality:** NightWave, an audio-first cinematic nocturnal player—not a generic clone of Spotify.
- **Core surfaces:** midnight/ink canvas, one restrained teal/mint accent, foreground text with accessible contrast. Draw secondary colors from genuine art only where readability allows.
- **Hierarchy:** real cover art > selected music/title > primary playback action > queue/lyrics/sound secondary access > diagnostics.
- **Spacing:** one consistent spacing scale and two density tiers (compact track rows versus immersive hero), rather than indiscriminate 18dp rounded cards and 52dp action bars.
- **Typography:** distinct wordmark, confident editorial headlines, comfortable track metadata, quiet technical labels; no tiny low-contrast explanations or gratuitous all-caps.
- **Motion:** meaningful state changes, e.g. miniplayer/artwork relationship, tab selection, swipe/undo. Limit unnecessary looping animation and comply with user's Reduced Motion selection.
- **Feedback:** inline immediate confirmation/Undo, explicit loading and unavailable states, resilient empty states. Show an error once with an actionable recovery path.

## Do not stack unverified source patches

Each batch gets:
1. One issue/feature scope and changed-files summary.
2. Compile-first/source preflight + unit tests on each commit.
3. An actual full Android Gradle build and packaging check; stop on failure.
4. Data preservation and playback regression checks.
5. Before/after screenshots or short recordings for user-visible redesigns.
6. A transparent status: CODED / BUILD PASS / DEVICE PASS / RELEASE READY. Never mark a phase 100% because only source guards pass.

Physical device validation can be concentrated at NW45 (Google), NW48 (offline), NW50 (visual motion), and NW52 (release), but **every** batch must still pass the full automated build before subsequent work begins.

## Research foundations

- Spotify's documented Play Queue separates current playback, items from a collection, manually queued songs and Autoplay recommendations, and says searching/playing a new song changes to that particular song with Autoplay off: https://community.spotify.com/forum/en/kb/articles/807-how-does-the-play-queue-work
- Google distinguishes Play services status 8 INTERNAL_ERROR from 10 DEVELOPER_ERROR: https://developers.google.com/android/reference/com/google/android/gms/common/api/CommonStatusCodes
- Material 3 Expressive / Compose animation: https://developer.android.com/develop/ui/compose/designsystems/material3 and https://developer.android.com/develop/ui/compose/animation/quick-guide
- Shared-element transitions: https://developer.android.com/develop/ui/compose/animation/shared-elements
- Android 48dp accessibility sizing: https://developer.android.com/develop/ui/compose/accessibility/api-defaults
- Android Core app quality and Baseline Profiles: https://developer.android.com/docs/quality-guidelines/core-app-quality and https://developer.android.com/topic/performance/appstartup/best-practices

**Tracking:** NW42 remains the baseline; batches NW43–NW52 are proposed and **not yet implemented**.
