# Repository guide

| Task | Start here |
| --- | --- |
| Install or update NightWave | [Public release](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v51) |
| Find the matching source | [Version 51 tag](https://github.com/Nahian2002/NightWave/tree/nightwave-v51) |
| Review the latest changes | [Version 52 review](https://github.com/Nahian2002/NightWave/pull/183) and [device evidence](https://github.com/Nahian2002/NightWave/blob/28d9d9861650363853584c3afa93c4cd30d4b3bf/docs/NW155_MIX_PREVIEW.md) |
| Build or verify on Windows | [Build guide](BUILD_AND_RELEASE.md) and [tool guide](../tools/README.md) |
| Check test results and coverage | [Test report](../TEST_REPORT.md) and [verification guide](VERIFICATION_MATRIX.md) |
| Follow product work | [Current roadmap](https://github.com/Nahian2002/NightWave/blob/28d9d9861650363853584c3afa93c4cd30d4b3bf/docs/PRODUCT_QUALITY_ROADMAP.md) |
| Report a problem | [Contribution guide](../CONTRIBUTING.md) or [private security reporting](../SECURITY.md) |

`app/src/main` contains production source and resources. JVM and device tests live in `app/src/test` and `app/src/androidTest`. `updates/stable.json` is the signed update status on the default branch. Keep Room schemas, migrations, signing checks and acceptance matrices with the app.

## Historical snapshots

- [Earlier README](https://github.com/Nahian2002/NightWave/blob/7667b68535bbdbc09b69350357ba2ca408e0730b/docs/README_HISTORY_20261027.md)
- [Legacy phase notes](https://github.com/Nahian2002/NightWave/blob/7667b68535bbdbc09b69350357ba2ca408e0730b/docs/README_LEGACY_PHASE_NOTES.md)
- [Earlier build guide](https://github.com/Nahian2002/NightWave/blob/7667b68535bbdbc09b69350357ba2ca408e0730b/docs/BUILD_AND_RELEASE_HISTORY_20261027.md)
- [Earlier verification narrative](https://github.com/Nahian2002/NightWave/blob/7667b68535bbdbc09b69350357ba2ca408e0730b/docs/VERIFICATION_MATRIX_HISTORY_20261028.md)
- [Previous repository index](https://github.com/Nahian2002/NightWave/blob/7667b68535bbdbc09b69350357ba2ca408e0730b/docs/REPOSITORY_INDEX.md)
