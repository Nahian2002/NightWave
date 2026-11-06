# Verification status

| Evidence | Result |
| --- | --- |
| Version 55 review | Windows build, 1,870 JVM tests, 94 source checks and lint passed. Android test compiled. The in-place S24 upgrade, App details, email draft and Search completion passed scoped checks. [Version 55 evidence](docs/NW158_SUPPORT_SEARCH.md) |
| Version 54 review | Windows build, 1,869 JVM tests, 94 source checks and lint passed. Android test compiled. Exact signed APK and owner-only Drive readback verified. App details and grouped You need a connected phone for the rendered checks. [Version 54 evidence](docs/NW157_APP_DETAILS_PLAN.md) |
| Version 52 review | Windows build, 1,869 JVM tests, 94 source checks and lint passed. Android instrumentation sources compiled. |
| S24 Ultra | Version 51 to 52 upgrade preserved 33 verified files before first launch. Mix preview, dismissal, show-all, options and manual update checks passed. |
| Public version 51 | Published APK hash and signed update metadata verified. |
| Broader device acceptance | Small screens, accessibility, playback and recovery scenarios remain open. |

The [version 52 device and build report](https://github.com/Nahian2002/NightWave/blob/28d9d9861650363853584c3afa93c4cd30d4b3bf/docs/NW155_MIX_PREVIEW.md) has exact artifacts, version identifiers and coverage. [Public release](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v51) · [Prior test report](https://github.com/Nahian2002/NightWave/blob/28d9d9861650363853584c3afa93c4cd30d4b3bf/TEST_REPORT.md)

## Repository maintenance

[PR #184](https://github.com/Nahian2002/NightWave/pull/184) refreshed the public default branch with documentation only. Four superseded snapshots are linked by commit from the repository guide. The audit reduced missing Markdown relative path targets from 75 to zero. All 59 default-branch source checks passed, and the public APK matched its previously verified hash. The application, build scripts, workflow controls and signed feed on main are byte-identical to the parent.

On this review branch, the standalone compact-layout checker now reads both language files as UTF-8. Its Windows code-page regression and all 94 current source checks passed with Python UTF-8 mode disabled. One unused string key and its paired translations were removed after checking callers. All 1,869 JVM tests and the Windows builder passed. The review rebuild was local. The public download and the earlier S24 evidence refer to their original APKs.

The tracked Python, PowerShell, XML, JSON and YAML files parsed on both branches. Compilation, source tests and lint cover the maintained paths. Separate runtime and historical scenarios still need their own evidence.

Review lint finished with zero errors, 316 warnings and 17 hints. The unused-string warning was removed and no new findings appeared. The local maintenance build was `NW-debug-FC9528CFAB7EAABC`. [Review PR #185](https://github.com/Nahian2002/NightWave/pull/185)
