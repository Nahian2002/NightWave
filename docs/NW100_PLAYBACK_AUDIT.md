# NW100 — Cumulative Playback Reliability Audit

Date: 27 October 2026. Parent: `b7d3086698e3e3d01daf43303d8f974c997c84ce`. Development branch: `feature/nightwave-nw100-playback-audit-gates`.

## Confirmed NW99 integration defect

The NW99 media-session handler accepted custom Next if a *selected* song existed, but selection does not imply that an additional song is available. On the final public/offline song with **Repeat Off**, the handler called a no-op `playNext(false)` and still returned `SessionResult.RESULT_SUCCESS`. A notification/widget controller could therefore acknowledge a command that made no selection change.

## Narrow fix

- `PlaybackQueue.hasNext()` and `OfflinePlaybackQueue.hasNext()` inspect manual NEXT availability without modifying any queue state; public explicit Play Next priority, shuffle forward redo and unseen cycle, and Repeat All are accounted for. **Manual** Next ignores Repeat One's automatic replay semantics.
- `PlaybackService.hasManualNext()` probes the active mode: public queue, legacy offline queue, or the prepared Media3 offline timeline's actual `nextMediaItemIndex` and the selected item identity. No speculative shuffle order or signed media URL is created.
- The notification/widget custom Next command returns `RESULT_ERROR_NOT_SUPPORTED` when the current queue or timeline cannot advance. The existing song remains selected. Previous behavior and Media3 native audio focus are unchanged.
- Add twelve JUnit cases across the two real queue classes and an exact-source JVM smoke suite plus source checker. The existing NW92–99 source verifiers remain in the cumulative workflow.

## Risk review and proof boundary

| Area | Confirmed source behavior | Pending proof |
|---|---|---|
| NW92 offline registration | Retained as code + previous source gates | Actual file/device integration |
| NW93 shuffle recovery | ID-validated restoration retained | Android process-death tests |
| NW94 checkpoint | Bookmark identity fence retained | Full Android JUnit/instrumented |
| NW95 headset focus | Distinct Play/Pause intent retained | Headset/Bluetooth device QA |
| NW96 sound effects | Manual band centers validated | Acoustic EQ and Android effect ownership |
| NW97 shuffle transitions | Repeat Off seen-cycle fix retained | Real Media3 handoff |
| NW98 pending play | Widget Play/Pause intent fence retained | Live notification/widget commands |
| NW99 buttons | Queue-aware custom media buttons retained | Android notification rendering |
| NW100 Next result | No-op Next is now rejected if availability check is negative | Runtime file/decoder failures can still prevent playback |

**A successful availability probe does not guarantee playback**: offline files may become unreadable or public stream extraction may fail after the command. Never represent this as universal media/playback certification.

## Build and publication gates

No full current Android build, JUnit execution or 139-case phone pass has been confirmed. GitHub Android workflow and empty canary have failed *before step execution* in prior phases. This branch maintains both workflow triggers and a standalone Kotlin queue smoke, but needs a functioning runner or local Windows build to certify.

Do not uninstall an existing app or clear data. Verify installed APK signer before updating. Google OAuth package/SHA, Play API target, internet music provider rights and physical background playback remain independent release gates.

**Release decision: NO-GO.** No private audio, public library records, credentials, package ID, signing key, Google auth, or manifest changes in NW100.
