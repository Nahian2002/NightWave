# NightWave — Final Android Acceptance Checklist

**Candidate:** `fix/nightwave-v2-phase6x8c4-daily-mix-unit-test-compile`  
**Application:** `com.nightwave.player`, `2.0.0-alpha5s`, build `35`  
**Date:** 19 October 2026  
**State:** Planned verification, **not results**. Do not mark a case PASS until tested.

## Gate 0 — before touching the installed app

- [ ] Extract a **new complete** branch ZIP rather than overlaying an old project.
- [ ] Run `build-windows.cmd`. Require `BUILD SUCCESSFUL`, and verify that `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:compileDebugAndroidTestKotlin` all completed successfully.
- [ ] Read `dist/NightWave-build-status.txt`; reject any build marked FAILED or any stale `NightWave-debug-previous.apk`.
- [ ] Check app ID, build name/code, SHA-256 and signing certificate against the currently installed APK **before** updating. Never uninstall to work around a mismatch.
- [ ] Preserve the current music/playlist/LRC state, and use isolated test music and a non-essential Google account/Drive folder for destructive acceptance scenarios.

## Gate 1 — essential operation (P0)

| ID | Test | Expected pass criteria |
| --- | --- | --- |
| P0-01 | Cold-launch NightWave; rotate, background and reopen | No crash/ANR, clipped primary control or loss of navigation state |
| P0-02 | Import two personally owned audio files | Both indexed once and playable; import remains after process restart |
| P0-03 | Play offline music and lock screen | Audio continues; notification controls and phone hardware controls work |
| P0-04 | On a 4-song offline list, jump A→D then Previous repeatedly | D→C→B→A, never stale jump history |
| P0-05 | Offline Next A→B→C then Previous/Next | Back and forward follow visible order in non-shuffle |
| P0-06 | Toggle Shuffle, jump to a different track, Previous; try invalid jump | Actual shuffle history is honored, invalid jump does not change current selection |
| P0-07 | One offline song + Shuffle + Repeat All | Automatic Next replays the only song rather than stopping |
| P0-08 | Same song with Repeat Off and with Repeat One | Repeat Off stops on completion; Repeat One replays on completion |
| P0-09 | Build a queue with duplicate saved links ahead of selected track | Selected song remains the one tapped; queue deduplicates without selection drift |
| P0-10 | Dismiss mini-player; reopen, swipe controls and use Android Back | No app exit from an unintended card tap, no stuck/occluded overlays |
| P0-11 | Kill app after selecting song; reopen | Documented resume behavior works; missing/deleted files fail safely without lost library |
| P0-12 | Switch audio output to/from Bluetooth and unplug wired headphones | Expected audio focus/pause behavior, no crash, stuck notification or unexpected auto-play |

## Gate 2 — library, search and creative controls (P1)

| ID | Test | Expected pass criteria |
| --- | --- | --- |
| P1-01 | Search three valid song/artist terms | Results or actionable provider error, not a permanent spinner or silent “no results” for a transport failure |
| P1-02 | Save a public-video reference, then play it | Track is kept in saved library; playback resolves only supported public streams; no implicit MP3 download claim |
| P1-03 | Create, rename and reorder playlists; relaunch | Songs remain in correct playlists with no data corruption |
| P1-04 | Smart collections and playlist folders | Non-destructive views; original playlists unchanged |
| P1-05 | Daily Mix 1–3 from sufficient saved/recent tracks | No fabricated tracks, duplicate song across mixes or new network dependency |
| P1-06 | Toggle listening memory consent | Opt-out never uses recorded listening data for recommendations |
| P1-07 | Crossfade with two locally imported playable files | Bounded handoff, no double-volume spike or stuck secondary player |
| P1-08 | Gapless, optional normalization, EQ and listening level | Controls act on intended route, no clipping/silence; unavailable device effects described honestly |
| P1-09 | Set/cancel/expire Sleep Timer | Playback pauses at expiry; cancel restores behavior; no unexpected queue deletion |
| P1-10 | Import real UTF-8 timed LRC, edit and adjust timing | Accurate line following; persisted edits survive reopen; invalid files rejected safely |
| P1-11 | Export lyrics and reimport a reviewed file | User's original lyrics preserved unless replacement explicitly confirmed |
| P1-12 | Compact UI on S24 Ultra portrait and smaller-width device | No clipped icons, overflow, unreadable contrast, phantom navigation or oversized bottom bars |

## Gate 3 — optional integrations and recovery (P1/P2)

| ID | Test | Expected pass criteria |
| --- | --- | --- |
| P1-13 | Google sign-in plus guest mode, sign out and return | No accidental guest data deletion or token exposure |
| P1-14 | Google Drive backup preview/recovery with isolated library | Explicit confirmation, integrity checks, safe merge; no silent restore or overwrite |
| P1-15 | Personal audio locker upload/download interruption | Failed/interrupted state is not reported as completed; resumable flow respects ownership and account authorization |
| P1-16 | Corrupted transfer journal (test-only fixture) | Journal not silently rewritten and imported audio/checkpoints remain untouched |
| P2-01 | Launcher widget Play/Pause/Next/Previous | Correct current-song transitions and background service behavior |
| P2-02 | Android Auto DHU/AAOS browse, search, select and play (stationary) | Browsing alone does not auto-play; selected audio plays and transport state remains coherent |
| P2-03 | Airplane mode, timeout, provider returns error, local file removed | Graceful failure and recovery; no crash loop, token logging or fake successful download |

## Build and device evidence to retain

Record: exact Git commit SHA, build log and status, SHA-256 and signer, Android device and version, tests actually run, P0/P1 results, screenshots of layout problems, redacted crash diagnostics and regression notes.

**Exit criteria:** Fresh build green, signer compatible, every P0 item passed, no unresolved data-loss/crash issues, all enabled high-risk P1 areas tested, and Android Auto explicitly labeled unverified if not exercised. Source checks or the historical 770/771 pass count do not satisfy this gate.

Reference: [Verification Matrix](VERIFICATION_MATRIX.md) · [Build & Release](BUILD_AND_RELEASE.md).
