# NightWave Phase 6C2 — Listening Trends & Smarter Recommendations

**Draft branch:** `feature/nightwave-v2-phase6c2-private-trends-personal-mixes`, stacked on 6C. Stable main unchanged. **GitHub source only; full Gradle/JVM, Android and physical device verification postponed.**

## Feature 1 — UTC daily trends, sourced from real opted-in listening
- A private `DailyListeningBucket` stores **UTC day**, approximate observed active audio milliseconds, and sessions that crossed the inherited 30-second qualifying threshold.
- `ListeningMemoryStore.record()` rechecks consent AND the originating consent revision under the same in-process lock used by **Erase & turn off**. A single SharedPreferences commit updates both existing per-track totals and the new UTC-day aggregate, avoiding partially committed reporting. No new telemetry or background service.
- Daily history is bounded to **30 UTC calendar days** and **8KB** serialized JSON, with defensive decoding and deduplication. Old daily buckets are removed on the **next read or successful write**; NightWave does not promise a background timer while the app is closed.
- **You > Listening memory · optional > Listening trends**: selectable **7 days** (individual daily bars) and **30 days** (grouped into at most five chronological ranges), with observed minutes, qualified-session totals, zero-data guidance and explicit UTC date labeling.
- These are **observed/committed playback intervals**, not billing-grade whole-song duration, Spotify listening history or an exhaustive device clock. Long Android suspension and killed processes can undercount. A bounded pulse that straddles midnight is attributed to its **flush day UTC**; no artificial date split is inferred. Disabled periods do not appear.
- **Daily trend retention is separate from existing Phase 6C per-song totals.** Older daily buckets are pruned after 30 days, but the longer-lived, capped per-song aggregate remains until **Erase & turn off**; daily expiration alone does not erase or retroactively alter those totals. No daily data is sent to Drive.

## Feature 2 — Learned-on-device, only from real saved music
- Phase 5Y's `PersonalMixEngine.make` accepts **optional** recorded play statistics. The old four-parameter entry point still works with defaults, so previous callers retain original behavior.
- **Only while consent remains enabled**, the UI reads up to 100 existing recorded public-video candidates from local private aggregates. Scores add a bounded weight for 30s+ qualified sessions and observed playback time to existing favorites/recent/playlist affinity.
- All recommendation output remains a **subset of songs already present in favorites, recent plays or local saved playlists**. It cannot invent music, launch a provider search, replay private offline file paths as video songs, expose a new URL, bypass source permissions or run a paid AI model.
- If enough distinct saved catalog songs were actually played, a new **Your listening rotation** mix may appear, with transparent explanation. A saved recommendation preview does not start playback unless a real track or **Play mix** is pressed. At most three mixes are shown.
- Switching consent OFF immediately hides recording-based ranking signals while retaining optional already-recorded per-song data in private storage. **Erase & turn off** removes both the underlying aggregates and the UTC daily buckets; quickly disabling/re-enabling does not allow stale queued audio samples to resurrect erased data.

## Strict safety boundaries
- Application package/version/signature unchanged. No new Android permissions, cloud schema changes, Google account changes, analytics SDKs, web providers, scrapers, microphone recording, playlist mutations, media service changes, new network calls, paid assets or profile data uploads.
- The underlying 6C consent revision and serial background writer remain authoritative. 6B private lyrics, 6A2D UI, mini-player X/swipe hide, Media3 queue/notification controls, 5Y resume guards, 5U backup and 5Z cross-device manual merge remain preserved.
- The new `dailyUtc` key stays in the app-private `nightwave_listening_memory_v1` preferences; Android system backup is disabled. **Neither daily bucket nor recommendation signals are added to v1 library export or private Google Drive appData backups.**

## QA to execute later — not executed for this implementation
1. Run `tools/verify_v2_phase6c2.py` and all inherited source gates, then complete JVM tests and `:app:assembleDebug :app:compileDebugAndroidTestKotlin` via Windows build process. Compile errors take precedence over source assertions. Recheck generated APK package/version/signature before in-place installation; never uninstall or clear current music data to bypass a signing mismatch.
2. Execute `ListeningTrendEngineTest` (7 cases) and `PersonalMixMemoryTest` (5 cases). Verify 7/30-day calendar selection, UTC midnight rollover, old/future records excluded, deterministic ranking and no fabricated track suggestions.
3. On a **disposable emulator only**, run `ListeningMemoryTrendInstrumentedTest` (5 cases): default OFF, atomic track/day save, revoked consent with queued write, erase with rapid opt-in, retention, and ranking opt-out. It intentionally skips physical phones before accessing prefs.
4. With tracking enabled, play a real local or public track beyond 30 seconds and confirm one qualified session and plausible observed seconds attributed to current UTC day. Cross the UTC day boundary, pause, buffer, resume, skip, repeat-one and force-stop: no fake listened time or double-credit. Confirm incomplete last fragments may undercount.
5. Choose 7 and 30 days in You while no data exists and with 30 days of data. Confirm bars, date labels and totals are accurate, 30-day grouping is readable, older buckets pruned only on next access, and 6C lifetime aggregates are not falsely presented as 30-day only.
6. Enable listening memory, play a known saved set, refresh Home, confirm suggestions include only real saved catalog tracks. Then turn OFF and confirm recommendations return to pre-memory ordering while retained private statistics stay available; erase and confirm empty list and zero trends.
7. Stress up to 250 aggregate songs, 30 daily buckets, malformed stored JSON, large labels, negative/future pulse times, rapidly toggled consent and two racing service pulses. Reject or sanitize errors without corrupting music, crashing playback or exceeding 120KB+8KB JSON bounds.
8. UI screenshots/TalkBack at 320dp/360dp/412dp, tablet, landscape, large font scales, dark/light/AMOLED and reduced motion. All chart labels, 7/30 chips and erase controls must remain reachable. No overlapping mini-player or bottom navigation.
9. Regress 6B lyrics, 6A2D saved search, 6A2 mini-player dismissal, 5Y smart resume, offline files, queue/Undo, background playback, Google OAuth scope, safe backup and merge without destructive primary-device sync.

**Readiness:** source code, JUnit and emulator test definitions and manual CI integration only. No Android build, Kotlin/JVM test execution, real-device playback measurement or signing validation claimed. Keep PR draft until verified.
