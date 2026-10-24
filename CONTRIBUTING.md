# Contributing to NightWave

Thank you for considering a contribution. NightWave is not yet approved for public distribution; contributions should preserve privacy, data durability and the existing music library.

## Before proposing a change

- Use an issue or discussion for a behavior change, or a draft PR for a small reproducible bug fix.
- Do not submit copyrighted recordings, decryption keys, DRM/access-control circumvention, credentials, unauthorized music-download functionality, or scraped personal data.
- Do not ask users to uninstall the app, clear its private storage or replace a signing key to resolve build errors.
- Keep new permissions and remote services opt-in and document exactly where data is transmitted.

## Changes and tests

1. Branch from the reviewed development baseline identified in the release checklist; the historical default `main` is not necessarily current.
2. Keep changes narrowly scoped and update relevant documentation/test cases.
3. Run the offline source check (`python tools/public_release_preflight.py --self-test`, then `python tools/public_release_preflight.py --json`).
4. On Windows run `build-windows.cmd` in a clean checkout. A successful source check is **not** a Gradle or Android device test.
5. Include a summary of build/test outcomes and any untested area. Do not upload personal keys, APKs signed with your private release key, account information, or unreduced logs.

## License and third-party rights

The project includes a GPL-3.0-or-later license declaration. By contributing, you agree that you have rights to license your contribution consistently with the repository's applicable terms. Third-party source, media and design assets require provenance and compatible permissions. **Do not claim that a YouTube URL grants music storage rights.**

## Security

Read [SECURITY.md](SECURITY.md). Report vulnerabilities privately, never in a public issue containing exploitable details.
