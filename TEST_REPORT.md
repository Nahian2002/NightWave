# Current verification — NW137 guided owned lyrics

30 October 2026. `update/nightwave-nw137-owned-lyrics`, stacked on SEARCH02 #158. Protected parent `721ece36c9b9900cae4daa6936dd1caf77e8ac9c`; plan `5df404d6`; implementation `afbb1fe7`; final built correction `a94328169b2cddad4669e9937296a8e90e1b0969`. [Exact changes/evidence](docs/NW137_OWNED_LYRICS.md), [eight new phone cases](docs/quality/NW137_OWNED_LYRICS_ACCEPTANCE.csv), [hourly sequence and weekly reserve](docs/quality/UPCOMING_UPDATES.md), [previous report](https://github.com/Nahian2002/NightWave/blob/721ece36c9b9900cae4daa6936dd1caf77e8ac9c/TEST_REPORT.md). **NW138 is next**, then NW139–NW145.

Guided user-owned UTF-8 LRC import/review/edit/follow/timing/export now binds to an original local ID or an exact canonical public video. Local stores/cloud formats remain unchanged; public-reference lyrics use a bounded separate device-private namespace with no provider fetch or new cloud support. Source/epoch/fingerprint/lifecycle checks reject stale operations, and a pending picker/review survives configuration changes safely. Process death/in-flight cancellation may require reselecting; no silent unreviewed save.

| Evidence | Final result / limit |
| --- | --- |
| Windows builder | Android/Compose/APK and instrumentation-source compilation PASS |
| JVM | **1,784 tests / 220 suites**, zero failures/errors/skips; twelve new methods |
| Source checks | **77 PASS**, including caller-negative fixtures and adapted prior lyric guards |
| Lint | **0 errors / 322 warnings / 17 hints**; five added preference notices triaged without suppression |
| Notice rationale | Three ApplySharedPref and two UseKtx notices: writes run on IO and retain synchronous commit Boolean/durability for truthful results; existing Library compiler warning unchanged |
| Preservation | **905 prior inputs identical**, twenty intended existing edits, five additions; nine callback inventories retained |
| Original data/source | Local LRC/offset/parser/editor/export/cloud, audio/network/quality, package/dependencies/signing and theme tokens unchanged |
| Secrets | Implementation-stage all-ref Gitleaks **3,556 commits**, no leaks; reviewed exclusions unchanged |
| APK/recovery/delivery | Exact embedded canonical source/signer; private working-byte ZIP/bundles/receipts and owner-only streamed SHA match |
| Android/physical | Seven added runtime methods compile only; eight new supplement/original NW137/prior gates **NOT_RUN** |
| Hosted/release | Actions remains disabled; no hosted pass, install/reset, account operation, merge or public binary |

Build **NW-debug-7027DA65E2963716**; source **7027DA65E2963716F7F8CFC39E39BB69619D6CB2B957AB25231BF65FBE27ADFD**, **930 inputs**. APK **BB1144C07D7377CA4CF6E5F9EB0258356F7F87AB1326F1D6DCA931BB293F3E82**, **33,844,842 bytes**. Signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**. Package/version `com.nightwave.player` / `2.0.0-alpha5s` / code35 unchanged. No new rating/acoustic/provider-rights/runtime certification is inferred. Weekly allowance checks remain required before scheduled work and expensive phases; stop/delete the automation at at most 10% weekly remaining.
