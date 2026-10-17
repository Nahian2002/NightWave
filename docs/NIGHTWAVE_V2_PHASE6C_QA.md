# NightWave Phase 6C — Opt-in, on-device listening memory

**Branch:** `feature/nightwave-v2-phase6c-optin-listening-memory`, stacked on 6B. **Draft source, NOT a verified Android build.** Stable main and installed user data untouched.

## What was implemented

- **Listening memory is OFF by default.** Under **You > Listening memory · optional**, the user may explicitly enable or disable local recording, refresh statistics, or select **Erase & turn off** (followed by an irreversible-action confirmation).
- **Only PlaybackService records** while its actual Media3 player reports both `isPlaying` and `STATE_READY`. It does not infer plays from queue selection, YouTube searches, saved bookmarks, or the user's deduplicated recent track list.
- **Observation clock:** Android `SystemClock.elapsedRealtime()` provides monotonic elapsed time, not device wall-clock time. The existing approximately 2.5-second bookmark ticker is reused, with records batched at ≥10 seconds, pause/track transition, or when the 30-second qualification threshold is reached. No extra background polling service.
- **Qualified listening session:** After 30 seconds of observed active playback, ONE qualifying session is credited for the current continuous track instance. Pauses do not charge idle time. Repeat-one transitions start a new session. A long process suspension can undercount because any observed gap is capped at 5 seconds (privacy and accuracy fail-safe). This is **not a guarantee of billing-grade or gap-free exact listening time**.
- **Aggregates only:** locally stores canonical public video URLs or private offline audio IDs, truncated title/artist, observed milliseconds, qualifying-session count, and latest event time. Maximum 250 tracked items and 120 KB encoded JSON; the oldest entries are dropped as the aggregate fills. Untracked tracks and old pruned sessions are not reconstructed.
- **Privacy boundary:** `nightwave_listening_memory_v1` is separate from music library prefs, local lyrics, Google Drive appData snapshots and existing manual library backup. No new API, network endpoint, permissions, analytics SDK, microphones, identities or ads.
- **Clear/disable concurrency:** all consent, clear and recording writes share one in-process lock; commits recheck consent and the originating revision under lock. The consent revision increments on both toggle and erase. PlaybackService resets any in-flight pending time on a revision change—even if the user disables/re-enables rapidly—rather than resurrecting an old session.
- **No disk writes in Media3 callbacks:** the player creates bounded pulse records only. A dedicated single-thread worker serializes SharedPreferences commits, checks consent revision again for every queued event, and drains eligible pending writes on normal service destruction. No raw event histories are stored.

## UI and limits

The optional You panel provides observed-time minutes, **30s+ sessions**, distinct tracked songs, and up to five titles ordered by observed time. It explicitly discloses that:
- values cover the retained local aggregates, **not** an all-time or cross-device history;
- sub-threshold plays contribute observed time but **not** a qualified session;
- very short final fragments can be lost if Android kills the process;
- metrics are not scraped from online services and are not part of Drive backups;
- the preexisting 6B **Your listening snapshot** is still a separate count of unique recent tracks, not a play-count database.

## Tests to run later (NOT executed now)

1. Execute `tools/verify_v2_phase6c.py` and inherited checks. Run `ListeningSessionMeterTest` and full JVM tests plus `:app:assembleDebug` and `:app:compileDebugAndroidTestKotlin` with the exact Windows branch ZIP. Archive build diagnostics; resolve Kotlin type/import/signature errors before any install.
2. Confirm new users see tracking OFF and no writes to `nightwave_listening_memory_v1` aggregates until opting in. Check public and imported offline tracks. Existing playlists/history should remain identical.
3. Play a single song for less than 30 seconds: observed duration may accrue but session count stays 0. Play for ≥30 seconds: exactly 1 qualifying session. Pause for 1 minute: no additional time. Seek and buffer: elapsed time should not double-count; queue changes and repeat-one correctly separate sessions.
4. Toggle OFF during a playing song, then ON quickly before next 2.5s sample: revision guard must drop old pending intervals; subsequent playback starts a new session. Test **Erase & turn off** during service updates and confirm statistics remain erased even if delayed callbacks execute.
5. Force-stop, power-cycle, crash, notification dismissal, app swipe-off and media interruption. Metrics may undercount short last fragments but must never claim time while paused or silently restart playback.
6. Test library at capacity 250 songs / 120 KB, malformed saved JSON, giant titles and unexpected URL/ID. No crash, unbounded file growth, unauthorized identifiers or cloud upload.
7. Inspect You at 320dp, 360dp, 412dp and tablets, font scales 1.0–1.8, light/dark/AMOLED, keyboard, TalkBack. The toggle, refresh and erase confirmation must remain usable.
8. Regression test Phase 6B private LRC import/highlighting, Phase 6A2D Search/Home, mini-player X and swipe hide, Media3 retries/queue, local offline audio, and Phase 5U/5Z cloud recovery without destructive restore.
9. Run `check-device-update.cmd` and compare package/version/signing certificate with installed NightWave. **Never uninstall or erase music data to work around a certificate mismatch.** Release or stable merge only after device verification.

## Not included

No daily chart, date-window analytics, ranking based on external streaming history, cross-device analytics sync, personalized ML training or tracking of private browser activity. A future Phase 6C2 could add opt-in **daily retention buckets**, export/delete options and explicit multi-device design after verifying 6C first.

**Status:** GitHub source and JUnit tests authored; static source readback only. Android build, JUnit execution, instrumentation, signing, real-device playback fidelity and UX remain unverified as requested.
