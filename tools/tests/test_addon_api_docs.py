"""Regression cases for documentation drift, using real SDK declarations."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import check_addon_api_docs as docs


class AddonApiDocsTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.texts = [(docs.ROOT / p).read_text(encoding='utf-8') for p in docs.DOCS]
        cls.contract = json.loads(docs.CONTRACT.read_text(encoding='utf-8'))
        cls.index = docs.java_index(docs.ROOT)

    def references(self, text):
        return docs.reference_errors(text, self.index, self.contract)

    def mutate(self, language, old, new):
        self.assertTrue(old in self.texts[language], f'Missing mutation target: {old}')
        texts = list(self.texts)
        texts[language] = texts[language].replace(old, new, 1)
        return docs.check(texts=texts)

    def test_current_guides_pass(self):
        self.assertEqual([], docs.check(texts=self.texts))

    def test_pre_patch_excerpt_detects_both_original_errors(self):
        text = (Path(__file__).parent / 'fixtures/addon_api_before_six_fixes.md').read_text(encoding='utf-8')
        errors = '\n'.join(self.references(text))
        self.assertIn('emit(String type', errors)
        self.assertIn('AddonPlatform.Services.inject', errors)
        self.assertIn('bootstrap', errors)

    def test_structure_notification_is_a_public_facade_not_a_bootstrap_entry(self):
        self.assertEqual([], self.references('`AddonStructures.notifyChanged(Level level, BlockPos pos)`'))
        self.assertTrue(self.references('`AddonPlatform.Services.structureChanged`'))
        self.assertTrue(self.references('`StructureWatchIndex.touch(level, pos)`'))
        for guide in self.texts:
            self.assertIn('`AddonStructures.notifyChanged(level, pos)`', guide)
            self.assertNotIn('`StructureWatchIndex.touch(level, pos)`', guide)

    def test_arbitrary_wrong_parameter_names_are_not_special_cased(self):
        self.assertTrue(self.references('`emit(String destination, long size, long amount)`'))
        self.assertEqual([], self.references('`emit(String part, long size, long amount)`'))
        self.assertTrue(self.references('`MachineBehaviorContext.emit(String part, int size, long amount)`'))

    def test_unknown_types_members_and_wrong_nested_owner(self):
        for token in ('MachineBehavour', 'MachineBehavior.noSuchCallback', 'EnergyInput.range',
                      'BehaviorMachine.Builder.rangeSpecified'):
            with self.subTest(token=token):
                self.assertTrue(self.references('`' + token + '`'))
        self.assertEqual([], self.references('`EnergyInput.Builder.range`'))

    def test_record_accessors_and_overloads_are_visible(self):
        self.assertEqual([], self.references('`SensorContext.side` `MaterialComposition.Part.amount`'))
        self.assertEqual([], self.references('`MachineBehaviorContext.emit(long size, long amount)`'))

    def test_fenced_examples_and_external_types_are_not_sdk_references(self):
        text = '```java\n`Imaginary.noSuchMember`\n```\n~~~java\n`Imaginary.other`\n~~~\n`ItemStack.EMPTY`'
        self.assertEqual([], self.references(text))
        self.assertEqual([], self.references('`energy(非 null)`'))

    def test_missing_signature_context_is_an_error(self):
        self.assertTrue(self.references('`unresolved(String name, long count)`'))

    def test_broken_anchor_and_stale_contents(self):
        errors = self.mutate(0, '[Limits](#limits)', '[Limits](#missing-limits)')
        self.assertTrue(any('broken anchor' in error for error in errors))
        self.assertTrue(any('contents' in error for error in errors))

    def test_broken_cross_file_anchor_is_detected(self):
        target = 'ADDON_API.zh-CN.md'
        # The live guide links this file; first prove a real heading is accepted.
        self.assertEqual([], self.mutate(0, target, target + '#概述'))
        errors = self.mutate(0, target, target + '#missing-boundary')
        self.assertTrue(any('cross-file anchor' in error for error in errors))

    def test_equal_heading_counts_do_not_hide_section_mismatch(self):
        errors = self.mutate(1, '### 共用上限', '### 临时标题')
        self.assertTrue(any('heading mismatch' in error for error in errors))

    def test_equal_table_counts_do_not_hide_row_drift(self):
        errors = self.mutate(1, '| --- | --- |\n', '| --- | --- |\n| 额外 | 一行 |\n')
        self.assertTrue(any('shape mismatch' in error for error in errors))

    def test_limit_drift_in_either_language_fails(self):
        for language in (0, 1):
            errors = self.mutate(language, '1,024,000,000', '1,024,000,001')
            self.assertTrue(any('limit capacity' in error for error in errors))

    def test_source_constant_drift_fails_without_changing_docs(self):
        index = dict(self.index)
        index['AddonLimits'] = dict(index['AddonLimits'])
        index['AddonLimits']['source'] = index['AddonLimits']['source'].replace('1_024_000_000', '1_024_000_001')
        with patch.object(docs, 'java_index', return_value=index):
            self.assertTrue(any('limit capacity' in e for e in docs.check(texts=self.texts)))

    def test_public_constant_aliases_resolve_to_shared_limit(self):
        self.assertEqual(1_024_000_000, docs.constant(self.index, 'FuelGenerator.MAX_TANK'))
        self.assertEqual(1_024_000_000, docs.constant(self.index, 'PartTanks.MAX_CAPACITY'))

    def test_negative_sentinel_and_coordinate_signs_are_checked(self):
        errors = self.mutate(0, '−16 to 16', '16 to 16')
        self.assertTrue(any('limit io-coordinate' in e for e in errors))
        errors = self.mutate(1, '或为 −1', '或为 1')
        self.assertTrue(any('limit repeat' in e for e in errors))

    def test_name_pattern_drift_fails(self):
        errors = self.mutate(0, '`[a-z][a-z0-9_]{1,63}`', '`[a-z][a-z0-9_]{1,64}`')
        self.assertTrue(any('name pattern addon-id' in e for e in errors))

    def test_duplicate_authoritative_limit_row_fails(self):
        errors = self.mutate(0, '<!-- limit:capacity -->', '<!-- limit:capacity -->\n| duplicate | <!-- limit:capacity -->')
        self.assertTrue(any('must have one marked row' in e for e in errors))

    def test_unrelated_method_calls_and_comments_cannot_satisfy_members(self):
        with tempfile.TemporaryDirectory() as directory:
            repo = Path(directory)
            source = repo / 'core/src/api/java/Example.java'
            source.parent.mkdir(parents=True)
            source.write_text('''public class Example {
                // public void imaginary(String name) {}
                private static final char BRACE = '{';
                public void real(String name) { imaginary(name); }
                public static class Builder {
                    public void onlyInBuilder(long count) {}
                }
            }''', encoding='utf-8')
            index = docs.java_index(repo)
            self.assertIn('real', index['Example']['names'])
            self.assertNotIn('imaginary', index['Example']['names'])
            self.assertNotIn('onlyInBuilder', index['Example']['names'])
            self.assertIn('onlyInBuilder', index['Example.Builder']['names'])

    def test_long_narrative_paragraphs_are_rejected(self):
        texts = list(self.texts)
        texts[0] += '\n\n' + 'word ' * 151
        self.assertTrue(any('150 words' in e for e in docs.check(texts=texts)))


if __name__ == '__main__':
    unittest.main()
