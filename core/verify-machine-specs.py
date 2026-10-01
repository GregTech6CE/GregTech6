"""Compile original/current record fixtures with JDK17 and java.base only; no Gradle or game."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import subprocess
import uuid

root = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--jdk-home', default=os.environ.get('JAVA17_HOME') or os.environ.get('JAVA_HOME'))
args = parser.parse_args()
if not args.jdk_home:
    parser.error('Set JAVA17_HOME/JAVA_HOME or provide --jdk-home with a JDK17 installation.')
jdk = Path(args.jdk_home)
if not all((jdk / 'bin' / name).is_file() for name in ('java.exe', 'javac.exe', 'javap.exe', 'jdeps.exe')):
    parser.error('The JDK home must contain java.exe, javac.exe, javap.exe and jdeps.exe.')
metadata = json.loads((root / 'core/provenance/machine-spec-extractions.json').read_text(encoding='utf-8'))
scratch = root / 'core/build' / ('machine-specs-' + str(uuid.uuid4()))
main_classes, test_classes, original_classes = [scratch / name for name in ('main-classes', 'test-classes', 'original-classes')]
for target in (main_classes, test_classes, original_classes):
    target.mkdir(parents=True)

def run(command):
    result = subprocess.run([str(part) for part in command], cwd=root, text=True,
                            encoding='utf-8', errors='replace', stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    print(result.stdout, end='')
    result.check_returncode()
    return result.stdout

def compile_sources(paths, target, classpath=None):
    argument_file = scratch / (target.name + '.args')
    argument_file.write_text('\n'.join('"' + Path(path).as_posix() + '"' for path in paths), encoding='utf-8')
    command = [jdk / 'bin/javac.exe', '-J-Duser.language=en', '-J-Duser.country=US', '--release', '17', '--limit-modules', 'java.base',
               '-encoding', 'UTF-8', '-d', target]
    if classpath:
        command += ['-classpath', classpath]
    command += ['@' + str(argument_file)]
    run(command)

if not run([jdk / 'bin/javac.exe', '--version']).strip().startswith('javac 17.'):
    raise RuntimeError('This independent probe requires an actual JDK17 compiler')
original_sources = []
for entry in metadata['files']:
    source = subprocess.check_output(['git', '-C', str(root), 'cat-file', 'blob',
                                      metadata['source_snapshot'] + ':' + entry['original_path']])
    current = (root / entry['path']).read_bytes()
    if (root / entry['original_path']).exists():
        raise RuntimeError('Duplicate Forge source record remains: ' + entry['original_path'])
    if source != current or hashlib.sha256(source).hexdigest() != entry['original_sha256']:
        raise RuntimeError('Exact original record relocation failed: ' + entry['path'])
    original_path = scratch / 'original-sources' / entry['original_path']
    original_path.parent.mkdir(parents=True, exist_ok=True)
    original_path.write_bytes(source)
    original_sources.append(original_path)

sources = subprocess.check_output(['rg', '--files', 'core/src/main/java', '-g', '*.java'], cwd=root).decode().splitlines()
compile_sources([root / path for path in sources], main_classes)
tests = subprocess.check_output(['rg', '--files', 'core/src/test/java', '-g', '*.java'], cwd=root).decode().splitlines()
compile_sources([root / path for path in tests], test_classes, str(main_classes))
compile_sources(original_sources, original_classes, str(main_classes))
modules = run([jdk / 'bin/jdeps.exe', '--multi-release', '17', '--print-module-deps', main_classes]).strip()
if modules != 'java.base':
    raise RuntimeError('Unexpected production core module dependencies: ' + modules)
for name in ('MachineSpec', 'CrucibleSpec'):
    class_name = 'com.gregtech.gregtech.api.machine.' + name
    signatures = []
    for compiled in (original_classes, main_classes):
        result = subprocess.check_output([str(jdk / 'bin/javap.exe'), '-public', '-s',
                                          '-classpath', str(compiled), class_name], cwd=root)
        signatures.append(result)
    if signatures[0] != signatures[1]:
        raise RuntimeError('Original/current public API descriptors differ: ' + name)
print('Original/current public record API descriptors match')

probe = 'com.gregtech.gregtech.core.MachineSpecBehaviorContracts'
outputs = []
for mode, roots in [('original', [original_classes, main_classes, test_classes]),
                    ('current', [main_classes, test_classes])]:
    print('FIXTURE ' + mode)
    outputs.append(run([jdk / 'bin/java.exe', '--limit-modules', 'java.base', '-cp',
                        os.pathsep.join(map(str, roots)), probe]))
if outputs[0] != outputs[1]:
    raise RuntimeError('Original/current fixture receipts differ')
for contract in ('CoreBehaviorContracts', 'MaterialBehaviorContracts', 'ThermalBehaviorContracts'):
    run([jdk / 'bin/java.exe', '--limit-modules', 'java.base', '-cp',
         os.pathsep.join(map(str, [main_classes, test_classes])), 'com.gregtech.gregtech.core.' + contract])
print('PASS original/current exact records and public API; java.base production closure; existing core/material/thermal contracts')
print('Fresh isolated output: ' + str(scratch))
