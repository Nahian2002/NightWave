#!/usr/bin/env python3
"""Conservative per-line Kotlin string/char lexer for regression detection.

Complements—not replaces—kotlinc and Gradle. Ignores line/block comments,
escaped characters and multiline triple-quoted strings. Detects the broken
Home greeting literal which prevented NW21 from compiling.
"""
from __future__ import annotations

def suspicious_quote_lines(source: str) -> list[int]:
    issues: list[int] = []
    block_comment = False
    triple_string = False
    triple_start = 0
    for line_no, line in enumerate(source.splitlines(), 1):
        i = 0
        quoted = False
        char_literal = False
        while i < len(line):
            ch = line[i]
            if block_comment:
                end = line.find("*/", i)
                if end < 0:
                    break
                block_comment = False
                i = end + 2
                continue
            if triple_string:
                end = line.find('"""', i)
                if end < 0:
                    break
                triple_string = False
                i = end + 3
                continue
            if quoted and ch == "\\":
                i += 2
                continue
            if char_literal and ch == "\\":
                i += 2
                continue
            if not quoted and not char_literal:
                if line.startswith("//", i):
                    break
                if line.startswith("/*", i):
                    block_comment = True
                    i += 2
                    continue
                if line.startswith('"""', i):
                    triple_string = True
                    triple_start = line_no
                    i += 3
                    continue
            if ch == '"' and not char_literal:
                quoted = not quoted
            elif ch == "'" and not quoted:
                char_literal = not char_literal
            i += 1
        if quoted or char_literal:
            issues.append(line_no)
    if triple_string and triple_start not in issues:
        issues.append(triple_start)
    return issues

def self_test() -> None:
    bad = 'Text("Welcome,  + firstName + " · Your music, your pace",'
    good = 'Text("Welcome, " + firstName + " · Your music, your pace",'
    assert suspicious_quote_lines(bad) == [1]
    assert suspicious_quote_lines(good) == []
    assert suspicious_quote_lines('val q = "say \\"hi\\""') == []
    assert suspicious_quote_lines('val url = "https://example.com/a" // a " comment') == []
    assert suspicious_quote_lines('val x = \'"\'') == []
    assert suspicious_quote_lines('/* " not code\n */ val x = "valid"') == []
    assert suspicious_quote_lines('val x = """raw\n" within triple\nmultiline"""') == []
    assert suspicious_quote_lines('val x = "unterminated') == [1]
    assert suspicious_quote_lines('val x = """unterminated') == [1]
    print("PASS: NW22 Kotlin quote lexer detects real NW21 greeting defect")

if __name__ == "__main__":
    self_test()
