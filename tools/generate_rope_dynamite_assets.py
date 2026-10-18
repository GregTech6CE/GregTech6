"""Register the five new rope/dynamite tool blocks' assets and language keys.

The blocks reuse GT6's shared greyscale models (`block/machine/tool/rope`, `.../dynamite`), so each
new id only needs a blockstate that points at that model plus an item model that parents it — the
material colour comes from the block colour handler (`GTToolBlocks` + `GregTechClient`).

Usage:  python tools/generate_rope_dynamite_assets.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import json
import pathlib
import shutil

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = native_path(ROOT / "src/main/resources/assets/gregtech")
LANG = ASSETS / "lang"

# port id -> (existing blockstate to copy, english name, chinese name)
NEW = {
    "rope_silk": ("rope", "Silk Rope", "丝绳"),
    "rope_grass": ("rope", "Grass Rope", "草绳"),
    "rope_vine": ("rope", "Vine Rope", "藤蔓绳"),
    "rope_plastic": ("rope", "Plastic Rope", "塑料绳"),
    "rope_steel": ("rope", "Steel Rope", "钢绳"),
    "boomstick": ("dynamite", "Boomstick", "爆竹"),
    "strong_dynamite": ("dynamite", "Strong Dynamite", "强力炸药"),
}


def write(path: pathlib.Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    if not path.exists() or path.read_text(encoding="utf-8") != text:
        path.write_text(text, encoding="utf-8")


def main() -> None:
    for new_id, (template, english, chinese) in NEW.items():
        source = ASSETS / "blockstates" / f"{template}.json"
        state = json.loads(source.read_text(encoding="utf-8"))
        for variant in state["variants"].values():
            variant["model"] = variant["model"].replace(f"/{template}", f"/{template}")
        write(ASSETS / "blockstates" / f"{new_id}.json", state)
        write(ASSETS / "models/item" / f"{new_id}.json",
              {"parent": f"gregtech:block/machine/tool/{template}"})

    for name, key_en, key_zh in (("en_us.json", "english", "chinese"), ("zh_cn.json", "chinese", "english")):
        path = LANG / name
        data = json.loads(path.read_text(encoding="utf-8"))
        for new_id, (_template, english, chinese) in NEW.items():
            data.setdefault(f"block.gregtech.{new_id}", english if name == "en_us.json" else chinese)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
                        encoding="utf-8")
    print(f"wrote {len(NEW)} blockstates, item models and lang keys")


if __name__ == "__main__":
    main()
