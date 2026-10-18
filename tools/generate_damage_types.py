#!/usr/bin/env python3
"""Generate GT6's damage types for 1.20.1 (data/gt6ce/damage_type + vanilla damage-type tags + death messages).

Source of truth: GT6 `gregapi/damage/DamageSource*.java` and `gregapi/damage/DamageSources.java`.
Every GT6 damage source is a `DamageSource` subclass whose only content is the source name and the
death message, plus a few flags:

    setDamageBypassesArmor()           -> minecraft:bypasses_armor
    setDamageIsAbsolute()              -> minecraft:bypasses_resistance (+ bypasses_armor,
                                          bypasses_enchantments: absolute damage ignores armor,
                                          potion resistance and protection enchantments alike)
    setDamageAllowedInCreativeMode()   -> minecraft:bypasses_invulnerability
    setProjectile()                    -> minecraft:is_projectile
    setFireDamage()                    -> minecraft:is_fire

In 1.20.1 the same facts live in a data-driven `DamageType` (data/<ns>/damage_type/<id>.json) plus
the vanilla damage-type tags, so this tool is the port of those flags. The 1.7.10 damage source
classes themselves do not exist in 1.20.1: `DamageSource` is a thin holder around a registry holder.
`com.gregtech.gregtech.damage.GTDamageTypes` is the port of `DamageSources`.

Generated (do not edit by hand):
  - src/main/resources/data/gt6ce/damage_type/<id>.json
  - src/main/resources/data/minecraft/tags/damage_type/<tag>.json
  - the `death.attack.gt6ce.<id>` keys of assets/gt6ce/lang/{en_us,zh_cn}.json  (merged, sorted)

Usage: python tools/generate_damage_types.py [--check]
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, str(native_path("src")), "main", "resources")

# id -> (GT6 source file, GT6 flags, effects, exhaustion, [tags], en, zh)
# The message texts are GT6's own (`DamageSource*#func_151519_b`); the two entries marked IC2 are the
# ones GT6 delegates to IC2 (`DamageSources.getElectricDamage`/`getRadioactiveDamage`) and falls back
# to heat for - the port has no IC2, so it registers its own types instead of saying "boiled alive"
# for a shock or a meltdown (documented in docs/PORTING_REMAINING_2026-09-14.md §91).
TYPES = {
    # --- environmental / processing hazards -------------------------------------------------
    "heat": (
        "DamageSourceHeat.java", [], "burning", 0.1, [],
        "%1$s was boiled alive", "%1$s 被活活煮死了"),
    "frost": (
        "DamageSourceFrost.java", [], "freezing", 0.1, ["is_freezing"],
        "%1$s got frozen", "%1$s 被冻僵了"),
    "chemical": (
        "DamageSourceChem.java", [], "hurt", 0.1, [],
        "%1$s had a chemical accident", "%1$s 遭遇了化学事故"),
    "bumble": (
        "DamageSourceBumble.java", ["setDamageBypassesArmor"], "hurt", 0.1, ["bypasses_armor"],
        "%1$s was allergic to Bumblebees", "%1$s 对大黄蜂过敏"),
    "crusher": (
        "DamageSourceCrusher.java", [], "hurt", 0.1, [],
        "%1$s was crushed to a pulp", "%1$s 被碾成了肉酱"),
    "shredder": (
        "DamageSourceShredder.java", [], "hurt", 0.1, [],
        "%1$s was shred into flakes", "%1$s 被撕成了碎片"),
    "spike": (
        "DamageSourceSpike.java", [], "poking", 0.1, [],
        "%1$s was impaled by a Spike!", "%1$s 被尖刺刺穿了！"),
    "exploded": (
        "DamageSourceExploding.java",
        ["setDamageBypassesArmor", "setDamageIsAbsolute", "setDamageAllowedInCreativeMode"],
        "hurt", 0.1,
        ["bypasses_armor", "bypasses_resistance", "bypasses_enchantments", "bypasses_invulnerability",
         "is_explosion"],
        "%1$s exploded", "%1$s 爆炸了"),
    # --- food tracker overdoses (GT6 EntityFoodTracker) --------------------------------------
    "alcohol": (
        "DamageSourceAlcohol.java", ["setDamageBypassesArmor", "setDamageIsAbsolute"], "hurt", 0.1,
        ["bypasses_armor", "bypasses_resistance", "bypasses_enchantments"],
        "%1$s died from alcohol poisoning", "%1$s 死于酒精中毒"),
    "caffeine": (
        "DamageSourceCaffeine.java", ["setDamageBypassesArmor", "setDamageIsAbsolute"], "hurt", 0.1,
        ["bypasses_armor", "bypasses_resistance", "bypasses_enchantments"],
        "%1$s overdosed on caffeine", "%1$s 咖啡因摄入过量"),
    "dehydration": (
        "DamageSourceDehydration.java", ["setDamageBypassesArmor", "setDamageIsAbsolute"], "hurt", 0.1,
        ["bypasses_armor", "bypasses_resistance", "bypasses_enchantments"],
        "%1$s took more than the deadly dose of Salt", "%1$s 摄入了超过致死量的盐"),
    "sugar": (
        "DamageSourceSugar.java", ["setDamageBypassesArmor", "setDamageIsAbsolute"], "hurt", 0.1,
        ["bypasses_armor", "bypasses_resistance", "bypasses_enchantments"],
        "%1$s died of Diabetes", "%1$s 死于糖尿病"),
    "fat": (
        "DamageSourceFat.java", ["setDamageBypassesArmor", "setDamageIsAbsolute"], "hurt", 0.1,
        ["bypasses_armor", "bypasses_resistance", "bypasses_enchantments"],
        "%1$s got a Heart Attack", "%1$s 心脏病发作"),
    # --- port-defined replacements for the IC2 types GT6 delegates to -------------------------
    "electric": (
        "(IC2 DMG_ELECTRIC)", [], "hurt", 0.1, [],
        "%1$s was electrocuted", "%1$s 触电身亡"),
    "radiation": (
        "(IC2 DMG_RADIATION)", ["setDamageBypassesArmor"], "hurt", 0.1, ["bypasses_armor"],
        "%1$s was irradiated", "%1$s 因辐射病倒下"),
}

# message_id namespace: 1.20.1 death messages are `death.attack.<message_id>`, and the port already
# ships `gt6ce.heat` (see heat.json / lang), so every GT6 id is namespaced the same way.
MESSAGE_NS = "gt6ce"

TAG_DIR = os.path.join(RES, "data", "minecraft", "tags", "damage_type")
TYPE_DIR = os.path.join(RES, "data", "gt6ce", "damage_type")
LANG_DIR = os.path.join(RES, "assets", "gt6ce", "lang")


def dump(obj) -> str:
    return json.dumps(obj, indent=2, ensure_ascii=False) + "\n"


def write(path: str, text: str, check: bool) -> bool:
    """Write text, or - with --check - only report whether the file is already current."""
    old = None
    if os.path.isfile(path):
        with io.open(path, encoding="utf-8") as handle:
            old = handle.read()
    if old == text:
        return False
    if check:
        print("STALE " + os.path.relpath(path, ROOT))
        return True
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(text)
    print("wrote " + os.path.relpath(path, ROOT))
    return True


def main() -> int:
    check = "--check" in sys.argv
    stale = False

    tags: dict[str, list[str]] = {}
    for type_id, (gt6, flags, effects, exhaustion, type_tags, _en, _zh) in TYPES.items():
        body = {
            "message_id": MESSAGE_NS + "." + type_id,
            "exhaustion": exhaustion,
            "scaling": "never",
        }
        if effects:
            body["effects"] = effects
        stale |= write(os.path.join(TYPE_DIR, type_id + ".json"), dump(body), check)
        for tag in type_tags:
            tags.setdefault(tag, []).append("gt6ce:" + type_id)

    for tag, values in sorted(tags.items()):
        stale |= write(os.path.join(TAG_DIR, tag + ".json"),
                       dump({"replace": False, "values": sorted(values)}), check)

    for lang, index in (("en_us.json", 5), ("zh_cn.json", 6)):
        path = os.path.join(LANG_DIR, lang)
        with io.open(path, encoding="utf-8") as handle:
            table = json.load(handle)
        if list(table.keys()) != sorted(table.keys()):
            print("refusing to rewrite " + lang + ": its keys are not sorted")
            return 2
        before = len(table)
        for type_id, row in TYPES.items():
            table.setdefault("death.attack." + MESSAGE_NS + "." + type_id, row[index])
        stale |= write(path, dump(dict(sorted(table.items()))), check)
        if not check:
            print("  " + lang + ": " + str(before) + " keys -> " + str(len(table)))

    if check and stale:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
