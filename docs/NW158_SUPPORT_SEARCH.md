# NW158 Support email and Search shortcuts

Add a contact row in You → App & support. Use the approved maintainer inbox from SECURITY.md and open a reviewable email draft with a short issue template. Keep address copying available and keep logs, profile data and search data out of the draft.

Put the online submit action beside the search mode controls and surface a few matching recent searches while typing. Keep suggestions local until a user chooses one. Preserve the existing provider routes, query history and blank-entry behavior.

Validate the Windows build, source checks, lint and APK identity. On the S24, if available, verify the support and search journeys without sending email or changing accounts.

## Version 55

Email support shows the maintainer address and provides a copy fallback. Android opens an email draft containing the recipient, subject and blank problem template. NightWave attaches no diagnostic, account, library or search data. Search keeps Find music and YouTube above library suggestions. It offers up to three matching recent searches from local history while the field is focused. Typing only updates on-device matches.

- Build `NW-debug-3E22936727E15A80`, version `2.0.0-alpha5s` (55), package `com.nightwave.player`.
- Source revision `c569de64cdd618fb6e5593e9b6bc778903a7588e`, canonical source SHA-256 `3E22936727E15A804CAB03C6469DE0A7BEE4FAB41B7397FCD9F57D1FEE989D1E`.
- APK SHA-256 `DD0EE037E8949C7325323DE3D2BA797F50703937E5B73494F92CD7065BEFCEDE`, 28,182,584 bytes. Owner-only Drive permissions and complete byte/hash readback verified.
- Signer SHA-256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`, matching version 53 pulled from the S24.
- Windows build, 1,870 JVM tests and 94 current source checks passed. The Search Android test fixture compiled. Sequential lint reported zero errors, 317 warnings and 17 hints. The one new finding suggests using the KTX URI conversion helper.
- The S24 Ultra upgrade from v53 preserved all 33 backed-up files before launch. The device returned the exact v55 APK. The grouped You screen and the compact and expanded App details states were observed. Gmail opened the correct recipient, subject and template. The test draft was discarded without sending.
- On the S24, Find music and YouTube were visible with the keyboard open. Typing left history untouched. An explicit test search produced 20 online results, and tapping a recent completion filled the complete query and released input focus. The temporary entry was removed through the app and the original history file matched byte for byte. The phone returned to Home with playback paused and no fatal log entry for the current process.
- The public updater still serves version 51. Provider ranking and routing were outside this change.
