# NightWave Privacy Policy — DRAFT / NOT YET PUBLISHED FOR USERS

**Last source/contact review:** 28 October 2026
**Publication status:** The statements below are a source-informed draft. The maintainer must verify them against the final APK, third-party SDK/network traffic and Google Play Data safety form, confirm monitoring of the contact below and publish a working web URL **before distribution**.

## Overview

NightWave is an independently developed Android media-player project. Its source supports personally imported audio, local playlists and lyrics, references to online media, optional Google Drive backups and personal music locker features.

The app does not currently require a dedicated NightWave-operated playback server in the reviewed source. **This does not mean that use of NightWave is offline or that no data is sent over the Internet:** Google, YouTube, and independent public search providers may receive requests.

## Information and storage

**Local data.** The app can store imported audio recordings, artwork-related references, playlist and playback information, lyrics, recent listening activity, user preferences, and bounded crash summaries in Android app-private storage. Some of these may be personal. Local audio remains on the device unless the user initiates an available cloud feature.

**Public searches and playback.** Requests for online media may be sent to YouTube and third-party public Invidious search providers. These services may receive search terms, video identifiers, IP addresses and request metadata. In-app YouTube web content may operate according to YouTube/Google privacy practices and set first-party website data.

**Google sign-in and Drive.** Connecting a Google account is optional. The code requests `drive.appdata` for NightWave backup and a separately authorized `drive.file` scope for personal file workflows. With explicit action and permissions, library backup metadata or the user's chosen audio may be transferred to the user's Google Drive. Google controls its own service processing and retention. NightWave's code does not intentionally persist OAuth access tokens in its local backup store; this statement still requires runtime verification.

**Diagnostics.** A limited crash summary with error class and filtered NightWave stack locations is stored locally for troubleshooting. Do not send such a report publicly; it may still reveal environment information.

## User control and deletion

Users can manage the app's locally saved recordings and app features. Deleting an item from the app's offline library should remove the corresponding local file subject to implementation and verification. Uninstalling or clearing app data generally removes the app's private local data and can irreversibly remove personal files.

**Disconnecting Google does not necessarily delete Google Drive files or backups.** Users may need to manage or delete those files within Google Drive or through a future verified in-app deletion flow. This must be tested and accurately explained before publication.

## Retention

Local audio and metadata remain until the app/user deletes them, the Android system removes app data, or a verified cleanup/migration happens. Third-party service retention is governed by the services' own policies. **Exact timelines and deletion controls remain unverified; no claim of automatic deletion is made.**

## Children and international use

No age-targeting, child privacy or region-specific legal compliance claims are made pending Play Console declarations and review. The maintainer must evaluate applicable privacy law, copyright licensing and platform rules before release.

## Contact and privacy rights

**Privacy/security contact:** [jayednahian2002@gmail.com](mailto:jayednahian2002@gmail.com), published with the maintainer's permission. Send only the information needed for your inquiry; never send passwords, authorization tokens or private audio.

A stable public policy URL, actual response/deletion procedures and runtime verification remain required before general app distribution. This repository document is not evidence that a public policy site is already live.

## Third-party privacy policies

- Google: https://policies.google.com/privacy
- YouTube: https://www.youtube.com/howyoutubeworks/our-commitments/protecting-user-data/
- Third-party Invidious instances: individual instance terms/privacy practices must be assessed independently.

**Release gate:** This draft must **not** be treated as a completed, legally reviewed, published privacy policy. It must be placed at a publicly accessible stable URL and linked from the app and Play Console, with accurate final disclosures, before any general-release APK or Play Store submission.

## Weekly update checks

When the approved stable feed is activated, app-opening checks request public release metadata from GitHub at most once per seven-day successful check (failed checks retry at most daily). No library/account identifiers are included; the server receives ordinary network connection information such as IP address. Manual checks and user-approved APK downloads also contact GitHub. Installer approval is handled by Android. Update preferences and verified APK cache are stored privately on device, outside library/Drive backup formats.
