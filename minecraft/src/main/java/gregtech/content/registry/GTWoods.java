package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.wood.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.*;
import java.util.function.Supplier;

/** GT6 tree/wood system: logs, stripped logs, planks, leaves and saplings by species. */
public final class GTWoods {
    private static final List<RegisteredBlock<? extends Block>> ALL = new ArrayList<>();

    // The currently registered species each have five basic wood blocks.
    private static final Map<WoodSpecies, RegisteredBlock<WoodLogBlock>> LOGS = new LinkedHashMap<>();
    private static final Map<WoodSpecies, RegisteredBlock<WoodPlanksBlock>> PLANKS = new LinkedHashMap<>();
    private static final Map<WoodSpecies, RegisteredBlock<WoodLeavesBlock>> LEAVES = new LinkedHashMap<>();
    private static final Map<WoodSpecies, RegisteredBlock<WoodSaplingBlock>> SAPLINGS = new LinkedHashMap<>();
    private static final Map<WoodSpecies, RegisteredBlock<WoodStrippedLogBlock>> STRIPPED_LOGS = new LinkedHashMap<>();
    /** GT6 BlocksGT.Planks2.mSlabs[1], meta 0: the Blue Spruce shelf support in dungeon libraries. */
    public static RegisteredBlock<WoodSlabBlock> SLAB_BLUE_SPRUCE;

    private GTWoods() {}

    public static List<RegisteredBlock<? extends Block>> all() { return Collections.unmodifiableList(ALL); }

    public static WoodLogBlock log(WoodSpecies s) { return LOGS.get(s).get(); }
    public static WoodPlanksBlock planks(WoodSpecies s) { return PLANKS.get(s).get(); }
    public static WoodStrippedLogBlock strippedLog(WoodSpecies s) { return STRIPPED_LOGS.get(s).get(); }
    public static WoodLeavesBlock leaves(WoodSpecies s) { return LEAVES.get(s).get(); }
    public static WoodSaplingBlock sapling(WoodSpecies s) { return SAPLINGS.get(s).get(); }

    /** Reverse lookup used by worldgen/tests: the species of a registered leaves block, else {@code null}. */
    public static WoodSpecies leavesOf(Block block) {
        for (Map.Entry<WoodSpecies, RegisteredBlock<WoodLeavesBlock>> e : LEAVES.entrySet()) {
            if (e.getValue().get() == block) return e.getKey();
        }
        return null;
    }

    private static <T extends Block> RegisteredBlock<T> reg(
            ContentRegistrar registrar, String id, Supplier<T> blockSupplier) {
        RegisteredBlock<T> ro = registrar.block(
                REGISTRY_NAMESPACE, id, blockSupplier, bare -> new BlockItem(bare, new Item.Properties()));
        ALL.add(ro);
        return ro;
    }

    public static void registerAll(ContentRegistrar registrar) {
        for (WoodSpecies sp : WoodSpecies.values()) {
            BlockBehaviour.Properties base = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD);
            final BlockBehaviour.Properties p = sp.fireproof() ? base.ignitedByLava() : base;

            LOGS.put(sp, reg(registrar, "log_" + sp.id(), () -> new WoodLogBlock(sp, p)));
            PLANKS.put(sp, reg(registrar, "planks_" + sp.id(), () -> new WoodPlanksBlock(sp, p)));
            LEAVES.put(sp, reg(registrar, "leaves_" + sp.id(), () -> new WoodLeavesBlock(sp,
                    BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2f).randomTicks().sound(SoundType.GRASS).noOcclusion())));
            SAPLINGS.put(sp, reg(registrar, "sapling_" + sp.id(), () -> new WoodSaplingBlock(sp,
                    new gregtech.content.worldgen.GTTreeGrower(sp),
                    BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.GRASS))));
            STRIPPED_LOGS.put(sp, reg(registrar, "stripped_log_" + sp.id(), () -> new WoodStrippedLogBlock(p)));
        }
        for (String wood : gregtech.content.plant.WoodSlabCatalog.WOODS) {
            var slab = reg(registrar, "slab_" + wood, () -> new WoodSlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(2.0f, 3.0f).sound(SoundType.WOOD)));
            if (wood.equals("bluespruce")) SLAB_BLUE_SPRUCE = slab;
        }
//? if >=1.21.1 {
        /*registerLegacyBeamAliases();
*///?}
    }

//? if >=1.21.1 {
    /*// Old beam ids resolve to the stripped-log blocks that replaced them.
    private static void registerLegacyBeamAliases() {
        for (WoodSpecies sp : WoodSpecies.values()) alias("beam_" + sp.id(), "stripped_log_" + sp.id());
        alias("beam_bluemahoe", "stripped_log_blue_mahoe");
        alias("beam_oak", "minecraft:stripped_oak_log");
        alias("beam_spruce", "minecraft:stripped_spruce_log");
        alias("beam_birch", "minecraft:stripped_birch_log");
        alias("beam_jungle", "minecraft:stripped_jungle_log");
        alias("beam_acacia", "minecraft:stripped_acacia_log");
        alias("beam_darkoak", "minecraft:stripped_dark_oak_log");
    }

    private static void alias(String from, String to) {
        var fromId = net.minecraft.resources.ResourceLocation.parse(from.indexOf(':') >= 0 ? from : "gt6ce:" + from);
        var toId = net.minecraft.resources.ResourceLocation.parse(to.indexOf(':') >= 0 ? to : "gt6ce:" + to);
        GTBlocks.BLOCKS.addAlias(fromId, toId);
        GTBlocks.BLOCK_ITEMS.addAlias(fromId, toId);
    }
*///?}
}
