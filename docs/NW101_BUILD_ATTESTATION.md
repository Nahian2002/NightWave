# NW101 — Build Evidence and Local Source Attestation Recovery

Date: 27 October 2026. Parent `d5c2f0de2ed4082bfd34284b6dd6881dd7821ab9`. Cumulative branch `feature/nightwave-nw101-build-attestation-recovery`.

## Confirmed post-build attestation gap

The existing Windows `run-nw91-final.cmd` entrypoint calls `nw90-windows-gate.ps1 -AfterBuild` after a successful Gradle attempt. That gate already checked the debug APK hash, JUnit proof, and whether the build-status fingerprint SHA256 equaled the saved manifest **file hash**. However, it did **not** re-read or re-hash the current source files listed inside that manifest.

This means a developer could edit `app/src` or a build script **after the APK was successfully built** without changing the stored manifest or build status. The old post-build verifier would then treat an older APK as a successful build of the current checkout.

The actual Windows builder already compares input hashes *during its own build*; the missing guarantee was independent re-attestation at the downstream post-build certification boundary.

## Fix

- Add `tools/nw101-source-attestation.ps1`, written using ASCII PowerShell 5.1-compatible syntax.
- It invokes the exact existing `get-nw08-source-inputs.ps1` enumerator again after the build and computes current SHA256 for every enumerated app source/resource/test, Gradle configuration and relevant tool script.
- It compares all canonical manifest lines (hash plus relative path) **byte-for-byte in ordinal order** to the recorded build-time entries. Modified, added, missing and reordered records fail closed. No files are changed.
- `tools/nw90-windows-gate.ps1` now requires the attestation file, runs its positive/negative fixture self-test in preflight, then enforces full live source attestation **after** the successful APK/JUnit/manifest-digest checks. A mismatch blocks the gate and keeps the APK unapproved.
- `tools/check-builder-syntax.ps1` includes both the NW90 gate and new NW101 verifier in its fail-closed Windows script syntax/encoding inventory.
- Add `tools/verify_nw101_attestation.py` for portable negative fixture checks and CI integration assertions. Update Android and empty canary triggers; CI additionally attempts the PowerShell self-test.

## Verification scope

- Python source-snapshot positive/negative regression cases cover fresh, mutated, added, removed, reordered and duplicated records.
- PowerShell verifier has its own positive/negative cases and will run on the Windows host and on CI when a runner is allocated.
- The source checker confirms the new gate is wired into the existing one-click `run-nw91-final.cmd` -> `nw90-windows-gate.ps1 -AfterBuild` path.
- **The local Linux analysis host cannot run Windows PowerShell 5.1 or Android Gradle.** A passing portable simulation is *not* a Windows runtime proof.
- GitHub hosted Android and zero-dependency canary jobs have consistently failed before any steps, not because of source compilation. Repository code cannot establish the account/organization billing, policy or runner allocation cause. A fresh successful Android build remains **NOT VERIFIED**.

## Safe owner build path

After the user decides to test, download this full branch ZIP, extract to a new directory, run `run-nw91-final.cmd`. NW101 source re-attestation is now automatically required by its post-build gate; the app is **not automatically installed**. Compare signing identity with the currently installed app using `check-device-update.cmd` before an update. Do not uninstall, clear app data or import duplicate libraries to make a new debug signer fit.

## Non-goals

No Android package, manifest, Kotlin app UI, OAuth, music playback, offline files, library data or persistence changes. This is a release evidence fix, **not a new feature wave**.

**Public release: NO-GO** until current Android build/JUnit/instrumentation, device acceptance, signer continuity, live Google Drive checks, Play target API review and music-source rights review are complete.
