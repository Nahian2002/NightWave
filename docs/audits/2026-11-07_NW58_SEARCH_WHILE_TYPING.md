# NW58 Search while typing

## What changed

- Search shows up to four word completions and two matching songs directly under the focused field. Recent explicit searches can contribute completions. Saved references and imported audio supply the matching songs.
- A completion finishes the current word or adds one word and places the cursor at the end. The existing search and playback actions remain separate.
- The preview scans prepared on-device metadata off the UI thread. It skips the full search’s deliberate 120 ms debounce and retains only a few ranked rows. Current query, index and focus fence stale results.
- Typing sends no online search request. Online search still requires the existing submit action. Typed text and tapped completions do not write search history.

## Evidence

On the Poco X3, Android 12, entering `bl` showed the `blue` completion and the matching yung kai song above the keyboard before submission. The capture also exposed an empty-library message below a real preview match. The final change suppresses that contradictory state. The original capture observed the preview and the separate online submit action. A successful completion tap was not captured.

The final APK was installed in place and read back on the Poco. All 19 backed-up files matched before first launch.

- Final source `c59a79e29f7ff02cdd3bee4f0b55b8b70a4a24a8`, build `NW-debug-9B1D398F3A6AB129`.
- APK SHA-256 `859E7365A4144F116627ACE299E61D580639FBB3B9DF64BDAF45903FE4D6A995`. Package `com.nightwave.player`, version code 58, trusted signer verified.
- Windows builder, 1,902 JVM tests, 94 source checks and sequential lint passed. Lint reported zero errors, 319 warnings and 17 hints. Android test sources compiled.
- [Draft PR #195](https://github.com/Nahian2002/NightWave/pull/195) tracks the source. The public update channel remains on [version 55](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55).
