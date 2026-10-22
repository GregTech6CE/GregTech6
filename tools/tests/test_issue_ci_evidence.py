"""Focused regression evidence must reject omissions and real gameplay failures."""
from pathlib import Path
import sys
import json
import zipfile
from unittest.mock import patch
import hashlib
import tempfile
import unittest
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'integration'))
from check_issue_gametest_report import check, EXPECTED
import verify_artifacts
from verify_artifacts import required_forge_mixin_mappings


class IssueEvidenceTests(unittest.TestCase):
    def report(self, platform, namespace='gregtech_issue_audit:test_empty', failure=False):
        root = ET.Element('testsuite')
        suite = ET.SubElement(root, 'testsuite')
        for identity in EXPECTED[platform]:
            case = ET.SubElement(suite, 'testcase', classname=namespace, name=identity.split('.', 1)[1])
        if failure:
            ET.SubElement(case, 'failure', message='real gameplay failure')
        return root

    def verify(self, root, platform):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'report.xml'
            ET.ElementTree(root).write(path)
            check(path, platform)

    def test_real_mojang_template_classname_and_bare_method_format(self):
        for platform in EXPECTED:
            with self.subTest(platform=platform):
                self.verify(self.report(platform), platform)

    def test_other_namespace_cannot_impersonate_selected_test(self):
        with self.assertRaisesRegex(ValueError, 'did not execute'):
            self.verify(self.report('neoforge', namespace='gt6ce:test_empty'), 'neoforge')

    def test_real_failure_is_not_masked_by_correct_case_names(self):
        with self.assertRaisesRegex(ValueError, 'failure/error/skipped'):
            self.verify(self.report('forge', failure=True), 'forge')

    def test_empty_report_rejected(self):
        with self.assertRaisesRegex(ValueError, 'did not execute'):
            self.verify(ET.Element('testsuite'), 'neoforge')

    def test_only_exact_loader_api_mixin_exemption(self):
        prefix = 'com/gregtech/gregtech/mixin/'
        config = {'package': 'com.gregtech.gregtech.mixin',
                  'mixins': ['GasImmersionMixin', 'EntityFallingOreDropMixin', 'NewEntityMixin'],
                  'client': ['BlockPaintColorMixin']}
        self.assertEqual(required_forge_mixin_mappings(config), {
            prefix + name for name in ('EntityFallingOreDropMixin', 'NewEntityMixin', 'BlockPaintColorMixin')})
        config['package'] = 'other.mixin'
        self.assertIn('other/mixin/GasImmersionMixin', required_forge_mixin_mappings(config))

    def test_full_forge_inspect_emits_mixin_report(self):
        # Exercise the actual archive inspection/report return path. Compiler paths
        # and descriptor parsing are fixture boundaries; Mixin validation is real.
        repo = Path(__file__).resolve().parents[2]
        config = {'package': 'com.gregtech.gregtech.mixin',
                  'mixins': ['GasImmersionMixin', 'EntityFallingOreDropMixin'],
                  'refmap': 'gregtech.refmap.json'}
        mappings = {'com/gregtech/gregtech/mixin/EntityFallingOreDropMixin': {
            'spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;':
            'Lnet/minecraft/world/entity/Entity;m_19998_(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;'}}
        refmap = json.dumps({'mappings': mappings, 'data': {'searge': mappings}}).encode()
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            current = root / 'tmp/compileJava/gregtech.refmap.json'
            current.parent.mkdir(parents=True)
            current.write_bytes(refmap)
            jar = root / 'fixture.jar'
            with zipfile.ZipFile(jar, 'w') as archive:
                for name in verify_artifacts.GAMEPLAY_CLASSES + verify_artifacts.PLATFORM_GAMEPLAY_CLASSES['forge']:
                    archive.writestr(name, b'fixture class')
                archive.writestr('META-INF/mods.toml', b'fixture descriptor')
                archive.writestr('META-INF/MANIFEST.MF', b'MixinConfigs: gregtech.mixins.json\n')
                archive.writestr('gregtech.mixins.json', json.dumps(config))
                archive.writestr('gregtech.refmap.json', refmap)
                for locale in ('en_us', 'zh_cn'):
                    name = f'assets/gt6ce/lang/{locale}.json'
                    archive.writestr(name, (repo / 'core/src/main/resources' / name).read_bytes())
            with patch.object(verify_artifacts, 'current_platform_hashes', return_value={}), \
                 patch.object(verify_artifacts, 'check_metadata', return_value={'fixture': True}), \
                 patch.object(verify_artifacts, 'platform_build', return_value=root):
                report = verify_artifacts.inspect(jar, 'forge', {}, {'mod_license': 'fixture'}, (set(), set()))
            result = report['mixin_verification']
            self.assertEqual(result['mapped_classes'], 1)
            self.assertEqual(result['sha256'], hashlib.sha256(refmap).hexdigest())
            self.assertEqual(result['unmapped_forge_api_mixins'], sorted(verify_artifacts.UNMAPPED_FORGE_API_MIXINS))
            self.assertTrue(report['zip_crc_checked'])

    def test_gas_exemption_stays_loader_only(self):
        source = (Path(__file__).resolve().parents[2] / 'multiversion/src/main/java/com/gregtech/gregtech/mixin/GasImmersionMixin.java').read_text()
        self.assertEqual(source.count('@Inject('), 1)
        self.assertIn('method = "setFluidTypeHeight"', source)
        self.assertIn('remap = false', source)


if __name__ == '__main__':
    unittest.main()
