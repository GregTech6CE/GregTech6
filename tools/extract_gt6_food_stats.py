#!/usr/bin/env python3
"""Extract GT6's food statistics (alcohol / caffeine / dehydration / sugar / fat / radiation).

Source of truth: GT6's ``CS.FoodsGT`` table *plus* the ``FoodStat`` declarations that replace it for
GT6's own items. There are two disjoint paths in the original and the port needs both:

  * ``gregtech/items/MultiItemFood.java`` — GT6's own food items. Their numbers never reach
    ``FoodsGT``: ``RM.crop`` puts only non-``MultiItemRandom`` stacks into the table
    (``RM.java:776``) and every ``Loader_Recipes_Food`` listener that touches a GT food family is
    guarded by ``!ST.isGT``. What carries them is ``FoodStat.onEaten``, which pushes
    ``mAlcohol/mCaffeine/mDehydration/mSugar/mFat/mRadiation`` into ``EntityFoodTracker`` directly
    (``FoodStat.java:164-172``). That pass is read from
    :mod:`tools.extract_gt6_food_items`, which owns the ``MultiItemFood`` parse, so the two
    generated tables can never disagree about a row.
  * ``gregtech/loaders/c/Loader_Recipes_Food.java`` and ``.../Loader_Recipes_Crops.java`` — the
    ``addListener("<oreDictName>", ...)`` blocks, one ``FoodsGT.put(aEvent.mStack, ...)`` each. Those
    exist for *other mods'* items (vanilla and every mod GT6 supports); the port has no other mods, so
    the names are mapped onto the 1.20.1 items that took their place and everything else is recorded
    as skipped with the reason GT6 itself gives (the ``!ST.isGT`` guard, or ``RM.crop``'s
    ``instanceof MultiItemRandom`` test).

``PlayerUseItemEvent.Finish`` in ``GT_API_Proxy:995-1010`` then reads ``FoodsGT.get(item)`` and pushes
the six numbers into ``EntityFoodTracker``.

Generated (do not edit by hand):
  src/main/java/com/gregtech/gregtech/data/generated/GTFoodStatsGen.java

Usage: python tools/extract_gt6_food_stats.py [--check]
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import extract_gt6_food_items as food_items  # noqa: E402  (same directory, shared MultiItemFood parse)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GT6 = r'F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java'
OUT = os.path.join(ROOT, str(native_path('src')), 'main', 'java', 'com', 'gregtech', 'gregtech', 'data', 'generated',
                   'GTFoodStatsGen.java')
LOADER_FOOD = os.path.join(GT6, 'gregtech', 'loaders', 'c', 'Loader_Recipes_Food.java')
LOADER_CROPS = os.path.join(GT6, 'gregtech', 'loaders', 'c', 'Loader_Recipes_Crops.java')

# 1.7.10 -> 1.20.1 vanilla item names (only the ones GT6 actually uses).
ITEMS = {
    'apple': 'APPLE',
    'baked_potato': 'BAKED_POTATO',
    'beef': 'BEEF',
    'bread': 'BREAD',
    'carrot': 'CARROT',
    'chicken': 'CHICKEN',
    'cookie': 'COOKIE',
    'cooked_beef': 'COOKED_BEEF',
    'cooked_chicken': 'COOKED_CHICKEN',
    'cooked_fished': 'COOKED_COD',
    'cooked_porkchop': 'COOKED_PORKCHOP',
    'fish': 'COD',
    'melon': 'MELON_SLICE',
    'mushroom_stew': 'MUSHROOM_STEW',
    'poisonous_potato': 'POISONOUS_POTATO',
    'porkchop': 'PORKCHOP',
    'potato': 'POTATO',
    'pumpkin_pie': 'PUMPKIN_PIE',
    'rotten_flesh': 'ROTTEN_FLESH',
    'spider_eye': 'SPIDER_EYE',
    'golden_apple': 'GOLDEN_APPLE',
}

# GT6 ore-dictionary food families -> the 1.20.1 items that carry the same food today. An empty list
# means "no 1.20.1 item has this name"; the reason is derived from GT6's own source by listener_reason().
LISTENERS = {
    # fish and meat: GT6 gives every raw/cooked variant of the family the same numbers.
    'listAllfishcooked': (['COOKED_COD', 'COOKED_SALMON'], None),
    'listAllbeefraw': (['BEEF'], None),
    'listAllbeefcooked': (['COOKED_BEEF'], None),
    'listAllchickenraw': (['CHICKEN'], None),
    'listAllchickencooked': (['COOKED_CHICKEN'], None),
    'listAllmuttonraw': (['MUTTON'], None),
    'listAllmuttoncooked': (['COOKED_MUTTON'], None),
    'listAllporkraw': (['PORKCHOP'], None),
    'listAllporkcooked': (['COOKED_PORKCHOP'], None),
    'listAllrabbitraw': (['RABBIT'], None),
    'listAllrabbitcooked': (['COOKED_RABBIT'], None),
    'foodScrapmeat': (['ROTTEN_FLESH'], None),
    # Crops registered through RM.crop/RM.crop_nut: only the two vanilla ones exist in 1.20.1.
    'cropCocoa': (['COCOA_BEANS'], None),
    'cropPotato': (['POTATO'], None),
    # Every remaining listener feeds other mods' items only; listener_reason() reads GT6's source.
    'foodCheese': ([], None),
    'foodSilkentofu': ([], None),
    'foodCaramel': ([], None),
    'foodTaffy': ([], None),
    'foodBaconraw': ([], None),
    'foodBaconcooked': ([], None),
    'listAllribraw': ([], None),
    'listAllribcooked': ([], None),
    'listAllturkeyraw': ([], None),
    'listAllturkeycooked': ([], None),
    'listAllcrabyraw': ([], None),
    'listAllcrabcooked': ([], None),
    'listAllratraw': ([], None),
    'listAllratcooked': ([], None),
    'listAllturtleraw': ([], None),
    'listAllturtlecooked': ([], None),
    'listAllostrichraw': ([], None),
    'listAllostrichcooked': ([], None),
    'listAllvenisonraw': ([], None),
    'listAllvenisoncooked': ([], None),
    'listAlltitanraw': ([], None),
    'listAlltitancooked': ([], None),
    'listAllhamraw': ([], None),
    'listAllhamcooked': ([], None),
    'listAllhorseraw': ([], None),
    'listAllhorsecooked': ([], None),
    'listAlldograw': ([], None),
    'listAlldogcooked': ([], None),
    'listAllhydraraw': ([], None),
    'listAllhydracooked': ([], None),
    'cropCorn': ([], None),
    'cropDevilishMaize': ([], None),
    'cropHemp': ([], None),
    'cropCandleberry': ([], None),
    'cropSunflower': ([], None),
    'cropStarAnise': ([], None),
    'cropCoffee': ([], None),
    'cropTea': ([], None),
    'cropCoconut': ([], None),
    'cropChilipepper': ([], None),
    'cropCinnamon': ([], None),
    'cropCurryleaf': ([], None),
    'cropPeppercorn': ([], None),
    # The nut trees (RM.crop_nut: fat 16 each).
    'cropPeanut': ([], None),
    'cropHazelnut': ([], None),
    'cropPistachio': ([], None),
    'cropNutmeg': ([], None),
    'cropAlmond': ([], None),
    'cropCandlenut': ([], None),
    'cropPecan': ([], None),
    'cropBeechnut': ([], None),
    'cropBrazilNut': ([], None),
    'cropGingkoNut': ([], None),
    'cropCashew': ([], None),
    'cropAcorn': ([], None),
    'cropButternut': ([], None),
    'cropWalnut': ([], None),
    'cropChestnut': ([], None),
}

CROP_CALL = re.compile(r'RM\.crop(?:_nut|_fruit|_veggie)?\(')


def listener_blocks(path: str):
    """``{listener name: (guarded by !ST.isGT, uses RM.crop)}`` for one loader file."""
    text = io.open(path, encoding='utf-8', errors='replace').read()
    out = {}
    for chunk in text.split('addListener(')[1:]:
        # The names are the string literals before the event object; `dust.dat(...)`-style names are
        # object expressions the LISTENERS table does not key on.
        head = chunk.split('new IOreDictListenerEvent')[0]
        names = re.findall(r'"([^"]+)"', head)
        guarded = '!ST.isGT(' in chunk
        crop = bool(CROP_CALL.search(chunk))
        for name in names:
            out.setdefault(name, (guarded, crop))
    return out


def parse(path: str, listener_mode: bool):
    """Yield (listener-or-None, target, stats) for every FoodsGT.put in one GT6 file."""
    text = io.open(path, encoding='utf-8', errors='replace').read()
    rows = []
    for match in re.finditer(r'FoodsGT\.put\((.*?)\)\s*;', text, re.S):
        body = ' '.join(match.group(1).split())
        stats_match = re.search(r'((?:-?\d+\s*,\s*)*-?\d+)$', body)
        if not stats_match:
            continue
        stats = [int(x) for x in stats_match.group(1).split(',')]
        target = body[:stats_match.start()].rstrip().rstrip(',').strip()
        listener = None
        if listener_mode:
            prefix = text[:match.start()]
            names = re.findall(r'addListener\("([^"]+)"', prefix)
            listener = names[-1] if names else None
        rows.append((listener, target, stats))
    return rows


def parse_crops(path: str):
    """Yield (listener, stats) for GT6's RM.crop / RM.crop_nut registrations (see RM.java:766-776)."""
    text = io.open(path, encoding='utf-8', errors='replace').read()
    rows = []
    listener = None
    for match in re.finditer(r'addListener\("([^"]+)"|RM\.crop(_nut)?\((.*?)\)\s*;', text, re.S):
        if match.group(1):
            listener = match.group(1)
            continue
        if match.group(2):
            # RM.crop_nut always forwards 0,0,0,0,16,0 to FoodsGT.put (RM.java:766).
            rows.append((listener, [0, 0, 0, 0, 16, 0]))
            continue
        ints = [int(x) for x in re.findall(r'(?<![\w.])(\d+)(?![\w.])', ' '.join(match.group(3).split()))]
        if len(ints) < 5:
            continue
        rows.append((listener, ints[-5:] + [0]))
    return rows


def listener_reason(name: str, guards: dict, port_items: dict, stat_of: dict) -> str:
    """Why a ``FoodsGT`` listener row has no 1.20.1 item, in GT6's own terms (never guessed)."""
    guarded, crop = guards.get(name, (False, False))
    reasons = []
    if guarded:
        reasons.append("GT6 guards the listener body with !ST.isGT, so it only ever fed other mods' items")
    if crop:
        reasons.append('RM.crop puts only non-MultiItemRandom stacks into FoodsGT (RM.java:776), '
                       "so GT6's own item never got these numbers from here")
    port = port_items.get(name)
    if port is None:
        reasons.append('the port has no item under this GT6 ore-dictionary name (it is another mod\'s '
                       'crop or food, and GT6 registers no item of its own for it)')
    else:
        stats = stat_of.get(port[0])
        if stats is None:
            reasons.append("the port's %s declares no FoodStat, so GT6 gives it no statistics"
                           % port[0])
        elif stats == listener_stats(name):
            reasons.append("the port's %s is in ROWS with the same numbers, read from its FoodStat at "
                           'MultiItemFood.java:%d' % (port[0], port[1]))
        else:
            reasons.append("the port's %s takes its numbers from its own FoodStat at "
                           'MultiItemFood.java:%d (%s)'
                           % (port[0], port[1], ','.join(str(value) for value in stats)))
    return '%s (%s)' % (name, '; '.join(reasons))


def listener_stats(name: str):
    """The statistics the listener named ``name`` writes, or None when it writes none."""
    return STATS_BY_LISTENER.get(name)


STATS_BY_LISTENER = {}


def main() -> int:
    check = '--check' in sys.argv
    files = [('MultiItemFood.java', food_items.MULTI_FOOD, False),
             ('Loader_Recipes_Food.java', LOADER_FOOD, True),
             ('Loader_Recipes_Crops.java', LOADER_CROPS, True)]

    entries = {}          # itemName -> stats
    skipped = []
    unknown = []
    for label, path, listener_mode in files:
        for listener, target, stats in parse(path, listener_mode):
            if len(stats) == 5:
                stats = stats + [0]
            if len(stats) != 6:
                unknown.append('%s: %s -> %s' % (label, target, stats))
                continue
            if listener_mode:
                if listener is None:
                    unknown.append('%s: no listener for %s' % (label, target))
                    continue
                items, _reason = LISTENERS.get(listener, (None, None))
                if items is None:
                    unknown.append('%s: unmapped listener %s' % (label, listener))
                    continue
                STATS_BY_LISTENER.setdefault(listener, stats)
                if not items:
                    continue      # recorded below, once the reasons can be derived
                for item in items:
                    entries.setdefault(item, stats)
                continue
            match = re.search(r'Items\.(\w+)', target)
            if not match:
                continue
            item = ITEMS.get(match.group(1))
            if item is None:
                skipped.append('%s (no 1.20.1 mapping for Items.%s)' % (target, match.group(1)))
                continue
            entries.setdefault(item, stats)

    # The RM.crop / RM.crop_nut registrations of Loader_Recipes_Crops.
    for listener, stats in parse_crops(LOADER_CROPS):
        if listener is None:
            unknown.append('crop: %s without a listener' % stats)
            continue
        items, _reason = LISTENERS.get(listener, (None, None))
        if items is None:
            unknown.append('crop: unmapped listener %s' % listener)
            continue
        STATS_BY_LISTENER.setdefault(listener, stats)
        for item in items or []:
            entries.setdefault(item, stats)

    # ── GT6's own food items: the FoodStat path (MultiItemFood + RM.crop exclusion) ──────────────
    food_entries, _food_skipped, _vanilla, _stubs = food_items.build()
    gt_rows = []          # (port id, six statistics, GT6 line)
    for entry in food_entries:
        stats = [entry['alcohol'], entry['caffeine'], entry['dehydration'],
                 entry['sugar'], entry['fat'], entry['radiation']]
        if any(stats):
            gt_rows.append((entry['id'], stats, entry['line']))

    # ── The listener rows that have no 1.20.1 item, with GT6's own reason ────────────────────────
    guards = {}
    guards.update(listener_blocks(LOADER_FOOD))
    guards.update(listener_blocks(LOADER_CROPS))
    port_items = food_items.oredict_names()
    stat_of = {row[0]: row[1] for row in gt_rows}
    for name, (items, _reason) in sorted(LISTENERS.items()):
        if items:
            continue
        if name not in STATS_BY_LISTENER and name not in port_items:
            # GT6 registers no FoodsGT row for this name at all (nothing to record).
            continue
        skipped.append(listener_reason(name, guards, port_items, stat_of))

    lines = []
    add = lines.append
    add('package com.gregtech.gregtech.data.generated;')
    add('')
    add('import com.gregtech.gregtech.content.food.GTFoodItems;')
    add('import com.gregtech.gregtech.content.food.GTFoodStats;')
    add('import net.minecraft.world.item.Item;')
    add('import net.minecraft.world.item.Items;')
    add('')
    add('import java.util.ArrayList;')
    add('import java.util.List;')
    add('')
    add('/**')
    add(' * GENERATED by tools/extract_gt6_food_stats.py - do not edit by hand.')
    add(' *')
    add(" * <p>GT6's {@code CS.FoodsGT} entries, in the order [alcohol, caffeine, dehydration, sugar, fat,")
    add(' * radiation], from both of the original\'s paths: the explicit {@code FoodsGT.put} rows of')
    add(' * {@code MultiItemFood}/{@code Loader_Recipes_Food}/{@code Loader_Recipes_Crops} (vanilla items)')
    add(" * and the {@code FoodStat} declarations GT6's own items carry instead, because neither")
    add(' * {@code RM.crop} ({@code RM.java:776}) nor the {@code !ST.isGT}-guarded listeners ever put a GT')
    add(' * item into the table ({@code FoodStat.java:164-172} feeds the tracker directly).')
    add(' */')
    add('public final class GTFoodStatsGen {')
    add('    private GTFoodStatsGen() {}')
    add('')
    add('    /** Port ids of GT6 food items the item registry did not have when the table was built. */')
    add('    private static final List<String> MISSING_IDS = new ArrayList<>();')
    add('')
    add('    public static final List<GTFoodStats.Row> ROWS = build();')
    add('')
    add('    /** GT6 food rows whose port item could not be resolved (a real gap, asserted by the test). */')
    add('    public static final List<String> MISSING = List.copyOf(MISSING_IDS);')
    add('')
    add('    /** GT6 entries whose food does not exist in 1.20.1 (other mods\' items and crops). */')
    add('    public static final List<String> SKIPPED = List.of(')
    notes = sorted(set(skipped))
    for index, note in enumerate(notes):
        comma = ',' if index < len(notes) - 1 else ''
        add('            "%s"%s' % (note.replace('\\', '\\\\').replace('"', '\\"'), comma))
    add('    );')
    add('')
    add('    private static List<GTFoodStats.Row> build() {')
    add('        List<GTFoodStats.Row> rows = new ArrayList<>();')
    for item in sorted(entries):
        add('        rows.add(new GTFoodStats.Row(Items.%-20s %s));'
            % (item + ',', ', '.join(str(value) for value in entries[item])))
    for port_id, stats, line in gt_rows:
        add('        gt(rows, "%-26s %s); // MultiItemFood.java:%d'
            % (port_id + '",', ', '.join(str(value) for value in stats), line))
    add('        return List.copyOf(rows);')
    add('    }')
    add('')
    add('    /** Resolves a port food item; a null here is recorded, never silently dropped. */')
    add('    private static void gt(List<GTFoodStats.Row> rows, String id, int alcohol, int caffeine,')
    add('                           int dehydration, int sugar, int fat, int radiation) {')
    add('        Item item = GTFoodItems.item(id);')
    add('        if (item == null) {')
    add('            MISSING_IDS.add(id);')
    add('            return;')
    add('        }')
    add('        rows.add(new GTFoodStats.Row(item, alcohol, caffeine, dehydration, sugar, fat, radiation));')
    add('    }')
    add('}')
    text = '\n'.join(lines) + '\n'

    print('vanilla rows: %d, GT6 FoodStat rows: %d, total: %d'
          % (len(entries), len(gt_rows), len(entries) + len(gt_rows)))
    print('skipped: %d, unknown: %d' % (len(notes), len(unknown)))
    for row in unknown:
        print('  UNKNOWN ' + row)
    if unknown:
        return 2

    old = io.open(OUT, encoding='utf-8').read() if os.path.isfile(OUT) else None
    if old == text:
        print('unchanged ' + os.path.relpath(OUT, ROOT))
        return 0
    if check:
        print('STALE ' + os.path.relpath(OUT, ROOT))
        return 1
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with io.open(OUT, 'w', encoding='utf-8', newline='\n') as handle:
        handle.write(text)
    print('wrote ' + os.path.relpath(OUT, ROOT))
    return 0


if __name__ == '__main__':
    sys.exit(main())
