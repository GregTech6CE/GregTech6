#!/usr/bin/env python3
"""Add the language keys of GT6's tracks (`Loader_Rails:41-72`) to both language files.

English uses GT6's own display names ("Aluminium Booster Track"); Chinese is built from the port's
own material translations plus the family noun, so it can never contradict `material.gregtech.*`.
Keys are inserted at their sorted position without reformatting the rest of the file.

Inputs : src/main/resources/assets/gregtech/lang/{en_us,zh_cn}.json
Output : same files, +30 keys each
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG = native_path(ROOT / "src/main/resources/assets/gregtech/lang")

MATERIALS = ["Aluminium", "Bronze", "Magnalium", "Steel", "StainlessSteel", "Tungsten",
             "Titanium", "Tungstensteel", "TungstenCarbide", "Adamantium"]

FAMILIES = [("track_%s", "%s Track", "%s轨道"),
            ("track_booster_%s", "%s Booster Track", "%s助推轨道"),
            ("track_detector_%s", "%s Detector Track", "%s探测轨道")]


def read(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def zh_material(material: str) -> str:
    """The port's own Chinese name for a material, e.g. {@code material.gregtech.aluminium}."""
    zh = read(LANG / "zh_cn.json")
    value = zh.get("material.gregtech." + material.lower())
    return value.strip() if isinstance(value, str) and value.strip() else material


def key_line(key: str, value: str) -> str:
    return '  %s: %s,' % (json.dumps(key, ensure_ascii=False), json.dumps(value, ensure_ascii=False))


def insert(path: Path, entries: dict) -> int:
    lines = path.read_text(encoding="utf-8").splitlines()
    added = 0
    for key in sorted(entries):
        if any(l.lstrip().startswith('"%s":' % key) for l in lines):
            continue
        line = key_line(key, entries[key])
        # A sorted insert: the first existing key that sorts after the new one.
        index = len(lines)
        for i, existing in enumerate(lines):
            m = re.match(r'\s*"([^"]+)":', existing)
            if m and m.group(1) > key:
                index = i
                break
        else:
            # before the closing brace
            index = next(i for i in range(len(lines) - 1, -1, -1) if lines[i].strip() == "}")
        lines.insert(index, line)
        added += 1
    path.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    return added


def main() -> int:
    for language, build in (("en_us", lambda m, f: f[1] % m),
                            ("zh_cn", lambda m, f: f[2] % zh_material(m))):
        entries = {}
        for material in MATERIALS:
            slug = material.lower()
            for family in FAMILIES:
                entries["block.gregtech." + family[0] % slug] = build(material, family)
        path = LANG / (language + ".json")
        added = insert(path, entries)
        check = read(path)
        missing = [k for k in entries if k not in check]
        print("%s: +%d keys, %d entries, %d still missing" % (language, added, len(entries), len(missing)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
