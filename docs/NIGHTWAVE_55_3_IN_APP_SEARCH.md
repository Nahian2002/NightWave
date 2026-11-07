# NightWave 55.3 in-app search

Music search stays on its own tab. Explicit submission searches the existing Audius, PeerTube and Openverse catalogs. Local matches and typing previews remain on-device. Filter out unrelated catalog rows, and distinguish an empty catalog result from an unavailable source. Preserve Sources as its own filter and require an explicit tap to open a supported provider page.

Remove the YouTube search handoff from Music, saved-result taps and the Classic search entry. Saved audio retains local playback. Preserve reference metadata, app data and the public source-rights boundary. Recheck the reported transliterated song and a known catalog hit. Use display version 55.3, Android build 62.

## Verified release

- The S24 Ultra stayed in NightWave for both submitted searches. The three catalog groups rendered for `ek paye nupur amar`, and the app showed a no-match state. `night drive` rendered matching Audius titles. The phone did not open YouTube or a browser during either search. The build 61 to 62 upgrade preserved all 17 checked files before first launch.
- Windows builder passed 1,904 JVM tests in 242 suites, 95 source checks, Android test compilation and sequential lint with 0 errors, 321 warnings and 17 hints. Instrumented suite execution and an older phone's installer handoff were outside this check.
- Exact source `ffc3cd8c955a472bafc92956e4b826068651f341`, embedded build `NW-debug-EAAFEFC96AE30AC3`, APK SHA-256 `724193C265C58A3673BB235307B96F79AC8C96BB5D812A2AB319FAF6EE11BB9A`, 28,316,471 bytes, package `com.nightwave.player`, and signer SHA-256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`.
- [NightWave 55.3](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55.3) passed anonymous full APK readback. [Stable update PR](https://github.com/Nahian2002/NightWave/pull/198) merged as `2e99630212aaefd64888478f9e571e7867454951`. The signed envelope at the merge revision verified against the embedded trust key.
