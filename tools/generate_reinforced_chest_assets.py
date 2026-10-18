"""GT6 reinforced wood chest assets; Chinese names come from the user's GregTech.lang.

Original textures: GregTech-6 Team, LGPL-3.0-or-later. Only this family's keys are updated.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

import json
import re
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = native_path(ROOT / 'src/main/resources/assets/gt6ce')
ORIGINAL = ROOT.parent / 'gregtech6-master/gregtech6-master'


def specs():
    text = (native_path(ROOT / 'src/main/java/com/gregtech/gregtech/registry/GTStorageMetals.java')).read_text(encoding='utf-8')
    port = re.findall(r'new Spec\("([^"]+)", [^,]+, ([\d.]+)F, ([\d.]+)F\)', text)
    original = (native_path(ORIGINAL / 'src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java')).read_text(encoding='utf-8')
    source = re.findall(r'metalset\(aRegistry, aMetal, aUtilMetal, aMachine, aWooden, (?:MT|ANY)\.(\w+)\s*,\s*(\d+),\s*([\d.]+)F,\s*([\d.]+)F,', original)
    assert len(port) == len(source) == 60
    # This table is explicitly stored in GT6 registration order; reject reordered physical specs.
    for (suffix, hardness, resistance), (material, old_id, h, r) in zip(port, source):
        assert (float(hardness), float(resistance)) == (float(h), float(r)), suffix
        yield suffix, int(old_id) + 500


def generate(assets=ASSETS):
    def write(path, value):
        target = assets / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

    names = dict(re.findall(r'^\s*S:gt.multitileentity\.(\d+)=(.*)$',
                           (ROOT.parent / 'GregTech.lang').read_text(encoding='utf-8'), re.M))
    en = json.loads((ASSETS / 'lang/en_us.json').read_text(encoding='utf-8'))
    zh = json.loads((ASSETS / 'lang/zh_cn.json').read_text(encoding='utf-8'))
    family = list(specs())
    write('models/block/machine/storage/reinforced_wood_chest.json', {
        'parent': 'minecraft:block/block', 'textures': {'particle': 'gt6ce:block/machines/woodchest/overlay'}})
    write('models/item/reinforced_wood_chest.json', {
        'parent': 'gt6ce:item/metal_chest', 'textures': {'particle': 'gt6ce:block/machines/woodchest/overlay'}})
    for suffix, old_id in family:
        name = 'reinforced_wood_chest_' + suffix
        write('blockstates/' + name + '.json', {'variants': {'': {'model': 'gt6ce:block/machine/storage/reinforced_wood_chest'}}})
        write('models/item/' + name + '.json', {'parent': 'gt6ce:item/reinforced_wood_chest'})
        en['block.gt6ce.' + name] = en['block.gt6ce.chest_' + suffix].removesuffix(' Chest') + ' Reinforced Wooden Chest'
        zh['block.gt6ce.' + name] = names[str(old_id)].strip()
    write('lang/en_us.json', en)
    write('lang/zh_cn.json', zh)
    for shell in ('woodchest', 'lootchest'):
        for source_layer, layer in (('colored', 'colored'), ('plain', 'overlay')):
            relative = f'textures/block/machines/{shell}/{layer}.png'
            source = native_path(ORIGINAL / f'src/main/resources/assets/gregtech/textures/model/gt.multitileentity/{shell}.{source_layer}.png')
            target = assets / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(source.read_bytes())
    return family


if __name__ == '__main__':
    family = generate()
    tag = native_path(ROOT / 'src/main/resources/data/forge/tags/items/chests/wooden.json')
    data = json.loads(tag.read_text(encoding='utf-8')) if tag.exists() else {'replace': False, 'values': []}
    data['values'] = sorted(set(data['values']) | {'gt6ce:reinforced_wood_chest_' + suffix for suffix, _ in family})
    tag.parent.mkdir(parents=True, exist_ok=True)
    tag.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
    manifest = ROOT / 'tools/gt6_texture_sources.json'
    data = json.loads(manifest.read_text(encoding='utf-8'))
    for shell in ('woodchest', 'lootchest'):
        for source_layer, layer in (('colored', 'colored'), ('plain', 'overlay')):
            relative = f'textures/block/machines/{shell}/{layer}.png'
            data[relative] = {'sha256': hashlib.sha256((ASSETS / relative).read_bytes()).hexdigest(),
                             'source': str(native_path(f'src/main/resources/assets/gt6ce/textures/model/gt.multitileentity/{shell}.{source_layer}.png'))}
    manifest.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(f'Generated {len(family)} chest variants, four original texture layers, bilingual names and wooden chest tags.')
