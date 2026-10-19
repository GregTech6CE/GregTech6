"""Generic viewer group headings requested by the user.

Representative material, voltage, colour, flavour and species do not name a group.
Actual item names and functional boundaries (dense, reinforced, axle size) stay intact.
"""
import json
import re
try:
    from .remi_functional_groups import catalog
    from .remi_title_labels import LABELS
    from .remi_catalog_groups import wooden_bucket_variants
except ImportError:
    from remi_functional_groups import catalog
    from remi_title_labels import LABELS
    from remi_catalog_groups import wooden_bucket_variants

VOLTAGE = r"(?:ULV|LV|MV|HV|EV|IV|LuV|ZPM|UV|PUV1|XV|T\d+)(?:[-–](?:ULV|LV|MV|HV|EV|IV|LuV|ZPM|UV|PUV1|XV))?"


def headings(root, language):
    groups, ids = catalog(root)
    english = json.loads((root / "core/src/main/resources/assets/gt6ce/lang/en_us.json").read_text(encoding="utf-8"))
    materials = {v for data in (language, english) for k, v in data.items() if k.startswith("material.gt6ce.")}
    by_id = {}
    for data in (language, english):
        for key, value in data.items():
            if key.startswith("material.gt6ce."):
                by_id.setdefault(key.removeprefix("material.gt6ce.").replace("_", ""), set()).add(value)
    chinese = language.get("item.gt6ce.clay_mold") != english.get("item.gt6ce.clay_mold")
    result = {}
    for group, members in groups.items():
        chosen = next((i for i in members if i == group + "_bronze"), next((i for i in members if i.endswith("_lv")), members[0]))
        key = ids[chosen]
        original = language[key]
        if group.startswith("stone_"):
            chosen = "stone_basalt_" + group.removeprefix("stone_")
            original = language[ids[chosen]].replace("玄武岩", "石头").replace("Basalt", "Stone")
        def qualifier(match):
            parts = [p.strip() for p in re.split(r"[,，]", match[1])]
            keep = [p for p in parts if p not in materials and not re.fullmatch(VOLTAGE, p)]
            if keep == parts: return match[0]
            return " (" + ", ".join(keep) + ")" if keep else ""
        value = re.sub(r"\s*[（(]([^()（）]+)[)）]", qualifier, original)
        for material in sorted(by_id.get(chosen.removeprefix(group + "_").replace("_", ""), ()), key=lambda s: (-len(s), s)):
            if re.search(r"[\u3400-\u9fff]", material):
                if value.startswith(material): value = value[len(material):]
            else:
                value = re.sub(r"\b" + re.escape(material) + r"\b\s*", "", value).strip()
        if group.endswith("_food_can") or group == "spray_paint":
            value = re.sub(r"\s*[（(].*?[)）]", "", value)
        # Axle thickness is retained while the representative material is removed.
        if group.startswith("axle_"):
            material_id = chosen.removeprefix("axle_").rsplit("_", 1)[0].replace("_", "")
            for material in sorted(by_id.get(material_id, ()), key=len, reverse=True):
                value = value.replace(material, "").replace("  ", " ").strip()
        # Stable keys in both languages, even if only one source needs stripping.
        result["emi.group.gt6ce.functional_" + group] = value
    # Explicit headings are UI vocabulary, not translations of representative items.
    for group, labels in LABELS.items():
        path = group if group.startswith("decor_") or group in {"fluid_item", "real_fluids", "safe", "key_safe", "heat_exchanger", "heat_exchanger_dense", "boiler_main_barometer"} else "functional_" + group
        result["emi.group.gt6ce." + path] = labels[1 if chinese else 0]
    # Transport groups must show only size/type, never the chosen copper/brass.
    try:
        from .generate_remi_material_groups import transport_groups
    except ImportError:
        from generate_remi_material_groups import transport_groups
    for path, material, names in transport_groups():
        value = language[f"block.gt6ce.{path}_{material}"]
        for name in names:
            value = value.replace(name, "")
        result["emi.group.gt6ce." + path] = value.replace("  ", " ").strip()
    # Preserve the original bucket name and content label, removing only UI hints.
    for content, members in wooden_bucket_variants(root).items():
        key = "item.gt6ce." + members[0]
        label = re.sub(r"\s*\(.*\)$", "", language[key + ".tooltip"])
        result["emi.group.gt6ce.wooden_bucket_" + content] = language[key] + " · " + label
    return result
