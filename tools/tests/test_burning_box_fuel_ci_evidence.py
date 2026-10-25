"""Require all issue #109 native fuel cases to execute with exact identities."""
import contextlib
import importlib.util
import io
from pathlib import Path
import re
import tempfile
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('issue_report', ROOT / 'tools/integration/check_issue_gametest_report.py')
report = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(report)
NAMESPACE = 'gt6ce_burning_box_fuels:test_empty'
METHODS = (
    'weakliquidandgasfuelssustainratednativeheatoutput',
    'originalengineliquidsarealsoavailabletoregisteredliquidburningboxes',
    'finalreservekeepsburninguntilanotherratedpacketcannotbeemitted',
    'roundedzeroheatrecipecannotconsumefuelorspinrefillloop',
    'originalcoalformsconsumeexactcalciteandretainheatandash',
    'legacyfluidbedtankcapacityrestoreswithoutlosingsavedcalcite',
    'fullashoutputblocksbothinputsuntilnativeextractionfreesspace',
    'fuelchargespreservestoredfluidamountsbeyondnativeintviews',
)
IDENTITIES = {'burningboxfuelregressiontests.' + method for method in METHODS}


class BurningBoxFuelEvidenceTests(unittest.TestCase):
    def fixture(self, platform):
        suite = ET.Element('testsuite', failures='0', errors='0', skipped='0')
        for identity in sorted(report.EXPECTED[platform]):
            ET.SubElement(suite, 'testcase', name=identity.split('.', 1)[1],
                          classname=report.expected_namespace(identity))
        return suite

    def verify(self, suite, platform):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'report.xml'
            ET.ElementTree(suite).write(path)
            with contextlib.redirect_stdout(io.StringIO()):
                report.check(path, platform)

    def test_all_eight_exact_cases_are_required_on_both_loaders(self):
        self.assertEqual(set(report.BURNING_BOX_FUEL_CASE_NAMESPACES), IDENTITIES)
        self.assertEqual(set(report.BURNING_BOX_FUEL_CASE_NAMESPACES.values()), {NAMESPACE})
        for platform in report.EXPECTED:
            self.assertTrue(IDENTITIES <= report.EXPECTED[platform])
            self.verify(self.fixture(platform), platform)

    def test_each_missing_failed_errored_or_skipped_case_is_rejected(self):
        for platform in report.EXPECTED:
            for method in METHODS:
                for outcome in ('missing', 'failure', 'error', 'skipped'):
                    with self.subTest(platform=platform, method=method, outcome=outcome):
                        suite = self.fixture(platform)
                        case = next(case for case in suite if case.get('name') == method)
                        if outcome == 'missing':
                            suite.remove(case)
                        else:
                            ET.SubElement(case, outcome, message='native fuel regression did not pass')
                        with self.assertRaises(ValueError):
                            self.verify(suite, platform)

    def test_wrong_template_or_qualified_names_cannot_impersonate_cases(self):
        for platform in report.EXPECTED:
            for method in METHODS:
                identity = 'burningboxfuelregressiontests.' + method
                for name, classname in (
                    (method, 'gregtech_issue_audit:test_empty'),
                    (method, 'gt6ce_burning_box_fuels:wrong_template'),
                    (method, 'foreign.burningboxfuelregressiontests'),
                    (identity, NAMESPACE),
                    ('foreign.' + identity, 'foreign:test_empty'),
                    (method + '_other', NAMESPACE),
                ):
                    with self.subTest(platform=platform, method=method, name=name, classname=classname):
                        suite = self.fixture(platform)
                        case = next(case for case in suite if case.get('name') == method)
                        case.set('name', name)
                        case.set('classname', classname)
                        with self.assertRaisesRegex(ValueError, 'did not execute'):
                            self.verify(suite, platform)

    def test_duplicate_case_cannot_replace_another_required_method(self):
        for platform in report.EXPECTED:
            for method in METHODS:
                with self.subTest(platform=platform, method=method):
                    suite = self.fixture(platform)
                    target = next(case for case in suite if case.get('name') == method)
                    other = next(case for case in suite if case.get('name') in METHODS and case is not target)
                    target.attrib.update(other.attrib)
                    with self.assertRaisesRegex(ValueError, 'did not execute'):
                        self.verify(suite, platform)

    def test_zero_discovery_and_old_cases_alone_are_rejected(self):
        for platform in report.EXPECTED:
            for suite in (ET.Element('testsuite'), self.fixture(platform)):
                for case in list(suite):
                    if case.get('classname') == NAMESPACE:
                        suite.remove(case)
                with self.subTest(platform=platform, count=len(suite)):
                    with self.assertRaisesRegex(ValueError, 'did not execute'):
                        self.verify(suite, platform)

    def test_ci_identities_match_actual_native_annotations_and_template(self):
        path = ROOT / 'multiversion/src/bootstrapGameTest/java/com/gregtech/gregtech/gametest/BurningBoxFuelRegressionTests.java'
        source = path.read_text(encoding='utf-8')
        self.assertIn('@GameTestHolder("gt6ce_burning_box_fuels")', source)
        self.assertIn('@PrefixGameTestTemplate(false)', source)
        self.assertRegex(source, r'private static final String EMPTY\s*=\s*"test_empty";')
        annotated = re.findall(r'@GameTest\(([^)]*)\)\s*public static void (\w+)\(GameTestHelper \w+\)', source)
        self.assertEqual({name.lower() for _, name in annotated}, set(METHODS))
        self.assertEqual(len(annotated), len(METHODS))
        for annotation, _ in annotated:
            self.assertRegex(annotation, r'template\s*=\s*EMPTY\b')

    def test_workflow_selects_native_namespace_and_runs_checker_regressions(self):
        workflow = (ROOT / '.github/workflows/issue-regressions.yml').read_text(encoding='utf-8')
        namespaces = workflow.split('-PgameTestNamespaces=', 1)[1].split()[0].split(',')
        self.assertIn('gt6ce_burning_box_fuels', namespaces)
        self.assertIn('tools.tests.test_burning_box_fuel_ci_evidence', workflow)


if __name__ == '__main__':
    unittest.main()
