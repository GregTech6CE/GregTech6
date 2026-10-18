"""Capture real large-crucible hull vertex UVs without starting Minecraft or a GPU."""

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
cp = (ROOT / 'multiversion/versions/1.20.1-forge/build/classpath/runGameTestServer_minecraftClasspath.txt').read_text(encoding='utf-8').strip().replace('\n', os.pathsep)
cp += os.pathsep + str(ROOT / 'multiversion/versions/1.20.1-forge/build/classes/java/main')
with tempfile.TemporaryDirectory(prefix='gt6-crucible-uv-') as folder:
    args=Path(folder)/'javac.args'
    sources=[native_path(ROOT/'src/main/java/com/gregtech/gregtech/client/LargeCrucibleHullFace.java'), native_path(ROOT/'src/test/java/com/gregtech/gregtech/client/LargeCrucibleUvContracts.java')]
    args.write_text('\n'.join('"'+str(x).replace('\\','/')+'"' for x in ['-proc:none','-cp',cp,'-d',folder,*sources]),encoding='utf-8')
    subprocess.run(['javac','@'+str(args)],check=True,cwd=ROOT)
    subprocess.run(['java','-cp',folder+os.pathsep+cp,'com.gregtech.gregtech.client.LargeCrucibleUvContracts'],check=True,cwd=ROOT)
