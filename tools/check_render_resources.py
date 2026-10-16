"""Check local model/blockstate references. Reports declarations, not proven visible defects.

Default checks the repaired controllers and panels, following their model references.
--all audits all local model/blockstate declarations, including unused legacy assets.
External namespaces are not checked. No placeholder assets are generated.
"""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / 'src/main/resources/assets/gregtech'
CORE_ASSETS = ROOT / 'core/src/main/resources/assets/gregtech'
ASSET_ROOTS = (SHARED_ASSETS, CORE_ASSETS, ASSETS)
CONTROLLERS = ('bedrock_drill_main', 'fusion_reactor_main', 'implosion_compressor_main', 'lightning_rod_main')


def resource_root(path):
    for root in ASSET_ROOTS:
        try:
            path.relative_to(root)
        except ValueError:
            continue
        return root
    return ASSETS


def shipped(relative):
    for root in ASSET_ROOTS:
        candidate = root / relative
        if candidate.is_file():
            return candidate
    return None


def collect(relative, pattern):
    found = []
    for root in ASSET_ROOTS:
        folder = root / relative
        if folder.is_dir():
            found.extend(folder.glob(pattern))
    return found


def locate(kind, relative, assets):
    folder = 'models' if kind == 'model' else 'textures'
    suffix = '.json' if kind == 'model' else '.png'
    name = Path(folder) / (relative + suffix)
    roots = [assets, *ASSET_ROOTS]
    for root in roots:
        candidate = root / name
        if candidate.is_file():
            return candidate
    return None


def check(paths, assets=None):
    pending = list(paths)
    visited = set()
    issues = set()
    while pending:
        path = pending.pop()
        if path in visited:
            continue
        visited.add(path)
        root = assets or resource_root(path)
        try:
            source = path.relative_to(root).as_posix()
        except ValueError:
            source = path.as_posix()
        try:
            data = json.loads(path.read_text(encoding='utf-8-sig'))
        except (ValueError, OSError) as error:
            issues.add((source, 'json', str(error)))
            continue

        def reference(value, kind):
            if not isinstance(value, str) or not value.startswith('gregtech:'):
                return
            relative = value.split(':', 1)[1]
            target = locate(kind, relative, root)
            if target is None:
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
    paths = collect('blockstates', 'panel_*.json')
    paths += [path for name in CONTROLLERS if (path := shipped(f'blockstates/{name}.json'))]
    paths += [path for name in ('mortar_block', 'grindstone_block', 'sifting_table', 'crank') if (path := shipped(f'blockstates/{name}.json'))]
    paths += collect('models/item', 'chest_*.json')
    paths += [path for name in ('electric', 'flux') if (path := shipped(f'models/block/machine/engine/engine_{name}.json'))]
    from rebuild_container_tool_models import TOOLS
    paths += [path for name in TOOLS if (path := shipped(f'blockstates/{name}.json'))]
    for pattern in ('fluid_*.json', 'battery_*.json', 'fuel_rod_*.json', 'electric_*.json', 'magnet_*.json'):
        paths += collect('models/item', pattern)
    paths += collect('blockstates', 'sensor_*.json')
    for pattern in ('reactor_rod_*.json', 'fuel_rod_*.json', 'tap*.json', 'cap_nozzle*.json', 'fluid_funnel*.json', 'fluid_cell_*.json'):
        paths += collect('blockstates', pattern)
    paths += collect('models/item', 'radiation_suit_*.json')
    from rebuild_source_extenders import SPECS as EXTENDERS
    from rebuild_source_filters import FILTERS
    from rebuild_panel_covers import MODELS as PANEL_COVERS
    from rebuild_signal_wires import IDS as SIGNAL_WIRES
    for name in SIGNAL_WIRES:
        paths += [path for relative in (f'blockstates/{name}.json', f'models/item/{name}.json') if (path := shipped(relative))]
    paths += [path for name in PANEL_COVERS if (path := shipped(f'models/item/{name}.json'))]
    from rebuild_machine_control_covers import SPECS as CONTROL_COVERS
    paths += [path for row in CONTROL_COVERS if (path := shipped(f'models/item/{row[0]}.json'))]
    for name, _, _ in FILTERS:
        paths += [path for relative in (f'blockstates/{name}.json', f'models/item/{name}.json') if (path := shipped(relative))]
    paths += [path for name in ('blank_cover','item_filter','fluid_filter') if (path := shipped(f'models/item/{name}.json'))]
    for name, _, _ in EXTENDERS:
        paths += [path for relative in (f'blockstates/{name}.json', f'models/item/{name}.json') if (path := shipped(relative))]
    for folder in ('energy', 'container'):
        paths += collect(f'models/block/machine/{folder}', '**/*.json')
    if args.all:
        paths = []
        for root in ASSET_ROOTS:
            paths += list((root / 'models').rglob('*.json')) + list((root / 'blockstates').glob('*.json'))
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
