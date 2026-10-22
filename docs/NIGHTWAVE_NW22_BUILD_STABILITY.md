# NightWave NW22 — Build and stability recovery

Date: 23 October 2026. Parent: NW21. Scope: make local Android builds safer and more diagnosable. **Draft / NO-GO for release until Android Gradle compilation and device checks succeed.**

## Confirmed upstream blocker

GitHub Actions run [39850377683](https://github.com/Nahian2002/NightWave/actions/runs/39850377683) completed with **failure, no executed job steps, and no compiler log**. This is an **unstarted build**, not a Kotlin/Gradle test verdict. We cannot determine an exact cause (billing, policy, quota, or hosted runner availability) from this result. Account owner can inspect the GitHub Actions run banner and repository Actions settings. A rerun without a change in the blocker is unlikely to help.

## NW22 delivered changes

- Added `tools/kotlin/NW22AudioCoreSmoke.kt`: 24 deterministic checks covering actual production MP3/AAC frame recognition, extension/MIME detection, malicious URL refusal, free-license/source restrictions, truthful audio formats, and end-to-end input mutation detection using the original-file SHA-1 hash.
- Added `tools/nw22_audio_core_smoke.py`: compiles **unmodified production Kotlin files directly from the extracted source ZIP** (not hand-written fakes) and runs the checks as an isolated JVM jar; reports compiler unavailable explicitly rather than PASS. GitHub CI runs this when `kotlinc` exists, followed by mandatory Android Gradle tasks if a runner starts.
- Added `tools/verify_nw22_build_readiness.py`: strict read-only source preflight for the Kotlin compiler runner, Windows build tasks, package identity and APK signer verification; includes a negative fixture test.
- Extended `tools/verify-apk-signature.ps1` to optionally compare the new APK certificate with a trusted installed-app certificate SHA-256 fingerprint. A mismatch fails closed without uninstalling, re-signing, installing or modifying app data.
- Windows builder forwards an **optional** `NIGHTWAVE_TRUSTED_SIGNER_SHA256` environment variable to the signer verifier. With no trusted fingerprint or previously verified APK, signer continuity remains **NOT CHECKED**; it must never be reported as verified. `build-windows.cmd` now explicitly warns of this distinction even when the APK compiles.

## Actually executed on local JVM

Exact Kotlin sources and fixture were verified against GitHub Git-blob SHA-1 fingerprints:

| File | Blob |
| --- | --- |
| `AudioFrameSignature.kt` | `b0dadd795b07b7b0f8898788d4bb9a8f3187a139` |
| `PermittedAudioDownloadPolicy.kt` | `f65e469742e23ed0c3ef1028816847586b4d2afe` |
| `FreeAudioCatalogPolicy.kt` | `29d79c8ff93a0b025f1a15cc466e33c79bd30e13` |
| `FreeAudioIntegrity.kt` | `a2c955078f41f27ef1243b6a3149ee8ba1edc8c1` |
| `NW22AudioCoreSmoke.kt` | `4271afeaef92ae814a6a8d4c1356afe29a68b95e` |

All five hash fingerprints matched the actual development repository contents. Kotlin/JVM **kotlinc 1.9.0 / OpenJDK 21** built the jar successfully. Running `java -jar` returned:

```text
PASS: NW22 24 production Kotlin audio policy assertions
LIMIT: Android compilation, downloads and APK signing not tested
```

This is executable evidence for the four pure Kotlin production policy files, **not** evidence for Android Media3, Compose, OkHttp networking, app signing, app startup or Samsung device behavior. The offline DB, playback service, YouTube gateway and Google Drive client remain unmodified by NW22.

## Windows local build / release gating

1. From a freshly extracted NW22 ZIP in an empty folder, run `build-windows.cmd`. It performs source preflights, sets up a suitable Java, Gradle and Android SDK, compiles JVM + Android test sources, and checks the freshly built APK identity/signature.
2. If build fails, run `send-build-report.cmd` and use only the current `dist/NightWave-build-diagnostic.txt`. Reports are privacy-reduced but should be reviewed before sharing. Do not send a keystore, private music, OAuth secrets or personal account data.
3. If you have independently verified the SHA-256 signing **certificate** digest of your currently installed NightWave, you may set `NIGHTWAVE_TRUSTED_SIGNER_SHA256` to that **64-character hexadecimal value** before building. This value is not a private key. Never guess it.
4. A successful local debug build **does not certify compatibility with the installed app** unless the trusted/previous signer comparison is MATCH. Do not uninstall or clear NightWave to bypass an update-signature error.

## Still outstanding

- Full Gradle Android compilation and on-device Android instrumentation remain unverified; hosted-runner failure occurs before test execution. No new APK was produced by this NW22 development session.
- Live Commons search and offline transfer, actual stored bytes, pause/cancel, app restart, real phone rendering, Android 15+/16 permissions, sign-in, Google backup and crash behavior must be tested before release.
- Universal YouTube-to-MP3 and DRM/access-restriction bypass are **not** NW22 features. NightWave saves eligible permitted audio sources only.

**Release status: draft, no APK installation recommended yet.**
