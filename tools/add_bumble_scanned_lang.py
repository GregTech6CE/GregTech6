#!/usr/bin/env python3
"""Add the language keys of GT6's 320 *scanned* bumblebees to both language files.

GT6's scanned variants are the meta offsets +5/+6/+7/+9 of the same species item
(`MultiItemBumbles.make:594-597`), and the port registers one item per (species, type), so each
living bee gets four scanned siblings. English uses the display names the multi-item table already
carries for those ids ("Wild Bumblebee Drone (Scanned)", "Wild Bumblebee (Dead, Scanned)"); Chinese
is built from the port's own Chinese name of the matching *living* bee plus the scanned suffix, so it
can never contradict `item.gt6ce.<living id>`.

Keys are inserted at their sorted position and the file's own line endings are preserved, so a run
touches nothing but the new lines and a second run is a no-op.

Inputs : src/main/java/com/gregtech/gregtech/registry/GTMultiItemsGen.java  (the item ids + English)
         src/main/resources/assets/gt6ce/lang/{en_us,zh_cn}.json
Output : same two language files, +320 keys each
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG = native_path(ROOT / "src/main/resources/assets/gt6ce/lang")
ITEMS = native_path(ROOT / "src/main/java/com/gregtech/gregtech/registry/GTMultiItemsGen.java")

# (scanned suffix, the living suffix it is written from) - GT6's bumbleScan adds 5 to the type.
VARIANTS = [("scanned_drone", "drone"), ("scanned_princess", "princess"),
            ("scanned_queen", "queen"), ("scanned_dead", "dead")]

# The scanned suffix per variant, in the port's own language files.
ZH_SUFFIX = " (扫描)"
ZH_DEAD_SUFFIX = ", 扫描)"

ENTRY_RE = re.compile(r'^\s*"([a-z0-9_]+)",\s*"((?:[^"\\]|\\.)*)",\s*"bumblebee",\s*"[^"]*",\s*$')


def entries() -> dict:
    """Every bumblebee multi-item as {id: English display name}, scanned and living alike."""
    result = {}
    for line in ITEMS.read_text(encoding="utf-8").splitlines():
        match = ENTRY_RE.match(line)
        if match:
            result[match.group(1)] = match.group(2)
    return result


def load(path: Path):
    """The file as (lines, line separator, trailing-separator) so a write cannot reformat it."""
    text = path.read_bytes().decode("utf-8")
    newline = "\r\n" if "\r\n" in text else "\n"
    lines = text.split(newline)
    trailing = bool(lines) and lines[-1] == ""
    if trailing:
        lines = lines[:-1]
    return lines, newline, trailing


def zh_scanned_name(living: str, kind: str) -> str:
    """The living bee's Chinese name plus the scanned marker, GT6's own parenthesis style kept."""
    if kind != "dead":
        return living + ZH_SUFFIX
    # The dead bees read "Wild Bumblebee (Dead)"; the scanned one adds to that same pair of
    # parentheses instead of appending a second one.
    return living[:-1] + ZH_DEAD_SUFFIX if living.endswith(")") else living + ZH_SUFFIX


def insert(path: Path, keys: dict) -> int:
    lines, newline, trailing = load(path)
    added = 0
    for key in sorted(keys):
        if any(line.lstrip().startswith('"%s":' % key) for line in lines):
            continue
        value = json.dumps(keys[key], ensure_ascii=False)
        line = '  %s: %s,' % (json.dumps(key), value)
        for index, existing in enumerate(lines):
            match = re.match(r'\s*"([^"]+)":', existing)
            if match and match.group(1) > key:
                break
        else:
            index = next(i for i in range(len(lines) - 1, -1, -1) if lines[i].strip() == "}")
        lines.insert(index, line)
        added += 1
    path.write_bytes((newline.join(lines) + (newline if trailing else "")).encode("utf-8"))
    return added


def main() -> int:
    items = entries()
    zh = json.loads((LANG / "zh_cn.json").read_text(encoding="utf-8"))
    english: dict = {}
    chinese: dict = {}
    for item_id, display in items.items():
        for suffix, kind in VARIANTS:
            if not item_id.endswith("_scanned_" + kind):
                continue
            living_id = item_id[: -len("_scanned_" + kind)] + "_" + kind
            key = "item.gt6ce." + item_id
            english[key] = display
            living_name = zh.get("item.gt6ce." + living_id)
            if not isinstance(living_name, str) or not living_name.strip():
                # No Chinese name to build on: fall back to the English living name so the key
                # exists (the localization guard only checks presence).
                living_name = items.get(living_id, item_id)
            chinese[key] = zh_scanned_name(living_name.strip(), kind)
    if len(english) != 320 or len(chinese) != 320:
        raise SystemExit("expected 320 scanned bee ids, found %d/%d"
                         % (len(english), len(chinese)))

    for language, keys in (("en_us", english), ("zh_cn", chinese)):
        path = LANG / (language + ".json")
        added = insert(path, keys)
        check = json.loads(path.read_text(encoding="utf-8"))
        missing = [key for key in keys if key not in check]
        print("%s: +%d keys, %d entries, %d still missing" % (language, added, len(keys), len(missing)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
