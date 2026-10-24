"""Generate optional Reliable EMI index/search groups, shared by both loaders."""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
# Keep existing group IDs stable (REMI persists per-group preferences).
PREFIXES = """dust dustSmall dustTiny dustDiv72 dustImpure crushed crushedTiny
crushedPurified crushedPurifiedTiny crushedCentrifuged crushedCentrifugedTiny rockGt
 gemChipped gemFlawed gem gemFlawless gemExquisite gemLegendary bouleGt nugget chunkGt
billet ingot ingotHot ingotDouble ingotTriple ingotQuadruple ingotQuintuple plateGemTiny
plateGem plateTiny plate plateDouble plateTriple plateQuadruple plateQuintuple plateDense
plateCurved foil scrapGt oreRaw stick stickLong bolt screw gearGt gearGtSmall ring chain
spring springSmall rotor lens round itemCasing wireFine""".split()

JAVA = ROOT / "core/src/main/java/gregtech"
ASSETS = ROOT / "core/src/main/resources/assets/gt6ce"

def additional_prefixes():
    items = (JAVA / "api/material/MaterialItemDefinitions.java").read_text(encoding="utf-8")
    blocks = (JAVA / "api/prefix/BlockMaterialPrefix.java").read_text(encoding="utf-8")
    # Tool heads are components, not the finished tools with independent stack data.
    candidates = items.split("return List.of(", 1)[1].split(");", 1)[0]
    yield from (p for p in dict.fromkeys(re.findall(r"MaterialPrefix\.(\w+)", candidates)) if p not in PREFIXES)
    yield from re.findall(r"public static BlockMaterialPrefix (\w+);", blocks)

def transport_groups():
    for kind, sizes in (("wire", range(1, 17)), ("cable", (1, 2, 4, 8, 12))):
        for size in sizes:
            yield f"{kind}_{size:02d}", "copper", ("Copper", "铜")
    for size in ("tiny", "small", "medium", "large", "huge", "quadruple", "nonuple"):
        yield f"pipe_{size}", "copper", ("Copper", "铜")
    for size in ("medium", "large", "huge", "restrictive_medium", "restrictive_large", "restrictive_huge"):
        yield f"item_pipe_{size}", "brass", ("Brass", "黄铜")

def resources():
    for prefix in [*PREFIXES, *additional_prefixes()]:
        path = re.sub(r"(?<!^)([A-Z])", r"_\1", prefix).lower()
        yield path, {
            "type": "remi:group", "name": "itemGroup.gt6ce." + path,
            "contents": ["#gt6ce:" + path],
            # Treated planks have the plate material prefix but are still decorative blocks.
            "exclusions": ["#minecraft:planks", "#gt6ce:wooden_planks",
                           "item:gt6ce:planks_treated"] + (
                               ["item:minecraft:glass_pane", "item:minecraft:paper", "item:gt6ce:plate_paper"]
                               if path == "plate" else []), "priority": 100,
        }
    for path, material, _ in transport_groups():
        # Crafting wire/pipe tags only cover selected metals. Match the exact native
        # registry shape here instead of widening those recipe ingredients.
        yield path, {"type": "remi:group", "name": f"block.gt6ce.{path}_{material}",
                     "regex": "^gt6ce:" + path + "_[a-z0-9_]+$", "priority": 100}
    catalog = (JAVA / "content/creative/SourceCreativeCatalog.java").read_text(encoding="utf-8")
    # Keep combination safes and key safes separate; both appear in the Safes tab.
    safe_ids = re.findall(r'ITEMS.put\("([^"]+)",new Entry\("safes",\d+\)\)', catalog)
    for prefix in ("safe_", "key_safe_"):
        ids = sorted(item for item in safe_ids if item.startswith(prefix) or item == prefix.rstrip("_"))
        assert len(ids) > 1, prefix
        path = prefix.rstrip("_")
        yield path, {"type": "remi:group", "name": "itemGroup.gt6ce.safes",
                     "contents": ["item:gt6ce:" + item for item in ids], "priority": 100}
    from remi_functional_groups import resources as functional_resources
    yield from functional_resources(ROOT)
    yield "ores", {"type": "remi:group", "name": "itemGroup.gt6ce.ore", "regex": "^item:gt6ce:(?:ore_|ore_small_|ore_broken_|ore_bedrock_)[a-z0-9_]+$", "priority": 110}


def titled_resources():
    from remi_group_titles import headings
    language = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))
    titles = headings(ROOT, language)
    for path, group in resources():
        title = "emi.group.gt6ce." + path
        if title in titles:
            group["name"] = title
        yield path, group

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    base = ROOT / "core/src/main/resources/assets/gt6ce/stack_groups/material_forms"
    expected = dict(titled_resources())
    languages = []
    for index, lang in enumerate(("en_us", "zh_cn")):
        file = ASSETS / f"lang/{lang}.json"
        language = json.loads(file.read_text(encoding="utf-8"))
        from remi_group_titles import headings
        for key, value in headings(ROOT, language).items():
            if args.check:
                assert language.get(key) == value, key
            else:
                language[key] = value
        if not args.check:
            file.write_text(json.dumps(dict(sorted(language.items())), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        languages.append(language)
    for path, group in expected.items():
        assert group["name"] in {"block.minecraft.glass_pane", "item.minecraft.paper"} or all(
                group["name"] in lang for lang in languages), group["name"]
        dest = base / (path + ".json")
        content = json.dumps(group, ensure_ascii=False, indent=2) + "\n"
        if args.check:
            assert dest.read_text(encoding="utf-8") == content, dest
        else:
            base.mkdir(parents=True, exist_ok=True)
            dest.write_text(content, encoding="utf-8")
    assert {p.stem for p in base.glob("*.json")} == set(expected), "Unexpected groups"
    print(f"{len(expected)} index/search groups {'checked' if args.check else 'generated'}; source-aligned English/Chinese names")

if __name__ == "__main__":
    main()
