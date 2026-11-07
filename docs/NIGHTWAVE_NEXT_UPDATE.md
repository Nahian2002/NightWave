# Next NightWave update

## 1. YouTube playback

Prioritize the Search → tap result → in-app playback flow. A fresh YouTube result should start playback inside NightWave when the video is playable. Keep the existing offline-copy preference and report a clear reason when a result cannot play. Search metadata alone does not satisfy this work.

Before release, verify an unsaved result on the connected phone using the reported “NEONI - Darkside (Lyrics)” case and a second known query. Confirm that audio plays, progress advances, pause and seek work where available, and the selected result matches the audio. Check that a saved local track still plays through its existing path and that an unavailable online result never starts a different song.

## 2. Downloaded music and player

The feedback asks for downloaded files to feel like part of a music player. Use Poweramp as a reference for readable track rows, clear playback state and smooth transitions. Start with Offline music → On device. Give title and artist room to read, make the current track easy to identify, and make the compact controls open the existing full player and queue naturally. Keep import, file options and download tools accessible.

Check imported and downloaded audio separately. Verify the list-to-player transition and return on the S24 Ultra and, when available, the smaller Poco. Check long filenames, an empty library, play and pause, and navigation back to the same list position.

## Owner preview on the S24 Ultra

The preview restores in-app audio for available YouTube streams in the debug build with `-PnightwaveOwnerPlaybackPreview=true`. The public release audit treats this capability as an owner-only preview. The signed stable channel continues to serve 55.5. The release build defaults to disabled playback extraction, and the offline save worker still rejects YouTube downloads.

- Source revision `a70a70bc2f5810b74dc239b777c30bcf0daaca74`
- Build ID `NW-debug-1428A1EEC17BDA39`, package `com.nightwave.player`, version 55.5, Android code 64
- APK SHA-256 `F5910FDB5CFA4A9FD153E20378B76956E14EF8F774DCED7EAE10E269C629E75A`
- Signer SHA-256 `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`
- The installed APK matched the verified candidate and all 25 backed-up files matched before launch.

### Playback checks

A matching “Darkside (Lyrics)” upload and “Ek Paye Nupur Amar” each started from Search in NightWave and advanced and paused on the first preview. A later resume attempt exposed a missing cold-start request. NewPipe can initialize its web client from its service worker or an empty results page. The final preview permits those specific bootstrap requests. [NewPipe source](https://github.com/TeamNewPipe/NewPipeExtractor/blob/v0.26.5/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/YoutubeParsingHelper.java)

On the final preview, “Ek Paye Nupur Amar” started with a cold process, resumed at the 5.775-second bookmark after restarting, and sought from 1:11 to 0:19 with the same track selected. The laptop resolved the same ID three times using the repaired request policy.

### Offline checks

The final preview played an imported WAV, advanced and paused, highlighted its row, and showed the second imported clip in the queue. Closing the queue returned to the same row position. The full player opened the correct track and minimizing it returned to the main Library. Both generated clips and their imported copies were removed, and the original empty offline library was verified.

The Poco, an actual download from a remote source, and a real unavailable YouTube result remain pending on device. The full-player return to the same position in the offline list remains open.

### Host checks

The Windows debug build, Android test compilation, and lint passed. Lint reported 0 errors, 299 warnings and 17 hints. All 95 current source checks and all five YouTube boundary tests passed. The full JVM run passed 1,906 of 1,907 tests. The existing parallel transfer fallback test failed at its active-call cleanup assertion and repeated the failure in a focused run. The transfer engine and that test have no source differences from the public 55.5 source revision.
