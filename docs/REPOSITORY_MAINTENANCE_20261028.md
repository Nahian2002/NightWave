# Repository maintenance — 28 October 2026

Scope: cumulative NW132 cleanup, information refresh and focused correctness review. Protected parent `aa6a56fd553a297d5aa790f6049a1f4634a11b63`. Branch `maintenance/nightwave-nw132-cleanup`. NW133 remains the next product batch.

The initial inventory found 1,159 tracked files, no tracked generated archives/logs/cache files and no byte-identical tracked-file groups. Source, migrations, historical tools, dated roadmaps, original acceptance matrices, private data and all recovery checkpoints are retained. Disposable local outputs are considered separately from tracked source. Cleanup never extends to personal folders or global SDK/Gradle downloads.

Library previously reused a deep list offset when another collection opened. Its list state is now keyed to the selected collection; the active collection still uses Compose's saveable state across recreation. A new actual-screen regression case compiles for opening a playlist from a deep overview; device execution remains NOT_RUN. The existing structural guard has a broken-fixture check for the new state wiring.

The host benchmark now safely handles absent OS metadata without its nullable-receiver compiler warning. Normal environment values and benchmark scenarios are preserved. This is not a new performance measurement.

Active README/About/build/contribution/readiness/verification/navigation information is aligned with NW132 and this maintenance. The old verification narrative is preserved in VERIFICATION_MATRIX_HISTORY_20261028.md, with its original date; old failed builds are not current failures. Superseded readiness paragraphs remain recoverable in Git. The original acceptance matrices and historical roadmaps are unchanged.

## Completed local cleanup

Removed **354 disposable files / 60.79 MiB**: obsolete root build packs/reports, stale corrupt-KSP residue and clone probes under `build/`; old root build logs; Python bytecode cache; superseded build diagnostics/log; and the previous APK after verifying it exactly matched protected NW132. Every resolved deletion target was inside this workspace, with linked targets rejected. The private cleanup inventory records each selected path, size and file count.

Protected source/APK checkpoint hashes were checked before deletion. Current APK/receipts, app/compiler/incremental caches, SDK/Gradle downloads, every checkpoint, personal folders and installed data are retained. Keeping useful incremental outputs reduces future rebuild time and downloads. No safely disposable tracked source or duplicate was identified; no tracked source was removed just to lower a file count.

See TEST_REPORT.md for exact final validation and artifact identity. This is a focused audit, not a claim that every bug or phone behavior has been verified. NW133 remains next.
