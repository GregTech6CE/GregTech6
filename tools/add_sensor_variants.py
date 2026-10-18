"""Adds the four GT6 sensors the port was missing (§108).

GT6 registers 20 sensor machines in ``Loader_MultiTileEntities.sensors`` (:1979-1998). The port had 16
block ids for them: the four weight-o-meters of :1988-1991 collapsed into one (``sensor_weightometric``,
which is GT6's *Heavy* one - tonnes, maximum 65535) and the TPS meter of :1992 was missing entirely.

This tool writes only assets; the Java side is ``GTSensors.registerAll`` / ``SensorBlockEntity.Kind``:

* copies the six textures of each new sensor (colored + overlay x front/back/side) out of the GT6
  sources into the port's ``textures/block/...`` tree (GT6 uses ``textures/blocks/...``),
* clones the existing heavy weightometer block model for each new sensor, rewriting the texture folder,
* writes the blockstate (six facings) and the item model,
* inserts the English and Chinese names into the two language files **line by line** in sorted key
  order, so the rest of those files stays byte for byte identical.

Idempotent: a second run writes nothing and reports ``0`` for every counter.

Usage: python tools/add_sensor_variants.py [--check]
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import argparse
import io
import json
import os
import sys

SRC_RES = os.path.join(str(native_path("src")), "main", "resources")
ASSETS = os.path.join(SRC_RES, "assets", "gregtech")
TEXTURE_ROOT = os.path.join(ASSETS, "textures", "block", "machines", "redstone", "sensors")
MODEL_ROOT = os.path.join(ASSETS, "models", "block", "machine", "sensor")
BLOCKSTATE_ROOT = os.path.join(ASSETS, "blockstates")
ITEM_MODEL_ROOT = os.path.join(ASSETS, "models", "item")
LANG_ROOT = os.path.join(ASSETS, "lang")

# GT6's own texture tree; the mod's source tree first, the resource pack as the fallback.
GT6_TEXTURE_ROOTS = [
    os.path.join("F:\\", "Dev", "GregTech6", "gregtech6-master", "gregtech6-master",
                 str(native_path("src")), "main", "resources", "assets", "gregtech", "textures", "blocks",
                 "machines", "redstone", "sensors"),
    os.path.join("F:\\", "Dev", "GregTech6", "GT6resourcepack", "gt6", "resources", "assets",
                 "gregtech", "textures", "blocks", "machines", "redstone", "sensors"),
]

# (texture folder in GT6, port block id, English name, Chinese name, GT6 registration line)
SENSORS = [
    ("lightweightometer", "sensor_weightometer_light", "Light Weight-O-Meter Sensor",
     "轻型称重计", "Loader_MultiTileEntities:1988"),
    ("mediumweightometer", "sensor_weightometer_medium", "Medium Weight-O-Meter Sensor",
     "中型称重计", "Loader_MultiTileEntities:1989"),
    ("superheavyweightometer", "sensor_weightometer_super_heavy", "Super Heavy Weight-O-Meter Sensor",
     "超重型称重计", "Loader_MultiTileEntities:1990"),
    ("tpsmeter", "sensor_tpsmeter", "TPS Sensor", "TPS 计", "Loader_MultiTileEntities:1992"),
]

FACES = ("front", "back", "side")
FOLDERS = ("colored", "overlay")

TEMPLATE_MODEL = os.path.join(MODEL_ROOT, "weightometric.json")
TEMPLATE_TEXTURE_FOLDER = "heavyweightometer"
TEMPLATE_BLOCKSTATE = os.path.join(BLOCKSTATE_ROOT, "sensor_weightometric.json")

FACINGS = ["north", "east", "south", "west", "up", "down"]
ROTATIONS = {
    "north": {},
    "east": {"y": 90},
    "south": {"y": 180},
    "west": {"y": 270},
    "up": {"x": 270, "y": 180},
    "down": {"x": 90, "y": 180},
}

written = 0
skipped = 0
copied = 0
lang_added = 0
lang_existing = 0


def write_if_changed(path, text, check):
    """Writes ``text`` when the file differs; returns True when something was (or would be) written."""
    global written, skipped
    existing = None
    if os.path.exists(path):
        with io.open(path, "r", encoding="utf-8") as fh:
            existing = fh.read()
    if existing == text:
        skipped += 1
        return False
    if check:
        print("would write %s" % path)
        written += 1
        return True
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(text)
    written += 1
    return True


def copy_textures(folder, check):
    global copied, skipped
    source_root = next((root for root in GT6_TEXTURE_ROOTS if os.path.isdir(os.path.join(root, folder))),
                       None)
    if source_root is None:
        raise SystemExit("no GT6 texture folder found for %s in %s" % (folder, GT6_TEXTURE_ROOTS))
    for sub in FOLDERS:
        for face in FACES:
            src = os.path.join(source_root, folder, sub, face + ".png")
            if not os.path.exists(src):
                raise SystemExit("missing source texture %s" % src)
            dst = os.path.join(TEXTURE_ROOT, folder, sub, face + ".png")
            with open(src, "rb") as fh:
                data = fh.read()
            if os.path.exists(dst) and open(dst, "rb").read() == data:
                skipped += 1
                continue
            if check:
                print("would copy %s" % dst)
                copied += 1
                continue
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            with open(dst, "wb") as fh:
                fh.write(data)
            copied += 1


def block_model(folder):
    with io.open(TEMPLATE_MODEL, encoding="utf-8") as fh:
        text = fh.read()
    assert TEMPLATE_TEXTURE_FOLDER in text, "the template no longer references %s" % TEMPLATE_TEXTURE_FOLDER
    return text.replace(TEMPLATE_TEXTURE_FOLDER, folder)


def blockstate(model, block_id):
    variants = {}
    for facing in FACINGS:
        entry = {"model": "gregtech:block/machine/sensor/%s" % model}
        entry.update(ROTATIONS[facing])
        variants["facing=" + facing] = entry
    return json.dumps({"variants": variants}, indent=2, ensure_ascii=False) + "\n"


def insert_lang(path, entries, check):
    """Inserts ``key -> value`` lines in sorted key order, keeping every other line untouched."""
    global lang_added, lang_existing
    with io.open(path, encoding="utf-8") as fh:
        lines = fh.read().split("\n")
    for key, value in entries:
        needle = '"%s":' % key
        if any(line.lstrip().startswith(needle) for line in lines):
            lang_existing += 1
            continue
        line = '  "%s": %s,' % (key, json.dumps(value, ensure_ascii=False))
        # Find the first line whose key sorts after ours; both files start with '{' and end with '}'.
        index = None
        for i, existing in enumerate(lines):
            stripped = existing.strip()
            if not stripped.startswith('"'):
                continue
            existing_key = stripped.split('":', 1)[0][1:]
            if existing_key > key:
                index = i
                break
        if index is None:
            raise SystemExit("no insertion point for %s in %s" % (key, path))
        lines.insert(index, line)
        lang_added += 1
    if check:
        return
    with io.open(path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(lines))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="report what would change, write nothing")
    args = parser.parse_args()
    check = args.check

    for folder, block_id, en_name, zh_name, source in SENSORS:
        copy_textures(folder, check)
        write_if_changed(os.path.join(MODEL_ROOT, folder + ".json"), block_model(folder), check)
        write_if_changed(os.path.join(BLOCKSTATE_ROOT, block_id + ".json"),
                         blockstate(folder, block_id), check)
        write_if_changed(os.path.join(ITEM_MODEL_ROOT, block_id + ".json"),
                         json.dumps({"parent": "gregtech:block/machine/sensor/%s" % folder},
                                    indent=2) + "\n", check)

    insert_lang(os.path.join(LANG_ROOT, "en_us.json"),
                [("block.gregtech." + b, en) for _, b, en, _, _ in SENSORS], check)
    insert_lang(os.path.join(LANG_ROOT, "zh_cn.json"),
                [("block.gregtech." + b, zh) for _, b, _, zh, _ in SENSORS], check)

    print("textures copied: %d (unchanged: %d)" % (copied, skipped))
    print("files written: %d (unchanged: %d)" % (written, skipped))
    print("lang keys added: %d (already present: %d)" % (lang_added, lang_existing))
    for folder, block_id, en_name, zh_name, source in SENSORS:
        print("  %-34s %-32s %s" % (block_id, en_name, source))
    return 0


if __name__ == "__main__":
    sys.exit(main())
