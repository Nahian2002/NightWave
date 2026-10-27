# NW125 — Tracked-file organization, deletion review, CI hygiene

Date: 27 October 2026
Source reviewed: NW124 commit `a21a50c7c4d8039b1950ae226f791cf2173be5c3`.
Target: `release/nightwave-nw125-final-candidate`.

## Exact inventory inspected

A recursive GitHub Git tree was requested at the pinned NW124 parent, with `recursive=1`; it returned **`truncated=false`**:

| Tracked scope | Entries |
| --- | ---: |
| File blobs overall | **1,089 at the parent**, plus **NW125 source gate** added = **1,090 at the first NW125 audit** |
| `app/` | 519 |
| `app/src/` | 516 |
| `docs/` | 261 |
| `tools/` | 283 (including the new NW125 gate at that audit) |
| `.github/` | 4 |
| Root, including Gradle/manifests/Windows launchers | Remaining tracked files |

**No tracked file path** matched a generated `.apk`, `.aab`, `.zip`, `.log`, `.jks`, `.keystore`, `.p12`, `.pem`, `google-services.json`, `credentials.json`, tracked `build/`, `dist/`, `.gradle/`, or `.idea/` in that inventory. This is **not** proof no secrets exist in Git history, file bodies, attachments or connected services.

## Cleanup actually made

- Added a deterministic *read-only* source/packaging gate that refuses common tracked credentials and generated output, checks essential Gradle/CI/safety/test paths, pins app identity and reads all 139 device matrix records. Integrated with the active Android build workflow, which must still execute on a real runner.
- Deactivated **25 stale historical branch push trigger lines** in `.github/workflows/nightwave-runner-canary.yml`. The canary remains available **manual-only** to distinguish GitHub runner/billing provisioning from app failures. Real `current-android.yml` is still the actual build CI.
- Reconciled the authoritative `README.md`, `RELEASE_READINESS.md`, `TEST_REPORT.md`, `docs/REPOSITORY_INDEX.md`, `docs/BUILD_AND_RELEASE.md`, `tools/README.md`, and NW111–125 roadmap, so the old `main` or obsolete NW122 pointers cannot silently become the source used for an alleged NW125 build.
- Kept prior evidence and all production Kotlin/Java/XML, schema migration files, source-check helpers, scripts and tests in place.

## Safe deletion decision

**Files deleted: 0.** The user authorized removing unnecessary files, not destroying history or test infrastructure. All obvious generated junk was already absent. Old `tools/verify_*.py`, historical `NW*.kt`, `run-nw90-final.cmd` and `run-nw91-final.cmd` are either evidence, directly called by active compatibility tooling or retained explicitly for earlier build recovery. No individual path was proven redundant *and* unreferenced by CI, Windows launchers, docs, SDK/signing/Room, original tests and recovery workflows. Blind deletion would regress a currently uncompiled final candidate; therefore no destructive tree removal was authorized by evidence.

No `main` update, bulk branch deletion, history rewrite, automatic merge, APK installation, external account action, user-library mutation or public release was performed.

## Further checks required for a public repository

- Scan **entire Git history and all branches**, not just HEAD, for credentials with tools like Gitleaks. Rotate exposed credentials if found. Binary and Git LFS/attachment review required.
- Verify actual third-party **code/media rights** and original graphics/fonts/LICENSE/NOTICE notices; API/source policies, Google consent and device runtime need independent review.
- Require exact-current-commit Android/JVM/Compose build, APK-signing continuity against installed Samsung NightWave and 139 device cases before calling it a stable production app.

See [NW125 candidate guide](NW125_FINAL_CANDIDATE.md). Public release remains **NO-GO**.
