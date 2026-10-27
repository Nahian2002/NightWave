#!/usr/bin/env python3
"""Prepare an APK-bound disposable test pack; never connects to a phone/account or imports app data."""
import argparse
import csv
import hashlib
import io
import json
from pathlib import Path
import struct
import tempfile
import wave

ROOT = Path(__file__).resolve().parents[1]
MATRICES = ("docs/NW90_EXPANDED_139_CASE_MATRIX.csv", "docs/quality/NW126_NW145_ACCEPTANCE.csv")
CONFIG = ("app/build.gradle.kts", "app/proguard-rules.pro", "build.gradle.kts",
          "settings.gradle.kts", "gradle.properties", "build-windows.cmd")


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


def source_hash(root):
    files = list((root / "app/src").rglob("*"))
    files += [p for p in (root / "tools").rglob("*") if p.suffix in (".ps1", ".py", ".cmd", ".kt")]
    files += [root / p for p in CONFIG]
    files = [p for p in files if p.is_file()]
    if any(p.is_symlink() or not p.resolve().is_relative_to(root.resolve()) for p in files):
        raise ValueError("Linked/outside source input")
    names = sorted({p.relative_to(root).as_posix() for p in files}, key=lambda s: s.encode("utf-16-be"))
    manifest = "NightWave-source-v1\n" + "".join(sha(root / p) + "  " + p + "\n" for p in names)
    return hashlib.sha256(manifest.encode("utf-8")).hexdigest().upper(), len(names)


def case_mapping(root):
    rows = []
    for index, name in enumerate(MATRICES):
        with (root / name).open(encoding="utf-8-sig", newline="") as stream:
            cases = list(csv.DictReader(stream))
        if len(cases) != (139 if index == 0 else 80):
            raise ValueError("Acceptance inventory changed; review the baseline mapping")
        for case in cases:
            text = (case.get("area", "") + " " + case.get("scenario", "") + " " + case.get("pass_criterion", "")).lower()
            fixtures = ["EMPTY_GUEST", "SMALL_AUDIO_6", "REFERENCE_LIBRARY_10000"]
            if any(word in text for word in ("backup", "drive", "restore", "account", "recovery")):
                fixtures.append("DISPOSABLE_ACCOUNT_ONLY")
            rows.append({"case_id": case["case_id"], "source_matrix": name,
                "source_status": case["status"], "baseline_status": "NOT_RUN",
                "scenario": case["scenario"], "fixture_ids": fixtures,
                "device_profiles": ["FLAGSHIP_REFERENCE", "MIDRANGE_REFERENCE"], "evidence": []})
    if len({r["case_id"] for r in rows}) != 219:
        raise ValueError("Duplicate acceptance identity")
    return rows


def tone(index):
    stream = io.BytesIO()
    with wave.open(stream, "wb") as audio:
        audio.setnchannels(1)
        audio.setsampwidth(2)
        audio.setframerate(48_000)
        period = 96 + index * 8
        # Original deterministic low-amplitude test waveform, not commercial music.
        audio.writeframes(b"".join(struct.pack("<h", ((n % period) - period // 2) * 30)
                                   for n in range(96_000)))
    return stream.getvalue()


def write_pack(output, root, identity):
    if output.exists():
        raise ValueError("Output already exists; use a fresh directory to preserve evidence")
    rows = case_mapping(root)
    output.mkdir(parents=True)
    audio = output / "owned-test-audio"
    audio.mkdir()
    for i in range(6):
        (audio / f"NightWave-fixture-{i + 1:02d}.wav").write_bytes(tone(i))
    invalid = output / "negative-audio-do-not-play"
    invalid.mkdir()
    (invalid / "empty.wav").write_bytes(b"")
    (invalid / "invalid-header.wav").write_bytes(b"NOT_AUDIO_NW126")
    tracks = [{"fixture_id": f"fixture:{i:05d}",
               "title": ("Moonlight Fixture " if i % 2 == 0 else "রাতের গান ") + str(i),
               "creator": f"Fixture creator {i % 40:02d}",
               "synthetic_reference": "https://www.youtube.com/watch?v=NW" + f"{i:09d}",
               "playable": False} for i in range(10_000)]
    (output / "reference-library-harness-only.json").write_text(json.dumps({
        "scope": "Synthetic metadata for a disposable harness; not an app backup/import format. Never fetch these references.",
        "tracks": tracks, "corpus_sizes": [0, 500, 5000, 10000],
        "boundary_cases": [350, 400, 5000, 9999], "playlist_count_target": 30}, ensure_ascii=False), encoding="utf-8")
    profiles = []
    for profile_id, role in (("FLAGSHIP_REFERENCE", "S24 Ultra proposed by existing phone checklist; confirm actual device"),
                             ("MIDRANGE_REFERENCE", "6GB-class Android phone/60Hz profile; record actual model before running")):
        profiles.append({"id": profile_id, "planned_role": role, "status": "AWAITING_DEVICE_CAPTURE",
            "manufacturer": None, "model": None, "android_version": None, "ram_gb": None,
            "refresh_hz": None, "free_storage_bytes": None, "power_mode": None,
            "temperature_condition": None, "network_condition": None, "audio_route": None,
            "volume_condition": None, "font_scale": None, "window_dp": None})
    plan = {"schema": 1, "status": "PREPARED_NOT_EXECUTED", "build": identity,
        "matrix_sha256": {name: sha(root / name) for name in MATRICES},
        "profiles": profiles, "cases": rows,
        "limits": ["No phone/account connected, no app data seeded or modified",
                   "Metadata corpus requires a future disposable UI harness; it does not populate the installed app",
                   "Six two-second WAVs test import/codec paths; use owned long-form audio for latency/soak",
                   "Installed signer and in-place preservation require separate actual device evidence"]}
    (output / "baseline-plan.json").write_text(json.dumps(plan, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    inventory = {p.relative_to(output).as_posix(): sha(p) for p in sorted(output.rglob("*")) if p.is_file()}
    (output / "files-sha256.json").write_text(json.dumps(inventory, indent=2) + "\n", encoding="utf-8")
    return plan


def self_test():
    rows = case_mapping(ROOT)
    assert len(rows) == 219 and all(r["baseline_status"] == "NOT_RUN" for r in rows)
    with wave.open(io.BytesIO(tone(0)), "rb") as audio:
        assert audio.getnframes() == 96_000 and audio.getframerate() == 48_000
    assert tone(0) == tone(0) and tone(0) != tone(1)
    with tempfile.TemporaryDirectory() as directory:
        output = Path(directory) / "pack"
        plan = write_pack(output, ROOT, {"status": "SELF_TEST_ONLY"})
        assert len(plan["cases"]) == 219 and all(p["model"] is None for p in plan["profiles"])
        inventory = json.loads((output / "files-sha256.json").read_text())
        assert all(sha(output / name) == value for name, value in inventory.items())
        try:
            write_pack(output, ROOT, {})
        except ValueError:
            pass
        else:
            raise AssertionError("Existing evidence directory overwritten")
    print("PASS: NW126 219-case mapping, original WAVs, file hashes and no-overwrite fixtures")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--prepare", action="store_true")
    parser.add_argument("--provenance", type=Path)
    parser.add_argument("--apk", type=Path)
    parser.add_argument("--output", type=Path, default=ROOT / "build/nw126-fixture-pack")
    args = parser.parse_args()
    if args.self_test:
        self_test()
    elif args.prepare and args.provenance and args.apk:
        try:
            identity = json.loads(args.provenance.read_text(encoding="utf-8-sig"))
            digest, count = source_hash(ROOT)
            if (identity.get("status") != "PASS_EMBEDDED_SOURCE_MATCH" or
                    identity.get("apkSha256") != sha(args.apk) or
                    identity.get("sourceSha256") != digest or identity.get("inputCount") != count):
                raise ValueError("APK/source/provenance mismatch; prepare from a verified current build")
            write_pack(args.output, ROOT, identity)
            print("PREPARED_NOT_EXECUTED: 219 cases mapped to the verified build; no app/device data changed")
        except (OSError, ValueError, KeyError) as error:
            parser.exit(1, "FAIL: " + str(error) + "\n")
    else:
        parser.error("Use --self-test or --prepare --provenance RECEIPT --apk APK [--output NEW_DIRECTORY]")
