# Test helper: original Git blobs are read-only; fresh GUID outputs stay under core/build.
from pathlib import Path
import hashlib
import json
import subprocess
import argparse
import os
import uuid

root = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description='Fresh original/current material-role fixtures; inert GameTest stubs, no game runtime.')
parser.add_argument('mode', choices=['original', 'current'])
parser.add_argument('--check', action='store_true', help='Assert four Neo material fixtures in original mode; current mode always asserts them.')
parser.add_argument('--jdk-home', default=os.environ.get('JAVA17_HOME') or os.environ.get('JAVA_HOME'))
args = parser.parse_args()
if not args.jdk_home:
    parser.error('Set JAVA17_HOME/JAVA_HOME or provide --jdk-home with a JDK17 installation.')
jdk = Path(args.jdk_home)
if not all((jdk / 'bin' / name).is_file() for name in ['java.exe','javac.exe','jdeps.exe']):
    parser.error('The JDK home must contain java.exe, javac.exe and jdeps.exe.')
mode = args.mode
check = args.check
scratch = root / 'core/build' / ('material-roles-' + mode + '-' + str(uuid.uuid4()))
sources = scratch / 'sources'
classes = scratch / 'classes'
classes.mkdir(parents=True)
material = json.loads((root / 'core/provenance/material-extractions.json').read_text(encoding='utf-8'))
base = json.loads((root / 'core/provenance/saltnya-extractions.json').read_text(encoding='utf-8'))
snapshot = material['source_snapshot']
roles = json.loads((root / 'core/provenance/material-role-extractions.json').read_text(encoding='utf-8'))

def blob(path):
    return subprocess.check_output(['git', '-C', str(root), 'cat-file', 'blob', snapshot + ':' + path])

def write(path, data):
    target = sources / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)

if mode == 'original':
    files = {entry['original_path']: entry for entry in material['files'] + base['files']}
    for path, entry in files.items():
        data = blob(path)
        expected = entry.get('source_sha256', entry.get('sha256'))
        if hashlib.sha256(data).hexdigest() != expected:
            raise AssertionError('Original material Git blob hash mismatch: ' + path)
        write(path.removeprefix('src/main/java/'), data)
    role_entries = {entry['original_path']: entry for entry in roles['files']}
    role_entries[roles['original_test_dependency']['path']] = {'source_sha256': roles['original_test_dependency']['sha256']}
    for path, entry in role_entries.items():
        data = blob(path)
        if hashlib.sha256(data).hexdigest() != entry['source_sha256']:
            raise AssertionError('Original role Git blob hash mismatch: ' + path)
        write(path.removeprefix('src/main/java/'), data)
    stubs = json.loads((root / 'core/src/test/fixtures/saltnya-boundary-stubs.json').read_text(encoding='utf-8'))
    stubs.update(json.loads((root / 'core/src/test/fixtures/material-role-boundary-stubs.json').read_text(encoding='utf-8')))
    for path, content in stubs.items():
        write(path, content.encode('utf-8'))
    write('OriginalMaterialRoleProbe.java', (root / 'core/src/test/fixtures/OriginalMaterialRoleProbe.java').read_bytes())
    main_class = 'OriginalMaterialRoleProbe'
elif mode == 'current':
    for path in (root / 'core/src/main/java').rglob('*.java'):
        write(path.relative_to(root / 'core/src/main/java'), path.read_bytes())
    stubs = json.loads((root / 'core/src/test/fixtures/material-role-boundary-stubs.json').read_text(encoding='utf-8'))
    # Current core compiles with NO game stubs/classpath; test annotations/helper are compile-only below.
    main_sources = sorted(sources.rglob('*.java'))
    main_classes = scratch / 'main-classes'
    main_classes.mkdir()
    subprocess.run([str(jdk / 'bin/javac.exe'), '--release', '17', '-encoding', 'UTF-8', '-d', str(main_classes)] + [str(p) for p in main_sources], check=True)
    dependencies = subprocess.check_output([str(jdk / 'bin/jdeps.exe'), '--multi-release', '17', '--recursive', '-s', str(main_classes)], text=True)
    print(dependencies, end='')
    if not dependencies.strip() or any(not line.endswith(' -> java.base') for line in dependencies.splitlines()):
        raise AssertionError('Current main core must depend on java.base only')
    for path, content in stubs.items():
        if '/gametest/' in path:
            write(path, content.encode('utf-8'))
    write('CurrentMaterialRoleProbe.java', b'''import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.platform.neoforge.gametest.SharedMaterialBootstrapGameTests;
public final class CurrentMaterialRoleProbe {
 public static void main(String[] args) throws Exception {
  java.util.Locale.setDefault(java.util.Locale.ROOT);
  ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gregtech"));
  GTMaterialRegistry.setLogSink((warning,message)->{});
  PrefixRegistry.ensurePrefixesLoaded();
  com.gregtech.gregtech.data.ModReferences.UNKNOWN.getClass();
  com.gregtech.gregtech.data.MaterialGroups.Glowstone.getClass();
  GTMaterialRegistry.init(); MaterialRoleFlags.apply(); GTMaterialRegistry.postInit();
  System.out.println("role_sha256="+SharedMaterialBootstrapGameTests.roleSnapshotSha256());
  var helper=new net.minecraft.gametest.framework.GameTestHelper();
  SharedMaterialBootstrapGameTests.completeMaterialDirectory(helper);
  SharedMaterialBootstrapGameTests.originalRolesUnitsAndComposition(helper);
  SharedMaterialBootstrapGameTests.originalCopperTinReaction(helper);
  SharedMaterialBootstrapGameTests.originalAlternateReactions(helper);
  System.out.println("Four shared-material fixtures passed against CURRENT core under inert GameTest stubs.");
 }
}''')
    main_class = 'CurrentMaterialRoleProbe'
else:
    raise ValueError(mode)

write('com/gregtech/gregtech/platform/neoforge/gametest/SharedMaterialBootstrapGameTests.java',
      (root / 'neoforge/src/bootstrapGameTest/java/com/gregtech/gregtech/platform/neoforge/gametest/SharedMaterialBootstrapGameTests.java').read_bytes())
inputs = sorted(sources.rglob('*.java'))
subprocess.run([str(jdk / 'bin/javac.exe'), '--release', '17', '-encoding', 'UTF-8', '-d', str(classes)] + [str(p) for p in inputs], check=True)
subprocess.run([str(jdk / 'bin/java.exe'), '-cp', str(classes), main_class] + (['check'] if check else []), check=True)
print('fresh_scratch=' + str(scratch), flush=True)
