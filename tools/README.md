# NightWave tools

| Task | Command |
| --- | --- |
| Windows build and JVM tests | `build-windows.cmd` |
| Check source contracts | `python tools/verify_current.py` |
| Compare APK and installed app | `check-device-update.cmd` |
| Generate a redacted build report | [`tools\windows\send-build-report.cmd`](windows/send-build-report.cmd) |
| Diagnose a local build | [`tools\windows\diagnose-nightwave-build.cmd`](windows/diagnose-nightwave-build.cmd) |
| Capture a private crash summary | [`tools\windows\capture-nightwave-crash.cmd`](windows/capture-nightwave-crash.cmd) |
| Inspect Google OAuth build setup | [`tools\windows\check-google-oauth.cmd`](windows/check-google-oauth.cmd) |
| Run the release evidence check | [`tools\windows\check-public-release.cmd`](windows/check-public-release.cmd) |

Paths are relative to the repository root. The Windows launchers also work when opened directly from File Explorer. The [build guide](../docs/BUILD_AND_RELEASE.md) explains APK verification, and the [test report](../TEST_REPORT.md) records release coverage.

Use `python tools/verify_current.py` for current source checks. The [NW90](windows/run-nw90-final.cmd) and [NW91](windows/run-nw91-final.cmd) launchers are kept with the Windows tools for historical acceptance work. Older notes may still show their original root paths.

[Previous tool catalog](https://github.com/Nahian2002/NightWave/blob/7667b68535bbdbc09b69350357ba2ca408e0730b/tools/README.md)
