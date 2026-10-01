package com.gregtech.gregtech.api.tool;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.MaterialPrefix;
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
    SWORD(0, "sword", MaterialPrefix.toolHeadSword, null, ToolIconSets.HANDLE_SWORD, 1.0F, 4.0F, 0, 1.0F, 100, 1, null, true, false, false, null),
    PICKAXE(2, "pickaxe", MaterialPrefix.toolHeadPickaxe, null, null, 1.0F, 3.0F, 0, 1.0F, 100, 1, BlockTags.MINEABLE_WITH_PICKAXE, true, true, false, null),
    SHOVEL(4, "shovel", MaterialPrefix.toolHeadShovel, null, null, 1.0F, 2.0F, 0, 1.0F, 100, 1, BlockTags.MINEABLE_WITH_SHOVEL, true, false, false, null),
    AXE(6, "axe", MaterialPrefix.toolHeadAxe, null, null, 1.0F, 3.0F, 0, 1.0F, 100, 1, BlockTags.MINEABLE_WITH_AXE, true, false, false, null),
    HOE(8, "hoe", MaterialPrefix.toolHeadHoe, null, null, 1.0F, 0.0F, 0, 1.0F, 100, 1, BlockTags.MINEABLE_WITH_HOE, true, false, false, null),
    SAW(10, "saw", MaterialPrefix.toolHeadSaw, null, ToolIconSets.HANDLE_SAW, 1.0F, 2.0F, 0, 1.0F, 100, 2, BlockTags.MINEABLE_WITH_AXE, true, false, true, null),
    HARD_HAMMER(12, "hammer", MaterialPrefix.toolHeadHammer, null, null, 1.0F, 5.0F, 0, 1.0F, 400, 1, BlockTags.MINEABLE_WITH_PICKAXE, true, false, false, "hard_hammer"),
    SOFT_HAMMER(14, "soft_hammer", MaterialPrefix.toolHeadHammer, null, null, 1.0F, 2.0F, 0, 1.0F, 100, 1, null, false, false, false, "soft_hammer"),
    WRENCH(16, "wrench", null, ToolIconSets.WRENCH, null, 1.0F, 5.0F, 0, 1.0F, 800, 3, null, false, false, true, "wrench"),
    FILE(18, "file", MaterialPrefix.toolHeadFile, null, ToolIconSets.HANDLE_FILE, 1.0F, 2.0F, 0, 1.0F, 100, 2, null, false, false, false, null),
    CROWBAR(20, "crowbar", null, ToolIconSets.CROWBAR, null, 1.0F, 2.0F, 0, 1.0F, 100, 2, null, false, false, true, "crowbar"),
    SCREWDRIVER(22, "screwdriver", MaterialPrefix.toolHeadScrewdriver, null, ToolIconSets.HANDLE_SCREWDRIVER, 1.0F, 1.0F, 0, 1.0F, 100, 2, null, false, false, false, null),
    CLUB(24, "club", null, ToolIconSets.CLUB, null, 1.0F, 6.0F, 0, 1.0F, 800, 1, BlockTags.MINEABLE_WITH_PICKAXE, true, false, false, null),
    WIRE_CUTTER(26, "wire_cutter", null, ToolIconSets.WIRE_CUTTER, null, 1.0F, 1.25F, 0, 1.0F, 400, 2, null, false, false, true, null),
    SCOOP(28, "scoop", null, ToolIconSets.SCOOP, null, 1.0F, 1.0F, 0, 1.0F, 100, 1, null, false, false, true, null),
    BRANCH_CUTTER(30, "branch_cutter", null, ToolIconSets.GRAFTER, null, 1.0F, 1.0F, 0, 1.0F, 100, 2, null, false, false, true, null),
    UNIVERSAL_SPADE(32, "universal_spade", MaterialPrefix.toolHeadUniversalSpade, null, null, 1.0F, 2.0F, 0, 1.0F, 100, 2, BlockTags.MINEABLE_WITH_SHOVEL, true, false, true, null),
    KNIFE(34, "knife", null, ToolIconSets.KNIFE, null, 1.0F, 1.5F, 0, 1.0F, 100, 1, null, false, false, true, null),
    BUTCHERY_KNIFE(36, "butchery_knife", null, ToolIconSets.BUTCHERY_KNIFE, null, 1.0F, 3.0F, 0, 1.0F, 100, 1, null, false, false, false, null),
    SENSE(40, "sense", MaterialPrefix.toolHeadSense, null, null, 1.0F, 1.0F, 0, 1.0F, 100, 2, null, false, false, true, null),
    PLOW(42, "plow", MaterialPrefix.toolHeadPlow, null, null, 1.0F, 1.0F, 0, 1.0F, 100, 1, null, false, false, true, null),
    PLUNGER(44, "plunger", null, ToolIconSets.PLUNGER, null, 1.0F, 1.0F, 0, 1.0F, 100, 1, null, false, false, true, null),
    ROLLING_PIN(46, "rolling_pin", null, ToolIconSets.ROLLING_PIN, null, 1.0F, 2.0F, 0, 1.0F, 50, 1, null, false, false, false, null),
    CHISEL(48, "chisel", MaterialPrefix.toolHeadChisel, null, ToolIconSets.HANDLE_CHISEL, 1.0F, 1.5F, 0, 1.0F, 400, 2, null, false, false, true, null),
    FLINT_AND_TINDER(50, "flint_and_tinder", null, ToolIconSets.FLINT_TINDER, null, 1.0F, 0.0F, 0, 1.0F, 100, 1, null, false, false, false, null),
    MONKEY_WRENCH(52, "monkey_wrench", null, ToolIconSets.MONKEYWRENCH, null, 1.0F, 5.0F, 0, 1.0F, 800, 3, null, false, false, true, "monkey_wrench"),
    BENDING_CYLINDER(54, "bending_cylinder", null, ToolIconSets.BENDING_CYLINDER, null, 1.0F, 2.0F, 0, 1.0F, 100, 1, null, false, false, false, null),
    BENDING_CYLINDER_SMALL(56, "small_bending_cylinder", null, ToolIconSets.BENDING_CYLINDER_SMALL, null, 1.0F, 1.0F, 0, 1.0F, 100, 1, null, false, false, false, null),
    DOUBLE_AXE(58, "double_axe", MaterialPrefix.toolHeadAxeDouble, null, null, 1.5F, 4.0F, 0, 1.0F, 100, 1, BlockTags.MINEABLE_WITH_AXE, true, false, false, "double_axe"),
    CONSTRUCTION_PICK(60, "construction_pick", MaterialPrefix.toolHeadConstructionPickaxe, null, null, 1.0F, 3.0F, 0, 2.0F, 100, 2, BlockTags.MINEABLE_WITH_PICKAXE, true, true, false, "construction_pick"),
    MAGNIFYING_GLASS(62, "magnifying_glass", null, ToolIconSets.MAGNIFYING_GLASS, null, 1.0F, 1.0F, 0, 1.0F, 400, 1, null, false, false, false, null),
    SCISSORS(64, "scissors", null, ToolIconSets.SCISSORS, null, 1.0F, 2.0F, 0, 1.0F, 100, 1, null, false, false, true, null),
    PINCERS(66, "pincers", null, ToolIconSets.PINCERS, null, 1.0F, 1.0F, 0, 1.0F, 100, 1, null, false, false, true, null),
    SPADE(68, "spade", MaterialPrefix.toolHeadSpade, null, null, 1.0F, 2.0F, 0, 1.0F, 100, 1, BlockTags.MINEABLE_WITH_SHOVEL, true, false, false, "spade"),
    GEM_PICK(70, "gem_tipped_pickaxe", MaterialPrefix.toolHeadPickaxeGem, null, null, 0.25F, 3.0F, 0, 1.0F, 100, 2, BlockTags.MINEABLE_WITH_PICKAXE, true, true, false, "gem_pick"),
    HAND_DRILL(72, "hand_drill", null, ToolIconSets.HAND_DRILL, null, 0.25F, 0.5F, 0, 0.5F, 100, 1, null, false, false, true, null),
    BUILDER_WAND(74, "builder_wand", MaterialPrefix.toolHeadBuilderwand, null, null, 0.1F, 1.0F, 0, 1.0F, 100, 2, null, false, false, true, "builder_wand"),
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
    @Nullable public TagKey<Block> harvestTag() { return harvestTag; }

    /** Displayed attack speed (player base is 4.0 before item modifier). */
    public float attackSpeed() {
        return switch (this) {
            case SWORD -> 1.5F;
            case AXE, BUTCHERY_KNIFE, DOUBLE_AXE -> 0.9F;
            case HOE -> 3.0F;
            case SHOVEL -> 1.0F;
            default -> 1.2F;
        };
    }

    /** GT6 {@code getToolDamagePerBlockBreak} default is 100; mining tools often use 25; wrench is 50. */
    public int damagePerBlockBreak() {
        return switch (this) {
            case WRENCH, MONKEY_WRENCH, WIRE_CUTTER, CROWBAR, PLUNGER, SCOOP, BRANCH_CUTTER, KNIFE, SCISSORS,
                 PINCERS, HAND_DRILL, BUILDER_WAND -> 50;
            default -> harvestTag != null && miningTool ? 25 : 100;
        };
    }

    /** GT6 default {@code getToolDamagePerEntityAttack}. */
    public int damagePerEntityAttack() {
        return 100;
    }

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
