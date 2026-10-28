# NW130 — decidable public music results

28 October 2026. Review branch `update/nightwave-nw130-public-music-results`; protected NW129 parent `da5db134d2563491cefae717b4ee5a3225ba8da7` (PR #132). Implementation commit `09114486009742d2fdef2d0af2a037e5405d5e37`; [draft PR #133](https://github.com/Nahian2002/NightWave/pull/133). Exact final build evidence is recorded below and in [TEST_REPORT](../TEST_REPORT.md).

## User-visible change

Public results now show the actual uploader, source type, available duration and title-provided version/format labels. Two Play Date candidates with different durations/labels can be compared directly; when the displayed title/uploader/duration/labels are otherwise identical, each gets its own actual video ID. Public titles and uploader names wrap rather than hiding decisive text behind ellipsis. Saved/local rows keep their existing compact layout and origin labels. All themes, narrow widths and large type still need rendered phone acceptance.

`Official audio`, `Live`, `Cover`, `Lyrics` and similar labels describe words supplied in a video title; they never prove recording, artist, ownership, rights, album, offline capability or availability. A zero/negative supplied duration reads **Duration unavailable**, not Live or Ready. No duration or source is fetched merely to decorate a row. Source refers to the video, not the search relay: a YouTube reference discovered via a community instance is still a YouTube video. Raw provider diagnostics remain outside consumer headings.

Songs groups unrequested versions using real whole-word title cues plus complete query/creator token evidence, including curated cross-script aids and repeated-token coverage. Explicitly requested versions remain primary candidates. Groups partition all input objects: none are merged, rewritten, removed, autoqueued or substituted. Videos keeps provider order and the original caption. Each public tap passes the chosen original object and `listOf(selected)` into the existing playback boundary. Existing exact-source offline-first lookup is retained; it does not match another recording by title.

## Search lifetime and recovery

The actual Search route uses `PublicMusicSearchRunner`: one **20-second total deadline**, IO work and a cancellation watcher. Each request owns only its active HTTP calls. A thread-local scope covers NewPipe's synchronous downloader plus community HTTP consumption, including body reads. Calls use the smaller of their original timeout and the remaining search budget. Scope and call registrations are released on every path. Playback resolution, offline workers and unscoped callers retain their own original timeouts; no client-wide `cancelAll` exists.

Gateway checkpoints prevent wrapped extractor/HTTP cancellation from becoming another filter/community retry. Progress distinguishes direct public-video lookup and independent public search without exposing endpoint telemetry. Cancel, typing/clearing, mode replacement, background/stop and retry invalidate the existing activity generation. Phase and final publications must still match that generation; UI visibility must match query and mode. A slow result cannot replace a newer search. Same-query prior results survive a failed/cancelled refresh with an explicit notice and Retry.

Loading offers **Cancel search** and explicit **Open YouTube search**. Timeout is actionable; other failures retain Retry/YouTube. Web fallback only follows a listener action. No query is newly sent to another account/service, no login/token is added, no consent/persistence schema changes occur. Existing public community failover remains within the same bounded search. A stalled direct extractor may exhaust the total deadline before community fallback; this is a bounded failure, not guaranteed provider success.

## Preregistered ranking corpus

The fixed candidate sets were saved and hashed before the NW130 scoring evaluation. Fixture: `app/src/test/resources/nw130-public-search-corpus.json`; SHA256 **8EEF27755ECFA3CC941B2DB72BD756A0E223420CED271C20A2C3211794763DF3**. A private preregistration receipt is in the checkpoint. The test verifies that exact digest before evaluating every expected winner and original selectable object. Its exact path is pinned to LF in `.gitattributes` so Windows clones keep the same preregistered bytes.

**36 cases; zero top-result errors** on supplied synthetic candidates. Cases cover English/Bangla titles and creators, Jhum/cultivation, bidirectional curated aliases/exact preference, Bangla vowel distinctions, repeated words, short/numeric boundaries, part-number/contraction/accent handling, explicitly requested remix/live/cover/speed/acoustic/karaoke, and genuine farming/tutorial/reaction/movie/cooking/gameplay/unboxing requests. Every case also checks full group membership/original object identity and Videos order. Scoring/alias/normalization policies are unchanged from NW129; presentation grouping is extended.

Report: `app/build/reports/nw130-public-corpus.json`, copied privately with all JUnit results. This is metadata ranking on small fixed candidate sets, not live provider recall, unbiased catalog precision, a listener study, legal verification or phone timing. No live provider searches were performed. The old policy/parity suites are retained.

## Verification scope

Fourteen new JVM test methods cover evidence, source-ID collisions, whole-word/Bangla cues, requested versions, the frozen 36-case corpus, real MockWebServer socket/body cancellation, total deadline, no subsequent endpoint after cancel, independent concurrent request isolation, unscoped transport, header/query encoding and scope cleanup. A cancelled preflight scope cannot issue network work. Network tests use localhost synthetic responses only; they do not execute phone playback.

Three additional Android cases compile against the actual StudioSearch: identical Play Date rows/evidence/exact single-URL taps; phase/cancel/retry/explicit web callbacks; and typing during loading hiding prior rows. These are synthetic UI callbacks with no actual account, player/service, owned audio or personal store operation. **Device execution NOT_RUN.** Existing Android cases remain in place.

`verify_nw130_public_search.py --self-test` adds positive/negative wiring assertions to the current runner: all three public sections supply evidence, chosen-song commands remain bounded, generation/phase/deadline wiring and both HTTP transports retain cancellation. This source check is not a substitute for executed JVM tests or Android acceptance.

## Physical acceptance still required

1. Compare the exact APK/source/build ID and installed signer before a permitted in-place update. Preserve profiles, settings, queue, playlists/history, original references and owned audio; never clear storage or uninstall.
2. Execute all three new actual-screen cases plus the retained suite. On real public results, distinguish two same-name versions, inspect actual duration/unknown duration, tap each exact source, use options and verify selected source through player/notification. Check the existing exact-source offline-first path separately.
3. At 320/360/412dp, 1×/2×/3× type and all themes, capture title/uploader/labels/ID wrapping, 48dp controls, scrolling and keyboard/IME. Run TalkBack; verify the two identical-title sources and Options have understandable focus context.
4. With controlled slow/disconnected provider conditions, cancel during headers/body/extraction and community failover; type, clear, change mode, retry, background/recreate and open explicit YouTube fallback. Confirm current phase/query, actionable timeout and no stale publication; record actual stop latency/network evidence. A local socket test does not prove real extractor/ART scheduling or every provider failure.
5. Execute the preregistered task study/live-provider result review separately and record every wrong/missing result. Carry forward NW126–NW129 data, visual, navigation, Android performance/heap, audible and recovery acceptance. Q18's broader source-capability/import workflow remains NW135 work.

The original 139-case matrix and frozen audit are retained. Four NW130 acceptance rows remain PARTIAL pending phone evidence. The provisional rating remains **6.7/10**; public release remains **NO-GO**. Next independent source batch: **NW131 — Home that earns its screen space**.

## Final local evidence

- Complete Windows builder PASS; Android production Kotlin/Compose, all instrumentation-source compilation and debug assembly PASS.
- **1,546 executed JVM tests / 195 suites**, zero failures/errors/skips; **57/57 current source checks PASS**. Fourteen new methods include the 36 frozen candidate cases. No safeguards/tests were removed or suppressed.
- Sequential lint **0 errors / 313 warnings / 17 hints**, unchanged from NW129. No new suppression/baseline.
- Build ID **NW-debug-4555E7A995AB9B03**; **834** inputs; canonical source SHA256 **4555E7A995AB9B03B0A2FDF481E53ABE7C82CABA47E98BA8E0976A2995E414EA**.
- APK SHA256 **6A9313A4468A9620D7BBAA98B65FC7CEC54D3263B2888431BD0DA6461EDD5639**. Independent asset/source verifier **PASS_EMBEDDED_SOURCE_MATCH**. NW08 manifest-file fingerprint **6ECB3BF105C8999B0516510EA73D365FB13C8AA67DECF2DE57883EC881E6459C** uses its retained different ordering/encoding contract.
- Package `com.nightwave.player`, version `2.0.0-alpha5s`, code35, compile/target36/min26 unchanged. Signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394** independently matches protected NW129 APK. Installed-phone signer **NOT_RUN**.
- Release app manifest/build identity, playback engine, offline workers, original 139-case matrix and frozen audit unchanged. No installation, account, library/audio or migration operation performed. `adb devices -l` returned no device.
- Private checkpoint: `C:\Users\jayed\NightWave-checkpoints\20261028-nw130-public-music-results-final`; contains protected parent, final source/history/APK, raw test/lint/source/signature/corpus receipts and prepared 219-case pack. The pack is prepared, not executed. Hosted CI is reported separately in the PR.
