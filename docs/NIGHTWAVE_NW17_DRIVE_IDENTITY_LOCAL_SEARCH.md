# NightWave NW17 — Verified Drive identity and imported-audio search

**Base:** NW16 authorized direct-audio downloads (68d4b8a6). **Status: DRAFT / NOT DEVICE-VERIFIED.**

## Google connection root cause and change

Google AuthorizationClient may grant `drive.appdata` and provide an access token without populating the deprecated `AuthorizationResult.toGoogleSignInAccount()`. The previous app erroneously rejected the completed chooser when the email field was absent.

After checking the granted Drive scope and obtaining a short-lived token, NightWave now calls **Drive API `about.get?fields=user(emailAddress)`** on an I/O dispatcher. This endpoint supports the existing `drive.appdata` scope; it does not require Gmail, broad Drive, or profile access. The verified email must agree with the optional legacy response email and, for every existing backup operation, the account stored locally. Both interactive actions and daily backup worker use the same token-bound identity check. Access tokens are never saved.

OAuth configuration (the Android package and SHA-1 for the actual APK signature) and Google Play services must still be correct. Network failure during verification leaves backup disconnected; do not describe a chooser as successful until identity verification succeeds.

## Local imported/downloaded music search

- In Offline Library, query tokens now match across artist/title text in imported filenames (case-insensitive, Unicode-aware), not only an exact substring. Exact/prefix matches rank first; all query terms must match.
- NightWave's main Search tab now lists matching **Downloaded & imported** audio separately from public-video favorites and provider results. Tapping an offline result opens a prefiltered Offline Library for explicit local playback.
- Offline matches are built from the existing private index (max 400 entries), read on tab refresh. No API request, audio upload or library rewrite occurs.
- Empty local queries preserve the prior Offline Library sorting behavior. No attempt is made to invent artist/album metadata if the original audio lacks tags.

## Retained NW16 downloader

Existing direct HTTPS MP3/M4A/FLAC/etc. downloader remains unchanged. This phase is **not** YouTube-to-MP3 and does not change online YouTube playback, APK package name, existing files, backup snapshots or installed user data.

## Acceptance gates

1. Run the repository's full Windows local `build-windows.cmd` on this branch ZIP, including Gradle unit tests and Android test-source compilation; require the resulting APK package/version/signature checks.
2. Device: Google choose account → **Drive connected**; back up a small library, verify upload, disconnect/reconnect, switch to another Google account, and confirm no cross-account restore.
3. Device: airplane mode → search imported track by artist plus partial title, open result from main Search, play from Offline Library. Search Bangla filename, mixed case, and a query with no matches.
4. Download an authorized direct HTTPS MP3 from NW16, then search the new entry and play it offline after reopening. Cancel a second transfer; verify existing library entries remain unchanged.
5. Regression: background playback, YouTube results, queue and previously imported songs remain usable.

**Source checks and JVM-only checks do not prove a passing Gradle build or Android runtime. Do not promote to stable `main` or tell the user to replace a working phone app until those gates pass.**
