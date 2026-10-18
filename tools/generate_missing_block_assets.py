"""Writes the blockstate / block model / item model of blocks that have none.

A client logs `Exception loading blockstate definition: 'gregtech:blockstates/<id>.json' missing model
for variant: 'gregtech:<id>#'` for every registered block whose blockstate file is absent, and draws
the block with the missing-model placeholder. The 2026-09-18 crash report contained 2,068 of those -
whole families of *material* blocks (dust/raw/ingot/plate/gem/crate/casing of materials the asset
generator's material list never covered, mostly ore-dictionary materials from other mods) plus a few
structural parts.

Every family writes the same three files, so this tool clones an existing sibling of the same family
and rewrites the id inside it:

    blockstates/<id>.json                the sibling's variant list, re-pointed at the block id
    models/.../<id>.json                 the sibling's model, at the path the cloned blockstate names
    models/item/<id>.json                {"parent": "gregtech:block/.../<id>"}

The model target is taken from the *template's own* model reference, not assumed to be
`block/blocks/<id>`: material families point at `block/blocks/<id>`, structural parts at
`block/machine/multiblock/<id>`, energy devices at `block/machine/energy/<id>`. A block is skipped
entirely (and reported) when the template's model does not exist, so this tool can never leave a
blockstate pointing at a missing model - the client logs "Unable to load model" for those.

The material's colour comes from the block colour handler, not from the file, which is why the cloned
model can be shared verbatim.

Usage:
  python tools/generate_missing_block_assets.py --from-log <latest.log>     # write what is missing
  python tools/generate_missing_block_assets.py --from-log <latest.log> --check
  python tools/generate_missing_block_assets.py --list ids.json             # ids already extracted

`--check` reports the ids that still have no blockstate and exits non-zero.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import argparse
import io
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, str(native_path('src')), 'main', 'resources', 'assets', 'gregtech')
BLOCKSTATES = os.path.join(ASSETS, 'blockstates')
BLOCK_MODELS = os.path.join(ASSETS, 'models', 'block', 'blocks')
ITEM_MODELS = os.path.join(ASSETS, 'models', 'item')
PREFIXES_JAVA = os.path.join(ROOT, str(native_path('src')), 'main', 'java', 'com', 'gregtech', 'gregtech', 'data',
                             'MaterialPrefixes.java')

BS_LINE = re.compile(r"blockstates/([a-z0-9_]+)\.json")


def snake(name):
    return re.sub(r'(?<!^)(?=[A-Z])', '_', name).lower()


def block_families():
    """The asset-name prefix of every block prefix the material prefix registry declares."""
    text = io.open(PREFIXES_JAVA, encoding='utf-8', errors='replace').read()
    names = re.findall(r'registerBlock\(block\("([A-Za-z0-9]+)"\)', text)
    names += re.findall(r'register\("([A-Za-z0-9]+)"\)', text)
    families = {snake(name) for name in names}
    # A few families are registered through their block id directly.
    families |= {'block_raw', 'block_dust', 'block_ingot', 'block_plate', 'block_gem', 'block_solid',
                 'casing_machine', 'crate_gt64_raw', 'crate_gt64_dust', 'crate_gt64_ingot',
                 'crate_gt64_plate', 'crate_gt64_gem', 'crate_gt64_plategem', 'block_plategem'}
    return sorted(families, key=len, reverse=True)


def ids_from_log(path):
    found = set()
    for line in io.open(path, encoding='utf-8', errors='replace'):
        if 'Exception loading blockstate definition' not in line:
            continue
        match = BS_LINE.search(line)
        if match:
            found.add(match.group(1))
    return sorted(found)


def existing_ids():
    return {name[:-5] for name in os.listdir(BLOCKSTATES) if name.endswith('.json')}


def find_template(block_id, families, known):
    """An existing block id of the same family, or one sharing the longest prefix / suffix."""
    for family in families:
        if block_id.startswith(family + '_'):
            for candidate in sorted(known):
                if candidate.startswith(family + '_'):
                    return candidate
    # Suffix families (`*_wall`, `*_main_housing`, ...): the last two segments.
    parts = block_id.split('_')
    for take in (2, 3, 1):
        if len(parts) <= take:
            continue
        suffix = '_' + '_'.join(parts[-take:])
        for candidate in sorted(known):
            if candidate.endswith(suffix) and candidate != block_id:
                return candidate
    return None


def referenced_models(blockstate_path):
    """The model references a blockstate variant list points at (`gregtech:block/...`)."""
    data = json.loads(io.open(blockstate_path, encoding='utf-8-sig').read())
    refs = []
    for variant in data.get('variants', {}).values():
        model = variant.get('model')
        if model and model not in refs:
            refs.append(model)
    return refs


def model_file(reference):
    """Repository path of a `gregtech:block/...` model reference."""
    return os.path.join(ASSETS, 'models', reference.split(':', 1)[1] + '.json')


def clone(source_path, target_path, source_id, target_id):
    text = io.open(source_path, encoding='utf-8-sig').read()
    text = text.replace(source_id, target_id)
    os.makedirs(os.path.dirname(target_path), exist_ok=True)
    with io.open(target_path, 'w', encoding='utf-8', newline='\n') as handle:
        handle.write(text)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--from-log', help='a client log to read the reported ids from')
    parser.add_argument('--list', help='a JSON file holding the ids')
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()

    if args.from_log:
        wanted = ids_from_log(args.from_log)
    elif args.list:
        wanted = json.load(io.open(args.list, encoding='utf-8'))
    else:
        print('give --from-log <latest.log> or --list <ids.json>')
        return 2

    families = block_families()
    known = existing_ids()
    missing = [block_id for block_id in wanted if block_id not in known]
    if args.check:
        print('%d of %d blocks still have no blockstate' % (len(missing), len(wanted)))
        if missing:
            print('  e.g. %s' % missing[:10])
            return 1
        return 0

    written = 0
    unmatched = []
    incomplete = []
    for block_id in missing:
        template = find_template(block_id, families, known)
        if template is None:
            unmatched.append(block_id)
            continue
        blockstate = os.path.join(BLOCKSTATES, template + '.json')
        item_model = os.path.join(ITEM_MODELS, template + '.json')
        if not os.path.exists(item_model):
            incomplete.append((block_id, 'no item model for template ' + template))
            continue
        # Follow the template's *own* model references instead of assuming `block/blocks/<id>`: a
        # structural part points at `block/machine/multiblock/<id>`, an energy device at
        # `block/machine/energy/<id>`. Cloning the blockstate without the model it names would leave
        # a dangling reference, which the client reports as "Unable to load model".
        plan = []
        for reference in referenced_models(blockstate):
            target_ref = reference.replace(template, block_id)
            source = model_file(reference)
            if not os.path.exists(source):
                plan = None
                break
            plan.append((source, model_file(target_ref), target_ref))
        if plan is None:
            incomplete.append((block_id, 'template %s references a model that does not exist' % template))
            continue
        clone(blockstate, os.path.join(BLOCKSTATES, block_id + '.json'), template, block_id)
        written += 1
        for source, target, _ref in plan:
            if not os.path.exists(target):
                clone(source, target, template, block_id)
                written += 1
        if not os.path.exists(os.path.join(ITEM_MODELS, block_id + '.json')):
            clone(item_model, os.path.join(ITEM_MODELS, block_id + '.json'), template, block_id)
            written += 1

    print('wrote %d files for %d blocks (templates cloned per family)'
          % (written, len(missing) - len(unmatched) - len(incomplete)))
    if unmatched:
        print()
        print('%d ids have no sibling to clone (a dedicated model is needed):' % len(unmatched))
        for block_id in unmatched:
            print('  ' + block_id)
    if incomplete:
        print()
        print('%d ids were skipped so that no blockstate points at a missing model:' % len(incomplete))
        for block_id, why in incomplete:
            print('  %-52s %s' % (block_id, why))
    return 0 if not unmatched and not incomplete else 3


if __name__ == '__main__':
    sys.exit(main())
