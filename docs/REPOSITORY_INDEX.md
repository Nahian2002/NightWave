# Repository guide

| Task | Start here |
| --- | --- |
| Install or update NightWave | [Version 55 release](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55) |
| Find the matching source | [Version 55 tag](https://github.com/Nahian2002/NightWave/tree/nightwave-v55) |
| Review the latest changes | [Release notes](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55) and [device evidence](../TEST_REPORT.md) |
| Build or verify on Windows | [Build guide](BUILD_AND_RELEASE.md) and [tool guide](../tools/README.md) |
| Find Windows troubleshooting and acceptance launchers | [Windows tools](../tools/windows) |
| Check test results and coverage | [Test report](../TEST_REPORT.md) and [verification guide](VERIFICATION_MATRIX.md) |
| Implement the upcoming zero-billing v56–v58 multi-source track | [Master plan](MULTISOURCE_NW56_NW58_MASTER_PLAN.md) · [acceptance checks](quality/MULTISOURCE_NW56_NW58_ACCEPTANCE.md) · [S24 QA](MULTISOURCE_S24U_DEVICE_QA.md) · [state ledger](quality/MULTISOURCE_NW56_NW58_STATUS.json) |
| Follow product work | [Current roadmap](https://github.com/Nahian2002/NightWave/blob/af4b67228fbb570b9679f10da82be5da2f8601d3/docs/PRODUCT_QUALITY_ROADMAP.md) |
| Report a problem | [Contribution guide](../CONTRIBUTING.md) or [private security reporting](../SECURITY.md) |

`app/src/main` contains production source and resources. JVM and device tests live in `app/src/test` and `app/src/androidTest`. `updates/stable.json` is the signed update status on the default branch. Keep Room schemas, migrations, signing checks and acceptance matrices with the app.

## Historical snapshots

- [Earlier README](https://github.com/Nahian2002/NightWave/blob/7667b68535bbdbc09b69350357ba2ca408e0730b/docs/README_HISTORY_20261027.md)
- [Legacy phase notes](https://github.com/Nahian2002/NightWave/blob/7667b68535bbdbc09b69350357ba2ca408e0730b/docs/README_LEGACY_PHASE_NOTES.md)
- [Earlier build guide](https://github.com/Nahian2002/NightWave/blob/7667b68535bbdbc09b69350357ba2ca408e0730b/docs/BUILD_AND_RELEASE_HISTORY_20261027.md)
- [Earlier verification narrative](https://github.com/Nahian2002/NightWave/blob/7667b68535bbdbc09b69350357ba2ca408e0730b/docs/VERIFICATION_MATRIX_HISTORY_20261028.md)
- [Previous repository index](https://github.com/Nahian2002/NightWave/blob/7667b68535bbdbc09b69350357ba2ca408e0730b/docs/REPOSITORY_INDEX.md)
