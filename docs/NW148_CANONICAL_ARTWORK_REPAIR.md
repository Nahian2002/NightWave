Current status: source LOCAL_VERIFIED; owner-approved Poco44 installation and scoped artwork/reopen PASS, full device acceptance PARTIAL. See [Poco follow-up](NW148_POCO_CODE44_SMOKE.md). Earlier pending-install statements below are historical.

# NW148 canonical-bound artwork repair — version44

4 November 2026. Resumes the committed code43 findings and repair plan in draft174. The source repair is **LOCAL_VERIFIED_DEVICE_PENDING**. Version44 has not been installed or visually accepted. Code43 and its Samsung findings are retained in [historical receipt](quality/NW148_CODE43_SOURCE_EVIDENCE.json) and [phone findings](NW148_SAMSUNG_CODE43_FINDINGS.md).

## Behavior and limits

Large CoverArt can now try maxres/sd/hq stills for a queried public YouTube thumbnail only when its strict known HTTPS host/path video ID matches an exact independent canonical watch URL. Exactly the public sqp/rs metadata keys and restricted values qualify. Unknown/duplicate/empty/encoded keys, credentials, foreign/encoded paths, fragments, mismatched track IDs, local and third-party sources retain their original behavior. At most four candidates; the exact original queried URL is the final fallback. Small/unbound plans stay unchanged. Stored source addresses, audio quality, cache keys, installed formats, feature/theme and consent boundaries remain intact. Candidate memoization includes track identity; existing completion/session/resize fences remain.

Five new JVM methods cover positive binding/order/bounds/exact fallback, identity mismatch, host/path/credential rejection, ambiguous queries, and unchanged unsigned/local plans. Two added synthetic Coil/Compose methods cover actual production high-resolution candidate decoding and four-attempt exhaustion to the exact queried original. All six artwork Android methods **COMPILED_NOT_RUN**; no helper/test APK installed. The fixture fetcher rejects unregistered synthetic images rather than falling through to real network access.

## Verified host evidence

- Full Windows builder PASS; **1,833 JVM tests /229 suites**, zero failures/errors/skips.
- **90 current source checks PASS_SOURCE_ONLY**. The initial pass identified three old caller-expression guards; updated to the new entry point with retained/extended negative fixtures and the full runner rerun successfully.
- Sequential lint PASS: **0 errors /327 warnings /17 hints**, unchanged ID/severity/message/path inventory from43; no suppression.
- All-ref Gitleaks **3,634 commits/no leaks** at implementation before these result docs.
- Build **NW-debug-48AEEBA1091506CB**, revision **17ba9faeda891493494d427dc0e6f53140583651**; canonical source **48AEEBA1091506CB8430873814586F9D29795174ACC6FF9D85CBAE67CA1D8BF2**,993 inputs.
- APK **A066454E4A958214DD6266341ED5E682FA54969DFD421B77D20879833FBB4022**,28,083,179 bytes. Package com.nightwave.player, version2.0.0-alpha5s/code44,min23/target36. Signer **EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394** matches the actual Poco-pulled code35 APK.
- Eight native entries and one other asset byte-identical to43; APK ZIP entry inventory unchanged.
- Owner-only Drive metadata and complete28,083,179-byte SHA256 readback PASS. Private APK/captures/logs/phone data/bundles remain outside Git. Original219-case pack prepared, not executed.

See [machine-readable identity](quality/NW148_SOURCE_EVIDENCE.json). No feed activation, public binary, main merge, repository settings or schedule change.

## Poco X3 baseline and required phone step

Owner connected the replacement Poco for rating/fix/debug. USB authorization works. Reports M2007J20CI,Android12/API31,1080x2400,density440,font0.81. Actual installed version35 APK SHA256 E99A1884758AFE14C381D2D502440188A75280ABB3F28EE4034BD5B0736A5917 is saved privately; signature matches44. Read-only baseline shows Home and a paused mini-player with visibly soft artwork. This old build is not a rating of44.

MIUI rejects remote taps/Back with `SecurityException: Injecting to another application requires INJECT_EVENTS permission`. Owner reports the separate security-debugging toggle asks for a SIM. Leave it off; no sign-in/consent/system-settings workaround. Screenshots, logs and package inspection work; manual navigation is needed. One early screenshot caught the notification shade while the later UI dump showed the app; that mismatched frame is not an app-rendering failure. A subsequent settled baseline visibly shows Home.

**Code44 installation requires fresh explicit approval** under the saved per-version boundary; approval43 covered the previous Samsung installation only. After approval, attempt a preserving in-place35→44 update with before/after saved-file hashes and pulled installed identity, then ask for manual full-player open/Back/reopen to capture actual dimensions/cache/fallback and visual rendering. OEM confirmation may need owner interaction. No reset/uninstall/data clear/account/library/sound change is authorized.

The Samsung code43 background-only captures recovered after relaunch, cause still unknown. No speculative animation/blur/layering patch, physical-quality pass or100% claim. Original219 cases and NW145 remain unpromoted/PARTIAL. Frozen7.83 review remains historical, full overall NOT_ASSESSED. Source repair permits **NW149** next, while these phone gates stay explicit. The admitted repair fitted the rechecked completion budget; no reset credit consumed or automation restarted.
