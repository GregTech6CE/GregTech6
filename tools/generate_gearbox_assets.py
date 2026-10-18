"""Verify GT6 gearbox tiers and generate the missing Forge 1.20.1 resources.

The original GT6 assets are used for the rotation transformer. Existing
registry IDs are retained; only missing block/item resources are added.
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
ASSETS = native_path(ROOT / "src/main/resources/assets/gt6ce")
ORIGINAL = ROOT.parent / "gregtech6-master/gregtech6-master"
SOURCE = native_path(ORIGINAL / "src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java")
CS = native_path(ORIGINAL / "src/main/java/gregapi/data/CS.java")
CHINESE = ROOT.parent / "GregTech.lang"
REGISTRY = native_path(ROOT / "src/main/java/com/gregtech/gregtech/registry/GTGearboxes.java")
ORIGINAL_TEXTURES = native_path(ORIGINAL / "src/main/resources/assets/gregtech/textures/blocks/machines/transformers/transformer_rotation")
PORT_TEXTURES = ASSETS / "textures/block/machines/transformers/transformer_rotation"
MODEL = ASSETS / "models/block/machine/transformer/rotation_transformer.json"

# Source's first axle numeric ID, material's English name, and port ID suffix.
TIERS = (
    ("wood", 24800, "Wooden", 0),
    ("bronze", 24810, "Bronze", 1),
    ("brass", 24770, "Brass", 1),
    ("arsenic_copper", 24780, "Arsenic Copper", 1),
    ("arsenic_bronze", 24790, "Arsenic Bronze", 1),
    ("steel", 24820, "Steel", 2),
    ("titanium", 24830, "Titanium", 3),
    ("tungstensteel", 24840, "Tungstensteel", 4),
    ("iridium", 24850, "Iridium", 5),
    ("iritanium", 24860, "Iritanium", 6),
    ("trinitanium", 24870, "Trinitanium", 7),
    ("trinaquadalloy", 24880, "Trinaquadalloy", 8),
    ("adamantium", 24890, "Adamantium", 9),
)
OLD_GEARBOXES = {"wood", "bronze", "steel", "titanium", "tungstensteel"}
OLD_TRANSFORMERS = {"bronze", "steel", "titanium", "tungstensteel"}


def java_array(name: str) -> list[int]:
    source = CS.read_text(encoding="utf-8")
    match = re.search(rf"\b{name}\s*=\s*\{{([^}}]+)\}}", source)
    if match is None:
        raise ValueError(f"Missing original GT6 {name}[]")
    return [int(value.strip().removesuffix("L")) for value in match.group(1).split(",")]


def original_line(source: str, numeric_id: int, class_name: str) -> str:
    found = [line for line in source.splitlines()
             if f'"Axles and Gearboxes", {numeric_id},' in line and class_name in line]
    if len(found) != 1:
        raise ValueError(f"Original GT6 {numeric_id} {class_name}: expected one registration")
    return found[0]


def expect_nbt_rate(line: str, key: str, table: str, tier: int, expected: int) -> None:
    match = re.search(rf"{key}\s*,\s*{table}\s*\[\s*(\d+)\s*\]", line)
    if match is None or int(match.group(1)) != tier or java_array(table)[tier] != expected:
        raise ValueError(f"Unexpected GT6 rate: {key}, {table}[{tier}]")


def language(path: Path, entries: dict[str, str]) -> bytes:
    raw = path.read_bytes()
    newline = "\r\n" if b"\r\n" in raw else "\n"
    lines = raw.decode("utf-8").splitlines(keepends=True)
    pattern = re.compile(r'  "block\.gt6ce\.(?:gearbox|rotation_transformer)_[^" ]+":')
    indices = [i for i, line in enumerate(lines) if pattern.match(line)]
    if not indices:
        raise ValueError(f"No insertion point in {path}")
    first = indices[0]
    selected = set(indices)
    lines = [line for i, line in enumerate(lines) if i not in selected]
    lines[first:first] = [f"  {json.dumps(key)}: {json.dumps(value, ensure_ascii=False)},{newline}"
                          for key, value in sorted(entries.items())]
    result = "".join(lines)
    parsed = json.loads(result)
    if any(parsed[key] != value for key, value in entries.items()):
        raise ValueError(f"Invalid generated translations in {path}")
    return result.encode("utf-8")


def expected_model_textures() -> dict[str, dict[str, str]]:
    expected: dict[str, dict[str, str]] = {}
    for layer_name, texture_kind in (("layer0", "colored"), ("layer1", "overlay")):
        expected[layer_name] = {
            face: f"gt6ce:block/machines/transformers/transformer_rotation/{texture_kind}/{original_face}"
            for face, original_face in (("down", "side"), ("up", "side"),
                                        ("north", "front"), ("south", "back"),
                                        ("west", "side"), ("east", "side"))
        }
    return expected


def expected() -> tuple[dict[Path, bytes], dict[str, str], dict[str, str]]:
    source = SOURCE.read_text(encoding="utf-8")
    registry = REGISTRY.read_text(encoding="utf-8")
    volts = java_array("V")
    max_speed = java_array("VMAX")
    registered = {
        match.group(1): (int(match.group(2)), int(match.group(3)))
        for match in re.finditer(r'new Tier\("([^"]+)",\s*[^,]+,\s*(\d+),\s*(\d+)\)', registry)
    }
    translations = {
        int(match.group(1)): match.group(2)
        for match in re.finditer(r"S:gt\.multitileentity\.(\d+)=(.*)", CHINESE.read_text(encoding="utf-8"))
    }
    gearbox_state = (ASSETS / "blockstates/gearbox_bronze.json").read_bytes()
    gearbox_item = (ASSETS / "models/item/gearbox_bronze.json").read_bytes()
    gearbox_model = (ASSETS / "models/block/gearbox_bronze.json").read_bytes()
    transformer_state = (ASSETS / "blockstates/rotation_transformer_bronze.json").read_bytes()
    # An item model inherits with `parent`; `model` belongs to blockstate variants
    # and silently bakes an empty inventory model in both Minecraft versions.
    transformer_item = (json.dumps({"parent": "gt6ce:block/machine/transformer/rotation_transformer"}, indent=2) + "\n").encode("utf-8")
    transformer_model = (ASSETS / "models/block/rotation_transformer_bronze.json").read_bytes()

    files: dict[Path, bytes] = {}
    english: dict[str, str] = {}
    chinese: dict[str, str] = {}
    for suffix, axle_id, material, tier in TIERS:
        if registered.get(suffix) != (tier, max_speed[tier]):
            raise ValueError(f"Port gearbox tier differs from GT6: {suffix}")
        transformer_id = axle_id + 8
        gearbox_id = axle_id + 9
        transformer = original_line(source, transformer_id, "MultiTileEntityTransformerRotation.class")
        gearbox = original_line(source, gearbox_id, "MultiTileEntityGearBox.class")
        expect_nbt_rate(transformer, "NBT_INPUT", "V", tier, volts[tier])
        expected_output = 2 if tier == 0 else volts[tier - 1]
        if tier == 0:
            if not re.search(r"NBT_OUTPUT\s*,\s*2\b", transformer):
                raise ValueError("Wood transformer must output 2 RU")
        else:
            expect_nbt_rate(transformer, "NBT_OUTPUT", "V", tier - 1, expected_output)
        expect_nbt_rate(gearbox, "NBT_INPUT", "VMAX", tier, max_speed[tier])
        if not re.search(r"NBT_MULTIPLIER\s*,\s*4\b", transformer):
            raise ValueError(f"GT6 transformer multiplier differs: {suffix}")
        for original in (transformer, gearbox):
            if not re.search(r"NBT_HARDNESS\s*,\s*6\.0F", original) or not re.search(r"NBT_RESISTANCE\s*,\s*6\.0F", original):
                raise ValueError(f"GT6 {suffix} hardness or blast resistance differs")
        if transformer_id not in translations or gearbox_id not in translations:
            raise ValueError(f"Missing GregTech.lang translation: {suffix}")

        transform_resource = f"rotation_transformer_{suffix}"
        gearbox_resource = f"gearbox_{suffix}"
        english[f"block.gt6ce.{transform_resource}"] = f"{material} Transformer Gearbox"
        english[f"block.gt6ce.{gearbox_resource}"] = f"Custom {material} Gearbox"
        chinese[f"block.gt6ce.{transform_resource}"] = translations[transformer_id]
        chinese[f"block.gt6ce.{gearbox_resource}"] = translations[gearbox_id]
        if suffix not in OLD_GEARBOXES:
            files[ASSETS / "blockstates" / f"{gearbox_resource}.json"] = gearbox_state
            files[ASSETS / "models/item" / f"{gearbox_resource}.json"] = gearbox_item
            files[ASSETS / "models/block" / f"{gearbox_resource}.json"] = gearbox_model
        if suffix not in OLD_TRANSFORMERS:
            files[ASSETS / "blockstates" / f"{transform_resource}.json"] = transformer_state
            files[ASSETS / "models/item" / f"{transform_resource}.json"] = transformer_item
            files[ASSETS / "models/block" / f"{transform_resource}.json"] = transformer_model

    if len(registered) != 13 or len(english) != 26 or len(chinese) != 26 or len(files) != 51:
        raise AssertionError("Expected 13+13 GT6 gearboxes and 17 × 3 new resource files")
    for original_file in ORIGINAL_TEXTURES.rglob("*"):
        if original_file.is_file() and original_file.suffix in {".png", ".mcmeta"}:
            files[PORT_TEXTURES / original_file.relative_to(ORIGINAL_TEXTURES)] = original_file.read_bytes()
    if len(files) != 65:
        raise AssertionError("Original rotation transformer needs 12 PNGs and 2 animation metadata files")
    return files, english, chinese


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    files, english, chinese = expected()
    files[ASSETS / "lang/en_us.json"] = language(ASSETS / "lang/en_us.json", english)
    files[ASSETS / "lang/zh_cn.json"] = language(ASSETS / "lang/zh_cn.json", chinese)
    differences = []
    for path, data in files.items():
        if path.is_file() and path.read_bytes() == data:
            continue
        if args.check:
            differences.append(str(path.relative_to(ROOT)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)

    model = json.loads(MODEL.read_text(encoding="utf-8"))
    for layer, textures in expected_model_textures().items():
        model["children"][layer]["textures"] = textures
    model["textures"]["particle"] = expected_model_textures()["layer0"]["north"]
    desired_model = json.dumps(model, indent=2) + "\n"
    if MODEL.read_text(encoding="utf-8").replace("\r\n", "\n") != desired_model:
        if args.check:
            differences.append(str(MODEL.relative_to(ROOT)))
        else:
            MODEL.write_text(desired_model, encoding="utf-8")

    if differences:
        raise SystemExit("Gearbox resources differ: " + ", ".join(differences[:16]))
    print("Checked 13+13 GT6 gearbox grades, 51 new model files, 14 original textures and exact Chinese names.")


if __name__ == "__main__":
    main()
