# NightWave Phase 6R2 — Cloud-to-Phone Audio Restoration

**Branch:** feature/nightwave-v2-phase6r2-verified-cloud-downloads
**Parent:** 6R1 cloud upload branch; original main and installed user files untouched.

## Implemented interaction
1. From NightWave's Offline Library, open **My Google Drive music** and use **Refresh my uploaded songs**. Only files in the app-created NightWave Music folder and visible through the limited drive.file permission appear.
2. Tap an uploaded song. **Download to NightWave?** displays the filename, size and explicitly asks for confirmation. Songs without both saved SHA-256 and MD5 metadata cannot be downloaded by NightWave.
3. The app gets a fresh Google authorization for the specific connected account. The client re-lists the folder, validates that the selected metadata is unchanged, then requests metadata by ID and demands the same MIME, size, SHA-256, MD5, source NightWave ID and parent folder, along with the download capability.
4. Only then does the client GET file bytes using Google Drive files.get with alt=media. A bounded stream writes to a newly created app-private UUID.pending file in locker_staging; it checks the advertised length if present, ensures never exceeding the expected size or 250 MiB, and syncs file bytes to disk.
5. The staging file is accepted only if its exact byte count, SHA-256 and MD5 match the previously verified Drive metadata. Incomplete or corrupt transfers are deleted and do not enter Offline Library.
6. OfflineAudioLibrary.importVerifiedCloud independently checks that the source is under the correct canonical app-private staging directory, repeats length/checksums, then examines existing private audio of the same MIME and length for exact SHA-256 duplicates. Duplicates are reused without changing their IDs or lyrics.
7. When new audio must be added, the 400-song and 3 GiB library limits are enforced. The original app-created local song ID may be preserved if no local audio/index conflict exists, enabling existing private LRC to reattach after a **separate, explicitly confirmed** cloud metadata+lyrics restore. Otherwise a fresh UUID audio ID is chosen, preventing overwrites. The staged file is moved to private offline_audio and registered through the existing index commit. Failed commits clean up the new file. A finally block also removes unused staging files.

## Boundaries and limitations
- **This is not unrestricted streaming-media downloading.** Only user-owned or licensed audio already uploaded through the NightWave Google Drive locker is eligible.
- No remote Drive delete, patch, share, broad Google Drive scope, automatic download, auto-restore, YouTube MP3 conversion or NightWave server.
- The existing hidden appDataFolder backup uses drive.appdata and remains distinct. The new visible music locker uses drive.file and requires separately configured Google Cloud OAuth consent/signing.
- Transfers are foreground, in-process, one file per confirmation. Cancellation stops future buffered work; no durable resumable download checkpoint across app/process death is claimed. The app writes at most 32 KiB chunks, with a 250 MiB cap, and cleans unsuccessful temporary files.
- A successful audio import does **not** automatically restore cloud LRC data. If the same imported source ID was preserved, the existing 6Q3 optional-backup restore can reattach LRC after its explicit confirmation. When another song occupies that ID, NightWave creates a new ID and skips unsafe automatic lyric association. Library playback and existing lyrics remain untouched.
- The existing older offline file-import route is preserved. The new cloud import is additive and does not delete any prior song. As with the preexisting offline import, concurrent unrelated additions from other screens have not been verified under realistic Android device concurrency; avoid simultaneous imports until integration QA.
- A Wi-Fi/cellular transfer will use network resources and the user's Google Drive quota. Neither Drive OAuth acceptance nor bandwidth/large-file behavior has been tested in this turn.

## Verification
- 17 new pure JVM LockerDownloadPolicyTest cases **authored** for checksum gating, path/name/MIME validation, source-ID reuse and collision-safe fresh IDs. They were not executed by Gradle in this turn.
- New 6R2 source-preservation gate integrated into the stacked Android CI, plus the previous 6R1 checker updated to reflect private temporary cleanup and new UI.
- **No Android/Kotlin compilation, JVM test execution, live Drive download, phone installation, APK signing, or actual device playback verified.** Prior GitHub Actions failed before allocating a runner. Keep the stacked PR in draft.

## Next
**6R3 — durable transfer progress/retry recovery** for interrupted uploads and downloads, plus end-to-end app/account testing and better local storage state. Later, a user-visible optional LRC import/association workflow can leverage matched original audio ID without forcibly overwriting existing lyrics.
