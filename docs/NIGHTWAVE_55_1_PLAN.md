# NightWave 55.1 fresh-install and update plan

- Use display version 55.1 and Android code 59 so existing installs on codes 55 and 58 can both receive the candidate once approved for the stable feed.
- Reproduce the fresh Home and manual update behavior on the S24 Ultra. Show all six existing mood searches as a responsive browse grid for an empty profile and shorten the welcome copy. Preserve personal Home sections, explicit searches and existing playback.
- Capture the Google Drive authorization outcome without selecting an account or granting consent. Separate an app defect from a Google-side configuration or account issue and verify what can be tested safely.
- Verify the exact signed APK and updater compatibility. Keep public release gates and the signed stable feed separate from source and device checks.

## Verified candidate

- Display version 55.1, Android build 59, package com.nightwave.player, minimum Android API 23. Exact source revision `244d0cc21fe6e277b996dd3ba90c6915b858acc0` and embedded build `NW-debug-22BF25D776BADF8A`.
- APK SHA-256 `F4290F4AE21E99134228CF0026CA7337D25219F357D8239271DE622F119CC321`, 28,294,586 bytes. Signer SHA-256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`.
- Windows builder passed 1,902 JVM tests in 241 suites and compiled the Android regression tests. The 94 current source checks passed. Sequential lint passed with 0 errors, 320 warnings and 17 hints. The added style warning suggests the preferences extension where the synchronous commit result is checked.
- On the S24 Ultra, the signed in-place update from Android build 58 to 59 preserved all 12 backed-up files before first launch. The empty profile rendered all six mood cards and the last card opened its existing Search query. The pre-update manual check correctly reported no newer build while the signed public feed offered code 55.
- The first-time Google Drive connection succeeded on the S24 and the app showed Drive connected with the first backup pending. Account selection and backup consent remained with the owner.
- The [GitHub release draft](https://github.com/Nahian2002/NightWave/releases/tag/untagged-dce7f7153cfe90e33af9) passed full APK byte and SHA-256 readback. The staged code 59 feed verifies against the installed public key. The canonical signed feed still offers code 55.
- Public release remains gated by the [source-rights and acceptance review](quality/MULTISOURCE_NW56_NW58_ACCEPTANCE.md). The current audit still flags independent YouTube audio playback and offline copies. An older client's public offer, download and installer flow remain pending live activation.

[Source review PR](https://github.com/Nahian2002/NightWave/pull/195)
