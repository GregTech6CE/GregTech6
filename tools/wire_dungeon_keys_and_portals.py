#!/usr/bin/env python3
"""Wire the dungeon keys and the two dungeon portal blocks: textures, models, blockstates and lang keys.

Sources, all from the original:

* the ten keys are the multi-items 30000..30009 of
  ``gregtech6-master/.../gregtech/items/MultiItemRandomTools.java:589-598`` ("Iron Key" .. "Plastic
  Key"); their textures are the 16x16 files ``<id>.png`` of the resource pack folder
  ``assets/gregtech/textures/items/gt.multiitem.randomtools``. 30000 Iron, 30001 Gold, 30002 Copper,
  30003 Tin, 30004 Bronze, 30005 Brass, 30006 Silver, 30007 Platinum, 30008 Lead, 30009 Plastic.
* the portal frame shapes are GT6's own twelve render passes
  (``MultiTileEntityMiniPortal.java:282-297``, ``sBlockBounds`` index 0 is the inner portal cube and
  1..12 are the frame bars), the frame texture is the vanilla one GT6 copies
  (``MultiTileEntityMiniPortalNether:133`` obsidian, ``MultiTileEntityMiniPortalEnd:128`` the end
  portal frame's top face), and the End portal's plane is the vanilla portal texture multiplied with
  ``DYE_Black`` - GT6 does that at render time (``BlockTextureCopied.get(Blocks.portal, SIDE_ANY, 0,
  DYE_Black, ...)``), the port bakes it into a texture because a 1.20.1 model cannot tint one part.
  The 1.7.10 portal texture (16x512, 32 frames, ``{"animation": {}}``) is taken out of the resource
  pack; 1.20.1's own texture is used for the Nether plane.

Nothing here is invented: every key texture is a byte copy of GT6's, the End texture is GT6's own
multiply, and the shapes come from GT6's constants.

Usage: python tools/wire_dungeon_keys_and_portals.py [--pack DIR]
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import sys
from pathlib import Path

from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parents[1]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
RESOURCES = ROOT / "src/main/resources"
ASSETS = RESOURCES / "assets/gregtech"
LANG = ASSETS / "lang"

# The port's GT6 resource pack, whose "gt6" profile is the original's own texture set.
DEFAULT_PACK = Path(r"F:\Dev\GregTech6\GT6resourcepack")
KEY_TEXTURES = "gt6/resources/assets/gregtech/textures/items/gt.multiitem.randomtools"
# Any profile of the pack that ships the 1.7.10 vanilla textures; the first one found is used.
VANILLA_TEXTURES = "*/assets/minecraft/textures/blocks"

# GT6 multi-item id -> (port item id, English name, Chinese name). The order is GT6's IL.KEYS
# (gregapi/data/IL.java:516), which is the order GTDungeonKeys registers them in.
KEYS = [
    (30005, "key_brass", "Brass Key", "\u9ec4\u94dc\u94a5\u5319"),
    (30004, "key_bronze", "Bronze Key", "\u9752\u94dc\u94a5\u5319"),
    (30002, "key_copper", "Copper Key", "\u94dc\u94a5\u5319"),
    (30001, "key_gold", "Gold Key", "\u91d1\u94a5\u5319"),
    (30000, "key_iron", "Iron Key", "\u94c1\u94a5\u5319"),
    (30008, "key_lead", "Lead Key", "\u94c5\u94a5\u5319"),
    (30009, "key_plastic", "Plastic Key", "\u5851\u6599\u94a5\u5319"),
    (30007, "key_platinum", "Platinum Key", "\u94c2\u91d1\u94a5\u5319"),
    (30006, "key_silver", "Silver Key", "\u94f6\u94a5\u5319"),
    (30003, "key_tin", "Tin Key", "\u9521\u94a5\u5319"),
]

# GT6's MultiTileEntityMiniPortal.sBlockBounds (:282-297): PX_P[n] = n, PX_N[n] = 16 - n, in pixels.
PX_P = {n: float(n) for n in range(17)}
PX_N = {n: float(16 - n) for n in range(17)}
BOUNDS = [
    ((PX_P[1], PX_P[1], PX_P[1]), (PX_N[1], PX_N[1], PX_N[1])),          # 0: the portal plane
    ((PX_P[0], PX_P[0], PX_P[0]), (PX_N[0], PX_N[14], PX_N[14])),        # 1
    ((PX_P[0], PX_P[2], PX_P[0]), (PX_N[14], PX_N[2], PX_N[14])),        # 2
    ((PX_P[0], PX_P[0], PX_P[0]), (PX_N[14], PX_N[14], PX_N[0])),        # 3
    ((PX_P[14], PX_P[0], PX_P[0]), (PX_N[0], PX_N[14], PX_N[0])),        # 4
    ((PX_P[14], PX_P[2], PX_P[0]), (PX_N[0], PX_N[2], PX_N[14])),        # 5
    ((PX_P[0], PX_P[14], PX_P[0]), (PX_N[14], PX_N[0], PX_N[0])),        # 6
    ((PX_P[0], PX_P[14], PX_P[0]), (PX_N[0], PX_N[0], PX_N[14])),        # 7
    ((PX_P[0], PX_P[2], PX_P[14]), (PX_N[14], PX_N[2], PX_N[0])),        # 8
    ((PX_P[0], PX_P[0], PX_P[14]), (PX_N[0], PX_N[14], PX_N[0])),        # 9
    ((PX_P[0], PX_P[14], PX_P[14]), (PX_N[0], PX_N[0], PX_N[0])),        # 10
    ((PX_P[14], PX_P[2], PX_P[14]), (PX_N[0], PX_N[2], PX_N[0])),        # 11
    ((PX_P[14], PX_P[14], PX_P[0]), (PX_N[0], PX_N[0], PX_N[0])),        # 12
]

FACES = ("down", "up", "north", "south", "west", "east")

# The two portal blocks: (block id, frame texture, plane texture or None, English, Chinese).
PORTALS = [
    ("dungeon_portal_nether", "minecraft:block/obsidian", "minecraft:block/nether_portal",
     "Dungeon Nether Portal", "\u5730\u7262\u4e0b\u754c\u4f20\u9001\u95e8"),
    ("dungeon_portal_end", "minecraft:block/end_portal_frame_top", "gregtech:block/dungeon/portal_end",
     "Dungeon End Portal", "\u5730\u7262\u672b\u5730\u4f20\u9001\u95e8"),
]

# Everything the two new item types and the two blocks need in both languages.
TOOLTIPS = [
    ("tooltip.gregtech.key.lock", "Can open certain regular Locks", "\u53ef\u4ee5\u6253\u5f00\u67d0\u4e9b\u5e38\u89c4\u7684\u9501"),
    ("tooltip.gregtech.key.id", "Key ID: %s", "\u94a5\u5319\u7f16\u53f7: %s"),
    ("tooltip.gregtech.key.blank", "*BLANK*", "*\u7a7a\u767d*"),
    ("tooltip.gregtech.portal.nether.range",
     "Only works between the Nether and the Overworld with a x8 Distance Factor!",
     "\u4ec5\u5728\u4e0b\u754c\u4e0e\u4e3b\u4e16\u754c\u4e4b\u95f4\u751f\u6548\uff0c\u8ddd\u79bb\u7f29\u653e\u500d\u7387\u4e3a 8 \u500d\uff01"),
    ("tooltip.gregtech.portal.nether.margin", "Margin of Error to still work: 128 Meters.",
     "\u4ecd\u80fd\u751f\u6548\u7684\u8bef\u5dee\u8303\u56f4: 128 \u683c\u3002"),
    ("tooltip.gregtech.portal.end.range",
     "Only works between the End and the Overworld with a x128 Distance Factor!",
     "\u4ec5\u5728\u672b\u5730\u4e0e\u4e3b\u4e16\u754c\u4e4b\u95f4\u751f\u6548\uff0c\u8ddd\u79bb\u7f29\u653e\u500d\u7387\u4e3a 128 \u500d\uff01"),
    ("tooltip.gregtech.portal.end.margin", "Margin of Error to still work: 512 Meters.",
     "\u4ecd\u80fd\u751f\u6548\u7684\u8bef\u5dee\u8303\u56f4: 512 \u683c\u3002"),
    ("tooltip.gregtech.portal.key", "Requires the matching Dungeon Key for activation",
     "\u9700\u8981\u5bf9\u5e94\u7684\u5730\u7262\u94a5\u5319\u624d\u80fd\u6fc0\u6d3b"),
    ("tooltip.gregtech.portal.ignite", "Flint and Steel opens and closes the Portal",
     "\u6253\u706b\u77f3\u53ef\u4ee5\u5f00\u542f\u6216\u5173\u95ed\u4f20\u9001\u95e8"),
]


def write_text(path: Path, text: str) -> bool:
    """Writes only when the content differs, so a second run touches nothing."""
    if path.exists() and path.read_text(encoding="utf-8") == text:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
    return True


def write_json(path: Path, data) -> bool:
    return write_text(path, json.dumps(data, ensure_ascii=False, indent=2) + "\n")


def copy_key_textures(pack: Path) -> int:
    source = pack / KEY_TEXTURES
    if not source.is_dir():
        print("WARNING: no key textures at %s - the key items stay without a texture" % source)
        return 0
    copied = 0
    for item_id, port_id, _en, _zh in KEYS:
        origin = source / ("%d.png" % item_id)
        if not origin.exists():
            print("WARNING: GT6 key texture %s is missing" % origin)
            continue
        target = ASSETS / "textures/item" / (port_id + ".png")
        target.parent.mkdir(parents=True, exist_ok=True)
        if not target.exists() or target.read_bytes() != origin.read_bytes():
            shutil.copyfile(origin, target)
            copied += 1
    return copied


def portal_texture(pack: Path) -> bool:
    """GT6's End portal plane: the 1.7.10 portal texture multiplied with DYE_Black (0x202020)."""
    sources = sorted(pack.glob(VANILLA_TEXTURES + "/portal.png"))
    if not sources:
        print("WARNING: no vanilla portal texture in %s - the End portal has no plane texture" % pack)
        return False
    source = sources[0]
    image = Image.open(source).convert("RGBA")
    black = Image.new("RGBA", image.size, (0x20, 0x20, 0x20, 255))
    tinted = ImageChops.multiply(image, black)
    target = ASSETS / "textures/block/dungeon/portal_end.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    tinted.save(target)
    # GT6's texture is an animation of 32 frames, one below the other; 1.20.1 reads the same layout
    # from the .mcmeta the pack already carries.
    write_text(target.with_suffix(".png.mcmeta"), '{\n  "animation": {}\n}\n')
    return True


def element(box, texture: str) -> dict:
    return {"from": list(box[0]), "to": list(box[1]),
            "faces": {face: {"texture": texture} for face in FACES}}


def portal_models() -> int:
    written = 0
    for block_id, frame_texture, plane_texture, _en, _zh in PORTALS:
        # The closed portal: GT6's twelve frame bars only.
        frame = {"render_type": "minecraft:cutout",
                 "textures": {"frame": frame_texture, "particle": frame_texture},
                 "elements": [element(box, "#frame") for box in BOUNDS[1:]]}
        written += write_json(SHARED_ASSETS / "models/block/dungeon" / (block_id + "_frame.json"), frame)
        # The open portal: the same frame plus GT6's inner portal cube.
        open_model = {"render_type": "minecraft:translucent",
                      "textures": {"frame": frame_texture, "portal": plane_texture,
                                   "particle": plane_texture},
                      "elements": [element(box, "#frame") for box in BOUNDS[1:]]
                                  + [element(BOUNDS[0], "#portal")]}
        written += write_json(SHARED_ASSETS / "models/block/dungeon" / (block_id + ".json"), open_model)
        state = {"variants": {
            "active=false": {"model": "gregtech:block/dungeon/%s_frame" % block_id},
            "active=true": {"model": "gregtech:block/dungeon/%s" % block_id}}}
        written += write_json(SHARED_ASSETS / "blockstates" / (block_id + ".json"), state)
        written += write_json(SHARED_ASSETS / "models/item" / (block_id + ".json"),
                              {"parent": "gregtech:block/dungeon/%s" % block_id})
    return written


def key_models() -> int:
    written = 0
    for _item_id, port_id, _en, _zh in KEYS:
        written += write_json(SHARED_ASSETS / "models/item" / (port_id + ".json"),
                              {"parent": "minecraft:item/generated",
                               "textures": {"layer0": "gregtech:item/" + port_id}})
    return written


def lang_entries() -> tuple[dict, dict]:
    english, chinese = {}, {}
    for _item_id, port_id, en, zh in KEYS:
        english["item.gregtech." + port_id] = en
        chinese["item.gregtech." + port_id] = zh
    for block_id, _frame, _plane, en, zh in PORTALS:
        english["block.gregtech." + block_id] = en
        chinese["block.gregtech." + block_id] = zh
    for key, en, zh in TOOLTIPS:
        english[key] = en
        chinese[key] = zh
    return english, chinese


def load_lines(path: Path):
    """The file as (lines, line separator, trailing-separator) so a write cannot reformat it."""
    text = path.read_bytes().decode("utf-8")
    newline = "\r\n" if "\r\n" in text else "\n"
    lines = text.split(newline)
    trailing = bool(lines) and lines[-1] == ""
    if trailing:
        lines = lines[:-1]
    return lines, newline, trailing


def insert(path: Path, keys: dict) -> int:
    """Sorted insert of the missing keys, the approach of tools/add_bumble_scanned_lang.py."""
    import re
    lines, newline, trailing = load_lines(path)
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
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pack", type=Path, default=DEFAULT_PACK,
                        help="the GT6 resource pack (default %s)" % DEFAULT_PACK)
    args = parser.parse_args()

    copied = copy_key_textures(args.pack)
    tinted = portal_texture(args.pack)
    assets = key_models() + portal_models()

    english, chinese = lang_entries()
    added_en = insert(LANG / "en_us.json", english)
    added_zh = insert(LANG / "zh_cn.json", chinese)
    for language, keys in (("en_us", english), ("zh_cn", chinese)):
        parsed = json.loads((LANG / (language + ".json")).read_text(encoding="utf-8"))
        missing = [key for key in keys if key not in parsed]
        if missing:
            print("ERROR: %s is still missing %s" % (language, missing))
            return 1
    print("key textures copied: %d, End portal texture: %s, asset files written: %d, lang keys: +%d/+%d"
          % (copied, "yes" if tinted else "no", assets, added_en, added_zh))
    return 0


if __name__ == "__main__":
    sys.exit(main())
