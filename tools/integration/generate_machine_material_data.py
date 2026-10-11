"""Import known GT6 CR.REV machine inputs, without selecting arbitrary tag members.

Source trees are read-only. Java comments are masked before parsing, so disabled
registrations cannot enter the table. Amounts use CS.U and integer arithmetic.
Unknown automatic ore data is retained in the audit, rather than assigned 1U.
"""
from __future__ import annotations

import argparse
import ast
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
import sys

REPO = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO / 'tools'))
from extract_basic_machine_recipes import match_paren, split_top_level, machine_key

U = 648_648_000


def masked(text):
    return re.sub(r'"(?:\\.|[^"\\])*"|//[^\n]*|/\*[\s\S]*?\*/',
                  lambda m: m[0] if m[0].startswith('"') else re.sub(r'[^\n]', ' ', m[0]), text)


def norm(text):
    return re.sub(r'\s+', '', text)


def calls(text, name):
    for match in re.finditer(re.escape(name) + r'\s*\(', text):
        opening = text.index('(', match.start())
        closing = match_paren(text, opening)
        yield match.start(), split_top_level(text[opening + 1:closing])


def amount(expression):
    expression = re.sub(r'\bU(\d*)\b', lambda m: str(U // int(m[1] or 1)), norm(expression))
    expression = re.sub(r'(?<=\d)[lL]\b', '', expression)

    def calculate(node):
        if isinstance(node, ast.Constant) and isinstance(node.value, int): return node.value
        if isinstance(node, ast.UnaryOp) and isinstance(node.op, ast.USub): return -calculate(node.operand)
        if isinstance(node, ast.BinOp):
            a, b = calculate(node.left), calculate(node.right)
            if isinstance(node.op, ast.Mult): return a * b
            if isinstance(node.op, (ast.Div, ast.FloorDiv)): return a // b
            if isinstance(node.op, ast.Add): return a + b
            if isinstance(node.op, ast.Sub): return a - b
        raise ValueError('Unsupported source amount: ' + expression)
    return calculate(ast.parse(expression, mode='eval').body)


def recipe(arguments):
    patterns = []
    for value in arguments:
        if re.fullmatch(r'"[^"]*"', value): patterns.append(value[1:-1])
        else: break
    keys = {}
    if not patterns: return patterns, keys
    tail = arguments[len(patterns):]
    for i in range(0, len(tail) - 1, 2):
        if not re.fullmatch(r"'(.)'", tail[i]): raise ValueError('Unsupported shaped recipe: ' + str(tail))
        keys[tail[i][1]] = tail[i + 1]
    return patterns, keys


def java_material(symbol):
    if symbol in ('Wood', 'WoodTreated'): return 'com.gregtech.gregtech.data.generated.GT6Materials.Woods.' + symbol
    if symbol in ('Stone', 'NetherBrick'): symbol = 'STONES.' + symbol
    if symbol in ('STONES.GraniteBlack', 'STONES.GraniteRed'): symbol = symbol.removeprefix('STONES.')
    return 'ImportedMaterialData.' + symbol


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--source', type=Path, required=True)
    ap.add_argument('--audit', type=Path, required=True)
    ap.add_argument('--out', type=Path, required=True)
    ap.add_argument('--smeltery-out', type=Path)
    ns = ap.parse_args()
    root = ns.source / 'src/main/java'
    relative = ['gregtech/loaders/b/Loader_MultiTileEntities.java', 'gregtech/items/MultiItemTechnological.java',
                'gregapi/data/MT.java', 'gregapi/data/ANY.java', 'gregapi/data/OP.java',
                'gregapi/load/LoaderItemData.java', 'gregapi/load/LoaderOreDictReRegistrations.java']
    sources = {name: masked((root / name).read_text(encoding='utf-8')) for name in relative}
    mte, tech, mt, any_source, op, item_data, automatic_source = [sources[n] for n in relative]
    arrays = {m[1]: split_top_level(m[2]) for m in re.finditer(r'(\w+)\s*=\s*\{([^{}]*)\}', mt)}
    targets = {m[1]: norm(m[2]) for m in re.finditer(r'^\s*(\w+)\s*\.[^\n]*?\.setAllToTheOutputOf\(([^)]*)\)', any_source, re.M)}
    weights = {m[1]: amount(split_top_level(m[2])[0]) for m in re.finditer(r'^\s*(\w+)\s*=\s*create\([^\n]*?\.setMaterialStats\(([^)]*)\)', op, re.M)}
    # OreDictPrefix.mAmount defaults to -1. CR.REV excludes nonpositive data;
    # stone.dat(...) does not acquire the material amount of a chosen stone item.
    for match in re.finditer(r'^\s*(\w+)\s*=\s*create\(', op, re.M):
        weights.setdefault(match[1], -1)
    byproducts = {}
    for m in re.finditer(r'(\w+)\s*\.mByProducts.add\(OM.stack\(([^,]+),\s*([^)]*)\)\)', op):
        weight = re.sub(r'(\w+)\.mAmount', lambda p: str(weights[p[1]]), m[3])
        byproducts[m[1]] = (norm(m[2]), amount(weight))
    direct = {}
    recipes = {}
    unknown = Counter()
    unresolved_items = Counter()
    items_without_data = Counter()
    used = set()

    def material(value):
        value = norm(value)
        a = re.fullmatch(r'MT.DATA.(\w+)\[(\d+)\]', value)
        if a: return material(arrays[a[1]][int(a[2])])
        if value.startswith('ANY.'):
            key = value[4:]
            if key not in targets: raise ValueError('Unresolved original ANY output: ' + value)
            return material(targets[key])
        return value.removeprefix('MT.')

    def pairs(arguments):
        if arguments and arguments[0].startswith('OM.stack('):
            arguments = [value for a in arguments for _, parts in calls(a, 'OM.stack') for value in parts]
        result = Counter()
        for i in range(0, len(arguments), 2):
            quantity = re.sub(r'OP\.(\w+)\.mAmount', lambda p: str(weights[p[1]]), norm(arguments[i + 1]))
            result[material(arguments[i])] += amount(quantity)
        return result

    for _, a in calls(automatic_source, 'OreDictManager.INSTANCE.setAutomaticItemData'):
        datum = list(calls(a[1], 'new OreDictItemData'))
        if datum: direct[norm(a[0])] = pairs(datum[0][1])
    for _, a in calls(item_data, 'OM.data'):
        if len(a) < 3: continue
        found = re.fullmatch(r'ST.make\((Blocks\.\w+|Items\.\w+),1,(W|\d+)\)', norm(a[0]))
        if found:
            try:
                datum = pairs(a[1:])
                direct[found[1] + '#' + found[2]] = datum
                if found[2] == 'W': direct[found[1]] = datum
            except (ValueError, SyntaxError, IndexError): pass # Nonconstant/conditional entries are outside this importer.
    for offset, a in calls(tech, 'CR.shaped'):
        if len(a) < 3 or 'REV' not in a[1]: continue
        output = re.fullmatch(r'(IL\.[\w]+(?:\[[\w]+\])?)\.get\((\d+)\)', norm(a[0]))
        if not output: continue
        iterations = range(10) if '[i]' in norm(a[0]) else [None]
        for tier in iterations:
            args = [re.sub(r'\bi\b', str(tier), v) if tier is not None else v for v in a]
            key = re.sub(r'\bi\b', str(tier), output[1]) if tier is not None else output[1]
            patterns, keys = recipe(args[2:])
            recipes[key] = (patterns, keys, '', int(output[2]), 'MultiItemTechnological:' + str(tech.count('\n', 0, offset) + 1))
    # Explicit item data and copied laser-emitter shells take priority over recipe guesses.
    for match in re.finditer(r'(IL\.\w+)\s*\.set\(addItem\(', tech):
        opening = tech.index('(', match.end() - len('addItem('))
        body = tech[opening + 1:match_paren(tech, opening)]
        datum = list(calls(body, 'new OreDictItemData'))
        if datum:
            try: direct[norm(match[1])] = pairs(datum[0][1])
            except (ValueError, SyntaxError, IndexError): pass
        copied = re.search(r'OM.data\((IL\.\w+)\.get\(1\)\)', body)
        if copied: direct[norm(match[1])] = norm(copied[1])
    def registry_calls():
        for offset, arguments in calls(mte, 'aRegistry.add'):
            if len(arguments) > 2 and norm(arguments[2]) in ('10080+i', '10090+i'):
                for tier in range(10):
                    replaced = [re.sub(r'\bi\b', str(tier), v) for v in arguments]
                    replaced[2] = str(amount(replaced[2]))
                    yield offset, replaced
            else: yield offset, arguments

    registrations = []
    registered_ids = set()
    previous_registry_id = None
    for offset, a in registry_calls():
        current_id = int(a[2]) if len(a) > 2 and re.fullmatch(r'\d+', a[2]) else None
        if current_id is not None: registered_ids.add(current_id)
        if len(a) < 9:
            previous_registry_id = current_id
            continue
        nbt = next((i for i, value in enumerate(a) if value.startswith('UT.NBT.make(')), None)
        sensor = a[1].strip('"') == 'Sensors' and norm(a[8]) == 'null'
        if nbt is None and not sensor:
            previous_registry_id = current_id
            continue
        if sensor: nbt = 8
        patterns, keys = recipe(a[nbt + 1:])
        # Only literal registry IDs can be referenced by aRegistry.getItem(id).
        if current_id is None:
            previous_registry_id = None
            continue
        prior = list(re.finditer(r'aMat\s*=\s*([^;]+);', mte[:offset]))
        casing = 'MT.NULL' if sensor else norm(prior[-1][1]) if prior else ''
        quality_material = casing
        declared = re.search(r'NBT_MATERIAL\s*,\s*([^,)]+)', a[nbt])
        if declared and norm(declared[1]) != 'aMat': casing = norm(declared[1])
        nbt_values = [] if sensor else next(calls(a[nbt], 'UT.NBT.make'))[1]
        nbt_parameters = {norm(nbt_values[i]): norm(nbt_values[i+1]) for i in range(0,len(nbt_values),2)}
        row = {'machine': machine_key(a[0]), 'tab': a[1].strip('"'), 'id': int(a[2]),
               'casing': casing, 'pattern': patterns, 'keys': keys, 'line': mte.count('\n', 0, offset) + 1,
               'quality_material': quality_material, 'harvest_expression': norm(a[5]), 'block_group': norm(a[7]),
               'nbt_parameters': nbt_parameters}
        registrations.append(row)
        recipe_keys = keys.copy()
        if any('aRegistry.getItem()' in norm(v) for v in recipe_keys.values()):
            if previous_registry_id is None: raise ValueError(f'Unresolved previous source item at {row["line"]}')
            row['previous_registry_id'] = previous_registry_id
            recipe_keys = {k: norm(v).replace('aRegistry.getItem()', f'aRegistry.getItem({previous_registry_id})') for k, v in keys.items()}
        if patterns: recipes[f'aRegistry.getItem({a[2]})'] = (patterns, recipe_keys, casing, 1, f'Loader_MultiTileEntities:{row["line"]}')
        line_end = mte.find('\n', offset)
        following = mte[offset:line_end if line_end >= 0 else len(mte)]
        for _, datum in calls(following, 'OM.data'):
            if norm(datum[0]) == 'aRegistry.getItem()':
                direct[f'aRegistry.getItem({a[2]})'] = pairs([casing if norm(v) == 'aMat' else v for v in datum[1:]])
        for setter in re.finditer(r'(IL\.\w+)\.set\(', following):
            for _, values in calls(following, setter[1] + '.set'):
                if norm(values[0]) != 'aRegistry.getItem()': continue
                datum = list(calls(values[1], 'new OreDictItemData')) if len(values) > 1 else []
                if datum:
                    direct[f'aRegistry.getItem({a[2]})'] = pairs([casing if norm(v) == 'aMat' else v for v in datum[0][1]])
                direct[setter[1]] = f'aRegistry.getItem({a[2]})'
        previous_registry_id = current_id
    direct['aRegistry.getItem(1005)'] = Counter({'Ceramic': U * 7}) # Explicit IL.Ceramic_Crucible.set data.

    memo = {}
    def resolve(expression, casing='', stack=()):
        expression = norm(expression)
        registry_id = re.fullmatch(r'aRegistry.getItem\(([\d+*\-]+)\)', expression)
        if registry_id and not registry_id[1].isdigit():
            return resolve(f'aRegistry.getItem({amount(registry_id[1])})', casing, stack)
        conditional = re.fullmatch(r'MD.HBM.mLoaded\?(.+):(.+)', expression)
        if conditional:
            left, right = resolve(conditional[1], casing, stack), resolve(conditional[2], casing, stack)
            if left != right: raise ValueError('Provider-dependent source material: ' + expression)
            return left # Both original branches have the identical prefix/material amount.
        if expression == 'aMat': return Counter({material(casing): U})
        array = re.fullmatch(r'MT.DATA.(\w+)\[(\d+)\]', expression)
        if array: return resolve(arrays[array[1]][int(array[2])], casing, stack)
        prefix = re.fullmatch(r'OP\.(\w+)\.dat\((.+)\)', expression)
        made = re.fullmatch(r'OP\.(\w+)\.mat\(([^,]+),(\d+)\)', expression)
        if made:
            result = resolve(f'OP.{made[1]}.dat({made[2]})', casing, stack)
            return Counter({key: value * int(made[3]) for key, value in result.items()})
        if prefix:
            name, mat = prefix[1], prefix[2]
            if weights[name] <= 0:
                unknown[expression + ' [original prefix amount <= 0]'] += 1
                return Counter()
            result = Counter({material(casing if mat == 'aMat' else mat): weights[name]})
            if name in byproducts:
                bymat, weight = byproducts[name]
                result[material(bymat)] += weight
            return result
        if expression in direct:
            return resolve(direct[expression], casing, stack) if isinstance(direct[expression], str) else direct[expression].copy()
        vanilla_stack = re.fullmatch(r'ST.make\((Blocks\.\w+|Items\.\w+),(\d+),(W|\d+)\)', expression)
        if vanilla_stack:
            datum = direct.get(vanilla_stack[1] + '#' + vanilla_stack[3], direct.get(vanilla_stack[1] + '#W'))
            if datum is not None: return Counter({key: value * int(vanilla_stack[2]) for key, value in datum.items()})
        if expression in recipes:
            if expression in stack: raise ValueError('Source material dependency cycle: ' + expression)
            if expression not in memo:
                patterns, keys, ctx, count, origin = recipes[expression]
                total = Counter()
                for char in ''.join(patterns):
                    if char in keys: total.update(resolve(keys[char], ctx, stack + (expression,)))
                memo[expression] = Counter({k: v // count for k, v in total.items() if v // count > 0})
                used.add(origin)
            return memo[expression].copy()
        # A missing registered item is different from an ore key without automatic data.
        if expression.startswith('aRegistry.getItem('):
            source_id = re.fullmatch(r'aRegistry.getItem\((\d+)\)', expression)
            if source_id and int(source_id[1]) in registered_ids: items_without_data[expression] += 1
            else: unresolved_items[expression] += 1
        else: unknown[expression] += 1
        return Counter()

    def missing_registry_refs(expression, seen=()):
        expression = norm(expression)
        registry_id = re.fullmatch(r'aRegistry.getItem\(([\d+*\-]+)\)', expression)
        if registry_id and not registry_id[1].isdigit():
            expression = f'aRegistry.getItem({amount(registry_id[1])})'
        if expression in seen: return set()
        if expression in direct:
            return missing_registry_refs(direct[expression], seen + (expression,)) if isinstance(direct[expression], str) else set()
        if expression in recipes:
            _, keys, _, _, _ = recipes[expression]
            return set().union(*(missing_registry_refs(value, seen + (expression,)) for value in keys.values()))
        source_id = re.fullmatch(r'aRegistry.getItem\((\d+)\)', expression)
        return {expression} if source_id and int(source_id[1]) not in registered_ids else set()

    indexed = {}
    for row in registrations:
        if row['tab'] not in ('Basic Machines', 'Multiblock Machines'): continue
        tier = re.search(r'\[(\d+)\]', row['casing'])
        indexed.setdefault(row['machine'], {})[int(tier[1]) if tier else 1] = row
    params = (REPO / 'core/src/main/java/com/gregtech/gregtech/data/BasicMachineOriginalParams.java').read_text(encoding='utf-8')
    output = []
    for match in re.finditer(r'new Params\("([^"]+)", (\d+), "([^"]+)"', params):
        name, tier, original = match[1], int(match[2]), match[3]
        options = indexed.get(original, {})
        row = options.get(tier) or (next(iter(options.values())) if len(options) == 1 else None)
        if row is None: raise ValueError(f'Missing active source registration: {name}/{tier}/{original}')
        missing = sorted(missing_registry_refs(f'aRegistry.getItem({row["id"]})'))
        if missing: raise ValueError(f'Original CR.shaped rejects null item input: {name}/{tier}/{missing}')
        parts = resolve(f'aRegistry.getItem({row["id"]})')
        if not parts: raise ValueError(f'Machine has no known source material: {name}/{tier}')
        output.append({'machine': name, 'tier': tier, 'source_id': row['id'], 'source_line': row['line'],
                       'components': dict(parts), 'pattern': row['pattern'], 'keys': row['keys']})

    # Stable port IDs for the audited original mechanical and burning-box registrations.
    mechanical = [('wood', 24800), ('bronze', 24810), ('brass', 24770), ('arsenic_copper', 24780),
                  ('arsenic_bronze', 24790), ('steel', 24820), ('titanium', 24830), ('tungstensteel', 24840),
                  ('iridium', 24850), ('iritanium', 24860), ('trinitanium', 24870), ('trinaquadalloy', 24880), ('adamantium', 24890)]
    bindings = {}
    sensor_catalog = REPO / 'core/src/main/java/com/gregtech/gregtech/api/sensor/SensorCatalog.java'
    for path, source_id in re.findall(r'new Entry\("([^"]+)",\s*"[^"]+",\s*(\d+),', masked(sensor_catalog.read_text(encoding='utf-8'))):
        bindings[int(source_id)] = path
    for suffix, base in mechanical:
        for size in range(4): bindings[base + size] = f'axle_{suffix}_{size + 1}'
        bindings[base + 9] = f'gearbox_{suffix}'
        bindings[base + 8] = f'rotation_transformer_{suffix}'
        bindings[base + 7] = 'engine_rotation_' + ('tungsten_steel' if suffix == 'tungstensteel' else suffix)
    tiers = ['lv', 'mv', 'hv', 'ev', 'iv']
    for index, tier in enumerate(tiers, 1):
        bindings[10020 + index] = 'electric_motor_' + tier
        bindings[10110 + index] = 'electric_dynamo_' + tier
        bindings[10000 + index] = 'electric_heater_' + tier
        bindings[10160 + index] = 'electric_cooler_' + tier
        bindings[11000 + index] = 'flux_heater_' + tier
        bindings[11160 + index] = 'flux_cooler_' + tier
        bindings[10030 + index] = 'electromagnet_' + tier
        bindings[11030 + index] = 'flux_magnet_' + tier
        if index <= 2:
            bindings[11020 + index] = 'flux_motor_' + tier
            bindings[11110 + index] = 'flux_dynamo_' + tier
    for index, suffix in enumerate(['bronze', 'steel', 'titanium', 'tungstensteel'], 1):
        bindings[16000 + index] = 'rotational_pump_' + suffix
    for index, step in enumerate(['ulv_lv', 'lv_mv', 'mv_hv', 'hv_ev', 'ev_iv', 'iv_luv', 'luv_zpm', 'zpm_uv', 'uv_xv']):
        bindings[10040 + index] = 'transformer_' + step
    for index, tier in enumerate(['ulv', *tiers, 'luv', 'zpm', 'uv', 'xv']):
        bindings[10080 + index] = 'battery_box_' + tier
        bindings[10090 + index] = 'energy_storage_' + tier
    bindings.update({10050: 'solar_panel_silicon', 10051: 'solar_panel_germanium',
                     1512: 'steam_turbine_bronze', 1515: 'steam_turbine_brass', 1518: 'steam_turbine_invar',
                     1522: 'steam_turbine_steel', 1525: 'steam_turbine_chromium'})
    bindings.update({1527: 'steam_turbine_ironwood', 1528: 'steam_turbine_steeleaf', 1529: 'steam_turbine_thaumium',
                     1530: 'steam_turbine_titanium', 1531: 'steam_turbine_fiery_steel', 1535: 'steam_turbine_aluminium',
                     1538: 'steam_turbine_magnalium', 1540: 'steam_turbine_void_metal', 1545: 'steam_turbine_trinitanium', 1548: 'steam_turbine_graphene'})
    bindings.update({32735: 'mortar_block', 32094: 'mortar_netherite', 32075: 'mortar_sapphire',
                     32076: 'mortar_diamond', 32089: 'mortar_amethyst', 32703: 'grindstone_block',
                     32702: 'sifting_table', 32706: 'mixing_bowl', 32705: 'mixing_bowl_table',
                     32722: 'juicer', 32708: 'bathing_pot', 32707: 'bathing_pot_table',
                     32721: 'bathing_pot_wood', 32720: 'bathing_pot_table_wood'})
    # Port controllers and structural parts retain their original numeric identities.
    bindings.update({17000: 'coke_oven_main', 17997: 'logistics_core', 17110: 'implosion_compressor_main',
                     17198: 'fusion_reactor_main', 17197: 'heat_exchanger_main', 17101: 'distillation_tower_main',
                     17111: 'cryo_distillation_main', 18000: 'coke_oven_wall',
                     18002: 'tank_wall', 18022: 'tank_wall_dense', 18023: 'implosion_compressor_wall',
                     18041: 'large_niobium_titanium_coil', 18100: 'centrifuge_part',
                     18101: 'heat_transmitter', 18102: 'distillation_tower_part', 18105: 'electrolyzer_part'})
    part_map = REPO / 'core/src/main/java/com/gregtech/gregtech/content/multiblock/SharedLargeMachineParts.java'
    for source_id, path in re.findall(r'new Part\((\d+),"([^"]+)"', masked(part_map.read_text(encoding='utf-8'))):
        bindings[int(source_id)] = path
    burning_families = {'Burning Box (Solid,': 'solid', 'Dense Burning Box (Solid,': 'solid_dense',
                       'Burning Box (Liquid,': 'liquid', 'Dense Burning Box (Liquid,': 'liquid_dense',
                       'Burning Box (Gas,': 'gas', 'Dense Burning Box (Gas,': 'gas_dense',
                       'Fluidized Bed Burning Box': 'fluidbed', 'Dense Fluidized Bed Burning Box': 'fluidbed_dense'}
    suffixes = {'Pb': 'lead', 'Bi': 'bismuth', 'Bronze': 'bronze', 'ArsenicCopper': 'arsenic_copper',
                'ArsenicBronze': 'arsenic_bronze', 'Invar': 'invar', 'Steel': 'steel', 'Cr': 'chromium', 'Ultimet': 'ultimet',
                'Ti': 'titanium', 'Netherite': 'netherite', 'W': 'tungsten', 'TungstenSteel': 'tungsten_steel', 'Ta4HfC5': 'ta4hfc5'}
    engine_suffixes = {**suffixes, 'SteelGalvanized': 'galvanized_steel', 'Al': 'aluminium', 'StainlessSteel': 'stainless_steel',
                       'Electrum': 'electrum', 'EnderiumBase': 'enderium_base', 'Enderium': 'enderium',
                       'TinAlloy': 'tin_alloy', 'Brass': 'brass', 'IronWood': 'ironwood', 'FierySteel': 'fiery_steel', 'Ir': 'iridium'}
    engine_families = {'Electric Engine': 'electric', 'Flux Engine': 'flux', 'Steam Engine': 'steam',
                       'Strong Steam Engine': 'steam_strong', 'Diesel Engine': 'diesel'}
    boiler_families = {'Steam Boiler Tank': 'steam_boiler', 'Strong Steam Boiler Tank': 'strong_steam_boiler'}
    for row in registrations:
        if row['machine'] in burning_families:
            bindings[row['id']] = 'burning_box_' + burning_families[row['machine']] + '_' + suffixes[material(row['casing'])]
        elif row['machine'] == 'Brick Burning Box (Solid)': bindings[row['id']] = 'burning_box_solid_brick'
        elif row['tab'] == 'Engines' and row['machine'] in engine_families:
            bindings[row['id']] = 'engine_' + engine_families[row['machine']] + '_' + engine_suffixes[material(row['casing'])]
        elif row['tab'] == 'Steam Boilers' and row['machine'] in boiler_families:
            bindings[row['id']] = boiler_families[row['machine']] + '_' + suffixes[material(row['casing'])]
    by_id = {row['id']: row for row in registrations}
    # Native IDs are already stable; associate all39 vessels and their four original companions.
    # Materials/amounts still come from source REV recipes or explicit IL item data above.
    crucibles = [(1005, 'smelting_crucible_ceramic')]
    definitions = (REPO/'core/src/main/java/com/gregtech/gregtech/api/machine/OriginalCrucibleDefinitions.java').read_text(encoding='utf-8')
    crucibles += [(int(number), path) for path, number in re.findall(
        r'CrucibleSpec.of\("(smelting_crucible_[^"]+)"\s*,\s*(?:GTMaterialRegistry.get\("[^"]+"\)|ImportedMaterialData\.[\w.]+)\s*,\s*(\d+)',definitions)]
    if len(crucibles) == 1:
        generated = (REPO/'core/src/main/java/com/gregtech/gregtech/api/machine/OriginalSmelteryDefinitions.java').read_text(encoding='utf-8')
        crucibles = [(int(number), path) for path,number in re.findall(r'add\("(smelting_crucible_[^"]+)", (\d+),', generated)]
    assert len(crucibles)==39 and len({number for number,_ in crucibles})==39
    for source_id,path in crucibles:
        bindings[source_id]=path
        suffix=path.removeprefix('smelting_crucible_')
        for offset,prefix in [(50,'mold_'),(750,'mold_basin_'),(850,'crucible_crossing_'),(700,'crucible_faucet_')]:
            bindings[source_id+offset]=prefix+suffix
    block_rows, empty_rows, invalid_rows = [], [], []
    for source_id, path in bindings.items():
        row = by_id[source_id]
        parts = resolve(f'aRegistry.getItem({source_id})')
        bound = {'path': path, 'source_id': source_id, 'source_line': row['line'], 'components': dict(parts),
                 'pattern': row['pattern'], 'keys': row['keys']}
        if 'previous_registry_id' in row: bound['previous_registry_id'] = row['previous_registry_id']
        missing = sorted(missing_registry_refs(f'aRegistry.getItem({source_id})'))
        if missing:
            bound['unresolved_source_item_references'] = missing
            bound['source_recipe_rejection'] = 'CR.shaped returns false on a null item input before reversible OM.data registration.'
            bound['known_parts_before_rejection'] = bound.pop('components')
            invalid_rows.append(bound)
        else: (block_rows if parts else empty_rows).append(bound)

    header = '''/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from CR.shaped, OreDictItemData, OP and Loader_MultiTileEntities.
 * Generated by tools/integration/generate_machine_material_data.py; source provenance is in docs/integration/verification. */
package com.gregtech.gregtech.content.machine;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.data.ImportedMaterialData;
import java.util.*;

/** Known source recipe components in exact CS.U units; unknown tag data is never replaced by an arbitrary ingredient. */
public final class OriginalMachineMaterialData {
    private OriginalMachineMaterialData() {}
    private static final Map<String, ItemComposition> DATA = new HashMap<>();
    private static final Map<String, ItemComposition> BLOCK_DATA = new HashMap<>();
    static { init0(); init1(); init2(); init3(); blocks0(); blocks1(); blocks2(); blocks3(); }
    public static Optional<ItemComposition> find(String machine, int tier) {
        return Optional.ofNullable(DATA.get(machine + "/" + tier));
    }
    public static List<MaterialChemistry.WeightedMaterial> weights(String machine, int tier) {
        return find(machine, tier).map(data -> data.components().stream()
                .map(part -> new MaterialChemistry.WeightedMaterial(part.material(), part.amount())).toList()).orElse(List.of());
    }
    public static Optional<ItemComposition> block(String path) { return Optional.ofNullable(BLOCK_DATA.get(path)); }
    public static Map<String, ItemComposition> blocks() { return Collections.unmodifiableMap(BLOCK_DATA); }
    private static MaterialComponent part(GTMaterial material, long amount) { return MaterialComponent.of(material, amount); }
    private static void add(String machine, int tier, int sourceId, MaterialComponent... parts) {
        DATA.put(machine + "/" + tier, ReversibleCraftingData.perItem(List.of(parts), 1,
                "GT6 Loader_MultiTileEntities " + sourceId + " known CR.REV inputs"));
    }
'''
    java = [header]
    size = (len(output) + 3) // 4
    for group in range(4):
        java.append(f'    private static void init{group}() {{\n')
        for row in output[group * size:(group + 1) * size]:
            parts = ', '.join(f'part({java_material(mat)}, {quantity}L)' for mat, quantity in row['components'].items())
            java.append(f'        add("{row["machine"]}", {row["tier"]}, {row["source_id"]}, {parts});\n')
        java.append('    }\n')
    size = (len(block_rows) + 3) // 4
    for group in range(4):
        java.append(f'    private static void blocks{group}() {{\n')
        for row in block_rows[group * size:(group + 1) * size]:
            parts = ', '.join(f'part({java_material(mat)}, {quantity}L)' for mat, quantity in row['components'].items())
            java.append(f'        BLOCK_DATA.put("{row["path"]}", ReversibleCraftingData.perItem(List.of({parts}), 1, "GT6 Loader_MultiTileEntities {row["source_id"]} known CR.REV inputs"));\n')
        java.append('    }\n')
    java.append('}\n')
    ns.out.parent.mkdir(parents=True, exist_ok=True)
    ns.out.write_text(''.join(java), encoding='utf-8')
    if ns.smeltery_out:
        quality_aliases = {m[1]: norm(m[2]) for m in re.finditer(r'^\s*(\w+)\s*\.[^\n]*?\.steal\(([^)]*)\)', any_source, re.M)}
        def hull_material(value):
            value = norm(value)
            if value.startswith('ANY.'):
                return hull_material(quality_aliases[value[4:]])
            return value.removeprefix('MT.')
        quantities = {r['source_id']: sum(r['components'].values()) for r in block_rows}
        smeltery = ['''/* Copyright GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original Loader_MultiTileEntities smeltery NBT, CR.REV and explicit ceramic item data.
 * Generated by tools/integration/generate_machine_material_data.py. */
package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.ImportedMaterialData;
import java.util.*;

/** Source hull statistics and construction amounts are separate: stone prefixes can have no positive REV data. */
public final class OriginalSmelteryDefinitions {
    private OriginalSmelteryDefinitions() {}
    private static final Map<Integer, CrucibleSpec> DATA = new LinkedHashMap<>();
    static {
''']
        smeltery_ids = {number+offset for number,_ in crucibles for offset in (0,50,750,850,700)}
        for source_id in bindings:
            if source_id not in smeltery_ids: continue
            row = by_id[source_id]; nbt = row['nbt_parameters']
            assert nbt['NBT_ACIDPROOF'] in ('T','F')
            smeltery.append(f'        add("{bindings[source_id]}", {source_id}, {java_material(hull_material(row["casing"]))}, {nbt["NBT_HARDNESS"]}, {nbt["NBT_RESISTANCE"]}, {str(nbt["NBT_ACIDPROOF"]=="T").lower()}, {quantities.get(source_id,0)}L);\n')
        smeltery.append('''    }
    private static void add(String path, int sourceId, GTMaterial hull, float hardness, float resistance, boolean acid, long constructionAmount) {
        DATA.put(sourceId, CrucibleSpec.of(path,hull,sourceId,hardness,resistance,acid,constructionAmount));
    }
    public static CrucibleSpec get(int sourceId) {
        var spec = DATA.get(sourceId);
        if (spec == null) throw new IllegalArgumentException("Unknown original smeltery ID " + sourceId);
        return spec;
    }
    public static List<CrucibleSpec> crucibles() {
        return DATA.values().stream().filter(s -> s.id().startsWith("smelting_crucible_")).toList();
    }
    public static Collection<CrucibleSpec> all() { return Collections.unmodifiableCollection(DATA.values()); }
}
''')
        ns.smeltery_out.parent.mkdir(parents=True,exist_ok=True)
        ns.smeltery_out.write_text(''.join(smeltery),encoding='utf-8')
    audit = {'source_root': str(ns.source), 'license': 'LGPL-3.0-or-later', 'authors': ['GregTech-6 Team', 'Gregorius Techneticies'],
             'source_files': [{'path': str(root / name), 'sha256': hashlib.sha256((root / name).read_bytes()).hexdigest()} for name in relative],
             'rows': output, 'block_rows': block_rows, 'source_without_known_data': empty_rows,
             'source_registration_parameters': [{key: row[key] for key in ('id', 'line', 'casing', 'quality_material', 'harvest_expression', 'block_group', 'nbt_parameters')} for row in registrations],
             'source_invalid_recipes': invalid_rows,
             'used_component_recipes': sorted(used), 'unknown_automatic_data': dict(sorted(unknown.items())),
             'unresolved_source_item_references': dict(sorted(unresolved_items.items())),
             'registered_source_items_without_material_data': dict(sorted(items_without_data.items())),
             'scope': 'Known original CR.REV components. No arbitrary tag member substitution; native crafted ingredients and runtime UI acceptance are separate checks.'}
    ns.audit.parent.mkdir(parents=True, exist_ok=True)
    ns.audit.write_text(json.dumps(audit, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'machines': len(output), 'blocks': len(block_rows), 'source_without_known_data': [r['path'] for r in empty_rows],
                      'component_recipes': len(used), 'unknown_inputs': dict(unknown),
                      'source_invalid_recipes': [r['path'] for r in invalid_rows],
                      'unresolved_source_item_references': dict(unresolved_items)}, ensure_ascii=False))


if __name__ == '__main__': main()
