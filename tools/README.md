# NightWave tools — current use and historical checks

**Current source baseline:** NW118 cumulative branch. These scripts have different evidence strength; do not treat an old smoke PASS as a current Android build.

## Recommended entrypoints

- **Windows build/diagnostics:** root `build-windows.cmd`, `diagnose-nightwave-build.cmd`; corresponding `tools/build-windows.ps1`, `tools/diagnose-build.ps1`.
- **Release preflight:** root `check-public-release.cmd`, `tools/public_release_preflight.py`, and source-rights/privacy checks. They only prove the gates they actually execute.
- **Device/signing:** root `check-device-update.cmd`, `capture-nightwave-crash.cmd`, `check-google-oauth.cmd`, and `tools/verify-apk-signature.ps1`.
- **Android CI:** `.github/workflows/android.yml` and any other workflows in that directory; review their trigger/status on the exact current SHA.

## Historical smoke suites

`tools/verify_nw*.py`, `tools/verify_v2_phase*.py`, `tools/NW*Smoke.kt`, `tools/kotlin/`, and the `tools/check-phase*.ps1` family preserve earlier compiler/source regression evidence. They are not obsolete merely because their phase number is old: later scripts, release checks, and test matrices may depend on them. Keep until references and exact replacements are proven.

## Expected evidence levels

1. **Source inspection:** no claim of compilation.
2. **Pure Kotlin/CLI smoke:** validates only named policies and executed fixtures.
3. **JVM unit suite:** record Gradle task, exact SHA, executed test count and failures.
4. **Android/Compose compilation and instrumentation:** provide current Android SDK, APK and device receipts.
5. **Release certification:** also requires signer, data/Drive restore, device matrix, privacy/security and legal source rights.

The authoritative roadmap is [NW111–NW125](../docs/NW111_NW125_SPOTIFY_QUALITY_ROADMAP.md). See [repository inventory and retention rules](../docs/NW118_REPOSITORY_HYGIENE_AUDIT.md). Keep reports and local APKs outside version control; do not commit credentials.
