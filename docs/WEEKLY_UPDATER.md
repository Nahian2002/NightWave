# Weekly approved updater — implementation plan

Owner authorizes this one update despite the weekly reserve; the deleted hourly schedule stays deleted. Cumulative parent contains checkpointed NW142; preserve it and repair its stale minimum-height source guard before validation. Do not advance the original roadmap merely because this additional feature is added.

Implement a seven-day automatic check on app opening, displaying only the newest owner-approved release. Download inside the app after explicit Update now; verify signed metadata, HTTPS source, bounded bytes/hash, exact package/version and existing signing certificate before Android installation confirmation. Keep unknown-source approval in Android settings only where the OS requires it. Retain local data and every existing feature/theme. No silent-install promise.

Keep feed signing private keys outside Git. Default the stable feed to disabled: development builds never notify users. Provide an offline approval/signing command; no automated binary publishing, merge or settings changes. Activating a public APK/feed is a separate final owner approval after reviewable artifacts exist. Keep previous verified NW141 until the replacement is fully verified. Identify exact owner-owned NightWave APK files in Drive before deleting superseded copies; do not delete recovery kits or unrelated files.

Run meaningful cooldown/signature/URL/version tests, full current checks, Windows builder then sequential lint. Record APK/source/signer and explicit device installation/recreation/permission/download cases NOT_RUN. Prepare an attached draft PR and verified owner-only delivery. Complete Drive cleanup only against verified obsolete app files.
