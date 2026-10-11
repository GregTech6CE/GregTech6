"""Import the twenty original thermal devices' names, creative order and textures.

Source trees are read-only. Keep native composite loaders, tint layers and IDs.
The canonical snapshot lacks some image files; the preserved _w asset snapshot
supplies those exact named files under its original CC0 asset license.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import re


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--source', type=Path, required=True)
    ap.add_argument('--comparison', type=Path, required=True)
    ap.add_argument('--zh-patch', type=Path, required=True)
    ap.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[2])
    ap.add_argument('--audit', type=Path, required=True)
    ns = ap.parse_args()
    loader = ns.source / 'src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java'
    raw = loader.read_text(encoding='utf-8')
    all_rows = re.findall(r'aRegistry\.add\(([^,\n]+),\s*"([^"]+)"\s*,\s*([^,]+)\s*,\s*(\d+)\s*,\s*([\w.]+)', raw)
    source_rows = [(expr, group, legacy.strip()) for expr, group, legacy, _, _ in all_rows if legacy.strip().isdigit()]
    order = {int(legacy): ordinal for ordinal, (_, _, legacy, _, _) in enumerate(all_rows) if legacy.strip().isdigit()}
    chinese = {}
    for line in ns.zh_patch.read_text(encoding='utf-8').splitlines():
        row = line.lstrip()
        if row.startswith('S:') and '=' in row:
            key, value = row[2:].split('=', 1)
            chinese[key] = value
    locales = {name: json.loads((ns.repo / f'core/src/main/resources/assets/gregtech/lang/{name}.json').read_text(encoding='utf-8'))
               for name in ('en_us', 'zh_cn')}
    variants, assets = [], {}
    tiers = ('lv', 'mv', 'hv', 'ev', 'iv')
    flux_material = ('Lead', 'Invar', 'Electrum', 'Enderium Base', 'Enderium')
    for cooler in (False, True):
        for rf in (False, True):
            folder = ('cooler/cryo_' if cooler else 'heaters/heat_') + ('flux' if rf else 'electric')
            for layer in ('colored', 'overlay', 'overlay_active'):
                for face in ('front', 'back', 'side'):
                    rel = f'assets/gregtech/textures/blocks/machines/{folder}/{layer}/{face}.png'
                    canonical = ns.source / 'src/main/resources' / rel
                    donor = canonical if canonical.is_file() else ns.comparison / 'src/main/resources' / rel
                    target = ns.repo / f'core/src/main/resources/assets/gregtech/textures/block/machines/{folder}/{layer}/{face}.png'
                    if not donor.is_file():
                        raise ValueError('Missing original asset: ' + str(donor))
                    target.parent.mkdir(parents=True, exist_ok=True)
                    if target.exists() and sha(target) != sha(donor):
                        raise ValueError('Existing asset differs; inspect before replacing: ' + str(target))
                    target.write_bytes(donor.read_bytes())
                    assets[str(target.relative_to(ns.repo))] = {'source': str(donor), 'sha256': sha(donor), 'canonical_available': canonical.is_file()}
                    meta = Path(str(donor) + '.mcmeta')
                    if meta.is_file():
                        out_meta = Path(str(target) + '.mcmeta')
                        out_meta.write_bytes(meta.read_bytes())
                        assets[str(out_meta.relative_to(ns.repo))] = {'source': str(meta), 'sha256': sha(meta)}
            for index, tier in enumerate(tiers):
                source_id = (10160 if cooler else 10000) + (1000 if rf else 0) + index + 1
                id = ('flux_' if rf else 'electric_') + ('cooler_' if cooler else 'heater_') + tier
                base_name = ('Thermofluxic Cooler' if rf else 'Thermoelectric Cooler') if cooler else ('Flux Heater' if rf else 'Electric Heater')
                name = f'{base_name} ({flux_material[index] if rf else tier.upper()})'
                source_key = f'gt.multitileentity.{source_id}'
                # Confirm the source expression belongs to this family; quantities are audited separately.
                expression, group, _ = next(row for row in source_rows if int(row[2]) == source_id)
                if not expression.strip().startswith('"' + base_name + ' ("'):
                    raise ValueError('Unexpected source name expression: ' + expression)
                key = 'block.gregtech.' + id
                locales['en_us'][key] = name
                locales['zh_cn'][key] = chinese[source_key]
                variants.append({'id': id, 'source_id': source_id, 'source_key': source_key,
                                 'en_us': name, 'zh_cn': chinese[source_key], 'folder': folder,
                                 'creative_family': group.lower(), 'source_order': order[source_id]})
                for platform in ('src', 'neoforge/src'):
                    root = ns.repo / platform / 'main/resources/assets/gregtech'
                    model_path = root / f'models/block/machine/energy/{id}.json'
                    template_id = 'electric_' + ('cooler_' if cooler else 'heater_') + tier
                    template = json.loads((root / f'models/block/machine/energy/{template_id}.json').read_text(encoding='utf-8'))
                    if rf:
                        template = json.loads(json.dumps(template).replace('/cryo_electric/', '/cryo_flux/').replace('/heat_electric/', '/heat_flux/'))
                        model_path.write_text(json.dumps(template, indent=2) + '\n', encoding='utf-8')
                        (root / f'models/item/{id}.json').write_text(json.dumps({'parent': f'gregtech:block/machine/energy/{id}'}, indent=2)+'\n', encoding='utf-8')
                    active = copy.deepcopy(template)
                    textures = active['children']['layer1']['textures']
                    for face, texture in textures.items():
                        textures[face] = texture.replace('/overlay/', '/overlay_active/')
                    (root / f'models/block/machine/energy/{id}_active.json').write_text(json.dumps(active, indent=2)+'\n', encoding='utf-8')
                    rotations = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
                    states = {f'activity={activity},facing={face},waterlogged={str(water).lower()}':
                              {'model': f'gregtech:block/machine/energy/{id}' + ('_active' if activity else ''), **rotation}
                              for activity in range(3) for face, rotation in rotations.items() for water in (False, True)}
                    (root / f'blockstates/{id}.json').write_text(json.dumps({'variants': states}, indent=2)+'\n', encoding='utf-8')
    for locale, data in locales.items():
        (ns.repo / f'core/src/main/resources/assets/gregtech/lang/{locale}.json').write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    catalog = ns.repo / 'core/src/main/java/com/gregtech/gregtech/content/creative/SourceCreativeCatalog.java'
    text = catalog.read_text(encoding='utf-8')
    # An explicit original ordinal prevents equal names or fallback classification from dropping flux variants.
    if 'initThermal();' not in text:
        text = text.replace('init6();\n}', 'init6();\ninitThermal();\n}')
    method = 'private static void initThermal(){\n' + ''.join(f'ITEMS.put("{v["id"]}",new Entry("{v["creative_family"]}",{v["source_order"]}));\n' for v in variants) + '}\n'
    text = re.sub(r'private static void initThermal\(\)\{[\s\S]*?\}\n', '', text)
    text = text.replace('public static Entry entry(', method + 'public static Entry entry(')
    catalog.write_text(text, encoding='utf-8')
    audit = {'variants': variants, 'assets': assets,
             'source_files': [{'path': str(p), 'sha256': sha(p)} for p in (loader, ns.zh_patch, ns.source/'LICENSE', ns.comparison/'LICENSE.assets')],
             'authors': ['GregTech-6 Team', 'Gregorius Techneticies'],
             'code_license': 'LGPL-3.0-or-later', 'asset_license': 'CC0-1.0 unless otherwise stated, per preserved LICENSE.assets',
             'scope': 'Exact source names/order and selected assets; model state enumeration is static, not runtime baking.',
             'native_states_per_platform': len(variants)*36}
    ns.audit.parent.mkdir(parents=True, exist_ok=True)
    ns.audit.write_text(json.dumps(audit, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({'variants': len(variants), 'textures_and_metadata': len(assets), 'native_states_per_platform': len(variants)*36}))


if __name__ == '__main__':
    main()
