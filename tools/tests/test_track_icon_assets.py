"""GT6 rail sprites are texture inputs for the 30 real tracks, not icon blocks."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path


import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
ASSETS = native_path(ROOT / "src/main/resources/assets/gt6ce")


class TrackIconAssetsTest(unittest.TestCase):
    def test_real_tracks_resolve_their_textures(self):
        blockstates = ASSETS / "blockstates"
        track_states = sorted(blockstates.glob("track_*.json"))
        self.assertEqual(30, len(track_states))
        models = ASSETS / "models/block"
        referenced_textures = set()
        for path in track_states:
            for variant in json.loads(path.read_text(encoding="utf-8"))["variants"].values():
                model = models / (variant["model"].removeprefix("gt6ce:block/") + ".json")
                self.assertTrue(model.is_file(), f"{path.name} -> {model}")
                texture = json.loads(model.read_text(encoding="utf-8"))["textures"]["rail"]
                self.assertTrue(texture.startswith("gt6ce:block/iconsets/rail_"), texture)
                png = ASSETS / "textures/block" / (texture.removeprefix("gt6ce:block/") + ".png")
                self.assertTrue(png.is_file(), f"{model.name} -> {png}")
                referenced_textures.add(png)
        self.assertEqual(60, len(referenced_textures))  # six shapes x ten GT6 materials

        item_models = sorted((ASSETS / "models/item").glob("track_*.json"))
        self.assertEqual(30, len(item_models))
        for item_model in item_models:
            parent = json.loads(item_model.read_text(encoding="utf-8"))["parent"]
            self.assertEqual(
                f"gt6ce:block/tracks/{item_model.stem}_flat", parent,
                f"{item_model.name} should preview its own idle flat track",
            )
            self.assertTrue((models / (parent.removeprefix("gt6ce:block/") + ".json")).is_file())

    def test_no_standalone_rail_icon_models_or_translations(self):
        for directory in ("blockstates", "models/block/iconsets", "models/item"):
            self.assertEqual([], list((ASSETS / directory).glob("rail_*.json")), directory)
        for locale in ("en_us", "zh_cn"):
            translations = json.loads((ASSETS / "lang" / f"{locale}.json").read_text(encoding="utf-8"))
            self.assertFalse(any(key.startswith("block.gt6ce.rail_") for key in translations))
            self.assertIn("block.gt6ce.track_steel", translations)


if __name__ == "__main__":
    unittest.main()
