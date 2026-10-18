"""Run the production JEI scissor calculation with translated recipe poses, without starting Minecraft."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
cache = Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle')) / 'caches/modules-2/files-2.1/org.joml/joml'
joml = next(p for p in cache.rglob('joml-1.10.5.jar'))
package = 'com/gregtech/gregtech/jei'
jdk = next((line.split('=', 1)[1].strip().replace('\\\\', '\\')
            for line in (ROOT / 'gradle.properties').read_text().splitlines()
            if line.startswith('org.gradle.java.home=')), os.environ.get('JAVA_HOME'))
def executable(name):
    return str(Path(jdk) / 'bin' / name) if jdk else name

with tempfile.TemporaryDirectory(prefix='gt6-preview-test-') as directory:
    subprocess.run([executable('javac'), '--release', '17', '-cp', str(joml), '-d', directory,
                    str(native_path(ROOT / f'src/main/java/{package}/PreviewViewport.java')),
                    str(native_path(ROOT / f'src/test/java/{package}/PreviewViewportContracts.java'))], check=True)
    subprocess.run([executable('java'), '-cp', directory + os.pathsep + str(joml),
                    'com.gregtech.gregtech.jei.PreviewViewportContracts'], check=True)
