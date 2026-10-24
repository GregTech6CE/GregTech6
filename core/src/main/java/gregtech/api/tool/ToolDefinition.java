package gregtech.api.tool;

import gregtech.api.material.GTMaterial;
import gregtech.data.MaterialPrefix;

import java.util.Locale;

/** GregTech meta-tool kinds (GT6 {@code CS.ToolsGT} manual tools, user list order). */
public enum ToolDefinition {
    SWORD(0, "sword", MaterialPrefix.toolHeadSword, null, "gt6ce:item/iconsets/handle_sword", 1.0F, 4.0F, 0, 1.0F, 100, 1, null, true, false, false, null),
    PICKAXE(2, "pickaxe", MaterialPrefix.toolHeadPickaxe, null, null, 1.0F, 3.0F, 0, 1.0F, 100, 1, "minecraft:mineable/pickaxe", true, true, false, null),
    SHOVEL(4, "shovel", MaterialPrefix.toolHeadShovel, null, null, 1.0F, 2.0F, 0, 1.0F, 100, 1, "minecraft:mineable/shovel", true, false, false, null),
    AXE(6, "axe", MaterialPrefix.toolHeadAxe, null, null, 1.0F, 3.0F, 0, 1.0F, 100, 1, "minecraft:mineable/axe", true, false, false, null),
    HOE(8, "hoe", MaterialPrefix.toolHeadHoe, null, null, 1.0F, 0.0F, 0, 1.0F, 100, 1, "minecraft:mineable/hoe", true, false, false, null),
    SAW(10, "saw", MaterialPrefix.toolHeadSaw, null, "gt6ce:item/iconsets/handle_saw", 1.0F, 2.0F, 0, 1.0F, 100, 2, "minecraft:mineable/axe", true, false, true, null),
    HARD_HAMMER(12, "hammer", MaterialPrefix.toolHeadHammer, null, null, 1.0F, 5.0F, 0, 1.0F, 400, 1, "minecraft:mineable/pickaxe", true, false, false, "hard_hammer"),
    SOFT_HAMMER(14, "soft_hammer", MaterialPrefix.toolHeadHammer, null, null, 1.0F, 2.0F, 0, 1.0F, 100, 1, null, false, false, false, "soft_hammer"),
    WRENCH(16, "wrench", null, "gt6ce:item/iconsets/wrench", null, 1.0F, 5.0F, 0, 1.0F, 800, 2, null, false, false, true, "wrench"),
    FILE(18, "file", MaterialPrefix.toolHeadFile, null, "gt6ce:item/iconsets/handle_file", 1.0F, 2.0F, 0, 1.0F, 100, 2, null, false, false, false, null),
    CROWBAR(20, "crowbar", null, "gt6ce:item/iconsets/crowbar", null, 1.0F, 2.0F, 0, 1.0F, 100, 1, null, false, false, true, "crowbar"),
    SCREWDRIVER(22, "screwdriver", MaterialPrefix.toolHeadScrewdriver, null, "gt6ce:item/iconsets/handle_screwdriver", 1.0F, 1.0F, 0, 1.0F, 100, 2, null, false, false, false, null),
    CLUB(24, "club", null, "gt6ce:item/iconsets/club", null, 1.0F, 6.0F, 0, 1.0F, 800, 1, "minecraft:mineable/pickaxe", true, false, false, null),
    WIRE_CUTTER(26, "wire_cutter", null, "gt6ce:item/iconsets/wire_cutter", null, 1.0F, 1.25F, 0, 1.0F, 400, 2, null, false, false, true, null),
    SCOOP(28, "scoop", null, "gt6ce:item/iconsets/scoop", null, 1.0F, 1.0F, 0, 1.0F, 100, 1, null, false, false, true, null),
    BRANCH_CUTTER(30, "branch_cutter", null, "gt6ce:item/iconsets/grafter", null, 1.0F, 1.0F, 0, 1.0F, 100, 2, null, false, false, true, null),
    UNIVERSAL_SPADE(32, "universal_spade", MaterialPrefix.toolHeadUniversalSpade, null, null, 1.0F, 2.0F, 0, 1.0F, 100, 2, "minecraft:mineable/shovel", true, false, true, null),
    KNIFE(34, "knife", null, "gt6ce:item/iconsets/knife", null, 1.0F, 1.5F, 0, 1.0F, 100, 1, null, false, false, true, null),
    BUTCHERY_KNIFE(36, "butchery_knife", null, "gt6ce:item/iconsets/butcheryknife", null, 1.0F, 3.0F, 0, 1.0F, 100, 2, null, false, false, false, null),
    SENSE(40, "sense", MaterialPrefix.toolHeadSense, null, null, 1.0F, 1.0F, 0, 1.0F, 100, 2, null, false, false, true, null),
    PLOW(42, "plow", MaterialPrefix.toolHeadPlow, null, null, 1.0F, 1.0F, 0, 1.0F, 100, 2, null, false, false, true, null),
    PLUNGER(44, "plunger", null, "gt6ce:item/iconsets/plunger", null, 1.0F, 1.0F, 0, 1.0F, 100, 1, null, false, false, true, null),
    ROLLING_PIN(46, "rolling_pin", null, "gt6ce:item/iconsets/rolling_pin", null, 1.0F, 2.0F, 0, 1.0F, 50, 1, null, false, false, false, null),
    CHISEL(48, "chisel", MaterialPrefix.toolHeadChisel, null, "gt6ce:item/iconsets/handle_chisel", 1.0F, 1.5F, 0, 1.0F, 400, 2, null, false, false, true, null),
    FLINT_AND_TINDER(50, "flint_and_tinder", null, "gt6ce:item/iconsets/flint_tinder", null, 1.0F, 0.0F, 0, 1.0F, 100, 1, null, false, false, false, null),
    MONKEY_WRENCH(52, "monkey_wrench", null, "gt6ce:item/iconsets/monkeywrench", null, 1.0F, 5.0F, 0, 1.0F, 800, 2, null, false, false, true, "monkey_wrench"),
    BENDING_CYLINDER(54, "bending_cylinder", null, "gt6ce:item/iconsets/bending_cylinder", null, 1.0F, 2.0F, 0, 1.0F, 100, 2, null, false, false, false, null),
    BENDING_CYLINDER_SMALL(56, "small_bending_cylinder", null, "gt6ce:item/iconsets/bending_cylinder_small", null, 1.0F, 1.0F, 0, 1.0F, 100, 2, null, false, false, false, null),
    DOUBLE_AXE(58, "double_axe", MaterialPrefix.toolHeadAxeDouble, null, null, 1.5F, 4.0F, 0, 1.0F, 100, 2, "minecraft:mineable/axe", true, false, false, "double_axe"),
    CONSTRUCTION_PICK(60, "construction_pick", MaterialPrefix.toolHeadConstructionPickaxe, null, null, 1.0F, 3.0F, 0, 2.0F, 100, 1, "minecraft:mineable/pickaxe", true, true, false, "construction_pick"),
    MAGNIFYING_GLASS(62, "magnifying_glass", null, "gt6ce:item/iconsets/magnifying_glass", null, 1.0F, 1.0F, 0, 1.0F, 400, 1, null, false, false, false, null),
    SCISSORS(64, "scissors", null, "gt6ce:item/iconsets/scissors", null, 1.0F, 2.0F, 0, 1.0F, 100, 2, null, false, false, true, null),
    PINCERS(66, "pincers", null, "gt6ce:item/iconsets/pincers", null, 1.0F, 1.0F, 0, 1.0F, 100, 2, null, false, false, true, null),
    SPADE(68, "spade", MaterialPrefix.toolHeadSpade, null, null, 1.0F, 2.0F, 0, 1.0F, 100, 1, "minecraft:mineable/shovel", true, false, false, "spade"),
    GEM_PICK(70, "gem_tipped_pickaxe", MaterialPrefix.toolHeadPickaxeGem, null, null, 0.25F, 3.0F, 0, 1.0F, 100, 2, "minecraft:mineable/pickaxe", true, true, false, "gem_pick"),
    HAND_DRILL(72, "hand_drill", null, "gt6ce:item/iconsets/hand_drill", null, 0.25F, 0.5F, 0, 0.5F, 100, 2, null, false, false, true, null),
    BUILDER_WAND(74, "builder_wand", MaterialPrefix.toolHeadBuilderwand, null, null, 0.1F, 1.0F, 0, 1.0F, 100, 1, null, false, false, true, "builder_wand"),
    PISTOL(5000, "pistol", null, "gt6ce:item/iconsets/pistol", "gt6ce:item/iconsets/handle_pistol", 1.0F, 0.0F, 0, 0.25F, 200, 2, null, false, false, false, null),
    CARBINE(5002, "carbine", null, "gt6ce:item/iconsets/carbine", "gt6ce:item/iconsets/handle_carbine", 1.0F, 0.0F, 0, 0.25F, 200, 2, null, false, false, false, null),
    RIFLE(5004, "rifle", null, "gt6ce:item/iconsets/rifle", "gt6ce:item/iconsets/handle_rifle", 1.0F, 0.0F, 0, 0.25F, 200, 2, null, false, false, false, null),
    POCKET_MULTITOOL(1000, "pocket_multitool", null, "gt6ce:item/iconsets/pocket_multitool_closed", null, 4.0F, 0.0F, 0, 1.0F, 100, 2, null, false, false, false, null),
    POCKET_KNIFE(1002, "pocket_knife", null, "gt6ce:item/iconsets/pocket_multitool_knife", null, 4.0F, 2F, 0, 0.5F, 100, 2, null, false, false, false, null),
    POCKET_SAW(1004, "pocket_saw", null, "gt6ce:item/iconsets/pocket_multitool_saw", null, 4.0F, 1.75F, 0, 1F, 100, 2, null, false, false, false, null),
    POCKET_FILE(1006, "pocket_file", null, "gt6ce:item/iconsets/pocket_multitool_file", null, 4.0F, 1.5F, 0, 1F, 100, 2, null, false, false, false, null),
    POCKET_SCREWDRIVER(1008, "pocket_screwdriver", null, "gt6ce:item/iconsets/pocket_multitool_screwdriver", null, 4.0F, 1.5F, 0, 1F, 100, 2, null, false, false, false, null),
    POCKET_WIRE_CUTTER(1010, "pocket_wire_cutter", null, "gt6ce:item/iconsets/pocket_multitool_cutter", null, 4.0F, 1.25F, 0, 1.0F, 400, 2, null, false, false, false, null),
    POCKET_SCISSORS(1012, "pocket_scissors", null, "gt6ce:item/iconsets/pocket_multitool_scissors", null, 4.0F, 1.0F, 0, 1.0F, 100, 2, null, false, false, false, null),
    POCKET_CHISEL(1014, "pocket_chisel", null, "gt6ce:item/iconsets/pocket_multitool_chisel", null, 4.0F, 1.5F, 0, 1.0F, 400, 2, null, false, false, false, null),
    ;

    private final int gt6Id;
    private final String id;

    private final MaterialPrefix headPrefix;

    private final String headlessIcon;

    private final String handleIcon;
    private final float durabilityMultiplier;
    private final float baseDamage;
    private final int baseQuality;
    private final float speedMultiplier;
    private final int damagePerCraft;
    private final int minToolTypes;

    private final String harvestTag;
    private final boolean miningTool;
    private final boolean canPenetrate;
    private final boolean canCollect;

    private final String tooltipKey;

    ToolDefinition(int gt6Id, String id,  MaterialPrefix headPrefix,  String headlessIcon,
                String handleIcon, float durabilityMultiplier, float baseDamage, int baseQuality,
               float speedMultiplier, int damagePerCraft, int minToolTypes,  String harvestTag,
               boolean miningTool, boolean canPenetrate, boolean canCollect,  String tooltipKey) {
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
     public MaterialPrefix headPrefix() { return headPrefix; }
     public String headlessIcon() { return headlessIcon; }
     public String handleIcon() { return handleIcon; }
    public float durabilityMultiplier() { return durabilityMultiplier; }
    public float baseDamage() { return baseDamage; }
    public int baseQuality() { return baseQuality; }
    public float speedMultiplier() { return speedMultiplier; }
    public int damagePerCraft() { return ToolCraftingRules.damage(id); }
    public int minToolTypes() { return minToolTypes; }
    public boolean requiresHeadAssembly() { return headPrefix != null; }
    public boolean isHeadless() { return headPrefix == null; }
    public boolean isMiningTool() { return gregtech.content.tool.OriginalToolFlags.of(name()).mining(); }
    public boolean isWeapon() { return gregtech.content.tool.OriginalToolFlags.of(name()).weapon(); }
    public boolean canPenetrate() { return canPenetrate; }
    public boolean canCollect() { return canCollect; }
     public String tooltipKey() { return tooltipKey; }
     public String harvestTag() { return harvestTag; }

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
            case POCKET_SAW,POCKET_FILE,POCKET_CHISEL -> 50;
            case POCKET_SCREWDRIVER,POCKET_SCISSORS -> 200;
            case POCKET_MULTITOOL,POCKET_KNIFE,POCKET_WIRE_CUTTER -> 100;
            case AXE, DOUBLE_AXE -> 50;
            case WRENCH, MONKEY_WRENCH, WIRE_CUTTER, CROWBAR, PLUNGER, SCOOP, BRANCH_CUTTER, KNIFE, SCISSORS,
                 PINCERS, HAND_DRILL, BUILDER_WAND -> 50;
            default -> harvestTag != null && miningTool ? 25 : 100;
        };
    }

    /** GT6 default {@code getToolDamagePerEntityAttack}. */
    public int damagePerEntityAttack() {
        return isGun()||isPocket()&&this!=POCKET_MULTITOOL?200:100;
    }

    public boolean isGun(){return this==PISTOL||this==CARBINE||this==RIFLE;}
    public boolean isPocket(){return name().startsWith("POCKET_");}
    public boolean canUseHead(GTMaterial head) {
        if (head == null || !head.isValid()) return false;
        if (head.getToolTypes() < minToolTypes) return false;
        if (headPrefix != null) return headPrefix.isValidFor(head);
        return head.hasToolStats();
    }

    public static ToolDefinition byId(String id) {
        for (ToolDefinition type : values()) {
            if (type.id.equals(id)) return type;
        }
        throw new IllegalArgumentException("Unknown GT tool type: " + id);
    }

    public String translationKey() {
        return "tool." + "gt6ce" + "." + id;
    }

    @Override
    public String toString() {
        return id.toUpperCase(Locale.ROOT);
    }
}
