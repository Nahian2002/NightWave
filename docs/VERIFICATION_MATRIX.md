# NightWave — Verification and Release Readiness

**Evidence date:** 19 October 2026  
**Audited branch:** `fix/nightwave-v2-phase6x8c4-daily-mix-unit-test-compile`  
**Application identity:** `com.nightwave.player` · `2.0.0-alpha5s` (`35`)

A code implementation, committed regression test, source preflight, actual Gradle compilation, on-device test and signed release are **different levels of verification**. No source audit or test count can honestly guarantee that every device and feature is bug-free.

## Latest observed build — user-supplied Windows logs

| Step | Evidence | Result |
| --- | --- | --- |
| `:app:compileDebugKotlin` | Completed in 2026-10-19 13:34 local build | **PASS, previous commit only** |
| `:app:compileDebugAndroidTestKotlin` | Completed in same build | **PASS, previous commit only** |
| `:app:assembleDebug` | Completed in same build | **PASS, but overall verification FAILED** |
| `:app:testDebugUnitTest` | `771 tests completed, 1 failed` | **FAIL** |
| Failing JUnit | `OfflinePlaybackQueueTest.jumpHandlesBoundsWithoutDamagingSelection` at line 43 | **FAIL on old source** |
| Kotlin warnings | Transfer-history nullable File, deprecated NewPipe field/Compose icons, ineffective Media3 opt-in | Non-fatal warnings on old source |
| `BUILD SUCCESSFUL` for audited branch | No full passing build result supplied | **NOT CONFIRMED** |

Do not install `dist/NightWave-debug.apk` from that failed build or confuse it with `dist/NightWave-debug-previous.apk`.

## Source corrections committed after that failure

| Area | Source-reviewed change | Evidence still needed |
| --- | --- | --- |
| Offline playback queue | Non-shuffled Previous follows visible order; visited-stack semantics reserved for shuffled playback; repeat one-item shuffle under Repeat All fixed; invalid jumps preserve selection | Rerun full JUnit suite and device navigation |
| Public streaming queue | Explicit selection tracks the original URL after de-duplicating input entries | JUnit tests and screen-to-service integration |
| Local Daily Mix compiler calls | Previous fixes preserved flat `List<VideoPreview>` arguments | Fresh full JUnit result |
| Transfer journal | Canonical parent is checked for null and limited to app-private files/cache roots | Fresh compiler plus isolated Android storage tests |
| Public extractor and UI | Use NewPipe's supported `isVideoOnly()` method; Compose auto-mirrored icons | Fresh compiler and UI checks |
| Media3 annotation | Removed ineffective Kotlin `@OptIn` for Media3 `UnstableApi`, retained API annotation; source preflight reconciled | Full compile and device runtime |
| Repository/CI | Current branch included in source-build workflow; README/About/changelog/build docs aligned | CI runner success or Windows evidence |

**Tests added in source:** nine JVM methods across offline/public queue behavior after the 771-test report (total should be approximately 780 if all prior test sources are unchanged). This is **not** a claim that 780 tests have passed.

## Feature-level acceptance

| Feature domain | Code present | Full Android/device validation of audited branch |
| --- | --- | --- |
| Import music and offline library persistence | Yes | Pending |
| Saved public-video searches and playback | Yes, depends on external sources | Pending |
| Public/offline queue, Next/Previous, Repeat/Shuffle | Yes, recent corrections | Pending |
| Background/lock-screen/Bluetooth audio focus | Yes | Pending |
| Gapless, crossfade, volume normalization, EQ and timer | Yes | Pending |
| Favorites, history, playlists, folders, smart collections | Yes | Pending |
| On-device Daily Mix and optional listening memory | Yes | Pending |
| LRC import, editing, synchronization and export | Yes | Pending |
| Guest profile and local data recovery | Yes | Pending |
| Google sign-in, Drive backups/recovery and private music locker | Yes, user authorization required | Pending |
| Home-screen widget/Media3 controls | Yes | Pending |
| Android Auto browse/search/playback | Development implementation | Pending emulator/car-device acceptance |
| Safe upgrade/signing identity | Build identity retained, signing not proven | Pending signer match |

## Required release gates

1. On a clean source checkout, run the official Windows builder and capture **`BUILD SUCCESSFUL`** for all three Gradle tasks: `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:compileDebugAndroidTestKotlin`.
2. Inspect `dist/NightWave-build-status.txt`, APK identity and cryptographic hash, then verify signing compatibility with the installed app.
3. Test the critical playback, storage, restoration, UI, Google and media-device scenarios from [Final Acceptance Checklist](NIGHTWAVE_FINAL_ACCEPTANCE_20261019.md) on a phone/emulator.
4. Check crash/ANR behavior, data preservation and failure recovery before tagging or distributing a production build.

**Blocker:** No successful full build/device validation for the final audited branch is yet confirmed. Do not uninstall NightWave or erase app storage to resolve an APK signature mismatch. GitHub Actions can produce a differently signed debug APK.

See [Build & Release](BUILD_AND_RELEASE.md) and [Changelog](../CHANGELOG.md).
