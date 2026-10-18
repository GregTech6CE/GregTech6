"""Find `material.gregtech.<key>` lang entries whose key belongs to a *different* material.

GT6's material field names are sometimes the local name of another material (the field `Gold` is the
BiomesOPlenty wood "Goldwood", the metal gold is `Au`). `tools/import_gt_lang.py` used to write a
`material.gregtech.<field>` alias for every field, so such an alias could overwrite the entry of the
material that actually owns that key — gold showed up as "Goldwood" in game.

This checks the two lang files against the registry names the port creates and reports every key whose
value does not match the owning material's display name.

Usage: python tools/check_material_lang_collisions.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import io
import json
import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LANG = native_path(ROOT / "src/main/resources/assets/gregtech/lang")
GENERATED = native_path(ROOT / "src/main/java/com/gregtech/gregtech/data/generated")

DISPLAY = re.compile(r'GTMaterialRegistry\.setDisplayName\("([^"]+)",\s*"([^"]*)"\)')


def display_names() -> dict[str, str]:
    out: dict[str, str] = {}
    for path in list(GENERATED.glob("*.java")) + list(
            (native_path(ROOT / "src/main/java/com/gregtech/gregtech/data")).glob("*.java")):
        try:
            text = path.read_text(encoding="utf-8")
        except OSError:
            continue
        for name, display in DISPLAY.findall(text):
            out.setdefault(name, display)
    return out


def main() -> None:
    names = display_names()
    for lang in ("en_us.json", "zh_cn.json"):
        path = LANG / lang
        data = json.loads(path.read_text(encoding="utf-8"))
        bad = []
        for key, value in data.items():
            if not key.startswith("material.gregtech."):
                continue
            material = key[len("material.gregtech."):]
            owner = names.get(material) or names.get(material.capitalize())
            if owner is None:
                continue
            if lang == "en_us.json" and value != owner:
                bad.append((key, value, owner))
            if lang == "zh_cn.json" and not value:
                bad.append((key, value, owner))
        print(f"{lang}: {len(bad)} entries disagree with their material's display name")
        for key, value, owner in bad[:25]:
            print(f"   {key:44} lang={value!r} registry={owner!r}")


if __name__ == "__main__":
    main()
