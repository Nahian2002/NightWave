# NightWave 6R3B — Reliable in-process Drive uploads

6R3B is stacked on 6R3. No changes to main or installed music.

User starts uploads explicitly from Offline Library > My Google Drive music. No automatic upload and no NightWave-owned server. A new bounded upload-resume policy interprets Google's 308 Range as accepted bytes, not sent bytes. On temporary network failure or Drive 408/429/5xx, NightWave sends a zero-byte PUT status request with Content-Range bytes */total to the current resumable session, then resends only after reconciling Google's accepted position. Up to three consecutive failures are retried; unexpected offsets, expired sessions and bad metadata fail closed.

Completion requires a real Drive 200/201 file response with matching folder, filename, audio MIME, byte length, source local ID, SHA-256 app property and Google-computed MD5. UI progress stays below 100 percent until that check. A later explicit retry first checks the Drive folder for an exact matching upload, reducing duplicate risk.

Security: Google session URLs are capability-bearing, so this phase does NOT persist them in files, account preferences, logs or checkpoints. Recovery is in-process only; after app termination the user must manually start a new upload. The prior 6R3 download checkpoints DO survive restart. No background transfers, new Drive permissions, remote deletes, MP3 stream extraction or account server.

Verification: 20 authored but unexecuted pure JVM policy tests and a new source checker. Actual Kotlin/Android compilation, unit test execution, Google OAuth, live resumable network transfer, signed APK and device QA are NOT verified. Previous GitHub Actions jobs failed before a runner was allocated. Keep draft.

Official API documentation: https://developers.google.com/workspace/drive/api/guides/manage-uploads
