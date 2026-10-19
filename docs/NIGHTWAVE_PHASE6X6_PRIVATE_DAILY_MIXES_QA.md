# Phase 6X6 — Private Daily Mixes

## Integration
Based on `feature/nightwave-v2-phase6x5-smart-library-folders`. All Phase 6W8–6X5 changes remain on the branch: equalizer choices, Sleep Timer dial, responsive Repeat, local gapless, crossfade, local normalization, offline shuffle/repeat, and separate playlist-folder metadata. NightWave application ID and signing are unchanged. No NightWave-owned backend.

## User-visible behavior
- Home > **Made for you** now contains Daily Mix 1, 2 and 3, followed by the existing personalized recent/rediscovery/creator picks. The old generic `daily` rotation card is hidden from this surface to avoid duplication.
- At least 3 validated songs create one Daily Mix; at least 6 create two; at least 9 create three.
- Each Daily Mix is a *different set of saved songs*: no track appears in more than one Daily Mix on the same day. A mix has 3–12 tracks. Audio still streams through the existing public playback resolver; no automatic offline download or stream-link prefetching.
- Sources are existing favorites, recently played videos and user-saved public playlist metadata only. VideoLink normalization rejects invalid URLs. The engine never fabricates new track links, artists, genres, play counts, or full-library listening histories.
- Mix 1 emphasizes favorites and recent listening; Mix 2 emphasizes saved tracks not in recent history; Mix 3 emphasizes creator variety based on existing creator labels. Generic/unknown creator names have zero artist-affinity weight.
- A day-number seed rotates equally ranked candidates across days. The same inputs on the same day produce the same playlists; NightWave updates visible mixes on a fresh Home composition after a new day or library revision.
- Playback memory is **opt-in only**. When disabled, `ListeningMemorySummary.recommendationTracks` is empty and the Daily Mix engine independently refuses to use recorded tracks. When enabled, it can re-rank *only eligible library tracks*. Turning it off must remove the memory influence.
- The selected card's **Play mix**, track rows, and explanation reuse the proven Home UI. No persisted smart playlist duplicate and no source library mutations.

## Technical implementation
- New `LocalDailyMixEngine.kt` extends, rather than rewrites, `PersonalMixEngine` / `MixDiversityPolicy`.
- Input bounded to 250 favorites, 100 recent and 350 playlist entries; up to 500 distinct eligible candidates.
- Three-lane deterministic ranking with mutual song exclusion; precomputed URL sets and a 12-track cap per lane.
- Optional `RecordedTrack` statistics are intersected with the eligible candidate IDs and ignored without explicit opt-in.
- Home’s existing library/card/player state contracts remain untouched except adding the daily mixes ahead of non-daily recommendations.
- No additional permissions, cloud data, network connections, on-disk storage keys or telemetry.

## Verification performed
- New `LocalDailyMixEngineTest.kt` with 11 JUnit cases added (Gradle execution pending).
- An isolated **Kotlin compilation** of the Daily Mix engine using minimal Android-independent collaborators succeeded locally, and 11 executable assertions passed. This is not a Gradle/Android full build, and the collaborator stubs are not the app’s full production implementations.
- New source gate `tools/verify_v2_phase6x6.py` wired into `.github/workflows/android.yml` for this branch; old source checks retained.
- Real Gradle `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`, Android device tests, app signing compatibility and exact UI layout are **not yet confirmed**.

## Deferred acceptance tests
1. Build via `build-windows.cmd`; require BUILD SUCCESS. Only install if the APK is signed with the same certificate as the installed app. Do **not** uninstall or clear any NightWave music data.
2. With no saved music, there should be no invented Daily Mix cards.
3. Save 3, 6 and 9 distinct valid songs in an isolated test collection to confirm one/two/three Daily Mix card thresholds, where feasible.
4. With >9 songs, verify no song appears in more than one of Daily Mix 1, 2 and 3. Tap each card to inspect its reason and play the first track.
5. Revisit on the same day: track order should stay stable without library changes. Revisit after the local calendar day changes: equally ranked candidates can rotate without deleting favorites or playlists.
6. Turn listening-memory tracking OFF: Daily Mix cards must still work from your library, but cannot use private playback memory. Enable memory explicitly and verify mix descriptions reflect that only when eligible recorded public tracks exist.
7. Save a song to a playlist or toggle a favorite, then reload Home: Daily Mix sets should update without creating extra saved playlists.
8. Check manual queue controls, crossfade/offline gapless, Sleep Timer, repeat/shuffle, library folders, Drive backups and LRC files remain unchanged.

## Known boundaries
- Daily mixes reuse already-saved public streaming links. They are not an external Spotify-sized music-discovery catalog, nor downloadable playlists.
- No emotion, mood, BPM, true artist identity or genre prediction without metadata. No music uploaded to NightWave.
- Existing generic `PersonalMixEngine` may still share songs with Daily Mixes in other, *non-daily* cards; strict exclusion applies between Daily Mix 1/2/3 only.
