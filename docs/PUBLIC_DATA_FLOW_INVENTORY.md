# NightWave public privacy and data-flow inventory

**Draft audit — 25 October 2026. Must be verified with a real signed build and network capture before publishing a final privacy policy or filling Google Play Data safety.**

| Feature | Data/route visible in source | User action and release validation required |
| --- | --- | --- |
| Offline Library | Selected audio copied to Android app-private files; indexed metadata in local preferences/Room/DataStore | Verify imports, deletion, backup, device-specific permissions and data-clear/uninstall effects |
| Search and online streaming | YouTube/NewPipe extractor, embedded YouTube WebView, and optional independently run Invidious instances | Search text, video IDs, HTTP metadata and device IP may be transmitted to those third parties; measure and disclose actual routes |
| WebView | JavaScript/DOM storage enabled for YouTube page; third-party cookies disabled; file/content access and mixed content disabled | First-party website cookies and third-party web terms still require checking |
| Google Drive app backup | User-approved Google Identity API, `drive.appdata` scope, Drive API HTTPS requests | Verify real sign-in, consent, remote snapshots, retention and disconnect. Disconnect/revoke does not automatically delete remote files |
| Optional personal music locker | Separate user-approved `drive.file` scope; app-created Drive files | Verify user consent, exact stored metadata/audio, cloud deletion and restoration flows |
| Lyrics and recommendations | Imported/edited LRC lyrics and on-device listening preferences/history | Verify local-only assumptions via runtime network capture |
| Crash diagnostics | Capped crash summary stored locally; error type, sanitized app stack frames and timestamps | Confirm it never sends automatically; ask before users share reports |
| Third-party endpoints | `CommunityVideoSearch.kt` contains public Invidious hostnames; Drive uses `www.googleapis.com` | Update endpoint/privacy notices and verify reachability for public users |
| Analytics and ads | No analytics/ad SDK discovered in reviewed Gradle dependencies; not an exhaustive runtime inspection | Do **not** claim zero tracking without network and dependency audit |

## Publication blockers

- No publicly verified support/privacy contact and deletion-request channel currently documented.
- No verified exact data-retention period for **all** remote and local services.
- No Google Play Data safety questionnaire completed from actual device captures.
- No real Google OAuth acceptance for production signing identity.
- Public source includes experimental YouTube offline saving. Resolve platform and recording rights before public distribution.

The final policy must distinguish **NightWave's own processing** from Google/YouTube/Invidious third-party processing and explain that app deletion may not remove remote Google Drive snapshots. Avoid implying that NightWave owns or controls those services.
