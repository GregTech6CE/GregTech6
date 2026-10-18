"""Spot-check the generated books: rows, page counts, and a sample page of each.

``GTBooksGen`` splits long rows into concatenated string literals and escapes the page separator
(U+0001) and newlines, so the rows are reassembled here the way the Java compiler would.

Usage:  python tools/report_gt6_books.py [name-substring]
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import pathlib
import re
import sys

GEN = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/content/book/GTBooksGen.java"))
SEPARATOR = "\u0001"
LITERAL = re.compile(r'"((?:[^"\\]|\\.)*)"')


def unescape(text: str) -> str:
    out = []
    index = 0
    while index < len(text):
        char = text[index]
        if char != "\\":
            out.append(char)
            index += 1
            continue
        nxt = text[index + 1]
        if nxt == "u":
            out.append(chr(int(text[index + 2:index + 6], 16)))
            index += 6
        elif nxt == "n":
            out.append("\n")
            index += 2
        elif nxt == "t":
            out.append("\t")
            index += 2
        else:
            out.append(nxt)
            index += 2
    return "".join(out)


def rows() -> list[str]:
    text = GEN.read_text(encoding="utf-8")
    body = text.split("ROWS = {", 1)[1].split("\n    };", 1)[0]
    out, current = [], []
    for match in LITERAL.finditer(body):
        current.append(unescape(match.group(1)))
        if body[match.end():match.end() + 1] == ",":
            out.append("".join(current))
            current = []
    return out


def main() -> None:
    all_rows = rows()
    wanted = sys.argv[1] if len(sys.argv) > 1 else ""
    print(f"books: {len(all_rows)}")
    for row in all_rows:
        parts = row.split("|", 3)
        pages = parts[3].split(SEPARATOR) if len(parts) > 3 else []
        longest = max((len(page) for page in pages), default=0)
        print(f"  {parts[0]:26} {len(pages):4} pages  longest {longest:3}  "
              f"{parts[1][:40]!r}  by {parts[2][:26]!r}")
        if wanted and wanted.lower() in parts[0].lower() and pages:
            print(f"      page 1: {pages[0][:160]!r}")


if __name__ == "__main__":
    main()
