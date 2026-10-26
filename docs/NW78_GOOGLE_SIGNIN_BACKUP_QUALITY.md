# NW78 — Google Sign-in & Verified Backup (development tranche 1)

**Roadmap authority:** [NW76–NW90 quality roadmap](CURRENT_CUMULATIVE_ROADMAP.md).  
**Source branch:** `feature/nightwave-nw78-google-backup-reliability`, cumulatively based on NW77.  
**Status:** Source hardening and regression cases committed. **The full NW78 user acceptance criteria have not yet been verified on a device.**

## Preserved NW78 behavior from existing cumulative code

- User reports Google account selection/connection succeeds after completing the external Android OAuth registration; this is user-observed, not an independent Google API audit.
- Narrow `drive.appdata` authorization, Drive token verification of Google account, separate connected vs backed-up stages, and explicit backup/recovery actions.
- Drive upload followed by byte-for-byte readback and backup decoder validation; unchanged snapshots are also downloaded and content-verified before being treated as already backed up.
- Offline library, playlists and songs remain local; selecting a cloud backup previews changes and requires confirmation. Consent revisions block a late worker and same-account reconnect history from becoming authoritative.
- No credentials are committed, no cloud file deletion or migration is performed.

## NW78 code changes in this tranche

1. In `CloudBackupState.success()` and `successIfCurrent()`, refuse timestamps <= 0; this ensures no invalid completion timestamp can set `enabled=true` or `lastVerifiedRevision`.
2. Add sandboxed Android instrumentation cases for zero/negative timestamps, switching authorized accounts after a successful snapshot, and reconnecting the same account while an older worker receipt exists.
3. Restore the user's original **NW76–NW90** phase order and quality-score benchmarks as the controlling roadmap. The earlier incorrectly summarized NW62–NW77 roadmap was replaced on this branch.

## Deferred final-device acceptance (not yet a passing result)

- Verify Google account connection remains successful on the exact signed APK.
- Create backup of disposable liked tracks and playlists; confirm positive verified timestamp only AFTER completed Drive readback.
- Preview a remote snapshot; compare and restore only disposable data; verify no unapproved overwrites.
- Switch to a second test Google account and back; verify previously reviewed snapshots and stale workers cannot cross identities.
- Disconnect/reconnect without data loss; review optional history/lyrics/preferences privacy choices, Wi-Fi restrictions and repeated backup behavior.
- Confirm errors show real recovery guidance without exposing emails/tokens in diagnostics.

## Evidence

**Exact GitHub source readback:** both guards, all three regression cases and all fifteen phases were found on the NW78 branch; braces of the edited regression file were balanced. This is a narrow textual consistency check, not a Kotlin compiler run.  
**Not executed:** Gradle, Android instrumentation, Google backup/upload/readback on device, signing compatibility, full long-session QA. The owner has chosen one comprehensive manual acceptance session at NW90.

No public-release claim is made. NW77's YouTube source-rights issues remain independently blocking.
