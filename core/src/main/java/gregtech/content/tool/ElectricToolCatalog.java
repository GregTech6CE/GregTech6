package gregtech.content.tool;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import java.util.List;

/** GT6 Loader_Tools and inherited electric ToolStats, shared by both loaders. */
public final class ElectricToolCatalog {
    private ElectricToolCatalog() {}

    public record Definition(
            String id,
            String name,
            int tier,
            String headPrefix,
            List<String> rows,
            float speed,
            int quality,
            int blockCost,
            int attackCost,
            float damage) {
        public long capacity() {
            return voltage() * 1000;
        }

        public long voltage() {
            return 8L << (2 * tier);
        }

        public long energyPerUse() {
            return blockCost;
        }

        public int durabilityMultiplier() {
            return 1 << (tier - 1);
        }

        public String handleMaterial() {
            return new String[] {"", "Orange", "Red", "Blue"}[tier];
        }

        public String chassisMaterial() {
            return new String[] {"", "SteelGalvanized", "Aluminium", "StainlessSteel"}[tier];
        }

        public String original() {
            return name.replace(" ", "").toUpperCase(java.util.Locale.ROOT) + "_"
                    + tierName().toUpperCase(java.util.Locale.ROOT);
        }

        public String tierName() {
            return new String[] {"ulv", "lv", "mv", "hv"}[tier];
        }
    }

    private static Definition tool(
            String id, String name, int tier, String headPrefix, List<String> rows, StatsBuilder stats) {
        return new Definition(
                id,
                name,
                tier,
                headPrefix,
                rows,
                stats.speed,
                stats.quality,
                stats.blockCost,
                stats.attackCost,
                stats.damage);
    }

    private static final List<String> STANDARD_ASSEMBLY = List.of("dAT", "XWX", "XVX");

    public static final List<Definition> ALL = List.of(
            tool(
                    "electric_drill",
                    "Drill",
                    1,
                    "toolHeadDrill",
                    List.of("fSY", "TXW", "dVZ"),
                    new StatsBuilder()
                            .speed(1.0F)
                            .quality(0)
                            .blockCost(200)
                            .attackCost(400)
                            .damage(1.5F)),
            tool(
                    "electric_chainsaw",
                    "Chainsaw",
                    1,
                    "toolHeadChainsaw",
                    STANDARD_ASSEMBLY,
                    new StatsBuilder()
                            .speed(2.0F)
                            .quality(1)
                            .blockCost(50)
                            .attackCost(400)
                            .damage(3.0F)),
            tool(
                    "electric_wrench",
                    "Wrench",
                    1,
                    "toolHeadWrench",
                    STANDARD_ASSEMBLY,
                    new StatsBuilder()
                            .speed(2.0F)
                            .quality(0)
                            .blockCost(50)
                            .attackCost(200)
                            .damage(1.0F)),
            tool(
                    "electric_screwdriver",
                    "Screwdriver",
                    1,
                    "toolHeadScrewdriver",
                    List.of("XdA", "TWY", "VYX"),
                    new StatsBuilder()
                            .speed(1.0F)
                            .quality(0)
                            .blockCost(100)
                            .attackCost(100)
                            .damage(1.0F)),
            tool(
                    "electric_mining_drill",
                    "Mining Drill",
                    1,
                    "toolHeadDrill",
                    STANDARD_ASSEMBLY,
                    new StatsBuilder()
                            .speed(3.0F)
                            .quality(0)
                            .blockCost(25)
                            .attackCost(200)
                            .damage(2.0F)),
            tool(
                    "electric_mixer",
                    "Mixer",
                    1,
                    "toolHeadDrill",
                    List.of("SSY", "SXW", "hVZ"),
                    new StatsBuilder()
                            .speed(1.0F)
                            .quality(0)
                            .blockCost(200)
                            .attackCost(400)
                            .damage(1.5F)),
            tool(
                    "electric_buzzsaw",
                    "BuzzSaw",
                    1,
                    "toolHeadBuzzSaw",
                    List.of("YXV", "TWX", "AdY"),
                    new StatsBuilder()
                            .speed(1.0F)
                            .quality(0)
                            .blockCost(50)
                            .attackCost(300)
                            .damage(1.0F)),
            tool(
                    "electric_trimmer",
                    "Trimmer",
                    1,
                    "toolHeadSword",
                    List.of("XAT", "ZYA", "VWd"),
                    new StatsBuilder()
                            .speed(0.25F)
                            .quality(0)
                            .blockCost(100)
                            .attackCost(100)
                            .damage(2.0F)),
            tool(
                    "electric_wrench_mv",
                    "Wrench",
                    2,
                    "toolHeadWrench",
                    STANDARD_ASSEMBLY,
                    new StatsBuilder()
                            .speed(3.0F)
                            .quality(1)
                            .blockCost(200)
                            .attackCost(800)
                            .damage(1.5F)),
            tool(
                    "electric_mining_drill_mv",
                    "Mining Drill",
                    2,
                    "toolHeadDrill",
                    STANDARD_ASSEMBLY,
                    new StatsBuilder()
                            .speed(6.0F)
                            .quality(1)
                            .blockCost(100)
                            .attackCost(800)
                            .damage(2.5F)),
            tool(
                    "electric_chainsaw_mv",
                    "Chainsaw",
                    2,
                    "toolHeadChainsaw",
                    STANDARD_ASSEMBLY,
                    new StatsBuilder()
                            .speed(3.0F)
                            .quality(1)
                            .blockCost(200)
                            .attackCost(1600)
                            .damage(3.5F)),
            tool(
                    "electric_wrench_hv",
                    "Wrench",
                    3,
                    "toolHeadWrench",
                    STANDARD_ASSEMBLY,
                    new StatsBuilder()
                            .speed(4.0F)
                            .quality(1)
                            .blockCost(800)
                            .attackCost(3200)
                            .damage(2.0F)),
            tool(
                    "electric_mining_drill_hv",
                    "Mining Drill",
                    3,
                    "toolHeadDrill",
                    STANDARD_ASSEMBLY,
                    new StatsBuilder()
                            .speed(9.0F)
                            .quality(1)
                            .blockCost(400)
                            .attackCost(3200)
                            .damage(3.0F)),
            tool(
                    "electric_chainsaw_hv",
                    "Chainsaw",
                    3,
                    "toolHeadChainsaw",
                    STANDARD_ASSEMBLY,
                    new StatsBuilder()
                            .speed(4.0F)
                            .quality(1)
                            .blockCost(800)
                            .attackCost(6400)
                            .damage(4.0F)));

    public static Definition get(String id) {
        return ALL.stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
    }

    public static Definition of(String name, int tier) {
        return ALL.stream()
                .filter(d -> d.name().equals(name) && d.tier() == tier)
                .findFirst()
                .orElseThrow();
    }

    public static boolean validMaterial(GTMaterial material) {
        return material != null
                && material.isValid()
                && material.resolve() == material
                && material.getToolTypes() >= 3
                && material.getToolQuality() >= 1
                && !material.has(MaterialProperty.WOOD)
                && !material.has(MaterialProperty.ANTIMATTER)
                && !OriginalToolMaterials.inFamily(material, "Rubber")
                && !OriginalToolMaterials.inFamily(material, "Plastic");
    }

    private static final class StatsBuilder {
        private float speed;
        private int quality;
        private int blockCost;
        private int attackCost;
        private float damage;

        private StatsBuilder speed(float speed) {
            this.speed = speed;
            return this;
        }

        private StatsBuilder quality(int quality) {
            this.quality = quality;
            return this;
        }

        private StatsBuilder blockCost(int blockCost) {
            this.blockCost = blockCost;
            return this;
        }

        private StatsBuilder attackCost(int attackCost) {
            this.attackCost = attackCost;
            return this;
        }

        private StatsBuilder damage(float damage) {
            this.damage = damage;
            return this;
        }
    }
}
