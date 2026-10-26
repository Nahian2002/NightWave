# NW92 — Data Integrity & Cloud Music Vault Recovery

**26 October 2026.** Based on NW91 `21ff6a1a65cf7f0ddd2ae3ca0b1a01f861cca0ae`. Branch `feature/nightwave-nw92-data-integrity-recovery`.

## Verified defect and correction

The old `OfflineAudioLibrary.importVerifiedCloud()` **moved** a verified Drive staging file to `offline_audio` before committing its index entry. A failed index commit then deleted the moved audio, also destroying the downloaded staging checkpoint bytes. An explicit retry could lose its recoverable file.

NW92 copies the authorized verified staged file to an app-private pending copy, fsyncs the copy, verifies byte length and SHA-256/MD5 hashes, promotes the copy without replacing any existing file, and only then commits the unchanged SharedPreferences index format. A failed hash check, occupied filename, or failed/throwing index commit discards only this operation's new private copy; the verified staged original and checkpoint remain intact for a user-initiated retry. After successful import, the existing locker UI discards the checkpoint normally.

Extra free space for one temporary copy is required; insufficient space fails closed. A process crash between file promotion and index commit can still leave an orphan *copy*; automated cleanup is deliberately not enabled without safe recovery QA.

## Evidence

- Local standalone Kotlin/JVM compiler and executable test: **7/7 synthetic file-transaction scenarios passed** (success, false/throwing index commit, copy verification rejection, protected existing destination/pending file, and verified file visibility at index commit).
- New `tools/verify_nw92_storage.py` source wiring checker, wired into hosted CI.
- Exact-source Kotlin tests also wired into CI when the runner can start and a compiler is present.
- **Full Android compile, Gradle JUnit, Android-test compilation, Samsung runtime, cloud account test, and 139-case matrix are NOT VERIFIED for NW92.** GitHub Actions provisioning has prevented runner steps in recent NW90/NW91 runs.
- NW91 certification safeguards remain cumulative and unchanged.

## Preservation

No change to application package/signature/version, Android manifest, Google OAuth, offline index encoding or song IDs, existing installed files, media providers or serverless architecture. The specific cloud-registration production path and its new helper are the only functional changes. `OfflineAudioLibrary.delete()` is untouched and deserves a separate future audit.

## Owner-side verification

Download fresh NW92 ZIP. Use the included `run-nw91-final.cmd` for the existing cumulative Windows Gradle/JUnit/APK check, then `check-device-update.cmd` before any installation. No uninstall, data reset or deletion should be used to bypass a signer mismatch.

Optional standalone Kotlin smoke:

```
kotlinc app/src/main/java/com/nightwave/player/offline/OfflineFileRegistration.kt tools/NW92OfflineFileRegistrationSmoke.kt -include-runtime -d nw92-smoke.jar
java -jar nw92-smoke.jar
```

Public release remains NO-GO due to source rights, Play target API, signing/privacy and runtime acceptance requirements.
