# NightWave NW33 — Same-track Retry and Offline Queue Reliability

**Status: LOCAL SOURCE REVISION / ANDROID APK AND PHONE QA PENDING.**

This phase preserves the NW32 search-to-offline pilot, offline source library,
streaming playback, premium UI, playlists, lyrics, Google backup, and all
previously recorded song IDs. No source files have been deleted.

## What changed

- Searched-song WorkManager requests now include a bounded **public video ID**
  tag. It is not an account ID, CDN URL, auth cookie, token or media file.
  Failed/cancelled jobs can be explicitly retried for the **same recording**
  from the Downloads screen, without searching an unrelated catalog.
- A finished job whose private audio file has subsequently been deleted can
  offer **Save this song again** (where a new-style video ID tag exists).
  The actual offline file is checked before showing the recovery action.
- Retries require a new affirmative user action and permission confirmation.
  No automatic infinite retry is created and an unapproved recording must not
  be downloaded. An unsupported/DRM-restricted track stays unavailable.
- ExistingWorkPolicy.KEEP still protects a currently unfinished download from
  being replaced or duplicated when the button is tapped repeatedly.
- New catalog, direct-link and searched-track jobs carry a request timestamp
  tag. The Downloads UI now shows **active first, then newest** instead of
  alphabetical UUID order, which could bury recent songs after dozens of jobs.
  Legacy untagged jobs remain accessible at lower sort priority.
- For new tagged searched-track jobs, only the current/newest job row is shown
  per video, rather than accumulating obsolete failure rows after retry.
- Error states now distinguish source denial (HTTP 401/403), temporary rate
  limiting (429), unavailable standalone audio, invalid audio, storage shortage,
  network failure, and unknown failure. The message policy never displays raw
  exception strings that could contain signed streaming URLs.
- Source validators, network safety checks, app-private format validation,
  rights confirmation and strict Ready Offline local file checks are retained.

## What did not change
- No DRM bypass, cookie forwarding, login scraping, YouTube challenge bypass,
  automatic stream conversion or guarantee for the entire YouTube catalog.
- No change to Android package ID, signing, versionCode, existing library IDs,
  local songs, playlist formats, Google OAuth or backup storage.
- Existing older WorkInfo entries lack the new retry identity tag. They remain
  readable but cannot be safely retried by identity from the history screen;
  retry those recordings by opening their original result in Search.
- YouTube platform rules restrict offline retention without prior authorization.
  This search-stream pilot is not cleared for public/commercial distribution.

## Local acceptance checklist
1. Extract the **complete** NW33 source ZIP into a new Windows folder.
2. Run `build-windows.cmd`. Require preflight, Kotlin/Java compile, unit tests,
   Android test source compilation and final APK signature verification.
3. Run `check-device-update.cmd` before installing; never uninstall or clear
   existing data just to bypass signing continuity.
4. In online Search choose a recording you are authorized to store and save it.
   Check background progress, accurate success and Airplane-mode playback.
5. Create a permitted failed/blocked source and verify a specific failure
   reason and **Retry saving this song** (same song, no unrelated match).
6. Test retry with slow network: repeated taps must not replace a running job.
7. Delete a disposable saved test recording, return to Downloads, confirm
   it shows missing/unavailable and offers **Save this song again**.
8. After several retries, the Downloads screen should show one current job
   per newly tagged searched track and prioritize recent jobs.
9. Check disk-space failures, network loss, cancellation, offline queue
   Next/Previous, app restart and offline library search.
10. Regression-test normal streaming, saved playlists, lyric display, Google
    Drive authorization, import, direct-link and free catalog downloads.

## Checks run in the source editing environment
- Exact NW33 retry policy compiled on local Kotlin/JVM with a stub for the
  previously existing canonical YouTube URL parser: 12 assertions PASS.
- Exact NW33 failure policy compiled on Kotlin/JVM: 8 assertions PASS.
- These are isolated checks, NOT a full Android Gradle build.
- No Android phone test was run; no GitHub Actions workflow was invoked.
