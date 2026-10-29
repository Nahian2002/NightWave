# NW139 optional recovery implementation plan

Protected parent `4520a961497a438a76a1904ed52d4ce0d2337202`, cumulative NW138 #160. Private checkpoint `20261030-nw139-recovery` retains the baseline bundle and exact working bytes. No active overlapping batch or uncommitted work was present at admission.

1. Compact the account hub around one contextual primary recovery action and concise metadata/audio scope. Preserve local export, all recovery/compare/options/help and explicit disconnect/account confirmation. Show meaningful local operation phases instead of an undifferentiated busy label; safe informational help remains reachable.
2. Bind preview/restore/merge/optional application to an ephemeral exact consent and access-revision receipt, including same-timestamp newer proof. Atomically recheck that receipt while committing local recovery; preserve existing library checkpoint/local-edit checks and original data formats. No network work belongs inside a local commit guard.
3. Fence stale access/backup success/failure receipts in interactive and worker flows using the existing access revision, preserving previous verified copies on cancellation/transient failure. Existing OAuth scopes, account choices, Drive endpoints/HTTP client, archive formats and source quality remain.
4. Add deterministic receipt/phase/presentation tests, real client in-memory roundtrip/merge regressions, isolated preference/Compose runtime test sources and negative caller checks. Run current source checks, full Windows builder and then sequential lint; verify signer/source/APK, preservation, private recovery and owner-only Drive bytes/hash. Commit a cumulative draft PR and next-batch ledger.
5. Prepare an explicit disposable-account live procedure and phone cases, all NOT_RUN until owner-scoped execution. Do not use the owner's live account for app backup/restore or auto-install/reset/merge/publish binaries/change Actions. Build success cannot prove a live restore, OAuth or final rating. NW140 follows locally verified source work; final acceptance still needs actual evidence.

Budget admission includes all implementation, repairs, validation, review and private delivery; recheck both shared limits before expensive phases and at least every ten minutes.

## Implemented behavior

The account hub starts with a concise local profile, exact backup/access status and one contextual primary action. Export, preview, compare/merge, account change and confirmed disconnect remain. Backup options have an explicit disclosure; informational connection help stays reachable during busy work. Metadata versus audio and matched-local versus public-video LRC scope are explicit. Current work names permission, account verification, preparation, checking/uploading, listing, opening, merge, restore or optional application; an unchanged remote copy does not falsely claim a new upload.

Recovery authority is an in-memory exact consent plus access-revision receipt. A newer proof at the same timestamp invalidates an older preview. Foreground picker/merge/restore/optional callbacks check it; local commits hold the shared cloud guard before existing library/profile/lyrics locks and retain the original exact-local-byte archive transaction. No network work or suspension runs inside that guard. Existing callers release local locks before reading cloud state; no inverse cloud lock order was found. The optional profile commit uses a shared-instance compare-and-save guard before lyric additions, preventing a newer local edit from being overwritten. Optional profile and lyric stores still are separate commits; partial failure is reported rather than represented as a cross-store transaction.

The original cloud/profile key sets, archive/lyric codecs, source URLs/quality and OAuth scopes/endpoints remain. New checked/failed/status/backup receipts use access-revision comparisons. A pre-picker receipt survives configuration change in a ViewModel; it contains no token or Drive ID and is not saved to disk. Process death without that receipt safely requires restarting the explicit permission action. Cancellation keeps matching prior access proof rather than recording a fabricated failure.

## Regression found and repaired

The real Drive client's in-memory regression exposed an unintended second upload: a 503 while comparing the latest remote copy was swallowed as if content had changed. Permission/rate/transport failures now stop before another upload. A missing remote copy (404) can still create a fresh copy; changed or corrupt returned content retains the original verified copy-on-write behavior. No remote file is patched or deleted. The new 401/403/429/503 cases check the actual client, not a duplicate implementation. The first failing assertion and raw reports are preserved privately; they are not counted as final passes.

An old NW59 caller marker initially expected a consent-only argument. It now follows the stronger consent/access receipt while retaining the historical policy/smoke checks and negative fixtures. A later review refined progress copy to cover unchanged-copy readback and ordered the optional profile CAS before lyric additions. Final checks use the final refined source only.

## Live acceptance still required

[Ten supplemental cases](quality/NW139_RECOVERY_ACCEPTANCE.csv) and the original/prior cases remain NOT_RUN. [The disposable live procedure](NW139_RECOVERY_LIVE_PROCEDURE.md) requires an owner-approved disposable Google account and an isolated empty Android test profile/device; no actual account authorization, app archive upload/restore, installation, reset or listener-data change was performed. APK delivery through the owner's private Drive is separate from testing the app's own appData backup. Source/JVM/compilation evidence cannot certify live recovery, rendered accessibility, performance or a final 9.5 rating.

## Current local verification

Final built implementation `c212789ff8935ad4e2de626732e74b9e71c34f63` follows committed plan `ba3b9ce25ed63f8e13b54130721ee708840757a9` on parent `4520a961497a438a76a1904ed52d4ce0d2337202`.

- Full Windows builder PASS: Android/Compose/APK and instrumentation-source compilation; **1,803 JVM tests / 222 suites**, zero failures/errors/skips. Ten new methods: seven receipt/phase policies and three actual-client merge/failure regressions.
- **79 current source checks PASS_SOURCE_ONLY**, including negative caller fixtures and updated historical stronger-receipt markers.
- **925 prior canonical inputs byte-identical**, ten intended existing edits and five additions; **940 final inputs**. Account/Activity callback inventories and cloud/profile key sets retained. Original archive/lyric codecs, exact-local-byte library transactions, audio/source quality, themes, package/dependencies/signing and OAuth scopes preserved. Drive HTTP options and upload/readback method are unchanged apart from line endings; only comparison-error admission changes.
- Implementation-stage all-ref Gitleaks **3,565 commits**, no leaks; reviewed exclusions unchanged. Original unchanged Library compiler condition warning remains triaged.
- Six isolated preference/profile runtime methods and four actual account-hub Compose methods compile only, **NOT_RUN**. All ten supplemental/original/prior phone/account/restore/data/performance/accessibility/release gates remain open.
- Build **NW-debug-238C337491E75208**; canonical source **238C337491E75208F04909F0F80990F876CCED743CAA5E4EC0628D469BEB31E7**. APK **D71A2B4A3C61763473C4D129388276F0E984B45BF67285D032E2A569F894839F**, **33,844,842 bytes**. Signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**, matching the actual NW138 APK. Package/version `com.nightwave.player` / `2.0.0-alpha5s` / code35 unchanged.

The private comparison helper initially compared CRLF baseline source to normalized LF text when checking unchanged HTTP/upload sections. Normalizing line endings only in that evidence comparison confirmed the same methods; exact canonical byte comparisons remain unchanged. This was an evidence-helper issue, not a product test result.

Sequential lint PASS: **0 errors / 322 warnings / 17 hints**, unchanged ID/severity/message/location inventory after ignoring line movement. No suppression/baseline added. Private owner-only APK permission and streamed bytes/SHA readback verified; exact working-byte recovery archive and Git bundle retained. NW140 is next; live recovery acceptance is still NOT_RUN.

Cumulative [draft PR #161](https://github.com/Nahian2002/NightWave/pull/161) targets NW138 #160; no merge is performed.
