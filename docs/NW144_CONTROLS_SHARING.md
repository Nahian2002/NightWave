# NW144 — External controls and playlist sharing

Plan committed before implementation, 4 November 2026. Cumulative parent NW143 #166; private recovery bundle outside Git. Preserve all features/themes, URLs/quality, library/installed data and package/signer/account/consent boundaries. Do not activate the stable feed, restart automation, install, merge or publish binaries automatically.

Inspect existing MediaSession notification/headset/widget routing, timeout/disconnect and no-loaded-item behavior; preserve authenticated custom-command boundaries. Fix confirmed playlist formatter/parser round-trip ambiguity without changing the declared text grammar, link cap, original URLs or stored metadata. Add explicit public-link preview before sharing/exporting, retaining snapshot identity through the chooser/document picker and existing import preview/name-conflict handling. Exercise malformed/omitted/duplicate/local-source/cancel cases with meaningful regressions. Keep external returns route/queue owned by existing saveable navigation and session, never auto-play an incoming playlist.

Validate targeted JVM regressions, full current source checks, Windows builder then sequential lint; record exact source/APK/signer, private Drive byte/hash readback and draft PR. Physical notification/widget/headset/car/reconnect/restart/share/picker/rotation cases stay NOT_RUN without device evidence. Album/artist catalogs, Connect-like remote handoff, collaboration/social/AI services remain separately evaluated scope; add no placeholder buttons or claims. NW145 is next only for actual acceptance evidence, not repeated speculative UI changes.

## Source review and remaining physical cases

| Surface | Source evidence / preserved boundary | Device acceptance |
| --- | --- | --- |
| Widget play/pause | Existing Media3 controller checks loaded media, playWhenReady and available command; never revives stored queue. Timeout releases the controller and finishes the receiver. | NOT_RUN: loaded/buffering/disconnected/restart states |
| Notification and headset | Existing authenticated custom-command policy and queue availability checks retained. Headset repeat handling and Media3 focus/route ownership unchanged. | NOT_RUN: Next/Previous, exhausted queues, Bluetooth/headset, route loss and reconnect |
| Car clients | Existing Media3 defaults retained; no new privileged custom commands granted to third-party controllers. | NOT_RUN: supported client behavior; no expanded car compatibility claim |
| Share / export | New preview uses exactly the prepared strict-importable public-only snapshot; chooser/file destination opens only after Continue. Cancel does not modify library. Existing cap30 from first500 and omission notice remain. | NOT_RUN: chooser cancel/return, document failure and rotation |
| Import | Existing strict bounded UTF-8 parser, preview-before-write, duplicate-name suffix and library transaction retained. Incoming playlists never autoplay. | NOT_RUN: file/paste/share, malformed/cap/conflict/cancel cases |
| External returns | Existing saveable tabs, navigation requests, no replay of incoming share on rotation, retained pending export and shared playback bridge remain owners. | NOT_RUN: same route/queue/position after provider/share/picker return |

Formatter fix normalizes the reserved ` — ` separator inside display hints to ` – ` before applying the existing caps. The format grammar/footer, original URLs, stored titles/creators and imported data schema are unchanged. New round-trip tests cover embedded separators in creators and title-only text with a long suffix, including preservation of the original in-memory metadata. No audio bytes or private URLs are included. Previews are transient: rotation may dismiss an unconfirmed preview; reopen the action to review again. The existing retained confirmed export survives configuration recreation; process-death and provider completion remain explicit device cases.

## NW144 verified source result — 4 November 2026

Source changes locally verified: exact public-link preview before sharing/export; separator-containing captions round-trip through the existing strict grammar; stored metadata, original URLs, cap30/first500 and private-audio exclusions remain. Existing import preview/name-conflict handling, retained export snapshot, saveable navigation and authenticated widget/session boundaries reviewed and retained. No playback/session engine or permissions rewrite.

Full Windows builder PASS; 1,819 JVM / 226 suites, zero failures/errors/skips, including two new previously failing round-trip cases. Android test sources compile. All 86 current checks PASS_SOURCE_ONLY, including NW144 caller-negative fixtures. Sequential lint PASS: 0 errors / 324 warnings / 17 hints; unchanged ID/severity/message/path inventory. Gitleaks implementation-stage 3,601 commits, no leaks. Build revision `dc2721b6d87c78c6e74e40c238f7f3474125d2d0`.

Build **NW-debug-AE25B5B1EADF6B31**; canonical source **AE25B5B1EADF6B3107956E5FFB73134B437CF414E7110E93EFA87B142D15D869**, 974 inputs. APK **679208F040B70799EEA6AC9F4FBF19960247BB7073C899BC400965BC44A51423**, 26,749,120 bytes. Package `com.nightwave.player`, version `2.0.0-alpha5s`, code39. Signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394**, compared with actual code38 APK. Owner-only Drive metadata and complete remote byte/hash readback verified. Private raw receipts/recovery are outside Git.

Original 219-case pack PREPARED_NOT_EXECUTED for this APK. Actual notification/widget/headset/Bluetooth/car, restart/reconnect, exact external-return/queue continuity, picker/share cancellation/rotation/process-death, large-font/native-speaker and install/data-preservation cases remain NOT_RUN. A transient unconfirmed preview may dismiss on rotation and must be reopened; no automatic sharing/export occurs. No expanded car compatibility, album/artist catalog, remote handoff, social/collaboration/AI service or new rating is claimed. Stable feed disabled; no public binary/main merge/device/account/settings action or automation restart. Next is NW145 physical acceptance and evidence-based re-rating; its dependency gates remain open.

[Draft NW144 review #167](https://github.com/Nahian2002/NightWave/pull/167), stacked on #166; unmerged.
