"""Copy GT6 logistics storage art and generate the 60 Forge model references.

The original texture files are copied byte for byte; this script only adapts
the JSON resource paths for the port. Use --check to verify generated output.
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import argparse
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
GT6 = ROOT.parent / "gregtech6-master" / "gregtech6-master"
ASSETS = native_path(ROOT / "src/main/resources/assets/gregtech")
TEXTURE_SOURCE = native_path(GT6 / "src/main/resources/assets/gregtech/textures/blocks/machines/massstorage/logistics")
TEXTURE_TARGET = ASSETS / "textures/block/machines/massstorage/logistics"


def expected_files() -> dict[Path, bytes]:
    metals = (native_path(ROOT / "src/main/java/com/gregtech/gregtech/registry/GTStorageMetals.java")).read_text(encoding="utf-8")
    suffixes = re.findall(r'new Spec\("([a-z_]+)"', metals)
    if len(suffixes) != 60 or len(set(suffixes)) != 60:
        raise ValueError(f"expected 60 unique GT6 metalset suffixes, found {len(suffixes)}")

    expected: dict[Path, bytes] = {}
    for group in ("colored", "overlay"):
        for face in ("bottom", "top", "front", "back", "side"):
            source = TEXTURE_SOURCE / group / f"{face}.png"
            expected[TEXTURE_TARGET / group / source.name] = source.read_bytes()

    standard = json.loads((ASSETS / "models/block/machine/storage/mass_storage.json").read_text(encoding="utf-8"))
    logistics = json.loads(json.dumps(standard).replace("massstorage/standard/", "massstorage/logistics/"))
    model_path = "gregtech:block/machine/storage/logistics_mass_storage"
    expected[ASSETS / "models/block/machine/storage/logistics_mass_storage.json"] = (
        json.dumps(logistics, indent=2, ensure_ascii=False) + "\n"
    ).encode("utf-8")
    facing = {
        "variants": {
            "facing=north": {"model": model_path},
            "facing=east": {"model": model_path, "y": 90},
            "facing=south": {"model": model_path, "y": 180},
            "facing=west": {"model": model_path, "y": 270},
        }
    }
    blockstate = (json.dumps(facing, indent=2) + "\n").encode("utf-8")
    item_model = (json.dumps({"parent": model_path}, indent=2) + "\n").encode("utf-8")
    for suffix in suffixes:
        name = f"logistics_mass_storage_{suffix}.json"
        expected[ASSETS / "blockstates" / name] = blockstate
        expected[ASSETS / "models/item" / name] = item_model
    return expected


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    files = expected_files()
    wrong = [path for path, data in files.items() if not path.is_file() or path.read_bytes() != data]
    if args.check:
        if wrong:
            print(f"{len(wrong)} missing or stale logistics storage assets")
            for path in wrong[:10]:
                print(path.relative_to(ROOT))
            return 1
        print(f"Verified {len(files)} logistics storage assets")
        return 0
    for path in wrong:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(files[path])
    print(f"Wrote {len(wrong)} of {len(files)} logistics storage assets")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
