# NightWave — public release readiness audit

**Date:** 2026-10-25  
**Audited development commit:** `d1df2636469a8d8bb464532119bb9e032f65c2b1` (`local/nightwave-nw34-offline-build-guard`)  
**Audit location:** `audit/nightwave-public-release-20261025`  
**Decision:** **NO-GO for public Android distribution and Google Play. NO-GO for flipping the GitHub repository public until history/secret/licensing checks are completed.**

## Scope and evidence

Evidence gathered directly from connected GitHub repository metadata, exact latest tree and selected source/configuration files; 706 tracked files across main Android app, tests, resources, build scripts and docs. Checked upstream policy resources current as of audit date. **Not** a complete manual read of every line, an executed full-tree secret scan, a Gradle compilation, a dependency vulnerability SBOM scan, a real Google account sign-in, or phone acceptance testing. Mark all of those **NOT VERIFIED**.

The `get_repo` metadata reports `visibility=private`, `default_branch=main`. Latest development commit `d1df2636` is on a different branch. Recursive trees contain **55** files on `main` and **706** on NW34. GitHub compare reports NW34 **2,209 commits ahead** of `main`, behind by zero. Merging requires intentional release curation, not a blind public-visibility toggle.

## P0 — mandatory before public distribution

1. **YouTube platform permissions and recording rights.**
   `app/src/main/java/com/nightwave/player/net/YouTubeGateway.kt` includes `resolveAudioOnlyForOffline`, and `app/src/main/java/com/nightwave/player/offline/SearchTrackSaveWorker.kt` passes that extracted stream to private storage. `NightWaveStudioActivity.kt` offers a search-result `Save offline` action. Explicit end-user consent is NOT YouTube approval. YouTube API Services Developer Policies prohibit caching/downloading/offline YouTube audiovisual content without prior written approval, and YouTube's compliance guide also identifies separating audio and background play of the YouTube player as prohibited/controlled behaviors for API clients. Google Play IP policies prohibit unauthorized streaming download apps. **Action:** remove/disable the YouTube save-to-offline code from a public distribution build and review online extraction/background playback compliance, OR obtain explicit applicable platform approval and recording rights. Do not advertise unrestricted YouTube/offline music.
   - https://developers.google.com/youtube/terms/developer-policies
   - https://developers.google.com/youtube/terms/developer-policies-guide
   - https://support.google.com/googleplay/android-developer/answer/9888072

2. **Play Store target API.**
   `app/build.gradle.kts`: `compileSdk=35`, `targetSdk=35`. Starting 2026-08-31 Google Play requires **API 36** for new Android phone apps and updates (subject to published exceptions/extensions). **Action:** update compile/target SDK, Android tools and lifecycle/notification/foreground-service compatibility; retest after migration.
   - https://support.google.com/googleplay/android-developer/answer/11926878

3. **No verified current full Android build.**
   NW34's own evidence docs say full Android Gradle/JUnit/instrumentation compilation and APK/device QA were not executed; source marker tests and isolated Kotlin smokes do NOT establish working app. Recent listed GitHub workflow runs on NW27 failed (the canary also failed before steps), and `.github/workflows/android.yml` does not include the `local/nightwave-nw34-offline-build-guard` development branch among push triggers. **Action:** build the exact public candidate offline with `build-windows.cmd`; require `:app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`, no Kotlin errors, no failed tests, fresh signed output. Execute Android instrumentation tests on a test phone/emulator; validate audio, pause/resume, storage, playback, crash reports, UI and privacy.

4. **No certified production AAB and release-signing continuity.**
   `app/build.gradle.kts` defines debug and a basic minified release variant but no maintained release `signingConfigs`/distribution process; Windows local builder and existing CI produce and verify **debug APKs**, not a Play release AAB. The repo has no published Releases. **Action:** establish a securely stored production upload signing key, reproducible release AAB build, Play App Signing, Play Console tests and update signature path. Never commit keys or install mismatched-signer APKs over an app containing user data.
   - https://developer.android.com/guide/app-bundle/faq

5. **No accurate, public user-data privacy policy discovered in latest source tree.**
   The app has optional Google sign-in and Drive backup/music locker, local crash reporting, local user audio/history, YouTube WebView and public Invidious search endpoints (which receive search text). Repo tree has no `PRIVACY.md`, privacy HTML, security policy or known public privacy site. **Action:** publish an accurate, non-geoblocked URL, link it from Play Console AND in the app, state data destinations, retention, deletion and contact, and truthfully complete Google Play Data safety form. Validate network behavior; don't assert zero data collection merely because there is no NightWave-owned server.
   - https://support.google.com/googleplay/android-developer/answer/10144311

6. **Public repository Git history and secret audit pending.**
   The current file tree contains no clearly named `.jks/.keystore/.pem/.p12` or `google-services.json`, but this is **not** a secret scan. Existing `.gitignore` only excludes common build/IDE files and does not prevent accidental signing-key or credential commits. A public GitHub visibility change exposes earlier commits and accessible refs. **Action:** scan **all branches/tags/commits**, forks/PR metadata and GitHub release artifacts for signing keys, API secrets, personal addresses, auth JSON, test accounts and private logs using an established scanner such as `gitleaks` plus manual review. Rotate any found secrets; remove sensitive history safely before exposing repository. Never disclose private key values in audit reports.

## P1 — high-priority release engineering

7. **Default branch and 15 open PRs.** Public visitors see old `main`, not NW34. Reconcile draft/stale PRs, choose a clean release candidate, use code review and branch protection, set the default branch, and ensure README/ABOUT/BUILD_AND_RELEASE/VERIFICATION_MATRIX match that exact commit. GitHub App integration couldn't read current branch protection (403), so policy is **UNKNOWN**, not confirmed disabled.

8. **OAuth public onboarding not proven.** App uses narrow Google `drive.appdata` and `drive.file` scopes, both listed by Google as non-sensitive scopes; basic OAuth app verification and accurate branding might still apply. Known field result: Google account chooser appeared but didn't connect. **Action:** verify actual production signer SHA-1/Android OAuth client, Google Drive API enablement, app consent screen publication, exact account/permission handling, upload + readback + restore + disconnect/reconnect under multiple test accounts.
   - https://developers.google.com/workspace/drive/api/guides/api-specific-auth

9. **Security/third-party data review.** `CommunityVideoSearch.kt` sends search text to independent Invidious servers. `YouTubeSearchBrowser.kt` enables JavaScript for a YouTube WebView (it disables file/content access, third-party cookies and mixed content). Inspect all third-party traffic/cookie/SDK handling and update privacy disclosures. `ExtractorDownloader.kt` follows redirects and forwards extractor-supplied headers; rely on current vetted upstream security behavior, not just string guards. Conduct OWASP MASVS-inspired testing and static/dynamic dependency checks.

10. **Open-source license and redistribution compliance.** Root `LICENSE` contains GPL v3; `NOTICE` calls project GPL-3.0-or-later and links NewPipeExtractor. Verify ownership of *all* original code, bundled icons, fonts, images, test samples and logos, and every dependency's license and notices. The current `NOTICE` is minimal. GPL source availability must accompany redistributed binaries as required by applicable licenses. Confirm app screenshots/branding avoid third-party trademarks/content rights without permission.

11. **Dependency supply-chain audit.** Dependencies include `NewPipeExtractor:v0.26.5` from JitPack, Media3 `1.6.1`, OkHttp `4.12.0`, AGP `8.9.2`, Kotlin `2.0.21`, Room `2.7.2` and AndroidX libs. Versions and vulnerabilities were **not** checked against an SBOM/CVE database in this audit. Configure Dependabot/renovation, dependency verification/lockfiles, vulnerability alerts and release SBOM before claiming security readiness.

12. **Test coverage, performance and UX.** Need real hardware/emulator tests: airplane-mode offline first tap and queues; signed playback continuity; reboot/process death; duplicate save/retry/cancel; low-storage/unknown-length downloads; WebView redirects; Android 16 behavior; EQ/crossfade/gapless/shuffle/loop; playback notifications/Bluetooth; local library deletion/recovery; Google Drive backup/restore; Bangla/English search; lyrics editor; tablet/foldable rotation; accessibility; crashes and ANRs. Monitor test outcomes and fix P0/P1 failures.

## P2 — release-polish tasks

13. Add `SECURITY.md` with a private vulnerability-reporting path, `CONTRIBUTING.md`, supported Android versions and a real changelog for the release candidate. Publish release notes without claiming 100% song availability.
14. Reduce the overly long development-phase README header and update stale `ABOUT.md`, `docs/BUILD_AND_RELEASE.md`, `docs/VERIFICATION_MATRIX.md` which refer to older branches. A public project's home page must make its real supported features obvious.
15. Add safe `.gitignore` patterns for keys, credential files and local signing properties (in addition to real secret scanning and key hygiene); check CI permissions, branch rules and GitHub releases. Note existing workflows use third-party GitHub Actions by mutable tags; pin actions by reviewed commit SHA for better supply-chain assurance.
16. Prepare original app icon and store listing, screenshots taken from a real verified build, content rating, Play data safety forms, developer contact and support site. For eligible newer personal Play Console accounts, closed testing requires **12 testers opted in continuously for at least 14 days**.
    - https://support.google.com/googleplay/android-developer/answer/14151465

## Separate publication decisions

**A. Open-source GitHub publication:** Potentially possible before Play distribution after history/secret/licensing/privacy disclosure checks and cleaning up the public-facing branch and claims. Does NOT mean an APK is certified or YouTube rights obtained. Consider keeping the experimental YouTube save pilot private until rights/platform position is resolved.

**B. GitHub APK download for general users:** NO-GO: rights + full build + signing continuity + tests + privacy/support + security checks.

**C. Google Play launch:** NO-GO: all preceding, plus API 36 target, App Bundle/Play App Signing, Play Console app/privacy content, applicable account testing, platform review.

## Minimal next safe local evidence required

```powershell
# From the freshly extracted exact candidate source folder:
python tools/audit_repo_nw22.py --self-test
python tools/audit_repo_nw22.py --json
.\build-windows.cmd
.\check-device-update.cmd
.\check-google-oauth.cmd
```

Only attempt device installation after build and signer checks have passed. Do not uninstall/reset/clear NightWave to fix a failed APK update. **Debug build success is not production-release signing.** Retain build log, test XML, APK verification output and acceptance notes as release evidence.

**Audit conclusion:** Public-release readiness is **NO-GO**. This report documents identified blockers and evidence gaps, not a completed certification or claim that all 706 tracked files / 2,209 intervening commits have been exhaustively security-reviewed.
