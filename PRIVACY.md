# NightWave Android privacy information

This page describes NightWave's public Android releases, including 55.7 and its 55.5 predecessor. The [latest release](https://github.com/Nahian2002/NightWave/releases/tag/nightwave-v55.7) is now public.

## On your device

NightWave stores imported audio, saved references, playlists, settings, lyrics and listening history on your device. The app also stores limited diagnostic information for troubleshooting. You can manage files and library items in the app. Android can remove app-private files when you clear app data or uninstall NightWave.

## Online services

- **Find music** sends a submitted keyword search to YouTube through NewPipe. If that search fails or returns no usable results, NightWave can send the same query to an independent Invidious server. These services can receive your search text, IP address and standard request information. Typing suggestions use your local library and search history.
- **Search sources** sends submitted queries to the providers in the selected filter, including Audius, PeerTube through Sepia Search, Radio Browser and Openverse.
- **YouTube references** appear in NightWave search. If you choose to open a reference in YouTube, that service handles its own privacy and account settings.
- **Offline downloads** from supported catalogs or a direct link you provide contact the relevant host. Those hosts can see request information for the files you request.
- **Check for updates** contacts GitHub for the signed update list and the APK you choose to download.
- **Google Drive** is optional. Library backup uses Google Drive app-data access. Personal Drive file workflows request separate permission. NightWave sends library metadata and selected backup options when you request a backup or enable automatic backup. Personal audio is transferred only through the file workflow you choose. Disconnecting your account in NightWave does not delete copies already in your Drive.

NightWave does not operate its own playback or search server. Third-party services handle their own request data and retention. See [Google privacy information](https://policies.google.com/privacy) and the privacy information for the providers you choose.

## Contact

For privacy or security questions, email [jayednahian2002@gmail.com](mailto:jayednahian2002@gmail.com). Include only what is needed to explain the issue.
