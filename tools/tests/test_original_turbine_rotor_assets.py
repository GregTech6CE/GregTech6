"""Guard GT6's 3x3 rotor sprites and the eight six-way original housings."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path


import json
import struct
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PORT = native_path(ROOT / "src/main/resources/assets/gt6ce")
SOURCE = ROOT.parent / "gregtech6-master/gregtech6-master/src/main/resources/assets/gregtech"

ORIGINALS = (
    "magnalium_steam_turbine_main_housing",
    "trinitanium_steam_turbine_main_housing",
    "graphene_steam_turbine_main_housing",
    "vibramantium_steam_turbine_main_housing",
    "magnalium_gas_turbine_main_housing",
    "trinitanium_gas_turbine_main_housing",
    "graphene_gas_turbine_main_housing",
    "vibramantium_gas_turbine_main_housing",
)


class OriginalTurbineRotorAssets(unittest.TestCase):
    def test_gt6_rotor_sheets_preserve_size_and_animation(self):
        for name, height in (("turbine.png", 48), ("turbine_active.png", 384)):
            original = SOURCE / "textures/blocks/machines/multiblockmains" / name
            shipped = PORT / "textures/block/machines/multiblockmains" / name
            self.assertEqual(shipped.read_bytes(), original.read_bytes(), name)
            with shipped.open("rb") as image:
                self.assertEqual(image.read(16)[:8], b"\x89PNG\r\n\x1a\n")
                self.assertEqual(struct.unpack(">II", image.read(8)), (48, height))
        animation = json.loads((PORT / "textures/block/machines/multiblockmains/turbine_active.png.mcmeta").read_text())
        self.assertEqual(animation["animation"]["frametime"], 1)
        self.assertEqual(animation["animation"]["frames"], list(range(7, -1, -1)))

    def test_original_housings_have_all_six_rotations_and_resolved_models(self):
        expected = {
            "facing=north": (0, 0), "facing=east": (0, 90),
            "facing=south": (0, 180), "facing=west": (0, 270),
            "facing=up": (270, 0), "facing=down": (90, 0),
        }
        for housing in ORIGINALS:
            with self.subTest(housing=housing):
                states = json.loads((PORT / "blockstates" / (housing + ".json")).read_text())["variants"]
                self.assertEqual({facing: (entry.get("x", 0), entry.get("y", 0))
                                  for facing, entry in states.items()}, expected)
                for entry in states.values():
                    namespace, model = entry["model"].split(":", 1)
                    self.assertEqual(namespace, "gt6ce")
                    self.assertTrue((PORT / "models" / (model + ".json")).is_file())


if __name__ == "__main__":
    unittest.main()
