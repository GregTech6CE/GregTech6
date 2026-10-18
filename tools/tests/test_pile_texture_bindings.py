"""Catch faces whose resource paths are mistaken for BlockModel texture-map keys."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("pile_assets", ROOT / "tools/generate_pile_assets.py")
generator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(generator)


class PileTextureBindings(unittest.TestCase):
    def assert_bound(self, model):
        for element in model["elements"]:
            for face in element["faces"].values():
                key = face["texture"].removeprefix("#")
                # BlockModel.getMaterial resolves keys in textureMap, not raw resource locations.
                self.assertIn(key, model["textures"])
                namespace, path = model["textures"][key].split(":", 1)
                self.assertTrue((native_path(ROOT / "src/main/resources/assets" / namespace / "textures" / (path + ".png"))).is_file())

    def test_generated_model_binds_every_face(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder) / "test.json"
            generator.write_model(target, "gt6ce:block/machines/placeables/ingot/top",
                                  generator.ingot_elements(9, generator.material_faces("ingot")))
            self.assert_bound(json.loads(target.read_text()))

    def test_all_shipped_pile_models_bind_faces(self):
        for path in generator.MODELS.glob("*.json"):
            with self.subTest(model=path.name):
                self.assert_bound(json.loads(path.read_text()))

    def test_material_pile_models_reach_gt6_selection_height(self):
        # MultiTileEntityIngot / Plate / PlateGem select ceil(size / 8) * 2 and
        # ceil(size / 4) * 1 model pixels respectively; the block's collision
        # test independently checks the complete-layer (floor) height.
        for kind, divisor, pixels_per_layer in (("ingot", 8, 2), ("plate", 4, 1),
                                                ("gem_plate", 4, 1)):
            for count in range(65):
                with self.subTest(kind=kind, count=count):
                    model = json.loads((generator.MODELS / f"{kind}_{count}.json").read_text())
                    self.assertEqual(max(item["to"][1] for item in model["elements"]),
                                     max(1, ((count + divisor - 1) // divisor) * pixels_per_layer))
                    self.assertEqual(len(model["elements"]),
                                     (max(1, count) if kind == "ingot" else max(1, min(count, 4))))

    def test_material_pile_textures_are_byte_identical_to_gt6(self):
        for port_folder, original_folder in generator.PILE_TEXTURES.items():
            for face in ("top", "sides"):
                with self.subTest(kind=port_folder, face=face):
                    port = generator.ASSETS / "textures/block/machines/placeables" / port_folder / f"{face}.png"
                    gt6 = (native_path(generator.ORIGINAL / "src/main/resources/assets/gregtech/textures/blocks"
                           / "machines/placeables" / original_folder / f"{face}.png"))
                    self.assertEqual(port.read_bytes(), gt6.read_bytes())


if __name__ == "__main__":
    unittest.main()
