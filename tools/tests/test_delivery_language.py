"""Console encoding must not corrupt a successful language self-check receipt."""
from pathlib import Path
import copy
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'integration'))
from launch_production_smoke import parse_receipt, validate_language_inventory, validate_generated_language


class DeliveryLanguageTests(unittest.TestCase):
    def test_generated_families_must_exist_in_both_language_receipts(self):
        row = {'bees': 640, 'anvils': 35, 'books': 28, 'canvases': 16, 'panels': 80, 'creativeTabs': 150,
               'bottles': 169, 'faceMasks': 64, 'machineLabels': 7, 'engineDescriptions': 20, 'wireDescriptions': 620}
        language = {'generatedNamesEnglish': row, 'generatedNamesChinese': dict(row)}
        validate_generated_language(language)
        for bad in [{}, {'generatedNamesChinese': row},
                    {**language, 'generatedNamesEnglish': {**row, 'books': 22}},
                    {**language, 'generatedNamesEnglish': {**row, 'bottles': 0}},
                    {**language, 'generatedNamesEnglish': {**row, 'faceMasks': 63}},
                    {**language, 'generatedNamesEnglish': {**row, 'machineLabels': 6}},
                    {**language, 'generatedNamesEnglish': {**row, 'engineDescriptions': 0}},
                    {**language, 'generatedNamesEnglish': {**row, 'wireDescriptions': 0}},
                    {**language, 'generatedNamesChinese': {**row, 'creativeTabs': 149}}]:
            with self.subTest(receipt=bad), self.assertRaisesRegex(ValueError, 'bilingual'):
                validate_generated_language(bad)

    def test_language_inventory_rejects_inconsistent_or_duplicate_source_gap_rows(self):
        forms = {'registered': 50002, 'exactOriginal': 50000, 'missingOriginal': 2, 'originalCasingNames': 300}
        missing = [{'id': 'gregtech:example_' + str(i), 'sourceKey': 'oredict.example' + str(i),
                    'sourceNameAvailable': False} for i in range(2)]
        language = {'englishItemNames': 70000, 'chineseItemNames': 70000,
                    'latinNameCandidates': 0, 'materialFormNames': forms}
        inventory = {'registeredItems': 70000, 'candidateCount': 0, 'candidates': [],
                     'materialFormNames': forms, 'missingMaterialFormNames': missing}
        validate_language_inventory(inventory, language)
        broken = []
        row = copy.deepcopy(inventory); row['materialFormNames']['registered'] += 1; broken.append(row)
        row = copy.deepcopy(inventory); row['missingMaterialFormNames'].pop(); broken.append(row)
        row = copy.deepcopy(inventory); row['missingMaterialFormNames'][1] = row['missingMaterialFormNames'][0]; broken.append(row)
        row = copy.deepcopy(inventory); row['missingMaterialFormNames'][0]['sourceNameAvailable'] = True; broken.append(row)
        row = copy.deepcopy(inventory); row['materialFormNames']['originalCasingNames'] = 0; broken.append(row)
        for row in broken:
            with self.subTest(row=row), self.assertRaises(ValueError):
                validate_language_inventory(row, language)

    def test_native_messages_do_not_determine_structured_receipt_encoding(self):
        raw = b'Native message \x85\xff\nPRODUCTION_SMOKE_SUCCESS ' + '{"text":"中文 / 1538°C"}'.encode('utf-8')
        self.assertEqual(parse_receipt(raw, 0, 'sample.log')['text'], '中文 / 1538°C')

    def test_legacy_codepage_receipt_must_not_silently_replace_characters(self):
        raw = b'PRODUCTION_SMOKE_SUCCESS ' + '{"text":"1538°C"}'.encode('cp936')
        with self.assertRaises(UnicodeDecodeError):
            parse_receipt(raw, 0, 'sample.log')

    def test_failure_marker_is_not_hidden_by_native_encoding(self):
        raw = b'\xff PRODUCTION_SMOKE_FAILED\nPRODUCTION_SMOKE_SUCCESS {}'
        with self.assertRaisesRegex(ValueError, 'Production client failure'):
            parse_receipt(raw, 0, 'sample.log')

    def test_duplicate_receipts_and_failed_exit_are_rejected(self):
        for raw, code in [(b'PRODUCTION_SMOKE_SUCCESS {}\nPRODUCTION_SMOKE_SUCCESS {}', 0),
                          (b'PRODUCTION_SMOKE_SUCCESS {}', 1)]:
            with self.subTest(raw=raw, code=code), self.assertRaises(ValueError):
                parse_receipt(raw, code, 'sample.log')
