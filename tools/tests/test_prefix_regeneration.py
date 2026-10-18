"""Legacy prefix generation must preserve restored geometry and curated translations."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('prefix_assets', ROOT / 'tools/generate_prefix_assets.py')
generator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(generator)
class PrefixRegenerationTests(unittest.TestCase):
    def generate(self, root):
        en, zh = root / 'en_us.json', root / 'zh_cn.json'
        original = {'item.gt6ce.custom_device': 'existing device',
                    'item.gt6ce.coin': 'curated coin name',
                    'itemGroup.gt6ce.coin': 'curated tab', 'jade.gt6ce.example': 'existing jade'}
        for path in (en, zh):
            path.write_text(json.dumps(original), encoding='utf-8')
        with patch.object(generator, 'MODELS', root / 'models'), \
                patch.object(generator, 'LANG_EN', en), patch.object(generator, 'LANG_ZH', zh):
            generator.main()
            generator.main()
        return original, en, zh
    def test_coin_remains_3d_for_every_texture_set(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            self.generate(root)
            for texture_set in generator.TEXTURE_SETS:
                coin = json.loads((root / 'models' / texture_set / 'coin.json').read_text())
                self.assertEqual('gt6ce:item/coin_minted', coin['parent'])
                self.assertNotIn('textures', coin)
    def test_existing_translations_survive_and_missing_ones_are_added(self):
        with tempfile.TemporaryDirectory() as folder:
            original, en, zh = self.generate(Path(folder))
            for path in (en, zh):
                result = json.loads(path.read_text(encoding='utf-8'))
                for key, value in original.items():
                    self.assertEqual(value, result.get(key), key)
                self.assertIn('item.gt6ce.dust', result)
if __name__ == '__main__':
    unittest.main()
