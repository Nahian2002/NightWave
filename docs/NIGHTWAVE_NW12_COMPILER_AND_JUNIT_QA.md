# NightWave NW-12 — Executed Kotlin Compiler Proof & JUnit Parser Repair

**Date:** 22 October 2026  
**Base:** NW-11 `b4accfc65e1333185e1411e5ddba8d5d7a7e53de`  
**Branch:** `fix/nightwave-v2-nw12-test-failure-parser`  
**Release decision:** **NO-GO** (full Android Gradle build, APK signer continuity and device acceptance not demonstrated).

## Work performed

### 1. Fixed a concrete JVM failure-reporting defect

The NW-11 PowerShell reporter called `XmlReader.ReadOuterXml()` inside a
loop whose next iteration called `Read()`. Because `ReadOuterXml` advances
the reader, the subsequent `Read` can skip the next sibling test case.
A failed Windows JVM suite could therefore have **missing failure entries**
in `NightWave-jvm-failure-summary.txt`, even though Gradle itself remained
correctly marked failed.

NW-12 replaces that parser with a bounded, safe XML document loaded through
an `XmlReader` configured with prohibited DTDs, disabled external resolution,
and an 8 MiB input cap. A query for `//testcase[failure or error]` preserves
adjacent failures; error messages are still bounded and privacy-reduced.

The synthetic reporter self-test now requires **four failing cases**, including
two consecutive failures, a passing case, an `error` node, and a missing
`message` attribute. The NW-11 source verifier was updated accordingly.
The actual PowerShell script still requires Windows execution verification.

### 2. Ran a real Kotlin compiler and behavioral checks

In the current execution environment, six *actual* production files were
retrieved using the connected GitHub repository and copied into a temporary
compile workspace. Each copy's **Git blob SHA-1 matched the repository blob**.
This is stricter than merely paraphrasing files or testing rewritten policies.

Compiler: `kotlinc-jvm 1.9.0`, running on OpenJDK 21.  
Command: `kotlinc *.kt -include-runtime -d smoke.jar && java -jar smoke.jar`.  
Observed output: `PASS NW-12 hash-matched source compilation and 28 Kotlin behavior assertions`.

| Production file | Git blob SHA-1 |
| --- | --- |
| `ShuffleModePolicy.kt` | `02a44ca0cbc13efc1b451473bde09d6358159672` |
| `PlaybackSeekIdentityPolicy.kt` | `2915c16bc8da76eb8d1ba1d0b4ad097048a30297` |
| `CloudAuthorizationRecoveryPolicy.kt` | `aba73546782875719a3a9d7254cfedaed67dcebb` |
| `NightWaveSeekGesturePolicy.kt` | `7663db71c6b24ced7fa9990359ac913b87cd41f7` |
| `NightWaveArtworkPolicy.kt` | `d098e59c207ef8b292b2c37b90f7ed887b9b9323` |
| `SoundStudioPolicy.kt` | `f1b56e571dc41912563ed58d6567594ad08f1c62` |

The checks covered shuffle intent, song-safe seeking, restored Drive consent
actions and stale requests, seek clamping, thumbnail URL validation and
device-supported equalizer gain limits. All **28/28 assertions passed**.
A reproduction harness was committed at `tools/kotlin/NW12VerifiedSmoke.kt`
and can run against the exact source checkout using
`python3 tools/nw12_verified_kotlin.py` when `kotlinc` and `java` exist.
If either executable is missing the helper exits with `UNAVAILABLE`, never
reporting a false PASS.

**Important limitations:** The local compiler test includes only six
Android-independent Kotlin files. It does **not** compile Jetpack Compose,
Media3, Android Activity/Service code, all Gradle tests or the APK. Java 21
in this local environment is not the same as NightWave's Android build JDK 17.

### 3. Repaired the source fingerprint's nested tools gap

NW-08 source enumeration included all `app/src` files, but only source
scripts directly under `tools/`. It **missed** `tools/kotlin/` and the NW-10
standalone smoke file. NW-12 now enumerates nested `.kt` files and build
scripts under `tools/`, so additions and modifications of this compile
harness also affect the Windows source fingerprint. The equivalent Python
inventory verifier now tests a temporary nested Kotlin file addition.

### 4. Kept the full Android gate mandatory

The NW-12 branch is registered in `.github/workflows/android.yml`.
The NW-12 source and JUnit fixture verification runs as a preflight.
When a standalone compiler exists on GitHub's runner, NW-12 additionally
executes its Kotlin policy test. If the standalone compiler is unavailable,
CI must explicitly say **SKIP**; the full Android Gradle tasks remain mandatory:
`:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`.

## Evidence ledger

| Verification | Result | Evidence boundary |
| --- | --- | --- |
| Six production Kotlin blobs | **PASS** | Six Git blob IDs matched the connected repository |
| Compiled Kotlin policy JAR | **PASS** | `kotlinc` exit code 0 and `java -jar` completed |
| Behavioral assertions | **28/28 PASS** | Local pure-Kotlin checks, not Android |
| Independent JUnit XML fixture | **PASS** | Four failures retained, including message-less failure |
| JUnit PowerShell execution | **UNVERIFIED** | No `pwsh` on local runner |
| Full Gradle + Kotlin/Compose/Media3 | **UNVERIFIED** | GitHub-hosted workflow jobs previously executed zero steps |
| APK signature/data-preserving update | **UNVERIFIED** | No full passing new APK |
| Samsung UI/audio/Google | **UNVERIFIED** | Phone testing deferred by user |

### What to do at the eventual combined test

Use the exact NW-12 source ZIP in a clean Windows folder and run
`build-windows.cmd`. If Gradle fails, read the current
`dist/NightWave-build-status.txt`, `NightWave-build-errors.txt`,
`NightWave-build-full.log` and optional
`NightWave-jvm-failure-summary.txt`. If the APK succeeds, confirm the
installed-versus-candidate signer read-only before attempting an update.
**Never uninstall NightWave or clear its app-private data to bypass signing.**

**Next useful milestone:** a real complete Gradle build and signer-compatible
candidate. Do not call the project production-ready based solely on the
28 pure-Kotlin checks.
