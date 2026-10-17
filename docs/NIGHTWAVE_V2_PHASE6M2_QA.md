# NightWave 6M2 — Reviewed cross-device playlist import

**Draft stacked on Phase 6M1.** The user's phone tests remain deferred to the combined verification checkpoint. This is not Spotify Connect or live library sync.

## New end-user workflow

1. On the sending phone, open a named playlist and use **Share playlist** (Phase 6M1).
2. On the receiving phone, pass the text to NightWave using Android's text share target. NightWave recognizes its own strict share format and shows an **Import playlist?** preview. It does not auto-play the embedded links or silently save anything.
3. Alternatively, open Library > **Import shared playlist**, paste that text, then review.
4. Only after confirmation does NightWave import one complete new playlist. Existing favorites, recent listening history, named playlists, offline files, queue, signed app identity, Drive backups and playback remain untouched.
5. Duplicate names create a new **(Shared)** or **(Shared N)** copy, without modifying the original. Importing exactly the same share twice makes no duplicate.

## Input and transaction protection

- Parse only the existing 6M1 NightWave share format, with exact footer, declared count, ordered numbered entries, optional standard omission notice and at most 12,000 text characters.
- Accept **1–30 unique canonical public YouTube watch URLs**; reject unknown domains, extra URL query parameters, file URIs, local offline IDs, invalid or truncated shares.
- Imported names and creator strings are only sender-supplied display hints. They are not proof of official artist metadata or playable streams.
- The pure LibraryMutationPlan validates all entries and destination-name constraints before mutating. LegacyLibraryTransaction writes a pre-change recovery snapshot and commits the entire imported playlist in a single preference transaction. No per-song partial commits.
- If the library already contains 50 playlists, fail without deleting anything. Do not create or alter a Drive snapshot.

## Authored automated checks

- **12** parser unit tests: existing formatter round trip, Windows newlines, truncation, count mismatch, duplicate/secret URLs, official 30-item notice, source preservation and list order.
- **10** transaction-plan unit tests: atomic creation, no-op repeated import, name conflicts and suffixes, invalid inputs, per-playlist limits, full-library rejection, unrelated library preservation.
- Python source-preservation gate: `python3 tools/verify_v2_phase6m2.py`.
- GitHub Android workflow configured for stacked 6M1-targeting PRs. A failed workflow with zero runner steps does **not** establish a Kotlin compiler failure; compile verification remains pending.

## Deferred manual phone QA

Try sending a playlist between Android apps, pasting the share, rejecting the preview, accepting it, re-importing the same text and importing a conflicting name. Inspect Playlists, favorites, private offline audio, current queue, saved progress and cloud settings afterward. Also verify compact screen, TalkBack, rotation, background playback and APK signer continuity. Do not uninstall/clear NightWave data as a workaround for signing mismatch.

## Explicit limits

This transfers **public video references and unverified labels**, not licensed/offline music, synced profiles, collaborative playlists or remote device control. Actual streams may not remain playable.
