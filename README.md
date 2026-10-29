# NightWave

**Upcoming updates:** [revised roadmap and hourly sequence](docs/quality/UPCOMING_UPDATES.md). FINISH02 and ART02 are locally verified; SEARCH02 precedes NW137–NW145. One eligible batch per hourly run, with actual checks recorded.

**Current review work:** [ART02 cached artwork continuity](docs/quality/ART02_CACHED_ARTWORK.md) on [update/nightwave-art02-cached-artwork](https://github.com/Nahian2002/NightWave/tree/update/nightwave-art02-cached-artwork), [draft PR #157](https://github.com/Nahian2002/NightWave/pull/157), stacked on FINISH02 #156. Existing known songs recover available cached covers behind bounded source/deletion/lifecycle fences. Current offline metadata and queue display refresh; all features/themes and audio/source/data boundaries remain.

**Current integrated source:** [`main`](https://github.com/Nahian2002/NightWave/tree/main), based on NW126 and the cumulative implementation. **Canonical `main` contains the reviewed cumulative source.**

**30 October 2026 verification:** **1,762 JVM tests / 218 suites**, **75 source checks**, full Android/Compose/APK/instrumentation-source build PASS. Lint: **0 errors / 317 warnings / 17 hints**, including three triaged style/localization additions without suppression. Build **FD80578F**, same signer. Controlled paced loopback: **2.12 Mbps serial / 8.83 Mbps parallel**, identical bytes. This is not a phone/provider speed guarantee. [Exact evidence and remaining gates](TEST_REPORT.md).

**Active next-upgrade plan:** [NW126–NW145 roadmap](docs/PRODUCT_QUALITY_ROADMAP.md), [structured backlog](docs/quality/NW126_NW145_BACKLOG.json), [original audit](docs/audits/2026-10-28_PRODUCT_QUALITY_AUDIT.md). **Next source batch: NW137.** NW126–NW136 are locally verified with physical acceptance pending. The original 6.7 product baseline and latest recorded 8.8 visual rubric remain historical ratings; networking changes do not establish a new 9.5 score.

**Public release: NO-GO.** Installed-phone signer continuity, Samsung acceptance, production OAuth/Drive restore and source-rights review remain separate gates. A successful build does not authorize distribution or certify phone behavior.

## Start here

- **[Current resumable transfer evidence and phone checks](docs/NW136_RESUMABLE_TRANSFERS.md)** · [safe upgrade guide](docs/BUILD_AND_RELEASE.md)
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
| Version | `2.0.0-alpha5s`, code `35` |
| Android | API 26 minimum, compile/target API 36 |
| Toolchain | AGP 8.10.1, Gradle 8.11.1, Kotlin 2.0.21, Java 17 target |

## History and license

Previous milestone announcements are preserved in [README history](docs/README_HISTORY_20261027.md); they are not current build instructions. Historical tests, migrations and evidence remain available.

See [LICENSE](LICENSE) and [NOTICE](NOTICE). NightWave is independent of Spotify, Google, YouTube and NewPipe. Public distribution requires separate dependency, privacy and media-rights approval.
