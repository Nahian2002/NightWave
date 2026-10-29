# NW139 disposable recovery acceptance — NOT_RUN

This is a prepared procedure, not an executed cloud test. Source/build/interceptor tests cannot prove Google OAuth, real Drive contents or restoration on a phone. The agent has not authorized an app account, installed/reset the app, uploaded an app archive or changed the owner's library. Delivering the APK privately through Drive is separate from testing NightWave's appData backup.

## Required owner scope

Use an explicitly approved disposable Google account and a spare device or empty Android test user/profile. Keep the owner's existing NightWave installation/data and real account separate. The owner controls any manual in-place installation; first check the exact APK/source/signature in TEST_REPORT.md. Do not uninstall or reset an existing listener profile. No account password or token belongs in chat, Git, recordings or reports.

## Fixed synthetic fixture

In the isolated test profile, create a named local guest and two liked public references plus playlists named Fixture A and Collision. Use only test metadata and authorized/owned fixture audio. Keep original selected source URLs unchanged. Export the local metadata archive privately and record its hash/counts. Enable optional history, matched local LRC and playback preferences only through their explicit checkboxes for the relevant test. Public-video LRC and audio files are outside cloud backup scope.

## Required run

1. Connect the disposable account through native Google permission UI. Verify the saved account and exact access receipt. Cancel a subsequent picker and confirm prior backup/proof survives appropriately. Rotate during permission; a retained receipt continues safely. After process death without that receipt, restart the explicit action.
2. Back up the fixture. Confirm preparation then upload/readback phases. Independently read and decode the appData archive with the approved disposable scope; compare exact selected content, original IDs/URLs and absence of audio/account permissions/unselected optional data. A green message alone does not pass.
3. Add a distinct local song/playlist, preview an older archive and confirm counts/removals. Cancel once and byte-compare the unchanged local archive. For a confirmed replacement, retain the local recovery checkpoint and verify exact restored contents. Only the disposable profile may be replaced.
4. Compare/merge a colliding playlist. Existing local contents/order/profile remain; a separate Drive copy is added. Repeating the same merge adds nothing. Compare the resulting archive and original checkpoint.
5. While a preview is open, edit local favorites/playlists/profile; a stale replacement must reject. Change history/lyrics/preferences/network consent, disconnect/reconnect the same account, switch test accounts or create a newer access revision at the same timestamp; stale previews and old status/success must reject. Capture isolated runtime receipts where the real UI cannot force the timing deterministically.
6. Revoke permission and inject controlled 401/403 versus transient 429/503/network/storage failures. The old verified backup remains historical; no false success or unintended extra upload follows a failed comparison. A missing remote copy (404) can create a fresh copy; valid changed/corrupt content still uses verified copy-on-write. Keep all copies; do not delete remote data as cleanup.
7. Confirm optional preferences use the unchanged current profile and cannot overwrite a later edit. Matched local LRC only fills missing lyrics. Optional items remain separate, not a single cross-store transaction; partial optional failure must be reported honestly.
8. Review all themes, 320/360/412dp, 1x/2x/3x font and TalkBack/Switch Access. One contextual primary, meaningful busy phase, export/preview/merge/options, connection details and separate account/disconnect confirmations stay reachable. Successful disconnect stops scheduling without deleting local or Drive data; failed saved disconnect must not claim success.

## Evidence to retain privately

Exact source/APK/signer and installed build, Android/device/output profile, timestamps, synthetic fixture definitions, pre/post archive hashes and counts, approved account alias (no public email), operation/consent/access revisions, failure injection details and task outcomes. Record each supplemental/original case as PASS/FAIL only after execution, preserving the original NOT_RUN history. No final 9.5 rating or live recovery claim before these receipts exist.
