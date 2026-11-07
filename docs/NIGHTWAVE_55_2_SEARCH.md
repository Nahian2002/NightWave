# NightWave 55.2 search repair

Build 60 changed Music search from a broad YouTube handoff to opening the smaller Sources catalog. Restore the official YouTube search action for Music submissions and offer it directly from every query scope. Preserve local typing previews and explicit provider searches. Verify the reported query on the connected phone, then publish a signed build 61 through the stable feed.

## Verified release

- Reproduced the build 60 regression on the S24 Ultra using `ek paye nupur amar`. Find music changed to Sources, displayed the local empty state, and removed the YouTube action.
- Build 61 kept the official search available in Sources. The exact query opened the official YouTube app and a matching result was visible. The Music keyboard action also opened the official app. In-place install from build 60 preserved all 17 checked files before first launch.
- Windows build passed 1,904 JVM tests, 95 source checks, Android test compilation and sequential lint with 0 errors, 321 warnings and 17 hints. Android test APK execution and older-phone installer handoff were outside this device check.
- Exact source `15545614bacaf4eab1d1004ce3497b0186bafb1d`, build `NW-debug-63793E95F7D20E8D`, APK SHA-256 `236C2AEFA71A294420AF2855EBA64CA1C9D5EB297234E7F3A1DE663E40438002` and size 28,295,054 bytes. Package `com.nightwave.player`. Signer SHA-256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`.
- [NightWave 55.2](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55.2) is published and passed full anonymous APK readback. The [stable feed PR](https://github.com/Nahian2002/NightWave/pull/197) merged as `9d443737c8b757bd46dec6635d96dcf32deed290`. Main returns the exact signed build 61 envelope by branch ref. Initial requests to the updater URL still returned build 60 from GitHub's cache.
