"""Require exact source-form output lifecycle identities and namespace."""
import importlib.util
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('report', ROOT / 'tools/integration/check_issue_gametest_report.py')
report = importlib.util.module_from_spec(spec)
spec.loader.exec_module(report)
NEW = report.ORE_FORM_OUTPUT_CASE_NAMESPACES

class OreFormOutputEvidenceTests(unittest.TestCase):
    def fixture(self, platform):
        suite = ET.Element('testsuite', failures='0', errors='0', skipped='0')
        for identity in sorted(report.EXPECTED[platform]):
            ET.SubElement(suite, 'testcase', name=identity.split('.', 1)[1], classname=report.expected_namespace(identity))
        return suite

    def check(self, suite, platform):
        with tempfile.TemporaryDirectory() as d:
            path = Path(d) / 'report.xml'
            ET.ElementTree(suite).write(path)
            return report.check(path, platform)

    def test_full_exact_reports_pass(self):
        self.assertEqual(len(NEW), 8)
        for platform in report.EXPECTED:
            self.check(self.fixture(platform), platform)

    def test_every_new_case_is_mandatory_on_both_loaders(self):
        for platform in report.EXPECTED:
            for identity in NEW:
                with self.subTest(platform=platform, identity=identity):
                    suite = self.fixture(platform)
                    target = next(c for c in suite if c.get('name') == identity.split('.', 1)[1])
                    suite.remove(target)
                    with self.assertRaises(ValueError): self.check(suite, platform)

    def test_wrong_template_and_qualified_identity_do_not_bypass_namespace(self):
        for platform in report.EXPECTED:
            for identity in NEW:
                for qualified in (False, True):
                    with self.subTest(platform=platform, identity=identity, qualified=qualified):
                        suite = self.fixture(platform)
                        case = next(c for c in suite if c.get('name') == identity.split('.', 1)[1])
                        case.set('classname', 'wrong_namespace:test_empty')
                        if qualified: case.set('name', identity)
                        with self.assertRaises(ValueError): self.check(suite, platform)

    def test_source_annotations_match_every_exact_case(self):
        import re
        for identity, template in NEW.items():
            test_class, method = identity.split('.', 1)
            matches = [p for p in (ROOT / 'multiversion/src/bootstrapGameTest/java/com/gregtech/gregtech/gametest').glob('*.java') if p.stem.lower() == test_class]
            self.assertEqual(len(matches), 1, identity)
            source = matches[0].read_text()
            self.assertIn('GameTestHolder("' + template.split(':')[0] + '")', source)
            self.assertIn('PrefixGameTestTemplate(false)', source)
            annotated = re.findall(r'@GameTest\(([^)]*)\)\s*public static void (\w+)\(GameTestHelper \w+\)', source)
            selected = [a for a, name in annotated if name.lower() == method]
            self.assertEqual(len(selected), 1, identity)
            self.assertRegex(selected[0], r'template\s*=\s*"' + template.split(':')[1] + '"')

    def test_workflow_enables_new_namespaces(self):
        workflow = (ROOT / '.github/workflows/issue-regressions.yml').read_text()
        for namespace in {v.split(':')[0] for v in NEW.values()}:
            self.assertIn(namespace, workflow)

if __name__ == '__main__': unittest.main()
