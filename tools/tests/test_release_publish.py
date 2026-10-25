"""Publishing must not remove the old pair on a failed or stale build."""
import hashlib
import json
from pathlib import Path
import tempfile
import unittest

from tools.ci.publish_release import publish


class FakeGitHub:
    repository = 'test/repo'

    def __init__(self, commit):
        self.commit = commit
        self.events = []
        self.fail_upload = False
        self.immutable = False
        self.old = {'id': 1, 'name': 'gt6ce-1.20.1-forge-' + 'b' * 12 + '.jar'}
        self.assets = [self.old, {'id': 2, 'name': 'unrelated.txt'}]

    def pages(self, path):
        if path == '/releases':
            return [{'id': 9, 'tag_name': 'nightly', 'immutable': self.immutable}]
        return list(self.assets)

    def optional(self, path):
        return {'object': {'sha': 'b' * 40}}

    def request(self, path, method='GET', body=None, file=None):
        self.events.append((method, path))
        if path == '/git/ref/heads/main':
            return {'object': {'sha': self.commit}}
        if file:
            if self.fail_upload and 'neoforge' in file.name:
                raise OSError('Upload interrupted')
            asset = {'id': len(self.assets) + 1, 'name': file.name, 'state': 'uploaded',
                     'size': file.stat().st_size, 'digest': 'sha256:' + hashlib.sha256(file.read_bytes()).hexdigest()}
            self.assets.append(asset)
            return asset
        if method == 'DELETE':
            self.assets = [a for a in self.assets if a['id'] != int(path.rsplit('/', 1)[1])]
        return {}


class ReleasePublishTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.folder = Path(self.temp.name)
        self.commit = 'a' * 40
        files = {}
        for node in ('1.20.1-forge', '1.21.1-neoforge'):
            name = f'gt6ce-{node}-{self.commit[:12]}.jar'
            data = node.encode()
            (self.folder / name).write_bytes(data)
            files[name] = hashlib.sha256(data).hexdigest()
        (self.folder / 'manifest.json').write_text(json.dumps({'commit': self.commit, 'files': files}))
        self.api = FakeGitHub(self.commit)

    def test_uploads_both_before_promoting_and_removing_old_assets(self):
        publish(self.api, self.folder, self.commit)
        methods = [method for method, path in self.api.events]
        self.assertEqual(['GET', 'POST', 'POST', 'GET', 'PATCH', 'PATCH', 'DELETE'], methods)
        self.assertNotIn(self.api.old, self.api.assets)
        self.assertTrue(any(a['name'] == 'unrelated.txt' for a in self.api.assets))

    def test_failed_second_upload_preserves_previous_release(self):
        self.api.fail_upload = True
        with self.assertRaises(OSError):
            publish(self.api, self.folder, self.commit)
        self.assertIn(self.api.old, self.api.assets)
        self.assertFalse(any(method in ('PATCH', 'DELETE') for method, path in self.api.events))

    def test_stale_build_does_not_write_anything(self):
        self.api.commit = 'c' * 40
        publish(self.api, self.folder, self.commit)
        self.assertEqual([('GET', '/git/ref/heads/main')], self.api.events)

    def test_immutable_release_is_left_untouched(self):
        self.api.immutable = True
        with self.assertRaisesRegex(ValueError, 'immutable'):
            publish(self.api, self.folder, self.commit)
        self.assertFalse(any(method != 'GET' for method, path in self.api.events))

    def test_corrupt_jar_is_rejected_before_contacting_github(self):
        next(self.folder.glob('*.jar')).write_bytes(b'corrupt')
        with self.assertRaisesRegex(ValueError, 'Corrupt'):
            publish(self.api, self.folder, self.commit)
        self.assertEqual([], self.api.events)


if __name__ == '__main__':
    unittest.main()
