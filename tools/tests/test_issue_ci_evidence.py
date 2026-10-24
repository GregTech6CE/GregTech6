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
from check_issue_gametest_report import check, EXPECTED, LEGACY_CASE_NAMESPACES, API_CONTRACT_CASES, ENERGY_STORAGE_CASE_NAMESPACES, COVER_REMOVAL_CASE_NAMESPACES, ORE_SOURCE_CASES, ORE_FOLLOWON_CASE_NAMESPACES, MATERIAL_PHASE_CASE_NAMESPACES, POSITIVE_ORE_CENTRIFUGE_CASE_NAMESPACES, NEXT_ORE_CENTRIFUGE_CASE_NAMESPACES, QUALITY_PHYSICS_CASE_NAMESPACES, expected_namespace
import verify_artifacts
from verify_artifacts import required_forge_mixin_mappings


LU_INTERACTION_CASES = {
    'crystalchargerinteractiontests.nativechargerinsertionkeepsenergyandinteractionguards',
    'crystalchargerinteractiontests.nativecrystalinsertionusesluandpreservescharge',
}


# Add later native material-contract regressions here only after their identity is confirmed.
MATERIAL_PHASE_CASES = (
    ('materialdecompositionpolicytests.decompositionusesfluidinputsambientphasesandoriginalwork',
     'gregtech_issue_audit:test_empty'),
    ('smeltingtargetremovaltests.removedsmeltingtargetretainsconsumeddustacrosshotphase',
     'gregtech_issue_audit:crucible_removal_empty'),
)


POSITIVE_ORE_CENTRIFUGE_CASES = (
    'positiveorecentrifugetests.copperpurifiedwholehasthreesourcebyproducts',
    'positiveorecentrifugetests.copperpurifiedtinykeepssourcegradeandwork',
    'positiveorecentrifugetests.galenapurifiedwholehasthreesourcebyproducts',
    'positiveorecentrifugetests.galenapurifiedtinykeepssourcegradeandwork',
    'positiveorecentrifugetests.netherquartzpurifiedwholeusesqualitythreework',
    'positiveorecentrifugetests.netherquartzpurifiedtinykeepssourcegradeandwork',
)


NEXT_ORE_CENTRIFUGE_CASES = (
    'solidorecentrifugetests.chalcopyritepurifiedwhole',
    'solidorecentrifugetests.chalcopyritepurifiedtiny',
    'solidorecentrifugetests.pentlanditepurifiedwhole',
    'solidorecentrifugetests.pentlanditepurifiedtiny',
    'solidorecentrifugetests.sphaleritepurifiedwhole',
    'solidorecentrifugetests.sphaleritepurifiedtiny',
    'solidorecentrifugetests.tetrahedritepurifiedwhole',
    'solidorecentrifugetests.tetrahedritepurifiedtiny',
    'solidorecentrifugetests.pyritepurifiedwhole',
    'solidorecentrifugetests.pyritepurifiedtiny',
    'solidorecentrifugetests.nickelpurifiedwhole',
    'solidorecentrifugetests.nickelpurifiedtiny',
    'solidorecentrifugetests.cooperitepurifiedwhole',
    'solidorecentrifugetests.cooperitepurifiedtiny',
    'solidorecentrifugetests.yellowlimonitepurifiedwhole',
    'solidorecentrifugetests.yellowlimonitepurifiedtiny',
)


class IssueEvidenceTests(unittest.TestCase):
    def report(self, platform, namespace=None, failure=False):
        root = ET.Element('testsuite')
        suite = ET.SubElement(root, 'testsuite')
        for identity in EXPECTED[platform]:
            case = ET.SubElement(suite, 'testcase', classname=namespace or expected_namespace(identity), name=identity.split('.', 1)[1])
        if failure:
            ET.SubElement(case, 'failure', message='real gameplay failure')
        return root

    def verify(self, root, platform):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'report.xml'
            ET.ElementTree(root).write(path)
            check(path, platform)

    def test_owned_builder_cases_require_exact_dedicated_template(self):
        identities = {
            'toolplacementtransactiontests.builderusesheadqualityandhitcoordinates',
            'toolplacementtransactiontests.buildercancellationrollsbackworldstockandwear',
        }
        namespace = 'gregtech_issue_audit:builder_placement_empty'
        for platform in EXPECTED:
            self.assertTrue(identities <= EXPECTED[platform])
            self.verify(self.report(platform), platform)
            for identity in identities:
                self.assertEqual(namespace, expected_namespace(identity))
                for wrong in ('gregtech_issue_audit:test_empty', 'gregtech_issue_audit:torch_placement_empty',
                              'gt6ce:builder_placement_empty'):
                    root = self.report(platform)
                    case = next(c for c in root.iter('testcase') if c.get('name') == identity.split('.', 1)[1])
                    case.set('classname', wrong)
                    with self.subTest(platform=platform, identity=identity, namespace=wrong):
                        with self.assertRaisesRegex(ValueError, 'did not execute'):
                            self.verify(root, platform)
        self.assertEqual('gregtech_issue_audit:test_empty',
                         expected_namespace('toolplacementtransactiontests.buildersixfacescommitexactlyonce'))
        self.assertEqual('gregtech_issue_audit:torch_placement_empty',
                         expected_namespace('toolplacementtransactiontests.drilltorchcancellationandcreativepreserveinventory'))

    def test_owned_builder_cases_cannot_be_missing_or_fail(self):
        identities = {
            'toolplacementtransactiontests.builderusesheadqualityandhitcoordinates',
            'toolplacementtransactiontests.buildercancellationrollsbackworldstockandwear',
        }
        for platform in EXPECTED:
            for identity in identities:
                for outcome in ('missing', 'failure', 'error', 'skipped'):
                    root = self.report(platform)
                    suite = root.find('testsuite')
                    case = next(c for c in suite if c.get('name') == identity.split('.', 1)[1])
                    if outcome == 'missing':
                        suite.remove(case)
                    else:
                        ET.SubElement(case, outcome, message='real builder placement outcome')
                    with self.subTest(platform=platform, identity=identity, outcome=outcome):
                        with self.assertRaises(ValueError):
                            self.verify(root, platform)

    def test_cover_removal_requires_exact_dedicated_template(self):
        self.assertEqual(len(COVER_REMOVAL_CASE_NAMESPACES), 3)
        for platform in EXPECTED:
            self.assertTrue(set(COVER_REMOVAL_CASE_NAMESPACES) <= EXPECTED[platform])
            self.verify(self.report(platform), platform)
            for identity in COVER_REMOVAL_CASE_NAMESPACES:
                for wrong in ('gregtech_issue_audit:test_empty', 'gt6ce:cover_removal_empty'):
                    root = self.report(platform)
                    case = next(c for c in root.iter('testcase') if c.get('name') == identity.split('.', 1)[1])
                    case.set('classname', wrong)
                    with self.subTest(platform=platform, identity=identity, wrong=wrong), self.assertRaisesRegex(ValueError, 'did not execute'):
                        self.verify(root, platform)

    def test_each_cover_removal_case_and_real_outcome_remains_required(self):
        for platform in EXPECTED:
            for identity in COVER_REMOVAL_CASE_NAMESPACES:
                for outcome in ('missing', 'failure', 'error', 'skipped'):
                    root = self.report(platform)
                    suite = root.find('testsuite')
                    case = next(c for c in suite if c.get('name') == identity.split('.', 1)[1])
                    if outcome == 'missing':
                        suite.remove(case)
                    else:
                        ET.SubElement(case, outcome, message='real result')
                    with self.subTest(platform=platform, identity=identity, outcome=outcome), self.assertRaises(ValueError):
                        self.verify(root, platform)

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

    def test_behavior_fixture_mod_is_enabled_in_native_run(self):
        repo = Path(__file__).resolve().parents[2]
        workflow = (repo / '.github/workflows/issue-regressions.yml').read_text()
        self.assertIn('-PaddonSample=true', workflow)
        self.assertIn('-PgameTestNamespaces=gregtech_issue_audit,gregtech_cover_issues,gregtech_pipe_auto,gt6ce_api_contracts,gt6ce_crystal_barrel,gt6ce_diesel_repair,gt6ce_legacy_energy,gt6ce_machine_performance', workflow)
        # The fixture deliberately depends on a registered SDK consumer, not a synthetic block.
        fixture = (repo / 'multiversion/src/bootstrapGameTest/java/com/gregtech/gregtech/gametest/BehaviorStructureSafetyTests.java').read_text()
        self.assertIn('"addon_sample", "irrigator"', fixture)

    def test_each_legacy_namespace_must_execute(self):
        for namespace in set(LEGACY_CASE_NAMESPACES.values()):
            root = self.report('forge')
            suite = root.find('testsuite')
            for case in list(suite):
                if case.get('classname') == namespace:
                    suite.remove(case)
            with self.subTest(namespace=namespace), self.assertRaisesRegex(ValueError, 'did not execute'):
                self.verify(root, 'forge')

    def test_issue_namespace_cannot_impersonate_legacy_methods(self):
        with self.assertRaisesRegex(ValueError, 'did not execute'):
            self.verify(self.report('forge', namespace='gregtech_issue_audit:test_empty'), 'forge')

    def test_each_addon_api_case_must_execute_on_both_loaders(self):
        for platform in EXPECTED:
            for identity in API_CONTRACT_CASES:
                root = self.report(platform)
                suite = root.find('testsuite')
                for case in list(suite):
                    if (case.get('classname') == expected_namespace(identity)
                            and case.get('name') == identity.split('.', 1)[1]):
                        suite.remove(case)
                with self.subTest(platform=platform, identity=identity), self.assertRaisesRegex(ValueError, 'did not execute'):
                    self.verify(root, platform)

    def test_old_namespaces_alone_cannot_pass_the_extended_gate(self):
        for platform in EXPECTED:
            root = self.report(platform)
            suite = root.find('testsuite')
            for case in list(suite):
                if case.get('classname') == 'gt6ce_api_contracts:test_empty':
                    suite.remove(case)
            with self.subTest(platform=platform), self.assertRaisesRegex(ValueError, 'did not execute'):
                self.verify(root, platform)

    def test_issue_namespace_cannot_impersonate_addon_api_methods(self):
        for platform in EXPECTED:
            root = self.report(platform)
            for case in root.iter('testcase'):
                if case.get('classname') == 'gt6ce_api_contracts:test_empty':
                    case.set('classname', 'gregtech_issue_audit:test_empty')
            with self.subTest(platform=platform), self.assertRaisesRegex(ValueError, 'did not execute'):
                self.verify(root, platform)

    def test_addon_api_failures_errors_and_skips_are_not_accepted(self):
        for platform in EXPECTED:
            for identity in API_CONTRACT_CASES:
                for result in ('failure', 'error', 'skipped'):
                    root = self.report(platform)
                    case = next(case for case in root.iter('testcase')
                                if case.get('classname') == expected_namespace(identity)
                                and case.get('name') == identity.split('.', 1)[1])
                    ET.SubElement(case, result, message='addon API contract did not pass')
                    with self.subTest(platform=platform, identity=identity, result=result), self.assertRaisesRegex(ValueError, 'failure/error/skipped'):
                        self.verify(root, platform)

    def test_each_energy_storage_case_must_execute_on_both_loaders(self):
        for platform in EXPECTED:
            for identity in ENERGY_STORAGE_CASE_NAMESPACES:
                root = self.report(platform)
                suite = root.find('testsuite')
                for case in list(suite):
                    if (case.get('classname') == expected_namespace(identity)
                            and case.get('name') == identity.split('.', 1)[1]):
                        suite.remove(case)
                with self.subTest(platform=platform, identity=identity), self.assertRaisesRegex(ValueError, 'did not execute'):
                    self.verify(root, platform)

    def test_each_energy_storage_namespace_must_execute_on_both_loaders(self):
        for platform in EXPECTED:
            for namespace in set(ENERGY_STORAGE_CASE_NAMESPACES.values()):
                root = self.report(platform)
                suite = root.find('testsuite')
                for case in list(suite):
                    if case.get('classname') == namespace:
                        suite.remove(case)
                with self.subTest(platform=platform, namespace=namespace), self.assertRaisesRegex(ValueError, 'did not execute'):
                    self.verify(root, platform)

    def test_issue_namespace_cannot_impersonate_energy_storage_methods(self):
        for platform in EXPECTED:
            root = self.report(platform)
            for case in root.iter('testcase'):
                if case.get('classname') in ENERGY_STORAGE_CASE_NAMESPACES.values():
                    case.set('classname', 'gregtech_issue_audit:test_empty')
            with self.subTest(platform=platform), self.assertRaisesRegex(ValueError, 'did not execute'):
                self.verify(root, platform)

    def test_energy_storage_failures_errors_and_skips_are_not_accepted(self):
        for platform in EXPECTED:
            for identity in ENERGY_STORAGE_CASE_NAMESPACES:
                for result in ('failure', 'error', 'skipped'):
                    root = self.report(platform)
                    case = next(case for case in root.iter('testcase')
                                if case.get('classname') == expected_namespace(identity)
                                and case.get('name') == identity.split('.', 1)[1])
                    ET.SubElement(case, result, message='energy/storage contract did not pass')
                    with self.subTest(platform=platform, identity=identity, result=result), self.assertRaisesRegex(ValueError, 'failure/error/skipped'):
                        self.verify(root, platform)

    def test_each_native_lu_interaction_case_must_execute_on_both_loaders(self):
        for platform in EXPECTED:
            self.assertTrue(LU_INTERACTION_CASES <= EXPECTED[platform])
            for identity in LU_INTERACTION_CASES:
                root = self.report(platform)
                suite = root.find('testsuite')
                for case in list(suite):
                    if (case.get('classname') == expected_namespace(identity)
                            and case.get('name') == identity.split('.', 1)[1]):
                        suite.remove(case)
                with self.subTest(platform=platform, identity=identity), self.assertRaisesRegex(ValueError, 'did not execute'):
                    self.verify(root, platform)

    def test_other_namespace_cannot_impersonate_native_lu_interaction(self):
        for platform in EXPECTED:
            for identity in LU_INTERACTION_CASES:
                root = self.report(platform)
                case = next(case for case in root.iter('testcase')
                            if case.get('classname') == expected_namespace(identity)
                            and case.get('name') == identity.split('.', 1)[1])
                case.set('classname', 'gt6ce_crystal_barrel:test_empty')
                with self.subTest(platform=platform, identity=identity), self.assertRaisesRegex(ValueError, 'did not execute'):
                    self.verify(root, platform)

    def test_native_lu_interaction_failures_errors_and_skips_are_not_accepted(self):
        for platform in EXPECTED:
            for identity in LU_INTERACTION_CASES:
                for result in ('failure', 'error', 'skipped'):
                    root = self.report(platform)
                    case = next(case for case in root.iter('testcase')
                                if case.get('classname') == expected_namespace(identity)
                                and case.get('name') == identity.split('.', 1)[1])
                    ET.SubElement(case, result, message='native LU block interaction did not pass')
                    with self.subTest(platform=platform, identity=identity, result=result), self.assertRaisesRegex(ValueError, 'failure/error/skipped'):
                        self.verify(root, platform)

    def test_all_27_ore_cases_required_on_both_loaders(self):
        self.assertEqual(len(ORE_SOURCE_CASES), 27)
        for platform in EXPECTED:
            for identity in ORE_SOURCE_CASES:
                root = self.report(platform)
                suite = root.find('testsuite')
                case = next(c for c in suite if c.get('classname') == expected_namespace(identity)
                            and c.get('name') == identity.split('.', 1)[1])
                suite.remove(case)
                with self.subTest(platform=platform, identity=identity), self.assertRaisesRegex(ValueError, 'did not execute'):
                    self.verify(root, platform)

    def test_ore_namespace_and_failure_cannot_be_masked(self):
        repo = Path(__file__).resolve().parents[2]
        self.assertIn('gt6ce_machine_performance,gregtech_ore_source_audit',
                      (repo / '.github/workflows/issue-regressions.yml').read_text())
        # Several audit classes intentionally share this template namespace.
        # Select each required identity, never the first namespace match from a set.
        for platform in EXPECTED:
            for identity in sorted(ORE_SOURCE_CASES):
                for result in ('wrong_namespace', 'qualified_method_wrong_namespace', 'class_suffix_wrong_namespace', 'failure', 'error', 'skipped'):
                    root = self.report(platform)
                    case = next(c for c in root.iter('testcase')
                                if c.get('name') == identity.split('.', 1)[1]
                                and c.get('classname') == expected_namespace(identity))
                    if result == 'wrong_namespace':
                        case.set('classname', 'gregtech_issue_audit:test_empty')
                    elif result == 'qualified_method_wrong_namespace':
                        case.set('name', identity)
                        case.set('classname', 'gregtech_issue_audit:test_empty')
                    elif result == 'class_suffix_wrong_namespace':
                        case.set('classname', 'wrong.' + identity.split('.', 1)[0])
                    else:
                        ET.SubElement(case, result, message='ore source contract did not pass')
                    with self.subTest(platform=platform, identity=identity, result=result), self.assertRaises(ValueError):
                        self.verify(root, platform)

    def test_thermal_and_rawcraft_followon_exact_cases_and_namespaces(self):
        self.assertEqual(len(ORE_FOLLOWON_CASE_NAMESPACES), 10)
        self.assertEqual(sum(v == 'gregtech_ore_thermal_audit:test_empty' for v in ORE_FOLLOWON_CASE_NAMESPACES.values()), 7)
        repo = Path(__file__).resolve().parents[2]
        self.assertIn('gregtech_ore_source_audit,gregtech_ore_thermal_audit',
                      (repo / '.github/workflows/issue-regressions.yml').read_text())
        for platform in EXPECTED:
            for identity in ORE_FOLLOWON_CASE_NAMESPACES:
                self.assertIn(identity, EXPECTED[platform])
                for outcome in ('missing', 'namespace', 'qualified_wrong_namespace', 'class_suffix_wrong_namespace', 'failure', 'error', 'skipped'):
                    root = self.report(platform)
                    suite = root.find('testsuite')
                    case = next(c for c in suite if c.get('name') == identity.split('.', 1)[1]
                                and c.get('classname') == expected_namespace(identity))
                    if outcome == 'missing':
                        suite.remove(case)
                    elif outcome == 'namespace':
                        case.set('classname', 'gregtech_issue_audit:test_empty')
                    elif outcome == 'qualified_wrong_namespace':
                        case.set('name', identity)
                        case.set('classname', 'gregtech_issue_audit:test_empty')
                    elif outcome == 'class_suffix_wrong_namespace':
                        case.set('classname', 'wrong.' + identity.split('.', 1)[0])
                    else:
                        ET.SubElement(case, outcome, message='real source contract outcome')
                    with self.subTest(platform=platform, identity=identity, outcome=outcome), self.assertRaises(ValueError):
                        self.verify(root, platform)

    def test_material_phase_cases_require_exact_identity_and_pass_on_both_loaders(self):
        for platform in ('forge', 'neoforge'):
            self.verify(self.report(platform), platform)
            for identity, namespace in MATERIAL_PHASE_CASES:
                self.assertIn(identity, EXPECTED[platform])
                self.assertEqual(MATERIAL_PHASE_CASE_NAMESPACES[identity], namespace)
                self.assertEqual(expected_namespace(identity), namespace)
                for outcome in ('missing', 'failure', 'error', 'skipped', 'wrong_namespace', 'wrong_method',
                                'qualified_wrong_namespace', 'class_suffix_wrong_namespace'):
                    root = self.report(platform)
                    suite = root.find('testsuite')
                    case = next(c for c in suite
                                if c.get('name') == identity.split('.', 1)[1]
                                and c.get('classname') == expected_namespace(identity))
                    if outcome == 'missing':
                        suite.remove(case)
                    elif outcome == 'wrong_namespace':
                        case.set('classname', 'gt6ce:test_empty')
                    elif outcome == 'wrong_method':
                        case.set('name', 'unrelatedmaterialphasetest')
                    elif outcome == 'qualified_wrong_namespace':
                        case.set('name', identity)
                        case.set('classname', 'gt6ce:test_empty')
                    elif outcome == 'class_suffix_wrong_namespace':
                        case.set('classname', 'wrong.' + identity.split('.', 1)[0])
                    else:
                        ET.SubElement(case, outcome, message='material phase contract did not pass')
                    with self.subTest(platform=platform, identity=identity, outcome=outcome), \
                            self.assertRaises(ValueError):
                        self.verify(root, platform)

    def test_positive_ore_centrifuge_exact_cases_and_outcomes_on_both_loaders(self):
        self.assertEqual(set(POSITIVE_ORE_CENTRIFUGE_CASES), set(POSITIVE_ORE_CENTRIFUGE_CASE_NAMESPACES))
        self.assertEqual(len(POSITIVE_ORE_CENTRIFUGE_CASES), 6)
        for platform in ('forge', 'neoforge'):
            self.verify(self.report(platform), platform)
            for identity in POSITIVE_ORE_CENTRIFUGE_CASES:
                self.assertIn(identity, EXPECTED[platform])
                self.assertEqual(expected_namespace(identity), 'gregtech_ore_source_audit:test_empty')
                for outcome in ('missing', 'failure', 'error', 'skipped', 'wrong_namespace',
                                'qualified_wrong_namespace', 'class_suffix_wrong_namespace', 'wrong_method'):
                    root = self.report(platform)
                    suite = root.find('testsuite')
                    case = next(c for c in suite
                                if c.get('name') == identity.split('.', 1)[1]
                                and c.get('classname') == expected_namespace(identity))
                    if outcome == 'missing':
                        suite.remove(case)
                    elif outcome == 'wrong_namespace':
                        case.set('classname', 'gregtech_issue_audit:test_empty')
                    elif outcome == 'qualified_wrong_namespace':
                        case.set('name', identity)
                        case.set('classname', 'gregtech_issue_audit:test_empty')
                    elif outcome == 'class_suffix_wrong_namespace':
                        case.set('classname', 'wrong.' + identity.split('.', 1)[0])
                    elif outcome == 'wrong_method':
                        case.set('name', 'unrelatedpositiveoretest')
                    else:
                        ET.SubElement(case, outcome, message='positive ore centrifuge contract did not pass')
                    with self.subTest(platform=platform, identity=identity, outcome=outcome), \
                            self.assertRaises(ValueError):
                        self.verify(root, platform)

    def test_next_ore_centrifuge_exact_cases_and_outcomes_on_both_loaders(self):
        self.assertEqual(set(NEXT_ORE_CENTRIFUGE_CASES), set(NEXT_ORE_CENTRIFUGE_CASE_NAMESPACES))
        self.assertEqual(len(NEXT_ORE_CENTRIFUGE_CASES), 16)
        for platform in ('forge', 'neoforge'):
            self.verify(self.report(platform), platform)
            for identity in NEXT_ORE_CENTRIFUGE_CASES:
                self.assertIn(identity, EXPECTED[platform])
                self.assertEqual(expected_namespace(identity), 'gregtech_ore_source_audit:test_empty')
                for outcome in ('missing', 'failure', 'error', 'skipped', 'wrong_namespace',
                                'qualified_wrong_namespace', 'class_suffix_wrong_namespace', 'wrong_method'):
                    root = self.report(platform)
                    suite = root.find('testsuite')
                    case = next(c for c in suite
                                if c.get('name') == identity.split('.', 1)[1]
                                and c.get('classname') == expected_namespace(identity))
                    if outcome == 'missing':
                        suite.remove(case)
                    elif outcome == 'wrong_namespace':
                        case.set('classname', 'gregtech_issue_audit:test_empty')
                    elif outcome == 'qualified_wrong_namespace':
                        case.set('name', identity)
                        case.set('classname', 'gregtech_issue_audit:test_empty')
                    elif outcome == 'class_suffix_wrong_namespace':
                        case.set('classname', 'wrong.' + identity.split('.', 1)[0])
                    elif outcome == 'wrong_method':
                        case.set('name', 'unrelatednextorecentrifugetest')
                    else:
                        ET.SubElement(case, outcome, message='next ore centrifuge contract did not pass')
                    with self.subTest(platform=platform, identity=identity, outcome=outcome), \
                            self.assertRaises(ValueError):
                        self.verify(root, platform)

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



class QualityPhysicsEvidenceTests(unittest.TestCase):
    def report(self, platform, mutation=None):
        root=ET.Element('testsuite')
        for identity in sorted(EXPECTED[platform]):
            ET.SubElement(root,'testcase',classname=expected_namespace(identity),name=identity.split('.',1)[1])
        if mutation: mutation(root)
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'report.xml'; ET.ElementTree(root).write(path); check(path,platform)
    def test_seven_exact_cases_and_two_templates(self):
        self.assertEqual(len(QUALITY_PHYSICS_CASE_NAMESPACES),7)
        self.assertEqual(set(QUALITY_PHYSICS_CASE_NAMESPACES.values()),{'gregtech_ore_source_audit:test_empty','gregtech_ore_source_audit:ore_host_physics_owned'})
        for platform in EXPECTED: self.report(platform)
    def test_every_new_case_requires_exact_template_and_bare_name(self):
        for platform in EXPECTED:
            for identity in QUALITY_PHYSICS_CASE_NAMESPACES:
                for wrong in ['gregtech_issue_audit:test_empty',identity.split('.',1)[0],'gregtech_ore_source_audit:wrong_template']:
                    def mutate(root):
                        next(n for n in root if n.get('name')==identity.split('.',1)[1]).set('classname',wrong)
                    with self.assertRaises(ValueError): self.report(platform,mutate)
                def prefixed(root):
                    next(n for n in root if n.get('name')==identity.split('.',1)[1]).set('name',identity)
                with self.assertRaises(ValueError): self.report(platform,prefixed)
    def test_every_new_case_required_no_duplicate_substitute(self):
        for platform in EXPECTED:
            for identity in QUALITY_PHYSICS_CASE_NAMESPACES:
                def remove(root): root.remove(next(n for n in root if n.get('name')==identity.split('.',1)[1]))
                with self.assertRaises(ValueError): self.report(platform,remove)
                def duplicate(root):
                    row=next(n for n in root if n.get('name')==identity.split('.',1)[1]);other=next(n for n in root if n.get('name')!=row.get('name'));row.attrib.update(other.attrib)
                with self.assertRaises(ValueError): self.report(platform,duplicate)

if __name__ == '__main__':
    unittest.main()
