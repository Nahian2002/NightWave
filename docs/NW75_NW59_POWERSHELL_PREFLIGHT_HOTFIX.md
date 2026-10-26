# NW75 Windows build hotfix 3 — NW59 PowerShell HOME and paging preflight

**Based on:** `fix/nightwave-nw75-nw34-offline-gate` (includes Phase 6P + NW34 fixes)  
**Newest cumulative branch:** `fix/nightwave-nw75-nw59-home-ps-preflight`  
**Release status:** NO-GO; full Windows Android Gradle build, JUnit, APK signature verification, and Samsung tests still pending.

## Evidence from the user's 2026-10-26 Windows build

The downloaded Hotfix 2 passed Phase 6P, the NW07 checks, NW34 (20/20), NW40, NW52, and the NW59 synthetic invariant fixture. The main NW59 source preflight then terminated with:

```
Cannot overwrite variable HOME because it is read-only or constant.
At tools/check-nw59-integration.ps1
CategoryInfo: WriteError: (HOME:String) [...], SessionStateUnauthorizedAccessException
FullyQualifiedErrorId: VariableNotWritable
```

The builder reported `NW59 cumulative upgrade regression preflight`. **Gradle never started; no verified new APK was produced.** Existing `NightWave-debug-previous.apk` is not a new NW75 build.

## Root cause, exact repair, preserved behavior

PowerShell variable names are case-insensitive. The script's `Assert-NW59Home` function assigned a local string to `$home`, which collides with PowerShell's automatic, read-only `$HOME`. The Windows PowerShell interpreter rejected it before its actual Home-screen assertions could run. The helper now uses `$homeSource` throughout.

The script's `-SelfTest` previously checked only generic string presence, absence and ordering, not the full `Assert-NW59Home` function. NW75 Hotfix 3 now includes positive and deliberately broken Home-screen fixtures that execute the function, making this exact mistake fail early during future preflight self-tests.

The source audit also found a pre-existing stale substring check: `DownloadHistoryPagePolicy.offset(historyPage, filteredJobs.size)` appeared in NW59 as one contiguous line, but actual NW63 source correctly wraps this call over two lines. The assertion now checks the method call and its filtered count arguments separately, without changing pager behavior.

**No Kotlin Android production code, library content, playback source, downloaded audio, Google scope, APK signing or data migration changed.** Only the PowerShell source checker and documentation were updated.

## What was verified in the assistant session

- Read back the new committed PowerShell checker from GitHub.
- Inspected **238 direct source Has/Missing assertions** covering **66 source bindings** in 5 batches. The only observed mismatch was the multiline paging source contract, repaired in this hotfix. Additional dynamic helper checks were examined against the actual Home, Search, Player and Google UI source; 11/11 targeted assertions passed source matching.
- Verified that no runtime assignment to `$home` remains in the updated NW59 script.
- Reviewed the three download-worker source contracts in the NW59 gate; they remain unchanged.
- **No Windows PowerShell execution in the assistant environment, Gradle compile, current APK production, or Samsung verification was performed.** Source text parity cannot guarantee that no future compile/runtime failure exists.

## Build procedure

1. Download the **complete new hotfix branch ZIP**, extract to a NEW Windows folder (do not reuse old `dist` or APKs).
2. Run `build-windows.cmd` once. After NW59 source gate passes, it should proceed to fingerprinting, Java/Gradle/Android SDK setup and real Gradle compilation.
3. If Gradle or another stage fails, run `send-build-report.cmd` and share the fresh `dist/NightWave-build-status.txt`, `dist/NightWave-build-diagnostic.txt`, `dist/NightWave-build-errors.txt` and, if Gradle starts, `dist/NightWave-build-full.log`.
4. Only install after a completely successful Android build plus `check-device-update.cmd` and `check-google-oauth.cmd`; do **not** uninstall/clear NightWave or remove existing offline music.

**Public release NO-GO pending real compile and end-to-end acceptance.**
