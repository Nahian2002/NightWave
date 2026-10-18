# NightWave Phase 6V6F — Build stabilization and integrated lyrics QA

## Scope

This development-only branch is based directly on 6V6E. It adds a factual
GitHub Actions infrastructure diagnosis, an optional **real Kotlin/JVM
command-line compiler smoke check**, read-only source/build integration
checks, and a narrow safeguard for lyric export when the active song or
editor session changes. The local library, MP3 files, saved lyric formats,
Google Drive data, package identity and previous signature gates are not
migrated or overwritten by this phase.

**No one has installed anything on the user's phone. No APK is released or
certified by this branch.**

## Investigation: CI runner not assigned

- On **2026-10-18**, draft PR **#73** targeting the 6V6D branch triggered
  GitHub Actions run **39146843740**. The original attempt and a retry each
  completed as failure in approximately three seconds, with
  `runner_id=0` and **zero job steps**. Neither attempt reached checkout,
  Kotlin, Android SDK, Gradle or JUnit.
- Sampling earlier NightWave run IDs `39113201206`, `39112116737`,
  `39109868494`, and `39100510248` found the same unassigned
  runner/zero-step pattern, before the latest lyrics changes.
- The repository's Actions list showed **no successful recent runs** at
  inspection. This points to a recurring **GitHub Actions execution blocker**,
  not proof of a specific source compilation error.
- There is not enough evidence to identify a single underlying root cause.
  Potential causes to check with the repo owner are Actions usage/billing
  limits, repository Actions restrictions, account policies, or runner
  availability. None is asserted as confirmed.
- Changing `runs-on: ubuntu-latest`, Kotlin files, build tools, or APK
  versions based only on these startup failures would be unjustified.

### Human check in GitHub

1. Open [PR #73](https://github.com/Nahian2002/NightWave/pull/73),
   choose **Checks**, and open the failed **NightWave Android verification**.
   Check for banners that explain why jobs were blocked.
2. In the repository's **Settings > Actions > General**, check whether
   Actions and appropriate runners are allowed for private repositories.
3. In the GitHub account's **Billing / usage and spending** settings,
   check whether Actions usage limits or billing restrictions are present.
   Only the account owner can resolve billing or policy restrictions.
4. After resolving runner availability, request a new workflow run.
   A successful runner allocation should show actual named steps, including
   checkout, Gradle, Kotlin compile, unit tests and APK checks.
5. Do **not** label a workflow green, merge PRs or install an APK if a job
   never started. A CI `failure` with zero steps is **not** a Kotlin failure.

### Optional offline JSON classifier

`tools/triage_github_actions.py` accepts a JSON file containing the
`jobs` array returned by GitHub's Actions jobs API. It uses no credentials,
internet connection or personal data, and reports one of:
`RUNNER_UNASSIGNED` (with `compilation=NOT_ATTEMPTED`),
`RUNNER_STARTED` (inspect actual failed step),
`PENDING_OR_UNKNOWN`, `NO_JOBS` or `INVALID`.

When the authorized `gh` CLI is already installed and authenticated, one
can export a job status locally:

```powershell
gh api repos/Nahian2002/NightWave/actions/runs/39146843740/jobs > jobs.json
python tools/triage_github_actions.py --jobs-json jobs.json
```

Do not share raw logs, tokens, personal song paths, or login information.
Run `python tools/triage_github_actions.py --self-test` to exercise
synthetic zero-step and real-step scenarios without any account access.

## Optional pure Kotlin/JVM checks without an Android SDK

The new `tools/smoke-lyrics-kotlin.ps1` compiles the seven **pure Kotlin**
lyrics sources in a temporary directory using the real local `kotlinc`
command-line compiler, then executes a small LRC policy harness in Java.
It checks canonical parsing, delayed sync, corrected timestamps, text editing,
search, match navigation, strict output, export and review warnings.
The script does **not** import Android or touch the user's actual LRC files,
audio, account, cloud data, APKs or package identities.

On Windows, only if Kotlin and Java are already installed:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/smoke-lyrics-kotlin.ps1 -RequireCompiler
```

Without Kotlin on PATH, a normal optional run reports `SKIP`; with
`-RequireCompiler`, missing Kotlin fails loudly rather than claiming a pass.
This is an additional check and **cannot replace** the Android Gradle build.

## Narrow export safeguard

Previously the SAF result callback checked the selected song/editor session
before dispatching to an IO coroutine. This phase performs the checks again
on the actual write path, before `openOutputStream`. This narrows a
time-of-check/time-of-use window for a changed song or superseded editor.
External document providers cannot offer a global atomicity guarantee, and
an already-started write is not a transactional rollback. A provider error
still warns of a potentially incomplete output document. The original
saved NightWave LRC and import files are untouched by export.

## Complete build remains mandatory

The existing `build-windows.cmd` is the supported fallback for GitHub-hosted
runner issues. It uses official upstream JDK/Gradle/Android SDK tooling,
runs earlier source gates plus the new 6V6F Windows audit, executes the
**real Gradle tasks**:

```text
:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

and then checks APK identity and signing continuity before promoting
`dist\NightWave-debug.apk`. A failed build preserves a previously built
APK separately and must not be mistaken for a fresh release.

The project source ZIP must be extracted into a fresh short Windows folder.
Run `build-windows.cmd`. Only if the final status explicitly says SUCCESS,
use `check-device-update.cmd` to verify the signature against the installed
NightWave. **Never uninstall, clear app data, or replace existing offline
audio to bypass signer differences.**

On failure, run `send-build-report.cmd` and share the privacy-filtered
diagnostic so an actual Kotlin, Compose, JUnit or SDK issue can be fixed.
Do not send raw credential-bearing logs or downloaded audio files.

## Phase 6V6F verification state

- **Reviewed source**: new CI startup classifier, optional pure Kotlin
  smoke runner, cumulative Python and PowerShell verification gates, Windows
  build/CI hooks, and the export callback's second session check.
- **Not proven**: completed `kotlinc` smoke on the user's machine, full
  Gradle test success, Kotlin/Compose compilation, instrumentation runtime,
  Google Drive access, SAF device export or in-place APK update.
- CI remains blocked until a GitHub-hosted runner is actually allocated.
- No new features such as automatic lyric transcription, online lyric
  scraping, MP3 ripping, or licensed Spotify catalog were added.
