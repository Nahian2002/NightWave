# NW145 — Device acceptance and re-rating

## Plan — 4 November 2026

Start from cumulative NW144 #167. Preserve all app/source/APK bytes, data, package/signer and account/consent boundaries. Check current evidence, attach the exact code39 artifact and existing prepared 219-case kit, consolidate the required device/user-study/soak/release gates, and provide a short first-session checklist. Record missing evidence honestly. No connected Android device was found by a read-only ADB inventory (zero devices); no app/device action was performed.

This is acceptance preparation, not another app build. No implementation change or new APK is needed without a demonstrated failure. Keep physical cases NOT_RUN, rating NOT_ASSESSED and release NO_GO until evidence and approvals close. Do not run tests on an unapproved personal profile, install/reset/uninstall, change language/settings/account, merge, activate feed, publish binaries or restart automation. A device profile and explicit test slot are required by the existing NW141 device runbook. Ask for the first phone model/Android version while preparing the checklist independently.
