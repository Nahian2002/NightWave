# NW85 — Now Playing Redesign

**Authoritative roadmap:** [NightWave NW76–NW90](CURRENT_CUMULATIVE_ROADMAP.md) · Wave 2, phase 5  
**Cumulative parent:** `feature/nightwave-nw84-music-first-home`  
**Branch:** `feature/nightwave-nw85-now-playing-redesign`  
**Status:** Now Playing UI and pure presentation policy source updated and committed. Targeted exact-source Kotlin compilation/smoke **PASS**; fresh full Android/Compose/Media3 build, JUnit suite and real Samsung acceptance remain pending NW90.

## Delivered user-facing improvements

1. **Music-first identity:** Now Playing and full-screen lyrics use one `MusicIdentity.display()` calculation for visible title/creator. This is presentation-only; original provider metadata, video IDs, queue and saved library are not mutated. Song titles can use three lines with large font or wide layouts; creators get additional room for enlarged system text.
2. **Accessible actual artwork:** `CoverArt` now labels real artwork with the current displayed track name (including the mini-player), keeps the existing fallback if thumbnails fail, preserves no remote/stock-generated art, and keeps the existing mild animated cover scale and crossfade. Screen-reader use still needs NW90 verification.
3. **Compact premium actions:** Offline and Lyrics actions now share a practical horizontal toolbar using the existing button actions and test tags. Both have minimum 64dp height and can grow for large text. The toolbar leaves queue preview and actual media prominent, instead of three oversized stacked action slabs.
4. **Truthful offline naming:** Public streaming references show **Offline options** (not a promise to save every YouTube song). An already-local track shows **Open offline library**; without a selected song the action is disabled and asks **Choose a song**. Authorized file handling and NW77 source-rights safeguards are unchanged.
5. **Live Up Next preview:** The queue card shows a real named next track when ordered, a user-prioritized **Play next** item when explicitly chosen, **Repeat one** if the current track will replay, and the first song when Repeat All wraps. Shuffle mode without a manually chosen next song explicitly says it is an unordered pool; no random track is predicted. Out-of-range/empty queues and missing metadata show truthful fallback text. The preview never invokes media commands or changes queue storage.
6. **Lyrics that explain availability:** Expanded Lyrics action now displays an unobtrusive status distinguishing imported LRC on offline music, offline songs awaiting a user LRC file, and public videos where synced lyrics are not supported. Existing timed-lyric seek guards, import/edit/removal and immersive lyric views remain in place.
7. **Motion and readability:** The named queue-preview track uses a short existing `NightWaveMotionPolicy.FEEDBACK_MS` crossfade; reduced-motion users get a zero-duration transition. Mini-player titles may wrap at larger text sizes, while playlist and queue action labels use adaptive minimum sizes rather than fixed-height clipping.

## Source protections

- Retained the NW80 identity-fenced `actions.seekToForTrack(originatingTrack, it)` callback for timeline scrubbing and original artwork swipe Next/Previous gesture policy.
- Retained NW58 opaque Now Playing overlay during its existing enter/exit transition; no transparent frames revealing Home.
- Retained independent mini-player hide (without pausing), lock-screen background service, real Queue sheet selection and per-row removal/Undo.
- Did **not** touch playback engine, WorkManager download workers, Google Drive/OAuth, offline audio storage, saved library, Search ranking, Android manifest, app Gradle build file or signing.
- No backend, paid API, analytics, high-volume artwork downloads, persisted new preferences, music-rights circumvention or fabricated metadata.

## Regression and evidence

- New `NightWaveNowPlayingPresentationPolicyTest.kt` includes seven focused JUnit tests: exact next-slot preview, manual Play Next priority, shuffle unpredictability, Repeat One/All, invalid queues, truthful lyric/offline labeling, large-text behavior.
- New `tools/NW85PlayerSmoke.kt` pure-JVM standalone smoke using the exact production policy. Its README-style commands compile just the production policy and test harness with kotlinc; no Android dependencies or user data.
- **Actually executed targeted compilation and smoke:** production `NightWaveNowPlayingPresentationPolicy.kt` downloaded via exact text from the connected GitHub branch was reproduced in the local runner and its Git blob hash matched `0a1f520b140a69798b1b3b58a417282afc2ec952`. `kotlinc` plus a targeted 11-assertion harness compiled and returned **NW85_PRESENTATION_POLICY_SMOKE_PASS**. This is **not** a JUnit/Compose/Android app compile.
- New `tools/verify_nw85_now_playing.py` source-only UI contract covering label wiring, mini-player, shuffle/repeat, gestures, seek identity, reduced motion, queue, Lyrics, Offline and overlay opacity; committed, **not executed** in this session.
- GitHub text readback confirmed every source binding and seven JUnit test groups, balanced Kotlin braces for sampled files, and exact blob equality of nine protected parent NW84 files. No APK or real device was tested.

## NW90 acceptance

Exercise queue states empty/one/two/large, exact Play Next/reordered slots/Repeat Off–All–One, shuffle, offline/private tracks, repeated URLs, no loaded media, expired/deleted audio, lyric import/missing LRC and timing edits, safe seek during rapid Next, artwork swipes during buffering, foreground/lock-screen controls, screen-reader artwork announcements, large font (1.3x–2x), split/landscape widths, all NightWave themes, transparent-overlay transitions, battery/framerate and Android process recreation. Full Gradle, JVM and instrumented tests, the owner's 139-case checklist, Google backup round-trip and public-media rights must pass before release certification.

**Boundary:** NW85 is a cumulative source milestone, **not** a justified 9.5+/10 visual rating or public release gate. Rights review from NW77 remains NO-GO.
