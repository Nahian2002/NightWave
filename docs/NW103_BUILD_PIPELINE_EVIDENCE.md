# NW103 — Build Pipeline Recovery and Final Certification Preparation

Date: 27 October 2026. Parent NW102: `7ad41091249d1b0577d1686763adfbc0e5df5d13`. Cumulative source branch: `feature/nightwave-nw103-build-receipt-final-gates`.

## Confirmed NW102 evidence bug

The standalone Kotlin/JUnit source runner wrote its JSON receipt **only after success**. On a subsequent failed or toolchain-blocked attempt, an old passing JSON file might remain on disk, giving a misleading impression that the new source had passed. The previous runner also computed source hashes *after* executing tests, so a source edit during compilation could be incorrectly attributed to the tested code.

## Fix

- A real test attempt first atomically invalidates any older receipt with `status=NOT_VERIFIED` and `executed_tests=0`, including when no JDK or Kotlin CLI is installed.
- Before compiling, calculate SHA-256 for all nine production policy files and eight original JUnit Kotlin test files. After every test has run, independently recompute these hashes. Reject a changed source rather than issuing a passing receipt for a different checkout.
- Only a successful 53-test run gets an atomic `POLICY_JVM_TESTS_PASS_ONLY` receipt. Any failure leaves `NOT_VERIFIED` and a nonzero exit status.
- Negative fixtures explicitly simulate old PASS, changed source, failed attempt, atomic success and a second run. They do not call any remote service or modify the NightWave app.
- CI always uploads the bounded standalone JVM JSON receipt when available, even if a subsequent Android Gradle step fails. It has a **NO-ANDROID-APPROVAL** label.
- The Android verification workflow already has `workflow_dispatch` support. NW103 preserves that manual trigger and the separate minimal runner canary. It does **not** attempt to bypass GitHub runner/account restrictions or to certify an Android APK without a runner.

## Final Windows build handoff

When ready to perform the final real build on the owner's Windows PC:

1. Download the full NW103 cumulative source ZIP from the NW103 branch and extract into a new, empty short path (not over an old extraction).
2. Run `run-nw91-final.cmd`. This executes the NW90 source/139-case matrix preflight, full current Android debug Gradle tasks, current JUnit proof, NW101 source-file re-attestation, APK identity and debug-signature verification. It never automatically installs, uninstalls or clears Android app data. **A passing local debug build is not public release approval.**
3. If it fails, use `send-build-report.cmd` and inspect only fresh redacted diagnostics from the same attempt; no stale APK should be installed.
4. If it passes, use `check-device-update.cmd` to compare the candidate signing certificate with the **existing installed app** before any same-package update. Never uninstall to get around a signer mismatch because that can erase local downloads and backups.
5. Complete the 139 manual/device acceptance cases, Google Drive account/restore checks, source rights and target API review separately. These are not automated or waived here.

## Verification and limits

NW103 makes evidence fail closed, but it does not repair an Android build or GitHub runner allocation. Recent Actions jobs—including the no-dependency canary—ended with no started steps; no compiling job can be declared passing on that basis. Kotlin/JVM regression tests remain a subset of the full app and use a temporary JUnit-API bridge.

No NightWave Android app, signing files, downloaded music, library database, playlists, account settings, Google OAuth, package identity or manifest changes.

**Release status: NO-GO** until full Android compilation, actual Gradle/JUnit execution, signer continuity, device QA and legal/Play requirements are verified.
