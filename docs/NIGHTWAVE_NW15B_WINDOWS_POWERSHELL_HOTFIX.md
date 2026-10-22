# NightWave NW-15B — Windows PowerShell 5.1 Build Syntax Hotfix

**Date:** 22 October 2026
**Parent:** NW-15 `d102c9dc2eacbb091f245f54292bd5f10084373f`
**Branch:** `fix/nightwave-v2-nw15b-windows-powershell-hotfix`
**Release status:** Development source hotfix. **APK is not verified.**

## User-confirmed failure

The user ran the Windows `build-windows.cmd` launcher and the local syntax gate printed:

```text
nw11-junit-report.ps1 line 55, column 41: Missing ')' in method call.
nw11-junit-report.ps1 line 55, column 42: Unexpected token '$message")'
nw11-junit-report.ps1 line 72, column 50: Unexpected token ...
BUILD CANCELLED. The PowerShell build script has a syntax error.
No Gradle tasks were run.
```

The first real failure is the diagnostic file itself, **not Kotlin, Gradle, account login, streaming, or the installed app**. Later errors are parser cascades caused by the earlier unbalanced quote.

## Likely root cause

The reporter source was delivered as UTF-8 without a byte-order mark. **Windows PowerShell 5.1** may interpret such files in the active Windows ANSI code page instead of UTF-8. The string at line 55 contained a typographic em dash (`U+2014`) in:

```powershell
$found.Add("$className.$name [em dash] $message")
```

Under Windows-1252, the UTF-8 em dash bytes `E2 80 94` can decode as a sequence ending in a typographic double-quote character. PowerShell treats smart quotes as quotation marks. Thus a harmless separator can prematurely end the string and produce the exact surrounding syntax errors shown in the screenshot. An ellipsis (`U+2026`) was also present in the same reporter and an en dash (`U+2013`) in another source gate.

This is a strongly supported encoding explanation from the reported failure and source bytes, but **the exact Windows code page was not measured from the user's PC**.

## NW-15B changes

1. `tools/nw11-junit-report.ps1` now uses ASCII `...` and a safe ASCII `-f` expression for failing-test formatting:
   `$found.Add(('{0}.{1} - {2}' -f $className, $name, $message))`.
2. `tools/check-nw07-source-gate.ps1` uses the ASCII label `NW01-NW06` to remove the older en dash.
3. `tools/check-builder-syntax.ps1` now examines each of the **26 registered PowerShell files**, including itself, before parsing. Any byte outside ASCII (`0x00..0x7F`) fails closed with a precise filename. An ASCII-only convention is intentionally enforced for the entire Windows source-script collection.
4. Added `tools/verify_nw15b_windows_encoding.py` to check source bytes, the complete registered-script set, the reporter's fixed string, integration with `build-windows.cmd`, and presence of required Gradle tasks.
5. GitHub Android CI now includes a source/encoding verifier plus `pwsh` syntax parsing and `nw11-junit-report.ps1 -SelfTest`, **before** the Gradle stage. The previous regression, APK signer, fingerprint and data-preservation gates remain unchanged.
6. No Android/Kotlin app files, signing configuration, package identity, offline music, profile or backup data are changed by this hotfix.

## Verification and important limits

- **Source checks:** The connected GitHub branch was inspected for unsafe script characters and verified to include the strict byte-level guard.
- **Windows PowerShell execution:** No Windows PowerShell 5.1 executable is available to this assistant's environment. The actual user's next run must establish whether the syntax and reporter self-test pass.
- **CI:** Earlier GitHub-hosted workflows failed with zero executed steps. A newly registered CI command does not count as passing until it really executes.
- **Gradle/APK:** The user's screenshot explicitly says that no Gradle tasks ran. The hotfix repairs this *preflight blocker* only; it does not establish successful Kotlin compilation, JVM tests, artifact generation or signing continuity.

## Rebuild on Windows — safe instructions

1. Download the **exact NW-15B ZIP** from the final GitHub commit linked in chat.
2. Extract into a **new empty Windows folder**, not on top of an older NW-15 extraction. Mixing old PowerShell scripts can reintroduce the same fault.
3. Double-click `build-windows.cmd` in the new folder. The first gate should print `Checking PowerShell build script syntax...` followed by a PASS message, not line 55/72 parser errors.
4. The remainder of the build must still complete all `:app:testDebugUnitTest`, `:app:assembleDebug` and `:app:compileDebugAndroidTestKotlin` tasks. If a new problem appears, the build should fail with a truthful report.
5. If there is another failure, send the screenshot and the current `dist/NightWave-build-status.txt`, `dist/NightWave-build-errors.txt`, and sanitized full log if created. Do not include private Drive tokens or unredacted account identifiers.
6. **Do not uninstall the existing NightWave app or clear its data.** Run the read-only signer/device compatibility check only after a verified APK exists.

**Disposition:** The concrete syntax/encoding defect is patched in GitHub source. Real Windows build and device verification remain pending.
