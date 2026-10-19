"""Restore original bucket sprites and source language entries; source trees are read-only."""
import argparse
import hashlib
import json
import re
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--textures", type=Path, required=True)
    parser.add_argument("--chinese", type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    catalog = (root / "core/src/main/java/com/gregtech/gregtech/content/tool/WoodenBucketCatalog.java").read_text()
    bands = re.findall(r'new Band\("([^"]+)", "[^"]+", (\d+)\)', catalog)
    contents = re.findall(r'new Content\("([^"]+)", "([^"]+)", (\d+),', catalog)
    translations = dict(re.findall(r'^\s*S:([^=]+)=(.*)$', args.chinese.read_text(encoding="utf-8"), re.M))
    assets = root / "core/src/main/resources/assets/gt6ce"
    languages = {locale: json.loads((assets / f"lang/{locale}.json").read_text(encoding="utf-8"))
                 for locale in ("en_us", "zh_cn")}
    records = []
    groups = []
    for band, base in bands:
        for content, tooltip, offset in contents:
            number = int(base) + int(offset)
            identity = f"wooden_bucket_{band}" + ("" if content == "empty" else f"_{content}")
            key = f"gt.multiitem.randomtools.{number}"
            source = args.textures / f"{number}.png"
            data = source.read_bytes()
            target = assets / f"textures/item/wooden_buckets/{identity}.png"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
            model = assets / f"models/item/{identity}.json"
            model.write_text(json.dumps({"parent": "minecraft:item/generated", "textures": {
                "layer0": f"gt6ce:item/wooden_buckets/{identity}"}}, indent=2) + "\n", encoding="utf-8")
            en_key = f"item.gt6ce.{identity}"
            languages["en_us"][en_key] = "Wooden Bucket"
            languages["en_us"][en_key + ".tooltip"] = tooltip
            languages["zh_cn"][en_key] = translations[key]
            languages["zh_cn"][en_key + ".tooltip"] = translations[key + ".tooltip"]
            records.append({"item": identity, "original_meta": number, "source": str(source),
                            "sha256": hashlib.sha256(data).hexdigest(), "chinese_key": key})
    # Keep the different liquids separate, folding only the nine hoop materials.
    for content, _, _ in contents:
        group_key = f"emi.group.gt6ce.wooden_bucket_{content}"
        first = next(r for r in records if r["item"] == "wooden_bucket_copper" +
                     ("" if content == "empty" else f"_{content}"))
        for locale in languages:
            name = languages[locale]["item.gt6ce." + first["item"]]
            detail = languages[locale]["item.gt6ce." + first["item"] + ".tooltip"].split(" (")[0]
            languages[locale][group_key] = f"{name} · {detail}"
        group = assets / f"stack_groups/material_forms/functional_wooden_bucket_{content}.json"
        group.write_text(json.dumps({"type": "remi:group", "name": group_key, "contents": [
            "item:gt6ce:wooden_bucket_" + band + ("" if content == "empty" else f"_{content}")
            for band, _ in bands], "priority": 125}, indent=2) + "\n", encoding="utf-8")
        groups.append(str(group.relative_to(root)))
    for locale, language in languages.items():
        (assets / f"lang/{locale}.json").write_text(json.dumps(language, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    provenance = {"author": "Gregorius Techneticies / GregTech-6 Team", "asset_license": "CC0-1.0",
                  "preserved_catalog": "wolfram0108/gregtech6_w", "chinese_source": str(args.chinese),
                  "chinese_sha256": hashlib.sha256(args.chinese.read_bytes()).hexdigest(),
                  "items": records, "groups": groups}
    (root / "docs/integration/verification/wooden-buckets-source-20261019.json").write_text(
        json.dumps(provenance, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Restored {len(records)} source sprites/models, bilingual names/tooltips and {len(groups)} stack groups.")


if __name__ == "__main__":
    main()
