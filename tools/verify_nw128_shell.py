"""NW128 wiring guard; source evidence only, never Android task/device proof."""
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OFFLINE = "app/src/main/java/com/nightwave/player/offline/"
UI = "app/src/main/java/com/nightwave/player/v2/ui/"


def verify(sources):
    for name in ("OfflineLibraryActivity", "FreeAudioCatalogActivity",
                 "CatalogDownloadsActivity", "PermittedAudioDownloadActivity"):
        text = sources[OFFLINE + name + ".kt"]
        assert f"class {name} : NightWaveOfflineShellActivity()" in text, name
        assert "setOfflineContent(frame," in text, name
        assert "NightWaveSafeInsets.bind(frame)" not in text, "duplicate insets"
    base = sources[OFFLINE + "NightWaveOfflineShellActivity.kt"]
    for marker in ("AndroidView(factory = { body }", "bridge?.pauseUiUpdates()",
                   "bridge?.release()", "setContentView(panel)", "NightWaveSafeInsets.bind(panel)",
                   "NightWaveOfflineNavigator.reuseOfflineIntent(intent)"):
        assert marker in base, marker
    navigator = sources[OFFLINE + "NightWaveOfflineNavigator.kt"]
    for forbidden in ("startService(", "ACTION_PLAY", "removeAll", "clearQueue("):
        assert forbidden not in navigator, forbidden
    assert "Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP" in navigator
    main = sources[UI + "NightWaveStudioActivity.kt"]
    for marker in ("NightWaveOfflineNavigator.reuseOfflineIntent(intent)",
                   "NightWaveOfflineNavigator.request(intent, appliedMainNavigation)",
                   'outState.putString("nw128-applied-main", appliedMainNavigation)',
                   "LaunchedEffect(mainNavigationRequest?.id)", "onMainNavigationApplied(request.id)",
                   "nw128-you-offline"):
        assert marker in main, marker
    shell = sources[UI + "NightWaveOfflineShell.kt"]
    assert "StudioMiniPlayer(state, actions," in shell
    assert "onReselect = { onMain(2, false) }" in shell
    assert "onMain(2, true)" in shell
    assert "hiddenForTrack = state.videoUrl" in shell


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    names = [OFFLINE + name + ".kt" for name in (
        "OfflineLibraryActivity", "FreeAudioCatalogActivity", "CatalogDownloadsActivity",
        "PermittedAudioDownloadActivity", "NightWaveOfflineShellActivity", "NightWaveOfflineNavigator")]
    names += [UI + "NightWaveStudioActivity.kt", UI + "NightWaveOfflineShell.kt"]
    sources = {name: (ROOT / name).read_text(encoding="utf-8") for name in names}
    verify(sources)
    if args.self_test:
        for name, good, bad in (
            (OFFLINE + "OfflineLibraryActivity.kt", ": NightWaveOfflineShellActivity()", ": ComponentActivity()"),
            (OFFLINE + "OfflineLibraryActivity.kt", "setOfflineContent(frame,", "setContentView(frame,"),
            (UI + "NightWaveStudioActivity.kt", "onMainNavigationApplied(request.id)", "ignored(request.id)"),
            (OFFLINE + "NightWaveOfflineShellActivity.kt", "bridge?.release()", "ignored()"),
            (OFFLINE + "NightWaveOfflineNavigator.kt", "fun open(", "fun startService("),
        ):
            broken = dict(sources)
            assert good in broken[name]
            broken[name] = broken[name].replace(good, bad)
            try:
                verify(broken)
            except AssertionError:
                continue
            raise AssertionError("Accepted broken wiring: " + good)
    print("PASS NW128 source wiring and negative fixtures; device/back/picker/audio NOT_RUN")


if __name__ == "__main__":
    main()
