"""Failure cases for the fast gate: ensure a green receipt cannot hide bad inputs."""
import gzip
import hashlib
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'integration'))
from localization import (CONFIG_PATH, LANG_PATH, expected_chinese,
                          json_text, load_source, parse_patch, read_json, synchronize, format_arguments)
from check_source import boundary_errors, check, literal_language_calls
from verify_artifacts import is_test_entry


class SourcePolicyTests(unittest.TestCase):
    def setUp(self):
        workspace = Path(__file__).resolve().parents[2] / 'work'
        workspace.mkdir(exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(prefix='source-policy-', dir=workspace)
        self.repo = Path(self.temp.name)
        self.assertEqual(self.repo.resolve().parent, workspace.resolve())
        self.addCleanup(self.temp.cleanup)
        self.source = {'gt.name': ' 原文§a%s ', 'gt.empty': ''}
        self.english = {'item.gregtech.name': 'Name %s', 'tooltip.gregtech.port': 'Port-only %s'}
        self.aliases = {'item.gregtech.name': 'gt.name'}
        raw = 'S:gt.name= 原文§a%s \nS:gt.empty=\n'.encode('utf8')
        config = self.repo / CONFIG_PATH
        config.mkdir(parents=True)
        (config / 'GregTech_zh_cn.lang.gz').write_bytes(gzip.compress(raw, mtime=0))
        self.write_json(CONFIG_PATH / 'source.json', {'sha256': hashlib.sha256(raw).hexdigest(), 'keys': 2})
        self.write_json(CONFIG_PATH / 'aliases.json', self.aliases)
        self.write_json(CONFIG_PATH / 'english_source.json', {'values': {}})
        self.write_json(LANG_PATH / 'en_us.json', self.english)
        self.good = expected_chinese(self.english, self.source, self.aliases)
        self.write_json(LANG_PATH / 'zh_cn.json', self.good)

    def write_json(self, relative, value):
        file = self.repo / relative
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(json_text(value), encoding='utf8')
        return file

    def test_spaces_formatting_empty_values_and_english_fallback_are_preserved(self):
        self.assertEqual(synchronize(self.repo)['changes'], {'missing': 0, 'unexpected': 0, 'changed': 0})
        self.assertEqual(self.good['item.gregtech.name'], ' 原文§a%s ')
        self.assertEqual(self.good['gt.empty'], '')

    def test_modified_direct_key_alias_and_fallback_are_rejected(self):
        for key in ('gt.name', 'item.gregtech.name', 'tooltip.gregtech.port'):
            with self.subTest(key=key):
                self.write_json(LANG_PATH / 'zh_cn.json', {**self.good, key: '自行翻译'})
                with self.assertRaisesRegex(ValueError, 'Chinese differs'):
                    synchronize(self.repo)

    def test_missing_and_unbound_extra_keys_are_rejected(self):
        for data in ({k: v for k, v in self.good.items() if k != 'gt.name'},
                     {**self.good, 'item.gregtech.guessed': ' 原文§a%s '}):
            self.write_json(LANG_PATH / 'zh_cn.json', data)
            with self.assertRaises(ValueError):
                synchronize(self.repo)

    def test_unknown_alias_and_original_key_override_are_rejected(self):
        for bindings in ({'item.name': 'nonexistent'}, {'gt.empty': 'gt.name'}):
            with self.assertRaises(ValueError):
                expected_chinese(self.english, self.source, bindings)

    def test_chinese_cannot_be_hidden_in_english_fallback(self):
        with self.assertRaisesRegex(ValueError, 'bypasses'):
            expected_chinese({'item.guessed': '自行翻译'}, self.source, {})

    def test_duplicate_json_and_conflicting_lang_keys_fail(self):
        file = self.repo / LANG_PATH / 'zh_cn.json'
        file.write_text('{"a":"one","a":"two"}', encoding='utf8')
        with self.assertRaisesRegex(ValueError, 'Duplicate'):
            read_json(file)
        with self.assertRaisesRegex(ValueError, 'Conflicting'):
            parse_patch(b'S:a=one\nS:a=two')

    def test_altered_snapshot_cannot_be_trusted(self):
        file = self.repo / CONFIG_PATH / 'GregTech_zh_cn.lang.gz'
        file.write_bytes(gzip.compress(b'S:gt.name=changed\nS:gt.empty='))
        with self.assertRaisesRegex(ValueError, 'SHA-256'):
            load_source(self.repo)

    def test_wrong_external_source_is_rejected_without_writing(self):
        external = self.repo / 'wrong.lang'
        external.write_text('S:wrong=wrong', encoding='utf8')
        with self.assertRaisesRegex(ValueError, 'differs from the pinned source'):
            synchronize(self.repo, external=external)
        self.assertEqual(read_json(self.repo / LANG_PATH / 'zh_cn.json'), self.good)

    def test_main_test_import_and_shared_core_loader_import_fail(self):
        self.assertTrue(boundary_errors(Path('src/main/java/Foo.java'),
                                        'import net.minecraft.gametest.framework.GameTest;'))
        self.assertTrue(boundary_errors(Path('src/main/java/Foo.java'),
                                        '@net.minecraft.gametest.framework.GameTest public void test() {}'))
        self.assertTrue(boundary_errors(Path('src/main/java/Foo.java'),
                                        'class Foo { net.minecraft.gametest.framework.GameTestHelper helper; }'))
        self.assertTrue(boundary_errors(Path('core/src/main/java/Foo.java'),
                                        'import net.minecraft.world.level.Level;', core=True))
        self.assertFalse(boundary_errors(Path('core/src/main/java/Foo.java'),
                                         '// import net.minecraft.world.level.Level;', core=True))

    def test_fully_qualified_platform_types_cannot_bypass_core_boundary(self):
        path = Path('core/src/main/java/Foo.java')
        for source in ('class Foo { net.minecraft.world.level.Level level; }',
                       'class Foo { net /* separator */ .\n minecraft.world.level.Level level; }',
                       'class Foo { Object tag = net.neoforged.neoforge.common.Tags.class; }',
                       'class Foo { com.gregtech.gregtech.platform.Adapter adapter; }',
                       r'class Foo { n\u0065t.minecraft.world.level.Level level; }'):
            with self.subTest(source=source):
                self.assertTrue(boundary_errors(path, source, core=True))

    def test_string_contents_are_not_code_or_imports(self):
        source = '''// 中文说明中提到 import net.minecraft.Level;
class Foo {
    String example = """
        import net.minecraft.Level;
        @GameTest
        """;
    char quote = '"';
    String escaped = "a \\"quote\\"; net.minecraft.Level";
}
'''
        self.assertFalse(boundary_errors(Path('core/src/main/java/Foo.java'), source, core=True))

    def test_chinese_strings_characters_and_text_blocks_use_language_resources(self):
        path = Path('src/main/java/Foo.java')
        for source in ('String name = "自拟中文";', "char letter = '中';",
                       'String paragraph = """\n一段中文，内含 "引号"\n""";',
                       r'String name = "\u4e2d\u6587";',
                       r'String name = "\ud840\udc00";'):
            with self.subTest(source=source):
                errors = boundary_errors(path, source)
                self.assertEqual(len(errors), 1)
                self.assertIn('Chinese literal bypasses', errors[0])

    def test_comments_and_literal_backslash_unicode_are_not_translations(self):
        source = r'''// "中文注释" net.minecraft.Level
/* Another "中文注释" */
String encoded = "\\u4e2d";
\u002f\u002f "中文注释" import net.minecraft.Level;
'''
        self.assertFalse(boundary_errors(Path('core/src/main/java/Foo.java'), source, core=True))

    def test_chinese_literal_check_is_wired_into_full_source_gate(self):
        path = self.repo / 'core/src/main/java/Foo.java'
        path.parent.mkdir(parents=True)
        path.write_text('class Foo {\n String bad = "自拟中文";\n}', encoding='utf8')
        with self.assertRaisesRegex(ValueError, r'Chinese literal bypasses.*Foo.java:2'):
            check(self.repo)

    def test_shadow_language_copy_fails(self):
        self.write_json(Path('neoforge/src/main/resources/assets/gregtech/lang/zh_cn.json'), self.good)
        with self.assertRaisesRegex(ValueError, 'shadows'):
            check(self.repo)

    def test_english_source_drift_is_rejected_even_if_chinese_is_regenerated(self):
        self.write_json(CONFIG_PATH / 'english_source.json', {'values': {
            'item.gregtech.name': {'source_key': 'gt.name', 'value': 'Name %s'}}})
        self.write_json(LANG_PATH / 'en_us.json', {**self.english, 'item.gregtech.name': 'Invented name %s'})
        with self.assertRaisesRegex(ValueError, 'English differs'):
            synchronize(self.repo, write=True)

    def test_english_and_chinese_format_arguments_must_match(self):
        self.write_json(LANG_PATH / 'en_us.json', {**self.english, 'item.gregtech.name': 'Name %s %s'})
        with self.assertRaisesRegex(ValueError, 'format arguments differ'):
            synchronize(self.repo, write=True)
        self.assertEqual(format_arguments('%2$s %1$.2f / %2$s %%'), {1, 2})
        self.assertEqual(format_arguments('Efficiency: 50%'), set())
        for value in ('%0$s', '%s and 50%', '%q', '%s %3$z', '%05s', '%1$'):
            with self.subTest(value=value), self.assertRaises(ValueError):
                format_arguments(value)

    def test_literal_english_key_and_argument_checks_ignore_comments_and_examples(self):
        path = Path('src/main/java/Sample.java')
        en = {'gt.name': '%2$s then %1$s'}
        source = '''// Component.translatable("gt.missing");
String example = "Component.translatable(\\"gt.missing\\")";
Component.translatable("gt.name", nested("text, text"), 2);
Component.translatable("gt.name", 1);
Component.translatable("gt.missing");
Component.translatable("item.minecraft.stone");
Component.translatable("gt." + suffix);
'''
        issues, checked, dynamic = literal_language_calls(path, source, en)
        self.assertEqual(checked, 3)
        self.assertEqual(dynamic, 1)
        self.assertEqual(len(issues), 2)
        self.assertIn('Insufficient', issues[0])
        self.assertIn('Missing English', issues[1])

    def test_english_encoding_empty_names_and_supplementary_chinese_are_rejected(self):
        for value in ('\ufffd', '', '\u0001'):
            with self.subTest(value=value):
                self.write_json(LANG_PATH / 'en_us.json', {**self.english, 'item.gregtech.name': value})
                with self.assertRaises(ValueError): synchronize(self.repo, write=True)
        with self.assertRaisesRegex(ValueError, 'bypasses'):
            expected_chinese({'item.gregtech.name':'\U00020000'}, self.source, {})

    def test_test_resources_and_nested_classes_are_forbidden_in_production(self):
        stems = {'com/gregtech/gregtech/jei/JeiMachineIndexTests'}
        self.assertTrue(is_test_entry('com/gregtech/gregtech/jei/JeiMachineIndexTests$Inner.class', stems, set()))
        self.assertTrue(is_test_entry('com/gregtech/gregtech/gametest/NewTest.class', set(), set()))
        self.assertTrue(is_test_entry('data/gregtech_repair/structures/test_empty.nbt', set(), set()))
        self.assertFalse(is_test_entry('com/gregtech/gregtech/registry/GTBlocks.class', stems, set()))
