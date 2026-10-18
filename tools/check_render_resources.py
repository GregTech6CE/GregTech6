"""Check local model/blockstate references. Reports declarations, not proven visible defects.

Default checks the repaired controllers and panels, following their model references.
--all audits all local model/blockstate declarations, including unused legacy assets.
External namespaces are not checked. No placeholder assets are generated.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = native_path(ROOT / 'src/main/resources/assets/gt6ce')
CORE_ASSETS = ROOT / 'core/src/main/resources/assets/gt6ce'
CONTROLLERS = ('bedrock_mining_drill_controller', 'fusion_reactor_main', 'implosion_compressor_main', 'lightning_rod_electric_output')


def check(paths, assets=ASSETS):
    pending = list(paths)
    visited = set()
    issues = set()
    def resolve(path):
        if path.is_file() or assets != ASSETS:
            return path
        return CORE_ASSETS / path.relative_to(assets)
    while pending:
        path = pending.pop()
        if path in visited:
            continue
        visited.add(path)
        source = path.relative_to(assets).as_posix()
        try:
            data = json.loads(resolve(path).read_text(encoding='utf-8-sig'))
        except (ValueError, OSError) as error:
            issues.add((source, 'json', str(error)))
            continue

        def reference(value, kind):
            if not isinstance(value, str) or not value.startswith('gt6ce:'):
                return
            relative = value.split(':', 1)[1]
            target = assets / ('models' if kind == 'model' else 'textures') / (relative + ('.json' if kind == 'model' else '.png'))
            if not resolve(target).is_file():
                issues.add((source, kind, value))
            elif kind == 'model':
                pending.append(target)

        def walk(value):
            if isinstance(value, list):
                for child in value:
                    walk(child)
            elif isinstance(value, dict):
                for key, child in value.items():
                    if key in ('parent', 'model'):
                        reference(child, 'model')
                    if key == 'textures' and isinstance(child, dict):
                        for texture in child.values():
                            reference(texture, 'texture')
                    walk(child)
        walk(data)
    return sorted(issues), len(visited)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--all', action='store_true')
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    paths = list((ASSETS / 'blockstates').glob('panel_*.json'))
    paths += [ASSETS / f'blockstates/{name}.json' for name in CONTROLLERS]
    paths += [ASSETS / f'blockstates/{name}.json' for name in ('mortar_block', 'grindstone_block', 'sifting_table', 'crank')]
    paths += list((ASSETS / 'models/item').glob('chest_*.json'))
    paths += [ASSETS / f'models/block/machine/engine/engine_{name}.json' for name in ('electric', 'flux')]
    from rebuild_container_tool_models import TOOLS
    paths += [ASSETS / f'blockstates/{name}.json' for name in TOOLS]
    for pattern in ('fluid_*.json', 'battery_*.json', 'fuel_rod_*.json', 'electric_*.json', 'magnet_*.json'):
        paths += list((ASSETS / 'models/item').glob(pattern))
    paths += list((ASSETS / 'blockstates').glob('sensor_*.json'))
    for pattern in ('reactor_rod_*.json', 'fuel_rod_*.json', 'tap*.json', 'cap_nozzle*.json', 'fluid_funnel*.json', 'fluid_cell_*.json'):
        paths += list((ASSETS / 'blockstates').glob(pattern))
    paths += list((ASSETS / 'models/item').glob('radiation_suit_*.json'))
    from rebuild_source_extenders import SPECS as EXTENDERS
    from rebuild_source_filters import FILTERS
    from rebuild_panel_covers import MODELS as PANEL_COVERS
    from rebuild_signal_wires import IDS as SIGNAL_WIRES
    for name in SIGNAL_WIRES:
        paths += [ASSETS / f'blockstates/{name}.json', ASSETS / f'models/item/{name}.json']
    paths += [ASSETS / f'models/item/{name}.json' for name in PANEL_COVERS]
    from rebuild_machine_control_covers import SPECS as CONTROL_COVERS
    paths += [ASSETS / f'models/item/{row[0]}.json' for row in CONTROL_COVERS]
    for name, _, _ in FILTERS:
        paths += [ASSETS / f'blockstates/{name}.json', ASSETS / f'models/item/{name}.json']
    paths += [ASSETS / f'models/item/{name}.json' for name in ('blank_cover','item_filter','fluid_filter')]
    for name, _, _ in EXTENDERS:
        paths += [ASSETS / f'blockstates/{name}.json', ASSETS / f'models/item/{name}.json']
    for folder in ('energy', 'container'):
        paths += list((ASSETS / f'models/block/machine/{folder}').rglob('*.json'))
    if args.all:
        paths = list((ASSETS / 'models').rglob('*.json')) + list((ASSETS / 'blockstates').glob('*.json'))
    issues, count = check(paths)
    report = {'scope': 'all-declarations' if args.all else 'repaired-content',
              'filesChecked': count, 'issues': [{'source': s, 'kind': k, 'target': t} for s, k, t in issues]}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f'{count} files checked; {len(issues)} unresolved declarations')
    for source, kind, target in issues[:20]:
        print(f'{source}: {kind} {target}')
    return bool(issues)


if __name__ == '__main__':
    raise SystemExit(main())
