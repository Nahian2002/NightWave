# NW108 — Google Drive Backup and Recovery Name Integrity

Date: 27 October 2026. Parent `2a941be683f353fb61b94aa141490abf8e7362c9`. Branch `feature/nightwave-nw108-cloud-playlist-identity`.

## Root cause

Playlist identity was not deterministic across devices: `LibraryBackupCodec.encode/decode` and `CloudLibraryMerger` used Kotlin `lowercase()`, which depends on `Locale.getDefault()`, while `CloudRecoveryPolicy` used `lowercase(Locale.ROOT)` while the actual local playlist mutation engine uses `equals(ignoreCase=true)`. Names such as `I` and `i` collide under English casing but not Turkish casing. A user could therefore have a different duplicate/conflict outcome when backing up, previewing, or merging from a second phone.

A standalone JVM reproduced the English/Turkish lowercasing difference. This is a real cross-locale archive identity defect, not an observed user's production data loss.

## Narrow fix

- `PlaylistNameIdentity` provides one locale-neutral `Locale.ROOT` identity rule and consistent duplicate/same-name checks. It retains the original display name bytes and never modifies saved playlists.
- `LibraryBackupCodec` now validates playlist uniqueness using that key for both encode and decode. An **ambiguous legacy archive** containing names that collide under the local-equivalent canonical policy is **rejected before any restore**, rather than silently flattening two playlists. The original Drive archive bytes and archive schema/version remain unchanged.
- `CloudLibraryMerger` uses the same comparison rule to detect conflicts, preserve identical imports, and allocate safe `(Drive)` names. The manual merge still keeps local collections and history intact.
- `CloudRecoveryPolicy` uses the same comparison rule for changed/lost playlist summaries prior to explicit restore confirmation.
- Add regression tests to existing backup codec, merge and restore preview JUnit classes: Turkish duplicate fail-closed, cross-locale repeat merge idempotence, and fail-closed ambiguous remote preview. These JSON/Android-adjacent tests require full Gradle/JUnit execution at final build.

## Independently executed verification

- Pure Kotlin production `PlaylistNameIdentity.kt` + `NW108PlaylistNameSmoke.kt`: **30/30 PASS** under five locale settings (English, Turkish, Azerbaijani, Lithuanian, Bangla), including 50-playlist collection limits and Unicode I-family collision handling consistent with local playlist validation.
- Same real policy + new original `PlaylistNameIdentityTest.kt`: **6/6 JUnit-style method execution PASS** using a temporary Java JUnit-compatible bridge, not the official Android Gradle JUnit runtime.
- CI source checker ensures the exact policy is wired to both sides of archive validation, merge and restore preview, and the existing regression suites remain in CI.
- No Drive network upload, actual Google account authorization, physical remote restore, Android compilation or application instrumentation has been verified by these checks.

## Compatibility and safety limitations

The metadata archive envelope version, OAuth app data scope, authorization flow, signed app ID and storage paths were deliberately unchanged. Cloud data and local user playlists are not altered by this code change. However, an older backup created under a locale-specific rule that contains playlist names newly considered duplicates will **fail validation** until separately reviewed, rather than risk silently overwriting a playlist. Do not delete that snapshot or attempt blind normalization.

GitHub Actions including a no-step runner canary have failed before runner start in recent waves. NW102's full 53-method policy suite, six public-queue methods, full Android build, signed APK continuity and final phone tests remain outstanding.

**Public release: NO-GO.** NW108 is not proof of zero bugs.
