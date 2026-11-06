# NightWave v58 Poco comparison and release review

## Poco X3 Android 12

The Poco X3 (M2007J20CI, Android 12 / API 31) upgraded in place from the exact public v55 APK to the signed v58 candidate. All 19 backed-up app files matched before first launch. The installed APK readback matched the candidate SHA-256. The device was checked at its existing 1080 × 2400 display mode, 440 dpi and 0.81 font scale.

- MIUI draws the adaptive launcher icon as a circle. The mint waveform stayed visible within it. The S24 uses Samsung’s rounded mask with the same waveform.
- Home, Search and Sources, Library, You, App details and the AMOLED theme chooser rendered the same design as on the S24. The Poco stacks the Search Offline shortcut, You shortcuts and Library actions at its available width. All four Sources filters and theme swatches fit in the tested orientation.
- The paused full player rendered artwork, a two-line title, playback controls, the queue summary and Related songs. Search this artist was visible. The user’s current track had no related rows in that capture.
- Check for updates returned the expected current-version message with v58 installed and the canonical feed on v55. This checks the status UI. The live older-client v55 offer, download and installer flow remains open.
- MIUI blocks remote input. The owner opened the checked views while screenshots were captured privately. Larger fonts, landscape, all four applied themes and full instrumentation remain outside this scoped pass.

## Release state

The owner explicitly requested publication. GitHub’s v58 draft now holds the exact candidate and passed full APK byte and SHA-256 readback. The locally signed v58 feed references that APK and verifies against the embedded public key. The canonical signed feed continues to advertise v55.

The current source audit returned `NO_GO` for independent YouTube audio playback and the YouTube-derived offline copy path. The repository’s release checklist requires provider authorization or a compliant source change. The workspace did not contain evidence that clears this requirement. The broader instrumented and provider acceptance gates also remain open. [NightWave source audit](../NW77_SOURCE_RIGHTS_RELEASE_AUDIT.md) · [YouTube Terms](https://www.youtube.com/t/terms) · [Acceptance matrix](../quality/MULTISOURCE_NW56_NW58_ACCEPTANCE.md)

- Exact build `NW-debug-99DE616675BB7939` from `3b70cde741042a439381f5f69f3fd5944d257a04`.
- APK SHA-256 `0BF4AED577367FCC104F085D72D5FAFD3AD145D143B4E4EE6A631F6020B9C624`, 30,598,708 bytes. APK signer SHA-256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`.
- The [S24 build record](2026-11-07_NW58_ADAPTIVE_SEARCH.md) records the 1,899 automated tests, 94 source checks and lint for this exact build.
- [Release draft](https://github.com/Nahian2002/NightWave/releases/tag/untagged-4dcdd268a9e5e4854914) · [Public v55](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55)
