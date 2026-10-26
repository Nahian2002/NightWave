# NW89 — Lyrics, Accessibility & Performance

**Roadmap:** [NW76–NW90](CURRENT_CUMULATIVE_ROADMAP.md) · Wave 3, source phase NW89  
**Cumulative parent:** `feature/nightwave-nw88-sound-studio-audio-quality`  
**Branch:** `feature/nightwave-nw89-lyrics-accessibility-performance`  
**Status:** Source changes and regression test sources committed. Full clean Gradle/JUnit/Android instrumentation and Samsung device acceptance are deferred to NW90. No new online lyrics provider or microphone/transcription service.

## Changes

### Correct imported LRC ordering and multilingual song checks

- `TimedLyricsCodec` continues strict UTF-8, **64 KB** and **1,200-timed-cue** limits, explicit `[mm:ss.xxx]` timestamps and duplicate collapse. Cues are now sorted **stably by timestamp**, retaining original author order for distinct lyric lines at the same timestamp. They are no longer alphabetically reordered.
- `LyricsEditorPolicy` uses the same stable timestamp-only sort. The importer, editor, Undo and export agree about what happens when two lines start simultaneously; the chosen line does not silently change after saving.
- `LyricsImportReviewPolicy.normalizedName()` retains Unicode **combining marks**, especially Bangla dependent vowels. Filenames such as `মালা.lrc` and `মেলা.lrc` are not falsely considered identical. Explicit LRC `[ti:]` titles must match a complete normalized word sequence rather than an arbitrary word substring: `Moonlight` vs `Moonlighter` cannot be silently treated as a valid tag.
- Import matching remains only **advisory**; the user must review/confirm the LRC. A filename cannot establish licensing or prove its text matches the actual recording.

### Accurate high-performance synced lyric follow

- `LyricsPresentationPolicy.lineProgress()` locates the next distinct timestamp using **O(log n)** binary search rather than scanning forward from the currently active line each update. It respects offset adjustments and keeps line-level progress only (never invents per-word karaoke timestamps).
- `LyricsReadingPolicy.shouldAnimateScroll()` permits a short scroll animation for nearby cues (at most 12 rows), and instead jumps immediately for distant seeks/song skips, unsupported indexes, or **reduced motion**.
- Integrated these rules into immersive Now Playing lyrics and the separate lyrics editor. A long LRC search result or 1,000-row skip need not animate through every intermediate lazy-list item.
- Actual playback/tap seek remains routed through `actions.seekToForTrack(originatingTrack, ...)` with existing track identity fences and `LyricsSyncPolicy.seekPosition`; this phase introduces no new media controller, background service or seek timing source.

### Legible, accessible full-screen lyrics

- Each tappable cue now has a minimum **56dp** row height, a TalkBack description containing its genuine text and **offset-adjusted seek time**, and a `Current synchronized lyric` state label when highlighted.
- Full-screen Follow and Edit actions have minimum **48dp** height. Follow announces its purpose, with separate hints for manual browsing versus synchronized follow.
- The full-screen song/artist header and lyrics editor title expand to **two lines** at larger font scales, avoiding a hard one-line clamp on accessibility-sized devices.
- Existing selected-line visuals, active line styling, 50ms/250ms manual offset, copy/export/review/undo, user LRC import, and dark/AMOLED/Pearl themes remain.

## Regression test sources

- Added `LyricsReadingPolicyTest.kt` with four groups: bounded/far-jump/reduced-motion rules, overflow inputs, precise accessibility seek labels for Unicode text and offset/duration, and follow-state messaging.
- Expanded `TimedLyricsCodecTest.kt` with two groups: simultaneous authored cue order and a **1,100-cue multilingual LRC**.
- Expanded `LyricsPresentationPolicyTest.kt` with two groups: large-file half-second progress and long runs of identical timestamps.
- Expanded `LyricsImportReviewPolicyTest.kt` with two groups: distinct Bangla vowel marks and rejecting falsely corroborated explicit title tags.
- Expanded `LyricsEditorPolicyTest.kt` with stable same-timestamp draft and round-trip.
- Added `tools/NW89LyricsSmoke.kt`: pure-JVM test runner for the exact `TimedLyrics`, `LyricsSyncPolicy`, `LyricsPresentationPolicy`, `LyricsReadingPolicy`, and `LyricsImportReviewPolicy` source files. Command is in the file header.
- Added `tools/verify_nw89_lyrics.py`: read-only source-contract checker for privacy/size limits, stable ordering, bounded scrolling, TalkBack semantics, tap-to-seek identity and test coverage.

**Evidence and limitations:** Targeted connected-GitHub source readback is available for the changed Kotlin policies, UI and tests. A source-contract check or structurally balanced braces is NOT Android compilation. The Kotlin smoke runner and full JUnit/Gradle/instrumented cases are committed but **not marked executed**. Responsive visual/scroll smoothness, actual TalkBack announcements, gesture interruptions, long-session battery and startup/performance profiling still require a real runtime in NW90. NW89 intentionally avoids risky arbitrary startup/background engine changes without a measured baseline.

## Preserved boundaries

No browser lyrics scraping, unauthorized lyrics catalog, automatic transcription, new billing or NightWave server; no changes to private audio indexing, Google Drive credentials/backup, online playback rights, download eligibility, EQ engine, queue storage, Android manifest or app signing. The **NW77 public-source-rights issue remains a hard NO-GO** for release independently of NW89 UI quality.

## NW90 acceptance

Check 0/1/1,200 cue imports; malformed and oversized UTF-8; simultaneous timestamps and multiline Unicode; Bangla filename/song identity ambiguity and dissimilar explicit `[ti:]` tags; local offset changes from -10s to +10s at 50ms steps; rapidly skipping source tracks mid lyric tap; persistent draft/undo/compare; accessibility with 1.0x–2.0x font scales, screen reader, split-screen and reduced motion; dragging to suspend auto-follow; Follow re-enable; far-range lyric editor searches; and long playback while lyrics remain open. Run all JVM tests, Compose/instrumented tests, verified Google Drive backup/restore, real device QA and media-source/legal audits at NW90 before claiming a public release.
