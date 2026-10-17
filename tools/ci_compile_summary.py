#!/usr/bin/env python3
"""Create a bounded, privacy-filtered compiler failure summary on GitHub CI.

Reads only the explicitly selected Gradle log; no network, ADB, signing keys,
API tokens, account data, music library or APK output is accessed.
"""
import argparse
import re
from pathlib import Path

MAX_BYTES = 4 * 1024 * 1024
MAX_FINDINGS = 65
PATTERNS = (
    re.compile(r"(?i)(^e:\s|\berror:|Unresolved reference|Type mismatch|"
               r"Overload resolution ambiguity|No value passed for parameter|"
               r"Compilation error|Execution failed for task|"
               r"Could not resolve|Could not find|\bFAILED\b|"
               r"\bFAILURE:|There were failing tests|"
               r"\d+ tests completed, \d+ failed)"),
)

def redact(line: str) -> str:
    line = re.sub(r"(?i)(Authorization:\s*(?:Bearer|Basic)\s+)\S+",
                  r"\1<redacted>", line)
    line = re.sub(r"(?i)([?&](?:token|access_token|refresh_token|"
                  r"api_key|key|secret|client_secret|signature|sig|code)=)[^\s&#]+",
                  r"\1<redacted>", line)
    line = re.sub(r"(?i)((?:client_secret|access_token|refresh_token|"
                  r"id_token)\s*[:=]\s*)\S+", r"\1<redacted>", line)
    line = re.sub(r"(?i)([a-z]:\\Users\\)[^\\]+\\", r"\1<user>\\", line)
    line = re.sub(r"(?i)(/home/)[^/\s]+/", r"\1<user>/", line)
    line = re.sub(r"(?i)(file:///([a-z]:)/Users/)[^/]+/", r"\1<user>/", line)
    return line.strip()[:600]

def summarize(content: str) -> str:
    lines = content.splitlines()
    state = ("SUCCESS reported" if any(x.startswith("BUILD SUCCESSFUL") for x in lines)
             else "FAILURE reported" if any(x.startswith("BUILD FAILED") or x.startswith("FAILURE:")
                                              for x in lines) else "No terminal Gradle result")
    matches = []
    for ln in lines:
        if any(p.search(ln) for p in PATTERNS):
            matches.append(redact(ln))
            if len(matches) >= MAX_FINDINGS:
                break
    return "\n".join([
        "NightWave CI compile report — diagnostic only",
        "Gradle result: " + state,
        "This report is not a device APK or proof of release readiness.",
        "Review redacted text before sharing; sanitization is best effort.",
        "",
        "First compiler/test/dependency findings:",
        *(matches or ["No specific compiler errors were found in the available log.",
                       "A missing log or a failed CI runner is NOT proof of successful compilation."]),
        ""
    ])

def report(input_path: Path, output_path: Path) -> None:
    if input_path.exists() and input_path.is_file():
        with input_path.open("rb") as log:
            log.seek(0, 2)
            log.seek(max(0, log.tell() - MAX_BYTES))
            data = log.read(MAX_BYTES).decode("utf-8", errors="replace")
    else:
        data = ""
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(summarize(data), encoding="utf-8")

def self_test() -> None:
    fake = ("e: file:///C:/Users/example/code/Main.kt:3:1 Unresolved reference\n"
            "Authorization: Bearer FAKE_PRIVATE\n"
            "> Task :app:compileDebugKotlin FAILED\n"
            "FAILURE: Build failed\n"
            "Could not resolve https://example.test/?access_token=FAKE_TOKEN&public=1")
    summary = summarize(fake)
    assert "Unresolved reference" in summary
    assert "FAKE_PRIVATE" not in summary
    assert "FAKE_TOKEN" not in summary
    assert "Users/example" not in summary
    assert "access_token=<redacted>" in summary
    assert "Task :app:compileDebugKotlin FAILED" in summary
    assert "SUCCESS reported" not in summarize("Gradle never started")
    print("PASS: redacted bounded CI compiler-summary self-test")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--log", default="build/nightwave-gradle.log")
    parser.add_argument("--output", default="dist/nightwave-ci-build-summary.txt")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
    else:
        report(Path(args.log), Path(args.output))
        print("Wrote bounded, privacy-filtered NightWave CI diagnostic; no APK installed")
