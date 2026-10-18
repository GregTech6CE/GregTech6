"""GT6 optical machine crafting whose required forms/components are currently registered.
Exact representative circuits/cables are used; the full original ore dictionary alternatives remain separate.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
TIERS = ['lv', 'mv', 'hv', 'ev', 'iv']

def main():
    folder = native_path(ROOT / 'src/main/resources/data/gt6ce/recipes/optical')
    folder.mkdir(parents=True, exist_ok=True)
    def recipe(name, pattern, ingredients):
        data = {'type': 'minecraft:crafting_shaped', 'pattern': pattern,
                'key': {key: {'item': 'gt6ce:' + value} for key, value in ingredients.items()},
                'result': {'item': 'gt6ce:' + name}}
        (folder / (name + '.json')).write_text(json.dumps(data, indent=2) + '\n', encoding='utf8')
    metals = ['steelgalvanized', 'aluminium', 'stainlesssteel', 'chromium', 'titanium']
    flux = ['lead', 'invar', 'electrum', 'enderiumbase', 'enderium']
    cables = ['cable_01_tin', 'cable_01_copper', 'cable_01_gold', 'cable_04_aluminium', 'cable_04_platinum']
    circuits = ['basic', 'good', 'advanced', 'elite', 'master']
    patterns = [[' L ', ' W ', 'CMC'], ['L L', 'W W', 'CMC'], ['LLL', 'WWW', 'CMC'],
                ['L L', 'LWL', 'CMC'], ['LLL', 'LWL', 'CMC']]
    quantum_ids = ['ev', 'iv', 'luv', 'zpm', 'uv']  # Keep historical registry IDs; original tiers are T1..T5.
    for i, tier in enumerate(TIERS):
        recipe('co2_laser_' + tier, patterns[i], {'L': 'laser_emitter_carbondioxide', 'W': cables[i],
               'C': 'circuit_' + circuits[i], 'M': 'casing_machine_' + metals[i]})
        recipe('flux_laser_' + tier, ['PPP', 'PMP', 'PPP'], {'M': 'co2_laser_' + tier, 'P': 'plate_' + flux[i]})
        recipe('quantum_energizer_' + quantum_ids[i], ['CFC', 'SME', 'CFC'], {
            'C': 'crystal_processor_sapphire', 'M': 'casing_machine_osmiridium',
            'F': 'compact_force_field_emitter_' + tier, 'S': 'compact_sensor_' + tier,
            'E': 'compact_signal_emitter_' + tier})
    tag_path = native_path(ROOT / 'src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json')
    tag = json.loads(tag_path.read_text(encoding='utf8'))
    blocks = ['gt6ce:' + family + '_' + tier for family in ['co2_laser', 'flux_laser', 'laser_absorber'] for tier in TIERS]
    blocks += ['gt6ce:quantum_energizer_' + tier for tier in quantum_ids] + ['gt6ce:laser_fiber_wire']
    tag['values'] += [block for block in blocks if block not in tag['values']]
    tag_path.write_text(json.dumps(tag, indent=2) + '\n', encoding='utf8')
    print('Generated 15 optical crafting recipes and optical harvest tags')

if __name__ == '__main__':
    main()
