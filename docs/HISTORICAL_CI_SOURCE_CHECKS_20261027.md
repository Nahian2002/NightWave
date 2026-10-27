# Historical CI source-check inventory — 27 October 2026

Baseline: `64160f1388b6dcef6439f384602d1a7c9a6c63d8`. These are source-text diagnostics from the old `android.yml`, not JVM test results. This baseline run executed 188 commands: 90 passed, 98 failed. Some failures assert superseded literal text or historical hashes; others need individual review. No finding is claimed fixed merely by changing CI.

The old workflow remains manually runnable as historical diagnostics. `current-android.yml` now runs structural/negative checks, Windows preflights, the full real JVM suite, APK assembly, instrumentation-source compilation, fresh test receipts, and APK signature/package verification. Device and distribution approval remain separate. All historical scripts and app regression tests are retained.

| Command | Baseline exit | Last diagnostic |
| --- | --- | --- |
| `tools/verify_source.py` | 0 | PASS: manifest, secure transport flag, dependency/version and CI artifact wiring |
| `tools/verify_nw40_stability.py --self-test` | 0 | PASS: NW40 integration gate self-test |
| `tools/verify_nw40_stability.py` | 0 | Source gates pass. Android Gradle compile, unit tests and device QA still required. |
| `tools/verify_nw43_queue.py --self-test` | 0 | PASS: NW43 source gate detects accidental Search queue pollution |
| `tools/verify_nw43_queue.py` | 1 | FAIL: queue: missing 'manualNext.addFirst(entry.url)' |
| `tools/verify_nw44_search.py --self-test` | 0 | PASS: NW44 source gate rejects bulk queue regression |
| `tools/verify_nw44_search.py` | 1 | FAIL: ui: missing 'key = "public:" + it.url' |
| `tools/verify_nw45_drive.py --self-test` | 0 | PASS: NW45 gate catches non-atomic verified backup regression |
| `tools/verify_nw45_drive.py` | 0 | NOT VERIFIED: Google Cloud registration, Gradle/Android compile or actual account connection |
| `tools/verify_nw46_player.py --self-test` | 1 | AssertionError: NW46 queue / lyrics / advanced settings hierarchy regressed |
| `tools/verify_nw46_player.py` | 1 | AssertionError: NW46 queue / lyrics / advanced settings hierarchy regressed |
| `tools/verify_nw47_surfaces.py --self-test` | 1 | AssertionError: activity missing: onPlay = { onPlay(song, recent) }, CloudAccountPresentation.BackupStage.CONNECTED_NOT_BACKED_UP -> "CONNECTED" |
| `tools/verify_nw47_surfaces.py` | 1 | AssertionError: activity missing: onPlay = { onPlay(song, recent) }, CloudAccountPresentation.BackupStage.CONNECTED_NOT_BACKED_UP -> "CONNECTED" |
| `tools/verify_nw48_downloads.py --self-test` | 1 | AssertionError: CatalogDownloadsActivity.kt: missing 'OfflineAudioLibrary(this).availableIds(allSavedIds)' |
| `tools/verify_nw48_downloads.py` | 1 | AssertionError: CatalogDownloadsActivity.kt: missing 'OfflineAudioLibrary(this).availableIds(allSavedIds)' |
| `tools/verify_nw49_audio_lyrics.py --self-test` | 1 | AssertionError: PlaybackService.kt: missing ['new.currentMediaItem?.mediaId != "offline:$id"'] |
| `tools/verify_nw49_audio_lyrics.py` | 1 | AssertionError: PlaybackService.kt: missing ['new.currentMediaItem?.mediaId != "offline:$id"'] |
| `tools/verify_nw50_motion.py --self-test` | 0 | UNVERIFIED: Android compilation, actual JVM/instrumented test execution and device QA |
| `tools/verify_nw50_motion.py` | 0 | UNVERIFIED: Android compilation, actual JVM/instrumented test execution and device QA |
| `tools/verify_nw51_reliability.py --self-test` | 1 | AssertionError: Regressed fixture was accepted |
| `tools/verify_nw51_reliability.py` | 0 | NW51 source contracts: PASS; no Android runtime assertion |
| `tools/verify_nw52_release.py --self-test` | 0 | NW52 synthetic fail-closed release checks: PASS |
| `tools/verify_nw52_release.py --json` | 0 | } |
| `tools/verify_nw52_ui_prebuild.py --self-test` | 1 | AssertionError: Broken UI/action contracts: empty-home-discovers-real-search, no-tap-on-empty-track-offline-save, empty-track-offline-action-explains-why, recent-play-not-remove, download-ready-requires-file |
| `tools/verify_nw52_ui_prebuild.py` | 1 | AssertionError: Broken UI/action contracts: empty-home-discovers-real-search, no-tap-on-empty-track-offline-save, empty-track-offline-action-explains-why, recent-play-not-remove, download-ready-requires-file |
| `tools/verify_nw53_oauth_ui.py --self-test` | 0 | UNVERIFIED: actual Google OAuth error 8, SHA-1 registration and Drive APIs |
| `tools/verify_nw53_oauth_ui.py` | 0 | UNVERIFIED: actual Google OAuth error 8, SHA-1 registration and Drive APIs |
| `tools/verify_nw54_search_copy.py --self-test` | 0 | UNVERIFIED: actual provider latency, search relevance, image rendering. |
| `tools/verify_nw54_search_copy.py` | 0 | UNVERIFIED: actual provider latency, search relevance, image rendering. |
| `tools/verify_nw55_offline_chrome.py --self-test` | 1 | AssertionError: downloads: missing Start download again |
| `tools/verify_nw55_offline_chrome.py` | 1 | AssertionError: downloads: missing Start download again |
| `tools/verify_nw56_contrast.py --self-test` | 0 | UNVERIFIED: actual font rendering, dynamic colors and device screenshots. |
| `tools/verify_nw56_contrast.py` | 0 | UNVERIFIED: actual font rendering, dynamic colors and device screenshots. |
| `tools/verify_nw57_home.py --self-test` | 1 | AssertionError: Home action missing: onResumeSpotlight(it, recent) |
| `tools/verify_nw57_home.py` | 1 | AssertionError: Home action missing: onResumeSpotlight(it, recent) |
| `tools/verify_nw58_overlay.py --self-test` | 0 | UNVERIFIED: actual transition rendering on Android. |
| `tools/verify_nw58_overlay.py` | 0 | UNVERIFIED: actual transition rendering on Android. |
| `tools/verify_nw60_offline_recovery.py --self-test` | 1 | AssertionError: service: missing generation != atErrorGeneration |
| `tools/verify_nw60_offline_recovery.py` | 1 | AssertionError: service: missing generation != atErrorGeneration |
| `tools/verify_nw61_oauth_report.py --self-test` | 0 | NOT VERIFIED: Google Cloud OAuth client, phone sign-in, actual APK signer. |
| `tools/verify_nw61_oauth_report.py` | 0 | NOT VERIFIED: Google Cloud OAuth client, phone sign-in, actual APK signer. |
| `tools/verify_nw62_download_health.py --self-test` | 1 | AssertionError: ui: missing 'OfflineAudioLibrary(applicationContext).availableIds(ids)' |
| `tools/verify_nw62_download_health.py` | 1 | AssertionError: ui: missing 'OfflineAudioLibrary(applicationContext).availableIds(ids)' |
| `tools/verify_nw01_recovery.py` | 1 | AssertionError |
| `tools/verify_nw02_sound_studio.py` | 1 | AssertionError |
| `tools/verify_nw03_account_hub.py` | 1 | AssertionError |
| `tools/verify_nw04_premium_player.py` | 1 | AssertionError |
| `tools/verify_nw05_integrated_stability.py` | 1 | AssertionError |
| `tools/verify_nw06_discovery_search.py` | 1 | AssertionError |
| `tools/verify_nw07_release_gate.py` | 0 | UNVERIFIED: Android compilation, JVM results, APK, signers and Samsung device QA |
| `tools/verify_nw08_attestation.py` | 0 | UNVERIFIED: PowerShell execution, Gradle tests, APK signer, Android runtime |
| `tools/verify_nw09_device_reliability.py` | 1 | AssertionError |
| `tools/verify_nw11_junit_source.py` | 1 | AssertionError |
| `tools/verify_nw12_integration.py` | 1 | AssertionError: Policy changed since actual NW12 compiler run: app/src/main/java/com/nightwave/player/playback/ShuffleModePolicy.kt |
| `tools/verify_nw13_queue_navigation.py` | 0 | UNVERIFIED: JUnit execution, full Compose/Media3 Android compilation and APK |
| `tools/verify_nw14_queue_window.py` | 1 | AssertionError |
| `tools/verify_nw15_queue_intent.py` | 1 | AssertionError |
| `tools/verify_nw15b_windows_encoding.py` | 1 | AssertionError: Windows syntax preflight script set mismatch: missing=['nw76-junit-proof.ps1', 'nw77-release-check.ps1'], stale=[] |
| `tools/verify_nw15c_kotlin_imports.py` | 0 | UNVERIFIED: Windows PowerShell 5.1 run, full Gradle Kotlin compilation, APK |
| `tools/verify_nw15d_nw07_imports.py` | 0 | UNVERIFIED: Windows PowerShell execution, Gradle JVM tests, APK signer |
| `tools/verify_nw16_permitted_downloads.py` | 1 | AssertionError |
| `tools/verify_nw18_audio_downloads.py` | 1 | AssertionError |
| `tools/verify_nw19_audio_integrity.py` | 0 | UNVERIFIED: complete Android Gradle build, real Android downloader and device QA |
| `tools/verify_nw21_download_ux.py` | 0 | UNVERIFIED: Android Gradle compilation, live Commons API and phone tests |
| `tools/verify_nw22_build_readiness.py --self-test` | 0 | PASS: NW22 read-only build-gate positive/negative self-tests |
| `tools/verify_nw22_build_readiness.py` | 1 | FAIL: Windows builder must pass optional trusted certificate fingerprint |
| `tools/audit_repo_nw22.py --self-test` | 0 | PASS: NW22 full-tree static audit positive and negative fixtures |
| `tools/audit_repo_nw22.py` | 0 | LIMIT: Python syntax, XML wellformedness, UTF-8, exact Kotlin imports, limited Kotlin per-line string/char check and conflict/key markers only. Not complete Kotlin/Gradle compilation, PowerShell parsing, execution or Android device QA. |
| `tools/verify_nw23_download_queue.py --self-test` | 0 | PASS: NW23 source gate fails closed on incomplete source |
| `tools/verify_nw23_download_queue.py` | 1 | FAIL: Network failures must have a bounded outcome |
| `tools/verify_nw24_library.py --self-test` | 0 | PASS: NW24 regression gate fails closed on incomplete source |
| `tools/verify_nw24_library.py` | 1 | FAIL: Search and page changes must reuse the current read-only health scan |
| `tools/verify_nw25_ui.py --self-test` | 0 | PASS: NW25 UI source gate fails closed on missing features |
| `tools/verify_nw25_ui.py` | 1 | FAIL: Profile rows must have 60dp minimum tap targets |
| `tools/verify_nw26_cloud_search.py --self-test` | 0 | PASS: NW26 regression gate rejects missing source |
| `tools/verify_nw26_cloud_search.py` | 1 | FAIL: Worker must validate before and after upload and classify retries |
| `tools/verify_nw27_hardening.py --self-test` | 0 | LIMIT: no Android compilation was attempted |
| `tools/verify_nw27_hardening.py` | 1 | FAIL: App package/SDK identity changed unexpectedly |
| `tools/verify_v2_phase5r.py` | 1 | AssertionError |
| `tools/verify_v2_phase5s.py` | 1 | AssertionError |
| `tools/verify_v2_phase5t.py` | 1 | AssertionError |
| `tools/verify_v2_phase5u.py` | 1 | AssertionError |
| `tools/verify_v2_phase5y.py` | 1 | AssertionError |
| `tools/verify_v2_phase5z.py` | 1 | AssertionError |
| `tools/verify_v2_phase6a.py` | 1 | AssertionError: AsyncImage( |
| `tools/verify_v2_phase6a2.py` | 1 | AssertionError |
| `tools/verify_v2_phase6a2d.py` | 1 | AssertionError |
| `tools/verify_v2_phase6b.py` | 1 | AssertionError |
| `tools/verify_v2_phase6c.py` | 1 | AssertionError |
| `tools/verify_v2_phase6c2.py` | 1 | AssertionError |
| `tools/verify_v2_phase6d.py` | 1 | AssertionError |
| `tools/verify_v2_phase6e.py` | 1 | AssertionError |
| `tools/verify_v2_phase6f.py` | 1 | AssertionError |
| `tools/verify_v2_phase6g.py` | 0 | PENDING: Android/Compose compile, full JVM tests, device playback, signature QA |
| `tools/verify_v2_phase6h.py` | 1 | ValueError: substring not found |
| `tools/verify_v2_phase6h1.py` | 0 | PENDING: actual Windows PowerShell self-test, Android Gradle and APK QA |
| `tools/verify_v2_phase6h2.py` | 0 | PENDING: complete Android Gradle/JUnit/instrumentation, signer and device QA |
| `tools/verify_v2_phase6i.py` | 1 | AssertionError |
| `tools/verify_v2_phase6j.py` | 1 | AssertionError |
| `tools/verify_v2_phase6k.py` | 1 | AssertionError |
| `tools/verify_v2_phase6x2.py` | 1 | AssertionError |
| `tools/verify_v2_phase6x3.py` | 0 | PENDING: full Android Gradle build, JUnit execution, and actual device loudness audition |
| `tools/verify_v2_phase6x4.py` | 1 | AssertionError |
| `tools/verify_v2_phase6x5.py` | 1 | AssertionError |
| `tools/verify_v2_phase6x6.py` | 1 | AssertionError |
| `tools/verify_v2_phase6x7.py` | 1 | AssertionError |
| `tools/verify_v2_phase6x8.py` | 1 | AssertionError |
| `tools/verify_v2_phase6x8b.py` | 0 | PENDING: complete Android Gradle build and AAOS/Android Auto emulator playback verification |
| `tools/verify_v2_phase6x8c.py` | 0 | NOT VERIFIED: Android Gradle compile, Android Auto host, Play/Pause/Next, Bluetooth and signing |
| `tools/verify_v2_phase6x8c3.py` | 0 | UNVERIFIED: Gradle compile, JVM tests, Android instrumentation, device playback and signer |
| `tools/verify_v2_phase6l.py` | 1 | AssertionError |
| `tools/verify_v2_phase6m1.py` | 1 | AssertionError |
| `tools/verify_v2_phase6m2.py` | 1 | AssertionError |
| `tools/verify_v2_phase6n1.py` | 0 | PENDING: Android compilation, JVM suite, signing and device verification |
| `tools/verify_v2_phase6n2.py` | 1 | AssertionError |
| `tools/verify_v2_phase6n3.py` | 0 | PENDING: Android compile/JVM runtime, actual audio routes, phone QA |
| `tools/verify_v2_phase6o1.py` | 1 | AssertionError |
| `tools/verify_v2_phase6o2.py` | 1 | AssertionError |
| `tools/verify_v2_phase6o3.py` | 1 | AssertionError |
| `tools/verify_v2_phase6o4.py` | 1 | AssertionError |
| `tools/verify_v2_phase6p.py` | 1 | AssertionError |
| `tools/verify_v2_phase6q1.py` | 1 | AssertionError |
| `tools/verify_v2_phase6q2.py` | 1 | AssertionError |
| `tools/verify_v2_phase6q3.py` | 1 | AssertionError |
| `tools/verify_v2_phase6r1.py` | 0 | PENDING: Kotlin/JUnit/Google OAuth/live upload/Android device and signed build |
| `tools/verify_v2_phase6r2.py` | 1 | AssertionError |
| `tools/verify_v2_phase6r3.py` | 1 | AssertionError |
| `tools/verify_v2_phase6r3b.py` | 0 | PENDING: real Gradle/JUnit, OAuth, Drive E2E and signed Android device test |
| `tools/verify_v2_phase6v.py` | 1 | No Gradle build or APK approval was attempted by this script. |
| `tools/verify_v2_phase6v1.py` | 0 | NOT VERIFIED: Android Gradle/CI execution, Google OAuth, live upload or device signing |
| `tools/verify_v2_phase6v2.py` | 1 | AssertionError |
| `tools/verify_v2_phase6v3.py` | 1 | AssertionError |
| `tools/verify_v2_phase6v4d.py --self-test` | 0 | PASS: synthetic good project accepted; missing runtime hook and tests rejected |
| `tools/verify_v2_phase6v4d.py` | 1 | FAIL: 1 Phase 6V4D integration source gaps |
| `tools/verify_v2_phase6v5.py --self-test` | 0 | PASS: 6V5 synthetic contracts accept safe hooks and reject removed guards |
| `tools/verify_v2_phase6v5.py` | 1 | FAIL: 1 Phase 6V5 integration errors |
| `tools/verify_v2_phase6v5b.py --self-test` | 0 | PASS: 6V5B synthetic source checker rejects disconnected history outcomes |
| `tools/verify_v2_phase6v5b.py` | 0 | NOT VERIFIED: Kotlin/JVM/Android runtime, Google OAuth and signer |
| `tools/verify_v2_phase6v5c.py --self-test` | 0 | PASS: synthetic 6V5C gate rejects old scan and missing targeted duplicate guard |
| `tools/verify_v2_phase6v5c.py` | 0 | UNVERIFIED: Gradle, Android UI, Google Drive API and signed APK |
| `tools/verify_v2_phase6v6a.py --self-test` | 0 | PASS: 6V6A synthetic checker rejects unsafe seek and unbound lyric offsets |
| `tools/verify_v2_phase6v6a.py` | 1 | FAIL: app/src/main/java/com/nightwave/player/v2/ui/StudioPlayerScreens.kt: missing 'if (reduceMotion) lyricListState.scrollToItem' |
| `tools/verify_v2_phase6v6b.py --self-test` | 0 | PASS: synthetic 6V6B gate rejects unsafe replacement and missing reviewed import |
| `tools/verify_v2_phase6v6b.py` | 0 | UNVERIFIED: Kotlin compile, JVM test execution, Android UI and signed APK |
| `tools/verify_v2_phase6v6c.py --self-test` | 0 | PASS: synthetic 6V6C source gate rejects unreviewed saves and cue truncation |
| `tools/verify_v2_phase6v6c.py` | 0 | UNVERIFIED: Kotlin/JUnit, Compose rendering, device testing, APK signer |
| `tools/verify_v2_phase6v6d.py --self-test` | 0 | PASS: synthetic 6V6D gate rejects unverified and stale LRC export |
| `tools/verify_v2_phase6v6d.py` | 0 | UNVERIFIED: Kotlin/JUnit, Compose, SAF provider, emulator, signed APK |
| `tools/verify_v2_phase6v6e.py --self-test` | 0 | PASS: cumulative 6V6E audit detects unbounded search and stale navigation |
| `tools/verify_v2_phase6v6e.py` | 0 | NOT VERIFIED: Gradle/JUnit execution, Compose/emulator, Drive, SAF or APK signer |
| `tools/verify_v2_phase6v6f.py --self-test` | 0 | PASS: 6V6F synthetic guard finds fake CI compile claims and unsafe LRC writes |
| `tools/verify_v2_phase6v6f.py` | 0 | NOT VERIFIED: Android Gradle/JUnit, Compose, Google Drive, signer or on-device upgrade |
| `tools/verify_v2_phase6v7a.py --self-test` | 0 | PASS: synthetic 6V7A gate detects pager-swipe and hide-audio regressions |
| `tools/verify_v2_phase6v7a.py` | 0 | NOT VERIFIED: Gradle, Compose instrumentation, APK signature or phone |
| `tools/verify_v2_phase6v7b.py --self-test` | 0 | PASS: 6V7B synthetic audit catches loss of existing library sorting |
| `tools/verify_v2_phase6v7b.py` | 1 | FAIL: app/src/main/java/com/nightwave/player/v2/ui/StudioComponents.kt: missing 'NightWaveCollectionChrome.TRACK_ARTWORK_DP.dp' |
| `tools/verify_nw81_titles.py` | 0 | UNVERIFIED: Kotlin compilation, Gradle, Android rendering, real-device QA |
| `tools/verify_nw82_search.py` | 1 | AssertionError |
| `tools/verify_nw83_premium_visual.py` | 0 | UNVERIFIED: Android compilation, rendered screenshots, font clipping and real-device UX |
| `tools/verify_nw84_home.py` | 1 | AssertionError: onContinue = recent.firstOrNull()?.let { last -> |
| `tools/verify_nw85_now_playing.py` | 0 | UNVERIFIED: full Android build, Compose screenshots, queue audio and device QA |
| `tools/verify_nw86_library.py` | 1 | AssertionError |
| `tools/verify_nw87_transfer_offline.py` | 1 | AssertionError: checked.getOrNull()?.indexReadable == true |
| `tools/verify_nw88_sound_studio.py` | 0 | UNVERIFIED: full Gradle/JUnit, Android EQ audibility, routing and device playback |
| `tools/verify_nw89_lyrics.py` | 0 | UNVERIFIED: Kotlin/JUnit execution, Android compilation and TalkBack/device QA |
| `tools/verify_nw90_final_audit.py` | 1 | AssertionError |
| `tools/verify_nw91_certification.py` | 1 | AssertionError |
| `tools/verify_nw92_storage.py` | 0 | NW92_STORAGE_SOURCE_WIRING_PASS |
| `tools/verify_nw93_queue_recovery.py` | 0 | NW93_QUEUE_RECOVERY_SOURCE_WIRING_PASS |
| `tools/verify_nw94_playback.py` | 0 | Android APK, device and power-loss durability still require independent tests. |
| `tools/verify_nw95_audio_route.py` | 0 | NW95_AUDIO_ROUTE_SOURCE_WIRING_PASS |
| `tools/verify_nw96_equalizer.py` | 0 | NW96_EQ_SESSION_SOURCE_WIRING_PASS |
| `tools/verify_nw97_offline_transitions.py` | 0 | NW97_OFFLINE_TRANSITION_SOURCE_WIRING_PASS |
| `tools/verify_nw98_background_intent.py` | 0 | NW98_BACKGROUND_INTENT_SOURCE_WIRING_PASS |
| `tools/verify_nw99_media_session.py` | 0 | NW99_MEDIA_SESSION_SOURCE_WIRING_PASS |
| `tools/verify_nw100_playback_audit.py` | 1 | AssertionError |
| `tools/verify_nw101_attestation.py --self-test` | 0 | NW101_SOURCE_SNAPSHOT_REGRESSION_PASS cases=8 |
| `tools/verify_nw101_attestation.py` | 0 | NW101_BUILD_EVIDENCE_WIRING_PASS checks=16 |
| `tools/verify_nw102_compiler_matrix.py` | 1 | AssertionError |
| `tools/verify_nw103_build_pipeline.py` | 1 | AssertionError |
| `tools/verify_nw104_nested_enum_test_sources.py --self-test` | 0 | NW104_ENUM_REFERENCE_SELFTEST_PASS cases=7 |
| `tools/verify_nw104_nested_enum_test_sources.py` | 1 | AssertionError: ('playback/MediaSessionQueueControlPolicyTest.kt', 13, 8) |
| `tools/verify_nw105_offline_junit.py` | 1 | AssertionError |
| `tools/verify_nw106_seek_timeline.py` | 0 | NW106_SEEK_TIMELINE_SOURCE_WIRING_PASS checks=16 |
| `tools/verify_nw107_lyrics_follow.py` | 0 | NW107_LYRICS_FOLLOW_ACCESSIBILITY_SOURCE_PASS checks=18 |
| `tools/verify_nw108_playlist_identity.py` | 0 | NW108_BACKUP_PLAYLIST_IDENTITY_WIRING_PASS checks=16 |
| `tools/verify_nw109_android36_privacy.py --self-test` | 0 | NW109_ANDROID36_PRIVACY_NEGATIVE_SELFTEST_PASS cases=10 |
| `tools/verify_nw109_android36_privacy.py --project-root .` | 0 | NW109_ANDROID36_PRIVACY_SOURCE_GATE_PASS (not Android/device certification) |
