import json,re,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
class LocalizationCoverageTests(unittest.TestCase):
    def test_all_english_fallback_keys_exist_in_chinese_catalog(self):
        root=ROOT/'core/src/main/resources/assets/gregtech/lang'
        en=json.loads((root/'en_us.json').read_text(encoding='utf-8'))
        zh=json.loads((root/'zh_cn.json').read_text(encoding='utf-8'))
        # The complete original Chinese patch intentionally contains additional
        # legacy keys. Exact formatting is checked against that source, not a
        # guessed modern-English argument count (test_standard_chinese).
        self.assertTrue(en.keys() <= zh.keys())
    def test_static_tooltip_and_jade_keys_exist(self):
        en=json.loads((ROOT/'core/src/main/resources/assets/gregtech/lang/en_us.json').read_text(encoding='utf-8'))
        missing=set()
        for directory in ('src/main/java', 'neoforge/src/main/java', 'core/src/main/java'):
            for path in (ROOT/directory).rglob('*.java'):
                for key in re.findall(r'"((?:tooltip|jade|message|gui|config)\.gregtech\.[a-zA-Z0-9_.]+)"',path.read_text(encoding='utf-8')):
                    if not key.endswith('.') and key not in en:missing.add(key)
        self.assertFalse(missing,sorted(missing))
