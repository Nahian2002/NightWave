# NightWave 55.4 release scope

Android code 63 brings the native YouTube metadata search tested on the S24 Ultra to the stable update channel. NewPipe searches first and an independent Invidious server can handle a failed search. The search transport is limited to metadata. YouTube audio extraction and offline saving stay disabled.

The owner explicitly authorized this release despite the unresolved unofficial-provider finding `NW77-PRIVATE-UNOFFICIAL-SEARCH`. The public-release audit continues to report that finding. This decision applies to 55.4 only. YouTube authorization is unverified. [YouTube Terms of Service](https://www.youtube.com/t/terms?hl=en)

The release requires a same-signer APK, embedded source verification, search and updater checks, public asset readback and a signed stable manifest. The prior signed manifest is retained for recovery. Reverting the feed can stop new offers. Users already on code 63 need a higher version for a corrective update.
