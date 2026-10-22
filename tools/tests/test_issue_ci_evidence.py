"""Focused regression evidence must reject omissions and real gameplay failures."""
from pathlib import Path
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'integration'))
from check_issue_gametest_report import check, EXPECTED
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

    def test_gas_exemption_stays_loader_only(self):
        source = (Path(__file__).resolve().parents[2] / 'multiversion/src/main/java/com/gregtech/gregtech/mixin/GasImmersionMixin.java').read_text()
        self.assertEqual(source.count('@Inject('), 1)
        self.assertIn('method = "setFluidTypeHeight"', source)
        self.assertIn('remap = false', source)


if __name__ == '__main__':
    unittest.main()
