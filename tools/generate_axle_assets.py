"""Generate and check the 52 original GT6 axle resource sets.

The static north/south models are a fallback. PipeWireClientModels replaces
them with the connected, animated 3D models during client model baking.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / "src/main/resources/assets/gregtech"
GT6 = ROOT.parent / "gregtech6-master/gregtech6-master"
GT6_SOURCE = GT6 / "src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java"
CHINESE = ROOT.parent / "GregTech.lang"

# Resource ID, original multitileentity ID, and original English material name.
FAMILIES = (
    ("brass", 24770, "Brass"),
    ("arsenic_copper", 24780, "Arsenic Copper"),
    ("arsenic_bronze", 24790, "Arsenic Bronze"),
    ("wood", 24800, "Wooden"),
    ("bronze", 24810, "Bronze"),
    ("steel", 24820, "Steel"),
    ("titanium", 24830, "Titanium"),
    ("tungstensteel", 24840, "Tungstensteel"),
    ("iridium", 24850, "Iridium"),
    ("iritanium", 24860, "Iritanium"),
    ("trinitanium", 24870, "Trinitanium"),
    ("trinaquadalloy", 24880, "Trinaquadalloy"),
    ("adamantium", 24890, "Adamantium"),
)
SIZES = ("Small", "Medium", "Large", "Huge")
DIAMETERS = (6, 9, 12, 16)


def resource_sets() -> tuple[dict[Path, str], dict[str, str], dict[str, str]]:
    source = GT6_SOURCE.read_text(encoding="utf-8")
    original_ids = {
        int(match.group(1))
        for match in re.finditer(r'"Axles and Gearboxes",\s*(\d+)', source)
    }
    names = {
        int(match.group(1)): match.group(2)
        for match in re.finditer(r"S:gt\.multitileentity\.(\d+)=(.*)", CHINESE.read_text(encoding="utf-8"))
    }

    files: dict[Path, str] = {}
    english: dict[str, str] = {}
    chinese: dict[str, str] = {}
    for family, base_id, material in FAMILIES:
        for size, (size_name, diameter) in enumerate(zip(SIZES, DIAMETERS, strict=True), 1):
            original_id = base_id + size - 1
            if original_id not in original_ids:
                raise ValueError(f"Original GT6 axle {original_id} is absent")
            if original_id not in names:
                raise ValueError(f"GregTech.lang has no original name for {original_id}")
            resource_id = f"axle_{family}_{size}"
            block_key = f"block.gregtech.{resource_id}"
            english[block_key] = f"{size_name} {material} Axle"
            chinese[block_key] = names[original_id]

            blockstate = {"variants": {"": {"model": f"gregtech:block/{resource_id}"}}}
            files[SHARED_ASSETS / "blockstates" / f"{resource_id}.json"] = pretty(blockstate)

            lo = (16 - diameter) / 2
            hi = (16 + diameter) / 2
            faces = {
                direction: {"texture": f"#{texture}", "tintindex": 0}
                for direction, texture in (
                    ("down", "horizontal"),
                    ("up", "horizontal"),
                    ("north", "rod"),
                    ("south", "rod"),
                    ("west", "vertical"),
                    ("east", "vertical"),
                )
            }
            model = {
                "parent": "block/block",
                "textures": {
                    "rod": "gregtech:block/iconsets/axle",
                    "horizontal": "gregtech:block/iconsets/axle_horizontal",
                    "vertical": "gregtech:block/iconsets/axle_vertical",
                },
                "elements": [{"from": [lo, lo, 0], "to": [hi, hi, 16], "faces": faces}],
            }
            files[SHARED_ASSETS / "models/block" / f"{resource_id}.json"] = pretty(model)
            files[SHARED_ASSETS / "models/item" / f"{resource_id}.json"] = pretty(
                {"parent": f"gregtech:block/{resource_id}"}
            )

    if len(english) != 52 or len(chinese) != 52 or len(files) != 156:
        raise AssertionError("GT6 axle resources must include 13 materials × 4 sizes")
    return files, english, chinese


def pretty(value: object) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2) + "\n"


def lang_bytes(path: Path, translations: dict[str, str]) -> bytes:
    raw = path.read_bytes()
    newline = "\r\n" if b"\r\n" in raw else "\n"
    lines = raw.decode("utf-8").splitlines(keepends=True)
    matching = [i for i, line in enumerate(lines) if re.match(r'  "block\.gregtech\.axle_[^" ]+":', line)]
    if not matching:
        raise ValueError(f"{path} has no axle insertion point")
    first = matching[0]
    lines = [line for i, line in enumerate(lines) if i not in set(matching)]
    added = [f"  {json.dumps(key)}: {json.dumps(value, ensure_ascii=False)},{newline}" for key, value in sorted(translations.items())]
    lines[first:first] = added
    result = "".join(lines)
    parsed = json.loads(result)
    if any(parsed[key] != value for key, value in translations.items()):
        raise AssertionError(f"Generated translations are invalid in {path}")
    return result.encode("utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify assets and translations without changing them")
    args = parser.parse_args()

    files, english, chinese = resource_sets()
    wanted = {path: content.replace("\n", "\r\n").encode("utf-8") for path, content in files.items()}
    wanted[ASSETS / "lang/en_us.json"] = lang_bytes(ASSETS / "lang/en_us.json", english)
    wanted[ASSETS / "lang/zh_cn.json"] = lang_bytes(ASSETS / "lang/zh_cn.json", chinese)

    if args.check:
        missing_or_different = [str(path.relative_to(ROOT)) for path, data in wanted.items() if not path.is_file() or path.read_bytes() != data]
        if missing_or_different:
            raise SystemExit("Axle assets differ: " + ", ".join(missing_or_different[:12]))
    else:
        for path, data in wanted.items():
            path.parent.mkdir(parents=True, exist_ok=True)
            if not path.is_file() or path.read_bytes() != data:
                path.write_bytes(data)

    for path in files:
        model = json.loads(path.read_text(encoding="utf-8") if args.check else files[path])
        for texture in model.get("textures", {}).values():
            namespace, resource = texture.split(":", 1)
            texture_path = ROOT / "src/main/resources/assets" / namespace / "textures" / f"{resource}.png"
            if not texture_path.is_file():
                raise SystemExit(f"Missing axle texture: {texture_path}")
    print("Checked 52 GT6 axles: 156 model/blockstate files, 52 exact Chinese names, all textures present.")


if __name__ == "__main__":
    main()
