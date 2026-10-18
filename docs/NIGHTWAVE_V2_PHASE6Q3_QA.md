# NightWave Phase 6Q3 — Optional Lyrics & Playback Preferences Backup

## Overview
The existing Drive v2 JSON envelope remains exactly the format used when both optional toggles are off. If one is enabled, this phase writes a strict, checksummed **format 3** envelope with the same embedded library and optional history. The decoder supports both format 2 and format 3, rejecting unexpected fields. The outer Google Drive snapshot name retains its historical \`nightwave-cloud-v2-...\` prefix to keep the existing private snapshot browser and restore picker compatible.

## Implemented
- Two independent **off-by-default** controls under You > Manage backups: Back up imported LRC lyrics and Back up playback preferences.
- Playback preferences use an explicit allowlist: motion preference, quality preset, crossfade seconds, lock-screen artwork visibility, and keep-screen-awake setting. Device identity, Google account, history privacy, credentials, and tokens are not serialized.
- LRC entries are user-imported timed lyrics only, keyed to a private app-generated local song identifier. Up to 20 entries / 512 KB total, each syntactically validated with the existing strict TimedLyricsCodec; no MP3 audio, source URI, SD-card path, signed media URL or music file is included. Backups with more than the supported count fail rather than silently drop lyrics.
- Both existing manual backup and WorkManager scheduled backup capture the two options. The worker rechecks option state before starting its Drive operation so a stale prepared payload cannot silently use a changed consent setting. The 6Q1 verified remote snapshot comparison uses the new decoded v3 fields to decide whether a new backup is necessary.
- Explicit cloud **library** recovery still uses the 6Q2 stale-preview transaction lock. Only after a successful library restore, when optional cloud data exists, a **second confirmation dialog** offers to apply optional preferences and any applicable missing LRC entries. Closing it keeps local preferences and LRC untouched.
- Optional lyrics restore is non-destructive and applies **only to existing local audio with the same exact private song ID**, and only when it has no lyrics. Typical separately imported audio on a second device will have a different ID and **cannot** automatically match. Optional preferences apply without changing account, history privacy, local profile ID, or Google tokens.
- The optional restore operation consists of preference save and a protected LRC preference commit, not a cross-store atomic transaction. If a storage write fails, one subset may already have been applied; the app says review local settings and does not claim full rollback. No existing local lyrics are deleted or overwritten.
- Existing data cap, cloud OAuth appDataFolder scope, no-remote-deletion policy, versioned Drive history, restore/merge and personal audio storage boundaries remain unchanged.

## Verification boundary
- 16 authored JVM cases in CloudOptionalDataTest; new source-only safety gate in Android CI. Test methods were **not executed**.
- No verified Gradle compile, OAuth/Drive live authorization, signed build or device regression in this phase. Prior CI runs failed before runner allocation; source gates cannot replace an actual build.
- No automated internet lyrics extraction, artist matching, cloud MP3 locker, full device-to-device audio restore, or general backup of app files is included. Those are separate future phases.
