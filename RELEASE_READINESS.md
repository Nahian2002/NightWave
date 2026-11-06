# Release and acceptance status

[Version 55](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55) is the current GitHub release and signed stable update. [Version 58](https://github.com/Nahian2002/NightWave/pull/195) is the current S24-tested review build. Its exact APK is staged in a private GitHub draft. Live discovery from Audius, PeerTube, Radio Browser and Openverse and local FLAC import/playback passed on the S24.

| Area | Remaining evidence |
| --- | --- |
| Android instrumentation | Execute the applicable suites in an isolated test environment |
| Devices and accessibility | Smaller screens, older Android, large text and screen-reader journeys |
| Playback and offline | Longer sessions, route changes, transfer failures and offline playback integrity |
| Recovery | Disposable backup, restore and account-switch scenarios |
| In-app update | Full download and installer handoff on an older compatible phone |

The [test report](TEST_REPORT.md) separates host checks from observed device results. [Previous readiness report](https://github.com/Nahian2002/NightWave/blob/28d9d9861650363853584c3afa93c4cd30d4b3bf/RELEASE_READINESS.md)
