"""Compare explicit recipe-map constructor flags by internal name.

This is a source inventory, not a runtime recipe check. Only single-line constructor
rows with literal trailing flags are supported; other constructor sites are reported.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re

from check_source import java_source_view


FLAGS = ("needs_outputs", "combine_power", "bucket_size_in", "bucket_size_out")


def read_rows(path: Path, original: bool) -> dict:
    raw = path.read_bytes()
    text = raw.decode("utf-8-sig")
    code_lines = java_source_view(text).code.splitlines()
    kind = r"RecipeMap\w*" if original else "RecipeMapSpec"
    site = re.compile(r"\bnew\s+" + kind + r"\s*\(")
    identity = re.compile(r"\bnew\s+" + kind + r'\s*\(\s*null\s*,\s*"([^"]+)"')
    flag = r"([TF])" if original else r"(true|false)"
    count = 7 if original else 4
    tail = re.compile(r",\s*" + r"\s*,\s*".join([flag] * count) + r"\s*\)")
    rows, unsupported = {}, []
    for line_number, (line, code) in enumerate(zip(text.splitlines(), code_lines), 1):
        if not site.search(code):
            continue
        name, values = identity.search(line), tail.search(code)
        if name is None or values is None:
            unsupported.append({"line": line_number, "text": line.strip()})
            continue
        key = name[1]
        if key in rows:
            raise ValueError(f"Duplicate map identity {key} in {path}")
        rows[key] = {
            "line": line_number,
            "flags": dict(zip(FLAGS, [value in ("T", "true") for value in values.groups()[-4:]])),
        }
    return {"path": str(path.resolve()), "sha256": hashlib.sha256(raw).hexdigest(),
            "rows": rows, "unsupported_sites": unsupported}


def inventory(source: Path, current: Path) -> dict:
    original = read_rows(source, True)
    port = read_rows(current, False)
    common = original["rows"].keys() & port["rows"].keys()
    differences = []
    for name in sorted(common):
        before, after = original["rows"][name], port["rows"][name]
        changed = [field for field in FLAGS if before["flags"][field] != after["flags"][field]]
        if changed:
            differences.append({"map": name, "changed": changed, "original": before, "current": after})
    return {"scope": __doc__.strip(), "original": original, "current": port,
            "compared_maps": len(common), "differences": differences,
            "original_only": sorted(original["rows"].keys() - port["rows"].keys()),
            "current_only": sorted(port["rows"].keys() - original["rows"].keys())}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--current", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.resolve() in (args.source.resolve(), args.current.resolve()):
        parser.error("Output must not overwrite either source")
    report = inventory(args.source, args.current)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"compared": report["compared_maps"], "different": len(report["differences"]),
                      "unsupported_original": len(report["original"]["unsupported_sites"]),
                      "unsupported_current": len(report["current"]["unsupported_sites"])}))


if __name__ == "__main__":
    main()
