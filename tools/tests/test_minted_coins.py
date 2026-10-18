
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import importlib.util
import json
from pathlib import Path
import re
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
ASSETS = native_path(ROOT / "src/main/resources/assets/gt6ce")
spec = importlib.util.spec_from_file_location("minted_coins", ROOT / "tools/generate_minted_coin_assets.py")
generator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(generator)


class MintedCoins(unittest.TestCase):
    def test_stack_specific_die_is_wired_to_item_renderer(self):
        client = native_path(ROOT / "src/main/java/com/gregtech/gregtech/client")
        item = (native_path(ROOT / "src/main/java/com/gregtech/gregtech/item/CoinItem.java")).read_text()
        aliases = (client / "MaterialClientModels.java").read_text()
        model = (client / "CoinItemBakedModel.java").read_text()
        renderer = (client / "CoinItemRenderer.java").read_text()
        self.assertIn("CoinItemBakedModel::new", aliases)
        self.assertIn("CoinGeometry.hasCustomPattern(stack)", model)
        self.assertIn("isCustomRenderer() { return stamped; }", model)
        self.assertIn("CoinItemRenderer.instance()", item)
        self.assertIn("CoinGeometry.pattern(stack)", renderer)
        self.assertIn("pattern.inventoryPixelBoxes()", renderer)
        self.assertIn("CoinPileRenderer.TEXTURE", renderer)
        self.assertIn("CoinPileRenderer.SIDE_TEXTURE", renderer)
        self.assertIn("CoinPileRenderer.quad", renderer)

    def test_pattern_is_the_original_gt6_mint(self):
        source = (ROOT.parent / "gregtech6-master/gregtech6-master/src/main/java/gregtech/tileentity/placeables/MultiTileEntityCoin.java").read_text(encoding="utf-8")
        part = source[source.index("mShape = new boolean[][][]"):].split("}};", 1)[0]
        rows = re.findall(r"\{([TF](?:,[TF]){15})\}", part)
        expected = [sum(1 << i for i, bit in enumerate(row.split(",")) if bit == "T") for row in rows]
        actual = json.loads((ASSETS / "geometry/coin_shape.json").read_text())
        self.assertEqual(len(expected), 32)
        self.assertEqual(actual[0] + actual[1], expected)

    def test_all_material_models_keep_minted_geometry_after_regeneration(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory)
            generator.generate(output)
            for path in output.rglob("*.json"):
                self.assertEqual(path.read_bytes(), (ASSETS / path.relative_to(output)).read_bytes())
        model = json.loads((ASSETS / "models/item/coin_minted.json").read_text())
        heights = {e["to"][1] for e in model["elements"]}
        self.assertEqual(heights, {8.5, 8.75, 9})
        self.assertLess(len(model["elements"]), 256)  # Cut-away corners are genuinely absent.
        for element in model["elements"]:
            for face in element["faces"].values():
                self.assertEqual(face["tintindex"], 0)
                self.assertIn(face["texture"].removeprefix("#"), model["textures"])

    def test_item_relief_hides_the_same_internal_faces_as_gt6(self):
        shape = json.loads((ASSETS / "geometry/coin_shape.json").read_text())
        model = json.loads((ASSETS / "models/item/coin_minted.json").read_text())

        def depth(x, z):
            return ((shape[0][x] >> z) & 1) + 2 * ((shape[1][x] >> z) & 1)

        side_faces = 0
        for element in model["elements"]:
            x, _, z = element["from"]
            current = depth(x, z)
            for side, nx, nz in (("north", x, z - 1), ("south", x, z + 1),
                                 ("west", x - 1, z), ("east", x + 1, z)):
                expected = nx < 0 or nx >= 16 or nz < 0 or nz >= 16 or current < depth(nx, nz)
                self.assertEqual(side in element["faces"], expected, (x, z, side))
                side_faces += expected
        self.assertEqual(side_faces, 184)


if __name__ == "__main__":
    unittest.main()
