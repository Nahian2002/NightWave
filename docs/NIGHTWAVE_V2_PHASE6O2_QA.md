# NightWave 6O2 — Artist & Creator Discovery

Draft phase stacked on 6O1. The stable main branch, original media URLs and local library remain unchanged.

## Implemented
- Search's **Artists & creators from your music** shelf now opens a *local* collection of saved/recent public video tracks by that video's original uploader or conservatively corroborated display creator. No network call occurs on tapping a creator chip.
- **Browse all saved creators** toggles a compact, two-column collection of on-device creator shortcuts. Creators are ranked by number of distinct saved public video references and then by name.
- The creator detail shows the derived source name, track count, tappable saved tracks, existing track option menu, and an explicit return to Discovery. Playing uses the existing public-video queue and exact original VideoPreview objects, not an edited or synthesized link.
- An optional **Search this creator on YouTube** action invokes the existing official-first public search only when tapped. It does not imply a licensed or verified official artist profile.
- Source budget: first 350 saved references; at most 48 creator groups; at most 100 tracks in a playable creator collection to match the current service's public queue bound. If more than 100 exist for one creator, the UI explicitly says so; the saved library remains intact and locally searchable.
- Excludes unsupported/offline IDs and generic unverified uploader labels. Duplicated saved URLs count once, and accent/spacing variants may group under a common key. Creator group labels remain uploader-derived and are **not** claimed to be verified singers/artists.
- All UI state is local and ephemeral except Compose restoration of the current selected source label. No new permission, auth, background I/O, private file enumeration, cache or media migration.

## Verification
- 16 pure JUnit test methods authored for no fake identities, conservative attribution, dedup, generic/offline exclusion, sorting, key normalization, stable track order, bounds and playable identity.
- The new 6O2 source checker is registered after 6O1; legacy 6J assertions updated to reflect direct browsing rather than applying a fake online search query.
- Structural source verification is not Android compilation. Earlier GitHub Actions sometimes completed with zero assigned runner steps. Real build and device QA must pass before release.

## Deferred device review
- Test empty/recent-only/mixed playlists, many creators, duplicate videos, VEVO/Topic variants, long uploader names, 48-creator shelf, 100-track queue and selecting the 100th row.
- Confirm creator chip does not call network and explicit YouTube button does. Check offline imported audio cannot appear as a public saved creator, public playback still resolves normally, Back to discovery works with gestures, and small-screen/tall-font layout remains accessible.
- No official artist/album page, catalog licensing or cloud creator subscription feature is claimed.
