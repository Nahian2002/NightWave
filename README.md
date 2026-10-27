# NightWave

An independent Android music player with an original midnight design, local music, playlists, background playback, imported synchronized lyrics, Sound Studio and optional Google Drive backup.

**Current integrated source:** [`update/nightwave-nw126-build-identity-baseline`](https://github.com/Nahian2002/NightWave/tree/update/nightwave-nw126-build-identity-baseline), based on the post-NW125 audit and cumulative NW111–NW125 implementation. **Default `main` remains the older v0.8 snapshot.**

**28 October 2026 verification:** NW126 passes the complete local Windows builder, **1,496 JVM tests / 188 suites**, Android app/test-source compilation, debug assembly and **54 current checks**. The build ID embedded in the APK matches 809 source inputs; You → App health → App details exposes source, installed APK and public signer hashes. Phone attribution, in-place preservation and device acceptance remain pending. See [current verification](TEST_REPORT.md) and [NW126 evidence](docs/NW126_BUILD_IDENTITY_BASELINE.md).

**Active next-upgrade plan:** [NW126–NW145 product-quality roadmap](docs/PRODUCT_QUALITY_ROADMAP.md), based on the [28 October recording/source audit](docs/audits/2026-10-28_PRODUCT_QUALITY_AUDIT.md). Provisional product rating **6.7/10**; 9.5+ is the target, not an achieved certification. **Next source batch: NW127.** NW126 is locally verified with physical acceptance pending. The audit/plan and gate repair are on [`audit/nightwave-post-nw125-quality-roadmap`](https://github.com/Nahian2002/NightWave/tree/audit/nightwave-post-nw125-quality-roadmap).

**Public release: NO-GO.** Installed-phone signer continuity, Samsung acceptance, production OAuth/Drive restore and source-rights review remain separate gates. A successful build does not authorize distribution or certify phone behavior.

## Start here

- **[NW125 final build and phone-test instructions](docs/NW125_FINAL_CANDIDATE.md)**
- **[Active NW126–NW145 roadmap](docs/PRODUCT_QUALITY_ROADMAP.md)** · [structured backlog](docs/quality/NW126_NW145_BACKLOG.json) · [80 new acceptance cases](docs/quality/NW126_NW145_ACCEPTANCE.csv)
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

Use a fresh clone or full ZIP of **`release/nightwave-nw125-final-candidate`**, then run `build-windows.cmd`. Require a fresh success in `dist/NightWave-build-status.txt`. Before installing, run `check-device-update.cmd` and verify the signer matches the existing app. Preserve playlists, music and settings; never uninstall or clear storage to force an upgrade.

Developers can run `python tools/verify_current.py` for structural and Windows preflight checks. The current automated workflow is `.github/workflows/current-android.yml`; `android.yml` is a manually invoked historical diagnostic workflow. Real JVM and Android compilation remain mandatory.

| Build identity | Value |
| --- | --- |
| Application ID | `com.nightwave.player` |
| Version | `2.0.0-alpha5s`, code `35` |
| Android | API 26 minimum, compile/target API 36 |
| Toolchain | AGP 8.10.1, Gradle 8.11.1, Kotlin 2.0.21, Java 17 target |

## History and license

Previous milestone announcements are preserved in [README history](docs/README_HISTORY_20261027.md); they are not current build instructions. Historical tests, migrations and evidence remain available.

See [LICENSE](LICENSE) and [NOTICE](NOTICE). NightWave is independent of Spotify, Google, YouTube and NewPipe. Public distribution requires separate dependency, privacy and media-rights approval.
