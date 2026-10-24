# NightWave Security Policy

NightWave is currently a **development project**, not a certified public Android release.

## Responsible disclosure

Please **do not** publish private data, exploit instructions, credentials, authorization tokens, user recordings, or unredacted crash/build logs in a public issue or pull request.

If GitHub's **Private Vulnerability Reporting** feature is enabled for this repository, use **Security → Advisories → Report a vulnerability**. Otherwise, contact the maintainer through a privately available contact route on their GitHub profile. **Do not create a public vulnerability report while waiting for a private contact method.**

Maintainers should triage reports privately, reproduce with disposable test data, assess security impact, and publish a fix and coordinated advisory before releasing details.

## Supported versions

| Source | Security support |
| --- | --- |
| Current private development branch | Best-effort maintenance; no public security SLA |
| Historical experimental APKs | Not supported for public distribution |
| Official public release | **None published or certified yet** |

Never use a third-party debug APK or a different signing identity to upgrade over an installed app containing private music. Do not uninstall or clear user data to bypass a signer mismatch.

## Design and reporting notes

- Local music, playlist metadata, lyrics, and crash summaries can contain sensitive personal information. Share redacted reproduction steps, not real music files, access tokens, account email addresses, or private paths.
- Google authorization uses narrow Drive scopes; a granted permission and the user's Drive contents must not be treated as public test data.
- Outdated external streaming endpoints, third-party services, DRM restrictions and licensing are release risks, not bugs that justify bypassing restrictions.
- The latest audit is [Public Release Readiness](docs/PUBLIC_RELEASE_READINESS_AUDIT_20261025.md).

**Before making this GitHub repository public:** enable private vulnerability reporting or provide a monitored nonpublic security contact channel.
