# NW56 implementation checkpoint

Baseline is the exact public v55 tag. The feature branch uses the four roadmap files from main and keeps the existing app and update lineage separate from main. The connected S24 Ultra is on v55. A fresh v55 build and source check run is in progress in the baseline checkout.

Add a separate, opt-in Sources view to Search. It will send only an explicitly submitted query to the selected providers and render results in source-specific sections. Use documented, anonymous, read-only HTTPS routes for Audius, PeerTube via Sepia Search, and Radio Browser. Validate canonical destinations before presenting external actions. In-app provider playback needs separate route and rights verification. Never expose third-party offline controls in this batch. Keep Jamendo blocked until the owner's noncommercial use, registration and app eligibility are established.

Keep state and network work scoped to the view, reject stale responses, enforce bounded transport and timeouts, and test failure isolation. Verify signed in-place upgrade and app-only S24 journeys before advancing the roadmap.
