# NightWave Phase 6H1 — Windows diagnostic preflight hotfix

**Draft hotfix branch**: `feature/nightwave-v2-phase6h1-diagnostic-redaction-hotfix`, based on the complete 6H build. Does **not** change Kotlin, app package/version, signing, music files, playlists, backup formats or installed device state.

## Evidence and exact failure

The Windows build status, error report and full log uploaded for the 6H source ZIP show:

- Windows script parse check **PASS**.
- Kotlin/Compose import preflight **PASS** (**121 .kt files**).
- Synthetic NightWave crash-report redaction check **PASS**.
- **Build diagnostic/redaction self-test FAILED**, before Java preparation.
- The full log says **Gradle not yet started**. No newly verified APK was published.

This is **not** evidence of an Android/Kotlin compiler failure. The preflight's synthetic query is:
`https://example.org/file?access_token=imaginary-secret&name=public`.

The original `Redact-Line` first replaced `access_token=imaginary-secret` with `access_token=<redacted>`, then applied an overly greedy generic assignment redaction `\S+` to the same `access_token` field. That second rule also consumed `&name=public`. The test explicitly checks that non-secret `name=public` survives, so the self-test failed.

## Exact changes

1. Limit the generic `client_secret`/`refresh_token`/`access_token`/`id_token` assignment value to `[^\s&#]+`, i.e., stop at whitespace, ampersand, or fragment separator. Continue redacting secrets; preserve harmless following URL fields. Use `${1}` for an unambiguous .NET replacement backreference.
2. Expand PowerShell `-SelfTest` for single and multiple URL secrets and non-secret parameter preservation, plus direct assignment format. **Do not bypass or disable the privacy check.**
3. The Windows builder now includes the synthetic failure category in `NightWave-build-errors.txt` rather than hiding it behind a generic failure, still without revealing real log credentials.
4. Update the inherited 6G source check and add a dedicated 6H1 gate. Existing CI stays fail-closed on build errors, and the old installed NightWave app is untouched.

## What was verified here

- GitHub source edits and readback, and a Python-equivalent set of three regex regressions: **PASS**.
- The real Windows PowerShell 5.1 diagnostic `-SelfTest`, complete Gradle build, Android tests, APK signing and phone playback are **not yet executed** in this environment. Those are the necessary next confirmations.

## How to retry on Windows

1. Download the **complete 6H1 source ZIP**, extract to a **new folder** instead of merging files into the old extracted 6H directory.
2. From the extracted NightWave root (with `build-windows.cmd`), optionally run the independent preflight:
   `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools\diagnose-build.ps1 -SelfTest`
   It must print `PASS: NightWave build diagnostic self-test.`
3. Run `build-windows.cmd` as before. It will automatically redo the preflights and only then prepare Java, Gradle and Android SDK dependencies.
4. If another stage fails, run `send-build-report.cmd` and review/share **`dist\NightWave-build-diagnostic.txt`**. File filtering is best effort, so remove anything you consider sensitive before sharing.
5. Never install a new APK unless actual Gradle compilation, APK identity and signer-continuity checks pass. **Do not uninstall or clear your existing NightWave library to fix any signature mismatch.**

**Expected result:** this exact synthetic query-redaction failure should be resolved; the eventual Android compilation result remains unknown until a full Windows build is run.
