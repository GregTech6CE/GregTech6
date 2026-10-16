"""Generate GT6 dungeon flower pots for the registered A/B indicator flowers.

The flower list is read from BedrockFlowers, so a new indicator cannot silently
disappear from the potted models or drop tables. Run with --check in CI.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
FLOWERS = ROOT / "src/main/java/com/gregtech/gregtech/content/plant/BedrockFlowers.java"
ASSETS = ROOT / "src/main/resources/assets/gregtech"
DATA = ROOT / "src/main/resources/data/gregtech"


def encoded(value: dict) -> str:
    return json.dumps(value, indent=2, ensure_ascii=False) + "\n"


def emit(path: Path, content: str, check: bool) -> None:
    if check:
        if not path.is_file() or path.read_text(encoding="utf-8") != content:
            raise SystemExit(f"outdated dungeon pot asset: {path}")
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    check = parser.parse_args().check
    ids = re.findall(r'\b[ab]\("(flower_[^"]+)"', FLOWERS.read_text(encoding="utf-8"))
    if len(ids) != 17 or len(set(ids)) != 17:
        raise SystemExit(f"expected the 17 GT6 A/B indicator flowers, found {len(ids)}")
    for flower in ids:
        texture = ASSETS / "textures/block/iconsets" / f"{flower}.png"
        if not texture.is_file():
            raise SystemExit(f"missing GT6 flower texture: {texture}")
        name = f"potted_{flower}"
        emit(SHARED_ASSETS / "blockstates" / f"{name}.json",
             encoded({"variants": {"": {"model": f"gregtech:block/{name}"}}}), check)
        emit(SHARED_ASSETS / "models/block" / f"{name}.json",
             encoded({"parent": "minecraft:block/flower_pot_cross",
                      "textures": {"plant": f"gregtech:block/iconsets/{flower}"}}), check)
        emit(DATA / "loot_tables/blocks" / f"{name}.json", encoded({
            "type": "minecraft:block",
            "pools": [
                {"rolls": 1, "conditions": [{"condition": "minecraft:survives_explosion"}],
                 "entries": [{"type": "minecraft:item", "name": "minecraft:flower_pot"}]},
                {"rolls": 1, "conditions": [{"condition": "minecraft:survives_explosion"}],
                 "entries": [{"type": "minecraft:item", "name": f"gregtech:{flower}"}]},
            ],
        }), check)
    print(f"{len(ids)} GT6 dungeon flower pots {'checked' if check else 'generated'}")


if __name__ == "__main__":
    main()
