# NightWave Phase 6V — Combined Android Build and Integration Verification

Branch: feature/nightwave-v2-phase6v-integrated-build-audit
Base: the Phase 6R3B branch, containing Google Drive Backup 3.0 and the Music Locker stages.

## Confirmed GitHub Actions blocker

The 18 October 2026 GitHub Actions run 39108775419 on PR 68 finished in about four seconds with runner_id 0, zero job steps, and zero billed Ubuntu runtime. Its check-run shows two annotations but their text was not accessible through the connected GitHub tools. No Gradle compilation started and there was no compiler log. The exact cause is UNKNOWN; it is not evidence that Kotlin code is either valid or invalid.

## Implemented

- tools/check-phase6v-integration.ps1: standalone, read-only and no-network Windows PowerShell 5.1 cross-phase source verifier. It checks the existing app identity, the metadata/lyrics/preferences backup versions, Drive permission separation, safeguarded restore, verified music upload/download and exact checkpoint policy, offline import, and retained signing gates. It has a -SelfTest option with deliberately valid/invalid synthetic checks.
- tools/verify_v2_phase6v.py: portable Python audit of cloud/locker contracts, XML manifest, Gradle identity/dependencies, runner build tasks, Kotlin source package paths/imports and AST syntax of all phase regression scripts. It does NOT compile Kotlin or call Google APIs.
- tools/build-windows.ps1 invokes the new source preflight after 6P checks and before downloading or using Java/Android/Gradle. tools/check-builder-syntax.ps1 now parses the new PowerShell script before any build.
- GitHub Actions adds the source checker after earlier phase gates and retains real Gradle testDebugUnitTest, assembleDebug and compileDebugAndroidTestKotlin tasks.
- The local Windows one-click build still validates the true Gradle BUILD SUCCESSFUL marker and checks APK signing identity/certificate against a prior APK, when supplied. It does not install or uninstall anything on a phone.

## Verification achieved vs. still blocked

Verified with GitHub connected source inspection: the 6V files and branch, staged check hooks, retained cloud/media safety tokens, Android app ID com.nightwave.player, versionCode 35, and existing Google scope separation. Source inspection is not a compiled Android test.

Not verified: actual PowerShell execution, Python execution against a checked-out repository, Kotlin/Android compilation, Android/JVM unit-test execution, device-test compilation, Google Drive OAuth, cloud transfer, APK signer continuity or installation on the user's phone. The GitHub Actions runner still has to become available or a local Windows build has to run.

## When the combined build is eventually run

Use the latest full-source Phase 6V branch and its existing build-windows.cmd launcher on a Windows machine. The builder prepares official Java/Gradle/Android SDK tools, checks source and the real Gradle output, and writes a build report under dist. If it fails, send-build-report.cmd prepares a compact diagnostic report. No need to repeatedly test each development phase on the phone.

Never install a differently signed debug APK over an app containing personal playlists, offline audio and lyrics. Preserve existing data and use check-device-update.cmd for comparison before installing anything. Android debug artifacts produced on hosted CI may not share the phone's installed signing certificate.

Only after an actual successful build and a compatible signer check should controlled Drive testing follow: metadata backups, optional LRC/preferences, a small authorized local MP3 upload, an interrupted transfer and verified download. Never use data deletion as a shortcut.

## Excluded

Automatic background cloud sync, real-time lyrics extraction, unrestricted streaming-to-MP3 features and persistence of sensitive Google upload session URLs are not part of 6V.

This is a draft build-validation milestone, not a released APK.
