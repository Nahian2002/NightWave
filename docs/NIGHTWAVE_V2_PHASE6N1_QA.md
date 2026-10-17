# NightWave 6N1 — Offline playback-speed controls

Phase 6N1 is the first low-risk Premium Playback 3.0 increment, stacked on draft 6M2; **main remains unchanged**. This phase does not claim Spotify crossfade, gapless playback, an equalizer, ReplayGain or adaptive audio quality.

## User-facing change
- Imported local audio in expanded Now Playing now offers **Offline playback speed**: 0.75×, 1×, 1.25×, 1.5×, 2×.
- The existing Media3 PlaybackService owns all actual playback-speed changes. The UI sends an explicit speed-choice Intent; the service synchronously persists only one validated integer and applies it only while an offline track is selected.
- The preference is restored when a subsequent offline song plays or a local listening session is explicitly resumed.
- Public streams always return to **1×** when switching from offline to public playback. The speed selector is hidden for public songs.
- Service creation does not load files, restart playback, access the network or autoplay.

## Safety and boundaries
- No changes to the app ID, version code, signing, private media ownership, cloud backup format, public queue, sleep timer, or history/privacy consent.
- No new audio processor, cross-player mixing, persistent audio source URL, or extra Android permission.
- Ten unit test methods authored for speed presets, fallback, exact labels and limits; Phase 6N1 source gate added to the GitHub Android workflow.
- GitHub Actions in this repository has previously failed with zero runner allocation; **a successful Android compilation is not assumed**.

## Deferred phone tests
- Play different imported MP3/FLAC/Opus files (where available), change speed, and verify playback timing, pitch, progress, next-track controls, pause, notifications, headset/Bluetooth audio and background behavior.
- Switch offline to public to offline and confirm 1× public playback and remembered offline speed.
- Try service restart then manual offline resume, queued song transitions, a sleep timer, rotation, narrow screens and large font scaling.
- Check signed APK identity with check-device-update.cmd before installing over existing saved data. Never uninstall or erase saved NightWave content to bypass a signature mismatch.

## Deferred features
Crossfade requires two synchronized audio sources and buffering; public sources are currently resolved individually. Gapless transitions and device-specific EQ/loudness normalization need deeper Media3 design and actual physical-device QA. The plan is to evaluate these separately rather than add speculative audio code.
