# NightWave 6V3 — Kotlin Linkage and Build Diagnostics

Branch: feature/nightwave-v2-phase6v3-kotlin-linkage-build-diagnostics
Stacked after 6V2. No changes to the stable main branch or installed phone.

## Confirmed obstacle
The GitHub Actions jobs in the prior 6V family were never allocated a runner. Example run 39112116737 shows runner_id=0 and zero job steps, with two check annotations whose content the connected GitHub API would not return. This is not evidence of either successful or failed Kotlin compilation. The exact external failure reason remains unknown.

## New checks
- tools/check-kotlin-internal-imports.py: Python-only source audit of first-party com.nightwave.player imports against Kotlin top-level declarations across app/src/main/java, test/java, androidTest/java and debug/java. It recognizes generated Android R/BuildConfig and nested project members.
- The --self-test option creates synthetic Kotlin files in a temporary folder, verifies resolvable imports and intentionally tests one missing class. This does not access Google accounts or music data.
- Default mode is informational: it reports unresolved internal imports without blocking a real compiler, since generic functions, nested declarations or other Kotlin language features may produce false positives. The optional --strict mode is only for verified findings.
- CI invokes the self-test and audit, and a new 6V3 regression gate, ahead of the existing real Gradle tasks. The build log redaction and APK identity/artifact gates remain.
- No runtime or Android app source files were modified. Cloud backups, LRC data, offline music and playback are preserved.

## What has not happened
Full Gradle compilation, JVM unit test suite, Android instrumentation-source compilation, device signing verification, Google Drive live checks and APK installation are not complete here. Source-level diagnostics must never be described as a verified APK.

## Decisive next verification
A genuine Android Gradle run must execute on a GitHub Actions runner or through the pre-existing one-click Windows build-windows.cmd. That local builder retains the actual Gradle success marker, signer verification and redacted build diagnostics under dist without installing anything on the phone. Once a build log exists, repair concrete Kotlin/compiler errors before adding more cloud features.

For a runner_id=0 failure, the GitHub Actions run page can expose annotations not available from the connector; checking repository Actions permissions, runner access and billing settings may identify an external blocker.
