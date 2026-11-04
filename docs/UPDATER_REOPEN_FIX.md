# Reopening and on-demand updates — 4 November 2026

Plan before implementation: preserve the cumulative updater and NW142 work; fix the mini-player label's fillMaxHeight inside a minimum-height row, which consumes the Scaffold viewport when a restored track appears. Add a measured Scaffold regression for a restored track and large text. Keep growing text and all transport actions.

Expose Check for updates directly in You / App health while retaining App details. Manual requests always fetch the newest approved stable release without reading or modifying the automatic cooldown. Keep automatic prompts at most once in seven days and bounded failed-check retry. Fix download timeout recovery so Retry is available rather than permanently showing progress. Prevent overlapping check requests. Retain signed metadata, APK hash/package/version/signer verification and explicit Android installation consent.

Validate with targeted regressions, current source checks, Windows builder and sequential lint. Keep actual cold launch/long absence, font/profile, manual/weekly/network/install acceptance NOT_RUN until phone evidence exists. No automatic public APK, feed activation, merge, installation or schedule restart. Recovery and raw evidence stay outside Git.
