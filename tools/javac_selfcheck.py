"""Fast javac self-check for changed sources, without gradle.

The whole point is to catch syntax/type errors in seconds instead of the ~60 s a gradle compile
costs. Two Windows traps this tool exists to avoid:

  * **The classpath is longer than a ``cmd`` command line.** ``runGameTestServer``'s minecraft
    classpath is ~17 KB; ``cmd /c "javac -cp <that> ..."`` dies with "The command line is too
    long." (PowerShell handles it, but ``-J`` flags get shredded there). javac's ``@argfile``
    syntax has no such limit - only ``-J`` options cannot go into the argfile, so those stay on
    the command line.
  * **A trailing comma in a Java argument list is a syntax error** (unlike an array initializer).
    A generator that joins rows with "," and then closes with ");" produces
    "illegal start of expression" pointing at the closing paren - see
    ``tools/extract_gt6_sensor_recipes.py``, where this bit once.

Usage:
  python tools/javac_selfcheck.py                            # check the default file set below
  python tools/javac_selfcheck.py <file.java> [<file.java>]  # check exactly these files

Exit code 0 means javac accepted every file. Compiled classes go to ``build/tmp/javac-selfcheck``
so nothing in ``build/classes`` is touched; a directory that gradle does not manage, so the gate's
"java sources newest < gate log" invariant is unaffected.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import glob
import io
import os
import subprocess
import sys

CLASSPATH_FILE = os.path.join('build', 'classpath', 'runGameTestServer_minecraftClasspath.txt')
OUT_DIR = os.path.join('build', 'tmp', 'javac-selfcheck')
ARGFILE = os.path.join('build', 'tmp', 'javac-selfcheck-args.txt')

# build.gradle's compileOnly mod APIs (Jade, JEI) are not on the runtime classpath the file above
# records, so a source that names one of their types fails with "cannot access <type>: class file
# for <type> not found" even though gradle compiles it fine. Pull them from the gradle cache.
EXTRA_JAR_GLOBS = [
    os.path.join(os.path.expanduser('~'), '.gradle', 'caches', 'modules-2', 'files-2.1',
                 'maven.modrinth', '**', '*.jar'),
    os.path.join(os.path.expanduser('~'), '.gradle', 'caches', 'modules-2', 'files-2.1',
                 'mezz.jei', '**', '*.jar'),
]

# Handy default: the round-109 sensor recipe files. Pass paths explicitly for anything else.
DEFAULT_SOURCES = [
    os.path.join(str(native_path('src')), 'main', 'java', 'com', 'gregtech', 'gregtech', 'data', 'SensorRecipePack.java'),
    os.path.join(str(native_path('src')), 'main', 'java', 'com', 'gregtech', 'gregtech', 'data', 'generated',
                 'GTSensorRecipesGen.java'),
    os.path.join(str(native_path('src')), 'main', 'java', 'com', 'gregtech', 'gregtech', 'gametest',
                 'SensorMatrixTests.java'),
]


def main(argv):
    sources = argv or DEFAULT_SOURCES
    missing = [s for s in sources if not os.path.exists(s)]
    if missing:
        print('missing source files: %s' % missing)
        return 2
    if not os.path.exists(CLASSPATH_FILE):
        print('missing %s - run compileJava once so gradle writes it' % CLASSPATH_FILE)
        return 2

    classpath = io.open(CLASSPATH_FILE, encoding='utf-8').read().strip()
    classpath = ';'.join(classpath.splitlines())
    classpath += ';' + os.path.join('build', 'classes', 'java', 'main')
    extras = []
    for pattern in EXTRA_JAR_GLOBS:
        extras.extend(glob.glob(pattern, recursive=True))
    extras = sorted(set(extras))
    for jar in extras:
        classpath += ';' + jar
    os.makedirs(OUT_DIR, exist_ok=True)

    args = ['-nowarn', '-proc:none', '-implicit:none', '-encoding', 'UTF-8',
            '-cp', classpath, '-d', OUT_DIR] + sources
    io.open(ARGFILE, 'w', encoding='utf-8', newline='\n').write('\n'.join(args) + '\n')

    proc = subprocess.run(['javac', '-J-Duser.language=en', '@' + ARGFILE],
                          capture_output=True)
    out = (proc.stdout or b'').decode('utf-8', 'replace') + \
          (proc.stderr or b'').decode('utf-8', 'replace')
    print('javac exit %d for %d file(s), %d extra jar(s) on the classpath'
          % (proc.returncode, len(sources), len(extras)))
    if out.strip():
        print(out.strip())
    return proc.returncode


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
