# NW110 — Final Integration and Release Certification Evidence

Date: 27 October 2026 · Parent `9babafdf9905fce8ebb0892bf497c19c19e7f4b6`. Cumulative branch `feature/nightwave-nw110-final-integration-certification`.

## Independent source regression execution — PASS (JVM only)

Every input file was reconstructed or copied from prior verified local NightWave test work and its **exact GitHub blob SHA** was checked against the NW109 cumulative branch. The nine real production policy files and eight ORIGINAL Kotlin JUnit test source classes, not recreated test approximations, were compiled together in one invocation using a local JDK 21 and Kotlin 1.9 CLI. A minimal runtime-retained JUnit annotation/assertion compatibility bridge was used only outside the Android module. The runner invoked every method.

| Test class | Executed / Passed |
|---|---:|
| AudioRouteTransportPolicyTest | 6 / 6 |
| EqualizerSelectionPolicyTest | 6 / 6 |
| OfflineShuffleCycleIntegrityTest | 8 / 8 |
| PlaybackResolutionPolicyTest | 8 / 8 |
| WidgetPlaybackIntentPolicyTest | 5 / 5 |
| MediaSessionQueueControlPolicyTest | 8 / 8 |
| PlaybackQueueManualNextAvailabilityTest | 6 / 6 |
| OfflineManualNextAvailabilityTest | 6 / 6 |
| **Total** | **53 / 53** |

The last six public queue cases were executed against the **real** PlaybackQueue implementation and did not use the offline adapter. Public, shuffle/offline, headset, EQ and widget policies were then compiled and run **together**. All 17 file blob SHAs matched current cumulative NW109 sources before the run. The independently run source tests are not Gradle's official JUnit execution.

## Additional deterministic and randomized checks — PASS (JVM only)

`tools/NW110PublicQueueStress.kt` compiles the real public queue policy and reproduces a seeded 90 × 120 transition matrix, covering shuffle toggles, Repeat states, jumps, Previous/Next, manual Play Next, stale removal attempts and priority restoration. It checks 3 invariants after each transition: availability is idempotent; the probe does not modify song/history/priorities; and manual Next returns a target iff availability was true. **32,400/32,400 assertions passed**. Tests do not reach real Media3, Android notification, internet streams or actual offline file decoders.

## Release evidence is intentionally fail-closed

`tools/nw110_final_certification.py` independently checks that an NW102-style policy-JVM receipt has exactly 53 executed methods, eight classes, 17 source files, unmodified SHA-256 for **every** source file, and explicitly `android_compilation=NOT_VERIFIED`. Ten synthetic tests confirm valid source proof is accepted as `PASS_POLICY_JVM_ONLY`, whereas a stale count/status/hash, altered/deleted source or absent receipt is rejected. This checker **always** returns `release_decision=NO_GO`; neither a source-only suite nor a debug APK can waive manual rights, signer, Google Drive or device gates.

CI runs the stress script plus existing NW102 53-method real-source JUnit gate, records the NW110 certification receipt even if subsequent Android tests fail, and preserves a specifically **NO-GO** artifact if the GitHub runner executes. A runner allocation failure produces NO execution artifacts and therefore NO assertion of Android success.

## Required final real-world proof — NOT COMPLETE

- Run `run-nw91-final.cmd` from this full current cumulative source on Windows, retaining fresh Gradle test XML, Android compilation log and debug APK identity receipt. NW101 re-attests every input after the build; never accept an old APK.
- Verify current installed signing certificate with `check-device-update.cmd` before any same-package update. **Never uninstall or clear data** to bypass a signer mismatch.
- Execute and record all 139 device acceptance cases, specifically Android 16 API 36 edge-to-edge/back navigation, background media/audio focus, headphones and notifications, offline file transfers, gapless/EQ, queue, LRC TalkBack accessibility, low storage and process recovery.
- Verify production Google OAuth/Drive appDataFolder account login, backup/upload/readback, merge/restoration preview, download and disconnect/reconnect on a physical device.
- Independently review published privacy policy/data safety, secrets across complete git history, third-party dependency licenses, signed bundle and applicable store requirements.
- The existing NW77 YouTube audio extraction/standalone background playback/offline save routes remain a public distribution **RIGHTS BLOCKER** without compliant redesign or authorization. A locally passing APK does not resolve this.

The current GitHub-hosted Android and empty runner canary both fail **before steps are allocated**; the root infrastructure problem is not solved by NW110 and is not a Kotlin compiler result.

## Decision

**NW110 roadmap code/source-integration milestone: COMPLETE.**

**Public APK/Play release certification: NO-GO.** No claim of bug-free Android or certified public availability. Source rights, real build, signing, device QA, Google Drive and Play compliance gates remain.

No changes to app code, local saved music, stored playlists, backup schema, Google scope, package `com.nightwave.player`, signer, min/target SDK or version numbers in NW110.
