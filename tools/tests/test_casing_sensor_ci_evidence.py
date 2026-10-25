"""Require exact native namespace, template and every restored casing/sensor case."""
import importlib.util
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('report', ROOT / 'tools/integration/check_issue_gametest_report.py')
report = importlib.util.module_from_spec(spec)
spec.loader.exec_module(report)
NEW = report.CASING_CRAFTING_CASE_NAMESPACES | report.SENSOR_INTERFACE_CASE_NAMESPACES

class CasingSensorEvidenceTests(unittest.TestCase):
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
        self.assertEqual(len(NEW), 13)
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

    def test_workflow_enables_new_namespaces(self):
        workflow = (ROOT / '.github/workflows/issue-regressions.yml').read_text()
        for namespace in {v.split(':')[0] for v in NEW.values()}:
            self.assertIn(namespace, workflow)

if __name__ == '__main__': unittest.main()
