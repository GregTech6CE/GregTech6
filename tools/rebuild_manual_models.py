"""GT6 F10 geometry, expressed in 1/16-block units. No GTM assets or code.

Reference: MultiTileEntityMortar/GrindStone/SiftingTable/Crank.setBlockBounds2,
getTexture2 in the original GT6 source. Recipe-display markers are not geometry.
"""
from pathlib import Path
import copy
import json

ROOT = Path(__file__).resolve().parents[1] / 'platform-shared/src/main/resources/assets/gregtech'
SIDES = ('down', 'up', 'north', 'south', 'west', 'east')

def box(lo, hi, texture, **faces):
    textures = dict.fromkeys(SIDES, texture) if texture else {}
    textures.update(faces)
    return {'from': lo, 'to': hi, 'faces': {side: {'texture': '#' + tex, 'tintindex': 0}
            for side, tex in textures.items() if tex}}

def model(name, folder, parts):
    keys = {face['texture'][1:] for part in parts for face in part['faces'].values()}
    children = {}
    for layer in ('colored', 'overlay'):
        elements = copy.deepcopy(parts)
        if name == "mortar_block" and layer == "colored":
            for face in elements[-1]["faces"].values(): face["tintindex"] = 1
        textures = {key: f'gregtech:block/machines/tools/{folder}/{layer}/{key}' for key in sorted(keys)}
        for path in textures.values():
            assert (ROOT / 'textures' / (path.split(':')[1] + '.png')).exists(), path
        if layer == 'overlay':
            for part in elements:
                for face in part['faces'].values(): face.pop('tintindex', None)
        children[layer] = {'textures': textures, 'elements': elements, 'render_type': 'minecraft:cutout'}
    particle = next(iter(children['colored']['textures'].values()))
    output = {'parent': 'minecraft:block/block', 'loader': 'forge:composite',
              'textures': {'particle': particle}, 'children': children}
    path = ROOT / f'models/block/machine/tool/{name}.json'
    path.write_text(json.dumps(output, indent=2) + '\n', encoding='utf-8')

def main():
    model('mortar_block', 'mortar', [
        box([2,0,2], [4,6,14], None, west='sides', east='insides', up='top'),
        box([2,0,2], [14,6,4], None, north='sides', south='insides', up='top'),
        box([12,0,2], [14,6,14], None, east='sides', west='insides', up='top'),
        box([2,0,12], [14,6,14], None, south='sides', north='insides', up='top'),
        box([2,0,2], [14,1,14], None, up='top', down='bottom'),
        box([6,0,6], [10,9,10], 'middleside', down=None, up='middletop'),
    ])
    # North-facing axle runs east-west; the removable stone is the last render pass.
    grind = [box([0,0,0], [16,2,16], 'bottom'),
             box([5,0,5], [6,11,11], 'legs'), box([10,0,5], [11,11,11], 'legs'),
             box([3,8,7], [13,10,9], 'axle')]
    model('grindstone_empty', 'grindstone', grind)
    model('grindstone_block', 'grindstone', grind + [box([6,3,2], [10,15,14], 'stone')])
    model('sifting_table', 'sifting_table', [
        *(box([x,0,z], [x+2,13,z+2], 'legs') for x,z in ((0,0),(14,0),(0,14),(14,14))),
        box([0,10,0], [16,12,16], 'border', up='grid', down='grid'),
        box([0,2,0], [16,5,16], 'plate'),
    ])
    # The port's FACING points outward; original mFacing points into the attached block.
    # Thus the north-facing model lies against the south block boundary.
    model('crank', 'crank', [box([2,2,13], [14,14,15], 'front'), box([5,5,15], [11,11,16], 'front')])
    crank_path = ROOT / 'models/block/machine/tool/crank.json'
    crank = json.loads(crank_path.read_text(encoding='utf-8'))
    crank['children']['colored']['textures']['axle'] = 'gregtech:block/iconsets/axle'
    for part in crank['children']['colored']['elements']:
        for side, face in part['faces'].items():
            if side != 'north': face['texture'] = '#axle'
    for part in crank['children']['overlay']['elements']:
        part['faces'] = {'north': part['faces']['north']}
    crank_path.write_text(json.dumps(crank, indent=2)+'\n', encoding='utf-8')
    variants = {}
    for facing, rotation in [('north',0),('east',90),('south',180),('west',270)]:
        for stone in (False, True):
            variants[f'facing={facing},stone={str(stone).lower()}'] = {
                'model': 'gregtech:block/machine/tool/' + ('grindstone_block' if stone else 'grindstone_empty'), 'y': rotation}
    (ROOT/'blockstates/grindstone_block.json').write_text(json.dumps({'variants': variants}, indent=2)+'\n', encoding='utf-8')

if __name__ == '__main__': main()
