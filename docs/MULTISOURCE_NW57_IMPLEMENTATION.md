# NW57 unified experience checkpoint

Continue from the tested NW56 review tip. Keep unresolved NW56 physical cases open. Implement a single Search field with explicit local and provider filters, isolate the existing public search, and keep saved provider links in a separate on-device reference collection. Do not put external URLs in the legacy YouTube queue or imply that references contain downloaded audio.

Audius documents third-party streaming and per-track creator controls. Verify fresh availability, opt-out and access flags and the actual HTTPS redirect chain before enabling any foreground stream. PeerTube and radio remain external when playback rights or transport cannot be established. The release, in-app update feed, library backup and account scopes stay separate from this review.

## Implemented

Search retains the existing Music & public route. Your music uses the existing local index and exact playback or offline entry actions. All permitted combines the local index with separately labeled Audius, PeerTube and radio sections. An individual provider filter submits only to that provider. Selecting a filter or typing alone makes no provider request. Scope changes, edits and leaving Search invalidate pending work. The draft and selected filter survive normal UI recreation, while provider results are requested only after a fresh explicit submission.

Within each provider, exact and strong matches use the existing Unicode-aware local ranking. Provider results retain their exact source ID and link. Same-title studio, live and cover recordings are never merged based on their labels. Each group initially shows up to three results and can expand to all returned results.

PeerTube and radio bookmarks are stored locally as source and address only. They are searchable under Saved links and can be removed. This store is independent of the existing library, queue and Drive backup formats. No stream URL, artwork, title, downloaded audio or account data is persisted for these bookmarks.

## Playback boundary

A bounded Windows probe of the official Audius API returned fresh track eligibility, two HTTPS redirects and HTTP 206 audio. Audius documents creator-controlled API streaming, but the applicable API terms and Open Music License landing pages returned HTTP 403 in direct reads. No new in-app Audius play or bookmark action is exposed in this candidate. PeerTube and radio continue to open their external destinations. Jamendo is still blocked. Third-party playback, playlists, queue restoration and downloads remain outside the verified candidate scope. [Audius API](https://docs.audius.co/api/) · [Creator controls](https://help.audius.co/product/editing-your-release) · [Audius terms](https://audius.co/documents/TermsOfUse.pdf)

## Host candidate

- Exact source revision `d6b2aeffccfcde372d393bd2a2ff31d48da36e69`, canonical source SHA-256 `4E3A17A2357D314E6D64A06C3AB000ED1393AE28DE194CE1D5E61DFA8D0DDAAD`.
- Build `NW-debug-4E3A17A2357D314E`, version code 57, package `com.nightwave.player`, signer `EA737A703B8F116EBC5C8C0A717DDBF3A2A5808ACC9079A8F79CD1B3CD860394`.
- APK size 29,535,317 bytes, SHA-256 `024137D5607D621B7508F553798A7595D0596AB41BF196FCA222BF3EB06B71C1`.
- Windows build, 1,883 JVM tests and 94 current source checks passed. The Android regression fixture compiled. Sequential lint reported 0 errors, 318 warnings and 17 hints. The one additional style suggestion concerns an explicit SharedPreferences commit whose success this code checks. Owner-only Drive permissions and full byte and hash readback passed.

## S24 verification

The verified same-signer v56 to v57 upgrade retained all 33 backed-up files before first launch. The exact installed APK matched the candidate. Your music displayed a local match without a provider submit action. PeerTube displayed live results in its own filter. All permitted showed separate live Audius, PeerTube and Radio Browser groups. A test PeerTube link persisted through app relaunch and was then removed. Existing Search history matched byte for byte.

The v56 regression also passed a generated clip import and audio check, fixture cleanup, and resume and pause of existing offline playback. Remaining NW57 acceptance includes in-app provider playback and queue behavior, broader device and accessibility checks, and instrumentation execution. The public GitHub download and signed update feed stay on v55 and v51 respectively.
