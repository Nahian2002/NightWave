# NightWave 55.1 public source plan

The owner authorized changes to the YouTube paths blocking public release. The build must hand off YouTube references to the official service and reject extracted stream downloads, including old pending work, while keeping the existing library, playlists, imported files, provider choices, backup and updater identity.

Keep display version 55.1. Use Android build 60 so phones with the earlier 55.1 test build 59 can also update. Record focused regression checks, exact signed artifact, S24 evidence and the remaining release cases. Verify the public asset and the signed stable feed separately.

## Release result

NightWave 55.1 (Android build 60) is [published on GitHub](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55.1). The separate [stable feed PR](https://github.com/Nahian2002/NightWave/pull/196) activated the signed offer on main. Its merge revision is `5a3a65a1aa2e4a93f51a6df9dd74efe520c8a824`.

- Exact source and release tag revision `5ac4f1ddbb3aa10a5453df818e888c8cadee99ed`. Embedded build `NW-debug-E7C49F4802A3B61D`. APK SHA-256 `1D4C65C293DF635179D12745B4E769B6CFDB169E91A6C0FAB75AA6BC41000390`, 29,648,966 bytes. Package `com.nightwave.player`, signer SHA-256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`.
- The Windows builder passed 1,904 JVM tests in 242 suites and compiled Android instrumentation tests. The 95 source checks passed. Sequential lint passed with 0 errors, 321 warnings and 17 hints. The NW77 audit found zero known blocking paths with the official-only build boundary. The static audit still calls for manual review.
- The S24 accepted an in-place install from build 59 to 60. All 16 checked app files matched before first launch. The existing Home history remained visible, the four provider sections rendered after an explicit search, YouTube opened in the official app, and the verified Drive backup receipt matched its current consent revision. The broader physical matrix, full restore, and older-phone public download-to-installer sequence remain pending.
- The public APK passed full anonymous byte and SHA-256 readback. The canonical stable endpoint returned the exact signed build 60 envelope. Some requests initially returned the prior build from cache. The retired build 59 draft stays private under its internal tag.

YouTube searches and track references now open in YouTube. The public build blocks extraction and new YouTube offline saves, including previously pending save jobs. Existing library references, app data and local audio files were retained.
