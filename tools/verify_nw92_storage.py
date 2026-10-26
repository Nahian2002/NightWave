#!/usr/bin/env python3
"""NW92 source integration check: not an Android build or device certificate."""
from pathlib import Path
R = Path(__file__).resolve().parents[1]
read = lambda p: (R / p).read_text(encoding="utf-8")
a = read("app/src/main/java/com/nightwave/player/offline/OfflineAudioLibrary.kt")
b = read("app/src/main/java/com/nightwave/player/offline/OfflineFileRegistration.kt")
c = read(".github/workflows/android.yml")
d = read("app/src/main/java/com/nightwave/player/v2/locker/NightWaveLockerActivity.kt")
scope = a.split("fun importVerifiedCloud(", 1)[1].split("fun delete(", 1)[0]
assert "OfflineFileRegistration.copyVerifiedAndRegister(" in scope
assert "OfflineLibraryCapacity.canStage(root.usableSpace, remote.bytes)" in scope
assert "LockerAudioPolicy.hashes(candidate)" in scope
assert 'prefs.edit().putString("items", encode(previous + saved)).commit()' in scope
assert "Files.move(file.toPath(), finalFile.toPath())" not in scope
assert "finalFile.delete()" not in scope
for snippet in ("staged.inputStream().use", "output.fd.sync()", "verifyCopiedFile(pending)",
                "Files.move(pending.toPath(), destination.toPath())", "check(registerIndex())",
                "if (promoted) destination.delete()"):
    assert snippet in b
assert "staged.delete()" not in b and "staged.renameTo(" not in b
assert "checkpoints.discard()" in d
assert "if (!staging.exists()) checkpoints.discard()" in d
assert "NW92OfflineFileRegistrationSmoke.kt" in c
assert "python3 tools/verify_nw92_storage.py" in c
print("NW92_STORAGE_SOURCE_WIRING_PASS")
