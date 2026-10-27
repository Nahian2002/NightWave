# NW128 — shared offline navigation and selected-song controls

28 October 2026. Branch `update/nightwave-nw128-offline-shell`, protected NW127 parent `a6d8842f5379579a164517745567664751713e7c` (PR #130). This continues the cumulative chain; default `main` is the old v0.8 app.

## Delivered behavior and boundaries

The four private offline screens — On device, Find audio, Downloads and Audio link — now share a themed navigation shell, Back, Settings, the four main destinations and the existing selected-song mini-player. Every offline screen can reach all four offline destinations through a horizontally scrollable, labeled route strip. Home and Library already link to On device, Search links to the catalog and offline results, and You now has an explicit Offline music shortcut.

The existing native panels, activity-result picker, source permission checks, foreground transfer cancellation, WorkManager jobs/observers, file/index ownership and MediaSession remain authoritative. This is an incremental shell migration, not an engine/storage rewrite. The native panel interiors still contain historical fixed styling and controls; this batch does not certify a complete adaptive/themed redesign of them. Later offline and accessibility/adaptive batches retain that work.

The same `StudioMiniPlayer` sends transport commands through a lifecycle-owned `StudioPlaybackBridge` to the existing service. Hiding/restoring its UI does not pause playback or clear a queue. Hidden state belongs to a specific selected media ID and does not hide a later selection. Expanding it opens a local transport sheet; Open full player and queue deliberately returns to the main player. No navigation request invokes Play, restores an old song as a new selection or rebuilds a queue.

The entire retained panel has a scrolling viewport, so adding navigation chrome does not reserve the remaining space exclusively for fixed legacy controls. Existing library/download page bounds remain. A compiled Android measurement case checks that weighted paged rows remain reachable in a short viewport; real screen/input behavior remains unrun.

## Route, lifecycle and state contract

- Private sibling launches use `REORDER_TO_FRONT | SINGLE_TOP`: reuse their existing activity and its query/form/scroll/work state rather than repeatedly adding copies. Unrelated/external intents pass through unchanged. Both the main Activity and shared offline base apply this normalization, covering existing links inside native panels and the Compose Search header.
- Sibling switches retain native Android Back history in most-recent activity order. System Back/gesture Back and the shell Back button use the platform dispatcher; no competing custom offline history intercepts them. Real repeated task-stack/gesture validation is still required.
- Choosing a main destination deliberately leaves the offline branch. A main-rooted task returns through `CLEAR_TOP | SINGLE_TOP`. A recognized legacy/offline-rooted task is replaced with a main-rooted task so an old offline loop cannot remain under a newly created main shell. Unknown task roots do not authorize clearing a task. This can close foreground transfers when deliberately leaving their owning offline branch, as their existing cancellation contract specifies; persistent WorkManager jobs continue under WorkManager.
- Main handoff accepts only a bounded valid request ID, one of four tabs and an explicit open-player view flag. It carries no audio source or account data. The main shell acknowledges the applied request; the acknowledgment is saved separately so recreating the Activity cannot replay an already applied navigation over restored UI state. Incoming shares retain their existing handler and safeguards.
- Bridge polling pauses on Stop and resume on visibility; controller release happens on Destroy. No second player/service is introduced. The shell reads the existing profile theme/motion preference; it does not alter those preferences.
- On device saves its bounded query, page, sort, filter and scroll. Explicit incoming queries reset the relevant browsing state; asynchronous health reads remain guarded by their original generation and destroyed-Activity checks. The document picker registration/order and import pipeline remain intact.
- Downloads retains filter/history/page and scroll while the original asynchronous WorkManager/file-verification generation fences remain. A first empty paint does not consume a saved offset before job rows arrive.
- The catalog retains a query/origin draft and discards obsolete search responses. Recreation does not silently rerun a network search or foreground download. A new query cannot replace an active foreground transfer. The user must explicitly retry a paused search.
- Audio link retains bounded URL/name drafts locally in instance state, resets the permission checkbox after recreation and explains interrupted foreground work. It never silently restarts a transfer or serializes permission consent. WorkManager submissions still use the original eligibility/permission paths.

Android's [Intent reference](https://developer.android.com/reference/android/content/Intent) and [task/back-stack guidance](https://developer.android.com/guide/components/activities/tasks-and-back-stack) define the platform flags and history behavior. This project uses [View/Compose interoperability](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose) to retain the native content during migration; source policy tests cannot certify actual task scheduling.

## Deliberate recovery route

Offline navigation options → Use native recovery view switches the current panel in place. Its original content/workers/picker stay alive, original native header returns, shared controller ticks pause and the native safe-insets handler owns the recovery panel. Restore main navigation switches back in place. The shell restores its viewport without repeatedly registering listeners. No screen recreation, app uninstall, file deletion, ownership change or consent action is needed to change chrome.

The internal `nw128-native-recovery` entry flag and saved recovery mode support a deliberately launched native path. There is no claim that an arbitrary asynchronous Compose crash can be caught automatically. Existing classic recovery also remains. Compose owns normal safe-area/IME insets once; the recovery panel owns native insets once.

## Verification and required device protocol

The unit policies validate route allowlisting, bounded Unicode drafts/positions, request acknowledgment, invalid destinations and legacy-root decisions. New Android sources cover real shell callbacks, hidden-player isolation, explicit full-player view requests, recovery/Back, intent normalization and short native viewport measurement. The current source runner also executes positive/negative NW128 wiring checks; it rejects an unmigrated caller, missing acknowledgment/controller release and a navigation-time service start. These are distinct evidence levels.

No physical device was connected. None of the following has been executed or certified:

1. On an authorized disposable reference target, compare the actual installed signer and perform only a permitted in-place update. Confirm profiles/settings/music/queue/account boundaries survive; never uninstall/reset to make an update work. Carry all NW126/NW127 device blockers forward.
2. From each main tab, reach all offline destinations, then perform at least 30 repeated sibling-route and Back/gesture-Back cycles. Record the real task activity list, source/build ID, final destination and screen captures. Confirm there is at most one retained instance per private sibling, no trapped legacy-root loop and no surprise main-tab reset.
3. Keep a selected public song and then an owned offline song playing while repeating routes, hiding/showing the mini-player, opening/closing transport, returning to the main player and using external controls. Confirm actual audible continuity, selected identity, queue order and no navigation-triggered Play.
4. Recreate/background each route with nonempty queries, pages, filters, scroll and an Audio link draft. Return from the system document picker, exercise an active foreground transfer and a persistent WorkManager job, and verify truthful interrupted/busy/readiness states. Confirm stale search/file results cannot overwrite a later request and consent is not restored as checked.
5. Exercise native recovery and restoration repeatedly, including a transfer/picker round trip. Check native and shared safe-area/IME bounds, landscape/short/narrow/large-type controls, scrolling, focus and TalkBack. Legacy panel styling/typography still needs later refinement and cannot be rated from compilation.

NW128 acceptance is local/source delivery with physical acceptance pending. The 139 original cases remain unchanged; all 80 extended case IDs remain. The next independent source batch is **NW129 — complete local search corpus**. The provisional 6.7/10 rating and public NO-GO remain unchanged until actual product and release evidence warrants a new decision.

## Executed local evidence

- Complete sequential Windows build PASS; 1,515 JVM tests / 190 suites, zero failures/errors/skips; Android app/test-source compilation and assembly PASS.
- 55/55 current source checks PASS, including NW128 negative wiring fixtures. Eight new Android cases compile only.
- Separate subsequent lint PASS: 0 errors, 313 warnings, 17 hints. API 26–28 task-ID fallback fixed; no new lint suppression/baseline.
- Build ID `NW-debug-208A902EBDBBFC18`, canonical source SHA256 `208A902EBDBBFC181507E36D430A6AF7C0D1DC36192542D70158CF5CB159EFA2` (818 inputs).
- APK SHA256 `42168E0715652385A1C4A8C0B82F779B424F0D8E9FE32D6418FB5D271770B28F`; retained NW08 manifest fingerprint `645643898C22A4DD115419E44A4175A166831EA6D68ABE9C255AC0251BE6C827`.
- Unchanged package/version/code/SDK; certificate SHA256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394` independently matches the saved NW127 candidate. This does not compare an actual installed phone.
- No device connected; all four new acceptance rows remain PARTIAL, device status NOT_RUN. Prepared 219-case fixture plan is APK/source-bound and NOT_RUN; preparation performs no import/installation/account operation.

The initial API lint failure and the separate generated KSP cache collision are retained as failed attempts. The final build and lint ran sequentially after the checked generated cache was moved aside. Recovery checkpoints include exact source/APK receipts, 190 JUnit reports, failed/final logs and Git bundles; generated/private artifacts remain outside Git. Hosted CI status is separate from these local results.
