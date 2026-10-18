#!/usr/bin/env python3
"""Fix UTF-8 corruption introduced by PowerShell bulk replace (truncated em-dash / arrows)."""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import re
from pathlib import Path

ROOT = native_path(Path(__file__).resolve().parents[1] / "src" / "main" / "java")

MOJIBAKE_REPLACEMENTS = {
    "â€?": " - ",
    "â†?": " -> ",
    "â\u0080?": " - ",
    "â\u0086?": " -> ",
    "\ufffd?": " - ",
    "�?": " - ",
}


def fix_text(text: str) -> str:
    for bad, good in MOJIBAKE_REPLACEMENTS.items():
        text = text.replace(bad, good)
    # Truncated UTF-8 em dash / arrow (lone lead bytes)
    text = re.sub(r"\u0080(?=\S)", " - ", text)
    text = re.sub(r"[\u0080-\u009f](?=\S)", " - ", text)
    return text


def fix_bytes(data: bytes) -> bytes:
    data = data.replace(b"\xe2\x80\x3f", b" - ")
    data = data.replace(b"\xe2\x86\x3f", b" -> ")
    data = re.sub(rb"\xe2\x80(?![\x94-\xbf])", b" - ", data)
    data = re.sub(rb"\xe2\x86(?![\x92-\xab])", b" -> ", data)
    data = data.replace(b"\xef\xbf\xbd?", b" - ")
    return data


def main() -> None:
    fixed = 0
    for path in ROOT.rglob("*.java"):
        raw = path.read_bytes()
        cleaned = fix_bytes(raw)
        try:
            text = cleaned.decode("utf-8")
        except UnicodeDecodeError:
            text = cleaned.decode("utf-8", errors="replace")
        text = fix_text(text)
        out = text.encode("utf-8")
        if out != raw:
            path.write_bytes(out)
            fixed += 1
    print(f"Fixed {fixed} Java files under {ROOT}")


if __name__ == "__main__":
    main()
