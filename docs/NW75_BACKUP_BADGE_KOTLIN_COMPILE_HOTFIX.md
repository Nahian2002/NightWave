# NW75 Android Gradle Kotlin hotfix — exhaustive backup badge

**Base:** `fix/nightwave-nw75-nw59-home-ps-preflight` (also includes cumulative NW75, Phase 6P, NW34 and NW59 Windows gate fixes)  
**Newest cumulative branch:** `fix/nightwave-nw75-backup-badge-compile`  
**Release:** NO-GO pending complete Android compilation, project JUnit, APK signer verification and Samsung testing.

## Actual Windows build evidence — October 26, 2026

Unlike the previous preflight errors, the Windows builder **passed NW59 and launched Gradle**. Gradle 8.11.1 reached `:app:compileDebugKotlin` after initial resource/manifest/KSP steps and reported **one compiler error**:

```
NightWaveStudioActivity.kt:4729:26 'when' expression must be exhaustive.
Add the 'PREVIOUS_BACKUP_NEEDS_REFRESH' branch or an 'else' branch.
```

The build stopped at Kotlin compilation; `42 actionable tasks: 42 executed`, `BUILD FAILED in 56s`. JUnit results were unavailable (`NO TEST XML`). **No fresh APK was generated or verified.** The provided logs do not show additional compiler errors beyond this one, but more may appear after it is fixed.

## Root cause and precise resolution

NW73 expanded `CloudAccountPresentation.BackupStage` from three to four cases, adding `PREVIOUS_BACKUP_NEEDS_REFRESH` when a previously verified Drive backup does not match current consent/privacy revision. The Home/You profile header still directly used a 3-branch `Text(when (CloudAccountPresentation.stage(cloud)))`; Kotlin requires exhaustive enum `when` expressions and rejected the missing case.

- `CloudAccountPresentation.kt`: adds `badge(stage: BackupStage): String` with an exhaustive map for all four states:
  - `LOCAL_ONLY` → `LOCAL`
  - `CONNECTED_NOT_BACKED_UP` → `CONNECTED`
  - `PREVIOUS_BACKUP_NEEDS_REFRESH` → `REFRESH NEEDED`
  - `VERIFIED_BACKUP` → `BACKED UP`
- `NightWaveStudioActivity.kt`: calls the central `CloudAccountPresentation.badge(CloudAccountPresentation.stage(cloud))` instead of duplicating and omitting part of the enum. This keeps the old card style intact and **never labels a stale backup verified**.
- `CloudAccountPresentationTest.kt`: adds regression assertions for all four distinct status badges and explicit stale-versus-verified differentiation.
- `tools/check-nw59-integration.ps1`: requires the exhaustive badge mapping, requires the UI to use it, and rejects the old 3-state inline `when`.

No Android package name, signer, OAuth scopes, stored music, Drive snapshot format, download worker, player behavior or private preferences were changed.

## Verification and next step

- Exact GitHub source readback and static checks verify the missing enum case is handled. This is a **targeted source fix**, not a certified successful Android or full JUnit build.
- Extract the new cumulative ZIP to a new Windows folder; run `build-windows.cmd`.
- If Gradle still fails, send the new `dist/NightWave-build-errors.txt`, `dist/NightWave-build-diagnostic.txt`, `dist/NightWave-build-status.txt`, and `dist/NightWave-build-full.log`.
- Only install a **fresh verified APK** after `check-device-update.cmd` confirms compatible signing. Never uninstall NightWave or clear app data to bypass a build/OAuth error.

**Public release stays NO-GO until build and real-device acceptance both pass.**
