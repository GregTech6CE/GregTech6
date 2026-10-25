"""Release collection rejects mixed commits, mismatched cores and corrupt downloads."""
import hashlib
import json
from pathlib import Path
import tempfile
import unittest

from tools.ci.artifacts import collect, PLATFORMS


class ReleaseArtifactsTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.output = self.root / 'release'
        self.commit = 'a' * 40
        self.reports = {}
        for loader, version in PLATFORMS.items():
            folder = self.root / f'gt6ce-{version}-{loader}'
            folder.mkdir()
            jar = folder / f'gt6ce-{version}-{loader}.jar'
            jar.write_bytes(loader.encode())
            core = {'gregtech/core/Example.class': 'core-hash'}
            report = {'status': 'passed', 'build_commit': self.commit, 'mod_id': 'gt6ce',
                      'mod_version': '0.0.0', 'compiled_core_class_sha256': core, 'shared_classes': 1,
                      loader: {'core_class_sha256': core, 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest()}}
            self.reports[loader] = folder / 'packaging.json'
            self.reports[loader].write_text(json.dumps(report))

    def change_report(self, loader, update):
        report = json.loads(self.reports[loader].read_text())
        update(report)
        self.reports[loader].write_text(json.dumps(report))

    def test_matching_pair_is_collected_without_sdk_jars(self):
        (self.reports['forge'].parent / 'gt6ce-addon-sdk.jar').write_bytes(b'sdk')
        collect(self.root, self.output, self.commit)
        manifest = json.loads((self.output / 'manifest.json').read_text())
        self.assertEqual(self.commit, manifest['commit'])
        self.assertEqual(2, len(manifest['files']))
        self.assertEqual(2, len(list(self.output.glob('*.jar'))))

    def test_stale_commit_cannot_be_published(self):
        self.change_report('neoforge', lambda report: report.update(build_commit='b' * 40))
        with self.assertRaisesRegex(ValueError, 'stale'):
            collect(self.root, self.output, self.commit)
        self.assertFalse(self.output.exists())

    def test_corrupt_download_cannot_be_published(self):
        jar = next(self.reports['forge'].parent.glob('*.jar'))
        jar.write_bytes(b'corrupt')
        with self.assertRaisesRegex(ValueError, 'does not match'):
            collect(self.root, self.output, self.commit)

    def test_two_individually_valid_but_different_cores_are_rejected(self):
        def different_core(report):
            report['compiled_core_class_sha256'] = {'gregtech/core/Example.class': 'other-hash'}
            report['neoforge']['core_class_sha256'] = report['compiled_core_class_sha256']
        self.change_report('neoforge', different_core)
        with self.assertRaisesRegex(ValueError, 'Platform mismatch'):
            collect(self.root, self.output, self.commit)


if __name__ == '__main__':
    unittest.main()
