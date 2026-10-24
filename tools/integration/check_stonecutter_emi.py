"""Prove Stonecutter migration preserves both native EMI implementations."""
from pathlib import Path
import hashlib
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(Path(__file__).resolve().parent))
from check_source import java_source_view


def signature(source):
    view = java_source_view(source)
    return re.sub(r"\s+", "", view.code), [value for _, value in view.literals]


def main():
    manifest = json.loads((ROOT / "multiversion/emi-migration.json").read_text())
    records = {}
    package = "gregtech/emi"
    for platform, node, prefix in (("forge", "1.20.1-forge", "src"),
                                    ("neoforge", "1.21.1-neoforge", "neoforge/src")):
        output = ROOT / f"multiversion/versions/{node}/build/generated/stonecutter/main/java/{package}"
        expected = {row["class"] + ".java" for row in manifest["classes"]}
        assert {file.name for file in output.glob("*.java")} == expected, f"Incomplete {platform} output"
        assert not list((ROOT / prefix / "main/java" / package).glob("*.java")), "Duplicate native sources"
        for row in manifest["classes"]:
            filename = row["class"] + ".java"
            generated = (output / filename).read_text(encoding="utf-8")
            tokens = json.dumps(signature(generated), ensure_ascii=False, separators=(',', ':')).encode()
            assert hashlib.sha256(tokens).hexdigest() == row["native_signature"][platform], \
                f"Changed native code: {platform}/{filename}"
        records[platform] = {"classes": len(expected), "native_code_and_literals_unchanged": True}
    report = {"baseline_commit": manifest["baseline_commit"], "status": "passed", "platforms": records}
    destination = ROOT / "build/reports/stonecutter/emi-source-parity.json"
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report))


if __name__ == "__main__":
    main()
