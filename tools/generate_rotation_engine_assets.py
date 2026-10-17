"""Complete and verify the original 13 GT6 RU -> KU rotation engine resources."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / "src/main/resources/assets/gregtech"
ORIGINAL = ROOT.parent / "gregtech6-master/gregtech6-master/src/main/java"
SOURCE = ORIGINAL / "gregtech/loaders/b/Loader_MultiTileEntities.java"
VOLTAGES = ORIGINAL / "gregapi/data/CS.java"
CHINESE = ROOT.parent / "GregTech.lang"
REGISTRY = ROOT / "src/main/java/com/gregtech/gregtech/registry/GTEngines.java"

# Keep the four existing registry names, including tungsten_steel's underscore.
TIERS = (
    ("wood", 24807, "Wooden", 0),
    ("bronze", 24817, "Bronze", 1),
    ("brass", 24777, "Brass", 1),
    ("arsenic_copper", 24787, "Arsenic Copper", 1),
    ("arsenic_bronze", 24797, "Arsenic Bronze", 1),
    ("steel", 24827, "Steel", 2),
    ("titanium", 24837, "Titanium", 3),
    ("tungsten_steel", 24847, "Tungstensteel", 4),
    ("iridium", 24857, "Iridium", 5),
    ("iritanium", 24867, "Iritanium", 6),
    ("trinitanium", 24877, "Trinitanium", 7),
    ("trinaquadalloy", 24887, "Trinaquadalloy", 8),
    ("adamantium", 24897, "Adamantium", 9),
)


def expected() -> tuple[dict[Path, bytes], dict[str, str], dict[str, str]]:
    source = SOURCE.read_text(encoding="utf-8")
    voltage_line = re.search(r"\bV\s*=\s*\{([^}]+)\}", VOLTAGES.read_text(encoding="utf-8"))
    if voltage_line is None:
        raise ValueError("Original GT6 V[] voltage table not found")
    volts = [int(value.strip().removesuffix("L")) for value in voltage_line.group(1).split(",")]
    registry = REGISTRY.read_text(encoding="utf-8")
    registered = {
        match.group(1): (int(match.group(2)), int(match.group(3)))
        for match in re.finditer(r'new RotationTier\("([^"]+)",\s*[^,]+,\s*(\d+),\s*(\d+)\)', registry)
    }
    translations = {
        int(match.group(1)): match.group(2)
        for match in re.finditer(r"S:gt\.multitileentity\.(\d+)=(.*)", CHINESE.read_text(encoding="utf-8"))
    }
    blockstate_template = (SHARED_ASSETS / "blockstates/engine_rotation_bronze.json").read_bytes()
    item_template = (SHARED_ASSETS / "models/item/engine_rotation_bronze.json").read_bytes()
    files: dict[Path, bytes] = {}
    english: dict[str, str] = {}
    chinese: dict[str, str] = {}

    for suffix, original_id, name, tier in TIERS:
        original_line = next((line for line in source.splitlines()
                              if f'"Axles and Gearboxes", {original_id},' in line
                              and "Rotation Engine" in line), None)
        if original_line is None:
            raise ValueError(f"GT6 registration for rotation engine {original_id} not found")
        rates = re.search(r"NBT_INPUT\s*,\s*V\s*\[\s*(\d+)\s*\].*NBT_OUTPUT\s*,\s*V\s*\[\s*(\d+)\s*\]/2", original_line)
        if rates is None or (int(rates.group(1)), int(rates.group(2))) != (tier, tier):
            raise ValueError(f"GT6 {original_id} has unexpected RU/KU rate")
        if "NBT_HARDNESS, 6.0F" not in original_line or "NBT_RESISTANCE, 6.0F" not in original_line:
            raise ValueError(f"GT6 {original_id} has unexpected hardness")
        if registered.get(suffix) != (tier, volts[tier]):
            raise ValueError(f"Port registry differs from GT6 rates for {suffix}")
        if original_id not in translations:
            raise ValueError(f"Missing GregTech.lang translation for {original_id}")

        block_id = f"engine_rotation_{suffix}"
        key = f"block.gregtech.{block_id}"
        english[key] = f"{name} Rotation Engine"
        chinese[key] = translations[original_id]
        files[SHARED_ASSETS / "blockstates" / f"{block_id}.json"] = blockstate_template
        files[SHARED_ASSETS / "models/item" / f"{block_id}.json"] = item_template

    if len(files) != 26 or len(english) != 13 or len(registered) != 13:
        raise AssertionError("The port must register exactly 13 original rotation engines")
    return files, english, chinese


def lang_bytes(path: Path, entries: dict[str, str]) -> bytes:
    raw = path.read_bytes()
    newline = "\r\n" if b"\r\n" in raw else "\n"
    lines = raw.decode("utf-8").splitlines(keepends=True)
    matching = {i for i, line in enumerate(lines) if re.match(r'  "block\.gregtech\.engine_rotation_[^" ]+":', line)}
    if not matching:
        raise ValueError(f"No rotation-engine insertion point in {path}")
    start = min(matching)
    lines = [line for i, line in enumerate(lines) if i not in matching]
    added = [f"  {json.dumps(key)}: {json.dumps(value, ensure_ascii=False)},{newline}"
             for key, value in sorted(entries.items())]
    lines[start:start] = added
    result = "".join(lines)
    parsed = json.loads(result)
    if any(parsed[key] != value for key, value in entries.items()):
        raise AssertionError(f"Invalid generated language entries in {path}")
    return result.encode("utf-8")


def check_textures() -> None:
    for model_name in ("engine_rotation", "engine_rotation_active"):
        model = json.loads((SHARED_ASSETS / "models/block/machine/engine" / f"{model_name}.json").read_text(encoding="utf-8"))
        for layer in model["children"].values():
            for texture in layer["textures"].values():
                namespace, resource = texture.split(":", 1)
                path = ROOT / "src/main/resources/assets" / namespace / "textures" / f"{resource}.png"
                if not path.is_file():
                    raise ValueError(f"Missing rotation-engine texture: {path}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify without writing")
    args = parser.parse_args()
    files, english, chinese = expected()
    files[ASSETS / "lang/en_us.json"] = lang_bytes(ASSETS / "lang/en_us.json", english)
    files[ASSETS / "lang/zh_cn.json"] = lang_bytes(ASSETS / "lang/zh_cn.json", chinese)
    check_textures()
    differences = []
    for path, data in files.items():
        if not path.is_file() or path.read_bytes() != data:
            if args.check:
                differences.append(str(path.relative_to(ROOT)))
            else:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(data)
    if differences:
        raise SystemExit("Rotation engine resources differ: " + ", ".join(differences))
    print("Checked 13 original RU -> KU rotation engines, 26 resource files, exact names and textures.")


if __name__ == "__main__":
    main()
