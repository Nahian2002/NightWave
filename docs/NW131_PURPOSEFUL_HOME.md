# NW131 — purposeful Home and truthful listening mixes

28 October 2026. Branch `update/nightwave-nw131-purposeful-home`; protected parent `e476a979491dcb09fa4734e2763c0f357b6c2cd9` from the completed source-publication preparation. Implementation `f2267fa4d4cd24fccf49228d6823527f08a479e5`. Final local evidence and review link are recorded in [TEST_REPORT](../TEST_REPORT.md).

## Listener-visible change

Home leads with one useful primary action. A new listener gets Find music; liked-only listeners open Liked Songs; playlist-only listeners open Library; listeners with history keep the existing Continue action. The matching quick shortcut is omitted when it would duplicate that primary action. Offline stays available. Empty history no longer consumes a second heading and empty card.

History alone never becomes saved membership. A history-only listener gets a short explanation of how to like or add a song to a playlist through its existing options. The next-listen shelf uses actual liked/playlist songs outside recent rotation. A small mix at least 80% repeated in that shelf is omitted; larger mixes with meaningful additional songs remain. Existing near-clone mix suppression remains; rejected cards do not reserve a key.

Daily mix captions now describe their real contents: Familiar picks, Recent rotation, Beyond recent plays, More recent plays, Playlist picks or Uploader variety. Each card identifies saved/recent/mixed music and the first actual song. Opening a mix shows counts of its distinct selected songs found in likes, playlists and history; memberships can overlap, so these are not additive totals. Explanation membership uses the supplied history, including entries beyond Home's 100-song rail. These labels describe local metadata and uploader names, not verified recording artists, genres, ownership, offline readiness or provider recommendations.

Compact single-column mix cards place one actual thumbnail beside an opaque caption. Large type gets the caption width without the artwork banner and allows title/first-song wrapping. The expanded explanation uses the existing adaptive section/action layout, preserving a minimum 48dp Play action. Missing art uses the existing artwork fallback. Mood tiles are explicitly labeled Search by mood and retain their existing search-prefill callbacks.

## Preserved contracts

- Mix engines, daily selection scores, memory consent/statistics, playback service/resolution, offline workers, account/backup boundaries and persistence schemas are unchanged. The presentation policy performs no IO or writes.
- Card tap expands/collapses a preview. Only explicit Play mix or a track tap sends the original selected object and original bounded mix queue. No source URL or metadata is rewritten.
- Continue retains `onResumeSpotlight` and the existing resume policy. Recent taps receive at most 100 distinct canonical original references; the saved history is never truncated. Expansion labels and Browse full history remain honest.
- Recent options remain behind NW127's accessible overflow; individual removal confirmation/Undo and full-history recovery are preserved. Clear history still requires confirmation and retains liked songs, playlists and downloaded files.
- Home remains a bounded curated preview. NW129's complete asynchronous Search corpus is unchanged; Home's existing playlist/track caps do not become a search limit.
- The app was unchanged during publication preparation. This separately authorized product batch is based on its completed checkpoint; it is not a prerequisite for making source public. Visibility, default branch and binary-distribution status are unchanged.

## Verification and remaining acceptance

Thirteen new executed JVM methods cover new/history-only/liked-only/playlist-only/mature states, invalid references, actual unique membership and original objects, older history beyond the visible rail, uploader evidence, consent-only memory wording, duplicate quick-shelf suppression without key reservation, and retention of a larger distinct mix. Existing Home/history/engine/queue/consent suites remain.

Six Android cases compile against the actual StudioHome: primary destinations, absence of duplicate empty history, original bounded Continue with a 5,000-event history, mix preview before explicit bounded playback, and mood-search without playback. They use synthetic metadata and callbacks only. They do not run an actual player, provider, account or store. Device execution is **NOT_RUN**. Existing density/theme/large-type component sources also compile.

`tools/verify_nw131_home.py --self-test` checks actual caller/presentation/history/action boundaries with broken fixtures; the current runner includes it. NW59's function-isolation check follows StudioHome's internal visibility for screen tests; its ordering, resume, remove, empty navigation and negative fixtures remain. Source-text checks are separate from Kotlin compilation and device proof.

All four NW131 acceptance rows remain **PARTIAL**, device **NOT_RUN**. Required phone protocol:

1. Attribute the installed candidate to the saved build ID and verify installed signer before any permitted in-place update; preserve data, accounts and owned audio. Do not reinstall/reset.
2. Capture new, history-only, liked-only, playlist-only and mature Home at 320/360/412dp and 1×/2×/3× text across themes, including blank/failed/bright artwork and long English/Bangla titles. Check wrapping, opaque text contrast, reachable controls and independent 48dp targets.
3. Use TalkBack/Switch Access to open primary actions, song options and mix previews. Preview must remain silent; explicit Play/Continue must use the intended source/queue and retained resume behavior. Test background/recreation and selected-song stability.
4. Browse a history over 100 unique songs with repeated events. Expand the Home window, open full history, remove/cancel/Undo and clear/cancel; compare persisted contents and unrelated playlists/likes/audio before/after.
5. Confirm every visible shelf/song/explanation against the disposable library, including overlapping memberships and disabled memory. Verify mood shortcuts open Search without autoquery/play or genre/verified-artist claims.

No phone was connected for this batch. No install, account or app-data operation occurred. The prepared 219-case pack is not an executed acceptance run. Provisional rating remains **6.7/10**; 9.5+ certification and app release approval remain pending. Next independent source batch: **NW132 — clear Library and collection identity**.
