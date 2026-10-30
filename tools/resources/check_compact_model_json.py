"""Verify a compact release against processed resource roots (stdlib only).

Usage: python tools/resources/check_compact_model_json.py MOD.jar --resources CORE_OUTPUT NODE_OUTPUT
Checks model/blockstate semantics, every PNG path/byte, and template expansion.
This verifies packaging, not Minecraft's rendered appearance.
"""

import argparse
import json
import zlib
from pathlib import Path
from zipfile import ZipFile

INDEX = "META-INF/gt6ce/model-json-aliases.json"


def model_json(path):
    return path.endswith(".json") and path.startswith(
        ("assets/gt6ce/models/", "assets/gt6ce/blockstates/")
    )


def verify(jar, roots):
    expected = {}
    for root in roots:
        for file in root.rglob("*"):
            if not file.is_file():
                continue
            path = file.relative_to(root).as_posix()
            if not (model_json(path) or path.endswith(".png")):
                continue
            if path in expected:
                raise ValueError(f"Duplicate input: {path}")
            expected[path] = file.read_bytes()
    with ZipFile(jar) as archive:
        names = set(archive.namelist())
        if len(names) != len(archive.infolist()):
            raise ValueError("Duplicate ZIP paths")
        index_data = json.loads(archive.read(INDEX))
        assert index_data['version'] == 1
        aliases = {}
        for group in index_data['groups']:
            segments = group['segments']
            for parameters, paths in group['variants']:
                assert len(segments) == len(parameters) + 1
                text = segments[0] + ''.join(json.dumps(value, ensure_ascii=False) + tail
                                             for value, tail in zip(parameters, segments[1:]))
                payload = text.encode('utf-8')
                for path in paths:
                    assert model_json(path) and path not in aliases and path not in names, path
                    assert path in expected and json.loads(expected[path]) == json.loads(payload), path
                    aliases[path] = payload
        for path, payload in expected.items():
            actual = aliases[path] if path in aliases else archive.read(path)
            if path in aliases:
                assert json.loads(actual) == json.loads(payload), f"JSON changed: {path}"
            else:
                assert actual == payload, f"Resource bytes changed: {path}"
        actual_paths = {p for p in names if model_json(p) or p.endswith('.png')} | aliases.keys()
        assert actual_paths == expected.keys(), "Unexpected or missing resource paths"
        # Estimate omitted payloads and basic ZIP headers without a second build.
        # Writer-specific extra fields and data descriptors are not included.
        saved = sum(len(zlib.compress(expected[path])) - 6 + 76 + 2 * len(path.encode('utf-8'))
                    for path in aliases)
        index = archive.getinfo(INDEX)
        saved -= index.compress_size + 76 + 2 * len(INDEX)
        print(f"{jar.name}: {len(aliases)} generated JSON paths; {len(expected)} resources verified")
        print(f"PNG unchanged: {sum(p.endswith('.png') for p in expected)}")
        print(f"JAR: {jar.stat().st_size:,} bytes; estimated saving: {saved:,} bytes")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path)
    parser.add_argument("--resources", nargs="+", type=Path, required=True)
    args = parser.parse_args()
    verify(args.jar, args.resources)
