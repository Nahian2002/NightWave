# NightWave — dependency and external-content review

**Status: identification only — license/CVE/asset provenance NOT certified.** Data source: source-declared `app/build.gradle.kts` and `settings.gradle.kts` at NW34 commit `d1df2636`, read 25 Oct 2026.

## Notable dependency families (declared versions, not vulnerability findings)

| Component | Declared source version | Review obligation |
| --- | --- | --- |
| Android Gradle Plugin | 8.9.2 | Build reproducibility, compatibility with API 36 |
| Kotlin and Compose compiler plugin | 2.0.21 | Known fixes and supported Android API |
| KSP | 2.0.21-1.0.28 | Plugin/Kotlin compatibility |
| NewPipeExtractor | v0.26.5 via JitPack | Exact upstream license, copyleft obligations, packaging, source redistribution and YouTube platform policy |
| Media3 / ExoPlayer | 1.6.1 | License and CVEs, playback/privacy behavior |
| OkHttp + MockWebServer | 4.12.0 | Network and redirect security, CVE status |
| Room | 2.7.2 | Storage and schema migration behavior |
| WorkManager | 2.10.4 | Background job and foreground service behavior |
| Google Play Services auth | 21.4.0 | OAuth, production consent and privacy declarations |
| Coil | 2.7.0 | External image fetch, rights and network disclosure |
| Guava | 33.3.1-android | Dependency graph/notice |
| AndroidX Compose and Activity/Lifecycle | BOM 2024.12.01 and pinned modules | Version matching, platform requirements |
| Android desugaring libraries | 2.1.5 | Android 8+ compatibility |
| JUnit, JSON and Android test libs | Versions in Gradle file | Test-only redistribution and graph audit |

**Do not interpret this table as a complete transitive SBOM.** Generate `python tools/release_dependency_inventory.py --json`, resolve Gradle's actual graph, then run a current CVE/license audit with trusted tools. Investigate all critical/high vulnerabilities before distribution; avoid upgrading app dependencies on the preparation branch.

## Third-party content and service obligations

- NightWave's code is declared `GPL-3.0-or-later` via `NOTICE` and GPL text at `LICENSE`. Review all actual upstream licenses, attribution obligations and any obligations to distribute corresponding source with binaries.
- Do not assume a public YouTube audio stream is permitted to be stored or redistributed. Live YouTube playback/embedded WebView/backgound extraction need an independent platform-terms review.
- Identify provenance, copyright ownership, and release rights for `app/src/main/res/`, typography, iconography, screenshots, marketing assets and all code imports. No binary image/font assets appear in the latest tracked source tree, but generated/distributed assets have not been inspected.
- Check each public Invidious endpoint's operator policy and user-data handling before including it in public disclosures.
- Document a permitted support/privacy contact, name/trademark review and app listing claims.

## Sign-off fields for a future release

- Dependency graph digest / SBOM URI: **MISSING**
- Security/CVE scanner date and result: **MISSING**
- All bundled asset ownership evidence: **MISSING**
- NewPipeExtractor license review and source notice: **MISSING**
- YouTube/recording distribution rights: **BLOCKED**
- Maintainer approval: **NOT GIVEN**

No public APK/Play release can be certified from this checklist alone.
