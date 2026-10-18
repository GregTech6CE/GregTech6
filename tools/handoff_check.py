"""Probe: is the port's working tree still green (numbers a fresh agent needs at hand-off)?

Reads only local artifacts: the last gate log, the progress snapshot and the multiblock part table.
Run from the repository root: python tools/handoff_check.py
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import io
import json
import os
import re

LOG = os.path.join("build", "gametest.log")
JAR = os.path.join("build", "libs", "gregtech-1.0.0.jar")
PROGRESS = os.path.join("docs", "porting-progress.json")
PARTS = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "content", "multiblock",
                     "LargeMachineParts.java")


def main():
    if os.path.exists(LOG):
        text = io.open(LOG, encoding="utf-8", errors="replace").read()
        for line in text.splitlines():
            if "required tests" in line or "GAME TESTS COMPLETE" in line or "failed!" in line:
                print("gate:", line.strip()[:120])
        print("gate log mtime:", __import__("time").ctime(os.path.getmtime(LOG)))
    else:
        print("gate: no build/gametest.log yet")

    if os.path.exists(PROGRESS):
        data = json.load(io.open(PROGRESS, encoding="utf-8"))
        print("registry:", json.dumps(data.get("registry", {}))[:160])
        print("missingContent:", json.dumps(data.get("missingContent", {})))

    if os.path.exists(PARTS):
        parts = io.open(PARTS, encoding="utf-8", errors="replace").read()
        print("multiblock parts:", len(re.findall(r"new Part\(", parts)))

    if os.path.exists(JAR):
        import hashlib
        digest = hashlib.sha256(io.open(JAR, "rb").read()).hexdigest()
        print("jar: %s  %.1f MB  sha256=%s" % (JAR, os.path.getsize(JAR) / 1048576.0, digest.upper()))
    else:
        print("jar: not built yet")

    java_newest = 0
    for root, _dirs, files in os.walk(os.path.join(str(native_path("src")), "main", "java")):
        for name in files:
            if name.endswith(".java"):
                java_newest = max(java_newest, os.path.getmtime(os.path.join(root, name)))
    if os.path.exists(LOG):
        print("java newest < gate log:", java_newest < os.path.getmtime(LOG))
    if os.path.exists(JAR):
        print("gate log < jar:", (not os.path.exists(LOG)) or os.path.getmtime(LOG) < os.path.getmtime(JAR))


if __name__ == "__main__":
    main()
