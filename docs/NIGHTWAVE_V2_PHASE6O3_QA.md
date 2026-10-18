# NightWave Phase 6O3 — Premium Library Organization

Stacked draft PR on 6O2; stable main and saved data untouched.

## What users get
- Compact **Sort & filter playlists** disclosure on Library root. Default presentation and saved playlist order stay the same.
- Optional playlist-name search with Unicode/diacritic normalization and multi-term matching; playlist order A–Z, Z–A, largest collection first or original saved order; optional Hide empty filter.
- Visible result count states the number shown out of total playlists. An empty result offers an explicit Clear playlist filters action; no library deletion or cleanup occurs.
- Sorted collection cards retain their original name and cover, original stored track metadata and normal navigation into the selected playlist.
- Existing per-playlist filter now uses the 6O1 normalizer, handling accents, punctuation and creator labels without rewriting song metadata. Inside a playlist the user can select Saved order, Title A–Z, Title Z–A or Creator. Ties are deterministic. The viewed order is passed to playback as before.
- Playlist/source snapshots are cached by genuine library revision and optional Room-preview state, so the playback tick does not trigger a new scan of up to 50 playlists on every render. Changing favorites, playlists or preview mode invalidates the cache.
- The UI-only filters are remembered across ordinary composition changes via Compose saveable state. They do not change the legacy persistence format, cloud backups, playback service, app signing or original ordering.

## Limits and honesty
- This phase does NOT introduce timestamps: Most songs sorts by count, not popularity or last-played.
- This does NOT persist sort preferences as user data and does not reorder playlists/songs in storage.
- Public streaming and playback errors remain managed by the same Media3 PlaybackService.
- No new permission, network provider, artificial album data, app-wide mutable database schema or migrated library format.

## Verification
- 19 pure JVM test methods authored for saved-order preservation, name sorting, size ordering, filtered empty collections, accented playlist names, creator/title song sorting, search, deterministic ties and original URL/object preservation.
- CI source checker tools/verify_v2_phase6o3.py added after existing source gates.
- Phase 6I source gate was updated to assert safe sorted presentation rather than relying on removed direct list-template wording.
- Passing source checks is not a compiled APK. Full Gradle/JVM/instrumentation compilation and device checks must be completed before release.

## Deferred phone QA
- 0, 1 and 50 playlists, empty/nonempty collections, long names, Bangla/accented titles, rotated and compact screens, large text and accessibility.
- Filter or sort then play the selected song; verify queue ordering, Previous/Next, playlist add/remove, original library saved order, import/share and Room preview switching.
- Check the signed APK matches the existing app before installing; do not uninstall or erase user music to bypass signature conflicts.
