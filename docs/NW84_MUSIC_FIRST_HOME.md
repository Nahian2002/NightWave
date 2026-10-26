# NW84 — Music-First Home Redesign

**Owner roadmap:** [NW76–NW90 quality roadmap](CURRENT_CUMULATIVE_ROADMAP.md) · Wave 2, NW84  
**Cumulative parent:** `feature/nightwave-nw83-unified-premium-visual-system`  
**New branch:** `feature/nightwave-nw84-music-first-home`  
**Status:** Source/UI implementation plus synthetic JUnit and Compose instrumentation tests committed; source readback and targeted pure-Kotlin Home layout smoke pass. Complete Android Gradle build, Compose instrumentation and real-device acceptance remain pending for NW90.

## Source changes delivered

### Continue listening: a real song and a real action

- The Home hero is no longer just a decorative greeting when a recent song exists. It shows the existing track's conservatively cleaned title and creator, alongside **genuine source thumbnail artwork** and a visible **Tap to continue** affordance.
- The hero's optional `onContinue` callback is wired to the **existing `onResumeSpotlight` action**, which already decides between keeping the current track, resuming, retrying or selecting the requested track. It does **not** create a new unverified playback path, force autoplay on entering Home, or change saved queue indices.
- When no recent song exists, the hero remains a normal welcoming message and has no Continue action. The original compact quick-action pill is retained, including its legacy source-contract and accessibility test tags.

### For your next listen: music you have saved but haven't just heard

- Added `NightWaveHomeCurationPolicy`: collects bounded genuine canonical public-video references from **Liked** and **playlist** entries, excludes URLs in the recent rail, removes duplicates by URL, and uses the existing `MixDiversityPolicy` for deterministic, day-aware tie rotation and creator diversity.
- Added an artwork-first horizontal **For your next listen** rail using `NightWaveNextListenTile`. Each card keeps the saved artwork URL, shows a cleaned title/creator, a **Liked song** or **From your playlists** provenance label, and separate Play and Options actions.
- Tapping a card plays the actual selected `VideoPreview` in the context of that visible shelf. No synthetic track, verified-artist claim, paid API, backend, credential, tracking call or fabricated cover is introduced.
- No card/shelf is shown if there are no eligible saved songs; users with only recent public references still see their genuine recently played library, without an invented recommendation.

### Less repetition, more music above the fold

- Up to five mix cards are shown from the previously built daily/personal mixes, preserving the original curated track lists and explanatory text. Pure presentation-level duplicate detection suppresses a nearly identical mix when at least 80% of the smaller track set overlaps an already-shown one. **The underlying playlist and listening history are untouched.**
- Home order is **Continue → Quick access → real next-listen shelf (when eligible) → Recently played → personal mixes → compact Library numbers → Explore**. Static collection counters no longer displace the music shelves near the top.
- Mix cards use one full-width column below 390dp or with system font scale >=1.3; otherwise two columns remain. Invalid/non-finite measurements are safely clamped.
- Existing Recently Played play/options/remove, Clear History, Offline, Liked Songs, Search and Library quick actions stay intact.

## Tests and evidence

- `NightWaveHomeCurationPolicyTest.kt`: seven synthetic JVM regression cases for empty state, actual metadata/URLs, recent exclusions, malformed links, deduplication, deterministic day selection, mix card overlap, five-card maximum and no listening-memory dependency.
- `NightWaveHomeLayoutPolicyTest.kt`: three pure JVM cases for screen widths, large-text and non-finite inputs.
- Extended `NightWaveEverydaySurfacesInstrumentedTest.kt`: two Compose scenarios checking independently tappable actual last-played hero and Play/Options actions on the next-listen card.
- `tools/NW84HomeSmoke.kt`, `tools/NW84HomeSmokeModels.kt`: standalone pure-Kotlin smoke fixture for compiling the real Home curation/source policies outside Android, with test-only stubs in `tools`; available for later full exact-source execution.
- `tools/verify_nw84_home.py`: read-only source-contract assertions preserving source identity, actual playback callback routing, shelf order and prior Home controls.
- **Executed now:** `NightWaveHomeLayoutPolicy.kt` compiled via `kotlinc` and passed a local targeted smoke (`NW84_EXACT_HOME_LAYOUT_POLICY_SMOKE_PASS`). Its Git blob hash `21531df4f4102580137df48950538cc068bbf045` matched the exact committed production file. Targeted GitHub readback found all required Home bindings and two instrumentation cases, with balanced braces.
- **Not executed:** entire `NW84HomeSmoke` (production `VideoLink` + diversity + curation), JUnit suite, Android instrumentation, signed APK build, actual rendered Home and playback behavior. The tested responsive policy alone is not proof of a successful app compilation.

## Privacy and rights

- Existing opt-in Listening Memory behavior is preserved in `LocalDailyMixEngine` and `PersonalMixEngine`. The new `NightWaveHomeCurationPolicy` takes **no Listening Memory or analytics input**; it uses saved favorites/recent/playlist *references* already locally available.
- Saved tracks, Google Drive backups, private offline audio, playback/queue engine and download engine formats are unchanged.
- NW77's public-source-rights blocker remains **NO-GO** independently of the Home redesign.

## Final NW90 acceptance checklist for Home

With the complete cumulative source, test a fresh/empty library and a populated one, newly played and resumed songs, an offline-playing track, backgrounds/app restore, small phone width (320dp), 390dp+ devices, landscape/split screen, 1.3×/2× font scales, TalkBack touch target labels, dark/AMOLED/Pearl themes, no artwork or failed thumbnail, library removal/playlist edits, a recently removed song, changing local date, changed Listening Memory consent, and 139-case final acceptance. Verify that tapping a hero or any pick never fabricates a recording, edits saved metadata, loses place, or queues unrelated songs.
