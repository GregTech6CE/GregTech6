"""Index original GT6 processing-target references without modifying its source.

This is a navigation aid, not a parity check. A matching identifier does not prove
that a recipe or interaction is reachable; review the recorded call sites.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from check_source import java_source_view


KINDS = ("Solidifying", "Bending", "Compressing", "Cutting", "Forging", "Smashing", "Working")
METADATA_FILES = {
    "gregapi/oredict/OreDictMaterial.java",
    "gregapi/util/UT.java",
}


def inventory(source_root: Path) -> dict:
    references = {kind: [] for kind in KINDS}
    sources = {}
    pattern = re.compile(r"\bmTarget(" + "|".join(KINDS) + r")\b")
    for path in sorted(source_root.rglob("*.java")):
        raw = path.read_bytes()
        text = raw.decode("utf-8-sig")
        if not pattern.search(text):
            continue
        code = java_source_view(text).code
        relative = path.relative_to(source_root).as_posix()
        found = False
        for line_number, line in enumerate(code.splitlines(), 1):
            for match in pattern.finditer(line):
                references[match[1]].append({
                    "path": relative,
                    "line": line_number,
                    "code": line.strip(),
                    "review_role": "definition/copy/display" if relative in METADATA_FILES
                    else "behavior/condition candidate; inspect caller",
                })
                found = True
        if found:
            sources[relative] = hashlib.sha256(raw).hexdigest()
    return {
        "scope": "Seven original processing-target fields, excluding comments and strings. References are not proof of runtime reachability or port parity.",
        "source_root": str(source_root.resolve()),
        "source_sha256": sources,
        "references": references,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", type=Path, required=True, help="Original src/main/java directory")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if not (args.source_root / "gregapi/oredict/OreDictMaterial.java").is_file():
        parser.error("--source-root must contain gregapi/oredict/OreDictMaterial.java")
    output = args.output.resolve()
    if output.is_relative_to(args.source_root.resolve()):
        parser.error("The report must be outside the preserved original source tree")
    report = inventory(args.source_root)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({kind: len(rows) for kind, rows in report["references"].items()}))


if __name__ == "__main__":
    main()
