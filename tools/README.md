# NightWave tools

| Task | Command |
| --- | --- |
| Windows build and JVM tests | `build-windows.cmd` |
| Check source contracts | `python tools/verify_current.py` |
| Compare APK and installed app | `check-device-update.cmd` |
| Generate a redacted build report | `send-build-report.cmd` |

Run commands from the repository root in the checkout you intend to build. The [build guide](../docs/BUILD_AND_RELEASE.md) explains APK verification. The [version 52 evidence](https://github.com/Nahian2002/NightWave/blob/28d9d9861650363853584c3afa93c4cd30d4b3bf/docs/NW155_MIX_PREVIEW.md) records the current review results. The maintained runner chooses current checks. Older scripts and reports remain for reproducing historical builds.

[Previous tool catalog](https://github.com/Nahian2002/NightWave/blob/7667b68535bbdbc09b69350357ba2ca408e0730b/tools/README.md)
