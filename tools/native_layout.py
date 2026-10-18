"""Native tooling views of Stonecutter outputs; never a second source of truth.

Read-only audits may call native_path directly. Run mutating legacy generators
through native_tool.py so edits are reconciled into shared/conditional sources.
Temporary test repositories without a migration manifest retain their layout.
"""
from pathlib import Path
import hashlib
import json
import re
import os
import sys

NODES = {'forge': '1.20.1-forge', 'neoforge': '1.21.1-neoforge'}
SETS = ('main', 'test', 'bootstrapGameTest', 'productionSmoke', 'generated')
_prepared = set()
_TOKEN = re.compile(r'//[^\r\n]*|/\*[\s\S]*?\*/|"""(?:\\[\s\S]|(?!""")[^\\])*"""'
                    r'|"(?:\\[\s\S]|[^"\\])*"|\'(?:\\[\s\S]|[^\'\\])*\'')


def require_managed_writer():
    if os.environ.get('GT_NATIVE_TOOL_ACTIVE') != '1' and not any(
            flag in sys.argv for flag in ('--check', '--dry-run', '--help', '-h')):
        raise SystemExit('Run native generators through: python tools/native_tool.py ' +
                         str(Path(sys.argv[0]).as_posix()) + ' [arguments]')


def clean_java(text, platform):
    """Drop inactive generated branches and directives, retaining active comments."""
    result, stack, active, cursor = [], [], True, 0
    for token in _TOKEN.finditer(text):
        if active: result.append(text[cursor:token.start()])
        value = token[0]
        if re.fullmatch(r'//\?\s*if\s+>=1\.21\.1\s*\{\s*', value):
            condition = platform == 'neoforge'
            stack.append((active, condition))
            active = active and condition
        elif re.fullmatch(r'//\?\s*}\s*else\s*\{\s*', value):
            parent, condition = stack[-1]
            active = parent and not condition
        elif re.fullmatch(r'//\?\s*}\s*', value):
            active, _ = stack.pop()
        elif value.startswith('//?'):
            raise ValueError('Unsupported Stonecutter condition in tooling view: ' + value)
        elif active:
            result.append(value)
        cursor = token.end()
    if active: result.append(text[cursor:])
    if stack: raise ValueError('Unclosed Stonecutter condition')
    return ''.join(result)


def file_hash(file):
    return hashlib.sha256(file.read_bytes()).hexdigest()


def file_stamp(file):
    stat = file.stat()
    return [stat.st_size, stat.st_mtime_ns]


def native_inputs(repo):
    for platform, node in NODES.items():
        for source_set in SETS:
            for kind in ('java', 'resources'):
                seen = set()
                for root in (repo / f'multiversion/versions/{node}/build/generated/stonecutter/{source_set}/{kind}',
                             repo / f'multiversion/versions/{node}/src/{source_set}/{kind}'):
                    for file in sorted(root.rglob('*')):
                        if not file.is_file(): continue
                        name = file.relative_to(root).as_posix()
                        if name in seen: raise ValueError(f'Duplicate native input: {platform}/{source_set}/{kind}/{name}')
                        seen.add(name)
                        yield platform, source_set, kind, name, file


def prepare(repo, *, force=False):
    repo = Path(repo).resolve()
    if repo in _prepared and not force: return repo / 'build/native-tools'
    view = repo / 'build/native-tools'
    inputs = list(native_inputs(repo))
    if not any(kind == 'java' and source_set == 'main' for _, source_set, kind, _, _ in inputs):
        raise RuntimeError('Generate Stonecutter outputs first: ./gradlew prepareNativeTools')
    stamp = hashlib.sha256(json.dumps([(str(file.relative_to(repo)), file.stat().st_size, file.stat().st_mtime_ns)
                                      for *_, file in inputs]).encode()).hexdigest()
    manifest = view / 'view.json'
    previous = json.loads(manifest.read_text()) if manifest.exists() else {}
    if not force and previous.get('input_stamp') == stamp:
        if changes(repo):
            raise RuntimeError('Tooling view has edits not regenerated from managed sources; regenerate Stonecutter outputs, or preserve edits and use native_tool.py --discard-view-changes')
        if 'metadata' not in previous:
            previous['metadata'] = {p: file_stamp(view / p) for p in previous['files']}
            manifest.write_text(json.dumps(previous, indent=2) + '\n')
        _prepared.add(repo)
        return view
    files = {}
    for platform, source_set, kind, name, file in inputs:
        relative = f'{platform}/src/{source_set}/{kind}/{name}'
        target = view / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        data = clean_java(file.read_text(encoding='utf-8'), platform).encode('utf-8') if kind == 'java' else file.read_bytes()
        if not target.exists() or target.read_bytes() != data: target.write_bytes(data)
        files[relative] = hashlib.sha256(data).hexdigest()
    # Only remove individual previously generated files within this checked view.
    stale = set(previous.get('files', {})) - set(files)
    if force:
        stale |= {p.relative_to(view).as_posix() for platform in NODES
                  for p in (view / platform / 'src').rglob('*') if p.is_file()} - set(files)
    for old in stale:
        target = (view / old).resolve()
        if not target.is_relative_to(view.resolve()): raise ValueError('Unsafe view manifest path')
        if target.is_file(): target.unlink()
    manifest.parent.mkdir(parents=True, exist_ok=True)
    manifest.write_text(json.dumps({'input_stamp': stamp, 'files': files,
                                    'metadata': {p: file_stamp(view / p) for p in files}}, indent=2) + '\n')
    _prepared.add(repo)
    return view


def native_path(path):
    """Resolve a former native path into a complete generated tooling view."""
    path = Path(path).absolute()
    for repo in (path, *path.parents):
        if not (repo / 'multiversion/migration-manifest.json').is_file(): continue
        relative = path.relative_to(repo).as_posix()
        for prefix, platform in (('neoforge/src', 'neoforge'), ('src', 'forge')):
            if relative == prefix or relative.startswith(prefix + '/'):
                return prepare(repo) / platform / relative.removeprefix('neoforge/')
        break
    return path


def platform_build(repo, platform):
    repo = Path(repo)
    return (repo / f'multiversion/versions/{NODES[platform]}/build'
            if (repo / 'multiversion/migration-manifest.json').exists()
            else repo / ('build' if platform == 'forge' else 'neoforge/build'))


def changes(repo):
    view = Path(repo) / 'build/native-tools'
    manifest = json.loads((view / 'view.json').read_text())
    recorded, metadata = manifest['files'], manifest.get('metadata', {})
    actual = {}
    for platform in NODES:
        for file in (view / platform / 'src').rglob('*'):
            if not file.is_file(): continue
            path = file.relative_to(view).as_posix()
            actual[path] = recorded[path] if metadata.get(path) == file_stamp(file) else file_hash(file)
    return {p for p in recorded.keys() | actual.keys() if recorded.get(p) != actual.get(p)}


def reconcile(repo):
    """Commit a successful generator's changed logical paths into managed inputs."""
    import sys
    sys.path.insert(0, str(Path(__file__).parent / 'integration'))
    from migrate_stonecutter_sources import merge
    repo = Path(repo).resolve()
    view = repo / 'build/native-tools'
    changed = changes(repo)
    controller = repo / 'multiversion/stonecutter.gradle.kts'
    if changed and controller.exists() and not re.search(
            r'stonecutter\s+active\s+"1\.20\.1-forge"', controller.read_text()):
        raise RuntimeError('Reset the Stonecutter active project to the Forge VCS version before running legacy generators')
    logical = {tuple(p.split('/', 4)[2:]) for p in changed}
    updates = []
    for source_set, kind, name in sorted(logical):
        versions = {platform: view / f'{platform}/src/{source_set}/{kind}/{name}' for platform in NODES}
        data = {platform: file.read_bytes() for platform, file in versions.items() if file.exists()}
        common = repo / f'multiversion/src/{source_set}/{kind}/{name}'
        overrides = {p: repo / f'multiversion/versions/{n}/src/{source_set}/{kind}/{name}' for p, n in NODES.items()}
        desired = {}
        if len(data) == 2:
            if kind == 'java':
                merged, _ = merge(data['forge'].decode('utf-8'), data['neoforge'].decode('utf-8'))
                desired[common] = merged.encode('utf-8')
            else:
                # Resource loader namespace is the one global replacement rule.
                normalized = re.sub(
                    rb'(?s)\A(\s*\{.*?"loader"\s*:\s*")neoforge:composite(".*)\Z',
                    rb'\1forge:composite\2', data['neoforge'])
                equal = data['forge'] == normalized
                if name.endswith(('.json', '.mcmeta')):
                    equal = json.loads(data['forge']) == json.loads(normalized)
                desired[common] = data['forge']
                if not equal: desired[overrides['neoforge']] = data['neoforge']
        elif data:
            platform = next(iter(data))
            desired[overrides[platform]] = data[platform]
        for path in [common, *overrides.values()]:
            if path in desired:
                if not path.exists() or path.read_bytes() != desired[path]: updates.append((path, desired[path]))
            elif path.exists(): updates.append((path, None))
    # Stage every transformation before modifying any managed file.
    for path, data in updates:
        if not path.is_relative_to(repo / 'multiversion'): raise ValueError('Unsafe managed output')
        if data is None: path.unlink()
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
    return len(changed), len(updates)
