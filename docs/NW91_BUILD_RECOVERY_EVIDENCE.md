# NW91 - Build recovery and reliability foundation

**Base:** NW90 cumulative commit `2d74f1009f0cc2f515515cce240542e43fa9c3d5`.
**Branch:** `feature/nightwave-nw91-build-recovery-reliability`.
**26 October 2026.** **Status: source fixes committed; Android full build/device certification NOT VERIFIED; public release NO-GO.**

## Reproducible defects corrected

1. **Skipped JUnit tests counted as executed:** both the Python NW90 evidence parser and Windows NW76 JUnit receipt accepted 1,000 *total* cases including skipped ones. Both now require 1,000 **executed** cases; failing/error test counts continue to block.
2. **Malformed 139-case matrix admitted as valid:** the Python parser accepted 139 distinct, correctly formatted IDs even if `NW90-139` was replaced by `NW90-999`. It now requires the exact 001–139 set and rejects altered CSV header/extra fields and parser errors.
3. **Unbound historical JUnit proof:** an arbitrary successful Gradle log could authorize older JUnit XML. Hosted CI now removes previous test reports before its build, touches an attempt marker, reruns the test/build tasks without Gradle build cache, and binds XML/log timestamps to the marker. Missing or stale evidence remains `NOT_VERIFIED`.
4. **Windows status-fingerprint mismatch:** the post-build PowerShell gate now checks that the on-disk source fingerprint manifest hashes to the SHA256 in the successful build status. An old/copied status and a different fingerprint cannot pass this additional check.

## Deliverables

- `tools/verify_nw91_certification.py`: disposable fake receipt checks for skipped-count rejection, wrong matrix IDs, absent attempt marker, stale XML and terminal build failure; no secrets or user files.
- `run-nw91-final.cmd`: convenient Windows execution using the existing, stricter read-only PowerShell preflight, full builder and post-build APK/source gate. No phone installation.
- New NW91 CI branch triggers (Android workflow and minimal runner canary), with source regression checks and current-attempt JUnit evidence.
- No production Kotlin, Android manifests, persistence, OAuth credentials, music, signing or media-source permissions changed.

## Evidence and limitations

- Verified using GitHub source readback and structural/source-contract review.
- Runner-blocked CI cannot produce a current Kotlin/Gradle/JUnit/APK result until GitHub runs a job. The earlier Android run and minimal runner canary both failed with **zero executed steps**; that is not a compiler verdict.
- Regression fixtures require actual execution in Python 3. The hosted job is set to execute them; if it does not reach steps, they are **not verified** by CI.
- No Samsung/Android emulator, Google Drive restore or 139-case acceptance run was performed in this commit. All 139 cases remain unexecuted in the baseline matrix.
- `app/build.gradle.kts` remains targetSdk 35; Play API 36 migration, media rights, release signing, privacy and production testing remain separate blockers.

## Owner's safe local build

Download this NW91 branch as a **fresh ZIP** on Windows (don't overlay an old ZIP), run `run-nw91-final.cmd`, then inspect `dist/NightWave-build-status.txt`, `dist/NightWave-jvm-test-proof.txt`, source fingerprint, compiler log, and `dist/NightWave-debug.apk`. If any stage fails, send only redacted reports from `send-build-report.cmd`. A success still **does not authorize installation**: run `check-device-update.cmd` first and preserve existing app/data if signing mismatches.

**NW92 cannot claim runtime reliability based on these source checks alone.**
