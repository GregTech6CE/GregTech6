"""Restore GT6 ore-dictionary battery/circuit inputs in the eight static axial recipes.

The runtime recipe resolver already handles these groups. Static JSON recipes must
use the same tags. Only source-declared B/C keys are changed; housings, tools,
pattern, output and mirror rules are preserved.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT.parent / 'gregtech6-master/gregtech6-master/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java'
RECIPES = native_path(ROOT / 'src/main/resources/data/gregtech/recipes/axial')
TARGETS = dict(zip(range(17221, 17225), [
    'large_dynamo_main', 'large_dynamo_titanium',
    'large_dynamo_tungstensteel', 'large_dynamo_adamantium']))
TARGETS.update(zip(range(17231, 17235), [
    'large_gas_turbine_main', 'large_gas_turbine_trinitanium',
    'large_gas_turbine_graphene', 'large_gas_turbine_vibramantium']))
TIERS = ['ulv', 'lv', 'mv', 'hv', 'ev', 'iv']


def normalize(recipe, source_line):
    battery = re.search(r"'B', \"gt:re-battery(\d+)\"", source_line)
    circuit = re.search(r"'C', OD_CIRCUITS\[(\d+)\]", source_line)
    if not battery or not circuit:
        raise ValueError('Expected explicit GT6 battery and circuit ore dictionary keys')
    result = json.loads(json.dumps(recipe))
    for symbol in ('B', 'C'):
        if symbol not in result['key'] or symbol not in ''.join(result['pattern']):
            raise ValueError('Recipe does not contain source key ' + symbol)
    result['key']['B'] = {'tag': 'gregtech:rechargeable_batteries/' + TIERS[int(battery[1])]}
    result['key']['C'] = {'tag': 'gregtech:circuits_tier_' + circuit[1] + '_plus'}
    return result


def run():
    lines = SOURCE.read_text(encoding='utf-8').splitlines()
    pending = []
    for number, name in TARGETS.items():
        matches = [line for line in lines if re.search(r',\s*' + str(number) + r'\s*,', line)]
        if len(matches) != 1:
            raise ValueError(f'Expected exactly one registration for {number}')
        path = RECIPES / (name + '.json')
        before = path.read_text(encoding='utf-8')
        after = json.dumps(normalize(json.loads(before), matches[0]), ensure_ascii=False, indent=2) + '\n'
        pending.append((path, before, after))
    for path, before, after in pending:
        if before != after:
            path.write_text(after, encoding='utf-8')
    print('Verified 8 axial recipes against GT6 battery/circuit groups.')


if __name__ == '__main__':
    run()
