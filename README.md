# NightWave

**Current review:** [weekly in-app updater](docs/WEEKLY_UPDATER.md), branch `update/nightwave-weekly-updater`. App-opening checks offer only the latest owner-approved version at most once every seven days. Downloads stay in the app and are verified before Android installation confirmation. Manual checks: You → App details → Check for updates. The signed stable feed is **disabled pending public-release/main approval**; no automatic publication or installation.

**Verification:**1,817 JVM tests/226 suites,84 source checks,full Windows build PASS; lint0 errors/324 warnings/17 hints(two new style suggestions). Build NW-debug-4085DB540B6787AA,code36,same package/signer. Phone installation, actual seven-day lifecycle and prior physical gates NOT_RUN. [Exact evidence](TEST_REPORT.md). Current integrated main remains older; this cumulative review includes the preserved NW142 layout work and is unmerged.

**Schedule:** the hourly automation remains deleted. The owner's allowance override covered this updater only. [Roadmap](docs/PRODUCT_QUALITY_ROADMAP.md), [backlog](docs/quality/NW126_NW145_BACKLOG.json), [upcoming work](docs/quality/UPCOMING_UPDATES.md). No new9.5 rating is inferred.

**Public release: NO-GO.** Installed-phone signer continuity, Samsung acceptance, production OAuth/Drive restore and source-rights review remain separate gates. A successful build does not authorize distribution or certify phone behavior.

## Start here

- **[Current bilingual/accessibility work and device checks](docs/NW141_ACCESSIBILITY_BILINGUAL.md)** · [safe upgrade guide](docs/BUILD_AND_RELEASE.md)
- **[Active quality roadmap and revised sequence](docs/PRODUCT_QUALITY_ROADMAP.md)** · [structured backlog](docs/quality/NW126_NW145_BACKLOG.json) · [80 new acceptance cases](docs/quality/NW126_NW145_ACCEPTANCE.csv)
- [NW111–NW125 implementation history and remaining gates](docs/NW111_NW125_SPOTIFY_QUALITY_ROADMAP.md)
- [Build and safe update instructions](docs/BUILD_AND_RELEASE.md)
- [Repository navigation](docs/REPOSITORY_INDEX.md)
- [Current checks and historical tooling](tools/README.md)
- [Release readiness](RELEASE_READINESS.md), [security](SECURITY.md), [privacy draft](PRIVACY_POLICY_DRAFT.md)

The cumulative source contains NW122's measured host performance work, NW123's accessibility/large-text improvements, and NW124's user-confirmed playlist text import/export, conservative Commons discovery and sleep-timer improvements. The new audit identifies 32 findings and preserves the original 139-case matrix. The owner's recording supplies useful visual evidence; controlled phone performance, data-preserving upgrade, live restore and playback soak still require the explicit acceptance runs.

## Features

| Area | Development source |
| --- | --- |
| Library | Owned/authorized local audio, liked songs, recent history, playlists, folders, sorting and bounded multiselect |
| Player | Media3 background playback, queue, shuffle/repeat, seeking, mini-player and expanded Now Playing |
| Lyrics | Explicit local LRC import/review/edit/export, full-screen follow, fine timing and accessible controls |
| Audio | Sound Studio, session-aware EQ and offline transition controls |
| Optional Drive | Consented backup, preview/restore and personal audio locker workflows |
| Discovery | Saved-music search and experimental public-video discovery |

Public video references and playable offline files are separate. Saving a URL does not download audio or grant storage rights. Unsupported or restricted streams can fail. There is no required NightWave server, paid account or subscription.

## Build on Windows

Use a fresh clone or full ZIP of the **current integrated source branch linked above**, then run `build-windows.cmd`. Require a fresh success in `dist/NightWave-build-status.txt`. Before installing, run `check-device-update.cmd` and verify the signer matches the existing app. Preserve playlists, music and settings; never uninstall or clear storage to force an upgrade.

Developers can run `python tools/verify_current.py` for structural and Windows preflight checks. Workflow definitions remain in `.github/workflows/`, but repository Actions is deliberately disabled after source publication. Run local verification; no hosted CI pass is claimed. Real JVM and Android compilation remain mandatory.

| Build identity | Value |
| --- | --- |
| Application ID | `com.nightwave.player` |
| Version | `2.0.0-alpha5s`, code `36` |
| Android | API 26 minimum, compile/target API 36 |
| Toolchain | AGP 8.10.1, Gradle 8.11.1, Kotlin 2.0.21, Java 17 target |

## History and license

Previous milestone announcements are preserved in [README history](docs/README_HISTORY_20261027.md); they are not current build instructions. Historical tests, migrations and evidence remain available.

See [LICENSE](LICENSE) and [NOTICE](NOTICE). NightWave is independent of Spotify, Google, YouTube and NewPipe. Public distribution requires separate dependency, privacy and media-rights approval.

[Draft updater PR #164](https://github.com/Nahian2002/NightWave/pull/164) is unmerged; stable activation remains disabled pending owner approval.
