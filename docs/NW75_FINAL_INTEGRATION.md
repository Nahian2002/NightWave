# NW75 — Final Feature Milestone and Integrated Build Handoff

**Cumulative source:** `feature/nightwave-nw75-final-integration-gates`  
**Parent:** `feature/nightwave-nw74-ui-state-crash-guards`  
**Milestone:** NW75 is the LAST **planned source feature batch**, not the final release certificate.  
**Release gate:** **NO-GO** until complete Windows Android build + project JVM tests, signer check, phone QA and legitimate download/source review actually pass.

## Verified final source changes

1. Repaired an actual **NW71 JUnit regression**: `SmartDiscoveryPoolPolicyTest.hugeFavoritesAndRecentDoNotHidePlaylistOnlyTracks` expected 250 playlist items from ONE playlist, even though the production policy intentionally reads at most 200 songs per playlist. The new fixture uses two playlists, 200 + 200, and still expects 250 total within the playlist category. **Production Smart Library logic, saved playlists and library data are unchanged.**
2. Added the **reproducible exact-source multi-policy Kotlin integration smoke**: `tools/NW75IntegrationSmoke.kt`. It compiles and executes FIVE existing Kotlin production sources together (Smart Library pool, UI-state selection guard, backup freshness, Drive account-session guard and backup consent). No Android SDK, network, credentials or personal library is required.
3. Extended `tools/check-nw59-integration.ps1` (the already-required zero-Python Windows preflight) with NW75 source checks for the repaired JUnit quota, the new reproducible smoke and preservation of NW72–NW74 safeguards. Windows builder continues to run this preflight **before Gradle**. Passing it is **not** compilation or app runtime proof.
4. Added NW75 to the Android GitHub workflow branch filters. If the hosted runner provides `kotlinc`, the workflow now executes this pure JVM regression **before** the existing full Gradle compile/test gate. Absence of `kotlinc` is still labeled SKIP, never PASS, and does not replace Android Gradle.
5. No app package ID, app signing, OAuth scopes, Google Cloud client, Media3 playback, file format, audio storage or data schema were modified. Do not merge to main or publish an APK based on this milestone alone.

## Executed local evidence

The exact five committed production Kotlin blobs compiled together under locally available Kotlin/JVM and passed **27,016/27,016** standalone assertions across:
- NW71 quota boundaries, source preservation, 200-entry per-playlist cap and fair 600-slot dynamic pool;
- NW74 selection of an existing playlist, invalid native-dialog indices and imported-playlist identity;
- NW72 same-account reconnection and busy action rejection before restore;
- NW73 backup freshness based on the exact verified privacy revision.

3,000 seeded integration iterations stress read-only source lists, indexed dialogs, cloud revisions and bounded recommendations. The committed `tools/NW75IntegrationSmoke.kt` blob matches the executed local source (**`fdcd4a18ff32c881d62e6240642683fca5205ec0`**).

Exact production Git blob SHA-1 values:
- `SmartDiscoveryPoolPolicy.kt`: `a547fd017ac922ac070a7d2d030425615262e3e1`
- `StudioUiStateSafetyPolicy.kt`: `70c87759e48ad0608e0e92a49f1f5cd84aa75b33`
- `CloudBackupFreshnessPolicy.kt`: `34e113b1f7a0527d25d5c42b61b72a610eccb50a`
- `CloudAccountSessionPolicy.kt`: `673c91e59030ccce1e86dac83284e1056ed4e9f4`
- `CloudBackupConsentPolicy.kt`: `c4d71ab2ca51e10c4057346ea29e8eec74ebc30a`

**Important:** These are genuine isolated JVM checks, **not** a complete Android Studio/Gradle build, execution of Android project JUnit suites, or real device proof. [NW75 hosted CI run 40279811067](https://github.com/Nahian2002/NightWave/actions/runs/40279811067) reported **failure** but the connected job returned **zero executed-step details**. It remains unknown whether a hosted runner launched or Android/Kotlin compilation began; this is **not** proof of an Android compiler failure, nor a successful build.

## Reproduce final pure Kotlin regression

In the root of this branch, with JDK and `kotlinc` installed:

```sh
kotlinc app/src/main/java/com/nightwave/player/v2/library/SmartDiscoveryPoolPolicy.kt app/src/main/java/com/nightwave/player/v2/ui/StudioUiStateSafetyPolicy.kt app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupFreshnessPolicy.kt app/src/main/java/com/nightwave/player/v2/cloud/CloudAccountSessionPolicy.kt app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupConsentPolicy.kt tools/NW75IntegrationSmoke.kt -include-runtime -d nw75-integration.jar
java -jar nw75-integration.jar
```

## One complete Windows build — next mandatory gate

1. On Windows choose **Code → Download ZIP** from the exact NW75 cumulative branch. Extract to a **new clean folder**, not over the installed app, existing downloaded music, or old output files.
2. Run **`build-windows.cmd`** from the extracted folder. It invokes `tools/build-windows.ps1`, validates source/Windows script consistency, downloads verified build tools when necessary, runs `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`, verifies the APK, and retains real diagnostic logs.
3. Only `BUILD SUCCESSFUL` with current source/signing evidence counts. If the build fails, run **`send-build-report.cmd`** and share the current `dist/NightWave-build-diagnostic.txt`, `dist/NightWave-build-status.txt`, `dist/NightWave-build-errors.txt` and `dist/NightWave-build-full.log` (or the ZIP created by the report command). **Do not install a stale prior APK.**
4. If and only if a fresh APK has been verified, run `check-google-oauth.cmd`, `check-device-update.cmd` and compare its signing certificate with the *already installed* NightWave application before updating. **Do not uninstall the installed app or clear application data to fix signing, Google error 8 or build problems.**
5. Device QA on Samsung: start/restart music, gapless/next/previous/shuffle/repeat, lock-screen background playback, equalizer routing, crossfade, search (Bangla/English), playlist changes, offline transfer/pause/retry/file verification, recently played, UI/navigation/rotation, backup preview and real Google Drive connection on a disposable test profile. Preserve existing downloaded audio.
6. Final public-release review: handle the still unresolved **Google OAuth internal error 8**, validate downloader eligibility/source rights, privacy disclosures, crash telemetry, backup durability, release signing and Play policy. **Public APK/Play remains NO-GO until evidence passes.**

**No live Google authentication, full APK build, on-phone playback, rights assessment or public deployment has been claimed or performed during NW75 source integration.**
