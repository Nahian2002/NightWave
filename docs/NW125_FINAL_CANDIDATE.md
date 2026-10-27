# NW125 — Final candidate handoff and evidence

> 28 October 2026 follow-up: the cumulative app has now passed a fresh local Windows build, all 1,484 JVM tests, Android production/test-source compilation and APK assembly. The post-NW125 audit fixes a Python syntax error in this phase's repository gate; both gate modes now run in the 52-check current verifier. See [current verification](../TEST_REPORT.md) and [active next roadmap](PRODUCT_QUALITY_ROADMAP.md). Installed-phone, full device/restore and release acceptance remain open. The 27 October statements below are preserved as the original handoff record.

**Date:** 27 October 2026
**Repository:** `Nahian2002/NightWave`
**Canonical cumulative source:** [`release/nightwave-nw125-final-candidate`](https://github.com/Nahian2002/NightWave/tree/release/nightwave-nw125-final-candidate)
**Base:** NW124 `a21a50c7c4d8039b1950ae226f791cf2173be5c3`. This includes all earlier NW111–NW124 work.
**Gate:** **SOURCE CANDIDATE PREPARED — APK NOT BUILT ON NW125 — DEVICE QA NOT RUN — PUBLIC RELEASE NO-GO.**

## What is finished and what isn't

- Current codebase and full folder tree audited at NW124 parent: 1,090 tracked files, including 516 `app/src` files, 261 docs and 283 tools. Recursive GitHub tree was not truncated. **No tracked APK, ZIP, keystore, generated build folder, diagnostic LOG or ignored credential filename** matched the inventory. Filename scans cannot prove whole history/contents are secret-free.
- All recent feature implementations are preserved. No code in `PlaybackService`, Google OAuth/Drive, download workers, Room migrations, signing material, package ID, or user library storage is deleted or reset by NW125.
- The current project is **not the default `main`**. `main` contains an older v0.8 snapshot. Use ONLY the linked cumulative NW125 branch for a final local build. Do not merge or change the repository's default branch based on unverified results.
- Former scripts, Room schemas, resource files, linked documentation and tests are intentionally kept because no dependency-proven redundant files were found. Never delete `tools/verify_*.py`, historical `tools/check-*.ps1`, old matrix entries or database schemas just because they look old.
- GitHub `current-android.yml` now includes an NW125 source/packaging gate; it still performs real JVM, Android compile, instrumentation-source compile, lint, APK signer/identity and evidence upload **if a hosted runner actually starts**. Previously hosted Actions were blocked by account billing; no CI pass is claimed here.
- **Latest executed end-to-end build evidence belongs to NW122**, not the final source. NW123 and NW124 added code and test declarations but have not yet been compiled together. No compiled final APK is attached to this PR.

## Build the actual private NightWave APK (Windows)

1. From the [NW125 branch](https://github.com/Nahian2002/NightWave/tree/release/nightwave-nw125-final-candidate), choose **Code → Download ZIP**; extract to a new writable folder outside the original OneDrive workspace, OR clone that exact branch. Keep all directories, not just `app/src`.
2. Open the extracted folder and run **`build-windows.cmd`**. This is the established zero-GitHub-billing builder. It installs/downloads Android tools as needed, compiles Kotlin/Compose, runs the Gradle JVM suite, compiles Android instrumentation sources, assembles debug, compares local APK signer where previous package evidence is available, and writes fresh status/fingerprint/test reports. The script refuses to promote a stale APK after failure.
3. If Python is installed, optional preflight: `python tools/nw125_release_candidate_gate.py --self-test` followed by `python tools/nw125_release_candidate_gate.py --json`. **PASS_SOURCE_ONLY is never a compiled APK**. The Windows builder itself is intentionally Python-free.
4. Confirm `dist/NightWave-build-status.txt` begins with fresh **SUCCESS** for this build. Check `dist/NightWave-jvm-test-proof.txt` and `dist/NightWave-source-fingerprint.txt`. Keep `dist/NightWave-debug.apk` only from this successful build; any `-previous.apk` is old.
5. Before installing over your existing app, connect your Samsung via USB debugging and run **`check-device-update.cmd`**. This read-only verification must say **PASS - IN-PLACE UPDATE SIGNER MATCH**. If INCONCLUSIVE or signer mismatch, **DO NOT install, uninstall, clear storage or regenerate signing credentials**. The installed app's data must be preserved.
6. Only when the fresh build and installed signer checks succeed, do the **single consolidated phone test**. Do not publish an APK or Play AAB based on a debug compilation alone.

Expected package ID: `com.nightwave.player`; intended current development identity: `2.0.0-alpha5s`, code `35`, compile/target Android 36, min Android 26. **Changing signing keys to get a build is not acceptable.**

## Phone test priorities (Samsung Galaxy S24 Ultra)

Use all **139 canonical cases** in [the matrix](NW90_EXPANDED_139_CASE_MATRIX.csv). The most failure-prone high-priority checks are:

1. **Data-preserving update:** signer match, update-in-place, same favorites/history/offline files/queue/Google connection, and restored UI settings.
2. **Playback correctness:** select one search song, no unrelated autoqueued tracks, Play Next, skip/back, shuffle/repeat, long-form crossfade and Bluetooth/headphone/lock-screen controls.
3. **Offline truth:** imported/eligible owned audio plays in Airplane Mode after force-stop; missing/corrupted audio shows honest recovery; download pause/resume/retry/queue are reflected accurately. Do not test unauthorized offline extraction.
4. **UX polish and accessibility:** Home, Search, Library, You, mini-player, full player and lyrics at 320/360/412dp equivalent and 1x/2x/3x type; observe overlap/latency, TalkBack queue and tab labels, seek sliders, and reduced motion.
5. **Personal library tools:** new NW124 `.txt` export/import/cancel/preview and 30-link cap, name collision (Shared) suffix, preservation of history; offline Commons source/license warning, Bangla search.
6. **Cloud:** account chooser/cancel, real signed-in Google Drive status, explicit disposable backup/readback/preview/restore, no silent overwrite, no token leakage. Device test account only; do not overwrite real music.
7. **Audio:** real EQ effect on speaker/wired/Bluetooth where supported, gapless/transitions, MediaSession, audio focus during call/unplug.
8. **Performance/stability:** cold/warm startup, search p95, scroll jank/battery, no fatal crash on background/restore, at least 30-minute real playback soak.

Capture separate short recordings of Home→Search→play→queue, Now Playing/lyrics, Library & playlist import/export, Sound Studio/EQ, and Google Drive (hide personal account details). Provide sanitized `dist/NightWave-build-status.txt`, `dist/NightWave-jvm-test-proof.txt`, `dist/NightWave-build-diagnostic.txt` only if build fails, and non-sensitive screenshots/videos. Avoid publishing private files or authentication tokens.

## Safety and release policy

Public distribution stays **NO-GO** pending independently established YouTube/other content source terms and recording rights, signed production build, privacy/security/history scans, Google consent-screen approval as needed, manifest/Android behavior on the actual device, final license and third-party notice review. An unofficial implementation cannot honestly claim Spotify's licensed catalog, backend sync or universal offline availability.

This document is a **prepared final candidate**, not proof the user-facing APK exists or passed. Record the exact NW125 commit SHA, package/SHA256 and signer verification in the resulting build report after the local build. Never fabricate device or Gradle outcomes.
