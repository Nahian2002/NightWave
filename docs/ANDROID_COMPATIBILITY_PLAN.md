# Android compatibility target — owner request, 4 November 2026

Target to evaluate: Android6/API23 and newer, covering Android6/7 software on roughly 2016-era phones as well as modern devices. This is a planned target, not current support or a guarantee for every model. Existing version39 declares minimum Android8/API26 and remains unchanged. Preserve all features, original sources/quality, installed data, account/consent and package/signing boundaries; OS-specific implementations may differ while preserving the task.

## COMPAT01 — required before broader support claims

1. Inventory pinned dependencies, Android framework API calls, Java/NIO desugaring and packaged CPU/native requirements. Read-only initial scan found 95 dependency manifests declaring numeric minima, highest21; this is a partial manifest scan, not proof of every dependency/runtime requirement. Private receipt retained outside Git.
2. Fix framework calls requiring26+ with supported older-OS equivalents and version guards. Confirmed audit lead: `WeeklyUpdateActivity` directly calls `PackageManager.canRequestPackageInstalls()` and launches per-source install permission settings; API23–25 need the appropriate OS installation approval flow. Never bypass Android confirmation or add silent installation.
3. Keep existing NIO desugaring, but test file import/verification/moves and signing on older Android. Audit foreground work/notifications, Media3/audio focus, SAF, WebView/network TLS and optional Google services. No weakening TLS, consent, file/signer checks or private-data boundaries to force compatibility.
4. Only after fixes, lower the declared minimum on an isolated review branch; update minimum-SDK source/signing/device gates intentionally, run old/new API-sensitive lint plus full regression/build/signature checks. Keep target SDK modern. Do not merely alter minSdk and claim success.
5. Prepare emulator and approved physical profiles for API23/24/26, modern permission/storage/media boundaries and the current target. Separate emulator functional evidence from real low-memory/OEM background limits, codecs/audio routes, WebView, performance and battery evidence. No emulator/phone setup, installation or settings change is authorized automatically by this plan.

## Device coverage

Cover older low-memory 32-bit devices where the packaged dependencies support them, common 64-bit budget/midrange/flagship phones, compact/large screens, all themes/font scales and supported accessibility input. Include major OEM background-management differences. Google-dependent features need compatible available Google services; guest/local music must be tested independently. Exact memory/storage budgets, reference models and unsupported combinations must be determined from evidence rather than invented now.

NW145 remains pending actual device evidence. COMPAT01 is the next source batch under this expanded owner scope; do not mark its API23 target implemented yet. Keep code39 as the last verified binary; it will not install below API26.

Official references: [Android6 APIs and version guards](https://developer.android.com/about/versions/marshmallow/android-6.0), [Android7 behavior and features](https://developer.android.com/about/versions/nougat/android-7.0), [NIO and Java API desugaring](https://developer.android.com/studio/write/java11-nio-support-table). These explain mechanisms, not NightWave device acceptance.
