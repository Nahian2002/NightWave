#!/usr/bin/env python3
"""NW108 exact-source JVM tests. Not Android/official Gradle certification."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import nw102_kotlin_junit_gate as junit

ROOT=Path(__file__).resolve().parents[1]
INPUTS=(
 "app/src/main/java/com/nightwave/player/v2/data/PlaylistNameIdentity.kt",
 "app/src/test/java/com/nightwave/player/v2/data/PlaylistNameIdentityTest.kt",
 "tools/NW108PlaylistNameJUnitRunner.kt",
)

def fingerprints(paths):
 return {str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}

def main():
 parser=argparse.ArgumentParser()
 parser.add_argument("--report",default="build/nightwave-nw108-playlist-locale-junit.json")
 args=parser.parse_args()
 target=ROOT/args.report
 try:
  junit.write_atomic_receipt(target,{"status":"NOT_VERIFIED","executed_tests":0,
                                   "android_compilation":"NOT_VERIFIED"})
 except OSError as exc:
  print("NW108_BLOCKED cannot invalidate old receipt "+str(exc),file=sys.stderr)
  return 2
 if any(shutil.which(x) is None for x in ("javac","java","kotlinc")):
  print("NW108_BLOCKED no Java/Kotlin CLI",file=sys.stderr)
  return 2
 try:
  paths=[ROOT/p for p in INPUTS]
  if any(not p.is_file() for p in paths): raise RuntimeError("missing NW108 source")
  if len(re.findall(r"@Test\b",paths[1].read_text()))!=6:
   raise RuntimeError("NW108 method count drift")
  before=fingerprints(paths)
  with tempfile.TemporaryDirectory(prefix="nightwave-nw108-") as d:
   tmp=Path(d)
   classes=junit.compile_bridge(tmp)
   jar=tmp/"nw108-policy.jar"
   junit.execute(["kotlinc","-classpath",str(classes),*[str(p) for p in paths],
                  "-include-runtime","-d",str(jar)],tmp)
   out=junit.execute(["java","-classpath",os.pathsep.join((str(jar),str(classes))),
                     "NW108PlaylistNameJUnitRunnerKt"],tmp)
   if "NW108_REAL_NAME_POLICY_JUNIT_PASS cases=6" not in out:
    raise RuntimeError("NW108 test execution receipt missing")
   if fingerprints(paths)!=before:raise RuntimeError("NW108 source edited midtest")
   junit.write_atomic_receipt(target,{
    "status":"PLAYLIST_IDENTITY_POLICY_JVM_PASS_ONLY","executed_tests":6,
    "source_sha256":before,"android_compilation":"NOT_VERIFIED",
    "official_gradle_junit":"NOT_RUN","Drive_network_test":"NOT_RUN"})
   print(out.rstrip())
   print("NW108_JVM_SOURCE_RECEIPT "+str(target))
   return 0
 except (OSError,RuntimeError,subprocess.TimeoutExpired) as exc:
  print("NW108_JVM_TEST_FAIL "+str(exc),file=sys.stderr)
  return 2
if __name__=="__main__":
 raise SystemExit(main())
