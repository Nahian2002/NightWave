# NightWave v0.4 — verification status

Date: 2026-10-17

## Verified baseline
- User completed a successful Windows Gradle APK build of the previous NightWave version.
- User reports all five S24 Ultra playback checks passed, including screen-off music and Bluetooth controls.
- Those results are not proof that the new v0.4 search UI works.

## v0.4 changes
- Search-first layout with 16:9 thumbnail results and metadata.
- Free-form song and artist searches with Songs and Videos tabs.
- YouTube Music songs filter with regular video-search fallback.
- Link parsing and Media3 playback service preserved.

## Source checks
- Kotlin smoke tests: 10 valid URLs, 9 rejected URLs, 4 time labels, 5 song-name queries plus URL/blank/bad-link classification: PASS.
- Offline XML parser: 12 XML documents and view-binding IDs: PASS.
- Android manifest, Media3 foreground service and CI wiring: PASS.

## Pending
- This environment cannot compile the Android app; v0.4 still needs a Windows APK build.
- Live network search and physical-device playback need verification.
- YouTube can change its search/stream implementations, so success is not guaranteed.

## Next step
Download a fresh GitHub NightWave ZIP; extract; run build-windows.cmd; install dist/NightWave-debug.apk.
Search `Adele Hello`, try Songs and Videos, tap a result, lock the screen, and verify audio continues.
