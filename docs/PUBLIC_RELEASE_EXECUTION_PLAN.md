> Historical 25 October plan. Current repository-only preparation and exact app-preservation evidence: [28 October source-publication status](../SOURCE_PUBLICATION.md). NW130 now builds and targets API36; older SDK/build statements below are retained as history. No app change is authorized for publication preparation.

# NightWave — execution plan without Android application changes

**Prepared:** 25 October 2026
**Protected app baseline:** `local/nightwave-nw34-offline-build-guard`, commit `d1df2636469a8d8bb464532119bb9e032f65c2b1`.
**Preparation branch:** `prep/nightwave-public-no-app-changes-20261025`.
**Current decision:** NO-GO for distributing an APK or claiming Play Store compliance; GitHub source publication also awaits the complete history/secret and rights reviews.

This plan separates **repository-only work** from work that cannot be completed without app modifications, credentials, outside approval, or real Android validation.

## 1. Repository tasks (may be done without changing the app)

- [x] Source-based public-release audit at `docs/PUBLIC_RELEASE_READINESS_AUDIT_20261025.md`
- [x] Draft security handling policy and contribution rules
- [x] Draft source-informed privacy and third-party data-flow disclosures, **not certified or published**
- [x] Release gates and no-data-loss verification procedure
- [x] Expanded `.gitignore` for credential and signing material (not a history-cleanup substitute)
- [x] Prepared Gradle/GitHub Actions Dependabot configuration (no dependency upgrades merged)
- [x] Automated local source/static/preflight scripts and dependency inventory (run evidence still required)
- [x] Release candidate documentation header and issue/approval plan
- [ ] Run whole-tree source static scan against exact checkout; archive sanitized report
- [ ] Run **all historical commits/refs** through a proven secret scanner; rotate and remediate any discoveries before public visibility
- [ ] Full third-party code/asset provenance/license review; document evidence and notices
- [ ] Decide how to merge/reconcile the 15 open draft PRs and designate a stable, **reviewed** release branch
- [ ] Enable repository secret scanning, dependency alerts, private vulnerability reports and branch protection through GitHub Settings where available
- [ ] Publish and monitor an actual private support/privacy contact channel; finalize URL

## 2. App changes or external services explicitly **NOT** performed

- [ ] **YouTube rights/terms:** obtain applicable YouTube written approval and recording rights **or** prepare a separate approved public app build without the restricted YouTube offline flow (requires explicit approval for app changes)
- [ ] **Play targetSdk/compileSdk:** Android phone apps submitted to Google Play after 31 Aug 2026 need API 36; current app stays at API 35 at the user's request
- [ ] **Production AAB signing:** production signing identity, secure key storage, Play App Signing and `bundleRelease` acceptance; no keys should be committed
- [ ] **Google OAuth:** verify production package/certificate SHA-1, consent publication, scopes, backup-upload-and-readback, music locker and disconnect
- [ ] **Privacy presentation inside the app:** add a publicly reachable accurate policy URL and an app entry to it (requires a later app change)
- [ ] **Android device QA:** installation, media controls, safe updates, Airplane mode, lyrics, background audio, offline saves, low storage, process death, Google Drive, TalkBack and UI ratios
- [ ] **External publication:** Play Console paperwork, developer verification, data safety, possibly closed testing, actual public repository visibility and final release
- [ ] **Play submission:** target API, IP, privacy and quality approval. No submission made.

## 3. Required evidence — do not mark green without artifacts

| Gate | Required evidence | Present |
| --- | --- | --- |
| Git source/secret history | History scanner result for all refs, no unreviewed keys or personal data | NO |
| Static source | Checked full-tree source inventory and parser/security report | NO |
| Dependencies/licenses | Reviewed dependency manifest + license and CVE assessment | NO |
| Android app | Full Gradle JVM tests, debug assemble, test-source compilation | NO |
| Production bundle | Signed AAB, reproducible signer/identity and mapping review | NO |
| Android hardware | Phone/emulator acceptance and no data loss during update | NO |
| Google OAuth/Drive | Real production-signer login and verified cloud readback | NO |
| Platform rights | Documentary approval or compliant design disposition | NO |
| Privacy/Play | Accurate published policy, in-app access and Play declarations | NO |

## 4. Safe local operator commands

Run these in the **complete checked-out preparation branch**:

```powershell
python tools/public_release_preflight.py --self-test
python tools/public_release_preflight.py --json
python tools/release_dependency_inventory.py --self-test
python tools/release_dependency_inventory.py --json
python tools/audit_repo_nw22.py --self-test
python tools/audit_repo_nw22.py --json
```

From a **full local git clone with all refs**, not only a GitHub ZIP, use:

```powershell
python tools/public_release_preflight.py --history --json
```

The history option requires `gitleaks` installed locally and will fail closed if unavailable. It writes a local redacted report under `dist/` (already Git-ignored). Never upload the raw findings or security reports to a public issue.

Finally, separately build the untouched app with `build-windows.cmd`, and compare the current installed-app signing identity using `check-device-update.cmd`. **Do not uninstall or clear app data.**

## 5. Publication sequence

**First:** clear sensitive historical content and license/rights issues, obtain owner review of privacy disclosures and exact source audit. **Second:** freeze and review a clean public source branch. **Third:** choose default branch/visibility with the maintainer's explicit consent, after checking every ref and issue/PR attachment. **Last:** distribute an Android app only after production signing, source permissions, Google Play requirements and phone QA.

No task in this document changes installed NightWave behavior, its Android package, or the existing offline library.
