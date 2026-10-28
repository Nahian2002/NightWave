# NW133 — player, artwork and metadata finish

28 October 2026. Branch `update/nightwave-nw133-player-artwork`; protected public-source parent `48abd3055aaf0d2ec7a736c7e85e79bf95dcee3b`. Implementation `d2828b9dd1e90a33e2d608b3a3b8289e25b8d54e`. [Draft PR #144](https://github.com/Nahian2002/NightWave/pull/144). Exact artifact and verification: [TEST_REPORT](../TEST_REPORT.md). Source review is separate from phone acceptance.

## Listener-visible change

Expanded Now Playing requests same-video maxres, SD and HQ stills before the original source image, only for recognized unsigned HTTPS YouTube still URLs. The sequence is distinct and bounded to four addresses. Small artwork retains the earlier HQ/original behavior and does not probe larger variants. Custom, local, signed, authenticated, unknown or malformed URLs remain exact. No new catalog, image provider, verified album identity or rights claim is introduced.

Artwork requests use measured square decode dimensions, bounded to 48–1280 physical pixels, with Coil memory/disk caching and the existing parent-owned transition. Missing/error images advance through the bounded candidates; small known large-variant placeholders also advance. Attempt guards prevent an obsolete error from skipping a later candidate; selected identity/source changes reset fallback state. Blank/exhausted art retains the NightWave motif and announces image unavailability. Existing Reduce Motion removes the artwork crossfade. Actual HTTP/image-decoder/cache behavior and rendered sharpness still need device evidence; no live provider request was used as a quality proof.

Mini and expanded player derive current-song status from the same playback-health policy. Expanded Now Playing shows Finding audio, Buffering audio, Paused, Playing, disconnected or failure status near the seek rail. Retry/reconnect stays an explicit action in a reserved 48dp slot, and the original detailed error remains available. Duplicate bottom loading/retry UI is removed. Save uses a playlist-add symbol with its existing label/callback. Title/creator layout, current-source identity, queue and lyrics destinations retain their existing contracts.

Seeking is disabled while disconnected, resolving, failed or without a positive known duration. A commit requires the originating selected URL to match the current URL and routes through the existing identity-checked player action. Drag state resets on identity changes. Accessibility exposes the seek position and disabled state. Buffering with a known duration remains seekable. This batch does not replace Media3's final identity guard or alter playback/queue persistence.

## Verification and boundaries

Fourteen new executed JVM methods cover bounded same-video candidates, large/small requests, signed/local/custom/invalid URLs, placeholder decisions, measured sizing, source isolation, health precedence and exact/clamped/stale/unavailable/nonfinite seek commands. Six new Android tests compile against actual mini/full player screens and CoverArt: shared status/identity, resolving-disabled seek, accessibility seek after identity change, explicit retry, queue/lyrics reachability and absent-art semantics. They inject blank art and in-memory actions; they do not run a provider, real player, notification or installed store.

The current runner adds `verify_nw133_player.py --self-test`, checking production caller wiring and seven broken fixtures. All 60 current source checks and 1,587 JVM tests / 198 suites pass. Production Android/Compose, instrumentation-source compilation and debug assembly pass. Lint and exact artifact/certificate receipts are recorded in the current test report. No test, dependency, permission, schema or warning baseline was removed or suppressed.

Addresses Q08/Q15/Q16 at source level. Q17 lyric availability/onboarding remains NW137 work; existing local-audio/LRC explanation and entry paths remain. The mini-player's hide action continues playback. No original public URL, imported audio, account/consent boundary, transport engine, signing identity or installed data is changed. Notification consistency is an acceptance task; this batch does not claim a notification implementation change. Repository Actions stays disabled and no APK is uploaded.

## Required phone acceptance

All four NW133 acceptance rows remain PARTIAL; device NOT_RUN:

1. Attribute the candidate build/signer before any approved in-place update. Compare legitimate full/small source art, maxres unavailable/404/HTTP-200 placeholder, missing/offline art, cache hit and rapid song changes. Check sharpness and absence of wrong-song art; custom/signed/local inputs retain exact source semantics.
2. Capture 320/360/412dp with 1×/2×/3× text and themes, including long Bangla/English metadata and bright/missing art. Verify Save/Like/transport/seek/queue/lyrics targets, contrast and scroll reachability. Distinguish horizontal artwork skip from vertical scrolling; verify no unintended transport during cancellation or a song change.
3. Exercise resolving, buffering, paused, playing, failed and disconnected/reconnected states; compare mini/full/notification source and audible identity across routes, background and Reduce Motion. Retry only on a deliberate action.
4. Test seek taps/drags/TalkBack/Switch Access during song changes, reconnect, unknown duration and failure; no old command may seek the next song. Verify one committed command, matching position feedback, queue entry and honest lyrics entry.

No device was connected and no install/reset/account/data operation ran. The provisional product score stays 6.7/10; app distribution stays NO-GO. The 219-case disposable pack is preparation only. Next source batch: **NW134 — queue and transport acceptance**; carry all earlier physical and release blockers forward.
