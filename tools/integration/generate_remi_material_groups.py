"""Generate optional Reliable EMI groups for material forms only, shared by both loaders."""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
# Bounded user scope: raw/processed material forms. Do not group tools, ammunition,
# plants, machines, decorative blocks, containers, rails or molds.
PREFIXES = """dust dustSmall dustTiny dustDiv72 dustImpure crushed crushedTiny
crushedPurified crushedPurifiedTiny crushedCentrifuged crushedCentrifugedTiny rockGt
 gemChipped gemFlawed gem gemFlawless gemExquisite gemLegendary bouleGt nugget chunkGt
billet ingot ingotHot ingotDouble ingotTriple ingotQuadruple ingotQuintuple plateGemTiny
plateGem plateTiny plate plateDouble plateTriple plateQuadruple plateQuintuple plateDense
plateCurved foil scrapGt oreRaw stick stickLong bolt screw gearGt gearGtSmall ring chain
spring springSmall rotor lens round itemCasing wireFine""".split()

def resources():
    for prefix in PREFIXES:
        path = re.sub(r"(?<!^)([A-Z])", r"_\1", prefix).lower()
        yield path, {
            "type": "remi:group", "name": "itemGroup.gregtech." + path,
            "contents": ["#gregtech:" + path],
            # Treated planks have the plate material prefix but are still decorative blocks.
            "exclusions": ["#minecraft:planks", "#gregtech:wooden_planks",
                           "item:gregtech:planks_treated"], "priority": 100,
        }

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    base = ROOT / "core/src/main/resources/assets/gregtech/stack_groups/material_forms"
    expected = dict(resources())
    languages = [json.loads((ROOT / f"core/src/main/resources/assets/gregtech/lang/{lang}.json").read_text(encoding="utf-8")) for lang in ("en_us", "zh_cn")]
    for path, group in expected.items():
        assert all(group["name"] in lang for lang in languages), group["name"]
        dest = base / (path + ".json")
        content = json.dumps(group, ensure_ascii=False, indent=2) + "\n"
        if args.check:
            assert dest.read_text(encoding="utf-8") == content, dest
        else:
            base.mkdir(parents=True, exist_ok=True)
            dest.write_text(content, encoding="utf-8")
    assert {p.stem for p in base.glob("*.json")} == set(expected), "Unexpected groups"
    print(f"{len(expected)} material-form groups {'checked' if args.check else 'generated'}; existing English/Chinese keys reused")

if __name__ == "__main__":
    main()
