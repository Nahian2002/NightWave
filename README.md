# NightWave

**Upcoming updates:** [hourly sequence and both usage guards](docs/quality/UPCOMING_UPDATES.md). NW139 recovery source is locally verified; NW140 Android performance is next. One eligible batch per run with actual evidence recorded.

**Current review work:** [NW139 optional recovery](docs/NW139_RECOVERY.md) on [update/nightwave-nw139-recovery](https://github.com/Nahian2002/NightWave/tree/update/nightwave-nw139-recovery), [draft PR #161](https://github.com/Nahian2002/NightWave/pull/161), stacked on [NW138 #160](https://github.com/Nahian2002/NightWave/pull/160). Compact account flow, clearer progress, exact access/consent fences and a fix for extra uploads after failed backup comparisons. Existing features/themes, scopes, archive formats, source quality, package and signer remain.

**Current integrated source:** [`main`](https://github.com/Nahian2002/NightWave/tree/main) remains the integrated baseline; newer cumulative changes are on the review branch above and are not automatically merged.

**30 October 2026 verification:** **1,803 JVM tests / 222 suites**, **79 source checks**, full Android/Compose/APK/instrumentation-source build PASS. Sequential lint: **0 errors / 322 warnings / 17 hints**, unchanged notice inventory. Build **NW-debug-238C337491E75208**, same signer. Ten new Android methods compile only. Actual disposable-account recovery and phone acceptance remain NOT_RUN; [prepared live procedure](docs/NW139_RECOVERY_LIVE_PROCEDURE.md). [Exact evidence and remaining gates](TEST_REPORT.md).

**Active next-upgrade plan:** [NW126–NW145 roadmap](docs/PRODUCT_QUALITY_ROADMAP.md), [structured backlog](docs/quality/NW126_NW145_BACKLOG.json), [original audit](docs/audits/2026-10-28_PRODUCT_QUALITY_AUDIT.md). **Next source batch: NW140.** NW126–NW139 and focused follow-ups are locally verified with physical acceptance pending. Historical ratings remain evidence; passing local checks does not establish a new 9.5 score.

**Public release: NO-GO.** Installed-phone signer continuity, Samsung acceptance, production OAuth/Drive restore and source-rights review remain separate gates. A successful build does not authorize distribution or certify phone behavior.

## Start here

- **[Current recovery evidence and phone/account checks](docs/NW139_RECOVERY.md)** · [safe upgrade guide](docs/BUILD_AND_RELEASE.md)
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
