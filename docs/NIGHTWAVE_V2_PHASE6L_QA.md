# NightWave 6L — More balanced private mixes

Stacked draft on Phase 6K. No merges to main, account migration, changed signing, provider change, streaming catalog, cloud export or new analytics collection.

## Implementation
- Keep existing saved-tracks-only Daily Rotation, Back in Rotation, Rediscover, and opt-in Listening Rotation feeds. Source URLs are validated and bounded before ranking.
- Select up to 12 genuine saved entries per mix with a **soft repeated-creator penalty**. Strong affinity can still win; a library with one creator is not artificially reduced.
- Make equal-score daily ordering actually rotate across UTC-independent local calendar days using deterministic whole-key mixing. No random server calls or generated songs.
- Balance opt-in replay-memory candidates with the same selector; consent-off behavior remains exactly history-blind.
- Allow up to four distinct mix cards when enough meaningful locally saved music exists. Do not display empty or identical mixes.
- Home UI and queue play routing reuse existing components; source metadata and original media URLs are unchanged.

## Verification
- Ten authored new JUnit cases for bounded output, duplicates, daily variation, same-day determinism, strong affinity, soft creator diversity, unknown labels and genuine-only candidates. Existing PersonalMixEngine and PersonalMixMemory suites retained.
- Source regression check: `python3 tools/verify_v2_phase6l.py`.
- GitHub Actions Android compile/JVM/test-source verification enabled for PRs targeting Phase 6K. Do not equate authored tests/source checks with passing tests until a workflow completes successfully.
- User device QA deferred per request. Later verify four-card Home on small/large screens, playback selection, creator diversity, memory consent on/off/erase, date rollover, restored queues, local audio, lyrics and signer continuity.

## Deliberate boundaries
This is private-library personalization, **not** Spotify-scale recommendations. It has no licensed catalog, predictive model, genre/mood inference, third-party remote tracking, offline YouTube download, ads or monetization.
