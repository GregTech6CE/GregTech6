"""GT6 metalset scaffold family: original material table, textures, names and recipes."""
import copy
import json
import re
from pathlib import Path
SHARED_ASSETS = Path(__file__).resolve().parents[1] / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
from generate_reinforced_chest_assets import specs, ROOT, ASSETS

def generate():
    def write(path, data):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding='utf-8')
    books = {v['gt6_id'] - 7100: v for v in json.loads((ROOT / 'src/main/resources/data/gregtech/bookshelf_variants.json').read_text())['variants'] if v['kind'] == 'metal'}
    source = (ROOT / 'src/main/java/com/gregtech/gregtech/registry/GTStorageMetals.java').read_text(encoding='utf-8')
    materials = dict(re.findall(r'new Spec\("([^"]+)", [^,]+\.(\w+),', source))
    names = dict(re.findall(r'^\s*S:gt.multitileentity\.(\d+)=(.*)$', (ROOT.parent / 'GregTech.lang').read_text(encoding='utf-8'), re.M))
    langs = {locale: json.loads((ASSETS / f'lang/{locale}.json').read_text(encoding='utf-8')) for locale in ('en_us','zh_cn')}
    states = json.loads((SHARED_ASSETS / 'blockstates/scaffold.json').read_text())
    for suffix, chest_id in specs():
        old_id = chest_id - 500
        name = 'scaffold' if suffix == 'steel' else 'scaffold_' + suffix
        texture = books[old_id]['texture'].replace('/casingmachine', '/blocksolid')
        assert (ASSETS / ('textures/' + texture.split(':')[1] + '.png')).is_file(), texture
        if suffix != 'steel':
            mapping = copy.deepcopy(states)
            for variant in mapping['variants'].values():
                variant['model'] = variant['model'].replace('tool/scaffold', 'tool/' + name)
            write(SHARED_ASSETS / f'blockstates/{name}.json', mapping)
            for design in range(4):
                tail = '_' + str(design) if design else ''
                model = json.loads((SHARED_ASSETS / f'models/block/tool/scaffold{tail}.json').read_text())
                base = model.get('children', {}).get('base', model)
                base['textures']['rod'] = texture
                write(SHARED_ASSETS / f'models/block/tool/{name}{tail}.json', model)
            write(SHARED_ASSETS / f'models/item/{name}.json', {'parent': f'gregtech:block/tool/{name}_1'})
        material = re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', materials[suffix]).lower()
        write(ROOT / f'src/main/resources/data/gregtech/recipes/scaffolds/{name}.json', {
            'type': 'gregtech:tool_shaped', '_comment': f'GT6 Loader_MultiTileEntities:147 ({8400 + old_id})',
            'pattern': ['TPT', 'SdS'], 'key': {'T': {'tag': 'forge:screws/' + material},
            'P': {'tag': 'forge:plates/' + material}, 'S': {'tag': 'forge:rods/' + material},
            'd': {'item': 'gregtech:tool_screwdriver'}}, 'allow_mirror': False,
            'result': {'item': 'gregtech:' + name}})
        langs['en_us']['block.gregtech.' + name] = langs['en_us']['block.gregtech.chest_' + suffix].removesuffix(' Chest') + ' Scaffold'
        langs['zh_cn']['block.gregtech.' + name] = names[str(8400 + old_id)].strip()
    for locale, data in langs.items():
        write(ASSETS / f'lang/{locale}.json', data)
    print('Generated 60 scaffold variants and recipes')
if __name__ == '__main__':
    generate()
