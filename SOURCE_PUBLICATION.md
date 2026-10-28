# NightWave source-publication preparation

28 October 2026. Protected app parent: `fb8f0455c6903249ab85299a76566153a2b8d2a7` (NW130). Preparation branch: `audit/nightwave-public-source-preparation`.

**The app is unchanged.** This work changes repository workflows, documentation and publication checks only. It does not remove or alter a feature, dependency, account flow, permission, resource, manifest, test, database, build script, package or signing identity. The existing APK is retained byte-for-byte. This freeze applies to this preparation change; the separate NW131 product roadmap remains planned.

**Current visibility remains private. Source publication is held for owner review and unresolved external checks. Public app distribution remains NO-GO.** No website, APK, store listing or repository visibility was published by this preparation.

## Completed repository work

- Pinned six upstream Actions repositories to exact reviewed tag resolutions; disabled persisted checkout credentials and retained read-only job tokens. This reduces mutable-reference risk; it is not a claim of auditing all upstream implementation code.
- Every existing artifact upload now requires a private repository and a non-PR event, in addition to its original success/failure/always condition. Public source builds cannot automatically upload APKs or diagnostic artifacts through these steps. Android compilation and existing verification commands are retained.
- Added a separate source-publication workflow with checksum-pinned Gitleaks 8.30.1, full available Git history, no inline allow comments and no scan-result uploads. Existing public job logs still need audience/content review before changing visibility.
- Reviewed all four original `.github` files with independent security and architecture passes. No reportable vulnerability was established in that narrow scope; application/helper implementations, remote settings and historical artifacts are excluded.
- Scanned 3,468 reachable commits after fetching all branch/tag references and 134 pull-request heads. The 12 scanner matches were individually verified as source hashes, Compose item keys or release-table text, not credentials. `.gitleaksignore` lists only their immutable commit/path/rule/line fingerprints, with reasons in `.github/security/gitleaks-reviewed-matches.json`. New matches remain blocking.
- Separately scanned the current tracked/new source snapshot, excluding ignored user media/build outputs; 12 equivalent noncredential matches were individually reviewed. A scanner cannot prove absence of secrets or replace manual review of discussions, attachments, metadata and hosted artifacts.
- Recorded 164 Maven coordinates from the offline-resolved release graph, including platform/BOM nodes. Cached POMs existed for 155, with license declarations for 152. The inventory is not a complete APK SBOM or a license-compatibility sign-off.
- Checked those coordinates against OSV. Three advisory matches require app-specific reachability review; no library was updated. See [dependency review](docs/publication/DEPENDENCY_REVIEW.md).
- Added maintainer ownership, a private reporting contact approved by the owner, source-use notes and fail-closed publication checks. The contact is `jayednahian2002@gmail.com`; monitoring remains the owner's responsibility.

## Remaining publication decisions

| Gate | Status / next evidence |
| --- | --- |
| App preservation | Require the final 839-file byte comparison, unchanged 834-input packaged source identity and unchanged APK hash. |
| Credentials in fetched Git history | No confirmed credential in the reviewed scanner matches. Future commits/refs must be rescanned; no unbounded no-secrets guarantee. |
| Hosted material | Review past Actions job logs/artifacts, release assets, issues/PR discussions/attachments, author identities and refs. Old copies are not removed by new workflow conditions. |
| Repository settings | These safeguards exist only on the preparation branch until reviewed/merged. Older branch/workflow revisions are not rewritten and can retain different upload behavior; review those refs or disable Actions repository-wide before public visibility. Select the intended reviewed default branch; old `main` remains v0.8. Verify branch/CODEOWNERS enforcement, fork-run approvals, secret scanning/push protection and available alerts. The connected connector does not expose these settings; no setting is claimed enabled. |
| Private reporting | Email is supplied. GitHub Private Vulnerability Reporting is a public-repository feature; enable and verify it during an explicitly approved publication operation if desired. |
| Software and asset rights | Complete code/asset provenance, transitive license compatibility and redistribution notices. A declared GPL license alone cannot prove ownership of every contribution. |
| Source/provider permissions | No YouTube/platform/recording approval is established. Existing extraction/offline behavior remains untouched; no claim of legality or guaranteed hosting/store acceptance is made. |
| Dependency advisories | Three database matches remain pending reachability/disposition. Changing app dependencies is prohibited for this preparation. |
| Privacy / public app distribution | Source-informed draft/contact updated; no live policy site or runtime/OAuth/deletion verification is claimed. Installed signer, release signing, device/audio/data-preservation, provider and distribution gates remain open. |

## Verification and recovery

Run `python .github/scripts/check_publication_controls.py --self-test --verify-app-unchanged` from this preparation checkout. It checks the actual workflow text, action revisions, every artifact condition and exact reviewed scanner exclusions, then rejects changes/additions to protected app/build paths. Negative fixtures reject public uploads, credential persistence, write tokens, privileged PR triggers, unknown action revisions and broad secret exclusions. Independent YAML parsing and scanner positive-control evidence are retained privately.

The local checkpoint `20261028-public-source-preparation` retains the protected history, app-file byte hashes, APK/source identity, redacted scanner/triage results, dependency reports, workflow resolution records and final review state. Never post raw findings, personal recordings or account data in a public issue. No historical refs were deleted or rewritten, no credentials were used to test validity, and no phone was installed/reset.

The original 139-case acceptance matrix, frozen product audit and app test evidence are retained. Current product verification remains the NW130 result in [TEST_REPORT.md](TEST_REPORT.md); repository-only checks do not create a new app build or change its provisional 6.7 rating.

## Enforcement risk

Making source public is not an automatic app ban. It also does not exempt the project from GitHub's acceptable-use/copyright processes, service-provider terms or app-store rules. A complaint or platform review is a separate event whose outcome cannot be guaranteed. Personal-use notices, GPL licensing, disclaimers or user consent do not replace applicable platform/content permissions.

Official policy references checked for this review: [GitHub visibility](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/managing-repository-settings/setting-repository-visibility), [GitHub acceptable use](https://docs.github.com/en/site-policy/acceptable-use-policies/github-acceptable-use-policies), [YouTube Terms](https://www.youtube.com/static?template=terms), [YouTube API policies](https://developers.google.com/youtube/terms/developer-policies) (their API-client scope must be evaluated separately from NewPipe extraction), [Play intellectual property](https://support.google.com/googleplay/android-developer/answer/9888072?hl=en), [Play user data](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en).
