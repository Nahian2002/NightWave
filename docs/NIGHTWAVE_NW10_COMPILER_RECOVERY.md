# NightWave NW-10 — Compiler Recovery and Runner Diagnosis

**Date:** 22 October 2026  
**Starting point:** NW-09 `d7632f3476683e647b9239aabc6fbc1ca72d8686`  
**Branch:** `fix/nightwave-v2-nw10-compiler-recovery`  
**Release decision:** **NO-GO** for new installation, because there is still no complete Android build or signer verification.

## Purpose

Stop inferring Kotlin/Gradle results from GitHub Actions runs that fail before allocating a working job. Generate independent, falsifiable Kotlin compiler evidence for Android-free playback policies, and identify where CI actually stops.

## Actual evidence collected

1. **GitHub Actions:** NW-09 and earlier runs regularly reported `failure`, but the job API reported **zero executed steps**. No Gradle/Kotlin compiler log exists for those runs. The cause has not been confirmed: possible account billing/minutes, GitHub-hosted runner allocation or Actions permissions must be inspected by the repository owner.
2. **Minimal runner canary:** Added `.github/workflows/nightwave-runner-canary.yml` with one `echo` step, no checkout, Android, dependencies, secrets or user data. Run `39771087799` ended **failure**, with **zero executed steps**. This independently demonstrates that a trivial task cannot currently start on the hosted runner. [Runner canary](https://github.com/Nahian2002/NightWave/actions/runs/39771087799).
3. **NW-10 main Android CI:** Run `39771087753` also ended **failure** with zero steps. Therefore, **no Gradle compile or JVM tests ran in that workflow**. [Android workflow](https://github.com/Nahian2002/NightWave/actions/runs/39771087753).
4. **Independent offline Kotlin test:** I reproduced four small production-policy files locally, checked their **Git blob SHA-1** against the actual connected GitHub source, compiled them with an installed Kotlin compiler into a runnable JAR, and executed **10 assertions successfully**. Source identities:
   - `ShuffleModePolicy.kt`: `02a44ca0cbc13efc1b451473bde09d6358159672`
   - `PlaybackSeekIdentityPolicy.kt`: `2915c16bc8da76eb8d1ba1d0b4ad097048a30297`
   - `CloudAuthorizationRecoveryPolicy.kt`: `aba73546782875719a3a9d7254cfedaed67dcebb`
   - `NightWaveSeekGesturePolicy.kt`: `7663db71c6b24ced7fa9990359ac913b87cd41f7`
   - Output: `PASS: Kotlin compiler and 10 smoke assertions on mirrored policy files`.
   - This is **real, narrow Kotlin compilation**, not an Android build and not proof that all source files compile.

## New committed developer tooling

- `tools/kotlin/NW10PureSmoke.kt`: A larger, self-contained **20-assertion** runtime test designed to compile actual Android-independent source files from the repository, including public/offline queue identities, Shuffle, Repeat, audio EQ bounds, artwork, seeking and Drive consent state.
- `tools/nw10_kotlin_smoke.py`: Uses the installed `kotlinc` and Java, compiles those exact repository source paths into a throwaway JAR and runs it. It **fails**, rather than faking a pass, when the Kotlin compiler is missing or a test fails. Supports `--list` for source inventory. **The new 20-assertion harness has not yet been executed end-to-end**, as the full source tree is not locally present in this session.
- `.github/workflows/nightwave-runner-canary.yml`: One-step runner allocation proof. No secrets, source checkout or APK.
- `.github/workflows/android.yml`: Added NW-10 branch trigger and optional standalone compiler smoke after Java setup. When standalone `kotlinc` is missing on a hosted runner it explicitly says **SKIP**, not PASS; full Gradle Kotlin compilation remains **mandatory**. Debug artifact label now identifies NW-10 instead of the stale NW-04 identifier.

## Priority to achieve a real APK

1. Review **GitHub repository → Settings → Actions** and **GitHub account billing/Actions minutes**; check for disabled runners, spending limits or platform-side restrictions. These are diagnostic possibilities, not a confirmed root cause. The canary must record **one executed step** before hosted compilation evidence can be trusted.
2. Alternatively use the existing **Windows** `build-windows.cmd` from a fresh exact NW-10 ZIP. This does not depend on GitHub-hosted runners, and it preserves the read-only signing check, full source inventory, complete Gradle JVM suite and safe APK publication rules.
3. Require `BUILD SUCCESSFUL`, all three tasks `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`, APK SHA-256, signer compatibility and the source fingerprint. Do **not** infer a pass from the narrow Kotlin policy smoke.
4. Only after the signer-safe candidate exists, perform combined Android testing of background streaming, offline queue, Google Drive authorization, actual audio output EQ, lyrics, search and Now Playing UI. **Never uninstall or clear existing NightWave data to bypass signing.**

## What's still blocked

No full Kotlin/Compose/Media3 Android compilation for NW-01–NW-10 is available; real Google consent, audible EQ, Android crashes, protected data migration and an installable compatible APK remain **unverified**.

**NW-10 is a CI diagnosis and compiler-test tooling phase, not a claim of production readiness.**
