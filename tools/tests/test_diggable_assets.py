
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import json
from pathlib import Path
import unittest


PORT = Path(__file__).resolve().parents[2]
ASSETS = PORT / "core/src/main/resources/assets/gregtech"
SOURCE = (PORT.parent / "gregtech6-master/gregtech6-master/src/main/resources"
          / "assets/gregtech/textures/blocks/iconsets")
ICONS = ("mud", "clay_brown", "turf", "clay_red", "clay_yellow",
         "clay_blue", "clay_white")


class DiggableAssets(unittest.TestCase):
    def test_original_textures_and_canonical_models(self):
        for icon in ICONS:
            texture = ASSETS / f"textures/block/iconsets/{icon}.png"
            model = ASSETS / f"models/block/iconsets/{icon}.json"
            state = ASSETS / f"blockstates/{icon}.json"
            item = ASSETS / f"models/item/{icon}.json"
            self.assertTrue(texture.is_file(), str(texture))
            self.assertEqual(json.loads(model.read_text(encoding="utf-8"))["textures"]["all"],
                             f"gregtech:block/iconsets/{icon}")
            self.assertEqual(json.loads(state.read_text(encoding="utf-8"))["variants"][""]["model"],
                             f"gregtech:block/iconsets/{icon}")
            self.assertEqual(json.loads(item.read_text(encoding="utf-8"))["parent"],
                             f"gregtech:block/iconsets/{icon}")
            if SOURCE.is_dir():
                self.assertEqual(texture.read_bytes(), (SOURCE / f"{icon.upper()}.png").read_bytes(),
                                 f"{icon}: GT6 icon set differs")

    def test_retired_duplicate_ids_have_no_assets(self):
        for block_id in ("diggable_clay", "diggable_peat"):
            for relative in (f"models/block/decoration/{block_id}.json",
                             f"models/item/{block_id}.json", f"blockstates/{block_id}.json"):
                for base in (ASSETS, native_path(PORT / "src/main/resources/assets/gregtech")):
                    self.assertFalse((base / relative).exists(), str(base / relative))


if __name__ == "__main__":
    unittest.main()
