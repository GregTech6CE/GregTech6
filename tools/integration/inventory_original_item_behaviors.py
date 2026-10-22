"""Locate original item-behavior consumers, excluding comments/imports and separating literal hints.

This is a source inventory, never a claim that a referenced behavior is registered,
ported or fully working. In particular, legacy no-op methods still need inspection.
"""
from pathlib import Path
import argparse
import hashlib
import json
import re

from check_source import java_source_view


IMPORT = re.compile(r"\bimport\s+(?:static\s+)?[\w.$*\s]+;")
REFERENCE = re.compile(r"\bBehavior_[A-Za-z0-9_]+\b")


def sha256(raw):
    return hashlib.sha256(raw).hexdigest()


def inventory(original):
    definitions = {}
    behavior_root = original / "gregtech/items/behaviors"
    for path in sorted(behavior_root.glob("Behavior_*.java")):
        raw = path.read_bytes()
        code = java_source_view(raw.decode("utf-8-sig")).code
        if not re.search(r"\bclass\s+" + re.escape(path.stem) + r"\b", code):
            raise ValueError(f"No matching class declaration: {path}")
        definitions[path.stem] = {
            "class": path.stem,
            "path": path.relative_to(original).as_posix(),
            "sha256": sha256(raw),
            "code_references": [],
            "literal_hints": [],
        }
    if not definitions:
        raise ValueError(f"No behavior classes found: {behavior_root}")

    inputs = []
    for path in sorted(original.rglob("*.java")):
        raw = path.read_bytes()
        relative = path.relative_to(original).as_posix()
        digest = sha256(raw)
        inputs.append((relative, digest))
        view = java_source_view(raw.decode("utf-8-sig"))
        # Keep newlines for source locations while removing otherwise misleading imports.
        code = IMPORT.sub(lambda match: re.sub(r"[^\r\n]", " ", match[0]), view.code)
        for match in REFERENCE.finditer(code):
            row = definitions.get(match[0])
            if row is None or relative == row["path"]:
                continue
            prefix = code[max(0, match.start() - 256):match.start()]
            if re.search(r"\bnew\s+(?:[\w$]+\s*\.\s*)*$", prefix):
                kind = "construction"
            elif re.search(r"\bextends\s+(?:[\w$]+\s*\.\s*)*$", prefix):
                kind = "inheritance"
            else:
                kind = "type_or_static_reference"
            row["code_references"].append({
                "path": relative, "sha256": digest,
                "line": code.count("\n", 0, match.start()) + 1, "kind": kind,
            })
        for line, literal in view.literals:
            for name, row in definitions.items():
                variants = (name, name.replace("Behavior_", "Behaviour_", 1))
                if relative != row["path"] and any(value in literal for value in variants):
                    row["literal_hints"].append({
                        "path": relative, "sha256": digest, "line": line,
                        "literal": literal,
                        "meaning": "String hint only; spelling, package and target existence need review.",
                    })

    rows = list(definitions.values())
    for row in rows:
        row["classification"] = (
            "code_reference_found" if row["code_references"]
            else "literal_hint_only" if row["literal_hints"]
            else "no_external_reference_found"
        )
    return {
        "schema": 1,
        "original_root": original.as_posix(),
        "scope": "gregtech/items/behaviors/Behavior_*.java and external references in the supplied Java tree.",
        "meaning": "Locator only. Neither class names nor references prove runtime registration or port parity.",
        "line_basis": "Java Unicode-expanded code view; comments/imports/literals excluded from code matches.",
        "source_input_count": len(inputs),
        "source_input_identity_sha256": sha256(json.dumps(inputs, separators=(",", ":")).encode()),
        "behavior_count": len(rows),
        "classes": rows,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--original", type=Path, required=True,
                        help="Original src/main/java containing gregtech and gregapi")
    parser.add_argument("--output", type=Path, required=True, help="Destination JSON outside the original tree")
    args = parser.parse_args()
    original = args.original.resolve()
    if not (original / "gregtech").is_dir() or not (original / "gregapi").is_dir():
        parser.error("--original must contain gregtech and gregapi")
    output = args.output.resolve()
    if output.is_relative_to(original) or output.suffix.lower() != ".json":
        parser.error("--output must be a .json path outside the read-only original tree")
    report = inventory(original)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "source_inputs": report["source_input_count"],
        "behavior_classes": report["behavior_count"],
        "classifications": {
            name: sum(row["classification"] == name for row in report["classes"])
            for name in ("code_reference_found", "literal_hint_only", "no_external_reference_found")
        },
    }))


if __name__ == "__main__":
    main()
