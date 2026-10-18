
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("bookshelf_assets", ROOT / "tools/generate_bookshelf_assets.py")
assets = importlib.util.module_from_spec(spec)
spec.loader.exec_module(assets)


class BookShelfAssets(unittest.TestCase):
    def test_regeneration_matches_shipped_assets(self):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder)
            assets.generate(output)
            files = [p for p in output.rglob("*") if p.is_file()]
            self.assertEqual(len(files), 17)  # 3 models/states plus seven original texture pairs.
            for generated in files:
                shipped = assets.ASSETS / generated.relative_to(output)
                self.assertEqual(generated.read_bytes(), shipped.read_bytes(), str(shipped))

    def test_dusty_guide_textures_keep_gt6_provenance(self):
        ledger = json.loads((ROOT / "tools/gt6_texture_sources.json").read_text(encoding="utf-8"))
        for face in ("back", "side"):
            relative = f"textures/block/books/book_dusty_{face}.png"
            source_relative = str(native_path(f"src/main/resources/assets/gregtech/textures/blocks/books/BOOK_DUSTY_{face.upper()}.png"))
            source = assets.ORIGINAL / "textures/blocks/books" / f"BOOK_DUSTY_{face.upper()}.png"
            self.assertEqual(ledger[relative]["source"], source_relative)
            self.assertEqual(ledger[relative]["sha256"], hashlib.sha256(source.read_bytes()).hexdigest())
            self.assertEqual((assets.ASSETS / relative).read_bytes(), source.read_bytes())

    def test_double_sided_frame_and_four_rotations(self):
        model = json.loads((assets.ASSETS / "models/block/bookshelf.json").read_text())
        self.assertEqual(len(model["elements"]), 7)
        self.assertEqual(model["textures"]["wood"], "minecraft:block/oak_planks")
        self.assertEqual(
            [(element["from"], element["to"], set(element["faces"])) for element in model["elements"]],
            [
                ([0, 0, 0], [16, 16, 16], {"up", "down", "west", "east"}),
                ([0, 0, 0], [16, 1, 16], {"up", "north", "south"}),
                ([0, 15, 0], [16, 16, 16], {"down", "north", "south"}),
                ([0, 1, 0], [1, 15, 16], {"up", "down", "north", "south", "east"}),
                ([15, 1, 0], [16, 15, 16], {"up", "down", "north", "south", "west"}),
                ([1, 1, 7], [15, 15, 9], {"north", "south"}),
                ([1, 7, 1], [15, 9, 15], {"up", "down", "north", "south"}),
            ],
        )
        for element in model["elements"]:
            for face in element["faces"].values():
                self.assertIn(face["texture"].removeprefix("#"), model["textures"])
        states = json.loads((assets.ASSETS / "blockstates/bookshelf.json").read_text())["variants"]
        self.assertEqual([states["facing=" + d]["y"] for d in ("north", "east", "south", "west")], [0, 90, 180, 270])


if __name__ == "__main__":
    unittest.main()
