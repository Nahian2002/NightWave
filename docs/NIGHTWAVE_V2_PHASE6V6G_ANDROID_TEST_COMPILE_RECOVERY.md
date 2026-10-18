# NightWave Phase 6V6G — Kotlin test-source compile recovery

## Why this phase comes before the visual redesign

Phase 6V6F found GitHub Actions runs ending before runner assignment.
That is still an **infrastructure blocker**, not evidence that Kotlin
or Gradle compilation succeeded or failed. Because the phone build is
deferred, 6V6G focuses on a real test-source defect identifiable without
a running Android SDK, and on catching the same defect in future changes.

## Confirmed defects repaired

Two test files constructed multiline LRC content as:

```kotlin
parse("first line" + "second line".toByteArray(Charsets.UTF_8))
```

Kotlin evaluates the final string's `.toByteArray()` first; the
containing `String + ByteArray` expression has type `String`, while
`TimedLyricsCodec.parse` requires `ByteArray`. These test fixtures
were corrected to encode the **entire** concatenation:

```kotlin
parse(("first line" + "second line").toByteArray(Charsets.UTF_8))
```

The files fixed are:
- `app/src/androidTest/java/com/nightwave/player/v2/ui/NightWaveLyricsSearchUiInstrumentedTest.kt`
- `app/src/test/java/com/nightwave/player/v2/lyrics/LyricsEditorSearchPolicyTest.kt`

No production music, saved lyrics, playback, accounts, Drive, UI features,
package identifiers or signing configuration changed to fix these tests.

## Verified reproduction

An isolated real **Kotlin 1.9 JVM compiler** run against a minimal
representative fixture produced:

```text
type mismatch: inferred type is String but ByteArray was expected
```

The corrected representative fixture compiled and ran. This demonstrates
the underlying language issue; it **does not** certify the entire Android
project or the exact two Android/JUnit suites as compiled.

## New build regression protection

- `tools/check_kotlin_bytearray_fixtures.py` scans both Kotlin test source
  trees, including nested folders; `--self-test` includes multiline,
  same-line, correct-parenthesized, and single-string controls.
- `tools/check-kotlin-bytearray-fixtures.ps1` provides a **Python-free**
  equivalent for Windows PowerShell 5.1 and an independent synthetic
  `-SelfTest`. The existing `tools/check-builder-syntax.ps1` adds its
  syntax to the parser gate.
- The Windows one-click builder runs the synthetic and full Kotlin
  fixture checks before provisioning Java/Gradle. If the source regresses,
  the build fails rather than accidentally delivering a stale APK.
- GitHub Actions runs the Python self-test and source scanner before the
  **unchanged** `testDebugUnitTest`, `assembleDebug`, and
  `compileDebugAndroidTestKotlin` Gradle tasks.
- A read-only connector-backed scan of the branch's full Git tree checked
  all **93 Kotlin files** under `app/src/test` and `app/src/androidTest`.
  It found no remaining instance of this particular ungrouped-literal
  conversion pattern after the two corrections. Other Kotlin syntax,
  runtime or Compose issues are not ruled out by this scan.

## Build acceptance (when the user chooses to test)

1. Extract the **complete pinned 6V6G source archive** in a fresh folder.
2. Run `build-windows.cmd`; require **BUILD SUCCESSFUL** and all source,
   Kotlin/Gradle unit, Android test-source, APK signer and package checks.
3. If the build fails, run `send-build-report.cmd` and share only the
   privacy-filtered diagnostics. Do not install an old
   `NightWave-debug-previous.apk` as though it were the new build.
4. Before updating the existing app, run `check-device-update.cmd`
   to confirm signing identity and in-place update compatibility.
   **Never uninstall NightWave or clear saved audio/lyrics to fix signing.**
5. On a disposable emulator, run the LRC search UI tests, verify searching,
   match navigation, no-results state, multi-language text and playback
   independence; then perform the previous A–F regression matrix.
6. Account owner may separately check Actions billing, usage limits and
   repository Actions runner restrictions. A no-runner CI failure does
   **not** count as a Kotlin compile result.

## Limitations

This is a concrete test compilation correction plus regression tooling,
not a Spotify catalog, new music source, LRC transcription system, APK
release or a finished visual redesign. Full Android compilation remains
**unverified** until the Gradle tasks actually execute successfully.
