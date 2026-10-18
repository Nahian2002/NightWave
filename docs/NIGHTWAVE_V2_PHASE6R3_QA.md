# NightWave Phase 6R3 — Recoverable Google Drive Downloads

## Implemented
This first bounded Transfer Recovery milestone adds restart-safe **downloads** of authorized audio already stored in the user's app-created NightWave Music folder. NightWave's stable main, JSON cloud backups, local offline tracks, and already imported LRC remain untouched.

- When the user explicitly downloads a cloud audio file, NightWave checks the current metadata, matching Drive folder parent, canDownload capability, expected size, SHA-256 app property and server MD5; it will not trust only stale cloud-list metadata.
- A private AtomicFile JSON record stores the chosen Google account, remote file identity, expected checksums, original NightWave audio ID (if present), random temporary filename and last durable byte count. No OAuth access/refresh token, session URL, cookies, passwords, or remote media bytes are saved in the record.
- Download chunks stream into a private .pending file. The actual file bytes are fsynced before recording each approximately 1 MiB checkpoint. An app crash can leave additional uncheckpointed bytes, which are **truncated** on resume. We never use an unsupported local byte count to request Range.
- After restart or an interrupted transfer, users see **Resume interrupted download**. This asks for confirmation and obtains fresh Google authorization. It then checks the remote file again and uses GET alt=media with Range: bytes=<committed>- . A 206 response must have the correct Content-Range start, end, total and compatible Content-Length. An unexpected 200 response to a resumed request is rejected rather than appended.
- Temporary IO errors, EOF and Drive 408/429/5xx errors get a maximum of **three consecutive attempts** per active transfer segment, with a short delay. Authentication, permission, changed metadata, bad range or checksum failures are **not** silently retried as valid downloads.
- A complete staged file still requires exact length, SHA-256 and MD5 matching before it can be handed to the 6R2 offline import. The importer independently repeats its checksum verification; duplicate audio keeps its old ID and files, while a newly downloaded song can retain its original ID if unoccupied.
- A successful import discards its checkpoint. If import fails before consuming the staged file, the verified data/checkpoint remain for an explicit retry. Checksum corruption discards its pending copy. A failed index commit removes the unindexed new offline file and cannot replace previous songs.
- **Discard incomplete download** deletes only the locally identified pending file and recovery record, never user-uploaded Drive files or existing saved songs. Corrupt recovery records are fail-closed and can be explicitly discarded; unidentified orphan pending files are not automatically removed.

## Deliberately not implemented
- Durable **upload** session persistence across process death. Saving a sensitive Google resumable upload URL requires a separate protected session-storage design, so uploads still have the 6R1 in-process behavior. Future 6R3B can add same-session upload retries and protected persisted upload sessions.
- Unattended background transfers, automatic synchronization, Google-to-Google backups, server accounts or YouTube-to-MP3 extraction.
- LRC reattachment happens only through the existing separate consent-driven recovery when the exact private song ID is available; never overwrite already imported lyrics.

## Verification
20 additional pure JVM tests were authored for valid/invalid ranges, resumed full responses, response lengths, content bounds, progress and retry policy. Dedicated source regression check is added to the Android workflow, and the 6R2 source checker is updated for the new explicit download method.
These checks do **not** establish a compilable Android APK, successful Google OAuth consent, real Drive range support on the test account, signer continuity or installed-app compatibility. GitHub Actions previously failed with zero steps because no runner was allocated. Keep the PR draft until a real Gradle run and controlled cloud test can occur.

Reference: Google Drive supports files.get alt=media and byte-range download headers; these are verified against documentation but not live-tested here.
