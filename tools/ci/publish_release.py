"""Maintain one nightly release, preserving old jars until both replacements exist."""
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import urllib.error
import urllib.parse
import urllib.request

TAG = 'nightly'
ASSET = re.compile(r'gt6ce-(?:1\.20\.1-forge|1\.21\.1-neoforge)-[0-9a-f]{12}\.jar')


class GitHub:
    def __init__(self, repository, token):
        if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', repository):
            raise ValueError('Invalid repository')
        self.repository, self.token = repository, token

    def request(self, path, method='GET', body=None, file=None):
        host = 'uploads.github.com' if file else 'api.github.com'
        headers = {'Authorization': f'Bearer {self.token}', 'Accept': 'application/vnd.github+json',
                   'User-Agent': 'GT6CE-Actions', 'X-GitHub-Api-Version': '2022-11-28'}
        data = None
        if file:
            data = file.read_bytes()
            headers['Content-Type'] = 'application/java-archive'
        elif body is not None:
            data = json.dumps(body).encode()
            headers['Content-Type'] = 'application/json'
        request = urllib.request.Request(f'https://{host}/repos/{self.repository}{path}',
                                         data=data, headers=headers, method=method)
        with urllib.request.urlopen(request, timeout=180) as response:
            content = response.read()
            return json.loads(content) if content else None

    def optional(self, path):
        try:
            return self.request(path)
        except urllib.error.HTTPError as error:
            if error.code != 404:
                raise
            return None

    def pages(self, path):
        page = 1
        while True:
            values = self.request(f'{path}?per_page=100&page={page}')
            yield from values
            if len(values) < 100:
                return
            page += 1


def release_files(folder, commit):
    manifest = json.loads((folder / 'manifest.json').read_text(encoding='utf-8'))
    expected = {f'gt6ce-{node}-{commit[:12]}.jar' for node in ('1.20.1-forge', '1.21.1-neoforge')}
    if not re.fullmatch(r'[0-9a-f]{40}', commit) or manifest.get('commit') != commit or set(manifest['files']) != expected:
        raise ValueError('Release must contain exactly both jars from this commit')
    for name, checksum in manifest['files'].items():
        if hashlib.sha256((folder / name).read_bytes()).hexdigest() != checksum:
            raise ValueError(f'Corrupt release file: {name}')
    return manifest['files']


def asset_matches(asset, file, checksum):
    return (asset.get('state') == 'uploaded' and asset.get('size') == file.stat().st_size
            and asset.get('digest') == 'sha256:' + checksum)


def publish(api, folder, commit):
    files = release_files(folder, commit)
    if api.request('/git/ref/heads/main')['object']['sha'] != commit:
        print('A newer main commit exists; leaving the release unchanged.')
        return
    releases = [r for r in api.pages('/releases') if r['tag_name'] == TAG]
    if len(releases) > 1:
        raise ValueError('Multiple nightly releases found')
    release = releases[0] if releases else api.request('/releases', 'POST', {
        'tag_name': TAG, 'target_commitish': commit, 'name': 'Latest build',
        'body': '', 'draft': True, 'prerelease': True})
    if release.get('immutable'):
        raise ValueError('The nightly release is immutable and cannot be updated')
    path = f'/releases/{release["id"]}'
    assets = {a['name']: a for a in api.pages(path + '/assets')}
    for name, checksum in files.items():
        file = folder / name
        if name in assets:
            if not asset_matches(assets[name], file, checksum):
                raise ValueError(f'Existing asset differs: {name}; previous release retained')
            continue
        asset = api.request(path + '/assets?name=' + urllib.parse.quote(name), 'POST', file=file)
        if not asset_matches(asset, file, checksum):
            raise ValueError(f'Upload verification failed: {name}; previous release retained')

    # A second freshness check prevents an old build becoming current during uploads.
    if api.request('/git/ref/heads/main')['object']['sha'] != commit:
        print('Main advanced during upload; the release was not promoted.')
        return
    tag = api.optional('/git/ref/tags/' + TAG)
    if tag:
        api.request('/git/refs/tags/' + TAG, 'PATCH', {'sha': commit, 'force': True})
    else:
        api.request('/git/refs', 'POST', {'ref': 'refs/tags/' + TAG, 'sha': commit})
    api.request(path, 'PATCH', {'name': 'Latest build', 'body': f'Build `{commit[:12]}`.',
                              'draft': False, 'prerelease': True, 'make_latest': 'false'})
    # Only our superseded jars are removed; unrelated attachments are left alone.
    for asset in api.pages(path + '/assets'):
        if ASSET.fullmatch(asset['name']) and asset['name'] not in files:
            api.request(f'/releases/assets/{asset["id"]}', 'DELETE')
    print(f'https://github.com/{api.repository}/releases/tag/{TAG}')


if __name__ == '__main__':
    if os.environ.get('GITHUB_REF') != 'refs/heads/main' or os.environ.get('GITHUB_EVENT_NAME') not in ('push', 'workflow_dispatch'):
        raise SystemExit('Only a main push or manual main build may publish')
    publish(GitHub(os.environ['GITHUB_REPOSITORY'], os.environ['GH_TOKEN']),
            Path(sys.argv[1]), os.environ['GITHUB_SHA'])
