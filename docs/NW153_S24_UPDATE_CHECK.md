# NW153 update check on the Galaxy S24 Ultra

Tested on the Galaxy S24 Ultra (SM-S928B, Android API 37). On version code 46, both manual update entry points and Retry showed the service-unavailable message. The canonical GitHub URL returned HTTP 404 because the signed envelope was present only on review branches.

With owner approval, [PR #179](https://github.com/Nahian2002/NightWave/pull/179) added the existing signed status envelope to `main`. The canonical URL returned HTTP 200 and matched the reviewed bytes (SHA-256 `085EB788E48591B4F65746DC8E7BFB2455D49FB292A0C6D13353E9A002270FFA`). The keys from the installed version 46 and the version 48 candidate accepted the envelope and rejected a modified payload. Version 46 then displayed the publisher's disabled status on the S24.

The owner approved the in-place upgrade to version 48. The private backup of 33 files from preferences, databases and app files matched their original hashes. All 33 also matched after installation and before first launch. The APK pulled from the phone matched the version 48 candidate (SHA-256 `96EC7D48D908DBCE481649D13A8FC6B1ED8AB4D90CBBD3A561EF48F091650B7E`). The guest profile matched again after the smoke test. Three of the 33 files differed after running the app.

On version 48, both **You → Check for updates** and **You → App details → Check for updates** displayed the publisher's disabled status. Home, Search, Library, You, App details and the paused mini-player were observed. The tested process had zero fatal exceptions in the crash log. The phone was left on Home with playback paused.

NW153 remains partial. A live release offer, APK download and installer handoff, the seven-day automatic interval, broader device acceptance and the final rating remain pending. The live feed is in the paused state and advertises no APK.
