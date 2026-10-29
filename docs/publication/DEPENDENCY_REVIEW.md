# Release dependency review — 28 October 2026

The app and dependencies are unchanged at NW130 parent `fb8f0455c6903249ab85299a76566153a2b8d2a7`. An offline Gradle `releaseRuntimeClasspath` report supplied 164 unique Maven coordinates, including platform/BOM nodes. OSV batch queries returned three advisory matches. These are dependency-database matches, **not validated NightWave exploit findings**; reachability and actual packaged/runtime use remain unverified.

| Resolved component | Advisory | Upstream subject | Reported upstream fixed line; not applied |
| --- | --- | --- | --- |
| `com.google.guava:guava:33.3.1-android` | [GHSA-xxph-c9ww-hj94 / CVE-2026-102554](https://github.com/google/guava/security/advisories/GHSA-xxph-c9ww-hj94) | Uncontrolled allocation during specific collection deserialization | 33.7.2 |
| `org.jsoup:jsoup:1.22.2` | [GHSA-65r4-943x-97jj / CVE-2026-75140](https://github.com/jhy/jsoup/pull/2556) | XML namespace/depth resource consumption | 1.23.2 |
| `org.jsoup:jsoup:1.22.2` | [GHSA-pmhh-3w7g-xqp8 / CVE-2026-71497](https://github.com/jhy/jsoup/security/advisories/GHSA-pmhh-3w7g-xqp8) | Cleaner/custom raw-text element handling | 1.23.1 |

The user explicitly prohibited app changes. No dependency upgrade, exclusion, source workaround or runtime feature change was applied. The upstream fixed lines are advisory metadata, not an approved upgrade instruction or compatibility guarantee. A later separately authorized review must establish affected call paths, prerequisites, Android applicability and an appropriate disposition. Do not mark these resolved because the app builds or a different scoped workflow audit found no vulnerability.

[The component inventory](RELEASE_COMPONENT_INVENTORY.json) retains supplier-declared POM licenses and POM hashes where cached; no private workstation paths, tokens or user audio are included. POM declarations are not proof of ownership or copyleft compatibility. Nine coordinates lacked cached POMs and twelve lacked license declarations in the inspected cache. JitPack/source provenance, build/test dependencies, actual APK contents, all bundled assets and license compatibility require further review. A missing vulnerability match is not a security guarantee or proof of database coverage.

Raw Gradle/OSV responses, advisory records and scanner results remain in the private recovery checkpoint. This document records public advisory IDs and package versions already present in the source/dependency graph; it provides no exploit instructions.

## Authorized networking follow-up - 29 October 2026

NETWORK01 deliberately adds `androidx.media3:media3-datasource-okhttp:1.6.1`, matching the existing pinned Media3 family, for the owner's media-networking request. Runtime resolution confirms this adapter and OkHttp4.12.0; its cached supplier POM declares Apache-2.0. [Implementation and evidence](../NETWORK_TRANSPORT_THROUGHPUT.md). This is separate from the unchanged historical publication baseline above; its frozen inventory is not a current full dependency scan. No advisory above is marked remediated, no new OSV/security certification is inferred and no public binary/release is approved. The adapter's newer-version lint notice is retained, with a deliberate matching-version choice rather than an incidental whole-player upgrade.
