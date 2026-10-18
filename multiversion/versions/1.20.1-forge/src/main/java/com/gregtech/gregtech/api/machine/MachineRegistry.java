package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.block.machine.GTMachineBlockItem;
import com.gregtech.gregtech.block.machine.CrucibleCrossingBlock;
import com.gregtech.gregtech.block.machine.CrucibleFaucetBlock;
import com.gregtech.gregtech.block.machine.CrucibleHullBlockItem;
import com.gregtech.gregtech.block.machine.EngineBlock;
import com.gregtech.gregtech.block.machine.HopperBlock;
import com.gregtech.gregtech.block.machine.HopperBlockItem;
import com.gregtech.gregtech.block.machine.MoldBasinBlock;
import com.gregtech.gregtech.block.machine.MoldBlock;
import com.gregtech.gregtech.block.machine.QueueHopperBlock;
import com.gregtech.gregtech.block.machine.QueueHopperBlockItem;
import com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.block.machine.SmeltingCrucibleBlockItem;
import com.gregtech.gregtech.block.machine.SolidBurningBoxBlock;
import com.gregtech.gregtech.block.machine.BurningBoxBlock;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.EngineBaseBlockEntity;
import com.gregtech.gregtech.blockentity.machine.KineticDieselEngineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.KineticElectricEngineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.KineticFluxEngineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.KineticRotationEngineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.KineticSteamEngineBlockEntity;
import com.gregtech.gregtech.registry.GTBlocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
    private static final List<RegistryObject<BasicMachineBlock>> BASIC_MACHINES = new ArrayList<>();
    /** RegistryObjects for basic machine blocks; used in lazy BE type registration pass. */
    private static final List<RegistryObject<BasicMachineBlock>> BASIC_MACHINE_BLOCKS = new ArrayList<>();
    private static final List<RegistryObject<EngineBlock>> ELECTRIC_ENGINES = new ArrayList<>();
    private static final List<RegistryObject<EngineBlock>> FLUX_ENGINES = new ArrayList<>();
    private static final List<RegistryObject<EngineBlock>> STEAM_ENGINES = new ArrayList<>();
    private static final List<RegistryObject<EngineBlock>> STRONG_STEAM_ENGINES = new ArrayList<>();
    private static final List<RegistryObject<EngineBlock>> ROTATION_ENGINES = new ArrayList<>();
    private static final List<RegistryObject<EngineBlock>> DIESEL_ENGINES = new ArrayList<>();
    /** Engine blocks queued for BE type registration. */
    private static final List<RegistryObject<EngineBlock>> ENGINE_BLOCKS = new ArrayList<>();

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
            String id, String materialName, int tintRgb, int efficiency, long outputHu,
            float hardness, float blastResistance) {
        MachineSpec spec = new MachineSpec(id, materialName, tintRgb, efficiency, outputHu,
                MachineTextures.BURNING_SOLID, hardness, blastResistance);
        return registerSolidBurningBox(spec);
    }

    public static RegistryObject<SolidBurningBoxBlock> registerSolidBurningBox(MachineSpec spec) {
        String id = spec.id();
        RegistryObject<SolidBurningBoxBlock> block = GTBlocks.BLOCKS.register(id, () -> new SolidBurningBoxBlock(
                spec,
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(spec.hardness(), spec.blastResistance())
                        .requiresCorrectToolForDrops()
                        .lightLevel(state -> state.getValue(SolidBurningBoxBlock.LIT) ? 13 : 0)));
        SOLID_BURNING_BOXES.add(block);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new GTMachineBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
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
            BurningBoxFuelType fuelType, String id, String materialName, int tintRgb, int efficiency, long outputHu,
            float hardness, float blastResistance, String textureSet) {
        MachineSpec spec = new MachineSpec(id, materialName, tintRgb, efficiency, outputHu,
                textureSet, hardness, blastResistance);
        RegistryObject<BurningBoxBlock> block = GTBlocks.BLOCKS.register(id, () -> new BurningBoxBlock(
                fuelType, spec,
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(hardness, blastResistance)
                        .requiresCorrectToolForDrops()
                        .lightLevel(state -> state.getValue(BurningBoxBlock.LIT) ? 13 : 0)));
        BURNING_BOXES.add(block);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new GTMachineBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        return block;
    }

    public static List<RegistryObject<BurningBoxBlock>> burningBoxes() {
        return Collections.unmodifiableList(BURNING_BOXES);
    }

    /**
     * Registers a smelting crucible tier (GT6 {@code Loader_MultiTileEntities#crucible}).
     */
    public static RegistryObject<SmeltingCrucibleBlock> registerSmeltingCrucible(CrucibleSpec spec) {
        RegistryObject<SmeltingCrucibleBlock> block = GTBlocks.BLOCKS.register(spec.id(),
                () -> new SmeltingCrucibleBlock(spec, SmeltingCrucibleBlock.defaultProperties(spec)));
        SMELTING_CRUCIBLES.add(block);
        GTBlocks.BLOCK_ITEMS.register(spec.id(),
                () -> new SmeltingCrucibleBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        registerSmelteryCompanions(spec);
        return block;
    }

    private static void registerSmelteryCompanions(CrucibleSpec crucibleSpec) {
        String suffix = crucibleSpec.id().substring("smelting_crucible_".length());
        registerMold(companionSpec(crucibleSpec, "mold_" + suffix, MOLD_META_OFFSET, CrucibleSpec.MOLD_HULL_UNITS));
        registerMoldBasin(companionSpec(crucibleSpec, "mold_basin_" + suffix, BASIN_META_OFFSET, CrucibleSpec.BASIN_HULL_UNITS));
        registerCrucibleCrossing(companionSpec(crucibleSpec, "crucible_crossing_" + suffix, CROSSING_META_OFFSET, CrucibleSpec.CROSSING_HULL_UNITS));
        registerCrucibleFaucet(companionSpec(crucibleSpec, "crucible_faucet_" + suffix, FAUCET_META_OFFSET, CrucibleSpec.FAUCET_HULL_UNITS));
    }

    private static CrucibleSpec companionSpec(CrucibleSpec base, String id, int metaOffset, long hullMaterialUnits) {
        return SmelteryCompanionDefinitions.copy(base,id,metaOffset,hullMaterialUnits);
    }

    public static RegistryObject<MoldBlock> registerMold(CrucibleSpec spec) {
        RegistryObject<MoldBlock> block = GTBlocks.BLOCKS.register(spec.id(),
                () -> new MoldBlock(spec, MoldBlock.defaultProperties(spec)));
        MOLDS.add(block);
        GTBlocks.BLOCK_ITEMS.register(spec.id(),
                () -> new CrucibleHullBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        return block;
    }

    public static RegistryObject<MoldBasinBlock> registerMoldBasin(CrucibleSpec spec) {
        RegistryObject<MoldBasinBlock> block = GTBlocks.BLOCKS.register(spec.id(),
                () -> new MoldBasinBlock(spec, MoldBasinBlock.defaultProperties(spec)));
        MOLD_BASINS.add(block);
        GTBlocks.BLOCK_ITEMS.register(spec.id(),
                () -> new CrucibleHullBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        return block;
    }

    public static RegistryObject<CrucibleCrossingBlock> registerCrucibleCrossing(CrucibleSpec spec) {
        RegistryObject<CrucibleCrossingBlock> block = GTBlocks.BLOCKS.register(spec.id(),
                () -> new CrucibleCrossingBlock(spec, CrucibleCrossingBlock.defaultProperties(spec)));
        CRUCIBLE_CROSSINGS.add(block);
        GTBlocks.BLOCK_ITEMS.register(spec.id(),
                () -> new CrucibleHullBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        return block;
    }

    public static RegistryObject<CrucibleFaucetBlock> registerCrucibleFaucet(CrucibleSpec spec) {
        RegistryObject<CrucibleFaucetBlock> block = GTBlocks.BLOCKS.register(spec.id(),
                () -> new CrucibleFaucetBlock(spec, CrucibleFaucetBlock.defaultProperties(spec)));
        CRUCIBLE_FAUCETS.add(block);
        GTBlocks.BLOCK_ITEMS.register(spec.id(),
                () -> new CrucibleHullBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
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
                (List) SMELTING_CRUCIBLES,
                (List) MOLD_BASINS,
                (List) MOLDS,
                (List) CRUCIBLE_CROSSINGS,
                (List) CRUCIBLE_FAUCETS);
    }

    // ── Hopper / QueueHopper registration ────────────────────────────────

    /** Registers a hopper variant (GT6 {@code MultiTileEntityHopper}). */
    public static RegistryObject<HopperBlock> registerHopper(HopperSpec spec) {
        RegistryObject<HopperBlock> block = GTBlocks.BLOCKS.register(spec.id(),
                () -> new HopperBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(spec.hardness(), spec.blastResistance())
                        .requiresCorrectToolForDrops()));
        HOPPERS.add(block);
        GTBlocks.BLOCK_ITEMS.register(spec.id(),
                () -> new HopperBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        return block;
    }

    /** Registers a queuehopper variant (GT6 {@code MultiTileEntityQueueHopper}). */
    public static RegistryObject<QueueHopperBlock> registerQueueHopper(HopperSpec spec) {
        RegistryObject<QueueHopperBlock> block = GTBlocks.BLOCKS.register(spec.id(),
                () -> new QueueHopperBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(spec.hardness(), spec.blastResistance())
                        .requiresCorrectToolForDrops()));
        QUEUE_HOPPERS.add(block);
        GTBlocks.BLOCK_ITEMS.register(spec.id(),
                () -> new QueueHopperBlockItem(block.get(), new Item.Properties().stacksTo(16), spec));
        return block;
    }

    public static List<RegistryObject<HopperBlock>> hoppers() {
        return Collections.unmodifiableList(HOPPERS);
    }

    public static List<RegistryObject<QueueHopperBlock>> queueHoppers() {
        return Collections.unmodifiableList(QUEUE_HOPPERS);
    }

    // ── Basic Machine registration ──────────────────────────────────────────

    /**
     * Registers a basic machine block (GT6 {@code MultiTileEntityBasicMachine}).
     * <p>
     * The matching {@link BlockEntityType} is registered separately via
     * {@link #registerBasicMachineBeTypes(DeferredRegister)} so that
     * {@code MachineRegistry} does not need to import {@code GTBlockEntities}.
     */
    public static RegistryObject<BasicMachineBlock> registerBasicMachine(BasicMachineSpec spec) {
        return registerBasicMachine(spec, props -> new BasicMachineBlock(spec, props));
    }

    /** Variant taking a custom block factory (multiblock controllers etc.). */
    public static RegistryObject<BasicMachineBlock> registerBasicMachine(
            BasicMachineSpec spec, java.util.function.Function<BlockBehaviour.Properties, BasicMachineBlock> factory) {
        RegistryObject<BasicMachineBlock> block = GTBlocks.BLOCKS.register(spec.id(), () ->
                factory.apply(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(spec.hardness(), spec.blastResistance())
                        .requiresCorrectToolForDrops()));
        BASIC_MACHINES.add(block);
        BASIC_MACHINE_BLOCKS.add(block);
        GTBlocks.BLOCK_ITEMS.register(spec.id(),
                () -> new GTMachineBlockItem(block.get(), new Item.Properties().stacksTo(16),
                        new MachineSpec(spec.id(), spec.material().getLocalName(), spec.material().getColor(),
                                0, 0, spec.textureFolder(), spec.hardness(), spec.blastResistance()),
                        spec));
        return block;
    }

    /**
     * Second pass: registers per-machine {@link BlockEntityType}s for every
     * basic machine queued via {@link #registerBasicMachine(BasicMachineSpec)}.
     * Call ONCE from the loader before submitting the BE type
     * {@code DeferredRegister} to the bus.
     */
    public static void registerBasicMachineBeTypes(DeferredRegister<BlockEntityType<?>> beRegistry) {
        for (RegistryObject<BasicMachineBlock> blockRO : BASIC_MACHINE_BLOCKS) {
            beRegistry.register("be_" + blockRO.getId().getPath(), () -> {
                BasicMachineBlock block = blockRO.get();
                BasicMachineSpec spec = block.basicSpec();
                AtomicReference<BlockEntityType<BasicMachineBlockEntity>> typeRef = new AtomicReference<>();
                BlockEntityType<BasicMachineBlockEntity> type = BlockEntityType.Builder.of(
                        (pos, state) -> {
                            BasicMachineBlockEntity be = block.createBlockEntity(typeRef.get(), pos, state);
                            be.setSpec(spec);
                            return be;
                        },
                        block
                ).build(null);
                typeRef.set(type);
                block.setBeTypeSupplier(() -> type);
                return type;
            });
        }
    }

    public static List<RegistryObject<BasicMachineBlock>> basicMachines() {
        return Collections.unmodifiableList(BASIC_MACHINES);
    }

    // ── Engine registration ────────────────────────────────────────────────

    private static RegistryObject<EngineBlock> registerEngineBlock(
            EngineType type, MachineSpec itemSpec, Object engineSpec, float hardness, float blastResistance) {
        boolean woodenRotation = type == EngineType.ROTATION
                && engineSpec instanceof RotationEngineSpec rotation
                && rotation.material() == WoodMaterials.WoodTreated;
        RegistryObject<EngineBlock> block = GTBlocks.BLOCKS.register(itemSpec.id(), () -> {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                    .mapColor(woodenRotation ? MapColor.WOOD : MapColor.METAL)
                    .strength(hardness, blastResistance)
                    .requiresCorrectToolForDrops();
            if (woodenRotation) properties.sound(SoundType.WOOD);
            return new EngineBlock(type, itemSpec, engineSpec, properties);
        });
        ENGINE_BLOCKS.add(block);
        GTBlocks.BLOCK_ITEMS.register(itemSpec.id(),
                () -> new GTMachineBlockItem(block.get(), new Item.Properties().stacksTo(16), itemSpec));
        return block;
    }

    public static RegistryObject<EngineBlock> registerElectricEngine(ElectricEngineSpec spec) {
        MachineSpec ms = engineMachineSpec(spec.id(), spec.materialName(), spec.tintRgb(), spec.outputRate(),
                spec.textureFolder(), spec.hardness(), spec.blastResistance());
        RegistryObject<EngineBlock> block = registerEngineBlock(EngineType.ELECTRIC, ms, spec, spec.hardness(), spec.blastResistance());
        ELECTRIC_ENGINES.add(block);
        return block;
    }

    public static RegistryObject<EngineBlock> registerFluxEngine(FluxEngineSpec spec) {
        MachineSpec ms = engineMachineSpec(spec.id(), spec.materialName(), spec.tintRgb(), spec.outputRate(),
                spec.textureFolder(), spec.hardness(), spec.blastResistance());
        RegistryObject<EngineBlock> block = registerEngineBlock(EngineType.FLUX, ms, spec, spec.hardness(), spec.blastResistance());
        FLUX_ENGINES.add(block);
        return block;
    }

    public static RegistryObject<EngineBlock> registerSteamEngine(SteamEngineSpec spec) {
        MachineSpec ms = engineMachineSpec(spec.id(), spec.materialName(), spec.tintRgb(), spec.outputRate(),
                spec.textureFolder(), spec.hardness(), spec.blastResistance());
        RegistryObject<EngineBlock> block = registerEngineBlock(EngineType.STEAM, ms, spec, spec.hardness(), spec.blastResistance());
        STEAM_ENGINES.add(block);
        return block;
    }

    public static RegistryObject<EngineBlock> registerStrongSteamEngine(StrongSteamEngineSpec spec) {
        MachineSpec ms = engineMachineSpec(spec.id(), spec.materialName(), spec.tintRgb(), spec.outputRate(),
                spec.textureFolder(), spec.hardness(), spec.blastResistance());
        RegistryObject<EngineBlock> block = registerEngineBlock(EngineType.STEAM, ms, spec, spec.hardness(), spec.blastResistance());
        STRONG_STEAM_ENGINES.add(block);
        return block;
    }

    public static RegistryObject<EngineBlock> registerRotationEngine(RotationEngineSpec spec) {
        MachineSpec ms = engineMachineSpec(spec.id(), spec.materialName(), spec.tintRgb(), spec.outputRate(),
                spec.textureFolder(), spec.hardness(), spec.blastResistance());
        RegistryObject<EngineBlock> block = registerEngineBlock(EngineType.ROTATION, ms, spec, spec.hardness(), spec.blastResistance());
        ROTATION_ENGINES.add(block);
        return block;
    }

    public static RegistryObject<EngineBlock> registerDieselEngine(DieselEngineSpec spec) {
        MachineSpec ms = engineMachineSpec(spec.id(), spec.materialName(), spec.tintRgb(), spec.outputRate(),
                spec.textureFolder(), spec.hardness(), spec.blastResistance());
        RegistryObject<EngineBlock> block = registerEngineBlock(EngineType.DIESEL, ms, spec, spec.hardness(), spec.blastResistance());
        DIESEL_ENGINES.add(block);
        return block;
    }

    private static MachineSpec engineMachineSpec(String id, String matName, int tint, long output,
                                                  String textureFolder, float hardness, float blastResistance) {
        return new MachineSpec(id, matName, tint, 10000, output,
                "engines/" + textureFolder, hardness, blastResistance);
    }

    /** Second pass: registers per-engine {@link BlockEntityType}s. */
    public static void registerEngineBeTypes(DeferredRegister<BlockEntityType<?>> beRegistry) {
        for (RegistryObject<EngineBlock> blockRO : ENGINE_BLOCKS) {
            beRegistry.register("be_" + blockRO.getId().getPath(), () -> {
                EngineBlock block = blockRO.get();
                BlockEntityType<?> type = switch (block.engineType()) {
                    case ELECTRIC -> buildElectricBeType(block);
                    case FLUX -> buildFluxBeType(block);
                    case STEAM -> buildSteamBeType(block);
                    case ROTATION -> buildRotationBeType(block);
                    case DIESEL -> buildDieselBeType(block);
                };
                block.setBeTypeSupplier(() -> type);
                return type;
            });
        }
    }

    private static BlockEntityType<KineticElectricEngineBlockEntity> buildElectricBeType(EngineBlock block) {
        ElectricEngineSpec spec = block.engineSpec(ElectricEngineSpec.class);
        AtomicReference<BlockEntityType<KineticElectricEngineBlockEntity>> ref = new AtomicReference<>();
        BlockEntityType<KineticElectricEngineBlockEntity> type = BlockEntityType.Builder.of(
                (pos, state) -> {
                    KineticElectricEngineBlockEntity be = new KineticElectricEngineBlockEntity(ref.get(), pos, state);
                    be.setSpec(spec);
                    return be;
                }, block).build(null);
        ref.set(type);
        return type;
    }

    private static BlockEntityType<KineticFluxEngineBlockEntity> buildFluxBeType(EngineBlock block) {
        FluxEngineSpec spec = block.engineSpec(FluxEngineSpec.class);
        AtomicReference<BlockEntityType<KineticFluxEngineBlockEntity>> ref = new AtomicReference<>();
        BlockEntityType<KineticFluxEngineBlockEntity> type = BlockEntityType.Builder.of(
                (pos, state) -> {
                    KineticFluxEngineBlockEntity be = new KineticFluxEngineBlockEntity(ref.get(), pos, state);
                    be.setSpec(spec);
                    return be;
                }, block).build(null);
        ref.set(type);
        return type;
    }

    private static BlockEntityType<KineticSteamEngineBlockEntity> buildSteamBeType(EngineBlock block) {
        SteamEngineData spec = block.engineSpec(SteamEngineData.class);
        AtomicReference<BlockEntityType<KineticSteamEngineBlockEntity>> ref = new AtomicReference<>();
        BlockEntityType<KineticSteamEngineBlockEntity> type = BlockEntityType.Builder.of(
                (pos, state) -> {
                    KineticSteamEngineBlockEntity be = new KineticSteamEngineBlockEntity(ref.get(), pos, state);
                    be.setSpec(spec);
                    return be;
                }, block).build(null);
        ref.set(type);
        return type;
    }

    private static BlockEntityType<KineticRotationEngineBlockEntity> buildRotationBeType(EngineBlock block) {
        RotationEngineSpec spec = block.engineSpec(RotationEngineSpec.class);
        AtomicReference<BlockEntityType<KineticRotationEngineBlockEntity>> ref = new AtomicReference<>();
        BlockEntityType<KineticRotationEngineBlockEntity> type = BlockEntityType.Builder.of(
                (pos, state) -> {
                    KineticRotationEngineBlockEntity be = new KineticRotationEngineBlockEntity(ref.get(), pos, state);
                    be.setSpec(spec);
                    return be;
                }, block).build(null);
        ref.set(type);
        return type;
    }

    private static BlockEntityType<KineticDieselEngineBlockEntity> buildDieselBeType(EngineBlock block) {
        DieselEngineSpec spec = block.engineSpec(DieselEngineSpec.class);
        AtomicReference<BlockEntityType<KineticDieselEngineBlockEntity>> ref = new AtomicReference<>();
        BlockEntityType<KineticDieselEngineBlockEntity> type = BlockEntityType.Builder.of(
                (pos, state) -> {
                    KineticDieselEngineBlockEntity be = new KineticDieselEngineBlockEntity(ref.get(), pos, state);
                    be.setSpec(spec);
                    return be;
                }, block).build(null);
        ref.set(type);
        return type;
    }

    public static List<RegistryObject<EngineBlock>> electricEngines() {
        return Collections.unmodifiableList(ELECTRIC_ENGINES);
    }

    public static List<RegistryObject<EngineBlock>> fluxEngines() {
        return Collections.unmodifiableList(FLUX_ENGINES);
    }

    public static List<RegistryObject<EngineBlock>> steamEngines() {
        return Collections.unmodifiableList(STEAM_ENGINES);
    }

    public static List<RegistryObject<EngineBlock>> strongSteamEngines() {
        return Collections.unmodifiableList(STRONG_STEAM_ENGINES);
    }

    public static List<RegistryObject<EngineBlock>> rotationEngines() {
        return Collections.unmodifiableList(ROTATION_ENGINES);
    }

    public static List<RegistryObject<EngineBlock>> dieselEngines() {
        return Collections.unmodifiableList(DIESEL_ENGINES);
    }

    public static List<RegistryObject<EngineBlock>> allEngines() {
        return Collections.unmodifiableList(ENGINE_BLOCKS);
    }
}
