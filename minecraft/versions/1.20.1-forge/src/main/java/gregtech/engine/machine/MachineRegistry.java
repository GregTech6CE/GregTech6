package gregtech.engine.machine;

import gregtech.content.block.machine.BurningBoxBlock;
import gregtech.content.block.machine.CrucibleCrossingBlock;
import gregtech.content.block.machine.CrucibleFaucetBlock;
import gregtech.content.block.machine.CrucibleHullBlockItem;
import gregtech.content.block.machine.GTMachineBlockItem;
import gregtech.content.block.machine.HopperBlock;
import gregtech.content.block.machine.HopperBlockItem;
import gregtech.content.block.machine.MoldBasinBlock;
import gregtech.content.block.machine.MoldBlock;
import gregtech.content.block.machine.QueueHopperBlock;
import gregtech.content.block.machine.QueueHopperBlockItem;
import gregtech.content.block.machine.SmeltingCrucibleBlock;
import gregtech.content.block.machine.SmeltingCrucibleBlockItem;
import gregtech.content.block.machine.SolidBurningBoxBlock;
import gregtech.content.registry.GTBlocks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

/**
 * GT6-style machine registration helper ({@code Loader_MultiTileEntities} burning box entries).
 */
public final class MachineRegistry {
    private static final List<RegistryObject<SolidBurningBoxBlock>> SOLID_BURNING_BOXES = new ArrayList<>();
    private static final List<RegistryObject<BurningBoxBlock>> BURNING_BOXES = new ArrayList<>();
    private static final List<RegistryObject<SmeltingCrucibleBlock>> SMELTING_CRUCIBLES = new ArrayList<>();
    private static final List<RegistryObject<MoldBasinBlock>> MOLD_BASINS = new ArrayList<>();
    private static final List<RegistryObject<MoldBlock>> MOLDS = new ArrayList<>();
    private static final List<RegistryObject<CrucibleCrossingBlock>> CRUCIBLE_CROSSINGS = new ArrayList<>();
    private static final List<RegistryObject<CrucibleFaucetBlock>> CRUCIBLE_FAUCETS = new ArrayList<>();
    private static final List<RegistryObject<HopperBlock>> HOPPERS = new ArrayList<>();
    private static final List<RegistryObject<QueueHopperBlock>> QUEUE_HOPPERS = new ArrayList<>();

    private static final int MOLD_META_OFFSET = 50;
    private static final int BASIN_META_OFFSET = 750;
    private static final int FAUCET_META_OFFSET = 700;
    private static final int CROSSING_META_OFFSET = 850;

    private MachineRegistry() {}

    /**
     * Registers a solid burning box tier.
     *
     * @param id           registry path, e.g. {@code burning_box_solid_bronze}
     * @param materialName display material name
     * @param tintRgb      hull tint (GT6 {@code mRGBa})
     * @param efficiency   0..10000 (GT6 {@code NBT_EFFICIENCY})
     * @param outputHu     HU/t (GT6 {@code NBT_OUTPUT})
     */
    public static RegistryObject<SolidBurningBoxBlock> registerSolidBurningBox(
            String id,
            String materialName,
            int tintRgb,
            int efficiency,
            long outputHu,
            float hardness,
            float blastResistance) {
        MachineSpec spec = new MachineSpec(
                id,
                materialName,
                tintRgb,
                efficiency,
                outputHu,
                MachineTextures.BURNING_SOLID,
                hardness,
                blastResistance);
        return registerSolidBurningBox(spec);
    }

    public static RegistryObject<SolidBurningBoxBlock> registerSolidBurningBox(MachineSpec spec) {
        String id = spec.id();
        RegistryObject<SolidBurningBoxBlock> block = GTBlocks.BLOCKS.register(
                id,
                () -> new SolidBurningBoxBlock(
                        spec,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(spec.hardness(), spec.blastResistance())
                                .requiresCorrectToolForDrops()
                                .lightLevel(state -> state.getValue(SolidBurningBoxBlock.LIT) ? 13 : 0)));
        SOLID_BURNING_BOXES.add(block);
        GTBlocks.BLOCK_ITEMS.register(
                id, () -> new GTMachineBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        return block;
    }

    public static List<RegistryObject<SolidBurningBoxBlock>> solidBurningBoxes() {
        return Collections.unmodifiableList(SOLID_BURNING_BOXES);
    }

    /**
     * Registers a liquid/gas/fluidized-bed burning box tier.
     *
     * @param fuelType     SOLID, LIQUID, GAS, or FLUIDIZED_BED
     * @param id           registry path, e.g. {@code burning_box_liquid_bronze}
     * @param materialName display material name
     * @param tintRgb      hull tint
     * @param efficiency   0..10000
     * @param outputHu     HU/t
     */
    public static RegistryObject<BurningBoxBlock> registerBurningBox(
            BurningBoxFuelType fuelType,
            String id,
            String materialName,
            int tintRgb,
            int efficiency,
            long outputHu,
            float hardness,
            float blastResistance,
            String textureSet) {
        MachineSpec spec =
                new MachineSpec(id, materialName, tintRgb, efficiency, outputHu, textureSet, hardness, blastResistance);
        RegistryObject<BurningBoxBlock> block = GTBlocks.BLOCKS.register(
                id,
                () -> new BurningBoxBlock(
                        fuelType,
                        spec,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(hardness, blastResistance)
                                .requiresCorrectToolForDrops()
                                .lightLevel(state -> state.getValue(BurningBoxBlock.LIT) ? 13 : 0)));
        BURNING_BOXES.add(block);
        GTBlocks.BLOCK_ITEMS.register(
                id, () -> new GTMachineBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        return block;
    }

    public static List<RegistryObject<BurningBoxBlock>> burningBoxes() {
        return Collections.unmodifiableList(BURNING_BOXES);
    }

    /**
     * Registers a smelting crucible tier (GT6 {@code Loader_MultiTileEntities#crucible}).
     */
    public static RegistryObject<SmeltingCrucibleBlock> registerSmeltingCrucible(CrucibleSpec spec) {
        RegistryObject<SmeltingCrucibleBlock> block = GTBlocks.BLOCKS.register(
                spec.id(), () -> new SmeltingCrucibleBlock(spec, SmeltingCrucibleBlock.defaultProperties(spec)));
        SMELTING_CRUCIBLES.add(block);
        GTBlocks.BLOCK_ITEMS.register(
                spec.id(), () -> new SmeltingCrucibleBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        registerSmelteryCompanions(spec);
        return block;
    }

    private static void registerSmelteryCompanions(CrucibleSpec crucibleSpec) {
        String suffix = crucibleSpec.id().substring("smelting_crucible_".length());
        registerMold(companionSpec(crucibleSpec, "mold_" + suffix, MOLD_META_OFFSET, CrucibleSpec.MOLD_HULL_UNITS));
        registerMoldBasin(
                companionSpec(crucibleSpec, "mold_basin_" + suffix, BASIN_META_OFFSET, CrucibleSpec.BASIN_HULL_UNITS));
        registerCrucibleCrossing(companionSpec(
                crucibleSpec, "crucible_crossing_" + suffix, CROSSING_META_OFFSET, CrucibleSpec.CROSSING_HULL_UNITS));
        registerCrucibleFaucet(companionSpec(
                crucibleSpec, "crucible_faucet_" + suffix, FAUCET_META_OFFSET, CrucibleSpec.FAUCET_HULL_UNITS));
    }

    private static CrucibleSpec companionSpec(CrucibleSpec base, String id, int metaOffset, long hullMaterialUnits) {
        return SmelteryCompanionDefinitions.copy(base, id, metaOffset, hullMaterialUnits);
    }

    public static RegistryObject<MoldBlock> registerMold(CrucibleSpec spec) {
        RegistryObject<MoldBlock> block =
                GTBlocks.BLOCKS.register(spec.id(), () -> new MoldBlock(spec, MoldBlock.defaultProperties(spec)));
        MOLDS.add(block);
        GTBlocks.BLOCK_ITEMS.register(
                spec.id(), () -> new CrucibleHullBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        return block;
    }

    public static RegistryObject<MoldBasinBlock> registerMoldBasin(CrucibleSpec spec) {
        RegistryObject<MoldBasinBlock> block = GTBlocks.BLOCKS.register(
                spec.id(), () -> new MoldBasinBlock(spec, MoldBasinBlock.defaultProperties(spec)));
        MOLD_BASINS.add(block);
        GTBlocks.BLOCK_ITEMS.register(
                spec.id(), () -> new CrucibleHullBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        return block;
    }

    public static RegistryObject<CrucibleCrossingBlock> registerCrucibleCrossing(CrucibleSpec spec) {
        RegistryObject<CrucibleCrossingBlock> block = GTBlocks.BLOCKS.register(
                spec.id(), () -> new CrucibleCrossingBlock(spec, CrucibleCrossingBlock.defaultProperties(spec)));
        CRUCIBLE_CROSSINGS.add(block);
        GTBlocks.BLOCK_ITEMS.register(
                spec.id(), () -> new CrucibleHullBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        return block;
    }

    public static RegistryObject<CrucibleFaucetBlock> registerCrucibleFaucet(CrucibleSpec spec) {
        RegistryObject<CrucibleFaucetBlock> block = GTBlocks.BLOCKS.register(
                spec.id(), () -> new CrucibleFaucetBlock(spec, CrucibleFaucetBlock.defaultProperties(spec)));
        CRUCIBLE_FAUCETS.add(block);
        GTBlocks.BLOCK_ITEMS.register(
                spec.id(), () -> new CrucibleHullBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        return block;
    }

    public static List<RegistryObject<MoldBlock>> molds() {
        return Collections.unmodifiableList(MOLDS);
    }

    public static List<RegistryObject<MoldBasinBlock>> moldBasins() {
        return Collections.unmodifiableList(MOLD_BASINS);
    }

    public static List<RegistryObject<CrucibleCrossingBlock>> crucibleCrossings() {
        return Collections.unmodifiableList(CRUCIBLE_CROSSINGS);
    }

    public static List<RegistryObject<CrucibleFaucetBlock>> crucibleFaucets() {
        return Collections.unmodifiableList(CRUCIBLE_FAUCETS);
    }

    public static List<RegistryObject<SmeltingCrucibleBlock>> smeltingCrucibles() {
        return Collections.unmodifiableList(SMELTING_CRUCIBLES);
    }

    /** All smeltery block registries in creative-tab display order. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<List<? extends RegistryObject<? extends Block>>> smelteryBlocks() {
        return Arrays.asList(
                (List) SMELTING_CRUCIBLES, (List) MOLD_BASINS, (List) MOLDS, (List) CRUCIBLE_CROSSINGS, (List)
                        CRUCIBLE_FAUCETS);
    }

    // ── Hopper / QueueHopper registration ────────────────────────────────

    /** Registers a hopper variant (GT6 {@code MultiTileEntityHopper}). */
    public static RegistryObject<HopperBlock> registerHopper(HopperSpec spec) {
        RegistryObject<HopperBlock> block = GTBlocks.BLOCKS.register(
                spec.id(),
                () -> new HopperBlock(
                        spec,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(spec.hardness(), spec.blastResistance())
                                .requiresCorrectToolForDrops()));
        HOPPERS.add(block);
        GTBlocks.BLOCK_ITEMS.register(
                spec.id(), () -> new HopperBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        return block;
    }

    /** Registers a queuehopper variant (GT6 {@code MultiTileEntityQueueHopper}). */
    public static RegistryObject<QueueHopperBlock> registerQueueHopper(HopperSpec spec) {
        RegistryObject<QueueHopperBlock> block = GTBlocks.BLOCKS.register(
                spec.id(),
                () -> new QueueHopperBlock(
                        spec,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(spec.hardness(), spec.blastResistance())
                                .requiresCorrectToolForDrops()));
        QUEUE_HOPPERS.add(block);
        GTBlocks.BLOCK_ITEMS.register(
                spec.id(), () -> new QueueHopperBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        return block;
    }

    public static List<RegistryObject<HopperBlock>> hoppers() {
        return Collections.unmodifiableList(HOPPERS);
    }

    public static List<RegistryObject<QueueHopperBlock>> queueHoppers() {
        return Collections.unmodifiableList(QUEUE_HOPPERS);
    }
}
