"""Package verified CI outputs and collect a matching pair for release."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
PLATFORMS = {'forge': '1.20.1', 'neoforge': '1.21.1'}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def only(paths, description):
    paths = list(paths)
    if len(paths) != 1:
        raise ValueError(f'Expected one {description}, found {paths}')
    return paths[0]


def check_script(name, *args):
    subprocess.run([sys.executable, '-X', 'utf8', str(ROOT / 'tools/integration' / name),
                    *map(str, args)], cwd=ROOT, check=True)


def package(loader):
    node = f'{PLATFORMS[loader]}-{loader}'
    build = ROOT / 'multiversion/versions' / node / 'build'
    jar = only((p for p in (build / 'libs').glob('*.jar')
                if not any(t in p.stem for t in ('-sources', '-javadoc', '-dev'))), 'mod jar')
    output = ROOT / 'work/ci-artifacts'
    output.mkdir(parents=True, exist_ok=True)
    report = output / 'packaging.json'
    check_script('verify_artifacts.py', '--platform', loader, '--jar', jar, '--output', report)
    evidence = json.loads(report.read_text(encoding='utf-8'))
    target = output / f'gt6ce-{node}.jar'
    shutil.copyfile(jar, target)
    checksum = digest(target)
    if checksum != evidence[loader]['sha256']:
        raise ValueError('Jar changed after verification')
    target.with_suffix('.jar.sha256').write_text(f'{checksum}  {target.name}\n', encoding='utf-8')

    core_sdk = only((ROOT / 'core/build/libs').glob('gt6ce-addon-api-*.jar'), 'core addon SDK')
    check_script('verify_addon_api.py', '--jar', core_sdk)
    shutil.copyfile(core_sdk, output / core_sdk.name)
    sdk = only((p for p in (build / 'addon-sdk').glob(f'gt6ce-addon-sdk-{node}-*.jar')
                if not p.stem.endswith(('-sources', '-javadoc'))), 'platform addon SDK')
    check_script('verify_addon_api.py', '--jar', sdk, '--loader', loader)
    shutil.copyfile(sdk, output / sdk.name)
    for classifier in ('sources', 'javadoc'):
        extra = sdk.with_name(f'{sdk.stem}-{classifier}.jar')
        if extra.is_file():
            shutil.copyfile(extra, output / extra.name)


def collect(source, output, commit):
    if not re.fullmatch(r'[0-9a-f]{40}', commit):
        raise ValueError('Missing or invalid build commit')
    reports, jars = {}, {}
    for loader, version in PLATFORMS.items():
        folder = source / f'gt6ce-{version}-{loader}'
        report = json.loads((folder / 'packaging.json').read_text(encoding='utf-8'))
        if report.get('status') != 'passed' or report.get('build_commit') != commit:
            raise ValueError(f'{loader}: failed or stale packaging evidence')
        core = report.get('compiled_core_class_sha256')
        if not core or report.get('shared_classes') != len(core) or report[loader]['core_class_sha256'] != core:
            raise ValueError(f'{loader}: incomplete shared-core evidence')
        jar = folder / f'gt6ce-{version}-{loader}.jar'
        if digest(jar) != report[loader]['sha256']:
            raise ValueError(f'{loader}: jar does not match its packaging report')
        reports[loader], jars[loader] = report, jar
    forge, neo = reports.values()
    for field in ('mod_id', 'mod_version', 'compiled_core_class_sha256'):
        if forge[field] != neo[field]:
            raise ValueError(f'Platform mismatch: {field}')
    if forge['mod_id'] != 'gt6ce':
        raise ValueError('Unexpected mod ID')

    output.mkdir(parents=True, exist_ok=True)
    manifest = {'commit': commit, 'files': {}}
    for loader, jar in jars.items():
        name = f'gt6ce-{PLATFORMS[loader]}-{loader}-{commit[:12]}.jar'
        shutil.copyfile(jar, output / name)
        manifest['files'][name] = reports[loader][loader]['sha256']
    (output / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    print(f'Collected both verified jars for {commit[:12]}')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    pack = commands.add_parser('package')
    pack.add_argument('loader', choices=PLATFORMS)
    gather = commands.add_parser('collect')
    gather.add_argument('source', type=Path)
    gather.add_argument('output', type=Path)
    args = parser.parse_args()
    if args.command == 'package':
        package(args.loader)
    else:
        collect(args.source, args.output, os.environ.get('GITHUB_SHA', ''))


if __name__ == '__main__':
    main()
