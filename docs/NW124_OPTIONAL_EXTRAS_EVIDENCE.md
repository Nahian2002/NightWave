# NW124 — Useful optional extras: document playlists, honest offline discovery, sleep timer

Date: 27 October 2026
Protected parent: NW123 commit `5cf65294622f930401d3f50981bdbb38a02bef0a`
Branch: `update/nightwave-nw124-useful-optional-extras`
Status: **SOURCE_IMPLEMENTED_UNVERIFIED — RELEASE NO-GO**

## Product changes

### Playlist text export/import
- Library now offers **Export playlist .txt** for a selected nonempty named playlist. Android's system document picker chooses the destination; NightWave does not request broad storage permissions.
- A dedicated Library overview action offers an explicit choice to paste text or select a `.txt` document. Both enter the existing **preview + confirm** import path. No file selection directly mutates the user's library; existing songs, favorites and history are preserved. Name conflicts use the previously tested `(Shared)` copy convention.
- The file is exactly the existing NightWave share format, **at most 30 canonical public links**. Local music files, signed media URLs, artwork, OAuth identity, account secrets and backups are never included. This is playlist link transfer, **not offline audio export**.
- The new `PlaylistDocumentPolicy` round-trips through the current strict parser; document input is bounded at 48,000 bytes, decoded as strict UTF-8, rejects corrupt/truncated/unexpected entries and fails closed on repeatedly zero-byte reads. Filenames allow Unicode letters/marks (including Bangla) but remove traversal punctuation and path separators. Export bytes are held only in memory across Android's document picker.
- The original Share playlist action, paste import, 30-link limit, library mutation transaction, and no-automatic-overwrite behavior are retained. Cancellation and picker/provider failure do not change NightWave's library; a failed document write may leave an incomplete external file.

### Offline catalog review
- The existing eligible Wikimedia Commons results (no new source/permission claims) are stably sorted by **whole title words**; ties keep provider order and all candidates/URLs remain present.
- Every result shows literal word overlap or an explicit different-title warning, **never** claims the recording is the same as a YouTube search result.
- Search requests, license filters, user confirmation to download, audio integrity, playback, and all YouTube restrictions are unchanged. This is review assistance, not new YouTube offline permission.

### Sleep timer
- The existing 1–120 minute dial now exposes a TalkBack SetProgress action and a 1–120 range while retaining the original drag and tap interaction. Exact input still permits 1–1440 minutes.
- Preset targets and Cancel active timer use minimum 48dp hit areas. When a timer already runs, the action explicitly says **Replace timer**.
- The monotonic scheduling, sole playback-service ownership, cancellation and pause behavior are unchanged.

## Test/source evidence (not execution proof)
- New pure JVM test declarations: **13** (6 offline discovery + 7 playlist documents).
- New Compose Android instrumentation test declarations: **2** (accessible sleep-dial progress action and active timer copy).
- Existing Kotlin tests and application tests remain in place; repository diff is focused on listed UI, pure-policy and tests only.
- **NOT EXECUTED for NW124:** Windows Gradle build, JVM suite, Android compilation/test execution, APK assembly/signer, instrumented TalkBack interaction, Google restore, 30-minute phone soak, offline playback, 139-case acceptance.
- NW122's 1,464 passing JVM tests and NW123's unexecuted test declarations are historical baselines, not NW124 evidence. GitHub Actions may remain blocked by billing/spending limits.

## Final consolidated Samsung QA
1. Open a named playlist with public song links; export `.txt` through Android Files picker; inspect the file for only public URLs, no local file paths or account information; verify 30-entry cap/omission note.
2. From Library overview choose **Import playlist** and import a saved `.txt`. Confirm preview, name-conflict copy, ordered entries and preservation of favorites/history/other playlists. Cancel import and retry malformed/oversized text; library should not change.
3. Search owned/licensed Commons recordings; verify title-word sorting, unrelated candidates still visible, warning about different recordings, source/license review and no automatic download. Test Bangla and unrelated terms.
4. Open the sleep timer with TalkBack, adjust dial via accessibility progress, use 15/30/45/60 presets, set 1440 via exact input, replace an active timer, cancel, test background pause and UI at 2x/3x font.
5. Compare installed signing certificate, test playback/Drive/offline-data preservation and the whole 139-case matrix for NW125.

## Release decision
No permissions, package identity, signing, database/schema, playback service, cloud, downloads, or app data are intentionally changed. Public YouTube music-source rights, live Google acceptance and device tests remain independent blockers. **NO-GO** until NW125 and all required external approvals have real evidence.

Next: **NW125 — final certification and safe-release reconciliation.**
