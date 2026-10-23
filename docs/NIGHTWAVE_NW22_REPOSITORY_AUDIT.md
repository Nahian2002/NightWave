# NightWave NW22 repository audit — evidence, findings and release gate

Date: 23 October 2026. Target: `fix/nightwave-v2-nw22-build-stability` (commit `6390e634a3dc4889d868c2417a8efbadf4f78c3c`). New audit branch: `audit/nightwave-v2-nw22-repository-hardening`.

## Actual repository inventory

GitHub recursive tree enumerated **709 entries**, with **298 Kotlin files, 120 Python files, 26 PowerShell scripts, 27 XML files, 141 Markdown files, 5 CMD scripts, 3 Gradle KTS files and 2 YAML files**. One app source is especially large: `NightWaveStudioActivity.kt` (~205 KB), increasing the risk of UI integration regressions.

## Findings (verified source/tool evidence)

1. **Critical release blocker — Android build has never been certified for NW22.** GitHub Actions fails before any job steps. Job inspection of `39855404720` returned a failed job with null steps. This is **not evidence of a Kotlin failure or success**. Exact infrastructure cause unconfirmed; repository owner must inspect Actions UI/banner and Actions settings. The Windows build is the path to a real compiler verdict.
2. **High — test coverage is materially incomplete.** A 24/24 *pure Kotlin* production policy smoke test passes, but does not compile Android UI/service/Google integration, execute Android JUnit/instrumentation or install APK. Numerous historical tools are source marker checks only. They must not be counted as full green app tests.
3. **High — update data-loss risk if APK signer differs.** NW22 can optionally enforce the known installed-app signing certificate SHA-256. Without known trusted signer or prior compatible APK, status must be explicitly **NOT CHECKED**, not installed-safe. Never uninstall or clear app data to work around an update error.
4. **High — download scope discrepancy.** Current search-to-download uses a separate Commons CC0/public-domain catalog; it does not download arbitrary YouTube songs. No code audit should treat the user goal as fulfilled.
5. **Medium — limited library capacity.** `OfflineAudioLibrary` sets 400-track, 3 GiB library and 250 MiB/file limits. Library scaling and free disk accounting remain deferred, though download deduplication checks the existing hash **before** the 400-track cap.
6. **Medium — download lifecycle and integration tests absent.** Real network redirect and midstream cancel, device rotation, repeated buttons, process termination, airplane mode, split-screen/font scaling and recovery of previous queues require instrumented tests. Source-only inspection cannot establish reliability.
7. **Medium — source base contains ~205 KB single Activity.** The extensive combined Compose Activity merits incremental separation, smaller screens/viewmodels and compile-tested regression coverage, not a blind all-at-once rewrite.

## Scope covered in this audit conversation

- Enumerated entire GitHub tree and languages.
- Read key NW22 build scripts, signature checker, Gradle config, full GitHub Actions recipe, standalone Kotlin smoke script, source gate, Commons catalog parser and UI, audio download policy, staging/commit library, and important portions of Android service/UI contracts.
- Examined prior actual run and latest GitHub job state: no compiler steps executed.
- Added `tools/audit_repo_nw22.py`, a **full local extracted-source scan** for Python grammar, XML well-formedness, UTF-8/NUL, exact Kotlin duplicate imports, merge conflict markers, basic private-key marker detection, and a bounded extension/line inventory. Wires its self-test and audit to GitHub Actions.
- **Limit:** The 709-file repository could not be cloned into this execution environment (GitHub hostname unavailable in local shell); the full static scan cannot be reported as executed or passed here. Its intended execution is on a checked-out/extracted source tree or an Actions runner that actually starts.

## How to run the full source static scan

With Python 3 installed and the full ZIP extracted:

```powershell
python tools/audit_repo_nw22.py --self-test
python tools/audit_repo_nw22.py --json
```

Success means only the enumerated static checks passed; it is **not** a complete line-by-line human review or Android runtime certification. Next: run `build-windows.cmd` and inspect `dist/NightWave-build-diagnostic.txt` if failing, compile all unit/instrumentation sources, verify signer continuity and finally test on Galaxy without clearing installed data.

## Remaining verification scope

Every Kotlin file still needs a **successful Android Gradle compile**; every Python and PowerShell script needs syntax/runtime validation on its host platform; resources and manifest need Android AAPT/AGP validation; downloaded dependencies need resolution; security checks need static and runtime analysis; user journeys need Android instrumentation. No truthful blanket 'zero bugs' guarantee is possible.
