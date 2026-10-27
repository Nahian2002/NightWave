# NW118 — repository hygiene / safe cleanup audit

**Baseline:** cumulative NW117 commit `6c78f25b416f8b2723c8d13267f397e46880475e`, 27 October 2026. **Result:** targeted organization and Library selection repair; **not** a complete all-files semantic/compiler/device audit.

## Inventory, not conjecture

- GitHub recursive tree at exact baseline: **1,130 tree entries, 1,047 tracked files and 83 directories**, with `truncated=false`.
- `app/src`: **499** tracked source/resources/test files; `docs/`: **242** documents; `tools/`: many per-phase regressions and build scripts. `.github/workflows` contains two checked-in workflows.
- No tracked **APK, AAB, ZIP, LOG, generated `build/` folders, `.gradle/` caches, IDE folders, common backup extensions, or committed signing-keystore filenames** matched the inventory scan. This is filename/tree analysis, **not a scan of all file bodies or Git history for credentials**.
- Git tree blob SHA grouping found **zero byte-identical tracked-file groups**. Similar names or old test numbers are not evidence a file is unwanted.
- **Deleted files: 0.** No safely proven unwanted tracked files were identified. Removing old verification scripts, migration/schema/evidence documents or Windows launchers simply to reduce file count would risk regressions and weaken forensic receipts. User-authorized cleanup is conservative.

## Organization changes

- `README.md`: replace stale NW110 "current source" branch pointer with the cumulative NW118 branch and current roadmap/index. Maintain original historic milestone notes rather than rewriting prior evidence.
- `docs/REPOSITORY_INDEX.md`: one clear starting point for current source, legacy plans, build rules, security/privacy and report interpretation.
- `tools/README.md`: explain which Windows entrypoints are current, where historical smoke scripts live, and what each evidence tier can actually prove.
- `.gitignore`: retain existing Android build/signing protections; additionally ignore future accidental source archives, temporary/editor backups, crashes, dumps, Python caches and local reports. This affects future untracked files only, not repository history.

## NW118 Library selection defect and preservation

`LibraryBulkSelectionPolicy.toggleVisible()` intersected stale selected IDs with the current playlist, but could carry an already-restored **more-than-20** selected set, even though the existing atomic playlist action is bounded to **20**. `toggle()` could also leave stale/removed IDs occupying the limit.

- New read-only `sanitize(current,source)` prunes vanished identities and bounds to 20 without editing music or playlists; `toggle()` and `toggleVisible()` use it.
- In `StudioLibrary`, the three individual toggle call sites previously passed the current **filtered/sorted view** as source. Sanitization requires the **complete real playlist** so temporarily hidden selections remain selected: all three now use `tracks`. The `LaunchedEffect` restoration check also applies sanitize against `tracks`.
- Selection stays ephemeral; no persistent migration or playlist writes. Existing selected-list ordering, batch limits and undo flow retained.
- Added **8 JUnit regression declarations** to `LibraryBulkSelectionPolicyTest` (7 existing + 8 new = **15 test methods**), covering oversized restoration, stale IDs, hidden filtered selection preservation, atomic limit, and no source mutation.
- These JUnit declarations have **NOT been run through Gradle** on this commit. Static source checks can confirm wiring but do not prove Android compilation. No release readiness claim.

## Deletion rules for later full audit

Only delete after checking **source references, Gradle/CI and Windows launchers, user-owned data/migrations, release receipts, and reachable link targets**. Deleted paths must be listed in the PR with replacement and revert instructions. Prefer retaining historical evidence and marking it ARCHIVED in the docs index. Do not run blind `rm -rf` or delete every file containing "old", "phase", "legacy" or "smoke".

## Unresolved gates

- A fully executed **clean Windows Android Gradle/JUnit** build, Compose compilation, Android instrumentation, process/media session tests, Samsung 5k library stress, multi-select/undo/migration, and production OAuth/Drive backup/restore are still pending.
- SHA-specific signing continuity, private user data protection, Google Play/platform compliance and licensed music source rights are independent **release NO-GO** gates.
- Do not claim repository-wide bug-free status, APK success, all files fixed, or all tasks completed from this limited audit.
