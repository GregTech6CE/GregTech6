"""Lossless material generation and Java identifier migration contracts."""
from pathlib import Path
import shutil
import sys
import uuid
import unittest

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))
from modular_materials import JAVA, GROUPS, parse_catalog, write_catalog
from readable_data_names import readable_java


class MaterialGenerationTests(unittest.TestCase):
    def test_regeneration_matches_checked_in_catalogs(self):
        source = (TOOLS / 'generated/materials-source.java.txt').read_text(encoding='utf-8')
        groups = parse_catalog(source)
        self.assertEqual({'Elements': 172, 'Compounds': 570, 'Ores': 125,
                          'Stones': 71, 'Woods': 124},
                         {name: len(rows) for name, rows in groups.items()})
        scratch = TOOLS.parent / 'work'
        scratch.mkdir(exist_ok=True)
        # Inherit the workspace ACL: Python's 0700 temporary-directory ACL can
        # exclude the sandbox identity on Windows. This uniquely owned directory
        # stays under work/, and only it is removed by this test's cleanup.
        root = (scratch / ('material-regeneration-' + uuid.uuid4().hex)).resolve()
        self.assertTrue(root.is_relative_to(scratch.resolve()))
        root.mkdir()
        self.addCleanup(shutil.rmtree, root)
        (root / 'data/generated').mkdir(parents=True)
        shutil.copyfile(JAVA / 'data/ImportedMaterialData.java', root / 'data/ImportedMaterialData.java')
        write_catalog(source, root, save_intermediate=False)
        paths = ['content/material/Materials.java', 'data/generated/GT6Materials.java']
        paths += [f'content/material/generated/{name}.java' for name in GROUPS.values()]
        for path in paths:
            with self.subTest(path=path):
                self.assertEqual((JAVA / path).read_text(encoding='utf-8'),
                                 (root / path).read_text(encoding='utf-8'))

    def test_rename_preserves_registry_strings_and_comments(self):
        source = 'RM.Mixer; String id = "RM.FL"; // RM stays\n/* FL stays */ FL.Water;'
        self.assertEqual('MachineRecipeMaps.Mixer; String id = "RM.FL"; // RM stays\n'
                         '/* FL stays */ RegisteredFluids.Water;', readable_java(source))

    def test_worldgen_references_resolve_after_material_rename(self):
        from generate_ore_block_assets import parse_definitions, WORLDGEN_FILES, WORLDGEN_REF
        materials, _ = parse_definitions()
        references = {token for path in WORLDGEN_FILES
                      for token in WORLDGEN_REF.findall(path.read_text(encoding='utf-8'))}
        self.assertGreater(len(references), 20)
        self.assertEqual(set(), references - materials.keys())


if __name__ == '__main__':
    unittest.main()
