#!/usr/bin/env python3
"""Import the user-supplied GT6 Chinese patch without translating or rewriting its values.

Legacy keys are retained for material-form names and source tooltips. Native aliases
are bound by original registration numbers, source declarations, or unambiguous
English text. Ambiguous/missing mappings are reported rather than guessed.
"""
import argparse
import collections
import hashlib
import json
from pathlib import Path
import re
from localization import read_patch

ROOT = Path(__file__).resolve().parents[1]
LANG = ROOT / 'core/src/main/resources/assets/gregtech/lang'


def snake(text):
    return re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', text).lower()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--patch', type=Path, required=True)
    parser.add_argument('--source', type=Path, required=True, help='Original src/main/java directory')
    parser.add_argument('--proposal', type=Path, required=True,
                        help='Write discovery evidence here; never overwrite runtime language or accepted bindings')
    args = parser.parse_args()
    patch = read_patch(args.patch)
    english = json.loads((LANG / 'en_us.json').read_text(encoding='utf-8-sig'))
    # Repair a pre-existing mojibake English fallback; it is not a Chinese translation.
    english['jade.gregtech.crucible.warning'] = 'Approaching meltdown!'
    # Unmapped port-only text remains English until a source binding exists. Do not
    # carry forward the port's invented Chinese names under an original-patch policy.
    output = dict(english)
    output.update(patch)
    aliases = {}
    reverse = collections.defaultdict(set)
    files = {}
    material_symbols = {}
    symbols = (ROOT / 'core/src/main/java/com/gregtech/gregtech/data/generated/GT6Materials.java').read_text(encoding='utf-8')
    for symbol, name in re.findall(r'GTMaterial\s+(\w+)\s*=\s*com\.gregtech\.gregtech\.content\.material\.generated\.\w+\.(\w+)', symbols):
        material_symbols[symbol] = name

    def material_name(symbol):
        name = material_symbols.get(symbol, symbol)
        return english.get('material.gregtech.' + name.lower())

    def registration_name(expression, material):
        parts = expression.strip().split('+')
        values = []
        for part in parts:
            part = part.strip()
            if re.fullmatch(r'"[^"\\]*"', part):
                values.append(part[1:-1])
            elif part == 'aMat.getLocal()' and material:
                values.append(material)
            else:
                return None
        return ''.join(values)

    def alias(native, legacy):
        if legacy in patch:
            output[native] = patch[legacy]
            aliases[native] = legacy

    for path in sorted(args.source.rglob('*.java')):
        raw = path.read_text(encoding='utf-8-sig')
        files[str(path)] = hashlib.sha256(path.read_bytes()).hexdigest()
        # Direct language registrations and numbered MultiItems/MultiTileEntities.
        for key, value in re.findall(r'\b(?:LH\.)?add\("([^"]+)"\s*,\s*"([^"\\]*)"', raw):
            if key in patch:
                reverse[value].add(key)
        category = {'MultiItemRandomTools':'randomtools', 'MultiItemFood':'food',
                    'MultiItemBottles':'bottles', 'MultiItemCans':'cans',
                    'MultiItemTechnological':'technological', 'MultiItemBooks':'books'}.get(path.stem)
        if category:
            for ident, name, tooltip in re.findall(r'addItem\(\s*(\d+)\s*,\s*"([^"\\]*)"(?:\s*,\s*"([^"\\]*)")?', raw):
                key = f'gt.multiitem.{category}.{ident}'
                if key in patch:
                    reverse[name].add(key)
                if key + '.tooltip' in patch and tooltip:
                    reverse[tooltip].add(key + '.tooltip')
        if path.name == 'Loader_MultiTileEntities.java':
            for name, ident in re.findall(r'aRegistry\.add\(\s*"([^"]+)"\s*,\s*"[^"]+"\s*,\s*(\d+)\s*,', raw):
                key = 'gt.multitileentity.' + ident
                if key in patch:
                    reverse[name].add(key)
            # Literal registrations with a material assigned on the same source line.
            for line in raw.splitlines():
                assigned = re.search(r'aMat\s*=\s*MT\.(?:\w+\.)?(\w+)\s*;', line)
                row = re.search(r'aRegistry\.add\(([^,]+),\s*"[^"]+"\s*,\s*(\d+)\s*,', line)
                if assigned and row:
                    name = registration_name(row[1], material_name(assigned[1]))
                    key = 'gt.multitileentity.' + row[2]
                    if name and key in patch:
                        reverse[name].add(key)
            # metalset is called with a stable material and numeric base ID.
            body = raw.split('private static void metalset(', 1)[1].split('private static void storages(', 1)[0]
            rows = re.findall(r'aRegistry\.add\(([^,]+),\s*"[^"]+"\s*,\s*(?:(\d+)\+)?aID\s*,', body)
            for symbol, base in re.findall(r'metalset\(aRegistry,\s*aMetal,\s*aUtilMetal,\s*aMachine,\s*aWooden,\s*MT\.(\w+)\s*,\s*(\d+)\s*,', raw):
                for expression, offset in rows:
                    name = registration_name(expression, material_name(symbol))
                    key = 'gt.multitileentity.' + str(int(base) + int(offset or 0))
                    if name and key in patch:
                        reverse[name].add(key)

    ambiguous = {}
    for key, value in english.items():
        if key in patch:
            alias(key, key)
        elif value in reverse:
            candidates = reverse[value]
            if len({patch[c] for c in candidates}) == 1:
                alias(key, sorted(candidates)[0])
            else:
                ambiguous[key] = sorted(candidates)
        elif key.startswith('block.gregtech.advanced_crafting_table'):
            material = value.removesuffix(' Advanced Crafting Table')
            candidates = reverse.get('Advanced Crafting Table (' + material + ')', set())
            if candidates and len({patch[c] for c in candidates}) == 1:
                alias(key, sorted(candidates)[0])
    for key in patch:
        if key.startswith('gt.material.'):
            alias('material.gregtech.' + key.removeprefix('gt.material.').lower(), key)
        elif key.startswith('fluid.'):
            alias('fluid.gregtech.' + key.removeprefix('fluid.'), key)
            alias('fluid_type.gregtech.' + key.removeprefix('fluid.'), key)
        elif key.startswith('itemGroup.') and not key.startswith('itemGroup.gt.'):
            alias('itemGroup.gregtech.' + snake(key.removeprefix('itemGroup.')), key)

    creative = json.loads((ROOT / 'docs/integration/verification/origin-creative-source-20261004.json').read_text(encoding='utf-8'))
    for family, data in creative['source_groups'].items():
        alias('itemGroup.gregtech.' + family, 'itemGroup.gt.multitileentity.' + str(data['legacy_tab']))
    for family, category in {'books':'books', 'bottles':'bottles', 'bumblebees':'bumblebee',
            'nature_foods':'food', 'equipment':'randomtools', 'multi_items':'randomtools',
            'technology':'technological'}.items():
        alias('itemGroup.gregtech.' + family, 'itemGroup.gt.multiitem.' + category)
    for family, legacy in {'tools':'gt.multiitem.randomtools', 'stones':'rockGt',
            'construction':'gt.multitileentity.32765', 'woods':'stick',
            'fluids':'gt.multitileentity.32719', 'energy_nodes':'gt.multitileentity.10111',
            'iconsets':'blockSolid'}.items():
        alias('itemGroup.gregtech.' + family, 'itemGroup.' + legacy)
    for native, legacy in {
            'pipes':'gt.multitileentity.26142', 'item_pipes':'gt.multitileentity.25202',
            'wires':'gt.multitileentity.28366', 'coin':'gt.multitileentity.32700',
            'item_casing':'casingSmall', 'glasstube':'chemtube',
            'crate_gt_dust':'crateGt64Dust', 'crate_gt_gem':'crateGt64Gem',
            'crate_gt_ingot':'crateGt64Ingot', 'crate_gt_plate':'crateGt64Plate',
            'crate_gt_plate_gem':'crateGt64PlateGem', 'crate_gt_raw':'crateGt64Raw'}.items():
        alias('itemGroup.gregtech.' + native, 'itemGroup.' + legacy)
    # Dynamic names contain a source material and meta value; do not use an English
    # StoneVariant display argument inside a Chinese format string.
    stone = (ROOT / 'core/src/main/java/com/gregtech/gregtech/block/stone/StoneType.java').read_text(encoding='utf-8')
    variants = (ROOT / 'core/src/main/java/com/gregtech/gregtech/block/stone/StoneVariant.java').read_text(encoding='utf-8')
    for ident, folder in re.findall(r'\w+\("([^"]+)",\s*"([^"]+)"', stone):
        for meta, suffix, label in re.findall(r'\w+\((\d+),\s*"([^"]+)",\s*"([^"]+)"', variants):
            material = english.get('material.gregtech.' + ident.replace('_', ''), ident.replace('_', ' ').title())
            for tail, ending in [('', ''), ('_slab', ' Slab')]:
                key = f'block.gregtech.stone_{ident}_{suffix}{tail}'
                english.setdefault(key, material + ' ' + label + ending)
                output.setdefault(key, english[key])
            alias(f'block.gregtech.stone_{ident}_{suffix}', f'{folder}.{meta}')
            alias(f'block.gregtech.stone_{ident}_{suffix}_slab', f'{folder}.slab.0.{meta}')
    books = (ROOT / 'core/src/main/java/com/gregtech/gregtech/content/book/ColoredBookRules.java').read_text(encoding='utf-8')
    for ident, original in re.findall(r'new Variant\("([^"]+)",(\d+),', books):
        alias('item.gregtech.' + ident, 'gt.multiitem.books.' + original)
        alias('item.gregtech.' + ident + '.tooltip', 'gt.multiitem.books.' + original + '.tooltip')

    # Discovery is reviewable data. Only localization.py may regenerate Chinese.
    accepted = json.loads((ROOT / 'tools/localization/aliases.json').read_text(encoding='utf-8'))
    report = {'patch':str(args.patch.resolve()), 'sha256':hashlib.sha256(args.patch.read_bytes()).hexdigest(),
              'author':'User-supplied original GregTech Chinese patch; no author attribution present in the configuration header',
              'license':'Not declared in the supplied language file; preserved local source, no publication in this batch',
              'policy':'All imported Chinese values copied verbatim; unmapped port-only values use English fallback',
              'source_keys':len(patch),
              'proposed_aliases':{k:v for k,v in aliases.items() if k not in accepted and k not in patch},
              'ambiguous':ambiguous,
              'unmapped_native_keys':[k for k in english if k not in aliases],
              'source_declaration_sha256':files}
    args.proposal.parent.mkdir(parents=True, exist_ok=True)
    args.proposal.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'source_keys':len(patch), 'aliases':len(aliases), 'ambiguous':len(ambiguous),
                      'unmapped':len(report['unmapped_native_keys'])}))


if __name__ == '__main__':
    main()
