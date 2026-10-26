# NW75 build hotfix — outdated Phase 6P preflight (2026-10-26)

**Cumulative source:** `fix/nightwave-nw75-phase6p-preflight`  
**Based on:** `feature/nightwave-nw75-final-integration-gates`  
**Build status:** **PRE-GRADLE SOURCE-GATE FIX ONLY. Full Windows Gradle build and Samsung QA are still unverified. Do NOT treat the previous failed build as a fresh APK.**

## Actual user evidence

The user's Windows builder reported:
- `FAIL: 6P integration source preflight: 2 issue(s)`
- `Missing accent-aware matching`
- `Missing local saved search`
- `Stage: Phase 6P integration source preflight`
- `Gradle not yet started for this NightWave build attempt.`

The uploaded full Gradle log contains no compile tasks. The error/diagnostic/status text clearly states the Android build never started. These messages are not Kotlin compiler errors.

## Root cause and repair

NW70 correctly refactored old search internals:
- `SearchRelevancePolicy.normalize()` delegates to `SearchScriptNormalization.normalize()` rather than embedding `Normalizer.Form.NFKD`.
- The new Unicode helper uses **NFKD** for Latin accent normalization, strips only Latin combining marks and recomposes Bengali vowel marks with **NFC**.
- `SavedTrackSearch.match()` computes `SearchRelevancePolicy.savedScore(prepared, track)` once and chooses the best metadata per canonical saved URL, instead of calling `savedMatches(prepared, track)` as a separate filter.
- The Compose Search screen already invokes `SavedTrackSearch.match(query, savedTracks)`.

The older `tools/check-phase6p-integration.ps1` and CI `tools/verify_v2_phase6p.py` still searched for the removed exact implementation text. This hotfix updates BOTH to assert the actual current source contracts, including Unicode helper delegation, NFKD/NFC, best-URL saved-score matching and Search UI integration. It does not weaken the safety contracts or edit any production Kotlin code.

## Source verification and limits

- Read back the two committed hotfix scripts and the exact search sources from GitHub.
- Confirmed the original two missing tokens no longer appear as outdated preflight assertions, and replacement checks target actual present source code.
- Verified that the next `tools/check-phase6v-integration.ps1` source contracts appear in the current repository (no missing required contracts or present forbidden signatures found during source inspection).
- Source verification is **not PowerShell execution on Windows**, a passed Android Gradle compilation or a successful APK installation. Other later preflight gates and Android compiler errors may still appear on a fresh local build.
- There are no changes to production Kotlin, the library database, offline music, Google OAuth client/scopes, APK identity, or signing.

## Rebuild

1. Download the full new hotfix branch ZIP, and extract it to a NEW folder. Do not reuse old `dist` outputs.
2. Run `build-windows.cmd`. The Phase 6P preflight should advance past these two outdated checks.
3. If any other stage fails, run `send-build-report.cmd` and send the fresh `dist/NightWave-build-diagnostic.txt` plus current `dist/NightWave-build-errors.txt`. For real Kotlin errors, include `NightWave-build-full.log`.
4. Do not install an old APK, delete music, uninstall NightWave or clear application data. If build passes, run `check-google-oauth.cmd` and `check-device-update.cmd` before upgrading the app.

**Public release still NO-GO.**
