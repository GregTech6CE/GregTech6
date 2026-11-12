package gregtech.engine.material;

import gregtech.api.material.GTValues;
import gregtech.content.data.MaterialPrefix;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Complete original VanillaUnificationLoader identity/weight table, including secondary components. */
public final class VanillaUnificationDefinitions {
    private VanillaUnificationDefinitions() {}

    /** Source category used to keep ores and modern raw-metal forms out of generic recovery. */
    public enum OreInputKind {
        OTHER,
        ORE,
        RAW_METAL,
        RAW_METAL_BLOCK
    }

    /** Secondary material carried by the same vanilla item, e.g. red sand's iron oxide. */
    public record ExtraComponent(String materialName, long amount) {}

    public record Entry(
            String itemPath,
            MaterialPrefix prefix,
            String materialName,
            long amount,
            List<ExtraComponent> extraComponents,
            List<MaterialPrefix> extraPrefixes) {
        public Entry(String itemPath, MaterialPrefix prefix, String materialName, long amount) {
            this(itemPath, prefix, materialName, amount, List.of(), List.of());
        }

        public Entry(
                String itemPath,
                MaterialPrefix prefix,
                String materialName,
                long amount,
                List<ExtraComponent> extraComponents) {
            this(itemPath, prefix, materialName, amount, extraComponents, List.of());
        }
    }

    public static List<Entry> entries() {
        return List.of(
                new Entry("ender_pearl", MaterialPrefix.gem, "EnderPearl", MaterialPrefix.gem.getMaterialWeight()),
                new Entry("ender_eye", MaterialPrefix.gem, "EnderEye", MaterialPrefix.gem.getMaterialWeight()),
                new Entry("diamond", MaterialPrefix.gem, "Diamond", MaterialPrefix.gem.getMaterialWeight()),
                new Entry("emerald", MaterialPrefix.gem, "Emerald", MaterialPrefix.gem.getMaterialWeight()),
                new Entry("coal", MaterialPrefix.gem, "Coal", MaterialPrefix.gem.getMaterialWeight()),
                new Entry("charcoal", MaterialPrefix.gem, "Charcoal", MaterialPrefix.gem.getMaterialWeight()),
                new Entry("quartz", MaterialPrefix.gem, "NetherQuartz", MaterialPrefix.gem.getMaterialWeight()),
                new Entry("nether_star", MaterialPrefix.gem, "NetherStar", MaterialPrefix.gem.getMaterialWeight()),
                new Entry("lapis_lazuli", MaterialPrefix.gem, "Lapis", MaterialPrefix.gem.getMaterialWeight()),
                new Entry("iron_ingot", MaterialPrefix.ingot, "Fe", MaterialPrefix.ingot.getMaterialWeight()),
                new Entry("gold_ingot", MaterialPrefix.ingot, "Au", MaterialPrefix.ingot.getMaterialWeight()),
                new Entry("copper_ingot", MaterialPrefix.ingot, "Cu", MaterialPrefix.ingot.getMaterialWeight()),
                new Entry("brick", MaterialPrefix.ingot, "Brick", MaterialPrefix.ingot.getMaterialWeight()),
                new Entry(
                        "nether_brick", MaterialPrefix.ingot, "NetherBrick", MaterialPrefix.ingot.getMaterialWeight()),
                new Entry("gold_nugget", MaterialPrefix.nugget, "Au", MaterialPrefix.nugget.getMaterialWeight()),
                new Entry("redstone", MaterialPrefix.dust, "Redstone", MaterialPrefix.dust.getMaterialWeight()),
                new Entry("glowstone_dust", MaterialPrefix.dust, "Glowstone", MaterialPrefix.dust.getMaterialWeight()),
                new Entry("gunpowder", MaterialPrefix.dust, "Gunpowder", MaterialPrefix.dust.getMaterialWeight()),
                new Entry("sugar", MaterialPrefix.dust, "Sugar", MaterialPrefix.dust.getMaterialWeight()),
                new Entry("bone_meal", MaterialPrefix.dust, "Bone", MaterialPrefix.dust.getMaterialWeight()),
                new Entry(
                        "blaze_powder", MaterialPrefix.dustTiny, "Blaze", MaterialPrefix.dustTiny.getMaterialWeight()),
                new Entry("blaze_rod", MaterialPrefix.stick, "Blaze", MaterialPrefix.stick.getMaterialWeight()),
                new Entry("stick", MaterialPrefix.stick, "Wood", MaterialPrefix.stick.getMaterialWeight()),
                new Entry("paper", MaterialPrefix.plate, "Paper", MaterialPrefix.plate.getMaterialWeight()),
                new Entry(
                        "glass_pane",
                        MaterialPrefix.plate,
                        "Glass",
                        MaterialPrefix.plate.getMaterialWeight(),
                        List.of(),
                        List.of(MaterialPrefix.plateGem)),
                new Entry("flint", null, "Flint", GTValues.U),
                new Entry("sand", null, "Sand", 9L * GTValues.U),
                new Entry("red_sand", null, "Sand", 9L * GTValues.U, List.of(new ExtraComponent("Fe2O3", GTValues.U))),
                new Entry("glass", null, "Glass", 9L * GTValues.U),
                new Entry("netherrack", null, "Netherrack", 9L * GTValues.U),
                new Entry("end_stone", null, "Endstone", 9L * GTValues.U),
                new Entry("obsidian", null, "Obsidian", 9L * GTValues.U),
                new Entry("soul_sand", null, "SoulSand", 9L * GTValues.U),
                new Entry("gravel", null, "Stone", 9L * GTValues.U),
                new Entry("sandstone", null, "Sand", 9L * GTValues.U),
                new Entry("iron_block", null, "Fe", 9L * GTValues.U),
                new Entry("gold_block", null, "Au", 9L * GTValues.U),
                new Entry("copper_block", null, "Cu", 9L * GTValues.U),
                new Entry("diamond_block", null, "Diamond", 9L * GTValues.U),
                new Entry("emerald_block", null, "Emerald", 9L * GTValues.U),
                new Entry("lapis_block", null, "Lapis", 9L * GTValues.U),
                new Entry("coal_block", null, "Coal", 9L * GTValues.U),
                new Entry("redstone_block", null, "Redstone", 9L * GTValues.U),
                new Entry("coal_ore", null, "Coal", 2L * GTValues.U),
                new Entry("iron_ore", null, "Fe", 2L * GTValues.U),
                new Entry("gold_ore", null, "Au", 2L * GTValues.U),
                new Entry("copper_ore", null, "Cu", 2L * GTValues.U),
                new Entry("lapis_ore", null, "Lapis", 2L * GTValues.U),
                new Entry("redstone_ore", null, "Redstone", 2L * GTValues.U),
                new Entry("diamond_ore", null, "Diamond", 2L * GTValues.U),
                new Entry("emerald_ore", null, "Emerald", 2L * GTValues.U),
                new Entry("nether_quartz_ore", null, "NetherQuartz", 2L * GTValues.U),
                new Entry("nether_gold_ore", null, "Gold", 2L * GTValues.U),
                new Entry("deepslate_coal_ore", null, "Coal", 2L * GTValues.U),
                new Entry("deepslate_iron_ore", null, "Fe", 2L * GTValues.U),
                new Entry("deepslate_gold_ore", null, "Au", 2L * GTValues.U),
                new Entry("deepslate_copper_ore", null, "Cu", 2L * GTValues.U),
                new Entry("deepslate_lapis_ore", null, "Lapis", 2L * GTValues.U),
                new Entry("deepslate_redstone_ore", null, "Redstone", 2L * GTValues.U),
                new Entry("deepslate_diamond_ore", null, "Diamond", 2L * GTValues.U),
                new Entry("deepslate_emerald_ore", null, "Emerald", 2L * GTValues.U),
                new Entry("raw_copper", null, "Cu", 2L * GTValues.U),
                new Entry("raw_copper_block", null, "Cu", 18L * GTValues.U),
                new Entry("raw_iron", null, "Fe", 2L * GTValues.U),
                new Entry("raw_iron_block", null, "Fe", 18L * GTValues.U),
                new Entry("raw_gold", null, "Au", 2L * GTValues.U),
                new Entry("raw_gold_block", null, "Au", 18L * GTValues.U),
                new Entry("iron_nugget", MaterialPrefix.nugget, "Fe", MaterialPrefix.nugget.getMaterialWeight()),
                new Entry("amethyst_shard", MaterialPrefix.gem, "Amethyst", MaterialPrefix.gem.getMaterialWeight()),
                new Entry("amethyst_block", null, "Amethyst", 4L * GTValues.U),
                new Entry(
                        "netherite_ingot", MaterialPrefix.ingot, "Netherite", MaterialPrefix.ingot.getMaterialWeight()),
                new Entry("netherite_block", null, "Netherite", 9L * GTValues.U),
                new Entry("netherite_scrap", MaterialPrefix.ingot, "AncientDebris", GTValues.U),
                new Entry("ancient_debris", null, "AncientDebris", 2L * GTValues.U),
                new Entry("clay_ball", null, "Clay", GTValues.U),
                new Entry("clay", null, "Clay", 4L * GTValues.U),
                new Entry("glowstone", null, "Glowstone", GTValues.U * 4),
                new Entry("quartz_block", null, "NetherQuartz", GTValues.U * 4),
                new Entry("bone", null, "Bone", GTValues.U * 2),
                new Entry("bone_block", null, "Bone", GTValues.U * 6),
                new Entry("stone", null, "Stone", 9L * GTValues.U),
                new Entry("cobblestone", null, "Stone", 9L * GTValues.U),
                new Entry("mossy_cobblestone", null, "Stone", 9L * GTValues.U),
                new Entry("granite", null, "Granite", 9L * GTValues.U),
                new Entry("diorite", null, "Diorite", 9L * GTValues.U),
                new Entry("andesite", null, "Andesite", 9L * GTValues.U),
                new Entry("deepslate", null, "Deepslate", 9L * GTValues.U),
                new Entry("cobbled_deepslate", null, "Deepslate", 9L * GTValues.U),
                new Entry("basalt", null, "Basalt", GTValues.U),
                new Entry("blackstone", null, "Blackstone", GTValues.U),
                new Entry("calcite", null, "Calcite", GTValues.U),
                new Entry("tuff", null, "Tuff", 9L * GTValues.U),
                new Entry("dripstone_block", null, "Dripstone", GTValues.U),
                new Entry("glass_bottle", null, "Empty", GTValues.U));
    }

    private static final Set<String> KNOWN_ITEM_PATHS =
            entries().stream().map(Entry::itemPath).collect(java.util.stream.Collectors.toUnmodifiableSet());

    /**
     * Classifies a declared vanilla item as ore/raw metal input.
     *
     * <p>Raw-metal item and raw-metal block are intentionally distinct: ore processing admits one
     * according to its own row rather than treating both as a path-name suffix match.</p>
     */
    public static OreInputKind oreInputKind(String itemPath) {
        if (itemPath == null || !KNOWN_ITEM_PATHS.contains(itemPath)) return OreInputKind.OTHER;
        if (itemPath.endsWith("_ore") || itemPath.equals("ancient_debris")) return OreInputKind.ORE;
        if (itemPath.startsWith("raw_") && itemPath.endsWith("_block")) return OreInputKind.RAW_METAL_BLOCK;
        if (itemPath.startsWith("raw_")) return OreInputKind.RAW_METAL;
        return OreInputKind.OTHER;
    }

    /**
     * The original form a vanilla ore block supplies. The source keeps the host in the form name,
     * so deepslate and nether ores do not collapse into the stone-hosted form.
     */
    public static String vanillaOreForm(String itemPath) {
        if (itemPath.startsWith("deepslate_")) return "oreDeepslate";
        if (itemPath.startsWith("nether_") && itemPath.endsWith("_ore")) return "oreNetherrack";
        if (itemPath.equals("ancient_debris")) return "oreNetherrack";
        return "oreVanillastone";
    }

    /** A vanilla block that supplies the original GT block form, e.g. {@code blockDust/Soulsand}. */
    public record BlockTarget(String itemPath, String blockPrefix, String materialName) {}

    /**
     * The source registers these targets before item generation, so the vanilla stack replaces a
     * generated GT block of that form. Every consumer of vanilla block unification reads this table;
     * a missing row leaves the form without a vanilla stack or leaves the GT duplicate unhidden.
     */
    private static final List<BlockTarget> BLOCK_TARGETS = List.of(
            new BlockTarget("gravel", "blockDust", "Stone"),
            new BlockTarget("sand", "blockDust", "Sand"),
            new BlockTarget("red_sand", "blockDust", "RedSand"),
            new BlockTarget("soul_sand", "blockDust", "Soulsand"),
            new BlockTarget("redstone_block", "blockDust", "Redstone"),
            new BlockTarget("iron_block", "blockIngot", "Iron"),
            new BlockTarget("gold_block", "blockIngot", "Gold"),
            new BlockTarget("copper_block", "blockIngot", "Copper"),
            new BlockTarget("netherite_block", "blockIngot", "Netherite"),
            new BlockTarget("diamond_block", "blockGem", "Diamond"),
            new BlockTarget("emerald_block", "blockGem", "Emerald"),
            new BlockTarget("lapis_block", "blockGem", "Lapis"),
            new BlockTarget("coal_block", "blockGem", "Coal"),
            new BlockTarget("sandstone", "blockSolid", "Sand"),
            new BlockTarget("glass", "blockSolid", "Glass"),
            new BlockTarget("smooth_stone", "blockSolid", "Stone"),
            new BlockTarget("netherrack", "blockSolid", "Netherrack"),
            new BlockTarget("end_stone", "blockSolid", "Endstone"),
            new BlockTarget("obsidian", "blockSolid", "Obsidian"));

    private static final Map<String, List<MaterialPrefix>> EXTRA_PREFIXES_BY_PATH = extraPrefixesByPath();

    private static Map<String, List<MaterialPrefix>> extraPrefixesByPath() {
        var byPath = new HashMap<String, List<MaterialPrefix>>();
        for (var entry : entries())
            if (!entry.extraPrefixes().isEmpty()) byPath.put(entry.itemPath(), entry.extraPrefixes());
        return Map.copyOf(byPath);
    }

    /** Additional forms supplied by the same vanilla item, e.g. a glass pane is plate and plateGem. */
    public static List<MaterialPrefix> extraPrefixes(String itemPath) {
        return EXTRA_PREFIXES_BY_PATH.getOrDefault(itemPath, List.of());
    }

    private static final Map<String, BlockTarget> BLOCK_TARGETS_BY_PATH = blockTargetsByPath();

    private static Map<String, BlockTarget> blockTargetsByPath() {
        var byPath = new HashMap<String, BlockTarget>();
        for (var target : BLOCK_TARGETS) byPath.put(target.itemPath(), target);
        return Map.copyOf(byPath);
    }

    public static List<BlockTarget> blockTargets() {
        return BLOCK_TARGETS;
    }

    public static BlockTarget blockTarget(String itemPath) {
        return BLOCK_TARGETS_BY_PATH.get(itemPath);
    }
}
