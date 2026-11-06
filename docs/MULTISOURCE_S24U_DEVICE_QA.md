# NightWave multi-source S24 Ultra local build + device QA runbook

**Status:** instructions only. No S24 device was connected to *this GitHub-documentation session* and no new app was built, installed or tested. Run on the owner's **Windows laptop** with the S24 Ultra physically connected and USB debugging authorized. See [master plan](MULTISOURCE_NW56_NW58_MASTER_PLAN.md) and [acceptance matrix](quality/MULTISOURCE_NW56_NW58_ACCEPTANCE.md).

## Owner constraints and safety

- Owner wants future coding agent to automate as much **safe app-specific** build and test work as practical; the presence of a cable never proves authorization, unlock state or successful testing.
- Never run `adb uninstall`, `pm clear`, factory reset, root, disable security, re-sign app with a different identity, delete personal media, swap Google accounts, change system settings/animation/font/airplane mode without owner permission, or publish/upload personal data. Do not place screenshots, recordings, system dumps, auth, backups, APKs or unredacted logs in GitHub.
- No paid GitHub runner/CI. Do not purchase API subscriptions or public device farm access.
- The previous v55 APK has real-world S24 install evidence in `TEST_REPORT.md`; NW56–58 still require **fresh physical verification**.
- Never automatically install an unsigned/differently signed APK over existing NightWave, install test-only builds or use `-d` downgrade. If signer continuity, package identity, backup or phone authorization cannot be established, stop installation and mark BLOCKED; run all independent host tests instead.
- Owner intent permits S24 **app testing**, not wiping, changing system/Google settings or bypassing consent. Keep personal music and app data intact; prefer disposable test playlist and locally owned audio imported explicitly for QA.

## Step 1: source and Windows environment

Work from a freshly verified descendant of `nightwave-v55` rather than the default branch by assumption:

```powershell
git fetch origin --tags --prune
git rev-parse HEAD
git rev-parse nightwave-v55
git merge-base --is-ancestor nightwave-v55 HEAD
# Exit code 0 means tagged v55 is ancestor; otherwise investigate before coding.
where.exe java
where.exe python
where.exe adb
```

Record Android Studio/SDK + JDK paths and Gradle dependencies according to current `docs/BUILD_AND_RELEASE.md`. Avoid touching source before recording the baseline. Use the same package id (`com.nightwave.player`) and signing identity for update testing. Keep `tools/build-windows.ps1` and `tools/check-device-update.ps1` behavior intact.

## Step 2: device discovery — read-only

```powershell
adb start-server
adb devices -l
# SELECT the single verified S24 Ultra serial after checking it is "device", NOT offline/unauthorized.
$serial = "<OBSERVED_SERIAL>"
adb -s $serial shell getprop ro.product.model
adb -s $serial shell getprop ro.build.version.release
adb -s $serial shell getprop ro.build.version.sdk
adb -s $serial shell pm path com.nightwave.player
adb -s $serial shell dumpsys package com.nightwave.player | Select-String "versionCode|versionName"
```

Check `SM-S928*` only as a *hint* (regional variants differ). Do not invent a serial, build number or Android version. When multiple devices connected, **never** run an implicit ADB operation without `-s $serial`. If unauthorized/offline, stop device steps rather than bypassing the lock or repeatedly prompting.

## Step 3: build and provenance — laptop only

Repo-provided launchers are authoritative. If running interactively use `build-windows.cmd` and `check-device-update.cmd`. If the agent needs noninteractive execution, call the underlying scripts **after inspecting them**:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\check-builder-syntax.ps1
if ($LASTEXITCODE -ne 0) { throw "Builder syntax failed" }
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\build-windows.ps1
if ($LASTEXITCODE -ne 0) { throw "NightWave build failed" }
python .\tools\verify_current.py
if ($LASTEXITCODE -ne 0) { throw "NightWave source checks failed" }
Get-FileHash -Algorithm SHA256 .\dist\NightWave-debug.apk
```

Read `dist/NightWave-build-status.txt`, full/diagnostic/error reports, source digest, APK provenance and signing receipt. Confirm exact APK was produced from the candidate branch and is NOT a stale `dist` file. Compile all Android instrumentation sources and run lint, as indicated by the currently checked out builder; **compilation alone is not instrumentation execution**. Never use a different signing key to make the installer pass.

## Step 4: in-place-safe update preflight

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\check-device-update.ps1
if ($LASTEXITCODE -ne 0) { throw "Unsafe/inconclusive update: do NOT install" }
Get-Content .\dist\NightWave-device-update-check.txt
```

Require evidence of exact package id, installed-app vs candidate signing-certificate match, target version/versionCode direction, and a **verified backup/snapshot of user data through owner-approved existing backup workflow**, with before-state counts/hashes for the relevant library/playlist/history/guest files. **ADB does not automatically give access to app-private files; don't claim a backup unless bytes/readback are actually verified.**

If all gates pass and the owner's current app-only testing approval covers candidate in-place installation:

```powershell
adb -s $serial install -r .\dist\NightWave-debug.apk
if ($LASTEXITCODE -ne 0) { throw "Install failed" }
adb -s $serial shell dumpsys package com.nightwave.player | Select-String "versionCode|versionName"
```

If the existing package signer differs or no reliable backup is possible, **do not uninstall**. Use an isolated spare device/emulator or ask for an explicit safe alternative, while preserving all existing data. Never clear the old app to bypass signer mismatch.

## Step 5: automate safe app test flows

Prefer Android instrumentation targeting NightWave, using app-scoped UI tests with explicit assertions and cleanup. Identify the test package/runner from the actual build; then run it with `adb -s $serial shell am instrument -w <real-test-component>` (do **not** invent the component). Run targeted unit/UI cases per provider and core journeys, then the full applicable instrumentation matrix.

Automated app-only scenarios:
1. Home/Search/Library/You/Now Playing/mini-player open and return, with no missing navigation or state loss.
2. Search existing YouTube/reference path and local songs, plus each enabled Audius/PeerTube/Radio/Jamendo provider. Assert source labels, canonical IDs, correct play versus external-open action, genuine metadata and loading/zero-result states.
3. Query A immediately followed by query B; stale A must not render or start A's player. Include Android activity recreation and rapid tab switches.
4. Play/pause/skip/shuffle/repeat/queue, background notification and lock-screen controls **where the source's rights permit**.
5. Import an explicitly owned test file; verify known file path/hash, pause/resume/airplane playback. A saved online reference must not masquerade as offline audio. Do not toggle device network settings automatically without separate permission.
6. Existing liked tracks/playlists/history, theme, large text, accessibility, guest Google Drive *consent boundaries*, back/forward and backup import/export. Never sign out/change the owner's existing Google account.
7. Simulate provider timeout/429/503/malformed response via test doubles; verify no crash and truthful status. Do not stress volunteers' public services with abusive live load.
8. Restart the app/process under test with test fixture (not user data), check selected song/queue and no unrequested random tracks. Run 30-minute bounded soak and baseline-aligned measurements when possible.

If unattended automation is blocked by device OEM input restrictions, use app-owned test instrumentation and explicit manual evidence gaps. Do not install a persistent remote-control backdoor or claim full UI automation from an ADB screenshot alone.

## Step 6: evidence and cleanup

Capture app-scoped test outputs and redacted logcat **privately**. Avoid clearing system-wide logs or exporting phone media. Read current crashes carefully; prove correct version/PID and test window. Record for each scenario: source commit, local build/variant, APK hash, observed model and Android SDK, test date/time, commands, expected/actual results, screenshots/video/log paths, pass/fail/blocked. Sanitize before sharing. Confirm all owner music/playlist/history/preferences counts and hashes still match expected values.

Restore any explicitly changed in-app test theme/filter/queue state and leave the phone safely usable. No uninstall, data reset, security bypass, app-store/public APK release or update feed change. If device not connected or installer blocked, mark `DEVICE_NOT_RUN` / `BLOCKED`; host success is **not** phone success.

## Minimum deliverables per mega-batch

- **NW56:** exact build + source checks; adapter unit/negative tests; genuine one-result smoke per enabled provider (or documented licensing block); local/YouTube regression; S24 connectivity/evidence.
- **NW57:** same plus full search/filter/queue/backup/offline test matrix, accessibility and stable IDs/migration proof.
- **NW58:** full host/lint/instrumentation run, regression + provider outages, safe in-place S24 acceptance, measured soak, privacy/rights sign-off, and explicit owner release decision.

The agent must append **real evidence**, not overwrite the baseline with claims. Consult `TEST_REPORT.md`, `docs/BUILD_AND_RELEASE.md`, `tools/README.md` and the acceptance matrix on every run.
