#!/usr/bin/env python3
"""Extract GT6's food items (``MultiItemFood.addItem``) into ``GTFoodItemsGen.java``.

Source of truth: ``gregtech/items/MultiItemFood.java`` (995 lines, 267 ``addItem(...)`` rows).
Every row is one GT6 food item:

    IL.Food_Cheese.set(addItem(1000, "Cheese", "Click the Cheese", "foodCheese",
                               new FoodStat(2, 1.200F, 0, C+37, 0.10F, 0,0,0,8,0,
                                            EnumAction.eat, null, F, T, F, T), TC.stack(...)));

The ``FoodStat`` carries everything the port needs (:class:`gregapi.item.multiitem.food.FoodStat`):

* ``aFoodLevel`` (0-20 half-bacon) and ``aSaturation`` (0.0-1.0) become vanilla's
  ``FoodProperties.nutrition`` / ``saturationMod`` — GT6 hands exactly those two to
  ``FoodStats.addStats(foodLevel, saturation)`` (``MultiItemRandom.java:297-304``), and 1.20.1's
  ``FoodData.eat(nutrition, saturationModifier)`` adds the same ``nutrition * sat * 2``.
* ``aAlcohol/aCaffeine/aDehydration/aSugar/aFat/aRadiation`` are the six statistics of
  ``EntityFoodTracker`` that ``FoodStat.onEaten`` pushes directly (``FoodStat.java:164-172``).
  They never reach ``CS.FoodsGT``: ``RM.crop`` skips every ``MultiItemRandom`` stack
  (``RM.java:776``) and the ``Loader_Recipes_Food`` listeners are all guarded by ``!ST.isGT``,
  which is why the port's ``GTFoodStats`` table only ever saw the *vanilla* items.
* ``aAlwaysEdible``/``aIsRotten`` are read back through ``IFoodStat`` in ``MultiItemRandom``.

Port ids: the port registers these items from ``GTMultiItemsGen`` (``food`` category), which
``tools/transpile_gt6_multiitems.py`` transpiles with ``snake(display name)`` plus ``_N`` collision
suffixes. This script re-derives that mapping and *verifies* it: GT6's rows sorted by meta line up
1:1 with the ``food`` entries of ``GTMultiItemsGen`` (266 rows / 266 entries / 0 mismatches), so
every id below is checked against the port's own registration rather than guessed.

Generated (do not edit by hand):
  src/main/java/com/gregtech/gregtech/data/generated/GTFoodItemsGen.java

Usage:
  python tools/extract_gt6_food_items.py [--check] [--report]
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

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GT6 = r'F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java'
OUT = os.path.join(ROOT, str(native_path('src')), 'main', 'java', 'com', 'gregtech', 'gregtech', 'data', 'generated',
                   'GTFoodItemsGen.java')
MULTI_FOOD = os.path.join(GT6, 'gregtech', 'items', 'MultiItemFood.java')
MULTI_GEN = os.path.join(ROOT, str(native_path('src')), 'main', 'java', 'com', 'gregtech', 'gregtech', 'registry',
                         'GTMultiItemsGen.java')

# 1.7.10 -> 1.20.1 vanilla items GT6 aliases its food onto (`IL.Food_X.set(ST.make(Items.x, 1, 0))`).
#    Those rows carry no FoodStat; GT6 gives them their numbers with an explicit
#    `FoodsGT.put(ST.make(Items.x, 1, W), ...)`, which tools/extract_gt6_food_stats.py reads.
VANILLA = {
    'carrot': 'minecraft:carrot',
    'poisonous_potato': 'minecraft:poisonous_potato',
    'potato': 'minecraft:potato',
    'baked_potato': 'minecraft:baked_potato',
    'apple': 'minecraft:apple',
    'bread': 'minecraft:bread',
    'egg': 'minecraft:egg',
}


def snake(name: str) -> str:
    """The port's id for a GT6 display name (tools/transpile_gt6_multiitems.py:41-45)."""
    s = name.lower()
    s = re.sub(r"[''.()&,!?]", "", s)
    s = re.sub(r"[^a-z0-9]+", "_", s).strip("_")
    return s


def strip_comments(text: str) -> str:
    """Blank out ``//`` and ``/* */`` comments without touching string literals.

    Comment characters become spaces (newlines are kept) so byte offsets - and therefore the
    line numbers reported for every row - still match the original file.
    """
    out = []
    i, n = 0, len(text)
    while i < n:
        ch = text[i]
        if ch == '"':
            j = i + 1
            while j < n:
                if text[j] == '\\':
                    j += 2
                    continue
                if text[j] == '"':
                    j += 1
                    break
                j += 1
            out.append(text[i:j])
            i = j
            continue
        if ch == '/' and i + 1 < n and text[i + 1] == '/':
            while i < n and text[i] != '\n':
                out.append(' ')
                i += 1
            continue
        if ch == '/' and i + 1 < n and text[i + 1] == '*':
            j = text.find('*/', i + 2)
            end = n if j < 0 else j + 2
            out.append(''.join('\n' if text[k] == '\n' else ' ' for k in range(i, end)))
            i = end
            continue
        out.append(ch)
        i += 1
    return ''.join(out)


def _split_top(text: str) -> list:
    """Split a Java argument list on commas at depth 0 (string- and bracket-aware)."""
    parts, depth, current, i, n = [], 0, [], 0, len(text)
    while i < n:
        ch = text[i]
        if ch == '"':
            j = i + 1
            while j < n:
                if text[j] == '\\':
                    j += 2
                    continue
                if text[j] == '"':
                    j += 1
                    break
                j += 1
            current.append(text[i:j])
            i = j
            continue
        if ch in '([{':
            depth += 1
        elif ch in ')]}':
            depth -= 1
        if ch == ',' and depth == 0:
            parts.append(''.join(current).strip())
            current = []
            i += 1
            continue
        current.append(ch)
        i += 1
    if current:
        parts.append(''.join(current).strip())
    return parts


def _call_body(text: str, start: int) -> str:
    """The argument text of the call whose ``(`` is at ``start`` (string- and bracket-aware)."""
    depth = 0
    i, n = start, len(text)
    while i < n:
        ch = text[i]
        if ch == '"':
            j = i + 1
            while j < n:
                if text[j] == '\\':
                    j += 2
                    continue
                if text[j] == '"':
                    j += 1
                    break
                j += 1
            i = j
            continue
        if ch == '(':
            depth += 1
        elif ch == ')':
            depth -= 1
            if depth == 0:
                return text[start + 1:i]
        i += 1
    return ''


def parse_temperature(expr: str):
    """``C+37`` -> 310 K (``CS.C = 273``, ``CS.java:132``); ``C`` alone -> 273."""
    expr = expr.strip()
    if expr == 'C':
        return 273, 'C'
    match = re.fullmatch(r'C\s*([+-])\s*(\d+)', expr)
    if match:
        offset = int(match.group(2)) * (1 if match.group(1) == '+' else -1)
        return 273 + offset, expr
    match = re.fullmatch(r'(\d+)', expr)
    if match:
        return int(match.group(1)), expr
    return None, expr


def parse_container(expr: str, meta_to_id: dict):
    """GT6's ``aEmptyContainer``: the stack ``FoodStat.onEaten`` hands back (``FoodStat.java:146-151``).

    Three shapes occur in ``MultiItemFood``:
      * ``ST.make(this, 1, <meta>)`` - another row of the same multi-item (the apple cores),
      * ``IL.Stick.get(n)`` - ``LoaderItemList.java:774`` aliases it onto vanilla's ``Items.stick``,
      * ``OP.scrapGt.mat(MT.<Material>, n)`` - a material-prefix scrap, whose port id is
        ``MaterialPrefix.getItemId`` = ``<registryName>_<material>`` (``MaterialPrefix.java:643-645``).
    """
    expr = ' '.join(expr.split())
    if expr in ('null', 'NI', ''):
        return '', 0
    match = re.fullmatch(r'ST\.make\(\s*this\s*,\s*(\d+)\s*,\s*(\d+)\s*\)', expr)
    if match:
        target = meta_to_id.get(int(match.group(2)))
        return (target, int(match.group(1))) if target else (None, 0)
    match = re.fullmatch(r'IL\.Stick\.get\(\s*(\d+)\s*\)', expr)
    if match:
        return 'minecraft:stick', int(match.group(1))
    match = re.fullmatch(r'OP\.scrapGt\.mat\(\s*MT\.(\w+)\s*,\s*(\d+)\s*\)', expr)
    if match:
        return 'gt6ce:scrap_gt_' + port_material_id(match.group(1)), int(match.group(2))
    return None, 0


# GT6's material *constants* whose spelling differs from the port's material *name*: the port names a
# material item ``<prefix registryName>_<material name lowercased>`` (MaterialPrefix.java:643-645), and
# GT6's ``MT.Al`` is the material called "Aluminium". Only the constants that actually occur in
# MultiItemFood's containers are listed; anything else keeps GT6's own spelling, which is what the port
# uses for every constant that is spelled out (MT.Paper -> "paper").
GT6_MATERIAL_NAMES = {
    'Al': 'Aluminium',
    'Cu': 'Copper',
    'Fe': 'Iron',
    'Sn': 'Tin',
    'Pb': 'Lead',
    'Zn': 'Zinc',
    'Ni': 'Nickel',
    'Ag': 'Silver',
    'Au': 'Gold',
    'W': 'Tungsten',
    'Ti': 'Titanium',
    'Steel': 'Steel',
}


def port_material_id(gt6_constant: str) -> str:
    """The port's material id for a GT6 ``MT.<constant>`` reference."""
    return GT6_MATERIAL_NAMES.get(gt6_constant, gt6_constant).lower()


def parse_food_stat(expr: str):
    """Parse a ``new FoodStat(...)`` expression into GT6's own numbers, or ``(None, reason)``."""
    match = re.search(r'new\s+FoodStat\s*\(', expr)
    if not match:
        return None, None
    args = _split_top(_call_body(expr, match.end() - 1))
    if len(args) < 7:
        return None, 'FoodStat with %d arguments' % len(args)
    numbers = {}
    try:
        numbers['foodLevel'] = int(args[0])
        numbers['saturation'] = float(args[1].rstrip('Ff'))
        numbers['hydration'] = int(float(args[2].rstrip('Ff')))
        temperature, raw_temperature = parse_temperature(args[3])
        if temperature is None:
            return None, 'temperature expression %s' % raw_temperature
        numbers['temperature'] = temperature
        numbers['temperatureExpr'] = raw_temperature
        numbers['temperatureEffect'] = args[4]
    except (ValueError, IndexError) as error:
        return None, 'FoodStat header %s (%s)' % (args[:5], error)

    # Everything between the five header numbers and the EnumAction argument is the statistics block:
    # five ints (alcohol..fat) or six (alcohol..fat + radiation), or nothing at all.
    index = 5
    stats = []
    while index < len(args) and not args[index].startswith('EnumAction'):
        stats.append(args[index])
        index += 1
    if index >= len(args):
        return None, 'FoodStat without an EnumAction argument'
    if len(stats) not in (0, 5, 6):
        return None, 'FoodStat with %d statistics' % len(stats)
    try:
        stats = [int(value) for value in stats]
    except ValueError:
        return None, 'FoodStat statistics %s' % stats
    stats = (stats + [0] * 6)[:6]
    (numbers['alcohol'], numbers['caffeine'], numbers['dehydration'],
     numbers['sugar'], numbers['fat'], numbers['radiation']) = stats

    numbers['action'] = args[index]
    tail = args[index + 1:]
    numbers['container'] = tail[0] if tail else 'null'
    booleans = tail[1:5]
    if len(booleans) != 4:
        return None, 'FoodStat without the four booleans (tail %s)' % tail
    if not all(value in ('T', 'F') for value in booleans):
        return None, 'FoodStat booleans %s' % booleans
    numbers['alwaysEdible'] = booleans[0] == 'T'
    numbers['invisibleParticles'] = booleans[1] == 'T'
    numbers['rotten'] = booleans[2] == 'T'
    numbers['autoDetectEmpty'] = booleans[3] == 'T'
    numbers['effects'] = tail[5:]
    return numbers, None


def parse_multi_item_food():
    """Yield one dict per GT6 food item, in GT6's meta order."""
    text = strip_comments(io.open(MULTI_FOOD, encoding='utf-8', errors='replace').read())
    line_of = []
    position = 0
    for number, line in enumerate(text.split('\n'), 1):
        line_of.append((position, number))
        position += len(line) + 1

    def line_at(offset: int) -> int:
        low, high = 0, len(line_of) - 1
        while low < high:
            middle = (low + high + 1) // 2
            if line_of[middle][0] <= offset:
                low = middle
            else:
                high = middle - 1
        return line_of[low][1]

    rows = []
    for match in re.finditer(r'\baddItem\s*\(', text):
        body = _call_body(text, match.end() - 1)
        args = _split_top(body)
        if not args or not re.fullmatch(r'\d+', args[0]):
            continue
        if len(args) < 2 or not args[1].startswith('"'):
            continue
        meta = int(args[0])
        display = re.sub(r'\\(.)', r'\1', args[1][1:-1])
        line = line_at(match.start())
        prefix = text[max(0, match.start() - 220):match.start()]
        field = re.findall(r'IL\.(\w+)\s*\.\s*set\(\s*$', prefix)
        stat, reason = parse_food_stat(body)
        rows.append({
            'meta': meta,
            'display': display,
            'field': field[-1] if field else '',
            'line': line,
            'call': ' '.join(body.split()),
            'stat': stat,
            'reason': reason,
            'vanilla': None,
        })

    # `IL.Food_X.set(ST.make(Items.y, 1, 0))` rows: GT6 aliases those onto a vanilla item.
    for match in re.finditer(r'IL\.(\w+)\s*\.\s*set\(\s*(ST\.make\(\s*Items\.(\w+)[^)]*\))', text):
        name = match.group(3)
        if name not in VANILLA:
            continue
        rows.append({
            'meta': -1,
            'display': '',
            'field': match.group(1),
            'line': line_at(match.start()),
            'call': ' '.join(match.group(2).split()),
            'stat': None,
            'reason': None,
            'vanilla': VANILLA[name],
        })
    return rows


def port_food_ids():
    """The port's ``food`` category ids, in ``GTMultiItemsGen`` order."""
    text = io.open(MULTI_GEN, encoding='utf-8', errors='replace').read()
    return [(match.group(1), re.sub(r'\\(.)', r'\1', match.group(2)))
            for match in re.finditer(
                r'^\s*"([a-z0-9_]+)", "((?:\\.|[^"\\])*)", "food", "[T]?",\s*$', text, re.M)]


def oredict_names():
    """``addItem`` rows keyed by the ore-dictionary names in their argument list.

    GT6's ``addItem(meta, display, tooltip?, <oreDictName>?, <FoodStat or OreDictItemData>?, ...)``
    passes the ore-dictionary name as the first plain string after the tooltip (``"foodCheese"``,
    ``"cropLemon"``, ``"foodRaisins"``). ``Loader_Recipes_Food``/``Loader_Recipes_Crops`` hook those
    names, so this is also the link between a listener and the port item it would have fed.

    Returns ``{oreDictName: (portId, line)}``; the rows are the same 266 verified 1:1 rows.
    """
    rows = parse_multi_item_food()
    rows = sorted((row for row in rows if row['meta'] >= 0 and row['display']),
                  key=lambda row: row['meta'])
    ids = port_food_ids()
    out = {}
    for row, (port_id, _display) in zip(rows, ids):
        args = _split_top(row['call'])
        for arg in args[3:]:
            if re.fullmatch(r'"[A-Za-z][A-Za-z0-9_]*"', arg):
                out.setdefault(arg[1:-1], (port_id, row['line']))
                break
    return out


def build():
    """Parse GT6 and return ``(entries, skipped, vanilla_rows, stubs)``.

    Shared with ``tools/extract_gt6_food_stats.py``, which turns the same rows into the port's
    ``GTFoodStats`` table: GT6 never puts its own items into ``CS.FoodsGT`` (``RM.crop`` skips every
    ``MultiItemRandom`` stack, ``RM.java:776``), so the ``FoodStat`` numbers read here are the only
    source the port has for them.
    """
    rows = parse_multi_item_food()
    rows.sort(key=lambda row: (row['meta'] if row['meta'] >= 0 else 10 ** 6, row['line']))
    items = [row for row in rows if row['meta'] >= 0 and row['display']]
    vanilla_rows = [row for row in rows if row['vanilla']]
    stubs = [row for row in rows if row['meta'] >= 0 and not row['display']]

    entries, skipped = [], []
    for row in stubs:
        skipped.append('%s (meta %d, GT6\'s hidden ID-migration stub: an empty display name and '
                       'TD.Creative.HIDDEN, MultiItemFood.java:%d)'
                       % (row['field'] or 'addItem', row['meta'], row['line']))
    for row in vanilla_rows:
        skipped.append('%s (meta -1: GT6 aliases this row onto the vanilla item %s, MultiItemFood.java:%d; '
                       'its numbers come from the explicit FoodsGT.put, not from a FoodStat)'
                       % (row['field'], row['vanilla'], row['line']))

    port_ids = port_food_ids()
    if len(port_ids) != len(items):
        raise SystemExit('FATAL: GT6 has %d food items but the port registers %d '
                         '(GTMultiItemsGen "food")' % (len(items), len(port_ids)))
    meta_to_id = {}

    unresolved = []
    for row, (port_id, port_display) in zip(items, port_ids):
        if row['display'] != port_display:
            unresolved.append('%s (GT6 %r vs port %r at GT6 line %d)'
                              % (port_id, row['display'], port_display, row['line']))
            continue
        base = re.sub(r'_\d+$', '', port_id)
        if base != snake(row['display']):
            unresolved.append('%s (port id does not match snake(%r), GT6 line %d)'
                              % (port_id, row['display'], row['line']))
            continue
        row['portId'] = port_id
        meta_to_id[row['meta']] = port_id
    if unresolved:
        for note in unresolved:
            print('  FATAL ' + note)
        raise SystemExit('FATAL: %d GT6 food rows did not line up with the port' % len(unresolved))

    for row in items:
        port_id = row['portId']
        if row['reason']:
            skipped.append('%s (%s, MultiItemFood.java:%d)' % (port_id, row['reason'], row['line']))
            continue
        stat = row['stat']
        container, container_count = parse_container(
            stat['container'] if stat else 'null', meta_to_id)
        if stat and stat['container'] not in ('null', 'NI') and container is None:
            skipped.append('%s (GT6 returns the empty container %s to the player, '
                           'FoodStat.java:146-151 / MultiItemFood.java:%d: the port has no such item)'
                           % (port_id, stat['container'], row['line']))
        if stat is None:
            container, container_count = '', 0
        entries.append({
            'id': port_id, 'display': row['display'], 'meta': row['meta'],
            'foodLevel': stat['foodLevel'] if stat else 0,
            'saturation': stat['saturation'] if stat else 0.0,
            'hydration': stat['hydration'] if stat else 0,
            'temperature': stat['temperature'] if stat else 0,
            'alcohol': stat['alcohol'] if stat else 0,
            'caffeine': stat['caffeine'] if stat else 0,
            'dehydration': stat['dehydration'] if stat else 0,
            'sugar': stat['sugar'] if stat else 0,
            'fat': stat['fat'] if stat else 0,
            'radiation': stat['radiation'] if stat else 0,
            'alwaysEdible': stat['alwaysEdible'] if stat else False,
            'rotten': stat['rotten'] if stat else False,
            'container': container or '',
            'containerCount': container_count,
            'hasStat': stat is not None,
            'line': row['line'],
        })
        if stat and stat['effects']:
            skipped.append('%s (%s potion effect(s) %s, MultiItemFood.java:%d: GT6 applies them in '
                           'FoodStat.onEaten:157-159, but GT6\'s own PotionsGT ids have no 1.20.1 '
                           'counterpart and the vanilla Potion.* subset is out of this batch\'s scope)'
                           % (port_id, len(stat['effects']) // 4, ','.join(stat['effects']), row['line']))

    # GT6's FoodStat temperature/hydration feed the ENVM body tracker (MultiItemRandom.java:308-316).
    skipped.append('FoodStat temperature/temperatureEffect (every row above, e.g. 310 K for C+37): '
                   'the port has no ENVM body-temperature tracker (MultiItemRandom.java:308-316)')
    skipped.append('FoodStat hydration (0..100, negative = salty): ENVM tracker only, same source')
    return entries, skipped, vanilla_rows, stubs


def _java_doc():
    return [
        '/**',
        ' * GENERATED by tools/extract_gt6_food_items.py - do not edit by hand.',
        ' *',
        " * <p>GT6's food items: every {@code addItem(...)} row of {@code gregtech/items/MultiItemFood.java}",
        ' * with the numbers of its {@code new FoodStat(...)} argument. {@code foodLevel}/{@code saturation}',
        ' * are what GT6 hands to vanilla food data ({@code MultiItemRandom.java:297-304}), the six',
        ' * statistics [alcohol, caffeine, dehydration, sugar, fat, radiation] are what',
        ' * {@code FoodStat.onEaten} pushes into {@code EntityFoodTracker} ({@code FoodStat.java:164-172}).',
        ' *',
        " * <p>{@code id} is the port's own item id: the rows are lined up 1:1 against the {@code food}",
        ' * category of {@code GTMultiItemsGen} (both 266 entries, verified by the extractor), so the ids',
        " * are read from the port's registration instead of derived from GT6's display names.",
        ' */',
    ]


def render(entries, skipped):
    """Render GTFoodItemsGen.java."""
    lines = []
    add = lines.append
    add('package com.gregtech.gregtech.data.generated;')
    add('')
    add('import com.gregtech.gregtech.content.food.GTFoodItems;')
    add('')
    add('import java.util.List;')
    add('')
    for line in _java_doc():
        add(line)
    add('public final class GTFoodItemsGen {')
    add('    private GTFoodItemsGen() {}')
    add('')
    add('    public static final List<GTFoodItems.Entry> ROWS = List.of(')
    for index, entry in enumerate(entries):
        comma = ',' if index < len(entries) - 1 else ''
        add('            new GTFoodItems.Entry("%s", "%s", %d,'
            % (entry['id'], entry['display'].replace('"', '\\"'), entry['meta']))
        add('                    new GTFoodItems.Food(%d, %sf, %d, %d, %s, %s),'
            % (entry['foodLevel'], _float(entry['saturation']), entry['hydration'], entry['temperature'],
               _bool(entry['alwaysEdible']), _bool(entry['rotten'])))
        add('                    new GTFoodItems.Stats(%d, %d, %d, %d, %d, %d), "%s", %d, %s, %d)%s'
            % (entry['alcohol'], entry['caffeine'], entry['dehydration'], entry['sugar'],
               entry['fat'], entry['radiation'], entry['container'], entry['containerCount'],
               _bool(entry['hasStat']), entry['line'], comma))
    add('    );')
    add('')
    add('    /** GT6 food rows the port cannot express as a table entry (never dropped silently). */')
    add('    public static final List<String> SKIPPED = List.of(')
    notes = sorted(set(skipped))
    for index, note in enumerate(notes):
        comma = ',' if index < len(notes) - 1 else ''
        add('            "%s"%s' % (note.replace('\\', '\\\\').replace('"', '\\"'), comma))
    add('    );')
    add('')
    add('    /**')
    add("     * GT6 food rows the port does <em>not</em> register, with the reason. Empty, and that is the")
    add('     * point: all %d rows of {@code MultiItemFood} line up 1:1 with the {@code food} category of' % len(entries))
    add('     * {@code GTMultiItemsGen} (the extractor fails loudly if a single row stops matching), and the')
    add("     * one row GT6 itself does not register is the hidden ID-migration stub in {@link #SKIPPED}.")
    add('     * {@code registry/GTFoodItems} registers whatever this list holds.')
    add('     */')
    add('    public static final List<String> GAP = List.of();')
    add('}')
    return '\n'.join(lines) + '\n', notes


def main() -> int:
    check = '--check' in sys.argv
    report = '--report' in sys.argv
    entries, skipped, vanilla_rows, stubs = build()
    text, notes = render(entries, skipped)

    with_stats = [entry for entry in entries if entry['hasStat']]
    tracked = [entry for entry in with_stats
               if any(entry[key] for key in ('alcohol', 'caffeine', 'dehydration', 'sugar', 'fat',
                                             'radiation'))]
    containers = [entry for entry in entries if entry['container']]
    print('rows: %d (with a FoodStat: %d, carrying statistics: %d, display-only: %d)'
          % (len(entries), len(with_stats), len(tracked), len(entries) - len(with_stats)))
    print('empty containers: %d, vanilla-aliased rows: %d, hidden stubs: %d, skipped notes: %d'
          % (len(containers), len(vanilla_rows), len(stubs), len(notes)))
    if report:
        print()
        print('| port id | GT6 | display | food | sat | alc | caf | deh | sug | fat | rad | GT6 line |')
        print('|---|---|---|---|---|---|---|---|---|---|---|---|')
        for entry in entries:
            print('| `%s` | %d | %s | %d | %s | %d | %d | %d | %d | %d | %d | MultiItemFood.java:%d |'
                  % (entry['id'], entry['meta'], entry['display'], entry['foodLevel'],
                     _float(entry['saturation']), entry['alcohol'], entry['caffeine'],
                     entry['dehydration'], entry['sugar'], entry['fat'], entry['radiation'],
                     entry['line']))

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


def _float(value) -> str:
    return ('%g' % float(value))


def _bool(value) -> str:
    return 'true' if value else 'false'


if __name__ == '__main__':
    sys.exit(main())
