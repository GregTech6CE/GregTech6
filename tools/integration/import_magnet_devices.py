"""Bind original ten bipolar magnet names/order; audit existing source texture/model layers.

Read-only canonical/_w sources. This importer never creates a second recipe set:
OriginalMagnetCrafting supplies both native registries under the retained recipe IDs.
"""
import argparse
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
    loader = ns.source/'src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java'
    rows = re.findall(r'aRegistry\.add\(([^,\n]+),\s*"([^"]+)"\s*,\s*([^,]+)\s*,\s*(\d+)\s*,\s*([\w.]+)', loader.read_text(encoding='utf-8'))
    chinese = dict(line.lstrip()[2:].split('=', 1) for line in ns.zh_patch.read_text(encoding='utf-8').splitlines()
                   if line.lstrip().startswith('S:') and '=' in line)
    locales = {locale: json.loads((ns.repo/f'core/src/main/resources/assets/gregtech/lang/{locale}.json').read_text(encoding='utf-8'))
               for locale in ('en_us', 'zh_cn')}
    variants, assets, models = [], {}, {}
    tiers = ('lv', 'mv', 'hv', 'ev', 'iv')
    flux = ('Lead', 'Invar', 'Electrum', 'Enderium Base', 'Enderium')
    for rf in (False, True):
        folder = 'magnets/magnet_' + ('flux' if rf else 'electric')
        for layer in ('colored', 'overlay', 'overlay_active'):
            for face in ('front', 'back', 'side'):
                rel = f'assets/gregtech/textures/blocks/machines/{folder}/{layer}/{face}.png'
                canonical = ns.source/'src/main/resources'/rel
                donor = canonical if canonical.is_file() else ns.comparison/'src/main/resources'/rel
                target = ns.repo/f'core/src/main/resources/assets/gregtech/textures/block/machines/{folder}/{layer}/{face}.png'
                if not target.is_file() or not donor.is_file() or sha(target) != sha(donor):
                    raise ValueError('Existing magnet asset needs inspection: ' + str(target))
                assets[str(target.relative_to(ns.repo))] = {'source': str(donor), 'sha256': sha(donor), 'canonical_available': canonical.is_file()}
        for i, tier in enumerate(tiers):
            source_id = (11031 if rf else 10031) + i
            ordinal, row = next((n, r) for n, r in enumerate(rows) if r[2].strip() == str(source_id))
            name = ('Flux Magnet (' + flux[i] if rf else 'Electromagnet (' + tier.upper()) + ')'
            id = ('flux_magnet_' if rf else 'electromagnet_') + tier
            source_key, key = f'gt.multitileentity.{source_id}', 'block.gregtech.'+id
            if row[1] != 'Magnets' or not row[0].lstrip().startswith('"' + ('Flux Magnet (' if rf else 'Electromagnet (')):
                raise ValueError(row)
            locales['en_us'][key], locales['zh_cn'][key] = name, chinese[source_key]
            variants.append({'id': id, 'source_id': source_id, 'source_key': source_key, 'en_us': name,
                             'zh_cn': chinese[source_key], 'source_order': ordinal, 'creative_family': 'magnets'})
            for platform in ('src', 'neoforge/src'):
                root = ns.repo/platform/'main/resources/assets/gregtech'
                state_path = root/f'blockstates/{id}.json'
                states = json.loads(state_path.read_text())['variants']
                # These twelve selectors intentionally omit waterlogged and cover both native values.
                if len(states) != 12: raise ValueError('Expected six facing / two overlay selectors')
                for key, state in states.items():
                    properties = dict(pair.split('=') for pair in key.split(','))
                    if state['model'] != f'gregtech:block/machine/energy/{id}' + ('_active' if properties['active'] == 'true' else ''):
                        raise ValueError((key,state))
                for active in (False, True):
                    model_path = root/f'models/block/machine/energy/{id}{"_active" if active else ""}.json'
                    model = json.loads(model_path.read_text())
                    layers = model['children']
                    for layer, subdir in (('colored', 'colored'), ('overlay', 'overlay_active' if active else 'overlay')):
                        if not all('/'+folder+'/'+subdir+'/' in t for t in layers[layer]['textures'].values()):
                            raise ValueError('Incorrect source layer '+str(model_path))
                    if not all(e['faces'][f].get('tintindex') == 0 for e in layers['colored']['elements'] for f in e['faces']):
                        raise ValueError('Missing material tint '+str(model_path))
                    models[str(model_path.relative_to(ns.repo))] = sha(model_path)
                models[str(state_path.relative_to(ns.repo))] = sha(state_path)
                item_path = root/f'models/item/{id}.json'
                if json.loads(item_path.read_text())['parent'] != f'gregtech:block/machine/energy/{id}': raise ValueError(item_path)
                models[str(item_path.relative_to(ns.repo))] = sha(item_path)
    for locale, data in locales.items():
        (ns.repo/f'core/src/main/resources/assets/gregtech/lang/{locale}.json').write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    catalog = ns.repo/'core/src/main/java/com/gregtech/gregtech/content/creative/SourceCreativeCatalog.java'
    text = catalog.read_text(encoding='utf-8')
    if 'initMagnets();' not in text: text = text.replace('initThermal();', 'initThermal();\ninitMagnets();')
    text = re.sub(r'private static void initMagnets\(\)\{[\s\S]*?\}\n', '', text)
    method = 'private static void initMagnets(){\n' + ''.join(f'ITEMS.put("{v["id"]}",new Entry("magnets",{v["source_order"]}));\n' for v in variants) + '}\n'
    text = text.replace('public static Entry entry(', method+'public static Entry entry(')
    catalog.write_text(text, encoding='utf-8')
    audit = {'variants': variants, 'assets': assets, 'static_models': models, 'states_per_platform': 240,
             'authors': ['GregTech-6 Team', 'Gregorius Techneticies'], 'code_license': 'LGPL-3.0-or-later',
             'asset_license': 'CC0-1.0 per preserved LICENSE.assets', 'scope': 'Existing layers and tint, exact names/order. No runtime baking claim.',
             'source_files': [{'path': str(p), 'sha256': sha(p)} for p in (loader, ns.zh_patch, ns.source/'LICENSE', ns.comparison/'LICENSE.assets')]}
    ns.audit.parent.mkdir(parents=True, exist_ok=True)
    ns.audit.write_text(json.dumps(audit, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({'variants': len(variants), 'assets': len(assets), 'static_models': len(models), 'states_per_platform': 240}))


if __name__ == '__main__': main()
