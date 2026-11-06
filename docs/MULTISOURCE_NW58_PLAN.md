# NW58 reliability and public update plan

Use the verified NW57 branch as the product baseline. Keep release approval and the live stable feed separate.

- Add Openverse discovery for openly licensed music through its anonymous audio API. Search only after a source submission. Preserve provider attribution and open the original license context on the verified source page. Keep stream and download rights outside this integration.
- Preserve existing sources, local import, playback, queue, bookmarks, themes, data and signing. Validate licensing filters, response bounds, provider isolation, failure and cancellation.
- Run the Windows builder, source checks and sequential lint. Run non-destructive S24 smoke tests when available and report device, performance, restore and distribution gates individually.
- Synchronize the signed stable channel with the already published v55 APK in a separate branch after matching its public bytes and signature. Manual checks bypass the seven-day automatic cadence. Future candidates enter the feed only after approval and validation.
