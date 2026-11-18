package gregtech.engine.tool;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialPrefix;
import java.util.Locale;

/** Shared manual tool definitions, with wear measured in GT6 durability units. */
public enum ToolDefinition {
    SWORD(
            0,
            "sword",
            headed(MaterialPrefix.toolHeadSword)
                    .handleIcon("gt6ce:item/iconsets/handle_sword")
                    .baseDamage(4.0F)
                    .damagePerBlockBreak(200)),
    PICKAXE(
            2,
            "pickaxe",
            headed(MaterialPrefix.toolHeadPickaxe)
                    .baseDamage(3.0F)
                    .damagePerBlockBreak(25)
                    .damagePerEntityAttack(200)
                    .harvestTag("minecraft:mineable/pickaxe")
                    .penetratesArmor()),
    SHOVEL(
            4,
            "shovel",
            headed(MaterialPrefix.toolHeadShovel)
                    .baseDamage(1.5F)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .harvestTag("minecraft:mineable/shovel")),
    AXE(
            6,
            "axe",
            headed(MaterialPrefix.toolHeadAxe)
                    .baseDamage(3.0F)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .harvestTag("minecraft:mineable/axe")),
    HOE(
            8,
            "hoe",
            headed(MaterialPrefix.toolHeadHoe)
                    .baseDamage(1.5F)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .harvestTag("minecraft:mineable/hoe")),
    SAW(
            10,
            "saw",
            headed(MaterialPrefix.toolHeadSaw)
                    .handleIcon("gt6ce:item/iconsets/handle_saw")
                    .baseDamage(1.75F)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)
                    .harvestTag("minecraft:mineable/axe")
                    .collectsDrops()),
    HARD_HAMMER(
            12,
            "hammer",
            headed(MaterialPrefix.toolHeadHammer)
                    .baseDamage(5.0F)
                    .speedMultiplier(0.75F)
                    .damagePerCraft(400)
                    .damagePerBlockBreak(25)
                    .damagePerEntityAttack(200)
                    .harvestTag("minecraft:mineable/pickaxe")
                    .tooltipKey("hard_hammer")),
    SOFT_HAMMER(
            14,
            "soft_hammer",
            headed(MaterialPrefix.toolHeadHammer)
                    .durabilityMultiplier(8.0F)
                    .baseDamage(3.0F)
                    .speedMultiplier(0.1F)
                    .damagePerCraft(800)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .tooltipKey("soft_hammer")),
    WRENCH(
            16,
            "wrench",
            headless("gt6ce:item/iconsets/wrench")
                    .baseDamage(5.0F)
                    .damagePerCraft(800)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)
                    .collectsDrops()
                    .tooltipKey("wrench")),
    FILE(
            18,
            "file",
            headed(MaterialPrefix.toolHeadFile)
                    .handleIcon("gt6ce:item/iconsets/handle_file")
                    .baseDamage(1.5F)
                    .damagePerCraft(400)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)),
    CROWBAR(
            20,
            "crowbar",
            headless("gt6ce:item/iconsets/crowbar")
                    .baseDamage(2.0F)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .collectsDrops()
                    .tooltipKey("crowbar")),
    SCREWDRIVER(
            22,
            "screwdriver",
            headed(MaterialPrefix.toolHeadScrewdriver)
                    .handleIcon("gt6ce:item/iconsets/handle_screwdriver")
                    .baseDamage(1.5F)
                    .damagePerCraft(400)
                    .damagePerBlockBreak(200)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)),
    CLUB(
            24,
            "club",
            headless("gt6ce:item/iconsets/club")
                    .baseDamage(5.0F)
                    .speedMultiplier(0.5F)
                    .damagePerCraft(800)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(50)
                    .harvestTag("minecraft:mineable/pickaxe")),
    WIRE_CUTTER(
            26,
            "wire_cutter",
            headless("gt6ce:item/iconsets/wire_cutter")
                    .baseDamage(1.25F)
                    .damagePerCraft(400)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)
                    .collectsDrops()),
    SCOOP(
            28,
            "scoop",
            headless("gt6ce:item/iconsets/scoop")
                    .baseDamage(1.0F)
                    .damagePerBlockBreak(200)
                    .damagePerEntityAttack(200)
                    .collectsDrops()),
    BRANCH_CUTTER(
            30,
            "branch_cutter",
            headless("gt6ce:item/iconsets/grafter")
                    .durabilityMultiplier(0.25F)
                    .baseDamage(2.5F)
                    .speedMultiplier(0.25F)
                    .minToolTypes(2)
                    .collectsDrops()),
    UNIVERSAL_SPADE(
            32,
            "universal_spade",
            headed(MaterialPrefix.toolHeadUniversalSpade)
                    .baseDamage(3.0F)
                    .speedMultiplier(0.75F)
                    .damagePerCraft(400)
                    .minToolTypes(2)
                    .harvestTag("minecraft:mineable/shovel")
                    .collectsDrops()),
    KNIFE(
            34,
            "knife",
            headless("gt6ce:item/iconsets/knife")
                    .baseDamage(2.0F)
                    .speedMultiplier(0.5F)
                    .damagePerEntityAttack(200)
                    .collectsDrops()),
    BUTCHERY_KNIFE(
            36,
            "butchery_knife",
            headless("gt6ce:item/iconsets/butcheryknife")
                    .baseDamage(1.0F)
                    .speedMultiplier(0.1F)
                    .damagePerBlockBreak(200)
                    .damagePerEntityAttack(400)
                    .minToolTypes(2)),
    SENSE(
            40,
            "sense",
            headed(MaterialPrefix.toolHeadSense)
                    .durabilityMultiplier(4.0F)
                    .baseDamage(3.0F)
                    .minToolTypes(2)
                    .collectsDrops()),
    PLOW(
            42,
            "plow",
            headed(MaterialPrefix.toolHeadPlow).baseDamage(1.0F).minToolTypes(2).collectsDrops()),
    PLUNGER(
            44,
            "plunger",
            headless("gt6ce:item/iconsets/plunger")
                    .durabilityMultiplier(0.25F)
                    .baseDamage(1.25F)
                    .collectsDrops()),
    ROLLING_PIN(
            46,
            "rolling_pin",
            headless("gt6ce:item/iconsets/rolling_pin")
                    .baseDamage(2.0F)
                    .damagePerCraft(50)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)),
    CHISEL(
            48,
            "chisel",
            headed(MaterialPrefix.toolHeadChisel)
                    .handleIcon("gt6ce:item/iconsets/handle_chisel")
                    .baseDamage(1.5F)
                    .damagePerCraft(400)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)
                    .collectsDrops()),
    FLINT_AND_TINDER(
            50,
            "flint_and_tinder",
            headless("gt6ce:item/iconsets/flint_tinder")
                    .durabilityMultiplier(0.25F)
                    .baseDamage(1.0F)),
    MONKEY_WRENCH(
            52,
            "monkey_wrench",
            headless("gt6ce:item/iconsets/monkeywrench")
                    .baseDamage(5.0F)
                    .damagePerCraft(800)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)
                    .collectsDrops()
                    .tooltipKey("monkey_wrench")),
    BENDING_CYLINDER(
            54,
            "bending_cylinder",
            headless("gt6ce:item/iconsets/bending_cylinder")
                    .baseDamage(2.0F)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)),
    BENDING_CYLINDER_SMALL(
            56,
            "small_bending_cylinder",
            headless("gt6ce:item/iconsets/bending_cylinder_small")
                    .baseDamage(1.5F)
                    .damagePerCraft(25)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)),
    DOUBLE_AXE(
            58,
            "double_axe",
            headed(MaterialPrefix.toolHeadAxeDouble)
                    .durabilityMultiplier(1.5F)
                    .baseDamage(6.0F)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)
                    .harvestTag("minecraft:mineable/axe")
                    .tooltipKey("double_axe")),
    CONSTRUCTION_PICK(
            60,
            "construction_pick",
            headed(MaterialPrefix.toolHeadConstructionPickaxe)
                    .baseDamage(3.0F)
                    .speedMultiplier(2.0F)
                    .damagePerBlockBreak(25)
                    .damagePerEntityAttack(200)
                    .harvestTag("minecraft:mineable/pickaxe")
                    .penetratesArmor()
                    .tooltipKey("construction_pick")),
    MAGNIFYING_GLASS(
            62,
            "magnifying_glass",
            headless("gt6ce:item/iconsets/magnifying_glass")
                    .baseDamage(1.0F)
                    .damagePerCraft(400)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)),
    SCISSORS(
            64,
            "scissors",
            headless("gt6ce:item/iconsets/scissors")
                    .baseDamage(1.0F)
                    .damagePerBlockBreak(200)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)
                    .collectsDrops()),
    PINCERS(
            66,
            "pincers",
            headless("gt6ce:item/iconsets/pincers")
                    .baseDamage(2.0F)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)
                    .collectsDrops()),
    SPADE(
            68,
            "spade",
            headed(MaterialPrefix.toolHeadSpade)
                    .baseDamage(1.5F)
                    .speedMultiplier(1.5F)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .harvestTag("minecraft:mineable/shovel")
                    .tooltipKey("spade")),
    GEM_PICK(
            70,
            "gem_tipped_pickaxe",
            headed(MaterialPrefix.toolHeadPickaxeGem)
                    .durabilityMultiplier(0.25F)
                    .baseDamage(3.0F)
                    .damagePerBlockBreak(25)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)
                    .harvestTag("minecraft:mineable/pickaxe")
                    .penetratesArmor()
                    .tooltipKey("gem_pick")),
    HAND_DRILL(
            72,
            "hand_drill",
            headless("gt6ce:item/iconsets/hand_drill")
                    .durabilityMultiplier(0.25F)
                    .baseDamage(0.5F)
                    .speedMultiplier(0.5F)
                    .minToolTypes(2)
                    .collectsDrops()),
    BUILDER_WAND(
            74,
            "builder_wand",
            headed(MaterialPrefix.toolHeadBuilderwand)
                    .durabilityMultiplier(0.1F)
                    .baseDamage(1.0F)
                    .damagePerBlockBreak(1)
                    .collectsDrops()
                    .tooltipKey("builder_wand")),
    PISTOL(
            5000,
            "pistol",
            headless("gt6ce:item/iconsets/pistol")
                    .handleIcon("gt6ce:item/iconsets/handle_pistol")
                    .baseDamage(1.0F)
                    .speedMultiplier(0.25F)
                    .damagePerCraft(200)
                    .damagePerBlockBreak(200)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)),
    CARBINE(
            5002,
            "carbine",
            headless("gt6ce:item/iconsets/carbine")
                    .handleIcon("gt6ce:item/iconsets/handle_carbine")
                    .baseDamage(1.0F)
                    .speedMultiplier(0.25F)
                    .damagePerCraft(200)
                    .damagePerBlockBreak(200)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)),
    RIFLE(
            5004,
            "rifle",
            headless("gt6ce:item/iconsets/rifle")
                    .handleIcon("gt6ce:item/iconsets/handle_rifle")
                    .baseDamage(1.0F)
                    .speedMultiplier(0.25F)
                    .damagePerCraft(200)
                    .damagePerBlockBreak(200)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)),
    POCKET_MULTITOOL(
            1000,
            "pocket_multitool",
            headless("gt6ce:item/iconsets/pocket_multitool_closed")
                    .durabilityMultiplier(4.0F)
                    .minToolTypes(2)),
    POCKET_KNIFE(
            1002,
            "pocket_knife",
            headless("gt6ce:item/iconsets/pocket_multitool_knife")
                    .durabilityMultiplier(4.0F)
                    .baseDamage(2.0F)
                    .speedMultiplier(0.5F)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)),
    POCKET_SAW(
            1004,
            "pocket_saw",
            headless("gt6ce:item/iconsets/pocket_multitool_saw")
                    .durabilityMultiplier(4.0F)
                    .baseDamage(1.75F)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)),
    POCKET_FILE(
            1006,
            "pocket_file",
            headless("gt6ce:item/iconsets/pocket_multitool_file")
                    .durabilityMultiplier(4.0F)
                    .baseDamage(1.5F)
                    .damagePerCraft(400)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)),
    POCKET_SCREWDRIVER(
            1008,
            "pocket_screwdriver",
            headless("gt6ce:item/iconsets/pocket_multitool_screwdriver")
                    .durabilityMultiplier(4.0F)
                    .baseDamage(1.5F)
                    .damagePerCraft(400)
                    .damagePerBlockBreak(200)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)),
    POCKET_WIRE_CUTTER(
            1010,
            "pocket_wire_cutter",
            headless("gt6ce:item/iconsets/pocket_multitool_cutter")
                    .durabilityMultiplier(4.0F)
                    .baseDamage(1.25F)
                    .damagePerCraft(400)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)),
    POCKET_SCISSORS(
            1012,
            "pocket_scissors",
            headless("gt6ce:item/iconsets/pocket_multitool_scissors")
                    .durabilityMultiplier(4.0F)
                    .baseDamage(1.0F)
                    .damagePerBlockBreak(200)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2)),
    POCKET_CHISEL(
            1014,
            "pocket_chisel",
            headless("gt6ce:item/iconsets/pocket_multitool_chisel")
                    .durabilityMultiplier(4.0F)
                    .baseDamage(1.5F)
                    .damagePerCraft(400)
                    .damagePerBlockBreak(50)
                    .damagePerEntityAttack(200)
                    .minToolTypes(2));

    private final int gt6Id;
    private final String id;

    private final MaterialPrefix headPrefix;

    private final String headlessIcon;

    private final String handleIcon;
    private final float durabilityMultiplier;
    private final float baseDamage;
    private final float speedMultiplier;
    private final int damagePerCraft;
    private final int damagePerBlockBreak;
    private final int damagePerEntityAttack;
    private final int minToolTypes;

    private final String harvestTag;
    private final boolean canPenetrate;
    private final boolean canCollect;

    private final String tooltipKey;

    ToolDefinition(int gt6Id, String id, Builder builder) {
        this.gt6Id = gt6Id;
        this.id = id;
        this.headPrefix = builder.headPrefix;
        this.headlessIcon = builder.headlessIcon;
        this.handleIcon = builder.handleIcon;
        this.durabilityMultiplier = builder.durabilityMultiplier;
        this.baseDamage = builder.baseDamage;
        this.speedMultiplier = builder.speedMultiplier;
        this.damagePerCraft = builder.damagePerCraft;
        this.damagePerBlockBreak = builder.damagePerBlockBreak;
        this.damagePerEntityAttack = builder.damagePerEntityAttack;
        this.minToolTypes = builder.minToolTypes;
        this.harvestTag = builder.harvestTag;
        this.canPenetrate = builder.canPenetrate;
        this.canCollect = builder.canCollect;
        this.tooltipKey = builder.tooltipKey;
    }

    private static Builder headed(MaterialPrefix headPrefix) {
        return new Builder(headPrefix, null);
    }

    private static Builder headless(String icon) {
        return new Builder(null, icon);
    }

    public int gt6Id() {
        return gt6Id;
    }

    public String id() {
        return id;
    }

    public MaterialPrefix headPrefix() {
        return headPrefix;
    }

    public String headlessIcon() {
        return headlessIcon;
    }

    public String handleIcon() {
        return handleIcon;
    }

    public float durabilityMultiplier() {
        return durabilityMultiplier;
    }

    public float baseDamage() {
        return baseDamage;
    }

    public int baseQuality() {
        return 0;
    }

    public float speedMultiplier() {
        return speedMultiplier;
    }

    public int damagePerCraft() {
        return damagePerCraft;
    }

    public int minToolTypes() {
        return minToolTypes;
    }

    public boolean requiresHeadAssembly() {
        return headPrefix != null;
    }

    public boolean isHeadless() {
        return headPrefix == null;
    }

    public boolean isMiningTool() {
        return gregtech.content.tool.OriginalToolFlags.of(name()).mining();
    }

    public boolean isWeapon() {
        return gregtech.content.tool.OriginalToolFlags.of(name()).weapon();
    }

    public boolean canPenetrate() {
        return canPenetrate;
    }

    public boolean canCollect() {
        return canCollect;
    }

    public String tooltipKey() {
        return tooltipKey;
    }

    public String harvestTag() {
        return harvestTag;
    }

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

    public int damagePerBlockBreak() {
        return damagePerBlockBreak;
    }

    public int damagePerEntityAttack() {
        return damagePerEntityAttack;
    }

    /** GT6 default {@code getExhaustionPerAttack}; the wrench overrides it to 0.6. */
    public float attackExhaustion() {
        return this == WRENCH || this == MONKEY_WRENCH ? 0.6F : 0.3F;
    }

    /** GT6 default {@code getHurtResistanceTime} is unchanged; the wrench doubles it. */
    public int hurtResistanceMultiplier() {
        return this == WRENCH || this == MONKEY_WRENCH ? 2 : 1;
    }

    /** Translation key for this tool's kill message, or {@code null} to keep the vanilla one. */
    public String deathMessageKey() {
        return switch (this) {
            case WRENCH -> "death.attack.gt6ce.wrench";
            case MONKEY_WRENCH -> "death.attack.gt6ce.monkey_wrench";
            default -> null;
        };
    }

    public boolean isGun() {
        return this == PISTOL || this == CARBINE || this == RIFLE;
    }

    public boolean isPocket() {
        return name().startsWith("POCKET_");
    }

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

    private static final class Builder {
        private final MaterialPrefix headPrefix;
        private final String headlessIcon;
        private String handleIcon;
        private float durabilityMultiplier = 1.0F;
        private float baseDamage;
        private float speedMultiplier = 1.0F;
        private int damagePerCraft = 100;
        private int damagePerBlockBreak = 100;
        private int damagePerEntityAttack = 100;
        private int minToolTypes = 1;
        private String harvestTag;
        private boolean canPenetrate;
        private boolean canCollect;
        private String tooltipKey;

        private Builder(MaterialPrefix headPrefix, String headlessIcon) {
            this.headPrefix = headPrefix;
            this.headlessIcon = headlessIcon;
        }

        private Builder handleIcon(String handleIcon) {
            this.handleIcon = handleIcon;
            return this;
        }

        private Builder durabilityMultiplier(float durabilityMultiplier) {
            this.durabilityMultiplier = durabilityMultiplier;
            return this;
        }

        private Builder baseDamage(float baseDamage) {
            this.baseDamage = baseDamage;
            return this;
        }

        private Builder speedMultiplier(float speedMultiplier) {
            this.speedMultiplier = speedMultiplier;
            return this;
        }

        private Builder damagePerCraft(int damagePerCraft) {
            this.damagePerCraft = damagePerCraft;
            return this;
        }

        private Builder damagePerBlockBreak(int damagePerBlockBreak) {
            this.damagePerBlockBreak = damagePerBlockBreak;
            return this;
        }

        private Builder damagePerEntityAttack(int damagePerEntityAttack) {
            this.damagePerEntityAttack = damagePerEntityAttack;
            return this;
        }

        private Builder minToolTypes(int minToolTypes) {
            this.minToolTypes = minToolTypes;
            return this;
        }

        private Builder harvestTag(String harvestTag) {
            this.harvestTag = harvestTag;
            return this;
        }

        private Builder penetratesArmor() {
            canPenetrate = true;
            return this;
        }

        private Builder collectsDrops() {
            canCollect = true;
            return this;
        }

        private Builder tooltipKey(String tooltipKey) {
            this.tooltipKey = tooltipKey;
            return this;
        }
    }
}
