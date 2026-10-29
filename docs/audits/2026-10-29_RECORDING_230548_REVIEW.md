# NightWave recording 230548 — review only

All 11,935 frames decoded sequentially through 227.4116 seconds; 228 one-second samples, eight contact sheets and selected full-size stills inspected. This is visual sampling, not acoustic audition or frame-performance measurement. Recording SHA256: 0c9001cc1aa3b8407332d042d6bb78b1c9c0ec9e8c1dac104f1636cdeb0aa811. Exact packaged build ID is not exposed. App code and repository remain unchanged during this review.

## Was the download instant?

The first NEW save is near-instant for the network transfer, not literally zero time. The dialog around 2:14 shows:

- Bounded parallel ranges, HTTP/1.1, HTTP 206, seven requests.
- 2,704,719 received bytes, zero retained-start bytes.
- Reported elapsed 507ms: calculated throughput 42.678 Mbps, approximately 5.33 decimal MB/s.
- DNS 3ms, connect 156ms (including TLS 109ms), headers 174ms, summed concurrent receive 443ms, write 2ms, sync 26ms. Concurrent phase totals must not be added to infer wall time.

The save dialog is visible around 2:03; Downloads is active around 2:04–2:06 and Ready by 2:07. Queueing/extraction/verification/UI are separate from the reported transfer interval. The later Blue save is active around 3:19 and Ready by 3:20; its precise elapsed/bytes report is not opened. The popup around 3:32 explicitly says Already verified in Offline Library and represents reuse, not another fresh transfer.

The byte count, zero retained start, live active-to-Ready transition and seven partial-content requests support a fresh network transfer for the first save. They do not establish sustained 100 Mbps, uncached upstream/CDN delivery, every-source performance or a controlled comparison with the different previously measured song.

## Ratings

Subjective demonstrated-usability rating: weighted 8.432/10, reported as about 8.4. Uses the existing 16-area weights (100 total); see review.json. This does not certify hidden reliability/accessibility/security/audio dimensions. Main visual five-part rubric stays 8.8; offline journey now 9.0 versus the previous recorded 7.5. Observed download-speed result merits 9.5 for this run, not a universal speed score.

Theme appearance: AMOLED 9.2, Midnight 9.0, Pearl 8.8, Aurora 8.7. Existing color grades are retained because the palette was not changed. This recording does not show a persistent mood-card clipping or stopped partial-page condition in inspected settled frames.

| Area | Score | Next improvement |
| --- | ---: | --- |
| Identity/coherence | 9.0 | Extend the same finish to legacy tools/dialogs. |
| Compactness | 8.2 | Bring music content above administrative controls. |
| Typography/readability | 8.6 | Reduce technical small print and verify large text/contrast. |
| Navigation | 8.8 | Reduce nested settings/offline decisions; retain every route. |
| Home | 8.8 | Remove repeated instructional copy; keep real distinct collections. |
| Search | 8.0 | Blue public results take roughly 7–8s after typing; improve permitted caching, phase timing and predictable fallback. |
| Artwork/identity | 7.8 | New Blue artwork/creator are present, but older Play Date still shows a placeholder in offline playback; raw long source titles remain in Downloads/Resume/dialogs. |
| Player layout | 9.0 | Clean source art and complete lyrics/queue/route task acceptance. |
| Library | 8.7 | Provide an obvious clear-query action and explain filtered-out saved songs. |
| Queue entry/usability | 8.0 | Up Next is visible; full reorder/remove/Undo/shuffle acceptance is not demonstrated here. |
| Offline journey | 9.0 | Fast saves, corrected guidance and compact navigation improve this considerably; missing art, labels and filtered-empty recovery still need polish. |
| Lyrics availability/onboarding | 6.0 | Online screen still says synchronized lyrics require local audio; complete guided owned-LRC flow before claiming premium coverage. |
| Sound Studio usability | 8.0 | Reduce duplicated preset concepts; clear draft/active state and sticky Apply. Audible A/B untested. |
| Account/recovery usability | 8.3 | Long nested sections and overlapping entry points; simplify without removing recovery tools. Actual restoration untested. |
| Personalization | 9.0 | Four themes switch visibly; device/font/accessibility consistency remains. |
| Copy/states | 8.3 | Shorter user-facing labels and clear saved/streaming/filtered states. |

## Important distinctions

The early saved Play Date exists before the new transfer. Removing it around 0:31 is followed by a paused/cleared offline state around 0:32–0:33, supporting the removal fix for that observed path. Blue is still an ONLINE selection when Offline options opens the Already saved popup around 3:32. Deleting its saved copy later need not stop that separate online stream. Do not report this as the same offline-playback deletion defect without checking the selected source.

After Blue removal the library still has one saved Amake track, while the search field filters by Blue. The 0-of-1 empty result around 3:43 is filtering, not evidence that all saved files vanished. This needs an obvious Clear search / Show all action and cleaned Resume identity.

## Route to 9.5+

1. Finish identity/art continuity across existing saved files, Resume, Downloads, dialogs, queue and notification; use known metadata/cached or embedded art, not invented album identity.
2. Polish offline density and states: primary content earlier, shorter help, clear query/filter recovery, meaningful completed duration/speed for sub-second transfers. No need for another palette rewrite.
3. Improve public search wait/recovery and cached-result continuity without hiding sources/versions or changing URLs.
4. Complete NW137 guided owned lyrics and NW138 Sound Studio active/draft/Apply clarity; then NW139 account/recovery simplification and real disposable restore proof.
5. Run NW140–NW145 phone acceptance: repeated same-source Wi-Fi/mobile transfers, simultaneous playback, pause/restart/process/network/storage failures, airplane-mode play/seek, queue editing/shuffle, TalkBack/font/contrast/narrow/tablet, route controls and two-hour resource soaks. A 9.5 claim requires all core areas at least 9, target task success and no serious unresolved failures.

One fast result is strong progress; it does not automatically prove sound quality, all feature correctness, battery life, accessibility, recovery or a finished 9.5 product. No new APK or code change was made for this review.
