# Build and update NightWave

Download the [version 55 APK](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55). To build its matching source, check out the [version 55 source tag](https://github.com/Nahian2002/NightWave/tree/nightwave-v55).

1. Use a fresh writable checkout on Windows and run `build-windows.cmd`.
2. Check the fresh `dist/NightWave-build-status.txt`, JVM receipt and APK provenance. The builder compiles the app and Android test sources and runs the host tests.
3. Run `check-device-update.cmd` with the phone connected. Compare the candidate and installed signing certificates. Preserve the existing app and library if the certificates differ.
4. For source-only checks, run `python tools/verify_current.py`. Run Android lint separately after the builder finishes.

Version 55 uses package `com.nightwave.player`, minimum API 23 and target API 36. The signed in-app update channel currently offers version 51. The app verifies the channel metadata, APK bytes and signing identity before handing installation to Android.

[Test evidence](../TEST_REPORT.md) · [Earlier build instructions](https://github.com/Nahian2002/NightWave/blob/7667b68535bbdbc09b69350357ba2ca408e0730b/docs/BUILD_AND_RELEASE_HISTORY_20261027.md)
