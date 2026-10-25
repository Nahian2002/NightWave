# NW52 — Final Integration and Release Gates

**Source integration branch:** `feature/nightwave-nw52-final-integration-release-gates`  
**Parent:** `feature/nightwave-nw51-performance-reliability`  
**Date:** 25 October 2026  
**Source roadmap:** NW43 → NW44 → NW45 → NW46 → NW47 → NW48 → NW49 → NW50 → NW51 → **NW52**  
**Decision:** **NO-GO** for a public APK, Play submission, or changing the repository's private visibility.

## What NW52 delivers

- Preserves the 10-batch stacked Android source without blindly merging unrelated PRs into the old default `main` branch.
- Adds `tools/verify_nw52_release.py`, a pure stdlib release preflight that performs fail-closed static checks and documents missing external validation; `--self-test` deliberately mutates unsafe manifest/API/CI fixtures.
- Adds PowerShell-only `tools/check-nw52-integration.ps1` with a synthetic negative test and file/identity/backup/seek source guards, and wires it into `tools/build-windows.ps1` **before Gradle**. No Python needed to build locally. GitHub CI also cross-checks this script on hosted PowerShell when a runner is available.
- Wires NW52 into `.github/workflows/android.yml`, while explicitly keeping debug APK artifacts DEVELOPMENT/NOT-FOR-PUBLIC-RELEASE.
- Wires NW52 into the harmless GitHub Actions runner canary. Past workflows failed before any reported job steps; an unallocated runner is **not** a Kotlin verdict.
- Provides an exact-branch source acquisition and single Windows build path, with signing and no-data-loss checks performed before device installation.
- Keeps existing `applicationId=com.nightwave.player`, version `2.0.0-alpha5s` / `versionCode=35`, target/compile API 35 for this **unverified private development APK**, because a premature API bump also requires updating Windows builder hardcodes, 35.0.0 build tools, instrumented tests and Android 16 behavior.
- Does not weaken downloaded-file integrity, make unrestricted streaming claims, add tracking, clear data, or publish artifacts.

## Crucial distinctions

**All source batches integrated does NOT mean app built, verified, installed or legal for public distribution.** Source-only/static scripts are not a substitute for JVM tests, Android compilation, instrumented tests or manual device checks.

**Google Play target API 36 is mandatory for new phone apps/updates from 31 August 2026** (subject to published exceptions/extensions). Current target API 35 intentionally fails the NW52 Play release gate until a separate tested Android 16 migration is completed. The existing AGP version can support API 36, but the Windows build, test expectations, edge-to-edge/foreground behavior and UI must be updated and revalidated together.

**YouTube rights and compliance are hard release blockers.** The optional search-to-offline extraction and public YouTube source integrations need documented YouTube/platform permission and content rights, or a separately reviewed compliant public edition that does not offer the restricted paths. **Neither end-user confirmation nor a GPL license grants platform download rights.** NW52 does not bypass restrictions.

## Local source and Windows build procedure

1. On GitHub select the exact `feature/nightwave-nw52-final-integration-release-gates` branch, then **Code → Download ZIP**. Use the complete ZIP, not default `main` or earlier NW branches.
2. Extract into a clean, short Windows directory. Preserve the installed app and its existing data.
3. `build-windows.cmd` runs the PowerShell-only NW52 cumulative-source integrity test automatically before Gradle. Optional standalone check (no Python needed): `powershell -NoProfile -ExecutionPolicy Bypass -File tools/check-nw52-integration.ps1 -SelfTest` and then `powershell -NoProfile -ExecutionPolicy Bypass -File tools/check-nw52-integration.ps1 -ProjectRoot .`.
4. Optional when Python is available (source-only):
   ```powershell
   python tools/verify_nw52_release.py --self-test
   python tools/verify_nw52_release.py --json
   python tools/public_release_preflight.py --self-test
   python tools/public_release_preflight.py --json
   ```
   `--json` should show **NO_GO** for public release even when the source is syntactically valid. `--require-release-go` must return a nonzero exit until every real-world blocker has evidence.
5. For the FIRST integrated Android compilation, double-click `build-windows.cmd`. The existing project builder runs Gradle `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`, rechecks its exact-source fingerprint and verifies APK identity/signing. Do not treat an old or preserved debug APK as a successful build.
6. Review newly created `dist/NightWave-build-status.txt`, compiler/JUnit failures, source fingerprint and diagnostic logs. **BUILD SUCCESSFUL** and a trustworthy hash/signer report are mandatory. If any fail, fix the exact failing source/CI issue before testing.
7. Before installing on the actual phone, use `check-device-update.cmd` with ADB enabled and **read its installed-app signer continuity result**. Debug APKs from different signing keys cannot upgrade in place without data loss. **Never uninstall or clear data** as a workaround. Back up owned local content first if possible.
8. Install the verified, signer-compatible local candidate on a test phone, then run the matrix below. Only after ALL tests pass may a release-candidate decision be revisited.

## Final Android on-device regression matrix

| Feature | Minimum acceptance |
| --- | --- |
| Startup/restore | Cold/warm start, app restart/process death, previous selected song and queue restoration; no crash dialog |
| NW43 queue | Search plays only selected result; Play Next precedes auto suggestions; Shuffle/repeat/Undo; fast consecutive skips |
| NW44 search | Songs and Videos relevance, local saved title search, empty results, fallback and Bangla/English queries |
| NW45 account | Verify installed signer SHA-1, real Google chooser-to-connection, Drive upload/readback/restore/disconnect; error 8/10 |
| NW46 player | Full art and compact controls at 320/360/390dp, 2× font, rotation, missing art, swipe/cue seek |
| NW47 surfaces | Home recent removal/undo, Library folder/playlist/pin/bulk operations, You shortcuts |
| NW48 downloads | Eligible authorized sources only; pause stops and retry starts at zero; unknown bytes, progress, offline Ready/Issues, interrupted/corrupt source |
| NW49 studio | Audible EQ on supported output, unsupported route feedback, true local gapless/crossfade eligibility, LRC import/edit/seek/follow |
| NW50 motion | Reduced Motion, tab haptics on/off, rapid tab navigation, mini-player hide/restore |
| NW51 stability | Rapid search/queue changes, large indexes, foreground/background, memory growth, battery and no stale results |
| Background controls | Screen-off Bluetooth/headset/car/media notification behavior, other audio interruptions, no uncontrolled service loop |
| Safety | No crash/ANR, saved audio/playlists not deleted on failed update, no unexpected exported component, privacy settings honored |

## Independent remaining blockers

1. **CI runner CONFIRMED UNASSIGNED on NW52:** [Android run 40202785474](https://github.com/Nahian2002/NightWave/actions/runs/40202785474) and the independent [canary run 40202785552](https://github.com/Nahian2002/NightWave/actions/runs/40202785552) both completed as failures with `runner_id=0` and `steps=[]`. **NO code, checkout, Python, PowerShell, Gradle, or JUnit step ran.** Check repository/account Actions settings, billing/spending limits, and runner provisioning; do not guess the root cause or mislabel this a Kotlin error.
2. **Build:** No confirmed successful NW52 full Android compile, JUnit tests, androidTest-source compile, fresh debug APK hash or real regression run.
3. **API 36:** Upgrade compile/target SDK and all matching builder, aapt and static expectations in a separate compatibility-tested patch before any 2026 Play submission.
4. **Production signing:** Stable release/upload keystore stored outside Git, signing continuity, an actual release `.aab` with `bundleRelease`, Play App Signing and verified metadata; debug APK is NOT a public release.
5. **Google OAuth:** End-to-end account connection and Drive backup restore on the production signer remain unproven.
6. **Permissions/media rights:** Public background streaming/audio extraction/YouTube downloads and content licenses need a satisfactory legal/platform disposition.
7. **Privacy:** Publish an accurate hosted privacy policy, link it in-app, finalize Play Data safety and contact disclosures, validate third-party search/Drive data flows.
8. **Repository exposure:** Complete all-ref/all-commit Git history secret scan and license/asset provenance review before switching from private or sharing APKs.
9. **Supply chain:** Review dependency versions, advisories, SBOM, JitPack dependency ownership, and action SHA pinning.
10. **Play Console:** Registration, store assets/screenshots from a verified build, appropriate content rating and required testing/approvals.

## NW52 accounting

**Roadmap source batches implemented:** 10/10.  
**Build / hardware / Play release readiness:** **NO-GO** until documented evidence exists.  
**No app, Play account, GitHub visibility or release was published.**  

The next work item is NOT another cosmetic batch: obtain first real build evidence, fix specific Kotlin/JUnit errors, then complete compatibility/legal and device gates.
