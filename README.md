# NightWave

**Latest cumulative review candidate:** NW152/code48, locally verified; [checks and offline release rehearsal](docs/NW152_RECOVERY_READINESS.md). Physical48 remains pending. Only NW153 final acceptance remains; feature scope is frozen. No public APK or feed activation.

**Current product review:** [detailed code40 review](docs/audits/2026-11-04_CODE40_PRODUCT_REVIEW.md), [next upgrades NW146–NW153](docs/UPGRADE_ROADMAP_NW146_NW153.md). Provisional presentation index7.83/10; full product overall unscored. Next batch NW153 final acceptance. Older status notes below are historical.

**First device evidence:** owner-approved code40 in-place update and scoped Samsung phone smoke recorded in [review #170](https://github.com/Nahian2002/NightWave/pull/170). Installed APK identity/signature and29 pre-launch data-file hashes verified; basic UI checks observed. NW145 is PARTIAL, not a full219-case pass or9.5 rating. [Exact scope and pending cases](docs/NW145_SAMSUNG_CODE40_SMOKE.md).

## Current COMPAT01 source result — 4 November 2026

COMPAT01 locally verified on `update/nightwave-compat01-android6`, based on acceptance preparation #168. Code40 declares API23/Android6+,target36; six initial API errors fixed,1821 JVM/226 suites,87 source checks,full builder and sequential lint0 errors/326 warnings/17 hints. See `docs/COMPAT01_IMPLEMENTATION.md`. Owner defers device checking until source work is finished; do not ask for interim phone tests. Planned source upgrades through COMPAT01 are complete; NW145 physical acceptance remains NOT_RUN, rating NOT_ASSESSED/release NO_GO. No universal device-support claim. Feed disabled; automation deleted. Historical notes below preserve earlier states.

## NW145 acceptance preparation and compatibility target — 4 November 2026

NW145 is prepared, not certified; no connected Android device or approved reference profiles. Code39/source974/APK identity unchanged and signer rechecked. See `docs/NW145_ACCEPTANCE_RUNBOOK.md` and `docs/quality/NW145_RELEASE_EVIDENCE.json`. Owner requests roughly ten-year phone coverage: next source batch COMPAT01 evaluates Android6/API23+, currently NOT_IMPLEMENTED; code39 still requires Android8/API26. See `docs/ANDROID_COMPATIBILITY_PLAN.md`. All physical gates NOT_RUN; rating NOT_ASSESSED, release NO_GO. No app changes/new APK, full rebuild/lint repeat, device actions, publication, merge or schedule restart.

## Current NW144 source result — 4 November 2026

NW144 is locally verified on `update/nightwave-nw144-controls-sharing`, based on NW143 #166. Exact share/export preview and caption round-trip fix; existing external-control boundaries retained. Code39, build NW-debug-AE25B5B1EADF6B31: full builder,1819 JVM/226 suites,86 source checks and lint0 errors/324 warnings/17 hints PASS. See `docs/NW144_CONTROLS_SHARING.md`. Physical cases NOT_RUN. Next NW145 requires device acceptance evidence; no new9.5 rating. Stable feed disabled; hourly automation deleted. Historical notes below retain earlier states.

## Current NW143 source result — 4 November 2026

NW143 is locally verified on `update/nightwave-nw143-trust`, based on reopen fix #165. First-use/help/failure copy and retained diagnostics; code38, build NW-debug-515F3474BA50AE1F. Full builder,1817 JVM/226 suites,85 source checks and lint0 errors/324 warnings/17 hints pass. Physical and release cases remain NOT_RUN; see `docs/NW143_FIRST_RUN_TRUST.md`. Next independent source batch NW144. Stable feed disabled; hourly automation deleted. Historical notes below are superseded only by these specific results.

## Reopening / manual updater fix — 4 November 2026

Current review branch `fix/nightwave-reopen-weekly-updates` is based on updater #164. The restored mini-player wraps its content; automatic checks run on resume, and direct You → App health → Check for updates bypasses the automatic cooldown. Code37, same package/signer; stable feed remains disabled and hourly automation remains deleted. Phone acceptance is NOT_RUN. Original roadmap next batch remains NW143. See `docs/UPDATER_REOPEN_FIX.md`.

**Previous review:** [reopening and manual update fix](docs/UPDATER_REOPEN_FIX.md), [draft PR #165](https://github.com/Nahian2002/NightWave/pull/165), branch `fix/nightwave-reopen-weekly-updates`. Restored mini-player wraps content; update checks run on resume. Manual checks are directly in You → App health → Check for updates, and remain in App details. They fetch the latest approved release immediately, independently of the automatic seven-day timer. Signed stable feed remains **disabled pending public-release/main approval**.

**Previous verification:** 1,817 JVM tests / 226 suites, 84 source checks, full Windows build PASS; sequential lint 0 errors / 324 warnings / 17 hints with unchanged inventory. Build NW-debug-40A67A50656ED77A, code37, same package/signer. Actual phone long-absence, weekly/manual timing and installation cases remain NOT_RUN. [Exact evidence](TEST_REPORT.md). Main remains older; the cumulative review is unmerged.

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
