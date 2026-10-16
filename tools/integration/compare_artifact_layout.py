"""Record and compare production jar entries by path and SHA-256.

Jar bytes themselves are not compared: entry order and archive metadata can
differ while every class and resource stays the same. Each class is hashed
twice. The second hash drops LineNumberTable, LocalVariableTable and
LocalVariableTypeTable, so a file whose only source change is imports or blank
lines still matches when the bytecode does.

Requires Python 3.11+.
"""
import argparse
import hashlib
import json
import subprocess
import sys
import zipfile
from pathlib import Path


SCHEMA = 1
DEBUG_ATTRIBUTES = {b'LineNumberTable', b'LocalVariableTable', b'LocalVariableTypeTable'}
# Bytes after the tag, excluding Utf8, Long and Double.
_CONSTANT_SKIP = {
    3: 4, 4: 4,
    7: 2, 8: 2, 16: 2, 19: 2, 20: 2,
    9: 4, 10: 4, 11: 4, 12: 4, 17: 4, 18: 4,
    15: 3,
}


def _u2(data, offset):
    return int.from_bytes(data[offset:offset + 2], 'big'), offset + 2


def _u4(data, offset):
    return int.from_bytes(data[offset:offset + 4], 'big'), offset + 4


def parse_constant_pool(data, offset=8):
    """Return the constant pool and the offset of the access flags."""
    count, offset = _u2(data, offset)
    pool = [None] * count
    index = 1
    while index < count:
        tag = data[offset]
        offset += 1
        if tag == 1:
            length, offset = _u2(data, offset)
            pool[index] = data[offset:offset + length]
            offset += length
        elif tag in (5, 6):
            offset += 8
            index += 1
        elif tag in _CONSTANT_SKIP:
            offset += _CONSTANT_SKIP[tag]
        else:
            raise ValueError(f'unsupported constant-pool tag {tag} at index {index}')
        index += 1
    return pool, offset


def encode_attributes(attributes):
    out = bytearray(len(attributes).to_bytes(2, 'big'))
    for name_index, body in attributes:
        out += name_index.to_bytes(2, 'big')
        out += len(body).to_bytes(4, 'big')
        out += body
    return bytes(out)


def read_attributes(data, offset, pool):
    count, offset = _u2(data, offset)
    kept = []
    for _ in range(count):
        name_index, offset = _u2(data, offset)
        length, offset = _u4(data, offset)
        body = data[offset:offset + length]
        offset += length
        if len(body) != length:
            raise ValueError('truncated attribute')
        name = pool[name_index]
        if not isinstance(name, bytes):
            raise ValueError(f'attribute name index {name_index} is not Utf8')
        if name in DEBUG_ATTRIBUTES:
            continue
        if name == b'Code':
            body = rewrite_code_attribute(body, pool)
        kept.append((name_index, body))
    return kept, offset


def rewrite_code_attribute(body, pool):
    if len(body) < 8:
        raise ValueError('short Code attribute')
    code_length = int.from_bytes(body[4:8], 'big')
    pos = 8 + code_length
    if pos + 2 > len(body):
        raise ValueError('Code attribute ends inside the bytecode')
    exception_count = int.from_bytes(body[pos:pos + 2], 'big')
    pos += 2 + exception_count * 8
    if pos > len(body):
        raise ValueError('Code attribute ends inside the exception table')
    attributes, end = read_attributes(body, pos, pool)
    if end != len(body):
        raise ValueError('Code attribute has trailing bytes')
    return body[:pos] + encode_attributes(attributes)


def read_members(data, offset, pool):
    count, offset = _u2(data, offset)
    members = []
    for _ in range(count):
        header = data[offset:offset + 6]
        if len(header) != 6:
            raise ValueError('truncated field or method')
        offset += 6
        attributes, offset = read_attributes(data, offset, pool)
        members.append((header, attributes))
    return members, offset


def encode_members(members):
    out = bytearray(len(members).to_bytes(2, 'big'))
    for header, attributes in members:
        out += header
        out += encode_attributes(attributes)
    return bytes(out)


def normalize_class(data):
    """Return class bytes without line-number and local-variable tables.

    The constant pool is left unchanged, including unused attribute names.
    A class with no debug attributes is returned unchanged.
    """
    if len(data) < 10 or data[:4] != b'\xca\xfe\xba\xbe':
        raise ValueError('not a class file')
    pool, offset = parse_constant_pool(data)
    if offset + 6 > len(data):
        raise ValueError('truncated class header')
    offset += 6
    interface_count, offset = _u2(data, offset)
    offset += interface_count * 2
    if offset > len(data):
        raise ValueError('truncated interface list')
    prefix = data[:offset]
    fields, offset = read_members(data, offset, pool)
    methods, offset = read_members(data, offset, pool)
    attributes, offset = read_attributes(data, offset, pool)
    if offset != len(data):
        raise ValueError(f'class file has {len(data) - offset} trailing bytes')
    return prefix + encode_members(fields) + encode_members(methods) + encode_attributes(attributes)


def _collect_attributes(blob, offset, pool, names):
    count, offset = _u2(blob, offset)
    for _ in range(count):
        name_index, offset = _u2(blob, offset)
        length, offset = _u4(blob, offset)
        name = pool[name_index]
        names.add(name)
        if name == b'Code':
            code = blob[offset:offset + length]
            code_length = int.from_bytes(code[4:8], 'big')
            pos = 8 + code_length
            exception_count = int.from_bytes(code[pos:pos + 2], 'big')
            pos += 2 + exception_count * 8
            _, end = _collect_attributes(code, pos, pool, names)
            if end != len(code):
                raise ValueError('Code attribute has trailing bytes')
        offset += length
    return names, offset


def attribute_names(data):
    """Every attribute name in the class, including attributes nested in Code."""
    pool, offset = parse_constant_pool(data)
    offset += 6
    interface_count, offset = _u2(data, offset)
    offset += interface_count * 2
    names = set()

    def members(body_offset):
        count, body_offset = _u2(data, body_offset)
        for _ in range(count):
            body_offset += 6
            _, body_offset = _collect_attributes(data, body_offset, pool, names)
        return body_offset

    offset = members(offset)
    offset = members(offset)
    _, end = _collect_attributes(data, offset, pool, names)
    if end != len(data):
        raise ValueError('trailing bytes while reading attribute names')
    return names


def git_revision(repo):
    try:
        return subprocess.run(
            ['git', 'rev-parse', 'HEAD'], cwd=repo, check=True,
            capture_output=True, text=True, encoding='utf-8').stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return None


def entry_record(name, data):
    digest = hashlib.sha256(data).hexdigest()
    if name.endswith('.class'):
        normalized = hashlib.sha256(normalize_class(data)).hexdigest()
    else:
        normalized = digest
    return {'sha256': digest, 'normalized': normalized, 'size': len(data)}


def read_jar_entries(jar_path):
    entries = {}
    with zipfile.ZipFile(jar_path) as archive:
        for info in archive.infolist():
            if info.is_dir():
                continue
            if info.filename in entries:
                raise ValueError(f'{jar_path} repeats entry {info.filename}')
            entries[info.filename] = entry_record(info.filename, archive.read(info))
    return entries


def record_inventory(jar_path, platform, repo):
    jar_path = Path(jar_path)
    blob = jar_path.read_bytes()
    return {
        'schema': SCHEMA,
        'platform': platform,
        'git': git_revision(repo),
        'jar': jar_path.as_posix(),
        'jar_sha256': hashlib.sha256(blob).hexdigest(),
        'jar_bytes': len(blob),
        'entries': read_jar_entries(jar_path),
    }


def compare_inventories(baseline, current, mode):
    if mode not in ('strict', 'normalized'):
        raise ValueError(f'unknown compare mode {mode}')
    field = 'sha256' if mode == 'strict' else 'normalized'
    left = baseline['entries']
    right = current['entries']
    changed = sorted(name for name in left.keys() & right.keys()
                     if left[name][field] != right[name][field])
    return {
        'mode': mode,
        'baseline_entries': len(left),
        'current_entries': len(right),
        'unchanged': len(left.keys() & right.keys()) - len(changed),
        'changed': changed,
        'added': sorted(right.keys() - left.keys()),
        'removed': sorted(left.keys() - right.keys()),
    }


def difference_count(report):
    return len(report['changed']) + len(report['added']) + len(report['removed'])


def write_json(path, payload):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + '\n', encoding='utf-8')


def print_report(report):
    print(f"{report['mode']}: {report['unchanged']} unchanged, "
          f"{len(report['changed'])} changed, {len(report['added'])} added, "
          f"{len(report['removed'])} removed")
    shown = report['removed'] + report['added'] + report['changed']
    for name in shown[:40]:
        kind = ('removed' if name in report['removed']
                else 'added' if name in report['added'] else 'changed')
        print(f'  {kind} {name}')
    if len(shown) > 40:
        print(f'  ... {len(shown) - 40} more')


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)

    record = commands.add_parser('record', help='write a path and hash inventory for one jar')
    record.add_argument('--jar', required=True, type=Path)
    record.add_argument('--output', required=True, type=Path)
    record.add_argument('--platform', required=True, choices=('forge', 'neoforge'))
    record.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[2])

    compare = commands.add_parser('compare', help='compare a jar with a recorded inventory')
    compare.add_argument('--baseline', required=True, type=Path)
    compare.add_argument('--jar', required=True, type=Path)
    compare.add_argument('--mode', choices=('strict', 'normalized'), default='strict')
    compare.add_argument('--output', type=Path)
    compare.add_argument('--platform', choices=('forge', 'neoforge'))

    args = parser.parse_args(argv)
    if args.command == 'record':
        inventory = record_inventory(args.jar, args.platform, args.repo)
        write_json(args.output, inventory)
        classes = sum(name.endswith('.class') for name in inventory['entries'])
        print(f"recorded {len(inventory['entries'])} entries ({classes} classes) to {args.output}")
        return 0

    baseline = json.loads(args.baseline.read_text(encoding='utf-8'))
    if baseline.get('schema') != SCHEMA:
        raise SystemExit(f"{args.baseline} has schema {baseline.get('schema')}, expected {SCHEMA}")
    if args.platform and baseline.get('platform') != args.platform:
        raise SystemExit(f"{args.baseline} is {baseline.get('platform')}, not {args.platform}")
    current = {'entries': read_jar_entries(args.jar)}
    report = compare_inventories(baseline, current, args.mode)
    print_report(report)
    if args.output:
        write_json(args.output, report)
    return 1 if difference_count(report) else 0


if __name__ == '__main__':
    sys.exit(main())

