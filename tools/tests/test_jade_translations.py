import json
import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class JadeTranslations(unittest.TestCase):
    def test_provider_config_names_exist_in_supported_languages(self):
        providers = ROOT / 'src/main/java/com/gregtech/gregtech/integration/jade'
        keys = set()
        for path in providers.glob('*Provider.java'):
            source = path.read_text(encoding='utf-8')
            identifiers = re.findall(
                r'ResourceLocation\.fromNamespaceAndPath\("([^"]+)",\s*"([^"]+)"\)', source)
            self.assertTrue(identifiers, f'Cannot determine Jade provider UID: {path.name}')
            keys.update(f'config.jade.plugin_{namespace}.{name}' for namespace, name in identifiers)
        self.assertTrue(keys)
        for locale in ('en_us', 'zh_cn'):
            translations = json.loads((ROOT / f'core/src/main/resources/assets/gregtech/lang/{locale}.json')
                                      .read_text(encoding='utf-8'))
            for key in sorted(keys):
                with self.subTest(locale=locale, key=key):
                    self.assertTrue(translations.get(key, '').strip(), f'Missing config translation: {key}')
