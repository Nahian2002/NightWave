# NW104 — Kotlin JUnit Compiler Blockers Resolved

Date: 27 October 2026. Parent `3f77610086a103b8ff8156d25460e4b086bd9a3e`. Branch `feature/nightwave-nw104-kotlin-junit-compiler-fixes`.

## Confirmed compilation failures

Direct `kotlinc` compilation of the original JUnit test source revealed invalid nested-enum access through instance alias `p`.

- `AudioRouteTransportPolicyTest.kt`: `p.Action` and `p.Key` replaced by `AudioRouteTransportPolicy.Action` and `AudioRouteTransportPolicy.Key`.
- `WidgetPlaybackIntentPolicyTest.kt`: `p.Command` replaced by `WidgetPlaybackIntentPolicy.Command`.

These are real Kotlin errors (nested enum class accessed via instance reference), rather than GitHub's pre-runner infrastructure failures. No production Kotlin app file changed.

## Executed evidence

Five existing Kotlin JUnit test classes and their production policies were obtained from NW103's exact GitHub source; the local bytes were checked with `git hash-object` against GitHub blob SHAs before repair. Both repaired test files were compiled together with the exact production policies on a local JDK/`kotlinc`. A temporary JUnit API compatibility shim was used outside the repo; it is NOT Android's real JUnit implementation.

| Original JUnit class | Executed |
|---|---:|
| AudioRouteTransportPolicyTest | 6/6 |
| EqualizerSelectionPolicyTest | 6/6 |
| PlaybackResolutionPolicyTest | 8/8 |
| WidgetPlaybackIntentPolicyTest | 5/5 |
| MediaSessionQueueControlPolicyTest | 8/8 |
| **Total** | **33/33 PASS** |

The NW104 test fixes have been applied to the development branch. `tools/verify_nw104_nested_enum_test_sources.py` includes seven positive/negative examples and asserts that all eight NW95–NW100 regression test classes still declare 53 JUnit methods. It is added to CI before the existing NW102 real-source JVM test runner.

## Outstanding certification

The remaining 20 JUnit methods of the 53-test matrix have not been executed in this local proof. The official Android Gradle/JUnit task, media service/Compose linking, APK signing continuity, background playback and final phone acceptance remain NOT VERIFIED. GitHub Actions including the trivial canary have failed before running steps; new CI runs do not prove Kotlin compilation unless they actually execute.

No downloaded songs, persistence, playlists, Google Drive auth, package identity or signer files changed. **Public release NO-GO.**
