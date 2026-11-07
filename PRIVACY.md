# NightWave Android privacy information

This page describes the NightWave 55.3 APK distributed through GitHub.

## On your device

NightWave stores imported audio, saved references, playlists, settings, lyrics and listening history on your device. The app also stores limited diagnostic information for troubleshooting. You can manage files and library items in the app. Android can remove app-private files when you clear app data or uninstall NightWave.

## Online services

- **Find music** sends a submitted search to Audius, PeerTube through Sepia Search, and Openverse. **Search sources** sends it to the providers in the selected filter, which can also include Radio Browser. Each service can receive your IP address and standard request information. Typing previews use your local library and search history.
- **YouTube references** may be stored as saved links. Search stays in NightWave. If you explicitly open a saved YouTube reference outside NightWave, the receiving app handles its own privacy and account settings.
- **Offline downloads** from supported catalogs or a direct link you provide contact the relevant host. Those hosts can see request information for the files you request.
- **Check for updates** contacts GitHub for the signed update list and the APK you choose to download.
- **Google Drive** is optional. Library backup uses Google Drive app-data access. Personal Drive file workflows request separate permission. NightWave sends library metadata and selected backup options when you request a backup or enable automatic backup. Personal audio is transferred only through the file workflow you choose. Disconnecting your account in NightWave does not delete copies already in your Drive.

NightWave does not operate its own playback or search server. Third-party services handle their own request data and retention. See [Google privacy information](https://policies.google.com/privacy) and the privacy information for the providers you choose.

## Contact

For privacy or security questions, email [jayednahian2002@gmail.com](mailto:jayednahian2002@gmail.com). Include only what is needed to explain the issue.
