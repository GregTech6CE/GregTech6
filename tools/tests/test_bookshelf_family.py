"""The shipped GT6 bookshelf variant manifest and assets stay in lockstep."""
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("bookshelf_family", ROOT / "tools/generate_bookshelf_family.py")
family = importlib.util.module_from_spec(spec)
spec.loader.exec_module(family)


class BookShelfFamilyAssets(unittest.TestCase):
    def test_all_available_wood_and_all_metalset_variants_are_distinct(self):
        variants = json.loads(family.MANIFEST.read_text(encoding="utf-8"))["variants"]
        wood = [variant for variant in variants if variant["kind"] == "wood"]
        metal = [variant for variant in variants if variant["kind"] == "metal"]
        self.assertEqual((len(wood), len(metal)), (15, 60))
        self.assertEqual(len({variant["path"] for variant in variants}), len(variants))
        self.assertEqual(len({variant["gt6_id"] for variant in variants}), len(variants))
        self.assertEqual(next(v for v in variants if v["gt6_id"] == 7839)["path"], "bookshelf_bluespruce")
        self.assertEqual(next(v for v in variants if v["gt6_id"] == 7110)["material"], "Steel")
        for variant in variants:
            self.assertGreater(variant["hardness"], 0)
            self.assertGreater(variant["resistance"], 0)
            if variant["texture"].startswith("gt6ce:block/"):
                texture = (family.ASSETS / "textures/block"
                           / (variant["texture"].removeprefix("gt6ce:block/") + ".png"))
                self.assertTrue(texture.is_file(), str(texture))

    def test_regeneration_matches_every_shipped_variant(self):
        variants = json.loads(family.MANIFEST.read_text(encoding="utf-8"))["variants"]
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder)
            self.assertEqual(family.generate(output, variants), len(variants))
            generated = [file for file in output.rglob("*.json") if file.is_file()]
            self.assertEqual(len(generated), (len(variants) - 1) * 3)
            for file in generated:
                shipped = family.ASSETS / file.relative_to(output)
                self.assertEqual(file.read_bytes(), shipped.read_bytes(), str(shipped))

    def test_gt6_survival_recipes_match_every_registered_variant(self):
        variants = json.loads(family.MANIFEST.read_text(encoding="utf-8"))["variants"]
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder)
            self.assertEqual(family.generate_recipes(output, variants), 75)
            generated = sorted(output.glob("*.json"))
            self.assertEqual(len(generated), 75)
            self.assertEqual({file.stem for file in generated}, {v["path"] for v in variants})
            for file in generated:
                self.assertEqual(file.read_bytes(), (family.RECIPES / file.name).read_bytes())
                recipe = json.loads(file.read_text(encoding="utf-8"))
                variant = next(v for v in variants if v["path"] == file.stem)
                self.assertEqual(recipe["type"], "gt6ce:tool_shaped")
                self.assertIs(recipe["allow_mirror"], False)
                self.assertEqual(recipe["result"], {"item": f"gt6ce:{file.stem}", "count": 1})
                if variant["kind"] == "wood":
                    self.assertEqual(recipe["pattern"], ["PPP", "sfr", "PPP"])
                    self.assertEqual(recipe["key"]["f"], {"item": "gt6ce:tool_file"})
                    self.assertEqual(recipe["key"]["r"], {"item": "gt6ce:tool_soft_hammer"})
                else:
                    self.assertEqual(recipe["pattern"], ["PTP", "sdh", "PTP"])
                    self.assertEqual(recipe["key"]["d"], {"item": "gt6ce:tool_screwdriver"})
                    self.assertEqual(recipe["key"]["h"], {"item": "gt6ce:tool_hammer"})
                    material = family.re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", variant["material"]).lower()
                    self.assertEqual(recipe["key"]["P"], {"tag": f"forge:plates/{material}"})
                    self.assertEqual(recipe["key"]["T"], {"tag": f"forge:screws/{material}"})
        self.assertEqual(family.generate_recipes(check=True), 75)

    def test_original_registers_both_exact_crafting_shapes(self):
        if not family.ORIGINAL_LOADER.is_file():
            self.skipTest("Original GT6 checkout is not present")
        source = family.ORIGINAL_LOADER.read_text(encoding="utf-8")
        self.assertIn('"PTP", "sdh", "PTP", \'T\', OP.screw.dat(aMat), \'P\', OP.plate.dat(aMat)', source)
        self.assertIn('"PPP", "sfr", "PPP", \'P\', PlankData.PLANKS[i', source)


if __name__ == "__main__":
    unittest.main()
