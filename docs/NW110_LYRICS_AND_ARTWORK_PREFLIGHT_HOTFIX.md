# NW110 Windows build hotfix — Phase 6V6A and proactive Phase 6V7B

Date: 27 October 2026.

## Phase 6V6A: observed blocking failure

In the user's `C:\NWG` build, the source preflights passed through 6V5C, then `check-phase6v6a-integration.ps1` failed with **Reduced-motion accessibility missing**. Uploaded diagnostic confirms no Gradle command had started and no current APK was verified.

**Actual production behavior:** `StudioPlayerScreens.kt` uses `LyricsReadingPolicy.shouldAnimateScroll(firstVisibleIndex, target, reduceMotion)` to decide between bounded `animateScrollToItem(target)` and immediate `scrollToItem(target)`. `LyricsReadingPolicy` explicitly returns false with reduced motion enabled, and restricts animated movement to at most 12 rows. This is newer and safer than the obsolete literal `if (reduceMotion) lyricListState.scrollToItem`.

**Safe correction:** update both PowerShell and Python 6V6A source gates to require the real policy, visible animated and instantaneous branches, and explicit reduced-motion handling. Added a positive Windows self-test with four negative scenarios, and Python regression negatives to reject bypassing the policy or removing its `!reduceMotion` condition.

## Phase 6V7B: found before the next Windows build

A proactive review of the upcoming source preflights discovered `check-phase6v7b-integration.ps1` and `verify_v2_phase6v7b.py` still required `NightWaveCollectionChrome.TRACK_ARTWORK_DP.dp` for row artwork. The current song-card implementation instead uses `NightWavePremiumDesignTokens.ROW_ARTWORK_DP.dp`, defined as 52dp, inside the actual `TrackArtwork(video,...)` call. Both old gates have been aligned with the current premium token. A negative Python source fixture rejects a hardcoded replacement.

## Neighboring gate review

Source-string comparisons show the required markers are present for all **59 source contracts checked across 6V6B and 6V6C**, all **50 checked across 6V6E and 6V6F**, plus **27/27 6V7A contracts**. The 6V7A mini-player Hide callback was checked separately: `onHide()` is present, and Pause/Stop mutations are absent. The 6V6D source markers also matched. These are **static checks**, not a PowerShell execution or an Android build. Other undocumented/semantic failures remain possible.

## Data protection and certification

**Only source verification scripts, workflow triggers and documentation changed.** No Android/Kotlin player file, imported lyric content, saved tracks, offline cache, account, Google Drive scope, version code, package name or signing identity was changed. The user's existing installed app and database remain untouched.

**Still required:** a full fresh Windows Gradle build, official JUnit/AndroidTest execution, APK certificate comparison, live Drive account/restore, device acceptance, and music content rights review. Do not install an APK from a failed build. Public release **NO-GO**.
