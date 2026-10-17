import json
from pathlib import Path
import unittest


PORT = Path(__file__).resolve().parents[2]
ASSETS = PORT / "src/main/resources/assets/gregtech"
SHARED_ASSETS = Path(__file__).resolve().parents[2] / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
SOURCE = (PORT.parent / "gregtech6-master/gregtech6-master/src/main/resources"
          / "assets/gregtech/textures/blocks/iconsets")
ICONS = ("mud", "clay_brown", "turf", "clay_red", "clay_yellow",
         "clay_blue", "clay_white")


class DiggableAssets(unittest.TestCase):
    def test_original_textures_and_canonical_models(self):
        for icon in ICONS:
            texture = ASSETS / f"textures/block/iconsets/{icon}.png"
            model = SHARED_ASSETS / f"models/block/iconsets/{icon}.json"
            state = SHARED_ASSETS / f"blockstates/{icon}.json"
            item = SHARED_ASSETS / f"models/item/{icon}.json"
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

    def test_legacy_ids_use_real_clay_and_turf_texture(self):
        for block_id, icon in (("diggable_clay", "clay_brown"), ("diggable_peat", "turf")):
            model = SHARED_ASSETS / f"models/block/decoration/{block_id}.json"
            self.assertEqual(json.loads(model.read_text(encoding="utf-8"))["parent"],
                             f"gregtech:block/iconsets/{icon}")
            self.assertEqual(json.loads((SHARED_ASSETS / f"models/item/{block_id}.json")
                                        .read_text(encoding="utf-8"))["parent"],
                             f"gregtech:block/decoration/{block_id}")


if __name__ == "__main__":
    unittest.main()
