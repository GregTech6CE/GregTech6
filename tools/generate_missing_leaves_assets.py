"""Creates the leaf models and textures of the wood species GT6 has no art for.

`tools/generate_gt6_tree_assets.py` emits a `blockstates/leaves_<species>.json` for **every** wood
species, each pointing at `gt6ce:block/iconsets/leaves_<species>`. The model (and the texture) was
only ever written for the species GT6 itself ships art for, so four species referenced a model that
does not exist:

  blue_mahoe, pine, ebony, white_mahoe

Every missing model costs a `FileNotFoundException` **per blockstate variant** - the 2026-09-18 crash
report held 59,192 of them (~590k log lines, a 99 MB `latest.log` for an 8 minute session). This tool
writes the four models, and the textures they name:

  * `blue_mahoe` - GT6 ships `LEAVES_BLUEMAHOE.png`, so that art is copied verbatim;
  * `pine`, `ebony`, `white_mahoe` - GT6 has no leaf art for these (they come from the ore dictionary,
    not from GT6's own tree list), so the texture is **synthesised**: the alpha and the luminance of
    GT6's neutral `LEAVES_RUBBER.png` are kept and the colour is replaced by the species colour from
    `WoodSpecies` (pine 0xBB974D, ebony 0x3A342E, white mahoe 0x7993A6). Recorded here because it is
    the only place in the port that invents art instead of restoring it.

Usage:
  python tools/generate_missing_leaves_assets.py            # write what is missing
  python tools/generate_missing_leaves_assets.py --check     # fail when something is missing
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
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, str(native_path('src')), 'main', 'resources', 'assets', 'gt6ce')
GT6_ICONS = os.path.join('F:\\', 'Dev', 'GregTech6', 'gregtech6-master', 'gregtech6-master', str(native_path('src')),
                         'main', 'resources', 'assets', 'gregtech', 'textures', 'blocks', 'iconsets')

# species -> (source texture in GT6, synthesised?, species colour for the synthesised ones)
SPECIES = {
    'blue_mahoe': ('LEAVES_BLUEMAHOE.png', False, 0x0F67FE),
    'pine': ('LEAVES_RUBBER.png', True, 0xBB974D),
    'ebony': ('LEAVES_RUBBER.png', True, 0x3A342E),
    'white_mahoe': ('LEAVES_RUBBER.png', True, 0x7993A6),
}

MODEL = {
    'parent': 'minecraft:block/cube_all',
    'textures': {'all': 'gt6ce:block/iconsets/leaves_%s'},
    'render_type': 'minecraft:cutout',
}


def recount(image, colour):
    """Keeps alpha and luminance, replaces the hue with `colour` scaled by the pixel's brightness."""
    pixels = image.convert('RGBA').load()
    width, height = image.size
    target = ((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF)
    out = Image.new('RGBA', image.size)
    out_pixels = out.load()
    for y in range(height):
        for x in range(width):
            red, green, blue, alpha = pixels[x, y]
            if alpha == 0:
                out_pixels[x, y] = (0, 0, 0, 0)
                continue
            luminance = (red * 299 + green * 587 + blue * 114) // 1000
            # GT6's leaf textures sit around mid grey; 128 as the neutral point keeps their contrast.
            scale = luminance / 128.0
            out_pixels[x, y] = (min(255, int(target[0] * scale)),
                                min(255, int(target[1] * scale)),
                                min(255, int(target[2] * scale)), alpha)
    return out


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, 'w', encoding='utf-8', newline='\n') as handle:
        json.dump(data, handle, indent=2)
        handle.write('\n')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()

    written = []
    missing = []
    for species, (source, synthesised, colour) in SPECIES.items():
        model_path = os.path.join(ASSETS, 'models', 'block', 'iconsets', 'leaves_%s.json' % species)
        texture_path = os.path.join(ASSETS, 'textures', 'block', 'iconsets', 'leaves_%s.png' % species)

        if args.check:
            if not os.path.exists(model_path):
                missing.append(model_path)
            if not os.path.exists(texture_path):
                missing.append(texture_path)
            continue

        source_path = os.path.join(GT6_ICONS, source)
        if not os.path.exists(source_path):
            print('GT6 source texture missing: %s' % source_path)
            return 2
        image = Image.open(source_path)
        result = recount(image, colour) if synthesised else image.convert('RGBA')
        os.makedirs(os.path.dirname(texture_path), exist_ok=True)
        result.save(texture_path)
        written.append(os.path.relpath(texture_path, ROOT))

        model = dict(MODEL)
        model['textures'] = {'all': MODEL['textures']['all'] % species}
        write_json(model_path, model)
        written.append(os.path.relpath(model_path, ROOT))

    if args.check:
        if missing:
            print('missing leaf assets: %s' % missing)
            return 1
        print('all %d species have a leaf model and texture' % len(SPECIES))
        return 0

    print('wrote %d files for %d species' % (len(written), len(SPECIES)))
    for path in written:
        print('  ' + path)
    return 0


if __name__ == '__main__':
    sys.exit(main())
