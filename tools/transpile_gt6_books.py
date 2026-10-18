"""Transpile GT6's written books into the port's book registry.

GT6 fills its books in ``Loader_Books`` through ``UT.Books.createWrittenBook(mapping, title, author,
stack, pages…)`` (20 books); ``Loader_Loot``'s ``gt.books`` table hands 17 of them out as loot
(``ST.book("Manual_…")``). ``UT.createWrittenBook`` writes exactly the vanilla written-book NBT —
``title``, ``author`` and a ``pages`` list, with ``¶`` turned into a newline and pages of 256
characters or more dropped — so the port can keep vanilla ``minecraft:written_book`` stacks and only
has to carry the text over. The Tool Index and Crucible Manual assemble pages through literal
``tBook.add`` calls rather than a ``new String[]``; those two sequences are extracted too.

Special characters are escaped for Java string literals; rows longer than 8 kB are split into
concatenated literals to stay clear of the class-file constant limit.

Usage:  python tools/transpile_gt6_books.py [--check]
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import argparse
import json
import pathlib
import re

GT6 = pathlib.Path(r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java")
SOURCE = GT6 / "gregtech/loaders/b/Loader_Books.java"
OUT = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/content/book/GTBooksGen.java"))
OUT_JSON = pathlib.Path("docs/gt6-books.json")

PAGE_SEPARATOR = "\u0001"
CALL = re.compile(r'createWrittenBook(\()\s*"([^"]+)"\s*,\s*"((?:[^"\\]|\\.)*)"\s*,'
                  r'\s*"((?:[^"\\]|\\.)*)"\s*,', re.S)
PAGES = re.compile(r"new\s+String\s*\[\s*\]\s*\{", re.S)
STRING = re.compile(r'"((?:[^"\\]|\\.)*)"', re.S)
FIXED_PAGE_COUNTS = {"Manual_Tools": 43, "Manual_Smeltery": 42}


def fixed_pages_before(source: str, call_start: int, name: str) -> list[str]:
    """Read literal tBook.add pages, rejecting silent changes to dynamic GT6 data."""
    start = source.rfind("tBook.clear();", 0, call_start)
    if start < 0:
        raise ValueError(f"{name}: no preceding tBook.clear()")
    pages = []
    for line in source[start:call_start].splitlines():
        stripped = line.strip()
        if not stripped.startswith("tBook.add("):
            continue
        match = re.fullmatch(r"tBook\.add\((.*)\);", stripped)
        if match is None:
            raise ValueError(f"{name}: unsupported tBook.add: {stripped}")
        expression = match.group(1)
        literals = STRING.findall(expression)
        remainder = STRING.sub("", expression).replace("+", "").strip()
        if not literals or remainder:
            raise ValueError(f"{name}: non-literal tBook.add: {stripped}")
        pages.append("".join(unescape(value) for value in literals).replace("\u00b6", "\n"))
    if len(pages) != FIXED_PAGE_COUNTS[name]:
        raise ValueError(f"{name}: expected {FIXED_PAGE_COUNTS[name]} literal pages, got {len(pages)}")
    return pages


def balanced_parens(text: str, start: int) -> str:
    """The text inside the parentheses that start at ``start``."""
    depth = 0
    index = start
    while index < len(text):
        char = text[index]
        if char == '"':
            index += 1
            while index < len(text) and text[index] != '"':
                index += 2 if text[index] == "\\" else 1
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return text[start + 1:index]
        index += 1
    return text[start + 1:]


def balanced_braces(text: str, start: int) -> str:
    """The ``{...}`` block starting at ``start`` (which must hold ``{``)."""
    depth = 0
    index = start
    while index < len(text):
        char = text[index]
        if char == '"':                       # skip string literals so braces inside them do not count
            index += 1
            while index < len(text) and text[index] != '"':
                index += 2 if text[index] == "\\" else 1
        elif char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return text[start + 1:index]
        index += 1
    return text[start + 1:]

# Java string escapes -> the characters they stand for (the texts use quotes, newlines and unicode)
JAVA_ESCAPES = {
    "n": "\n", "t": "\t", "r": "\r", "b": "\b", "f": "\f", "s": " ", "0": "\0",
    '"': '"', "'": "'", "\\": "\\",
}


def unescape(text: str) -> str:
    out = []
    index = 0
    while index < len(text):
        char = text[index]
        if char != "\\" or index + 1 >= len(text):
            out.append(char)
            index += 1
            continue
        nxt = text[index + 1]
        if nxt == "u" and index + 5 < len(text):
            try:
                out.append(chr(int(text[index + 2:index + 6], 16)))
                index += 6
                continue
            except ValueError:
                pass
        out.append(JAVA_ESCAPES.get(nxt, nxt))
        index += 2
    return "".join(out)


def escape(text: str) -> str:
    out = []
    for char in text:
        if char == "\\":
            out.append("\\\\")
        elif char == '"':
            out.append('\\"')
        elif char == "\n":
            out.append("\\n")
        elif char == "\t":
            out.append("\\t")
        elif ord(char) < 0x20:
            out.append(f"\\u{ord(char):04x}")
        else:
            out.append(char)
    return "".join(out)


def literal(text: str, limit: int = 8000) -> list[str]:
    """The text as one or more Java string literals (long rows are split for the constant pool)."""
    pieces = [text[i:i + limit] for i in range(0, max(len(text), 1), limit)]
    return ['"' + escape(piece) + '"' for piece in pieces]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify outputs without rewriting them")
    args = parser.parse_args()
    text = SOURCE.read_text(encoding="utf-8", errors="replace")
    books = []
    long_pages = 0
    for match in CALL.finditer(text):
        name, title, author = match.group(2), match.group(3), match.group(4)
        call = balanced_parens(text, match.start(1))
        pages_match = PAGES.search(call)
        if pages_match:
            # Only this call's own array: take the balanced braces right after "new String[] ".
            block = balanced_braces(call, pages_match.end() - 1)
            source_pages = [unescape(raw).replace("\u00b6", "\n") for raw in STRING.findall(block)]
        elif name in FIXED_PAGE_COUNTS:
            source_pages = fixed_pages_before(text, match.start(), name)
        else:
            continue
        pages = []
        for page in source_pages:
            if len(page) >= 256:                    # UT.createWrittenBook drops these as well
                long_pages += 1
                continue
            pages.append(page)
        if pages:
            books.append({"name": name, "title": unescape(title), "author": unescape(author),
                          "pages": pages})

    lines = [
        "package com.gregtech.gregtech.content.book;",
        "",
        "/**",
        " * GT6's written books, transpiled from {@code Loader_Books} by",
        " * {@code tools/transpile_gt6_books.py}: {@code mapping|title|author|page\u0001page…}. The rows",
        " * mirror {@code UT.Books.createWrittenBook}, which is exactly the vanilla written-book NBT",
        " * ({@code title}, {@code author}, {@code pages}), with {@code \u00b6} turned into a line break",
        " * and pages of 256 characters or more dropped like the original does.",
        " */",
        "public final class GTBooksGen {",
        "    private GTBooksGen() {}",
        "",
        "    /** One row per book; fields are separated by {@code |}, pages by U+0001. */",
        "    public static final String[] ROWS = {",
    ]
    for book in books:
        row = "|".join([book["name"], book["title"], book["author"],
                        PAGE_SEPARATOR.join(book["pages"])])
        parts = literal(row)
        lines.append("        " + ("\n                + ".join(parts)) + ",")
    lines += ["    };", "}", ""]
    java_output = "\n".join(lines)
    json_output = json.dumps(
        {"books": len(books), "droppedLongPages": long_pages,
         "names": [b["name"] for b in books],
         "pageCounts": {b["name"]: len(b["pages"]) for b in books}},
        indent=2, ensure_ascii=False) + "\n"
    expected = {OUT: java_output, OUT_JSON: json_output}
    stale = [path for path, content in expected.items()
             if not path.is_file() or path.read_text(encoding="utf-8") != content]
    if args.check:
        if stale:
            print("missing or stale book outputs:", ", ".join(map(str, stale)))
            return 1
        print(f"Verified {len(books)} books in {len(expected)} outputs")
        return 0
    for path in stale:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(expected[path], encoding="utf-8")
    print(f"{len(books)} books transpiled ({long_pages} over-long pages dropped)")
    for book in books:
        print(f"  {book['name']:32} {book['title'][:40]:42} {len(book['pages'])} pages")
    print("updated", ", ".join(map(str, stale)) if stale else "nothing")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
