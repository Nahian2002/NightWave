# NightWave

An independent Android music player with an original midnight design, local music, playlists, background playback, imported synchronized lyrics, Sound Studio and optional Google Drive backup.

**Current cumulative source:** [`update/nightwave-nw120-audio-session-truth`](https://github.com/Nahian2002/NightWave/tree/update/nightwave-nw120-audio-session-truth), based on verified NW119 commit `ccbaae441fecea1bc3ca5b018fde33bb17ced554`. The default `main` still contains the old v0.8 app.

**27 October 2026 validation:** Android app and instrumentation sources compile, the debug APK assembles, and **1,433 JVM tests pass with zero failures or skips**. See the [current test report](TEST_REPORT.md) and [update evidence](docs/NW120_AUDIO_SESSION_EVIDENCE.md) for scope and receipts.

**Public release: NO-GO.** Installed-phone signer continuity, Samsung acceptance, production OAuth/Drive restore and source-rights review remain separate gates. A successful build does not authorize distribution or certify phone behavior.

## Start here

- [Active NW111–NW125 roadmap](docs/NW111_NW125_SPOTIFY_QUALITY_ROADMAP.md)
- [Build and safe update instructions](docs/BUILD_AND_RELEASE.md)
- [Repository navigation](docs/REPOSITORY_INDEX.md)
- [Current checks and historical tooling](tools/README.md)
- [Release readiness](RELEASE_READINESS.md), [security](SECURITY.md), [privacy draft](PRIVACY_POLICY_DRAFT.md)

NW120 makes EQ commands specific to the reviewed audio output, verifies preset readback, refreshes control-loss status and keeps EQ on both eligible crossfade decks. Audible speaker/Bluetooth/headphone acceptance remains pending.

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

Use a fresh clone or full ZIP of the current branch, then run `build-windows.cmd`. Require a fresh success in `dist/NightWave-build-status.txt`. Before installing, run `check-device-update.cmd` and verify the signer matches the existing app. Preserve playlists, music and settings; never uninstall or clear storage to force an upgrade.

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
