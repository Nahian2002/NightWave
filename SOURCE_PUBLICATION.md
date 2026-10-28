# NightWave source-publication record

**28 October 2026 â€” final source review complete; visibility operation/readback pending.** This is source-only GitHub publication. No APK, store submission, website, app-feature change, dependency change, installation or account-data operation is included.

The owner requested publication after receiving the copying/public-log/contact, licensing/provider and enforcement risks. The precise payload is cumulative NW132 plus maintenance, protected at `736f014e6ff38d73f29e014057f3c398a83a63d3`; preparation is on `audit/nightwave-source-publication-final`. It was promoted to canonical `main` by fast-forward, preserving history. [Final review](docs/publication/FINAL_SOURCE_REVIEW_20261028.md).

## Verified payload

| Check | Evidence |
| --- | --- |
| App/build preservation | 848 protected files byte-identical to NW132 maintenance; all 843 packaged source inputs and existing APK unchanged. [Receipt](.github/security/final-source-preservation.json). |
| Git history | Fresh all-ref scan, with PR heads through #137, covers 3,481 reachable commits before final preparation commits. Thirteen exact immutable noncredential exclusions; no wildcard/rule/path exclusion. Later publication commits must pass the same scan. |
| Current tracked/new source | Thirteen individually reviewed noncredential matches: source hashes, Compose keys and historical release-table text. |
| Hosted records | All 836 retained workflow-log archives, 137 PR records and five comments checked privately; zero hosted secret matches or attachments. Only OpenAI/GitHub no-reply email identities appeared there. |
| Existing downloads | Zero retained Actions artifacts and zero releases/assets in the reviewed inventory. No prior upload is silently assumed safe. |
| Source/media inventory | App source contains Kotlin, XML and one JSON test corpus; no bundled recording, lyric file, bitmap, font or third-party binary was identified. GPL declaration/license and upstream notice remain. |
| Dependency/provider review | Supplier license inventory and three unvalidated Guava/jsoup advisory matches remain disclosed. Source-only publication does not grant media/service permissions or certify the runtime app. [Dependency review](docs/publication/DEPENDENCY_REVIEW.md). |

## Repository controls

Repository-wide Actions was disabled and independently read back before exposure. This protects against older refs whose workflows predate the current private-only artifact guards. Local build/source checks remain available; re-enabling hosted execution requires deliberate review of active refs. Read-only tokens and disabled workflow PR approval remain; fork write tokens/secrets are disabled.

Current workflows use exact Action revisions, no persisted checkout credentials and private non-PR artifact conditions. The scanner is checksum-pinned; broad exclusions and unsafe workflow fixtures are rejected. The final preparation has an explicit app-preservation check against the current protected parent, while the historical NW130 preparation retains its original baseline.

Verified live settings after publication:

| Setting | Readback |
| --- | --- |
| Repository/default branch | Public; canonical `main` contains the reviewed cumulative source |
| Secret scanning/push protection | Enabled |
| Vulnerability alerts/private reporting | Enabled |
| Outside-contributor workflow approval | All external contributors |
| Workflow token | Read-only; cannot approve PRs |
| Hosted Actions | Disabled repository-wide; no automatic APK upload |
| Main branch | One review and CODEOWNERS review; stale reviews dismissed; conversations resolved; force pushes/deletions disallowed by the configured rule |

Owner administrators retain their documented bypass. No hosted status check is required while Actions is disabled. Automatic Dependabot update PRs are not enabled, preserving the no-dependency-change scope. GitHub showed zero open secret alerts at initial readback; this is not proof its asynchronous history scan has finished. Local all-ref/source/hosted reviews remain the separate evidence. [Sanitized operation receipt](.github/security/source-publication-result.json).

## Boundaries that remain

- Public copies/forks cannot be recalled by making the repository private later. The owner approved publishing the personal security/privacy contact; monitoring remains the owner's responsibility.
- GPL covers applicable software, not recordings, lyrics, artwork, trademarks or third-party services. No blanket license-compatibility, provider approval or takedown-immunity claim is made. No specific hosting copyright violation was established by this review.
- Dependency matches are not confirmed app exploits, but remain unresolved app-security review items. They are not marked fixed by publishing source.
- App release remains **NO-GO** pending installed signer, phone/accessibility/audio/recovery/performance and applicable distribution review. Source-only publication has no 9.5-score or Play-submission prerequisite. NW133 remains next.

The earlier NW130-only preservation work is historical [PR #134](https://github.com/Nahian2002/NightWave/pull/134), with its original 839-file/834-input receipt retained. NW131/NW132 and maintenance were separately authorized product work. This final publication changes none of their app/build inputs.

Private recovery: `20261028-source-publication-final`, containing protected history, scan/triage/log/privacy/settings receipts and final operation evidence. The verified app build remains [NW132 maintenance](TEST_REPORT.md). Never publish the raw administrative/log/scanner checkpoint.

Policy references: [GitHub visibility](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/managing-repository-settings/setting-repository-visibility), [GitHub copyright process](https://docs.github.com/en/site-policy/content-removal-policies/dmca-takedown-policy), [YouTube terms](https://www.youtube.com/static?template=terms).
