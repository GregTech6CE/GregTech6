#!/usr/bin/env python3
"""Assets and language keys for GT6's food items (``MultiItemFood``).

The port already registers all 266 food items from ``GTMultiItemsGen``, and
``tools/transpile_gt6_multiitems.py`` already copied their textures and wrote their item models out of
GT6's own resource folder. What this script does is the *verification* half of that job for this batch,
plus the language keys the food table adds:

  1. For every row of ``GTFoodItemsGen`` it checks that GT6's texture
     (``<gt6>/resources/assets/gregtech/textures/items/gt.multiitem.food/<meta>.png``, or the
     ``GT6resourcepack`` copy) has a counterpart in the port
     (``src/main/resources/assets/gregtech/textures/item/food/<id>.png``) and that the item model
     exists. **No texture is ever invented**: a row without one is reported instead of being given a
     placeholder, and so is a model that is missing while its texture is present (that one is written,
     because the texture is the thing that cannot be made up).
  2. It adds the language keys the table owns - the GT6 display name and, where GT6 has one, the
     {@code FoodStat.addAdditionalToolTips} line ({@code FoodStat.java:198}:
     "Food: <foodLevel> - Saturation: <saturation>"). Both files are edited idempotently: an existing
     key is never overwritten, so hand-written translations survive a re-run.

Usage: python tools/generate_food_item_assets.py [--check]
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
import os
import re
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GT6_TEXTURES = os.path.join(r'F:\Dev\GregTech6\gregtech6-master\gregtech6-master', str(native_path('src')), 'main',
                            'resources', 'assets', 'gregtech', 'textures', 'items', 'gt.multiitem.food')
PACK_TEXTURES = os.path.join(r'F:\Dev\GregTech6\GT6resourcepack\gt6', 'resources', 'assets', 'gregtech',
                             'textures', 'items', 'gt.multiitem.food')
ASSETS = os.path.join(ROOT, str(native_path('src')), 'main', 'resources', 'assets', 'gregtech')
PORT_TEXTURES = os.path.join(ASSETS, 'textures', 'item', 'food')
MODELS = os.path.join(ASSETS, 'models', 'item')
LANG = os.path.join(ASSETS, 'lang')
GENERATED = os.path.join(ROOT, str(native_path('src')), 'main', 'java', 'com', 'gregtech', 'gregtech', 'data',
                         'generated', 'GTFoodItemsGen.java')

ROW = re.compile(
    r'new GTFoodItems\.Entry\("([a-z0-9_]+)", "((?:\\.|[^"\\])*)", (\d+),\s*'
    r'new GTFoodItems\.Food\((-?\d+), ([\d.]+)f, (-?\d+), (-?\d+), (true|false), (true|false)\),\s*'
    r'new GTFoodItems\.Stats\((-?\d+), (-?\d+), (-?\d+), (-?\d+), (-?\d+), (-?\d+)\),\s*'
    r'"((?:\\.|[^"\\])*)", (\d+), (true|false), (\d+)\)')


def rows():
    text = io.open(GENERATED, encoding='utf-8').read()
    out = []
    for match in ROW.finditer(text):
        out.append({
            'id': match.group(1),
            'display': match.group(2).replace('\\"', '"'),
            'meta': int(match.group(3)),
            'foodLevel': int(match.group(4)),
            'saturation': match.group(5),
            'hasStat': match.group(18) == 'true',
            'line': int(match.group(19)),
        })
    return out


def texture_source(meta: int):
    """GT6's PNG for a meta id, from the original sources or the resource pack copy."""
    for folder in (GT6_TEXTURES, PACK_TEXTURES):
        candidate = os.path.join(folder, '%d.png' % meta)
        if os.path.isfile(candidate):
            return candidate
    return None


def main() -> int:
    check = '--check' in sys.argv
    entries = rows()
    if len(entries) != 266:
        raise SystemExit('FATAL: GTFoodItemsGen has %d rows, expected 266' % len(entries))
    with_stat = sum(1 for entry in entries if entry['hasStat'])
    if with_stat != 207:
        raise SystemExit('FATAL: parsed %d rows with a FoodStat, expected 207 (the row regex is out of '
                         'step with the generator)' % with_stat)

    missing_textures, missing_models, wrote_models, lang_added = [], [], [], 0
    for entry in entries:
        item_id = entry['id']
        texture = os.path.join(PORT_TEXTURES, item_id + '.png')
        model = os.path.join(MODELS, item_id + '.json')
        has_texture = os.path.isfile(texture)
        if not has_texture:
            # Never invent a texture: record the row and skip it.
            source = texture_source(entry['meta'])
            missing_textures.append('%s (GT6 meta %d, %s)'
                                    % (item_id, entry['meta'],
                                       'GT6 has ' + os.path.basename(source) if source
                                       else 'GT6 has no texture either'))
            continue
        if not os.path.isfile(model):
            wrote_models.append(item_id)
            if not check:
                os.makedirs(MODELS, exist_ok=True)
                with io.open(model, 'w', encoding='utf-8', newline='\n') as handle:
                    json.dump({'parent': 'minecraft:item/generated',
                               'textures': {'layer0': 'gregtech:item/food/' + item_id}},
                              handle, indent=2)
                    handle.write('\n')
        if os.path.getsize(texture) == 0:
            missing_textures.append('%s (the port texture is empty)' % item_id)

    # GT6 FoodStat.addAdditionalToolTips (FoodStat.java:198) shows the food values on the item.
    for name in ('en_us.json', 'zh_cn.json'):
        path = os.path.join(LANG, name)
        with io.open(path, encoding='utf-8-sig') as handle:
            lang = json.load(handle)
        added = []
        for entry in entries:
            key = 'item.gregtech.' + entry['id']
            if key not in lang:
                lang[key] = entry['display']
                added.append(key)
            # The nutrition line is shown for every row whose FoodStat declares food or saturation
            # (FoodStat.java:198), and only then.
            food_key = key + '.food'
            if entry['hasStat'] and (entry['foodLevel'] > 0 or float(entry['saturation']) > 0.0):
                if food_key not in lang:
                    lang[food_key] = 'Food: %d - Saturation: %s' % (entry['foodLevel'], entry['saturation'])
                    added.append(food_key)
        lang_added += len(added)
        if added and not check:
            with io.open(path, 'w', encoding='utf-8', newline='\n') as handle:
                json.dump(lang, handle, indent=2, ensure_ascii=False, sort_keys=True)
                handle.write('\n')

    print('rows: %d, textures present: %d, models written: %d, lang keys added: %d'
          % (len(entries), len(entries) - len(missing_textures), len(wrote_models), lang_added))
    for note in missing_textures:
        print('  MISSING TEXTURE ' + note)
    for item_id in missing_models:
        print('  MISSING MODEL ' + item_id)
    if check and (wrote_models or lang_added):
        print('STALE: models or language keys would change')
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
