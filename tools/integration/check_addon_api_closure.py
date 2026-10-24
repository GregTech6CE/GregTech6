"""Fail when an addon API class refers to a type outside the API, the JDK, Minecraft and its loader.

The sample addon is checked against the SDK jar, so a compile classpath leak still fails here.
"""
import argparse
import sys
import zipfile
from pathlib import Path


def u2(data, index):
    return int.from_bytes(data[index:index + 2], 'big'), index + 2


def parse_pool(data):
    if data[:4] != b'\xca\xfe\xba\xbe':
        raise ValueError('not a class file')
    count = int.from_bytes(data[8:10], 'big')
    index = 10
    pool = [None]
    slot = 1
    while slot < count:
        tag = data[index]
        index += 1
        if tag == 1:
            length = int.from_bytes(data[index:index + 2], 'big')
            index += 2
            pool.append(data[index:index + length].decode('utf-8', 'surrogateescape'))
            index += length
        elif tag in (7, 8, 16, 19, 20):
            pool.append((tag, int.from_bytes(data[index:index + 2], 'big')))
            index += 2
        elif tag == 15:
            pool.append((tag,))
            index += 3
        elif tag in (5, 6):
            pool.append((tag,))
            pool.append(None)
            index += 8
            slot += 1
        elif tag == 12:
            pool.append((tag, int.from_bytes(data[index:index + 2], 'big'),
                         int.from_bytes(data[index + 2:index + 4], 'big')))
            index += 4
        else:
            pool.append((tag,))
            index += 4
        slot += 1
    if slot != count:
        raise ValueError('constant pool ended early')
    return pool, index


def descriptor_types(descriptor):
    types = []
    cursor = 0
    while cursor < len(descriptor):
        if descriptor[cursor] == 'L':
            end = cursor + 1
            while end < len(descriptor) and descriptor[end] not in ';<:':
                end += 1
            if end < len(descriptor) and descriptor[end] == ':':
                # A formal type parameter whose name begins with L, not a class.
                cursor = end + 1
                continue
            types.append(descriptor[cursor + 1:end])
            # Continue inside generic arguments rather than treating them as
            # part of a permitted java/util/List class name.
            cursor = end
        elif descriptor[cursor] == 'T':
            end = descriptor.find(';', cursor)
            bound = descriptor.find(':', cursor)
            cursor = bound + 1 if bound >= 0 and (end < 0 or bound < end) else end + 1 if end >= 0 else len(descriptor)
        else:
            cursor += 1
    return types


def member_descriptors(data, pool, index):
    """Field, method and signature descriptors. A parameter type is not always a Class entry."""
    descriptors = []
    index += 6  # access flags, this class, super class
    interface_count, index = u2(data, index)
    index += interface_count * 2
    for _ in range(2):
        count, index = u2(data, index)
        for _ in range(count):
            index += 4  # access flags and name
            descriptor, index = u2(data, index)
            if isinstance(pool[descriptor], str):
                descriptors.append(pool[descriptor])
            attribute_count, index = u2(data, index)
            for _ in range(attribute_count):
                name_index, index = u2(data, index)
                length = int.from_bytes(data[index:index + 4], 'big')
                index += 4
                if pool[name_index] == 'Signature' and isinstance(pool[int.from_bytes(data[index:index + 2], 'big')], str):
                    descriptors.append(pool[int.from_bytes(data[index:index + 2], 'big')])
                index += length
    if index + 2 <= len(data):
        attribute_count, index = u2(data, index)
        for _ in range(attribute_count):
            name_index, index = u2(data, index)
            length = int.from_bytes(data[index:index + 4], 'big')
            index += 4
            if pool[name_index] == 'Signature' and isinstance(pool[int.from_bytes(data[index:index + 2], 'big')], str):
                descriptors.append(pool[int.from_bytes(data[index:index + 2], 'big')])
            index += length
    return descriptors


def referenced(data):
    pool, index = parse_pool(data)
    names = []
    for entry in pool:
        if not isinstance(entry, tuple) or not entry:
            continue
        if entry[0] == 7:
            name = pool[entry[1]]
            if isinstance(name, str):
                if name.startswith('['):
                    names.extend(descriptor_types(name))
                else:
                    names.append(name)
        elif entry[0] in (12, 16):
            descriptor = pool[entry[-1]]
            if isinstance(descriptor, str):
                names.extend(descriptor_types(descriptor))
    for descriptor in member_descriptors(data, pool, index):
        names.extend(descriptor_types(descriptor))
    return names


def class_names(directory):
    root = Path(directory)
    if not root.is_dir():
        raise ValueError('missing class directory ' + str(root))
    names = {path.relative_to(root).with_suffix('').as_posix() for path in root.rglob('*.class')}
    if not names:
        raise ValueError('no classes in ' + str(root))
    return names


def jar_classes(path):
    with zipfile.ZipFile(path) as archive:
        return {name[:-6] for name in archive.namelist() if name.endswith('.class')}


def sdk_contents(path, allowed):
    """The compile-only SDK is exactly the public classes plus manifest/license notices."""
    errors = []
    with zipfile.ZipFile(path) as archive:
        names = [entry.filename for entry in archive.infolist() if not entry.is_dir()]
        if len(names) != len(set(names)):
            errors.append('SDK contains duplicate entries')
        classes = {name[:-6] for name in names if name.endswith('.class')}
        errors.extend('SDK contains non-API class ' + name for name in sorted(classes - allowed))
        errors.extend('SDK is missing API class ' + name for name in sorted(allowed - classes))
        for name in names:
            if not (name.endswith('.class') or name == 'META-INF/MANIFEST.MF'
                    or name.startswith('META-INF/gregtech6/')):
                errors.append('SDK contains runtime/non-API resource ' + name)
        if 'META-INF/MANIFEST.MF' in names:
            manifest = archive.read('META-INF/MANIFEST.MF').decode('utf-8')
            if any(line.lower().startswith(('mixinconfigs:', 'fmlmodtype:', 'modtype:'))
                   for line in manifest.splitlines()):
                errors.append('SDK manifest declares runtime mod/mixin metadata')
    return errors


def permitted(name, kind, api_names):
    if name in api_names or name.startswith(('java/', 'javax/', 'jdk/')):
        return True
    if kind == 'core':
        return False
    if name.startswith('net/minecraft/'):
        return True
    # Block entity renderers take a PoseStack and a vertex buffer, so an addon
    # renderer names these Minecraft rendering types directly.
    if name.startswith('com/mojang/blaze3d/'):
        return True
    if kind == 'forge':
        return name.startswith('net/minecraftforge/')
    if kind == 'neoforge':
        return name.startswith('net/neoforged/')
    raise ValueError('unknown kind ' + kind)


def violations(directory, allowed, kind):
    found = []
    root = Path(directory)
    for path in sorted(root.rglob('*.class')):
        for name in referenced(path.read_bytes()):
            if not permitted(name, kind, allowed):
                found.append(f'{path.relative_to(root).as_posix()} -> {name}')
    return found


def leak_class():
    """A tiny class whose field type is an internal GregTech material. """
    strings = [
        'com/example/Leak',
        'java/lang/Object',
        'gregtech/api/material/GTMaterial',
        'leak',
        'Lgregtech/api/material/GTMaterial;',
    ]
    entries = [(1, text.encode('utf-8')) for text in strings]
    entries.append((7, (1).to_bytes(2, 'big')))
    entries.append((7, (2).to_bytes(2, 'big')))
    entries.append((7, (3).to_bytes(2, 'big')))
    entries.append((12, (4).to_bytes(2, 'big') + (5).to_bytes(2, 'big')))
    body = bytearray(b'\xca\xfe\xba\xbe' + (0).to_bytes(2, 'big') + (61).to_bytes(2, 'big'))
    body += (len(entries) + 1).to_bytes(2, 'big')
    for tag, payload in entries:
        body.append(tag)
        if tag == 1:
            body += len(payload).to_bytes(2, 'big') + payload
        else:
            body += payload
    body += (0x0021).to_bytes(2, 'big')
    body += (6).to_bytes(2, 'big')
    body += (7).to_bytes(2, 'big')
    body += (0).to_bytes(2, 'big') * 4
    return bytes(body)


def self_check():
    names = referenced(leak_class())
    if 'gregtech/api/material/GTMaterial' not in names:
        raise SystemExit('closure self-check did not read the internal field type')
    allowed = {'com/example/Leak'}
    if permitted('gregtech/api/material/GTMaterial', 'core', allowed):
        raise SystemExit('closure self-check allowed an internal type')
    if not permitted('java/lang/Object', 'core', allowed):
        raise SystemExit('closure self-check rejected the JDK')
    if permitted('net/minecraft/world/item/ItemStack', 'core', allowed):
        raise SystemExit('closure self-check allowed Minecraft in core')
    if not permitted('net/minecraft/world/item/ItemStack', 'forge', allowed):
        raise SystemExit('closure self-check rejected Minecraft on Forge')
    if permitted('net/neoforged/neoforge/fluids/FluidStack', 'forge', allowed):
        raise SystemExit('closure self-check allowed NeoForge inside the Forge API')
    internal = 'gregtech/api/material/GTMaterial'
    for descriptor in ('Ljava/util/List<L' + internal + ';>;',
                       '<T:L' + internal + ';>Ljava/lang/Object;',
                       '<Legacy:L' + internal + ';>Ljava/lang/Object;'):
        if internal not in descriptor_types(descriptor):
            raise SystemExit('closure self-check missed a generic signature/bound internal type')
    if descriptor_types('Ljava/util/List<TLANG;>;') != ['java/util/List']:
        raise SystemExit('closure self-check confused a generic type variable with a class')


def main():
    self_check()
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='append', required=True)
    parser.add_argument('--allow', action='append', required=True)
    parser.add_argument('--kind', choices=('core', 'forge', 'neoforge'), required=True)
    parser.add_argument('--sample')
    parser.add_argument('--sdk')
    args = parser.parse_args()
    allowed = set()
    for directory in args.allow:
        allowed.update(class_names(directory))
    errors = []
    for directory in args.check:
        errors.extend(violations(directory, allowed, args.kind))
    if args.sample or args.sdk:
        if not (args.sample and args.sdk):
            parser.error('--sample and --sdk are used together')
        sample_names = class_names(args.sample)
        sdk_names = jar_classes(args.sdk)
        errors.extend(sdk_contents(args.sdk, allowed))
        errors.extend(violations(args.sample, sample_names | sdk_names, args.kind))
    if errors:
        raise SystemExit('Addon API closure failed:\n' + '\n'.join(errors[:40]))
    print(f'Addon API closure passed ({args.kind}): {len(allowed)} API classes')


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, zipfile.BadZipFile) as error:
        sys.exit(f'Addon API closure failed: {error}')
