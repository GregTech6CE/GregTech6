"""Round-trip the native generator boundary without changing project sources."""
from pathlib import Path
import json
import tempfile
import unittest
from tools.native_layout import NODES, prepare, changes, reconcile, clean_java, native_path


class NativeLayoutTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='gt6-native-')
        self.addCleanup(self.temporary.cleanup)
        self.repo = Path(self.temporary.name)
        (self.repo / 'multiversion').mkdir()
        (self.repo / 'multiversion/migration-manifest.json').write_text('{}')

    def output(self, platform, kind, name, value):
        path = self.repo / f'multiversion/versions/{NODES[platform]}/build/generated/stonecutter/main/{kind}/{name}'
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(value, encoding='utf-8')
        return path

    def seed(self):
        for p in NODES:
            self.output(p, 'java', 'Sample.java', '/** Original author notice. */\nclass Sample { int value = 1; }\n')

    def test_clean_generated_branches_preserve_active_comments_and_literals(self):
        source = '//? if >=1.21.1 {\n/*int value=2; /^ License ^/ */\n//?} else {\n/** License */ int value=1; String text="//?}";\n//?}\n'
        cleaned = clean_java(source, 'forge')
        self.assertIn('/** License */', cleaned)
        self.assertIn('String text="//?}";', cleaned)
        self.assertNotIn('value=2', cleaned)
        with self.assertRaises(ValueError): clean_java('//? if >=1.22 {\n', 'forge')

    def test_generator_change_to_one_version_becomes_condition_in_common_file(self):
        self.seed()
        view = prepare(self.repo)
        (view / 'neoforge/src/main/java/Sample.java').write_text('/** Original author notice. */\nclass Sample { int value = 7; }\n')
        self.assertEqual(reconcile(self.repo), (1, 1))
        managed = (self.repo / 'multiversion/src/main/java/Sample.java').read_text()
        self.assertIn('if >=1.21.1', managed)
        self.assertIn('value = 7', managed)
        self.assertIn('value = 1', managed)
        self.assertIn('Original author notice.', managed)

    def test_loader_only_resource_difference_deduplicates_without_override(self):
        self.seed()
        for platform in NODES:
            self.output(platform, 'resources', 'model.json', json.dumps({'loader': platform + ':composite', 'elements': []}))
        view = prepare(self.repo)
        for platform in NODES:
            path = view / f'{platform}/src/main/resources/model.json'
            data = json.loads(path.read_text()); data['new'] = True
            path.write_text(json.dumps(data))
        self.assertEqual(reconcile(self.repo), (2, 1))
        self.assertEqual(json.loads((self.repo / 'multiversion/src/main/resources/model.json').read_text())['loader'], 'forge:composite')
        self.assertFalse(any((self.repo / f'multiversion/versions/{n}/src').exists() for n in NODES.values()))

    def test_read_only_and_legacy_fixture_paths(self):
        self.seed()
        prepare(self.repo)
        self.assertFalse(changes(self.repo))
        self.assertEqual(reconcile(self.repo), (0, 0))
        self.assertEqual(native_path(self.repo / 'src/main/java'), self.repo / 'build/native-tools/forge/src/main/java')
        foreign = self.repo / 'reference/src/main/java'
        self.assertEqual(native_path(foreign), foreign)

    def test_isolated_api_source_set_is_projected_and_reconciled(self):
        self.seed()
        for platform, node in NODES.items():
            api = self.repo / f'multiversion/versions/{node}/build/generated/stonecutter/api/java/PublicApi.java'
            api.parent.mkdir(parents=True, exist_ok=True)
            api.write_text('public interface PublicApi { int version(); }\n')
        view = prepare(self.repo)
        self.assertEqual(native_path(self.repo/'src/api/java'), view/'forge/src/api/java')
        for platform in NODES:
            (view/f'{platform}/src/api/java/PublicApi.java').write_text(
                'public interface PublicApi { int version(); String name(); }\n')
        self.assertEqual(reconcile(self.repo), (2, 1))
        self.assertIn('String name()', (self.repo/'multiversion/src/api/java/PublicApi.java').read_text())


if __name__ == '__main__': unittest.main()
