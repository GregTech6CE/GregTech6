"""Bind flattened bees and anvils by original identity, never by translated words.

The original checkout is read-only. A dry run writes an audit; --write also updates
the accepted bindings and shared language files through the strict pipeline.
"""
import argparse
import hashlib
from pathlib import Path
import re
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from localization import (ROOT, CONFIG_PATH, LANG_PATH, check_english, expected_chinese, json_text,
                          load_source, read_json)
from source_numbered_language import bumble_declarations

LITERAL = r'"((?:\\.|[^"\\])*)"'


def extract(repo, original):
    import json
    sources = []

    def read(path):
        sources.append(path)
        return path.read_text(encoding='utf-8-sig')

    def string(value):
        return json.loads('"' + value + '"')

    bee_file = original / 'src/main/java/gregtech/items/MultiItemBumbles.java'
    bees = read(bee_file)
    original_species, original_types = bumble_declarations(bees)
    catalog = read(repo / 'core/src/main/java/com/gregtech/gregtech/content/bumble/GTBumbleSpecies.java')
    species = re.findall(r'new Species\((\d+),\s*' + LITERAL + r',\s*' + LITERAL, catalog)
    type_maps = []
    for directory in ('src/main/java', 'neoforge/src/main/java'):
        types = read(repo / directory / 'com/gregtech/gregtech/content/bumble/BumbleBeeType.java')
        suffixes = dict(re.findall(r'^\s*(\w+)\("([^"]+)",', types, re.M))
        body = types.split('public int meta()', 1)[1].split('/** The scanned variant', 1)[0]
        type_maps.append({suffixes[name]: int(meta) for name, meta in
                          re.findall(r'case (\w+) -> (\d+);', body)})
    if type_maps[0] != type_maps[1] or set(type_maps[0].values()) != set(original_types):
        raise ValueError('Bumble state identities differ between original and platforms')
    registered = read(repo / 'core/src/main/java/com/gregtech/gregtech/registry/GTMultiItemsGen.java')
    native_bees = {ident for ident, name in re.findall(LITERAL + r',\s*' + LITERAL + r',\s*"bumblebee"', registered)}
    aliases, english, evidence = {}, {}, {}

    def bind(native, source, detail, text=None):
        if native in aliases:
            raise ValueError('Duplicate native identity: ' + native)
        aliases[native] = source
        evidence[native] = detail
        if text is not None:
            english[native] = text

    consumed = set()
    for ident, prefix, name in species:
        original_name, tooltip = original_species[int(ident)]
        if string(name) != original_name:
            raise ValueError('Bumble species ID/name differs from original: ' + ident)
        for suffix, meta in type_maps[0].items():
            native = prefix + '_' + suffix
            if native not in native_bees:
                raise ValueError('Unregistered bumble identity: ' + native)
            consumed.add(native)
            legacy = 'gt.multiitem.bumblebee.' + str(int(ident) + meta)
            key = 'item.gregtech.' + native
            detail = {'species_id': int(ident), 'state_meta': meta}
            bind(key, legacy, detail, original_name + original_types[meta])
            bind(key + '.tooltip', legacy + '.tooltip', detail, tooltip)
    if consumed != native_bees or {int(row[0]) for row in species} != set(original_species):
        raise ValueError('Bumble language import does not cover both complete registries')

    facade = read(repo / 'core/src/main/java/com/gregtech/gregtech/data/generated/GT6Materials.java')
    symbols = {}
    for symbol, canonical in re.findall(r'GTMaterial\s+(\w+)\s*=\s*com\.gregtech\.gregtech\.content\.material\.generated\.\w+\.(\w+)', facade):
        symbols.setdefault(symbol, set()).add(canonical)
    registrations = read(original / 'src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java')
    anvils = {}
    for line in registrations.splitlines():
        if 'MultiTileEntityAnvil.class' not in line:
            continue
        match = re.search(r'aMat\s*=\s*(?:MT\.(?:STONES\.)?|ANY\.)(\w+);.*?"Misc Tool Blocks"\s*,\s*(\d+)', line)
        if match is None:
            raise ValueError('Unparsed original anvil registration')
        symbol, ident = match.groups()
        candidates = symbols.get(symbol, {symbol})
        if len(candidates) != 1:
            raise ValueError('Ambiguous anvil material symbol: ' + symbol)
        canonical = next(iter(candidates)).lower()
        if canonical in anvils:
            raise ValueError('Duplicate original anvil material: ' + canonical)
        anvils[canonical] = int(ident)
    catalog = read(repo / 'core/src/main/java/com/gregtech/gregtech/content/tool/AnvilVariantCatalog.java')
    variants = re.findall(r'new Variant\("([^"]+)","([^"]+)",(\d+)L\)', catalog)
    if {material.lower() for _, material, _ in variants} != set(anvils):
        raise ValueError('Anvil material catalog differs from original registrations')
    for native, material, _ in variants:
        ident = anvils[material.lower()]
        bind('block.gregtech.' + native, 'gt.multitileentity.' + str(ident),
             {'original_id': ident, 'material': material})
    return aliases, english, evidence, sources


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--repo', type=Path, default=ROOT)
    ap.add_argument('--source', type=Path, required=True, help='Original 1.7.10 checkout')
    ap.add_argument('--audit', type=Path, required=True)
    ap.add_argument('--write', action='store_true')
    ns = ap.parse_args()
    source, meta = load_source(ns.repo)
    aliases, english, details, files = extract(ns.repo, ns.source)
    bindings = read_json(ns.repo / CONFIG_PATH / 'aliases.json')
    en = read_json(ns.repo / LANG_PATH / 'en_us.json')
    zh = read_json(ns.repo / LANG_PATH / 'zh_cn.json')
    changes = {}
    for key, original in aliases.items():
        if original not in source:
            raise ValueError('Original translation is absent: ' + original)
        if key in bindings and bindings[key] != original:
            raise ValueError('Existing binding contradicts original identity: ' + key)
        if key not in en and key not in english:
            raise ValueError('Unrecognized native language key: ' + key)
        if key not in bindings or zh.get(key) != source[original]:
            changes[key] = {'source_key': original, 'before': zh.get(key),
                            'after': source[original], **details[key]}
        bindings[key] = original
    en.update(english)
    translated = expected_chinese(en, source, bindings)
    report = {'source_language_sha256': meta['sha256'], 'changes': changes,
              'mapped_names': sum(not k.endswith('.tooltip') for k in aliases),
              'mapped_tooltips': sum(k.endswith('.tooltip') for k in aliases),
              'source_files': [{'path': str(p.resolve()), 'sha256': hashlib.sha256(p.read_bytes()).hexdigest()}
                               for p in files],
              'code_author': 'GregTech-6 Team / Gregorius Techneticies',
              'code_license': 'LGPL-3.0-or-later; original source headers retained in read-only checkout',
              'chinese_source': 'User-supplied pinned language file; copied verbatim including empty values; no publication.'}
    if ns.write:
        # Do not let this specialized importer bypass the unified English gate.
        # Source-name updates must be reviewed through the full identity importer.
        check_english(ns.repo, en, translated, bindings, source)
        (ns.repo / CONFIG_PATH / 'aliases.json').write_text(json_text(bindings), encoding='utf-8')
        (ns.repo / LANG_PATH / 'en_us.json').write_text(json_text(en), encoding='utf-8')
        (ns.repo / LANG_PATH / 'zh_cn.json').write_text(json_text(translated), encoding='utf-8')
    ns.audit.parent.mkdir(parents=True, exist_ok=True)
    ns.audit.write_text(json_text(report), encoding='utf-8')
    print(f"{'Imported' if ns.write else 'Proposed'} {len(changes)} bindings: "
          f"{report['mapped_names']} names, {report['mapped_tooltips']} tooltips")


if __name__ == '__main__':
    main()
