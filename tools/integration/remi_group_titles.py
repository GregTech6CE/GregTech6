"""Source-derived machine group headings requested by the user.

Only material identities and voltage qualifiers are removed. Actual item names
and functional qualifiers (solid/gas, dense, reinforced, axle size) stay intact.
"""
import json
import re
try:
    from .remi_functional_groups import catalog
except ImportError:
    from remi_functional_groups import catalog

VOLTAGE = r"(?:ULV|LV|MV|HV|EV|IV|LuV|ZPM|UV|PUV1|XV|T\d+)(?:[-–](?:ULV|LV|MV|HV|EV|IV|LuV|ZPM|UV|PUV1|XV))?"


def headings(root, language):
    groups, ids = catalog(root)
    english = json.loads((root / "core/src/main/resources/assets/gregtech/lang/en_us.json").read_text(encoding="utf-8"))
    materials = {v for data in (language, english) for k, v in data.items() if k.startswith("material.gregtech.")}
    by_id = {}
    for data in (language, english):
        for key, value in data.items():
            if key.startswith("material.gregtech."):
                by_id.setdefault(key.removeprefix("material.gregtech.").replace("_", ""), set()).add(value)
    result = {}
    for group, members in groups.items():
        chosen = next((i for i in members if i.endswith("_bronze")), members[0])
        key = ids[chosen]
        if not key.startswith("block.") or group.startswith("stone_"):
            continue
        original = language[key]
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
        # Stable keys in both languages, even if only one source needs stripping.
        result["emi.group.gregtech.functional_" + group] = value
    return result
