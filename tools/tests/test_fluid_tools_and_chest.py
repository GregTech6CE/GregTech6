
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import json
import unittest
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = native_path(ROOT / "src/main/resources/assets/gregtech")

class FluidToolsAndChest(unittest.TestCase):
    def test_chest_pixels_match_slot_coordinates(self):
        image = Image.open(ASSETS / "textures/gui/chests/54.png").convert("RGB")
        for y in [18,36,54,72,90,108,140,158,176,198]:
            for x in range(8,153,18):
                self.assertEqual(image.getpixel((x-1,y-1)), (55,55,55))
                self.assertEqual(image.getpixel((x,y)), (139,139,139))
                self.assertEqual(image.getpixel((x+16,y+16)), (255,255,255))
    def test_solid_gas_source_texture(self):
        image = Image.open(ASSETS / "textures/block/material_icons/fluid/gas.png").convert("RGBA")
        self.assertEqual(len(image.getcolors(image.width*image.height)), 1)
        self.assertEqual(image.getpixel((0,0)), (255,255,255,170))
    def test_variant_resources_and_translations(self):
        ids = []
        for kind in ["measuring_pot","barometer_gas_cylinder"]:
            ids += ["fluid_"+kind+"_"+mat for mat in ["stainless_steel","tungsten","tantalum_hafnium_carbide"]]
        for kind in ["tap","fluid_funnel","cap_nozzle"]:
            ids += [kind+"_"+mat for mat in (["steel"] if kind=="cap_nozzle" else ["ceramic"])+["plastic","stainless_steel","tungsten","tantalum_hafnium_carbide","adamantium"]]
        for locale in ["en_us","zh_cn"]:
            lang=json.loads((ASSETS/"lang"/(locale+".json")).read_text(encoding="utf-8"))
            for name in ids:
                self.assertIn("block.gregtech."+name,lang)
                for folder in ["blockstates","models/item"]:
                    self.assertTrue((ASSETS/folder/(name+".json")).is_file(), name)
