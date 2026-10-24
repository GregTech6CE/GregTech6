"""Synthetic checker regressions; these fixtures do not launch Minecraft."""

import copy
import importlib.util
from pathlib import Path
import tempfile
import unittest


CHECKER_PATH = Path(__file__).resolve().parents[1] / 'integration' / 'check_restart_phase.py'
SPEC = importlib.util.spec_from_file_location('restart_phase_checker', CHECKER_PATH)
checker = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(checker)


class CheckerTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix='gt6-restart-checker-')
        self.addCleanup(temporary.cleanup)
        self.directory = Path(temporary.name)
        self.world = self.directory / 'world'
        self.world.mkdir()
        (self.world / 'region').mkdir()
        for name in ('level.dat', 'region/r.39062.39062.mca', 'gt6-restart-phase1.properties'):
            (self.world / name).write_text('SYNTHETIC CHECKER FIXTURE, NOT MINECRAFT')

        self.report = self.directory / 'test.xml'
        self.report.write_text(
            '<testsuite><testcase classname="gregtech_restart_read:test_empty" '
            'name="readdisposablefixture"/></testsuite>'
        )
        self.log = self.directory / 'test.log'
        self.log.write_text(
            f'GT6_RESTART_READ warm=false world={self.world} '
            'pid=999999999 writerPid=999999998 nonce=synthetic\n'
            'All 1 required tests passed\nStopping server\nSaving worlds\n'
            'All dimensions are saved\nGame test server shutting down\nBUILD SUCCESSFUL'
        )

    def check(self, exit_code=0):
        return checker.check('read', self.report, self.log, exit_code, self.directory)

    def pair(self):
        reader = self.check()
        writer = copy.deepcopy(reader)
        writer.update(phase='write', pid='999999998', writer_pid='')
        return writer, reader

    def test_complete_evidence(self):
        self.assertEqual(self.check()['nonce'], 'synthetic')

    def test_extra_case(self):
        self.report.write_text(
            self.report.read_text().replace('</testsuite>', '<testcase/></testsuite>')
        )
        with self.assertRaises(ValueError):
            self.check()

    def test_failed_exit(self):
        with self.assertRaises(ValueError):
            self.check(1)

    def test_warm_missing(self):
        self.log.write_text(self.log.read_text().replace(' warm=false', ''))
        with self.assertRaises(ValueError):
            self.check()

    def test_missing_region(self):
        (self.world / 'region/r.39062.39062.mca').unlink()
        with self.assertRaises(ValueError):
            self.check()

    def test_incomplete_save(self):
        self.log.write_text(self.log.read_text().replace('All dimensions are saved', ''))
        with self.assertRaises(ValueError):
            self.check()

    def test_matching_pair(self):
        writer, reader = self.pair()
        self.assertEqual(checker.check_pair(writer, reader)['nonce'], 'synthetic')

    def test_pair_rejects_mismatches(self):
        for key, value in (
            ('world', '/other'),
            ('nonce', 'other'),
            ('pid', '999999998'),
            ('writer_pid', '123'),
            ('phase', 'write'),
        ):
            writer, reader = self.pair()
            reader[key] = value
            with self.subTest(key=key), self.assertRaises(ValueError):
                checker.check_pair(writer, reader)

    def test_pair_rejects_changed_marker(self):
        writer, reader = self.pair()
        key = str(self.world / 'gt6-restart-phase1.properties')
        reader['artifacts'][key]['sha256'] = '0' * 64
        with self.assertRaises(ValueError):
            checker.check_pair(writer, reader)

    def test_pair_rejects_unrelated_marker_path(self):
        # Even matching marker hashes in both receipts cannot refer to another world.
        for changed_phases in (('write',), ('read',), ('write', 'read')):
            writer, reader = self.pair()
            for receipt in (writer, reader):
                if receipt['phase'] in changed_phases:
                    key = str(self.world / 'gt6-restart-phase1.properties')
                    unrelated = str(self.directory / 'other-world/gt6-restart-phase1.properties')
                    receipt['artifacts'][unrelated] = receipt['artifacts'].pop(key)
            with self.subTest(phases=changed_phases), self.assertRaisesRegex(
                ValueError, 'outside the recorded world'
            ):
                checker.check_pair(writer, reader)

    def test_wrong_namespace(self):
        self.report.write_text(self.report.read_text().replace(
            'gregtech_restart_read:test_empty', 'gregtech_issue_audit:test_empty'
        ))
        with self.assertRaises(ValueError):
            self.check()

    def test_reused_pid(self):
        self.log.write_text(self.log.read_text().replace(
            'writerPid=999999998', 'writerPid=999999999'
        ))
        with self.assertRaises(ValueError):
            self.check()

    def test_failed_case_states(self):
        original = self.report.read_text()
        for kind in ('failure', 'error', 'skipped'):
            self.report.write_text(original.replace('/>', f'><{kind}/></testcase>'))
            with self.subTest(kind=kind), self.assertRaises(ValueError):
                self.check()


if __name__ == '__main__':
    unittest.main()
