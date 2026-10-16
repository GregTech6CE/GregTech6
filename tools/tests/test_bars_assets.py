import importlib.util
import json
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / "src/main/resources/assets/gregtech"
SPEC = importlib.util.spec_from_file_location("bars", ROOT / "tools/generate_bars_assets.py")
bars = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(bars)


class BarsAssets(unittest.TestCase):
    def test_every_connection_mask_has_the_gt6_geometry(self):
        expected = bars.output_files()
        for path, payload in expected.items():
            self.assertEqual(json.loads(path.read_text(encoding="utf-8")), payload, path)
        self.assertEqual(len(expected), 7 * (16 + 3))
        for material in bars.MATERIALS:
            for mask in range(16):
                model = json.loads((SHARED_ASSETS / f"models/block/decoration/bars_{material}_{mask}.json")
                                   .read_text(encoding="utf-8"))
                self.assertGreater(len(model["elements"]), 0, (material, mask))
                self.assertEqual(len(model["elements"]),
                                 6 if mask == 0 else sum(bool(mask & bits) for bits, _, _ in bars.PASSES))

    def test_inventory_and_world_share_a_tinted_texture(self):
        self.assertTrue((ASSETS / "textures/block/machines/multiblockparts/metalwall/0/colored/side.png").is_file())
        for material in bars.MATERIALS:
            item = json.loads((SHARED_ASSETS / f"models/item/bars_{material}.json").read_text(encoding="utf-8"))
            self.assertEqual(item["parent"], f"gregtech:block/decoration/bars_{material}")
            for mask in range(16):
                model = json.loads((SHARED_ASSETS / f"models/block/decoration/bars_{material}_{mask}.json")
                                   .read_text(encoding="utf-8"))
                for element in model["elements"]:
                    self.assertTrue(all(face["tintindex"] == 0 for face in element["faces"].values()))


if __name__ == "__main__":
    unittest.main()
