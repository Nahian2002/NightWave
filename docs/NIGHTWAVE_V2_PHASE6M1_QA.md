# NightWave 6M1 — Safe playlist sharing

Stacked on 6L; no main merge. This is user-initiated playlist sharing, NOT automatic cross-device sync or Spotify Connect.

## Implemented
- Library > named playlist > Share playlist opens Android Sharesheet by explicit tap.
- Only canonical public watch URLs with bounded sanitized title/creator metadata, in saved order.
- Up to 30 unique eligible links. No imported offline audio, local paths, tokens, private profile, listening memory or Drive credentials.
- Existing Media3 service, saved playlists, Google Drive backup/compare schema and app/signing identity unchanged.
- 10 JUnit tests authored and source-gate workflow configured.

## Later phone QA
- Tap Share in a normal playlist, preview text and verify no automatic send.
- Verify that Liked Songs and offline-only playlists do not expose sharing.
- Try short/narrow screen, rotation, TalkBack; check no regressions in queue, saving and playback.
- Automated Actions builds have failed with no runner/steps, so full Android compilation is still UNKNOWN.

## Boundaries
Text links are not live collaborative playlists or an importable Spotify-equivalent cloud account.
