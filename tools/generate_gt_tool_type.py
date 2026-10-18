"""Generate GTToolType.java from GT6 manual tool list (user order)."""
from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import os

# (enum_name, id, gt6_id, head_prefix|null, icon|null, handle|null,
#  dur_mult, base_dmg, base_qual, speed_mult, craft_cost, min_types, flags)
# flags: P=penetrate, C=collect, N=no mining speed line (still shows 0 or min)
TOOLS = [
    ("SWORD", "sword", 0, "toolHeadSword", None, "HANDLE_SWORD", 1.0, 4.0, 0, 1.0, 100, 1, ""),
    ("PICKAXE", "pickaxe", 2, "toolHeadPickaxe", None, None, 1.0, 3.0, 0, 1.0, 100, 1, "P"),
    ("SHOVEL", "shovel", 4, "toolHeadShovel", None, None, 1.0, 2.0, 0, 1.0, 100, 1, ""),
    ("AXE", "axe", 6, "toolHeadAxe", None, None, 1.0, 3.0, 0, 1.0, 100, 1, ""),
    ("HOE", "hoe", 8, "toolHeadHoe", None, None, 1.0, 0.0, 0, 1.0, 100, 1, ""),
    ("SAW", "saw", 10, "toolHeadSaw", None, "HANDLE_SAW", 1.0, 2.0, 0, 1.0, 100, 2, "C"),
    ("HARD_HAMMER", "hammer", 12, "toolHeadHammer", None, None, 1.0, 5.0, 0, 1.0, 400, 1, ""),
    ("SOFT_HAMMER", "soft_hammer", 14, "toolHeadHammer", None, None, 1.0, 2.0, 0, 1.0, 100, 1, ""),
    ("WRENCH", "wrench", 16, None, "WRENCH", None, 1.0, 5.0, 0, 1.0, 800, 3, "C"),
    ("FILE", "file", 18, "toolHeadFile", None, "HANDLE_FILE", 1.0, 2.0, 0, 1.0, 100, 2, ""),
    ("CROWBAR", "crowbar", 20, None, "CROWBAR", None, 1.0, 2.0, 0, 1.0, 100, 2, "C"),
    ("SCREWDRIVER", "screwdriver", 22, "toolHeadScrewdriver", None, "HANDLE_SCREWDRIVER", 1.0, 1.0, 0, 1.0, 100, 2, ""),
    ("CLUB", "club", 24, None, "CLUB", None, 1.0, 6.0, 0, 1.0, 800, 1, ""),
    ("WIRE_CUTTER", "wire_cutter", 26, None, "WIRE_CUTTER", None, 1.0, 1.25, 0, 1.0, 400, 2, "C"),
    ("SCOOP", "scoop", 28, None, "SCOOP", None, 1.0, 1.0, 0, 1.0, 100, 1, "C"),
    ("BRANCH_CUTTER", "branch_cutter", 30, None, "GRAFTER", None, 1.0, 1.0, 0, 1.0, 100, 2, "C"),
    ("UNIVERSAL_SPADE", "universal_spade", 32, "toolHeadUniversalSpade", None, None, 1.0, 2.0, 0, 1.0, 100, 2, "C"),
    ("KNIFE", "knife", 34, None, "KNIFE", None, 1.0, 1.5, 0, 1.0, 100, 1, "C"),
    ("BUTCHERY_KNIFE", "butchery_knife", 36, None, "BUTCHERY_KNIFE", None, 1.0, 3.0, 0, 1.0, 100, 1, ""),
    ("SENSE", "sense", 40, "toolHeadSense", None, None, 1.0, 1.0, 0, 1.0, 100, 2, "C"),
    ("PLOW", "plow", 42, "toolHeadPlow", None, None, 1.0, 1.0, 0, 1.0, 100, 1, "C"),
    ("PLUNGER", "plunger", 44, None, "PLUNGER", None, 1.0, 1.0, 0, 1.0, 100, 1, "C"),
    ("ROLLING_PIN", "rolling_pin", 46, None, "ROLLING_PIN", None, 1.0, 2.0, 0, 1.0, 50, 1, ""),
    ("CHISEL", "chisel", 48, "toolHeadChisel", None, "HANDLE_CHISEL", 1.0, 1.5, 0, 1.0, 400, 2, "C"),
    ("FLINT_AND_TINDER", "flint_and_tinder", 50, None, "FLINT_TINDER", None, 1.0, 0.0, 0, 1.0, 100, 1, ""),
    ("MONKEY_WRENCH", "monkey_wrench", 52, None, "MONKEYWRENCH", None, 1.0, 5.0, 0, 1.0, 800, 3, "C"),
    ("BENDING_CYLINDER", "bending_cylinder", 54, None, "BENDING_CYLINDER", None, 1.0, 2.0, 0, 1.0, 100, 1, ""),
    ("BENDING_CYLINDER_SMALL", "small_bending_cylinder", 56, None, "BENDING_CYLINDER_SMALL", None, 1.0, 1.0, 0, 1.0, 100, 1, ""),
    ("DOUBLE_AXE", "double_axe", 58, "toolHeadAxeDouble", None, None, 1.5, 4.0, 0, 1.0, 100, 1, ""),
    ("CONSTRUCTION_PICK", "construction_pick", 60, "toolHeadConstructionPickaxe", None, None, 1.0, 3.0, 0, 2.0, 100, 2, "P"),
    ("MAGNIFYING_GLASS", "magnifying_glass", 62, None, "MAGNIFYING_GLASS", None, 1.0, 1.0, 0, 1.0, 400, 1, ""),
    ("SCISSORS", "scissors", 64, None, "SCISSORS", None, 1.0, 2.0, 0, 1.0, 100, 1, "C"),
    ("PINCERS", "pincers", 66, None, "PINCERS", None, 1.0, 1.0, 0, 1.0, 100, 1, "C"),
    ("SPADE", "spade", 68, "toolHeadSpade", None, None, 1.0, 2.0, 0, 1.0, 100, 1, ""),
    ("GEM_PICK", "gem_tipped_pickaxe", 70, "toolHeadPickaxeGem", None, None, 0.25, 3.0, 0, 1.0, 100, 2, "P"),
    ("HAND_DRILL", "hand_drill", 72, None, "HAND_DRILL", None, 0.25, 0.5, 0, 0.5, 100, 1, "C"),
    ("BUILDER_WAND", "builder_wand", 74, "toolHeadBuilderwand", None, None, 0.1, 1.0, 0, 1.0, 100, 2, "C"),
]

HARVEST = {
    "PICKAXE": "BlockTags.MINEABLE_WITH_PICKAXE",
    "CONSTRUCTION_PICK": "BlockTags.MINEABLE_WITH_PICKAXE",
    "GEM_PICK": "BlockTags.MINEABLE_WITH_PICKAXE",
    "SHOVEL": "BlockTags.MINEABLE_WITH_SHOVEL",
    "SPADE": "BlockTags.MINEABLE_WITH_SHOVEL",
    "UNIVERSAL_SPADE": "BlockTags.MINEABLE_WITH_SHOVEL",
    "AXE": "BlockTags.MINEABLE_WITH_AXE",
    "DOUBLE_AXE": "BlockTags.MINEABLE_WITH_AXE",
    "SAW": "BlockTags.MINEABLE_WITH_AXE",
    "HOE": "BlockTags.MINEABLE_WITH_HOE",
    "HARD_HAMMER": "BlockTags.MINEABLE_WITH_PICKAXE",
    "CLUB": "BlockTags.MINEABLE_WITH_PICKAXE",
}

MINING = {
    "ROLLING_PIN", "FLINT_AND_TINDER", "MAGNIFYING_GLASS", "HAND_DRILL",
    "WRENCH", "CROWBAR", "SCREWDRIVER", "WIRE_CUTTER", "SCOOP", "BRANCH_CUTTER",
    "KNIFE", "BUTCHERY_KNIFE", "PLUNGER", "MONKEY_WRENCH", "BENDING_CYLINDER",
    "BENDING_CYLINDER_SMALL", "SCISSORS", "PINCERS", "BUILDER_WAND", "FILE",
    "SOFT_HAMMER", "CHISEL", "SENSE", "PLOW",
}

TOOLTIP = {
    "CONSTRUCTION_PICK": "construction_pick",
    "GEM_PICK": "gem_pick",
    "WRENCH": "wrench",
    "MONKEY_WRENCH": "monkey_wrench",
    "CROWBAR": "crowbar",
    "SOFT_HAMMER": "soft_hammer",
    "HARD_HAMMER": "hard_hammer",
    "SPADE": "spade",
    "DOUBLE_AXE": "double_axe",
    "BUILDER_WAND": "builder_wand",
}


def ref(name: str | None) -> str:
    if name is None:
        return "null"
    if name.startswith("toolHead"):
        return "MaterialPrefix." + name
    if name.startswith("HANDLE_") or name in {
        "WRENCH", "CROWBAR", "WIRE_CUTTER", "SCOOP", "GRAFTER", "PLUNGER",
        "KNIFE", "BUTCHERY_KNIFE", "CLUB", "ROLLING_PIN", "FLINT_TINDER",
        "MONKEYWRENCH", "BENDING_CYLINDER", "BENDING_CYLINDER_SMALL",
        "MAGNIFYING_GLASS", "SCISSORS", "PINCERS", "HAND_DRILL",
    }:
        return "ToolIconSets." + name
    return name


def emit_enum(t) -> str:
    name, id_, gt6, head, icon, handle, dur, dmg, qual, spd, craft, min_t, flags = t
    harvest = HARVEST.get(name, "null")
    mining = "true" if name not in MINING else "false"
    pen = "true" if "P" in flags else "false"
    col = "true" if "C" in flags else "false"
    tip = TOOLTIP.get(name)
    tip_arg = f'"{tip}"' if tip else "null"
    return (
        f"    {name}({gt6}, \"{id_}\", {ref(head)}, {ref(icon)}, {ref(handle)}, "
        f"{dur}F, {dmg}F, {qual}, {spd}F, {craft}, {min_t}, {harvest}, {mining}, {pen}, {col}, {tip_arg}),"
    )


header = '''package com.gregtech.gregtech.api.tool;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.prefix.MaterialPrefix;
import com.gregtech.gregtech.client.ToolIconSets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Locale;

/** GregTech meta-tool kinds (GT6 {@code CS.ToolsGT} manual tools, user list order). */
public enum GTToolType {
'''
footer = '''
    ;

    private final int gt6Id;
    private final String id;
    @Nullable
    private final MaterialPrefix headPrefix;
    @Nullable
    private final ResourceLocation headlessIcon;
    @Nullable
    private final ResourceLocation handleIcon;
    private final float durabilityMultiplier;
    private final float baseDamage;
    private final int baseQuality;
    private final float speedMultiplier;
    private final int damagePerCraft;
    private final int minToolTypes;
    @Nullable
    private final TagKey<Block> harvestTag;
    private final boolean miningTool;
    private final boolean canPenetrate;
    private final boolean canCollect;
    @Nullable
    private final String tooltipKey;

    GTToolType(int gt6Id, String id, @Nullable MaterialPrefix headPrefix, @Nullable ResourceLocation headlessIcon,
               @Nullable ResourceLocation handleIcon, float durabilityMultiplier, float baseDamage, int baseQuality,
               float speedMultiplier, int damagePerCraft, int minToolTypes, @Nullable TagKey<Block> harvestTag,
               boolean miningTool, boolean canPenetrate, boolean canCollect, @Nullable String tooltipKey) {
        this.gt6Id = gt6Id;
        this.id = id;
        this.headPrefix = headPrefix;
        this.headlessIcon = headlessIcon;
        this.handleIcon = handleIcon;
        this.durabilityMultiplier = durabilityMultiplier;
        this.baseDamage = baseDamage;
        this.baseQuality = baseQuality;
        this.speedMultiplier = speedMultiplier;
        this.damagePerCraft = damagePerCraft;
        this.minToolTypes = minToolTypes;
        this.harvestTag = harvestTag;
        this.miningTool = miningTool;
        this.canPenetrate = canPenetrate;
        this.canCollect = canCollect;
        this.tooltipKey = tooltipKey;
    }

    public int gt6Id() { return gt6Id; }
    public String id() { return id; }
    @Nullable public MaterialPrefix headPrefix() { return headPrefix; }
    @Nullable public ResourceLocation headlessIcon() { return headlessIcon; }
    @Nullable public ResourceLocation handleIcon() { return handleIcon; }
    public float durabilityMultiplier() { return durabilityMultiplier; }
    public float baseDamage() { return baseDamage; }
    public int baseQuality() { return baseQuality; }
    public float speedMultiplier() { return speedMultiplier; }
    public int damagePerCraft() { return damagePerCraft; }
    public int minToolTypes() { return minToolTypes; }
    public boolean requiresHeadAssembly() { return headPrefix != null; }
    public boolean isHeadless() { return headPrefix == null; }
    public boolean isMiningTool() { return miningTool; }
    public boolean canPenetrate() { return canPenetrate; }
    public boolean canCollect() { return canCollect; }
    @Nullable public String tooltipKey() { return tooltipKey; }

    public boolean canUseHead(GTMaterial head) {
        if (head == null || !head.isValid()) return false;
        if (head.getToolTypes() < minToolTypes) return false;
        if (headPrefix != null) return headPrefix.isValidFor(head);
        return head.hasToolStats();
    }

    public boolean canHarvest(BlockState state) {
        return harvestTag != null && state.is(harvestTag);
    }

    public static GTToolType byId(String id) {
        for (GTToolType type : values()) {
            if (type.id.equals(id)) return type;
        }
        throw new IllegalArgumentException("Unknown GT tool type: " + id);
    }

    public String translationKey() {
        return "tool." + com.gregtech.gregtech.GregTech.MODID + "." + id;
    }

    @Override
    public String toString() {
        return id.toUpperCase(Locale.ROOT);
    }
}
'''

lines = [emit_enum(t) for t in TOOLS]
out = header + "\n".join(lines) + footer
path = os.path.join(os.path.dirname(__file__), "..", str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "api", "tool", "GTToolType.java")
with open(path, "w", encoding="utf-8", newline="\n") as f:
    f.write(out)
print("wrote", path, len(TOOLS), "tools")
