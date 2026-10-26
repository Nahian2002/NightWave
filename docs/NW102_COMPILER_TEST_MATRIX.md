# NW102 — Kotlin/JUnit Test-Source Compiler Gate

Date: 27 October 2026. Parent: `7e1dbc169e8c104da06928687ca550a37db7fd5d`.

This phase adds an independent JVM compilation and execution gate for **nine actual production Kotlin policy files and eight existing Android module JUnit test files (53 genuine `@Test` methods)** from NW95–NW100.

The Android Gradle runner has repeatedly failed before any step. Previous Kotlin policy smokes did not compile all of the new *JUnit test source files*. The new `tools/nw102_kotlin_junit_gate.py` compiles those exact existing production/test files together using a locally installed `kotlinc`, then executes their original `@Test` method bodies via Java reflection.

A minimal JUnit-API-compatible **temporary Java assertion/annotation bridge** supports source compilation outside the Android/Gradle environment; a deliberately failing assertion must throw before the test runner starts. The bridge is never added to the application or official tests. A missing Kotlin CLI, missing test, compilation error or failed assertion is a gate failure, not SKIP or PASS.

A JSON receipt writes the SHA-256 of all 17 Kotlin inputs, total executed tests, and explicit warnings `android_compilation=NOT_VERIFIED`, `official_gradle_junit=NOT_RUN`, `device_instrumentation=NOT_RUN`.

| Source phase | Original JUnit class | Cases |
|---|---|---:|
| NW95 | AudioRouteTransportPolicyTest | 6 |
| NW96 | EqualizerSelectionPolicyTest | 6 |
| NW97 | OfflineShuffleCycleIntegrityTest | 8 |
| NW98 | PlaybackResolutionPolicyTest | 8 |
| NW98 | WidgetPlaybackIntentPolicyTest | 5 |
| NW99 | MediaSessionQueueControlPolicyTest | 8 |
| NW100 | PlaybackQueueManualNextAvailabilityTest | 6 |
| NW100 | OfflineManualNextAvailabilityTest | 6 |
| **Total** | **8 existing test classes** | **53** |

CI executes bridge self-test and real source compilation before Gradle when a runner is allocated. The existing Android compile/JUnit and device gates remain independently mandatory. This check deliberately does not claim Media3 API/Compose compatibility, successful APK signing, Android notification functionality, source rights or physical-device acceptance.

**Full Android build / public release: NO-GO.**
