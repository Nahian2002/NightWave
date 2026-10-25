# NW55 — Offline and Downloads visual cohesion

**Parent:** `feature/nightwave-nw54-search-clarity`  
**Branch:** `feature/nightwave-nw55-offline-download-polish`  
**Status:** Source changes implemented; **full Gradle and phone rendering still unverified**.

## Offline Library
- Unified the Offline Library's native screen backdrop with the Downloads screen, refined the typography and reduced excess title/subtitle padding.
- The permanent technical storage/uninstall footnote has moved into the expandable **More sources & playback** area. Its information remains accessible; it no longer consumes 45dp of song-list height.
- **Issues** is a compact on-screen filter while screen readers still announce **Needs attention**. Readiness still depends on the original physical-file health report, not on a WorkManager success label.
- More readable summary of number of visible/total tracks, ready tracks and entries requiring attention; removes the developer-facing `MB indexed` status from the prominent live UI.
- Existing safe import, sorting, collection playback, shuffle, history, storage health, recovery, per-song actions and confirmation-before-delete remain.

## Downloads
- Back control is now an explicit 48dp target with accessible description. Title and subtitle follow the Offline Library hierarchy.
- Playback, retry and navigation actions previously 44dp high are now at least 48dp.
- The action label **Pause** remains explicit: the same confirmation and warning clarify that restarting begins from zero; there is **no false partial-byte resume**.
- An explicitly paused download offers **Start download again** instead of a potentially misleading Resume label.
- Existing WorkManager state, rate estimation, file-ready verification, cancellation, file availability and history visibility stay unchanged.

## Testing and release gates
- Two new JVM checks in `OfflineLibraryViewPolicyTest` cover visible filter labels vs screen-reader descriptions and readable summary counts.
- `tools/verify_nw55_offline_chrome.py --self-test` checks production/source contracts plus four negative fixtures. CI runs this after NW53 and NW54 gates.
- These are layout-policy and source reviews, **not proof of smooth 320dp rendering, faster downloads, Google OAuth fixes, or arbitrary YouTube offline eligibility**.
- Next phone acceptance: verify Offline Library with 0/1/100 tracks, damaged/missing entries, downloads in progress, screen rotation, large fonts and real offline playback. Screenshot and visually rate against NightWave's main Compose theme.

**Next planned:** NW56 contrast/theme consistency, then NW57 Home hierarchy and player transition polishing, each in a separate stacked branch.
