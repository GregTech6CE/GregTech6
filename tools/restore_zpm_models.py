"""GT6 ZPM models and language entries; retain old discharger registry names."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

from pathlib import Path
import json
import shutil
import hashlib
from tools.restore_laser_models import ROOT, ORIGINAL, ASSETS, SOURCE, write, cube, layer


def main():
    manifest = ROOT / 'tools/gt6_texture_sources.json'
    entries = json.loads(manifest.read_text(encoding='utf8'))

    def copy(source):
        relative = 'textures/block/' + source.relative_to(SOURCE).as_posix().lower()
        dest = ASSETS / relative
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, dest)
        entries[relative] = {'source': source.relative_to(ORIGINAL).as_posix(),
                             'sha256': hashlib.sha256(source.read_bytes()).hexdigest()}
        return 'gregtech:' + relative.removeprefix('textures/').removesuffix('.png')

    icons = {face: copy(next(SOURCE.rglob('ZPM_' + suffix + '.png')))
             for face, suffix in [('up', 'TOP'), ('down', 'BOTTOM'), ('north', 'SIDES')]}
    for face in ['south', 'west', 'east']:
        icons[face] = icons['north']
    model = layer(dict(icons, particle=icons['north']),
                  [cube([4, 0, 4], [12, 4, 12], True), cube([5, 4, 5], [11, 12, 11], True)])
    write('models/block/machine/energy/zpm.json', model)
    write('models/item/zpm.json', {'parent': 'gregtech:block/machine/energy/zpm'})
    write('blockstates/zpm.json', {'variants': {'': {'model': 'gregtech:block/machine/energy/zpm'}}})
    rotations = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270},
                 'up': {'x': 270}, 'down': {'x': 90}}
    for suffix, folder in [('basic', 'zpm_quantum'), ('advanced', 'zpm_electricity'), ('elite', 'zpm_quantum')]:
        source = SOURCE / 'machines/energystorages' / folder
        for path in source.rglob('*.png'):
            copy(path)
        name = 'zpm_discharger_' + suffix
        variants = {}
        for active in (False, True):
            for loaded in (False, True):
                children = {}
                for child, part in [('colored', 'colored'), ('overlay', 'overlay_active' if active else 'overlay')]:
                    textures = {face: f'gregtech:block/machines/energystorages/{folder}/{part}/' +
                                ('front' if face == 'north' else 'back' if face == 'south' else 'side') for face in rotations}
                    children[child] = layer(textures, [cube([0, 0, 0], [16, 16, 16], child == 'colored')])
                if loaded:
                    children['module'] = layer({'module': icons['up']}, [{'from': [0, 0, 16.01], 'to': [16, 16, 16.01],
                        'faces': {'south': {'texture': '#module', 'tintindex': 1}}}])
                path = f'block/machine/energy/{name}_{int(active)}_{int(loaded)}'
                write('models/' + path + '.json', {'parent': 'minecraft:block/block', 'loader': 'forge:composite',
                      'textures': {'particle': f'gregtech:block/machines/energystorages/{folder}/colored/side'}, 'children': children})
                for facing, rotation in rotations.items():
                    variants[f'facing={facing},active={str(active).lower()},loaded={str(loaded).lower()}'] = dict(model='gregtech:' + path, **rotation)
        write(f'blockstates/{name}.json', {'variants': variants})
        write(f'models/item/{name}.json', {'parent': f'gregtech:block/machine/energy/{name}_0_0'})
    manifest.write_text(json.dumps(entries, indent=2, sort_keys=True) + '\n', encoding='utf8')
    translations = {
        'block.gregtech.zpm': ('Zero-Point Module (ZPM)', '零点模块（ZPM）'),
        'block.gregtech.zpm_discharger_basic': ('ZPM Decharger (QU)', 'ZPM 放电器（QU）'),
        'block.gregtech.zpm_discharger_advanced': ('ZPM Decharger (EU)', 'ZPM 放电器（EU）'),
        'block.gregtech.zpm_discharger_elite': ('ZPM Decharger (QU, Legacy)', 'ZPM 放电器（QU，旧版别名）'),
        'gregtech.zpm.charge': ('Charge: %s / %s QU', '储能：%s / %s QU'),
        'gregtech.zpm.non_rechargeable': ('Ancient artifact; cannot be recharged', '远古遗物；无法重新充电'),
        'gregtech.zpm.output': ('Front output: %s %s/t', '正面输出：%s %s/t'),
        'gregtech.zpm.controls': ('Use ZPM to insert; empty hand to remove; sneak empty hand to switch', '手持 ZPM 放入；空手取出；潜行空手切换开关'),
        'gregtech.zpm.status': ('Stored: %s QU | %s | Output: %s', '储能：%s QU | %s | 输出：%s'),
    }
    for i, locale in enumerate(['en_us', 'zh_cn']):
        p = ASSETS / f'lang/{locale}.json'
        lang = json.loads(p.read_text(encoding='utf8'))
        lang.update({key: value[i] for key, value in translations.items()})
        p.write_text(json.dumps(lang, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    tagpath = native_path(ROOT / 'src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json')
    tag = json.loads(tagpath.read_text(encoding='utf8'))
    for suffix in ['basic', 'advanced', 'elite']:
        name = 'gregtech:zpm_discharger_' + suffix
        if name not in tag['values']:
            tag['values'].append(name)
    tagpath.write_text(json.dumps(tag, indent=2) + '\n', encoding='utf8')

    recipes = native_path(ROOT / 'src/main/resources/data/gregtech/recipes/zpm')
    recipes.mkdir(parents=True, exist_ok=True)
    # Original 11170/11171 patterns, using currently registered representative circuits.
    for suffix, crystal in [('basic', 'ruby'), ('advanced', 'sapphire')]:
        recipe = {'type': 'minecraft:crafting_shaped', 'pattern': ['PCP', 'CMC', 'FCF'],
                  'key': {key: {'item': 'gregtech:' + item} for key, item in {
                      'P': 'crystal_processor_' + crystal, 'C': 'circuit_ultimate',
                      'F': 'compact_force_field_emitter_luv', 'M': 'casing_machine_dense_osmiridium'}.items()},
                  'result': {'item': 'gregtech:zpm_discharger_' + suffix}}
        (recipes / (suffix + '.json')).write_text(json.dumps(recipe, indent=2) + '\n', encoding='utf8')


if __name__ == '__main__':
    main()
