import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / "src/main/resources/assets/gregtech"

class ElectricToolResources(unittest.TestCase):
    def test_all_four_layered_models_resolve_textures(self):
        for tool in ("drill", "chainsaw", "wrench", "screwdriver"):
            model = json.loads((SHARED_ASSETS / f"models/item/electric_{tool}.json").read_text(encoding="utf-8"))
            self.assertEqual(set(model["textures"]), {f"layer{i}" for i in range(4)})
            for texture in model["textures"].values():
                namespace, path = texture.split(":", 1)
                self.assertEqual(namespace, "gregtech")
                self.assertTrue((ASSETS / "textures" / (path + ".png")).is_file(), texture)

    def test_material_name_and_charge_have_bilingual_placeholders(self):
        for lang in ("en_us", "zh_cn"):
            text = json.loads((ASSETS / f"lang/{lang}.json").read_text(encoding="utf-8"))
            for key in ("item.gregtech.electric_tool.named", "tooltip.gregtech.electric_tool.energy"):
                self.assertEqual(text[key].count("%s"), 2)
            for tool in ("drill", "chainsaw", "wrench", "screwdriver"):
                self.assertTrue(text[f"item.gregtech.electric_{tool}"])

if __name__ == "__main__":
    unittest.main()
