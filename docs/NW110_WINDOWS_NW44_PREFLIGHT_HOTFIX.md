# NW110 Windows Builder Hotfix — NW44 Search Group Preflight

Date: 27 October 2026. Parent NW110 commit: `b29267223ae6ef042efaa6d56b40f3a072e12f59`.

## Observed first Windows build (user-provided evidence)

From `C:\NW\run-nw91-final.cmd`, the source compatibility checker stopped with:
`NW44: all grouped Search taps must remain single-song.`
The builder reported `Kotlin source compatibility preflight` and the full build log stated **Gradle not yet started**. This is **not** a Kotlin compiler, Android Gradle, or runtime failure, and no new APK was verified. The original installed app/data was not changed.

## Verified root cause

`tools/check-kotlin-compat.ps1` expected `playSinglePublic(song)` exactly **two** times. The actual `StudioSearch` function has **three** such callbacks, one each in Best matches, Other versions, and More results. These all invoke the same `onPlay(selected, listOf(selected))` single-track playback callback. The three local/saved Search handlers also remain `onPlay(song, listOf(song))`.

Existing `tools/verify_nw43_queue.py` and `tools/verify_nw44_search.py` both independently expect **three** public group callbacks and three saved-track handlers. Therefore the Windows-only preflight was stale after the third UI group was introduced. No public result ever needed a multi-track implicit queue.

## Fix and regression protection

- Added `Test-NW44SingleSongSearchTaps` to the Windows PowerShell preflight, requiring 3 saved tracks, one selected-track forwarding callback, and 3 public grouped click sites.
- Replaced the stale two-click assertion with the new helper.
- Added Windows `-SelfTest` fixtures: a three-group positive and negative cases for absent handlers, multi-track public click, and bulk saved-track click.
- Kept the PowerShell 5.1 source ASCII-only.
- GitHub branch is cumulative NW110; no Java/Kotlin app changes, storage changes, package changes, signing or Google authorization changes.

## Local source checks vs real build

**Verified from GitHub:** 13/13 targeted source/negative-scenario wiring checks agreed with the committed Kotlin UI and PowerShell guard. The script's new self-test cases are present. PowerShell 5.1 itself was **not executed** in this non-Windows environment, and Android Gradle still has not been observed passing on the user's computer.

To re-test: download the latest **hotfix branch** ZIP and extract into a fresh short path, e.g. `C:\NWF`; execute `run-nw91-final.cmd`. The Windows builder will run the preflight again, then attempt actual Kotlin/Android Gradle compilation and official tests. Do not skip further errors; share the new `dist\NightWave-build-diagnostic.txt` and `dist\NightWave-build-errors.txt` after the next attempt.

**Public release still NO-GO** until signed Android build, device QA, Google Drive, music rights and Play checks all pass.
