package gregtech.platform.neoforge.smeltery;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.RegisteredBlock;
import gregtech.content.registry.RegisteredItem;
import gregtech.engine.machine.InitialSmelteryDefinitions;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Real GT6 block/BE identities; domain specifications come exclusively from core. */
public final class SmelteryRegistries {
    private SmelteryRegistries() {}

    public static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, REGISTRY_NAMESPACE);
    public static RegisteredItem<? extends gregtech.content.item.GTToolItem> CHISEL;
    public static RegisteredItem<? extends gregtech.content.item.GTToolItem> PINCERS;

    private static final java.util.List<RegisteredBlock<gregtech.content.block.machine.SolidBurningBoxBlock>>
            SOLID_BOXES = new java.util.ArrayList<>();
    private static final java.util.List<RegisteredBlock<gregtech.content.block.machine.BurningBoxBlock>> FUEL_BOXES =
            new java.util.ArrayList<>();

    public static java.util.List<RegisteredBlock<gregtech.content.block.machine.SolidBurningBoxBlock>> solidBoxes() {
        return java.util.List.copyOf(SOLID_BOXES);
    }

    public static java.util.List<RegisteredBlock<gregtech.content.block.machine.BurningBoxBlock>> fuelBoxes() {
        return java.util.List.copyOf(FUEL_BOXES);
    }

    private static BlockBehaviour.Properties boxProperties(gregtech.engine.machine.MachineSpec spec) {
        return BlockBehaviour.Properties.of()
                .mapColor(net.minecraft.world.level.material.MapColor.METAL)
                .strength(spec.hardness(), spec.blastResistance())
                .requiresCorrectToolForDrops()
                .lightLevel(state -> state.getValue(gregtech.content.block.machine.GTFacingMachineBlock.LIT) ? 13 : 0);
    }

    private static RegisteredBlock<gregtech.content.block.machine.SolidBurningBoxBlock> solidBox(
            ContentRegistrar registrar, gregtech.engine.machine.MachineSpec spec) {
        var holder = registrar.block(
                REGISTRY_NAMESPACE,
                spec.id(),
                () -> new gregtech.content.block.machine.SolidBurningBoxBlock(spec, boxProperties(spec)),
                box -> new gregtech.content.block.machine.BurningBoxBlockItem(
                        box, new Item.Properties().stacksTo(16), spec));
        SOLID_BOXES.add(holder);
        return holder;
    }

    private static void fuelBox(ContentRegistrar registrar, gregtech.engine.machine.BurningBoxDefinitions.Entry entry) {
        var spec = entry.spec();
        var holder = registrar.block(
                REGISTRY_NAMESPACE,
                spec.id(),
                () -> new gregtech.content.block.machine.BurningBoxBlock(entry.fuelType(), spec, boxProperties(spec)),
                box -> new gregtech.content.block.machine.BurningBoxBlockItem(
                        box, new Item.Properties().stacksTo(16), spec));
        FUEL_BOXES.add(holder);
    }

    public static RegisteredBlock<gregtech.content.block.machine.SolidBurningBoxBlock> BRICK_BURNING_BOX;
    private static final java.util.List<RegisteredBlock<gregtech.content.block.machine.SmeltingCrucibleBlock>>
            CRUCIBLES = new java.util.ArrayList<>();
    public static RegisteredBlock<gregtech.content.block.machine.SmeltingCrucibleBlock> CERAMIC_CRUCIBLE;

    private static RegisteredBlock<gregtech.content.block.machine.SmeltingCrucibleBlock> crucible(
            ContentRegistrar registrar, gregtech.engine.machine.CrucibleSpec spec) {
        var block = registrar.blockWithoutItem(
                REGISTRY_NAMESPACE,
                spec.id(),
                () -> new gregtech.content.block.machine.SmeltingCrucibleBlock(
                        spec,
                        gregtech.content.block.machine.SmeltingCrucibleBlock.defaultProperties(spec)
                                .noOcclusion()));
        CRUCIBLES.add(block);
        return block;
    }

    public static java.util.List<RegisteredBlock<gregtech.content.block.machine.SmeltingCrucibleBlock>> crucibles() {
        return java.util.List.copyOf(CRUCIBLES);
    }

    private static final java.util.List<RegisteredBlock<gregtech.content.block.machine.MoldBlock>> MOLDS =
            new java.util.ArrayList<>();
    private static final java.util.List<RegisteredBlock<gregtech.content.block.machine.MoldBasinBlock>> BASINS =
            new java.util.ArrayList<>();
    private static final java.util.List<RegisteredBlock<gregtech.content.block.machine.CrucibleFaucetBlock>> FAUCETS =
            new java.util.ArrayList<>();
    private static final java.util.List<RegisteredBlock<gregtech.content.block.machine.CrucibleCrossingBlock>>
            CROSSINGS = new java.util.ArrayList<>();

    public static java.util.List<RegisteredBlock<gregtech.content.block.machine.MoldBlock>> molds() {
        return java.util.List.copyOf(MOLDS);
    }

    public static java.util.List<RegisteredBlock<gregtech.content.block.machine.MoldBasinBlock>> basins() {
        return java.util.List.copyOf(BASINS);
    }

    public static java.util.List<RegisteredBlock<gregtech.content.block.machine.CrucibleFaucetBlock>> faucets() {
        return java.util.List.copyOf(FAUCETS);
    }

    public static java.util.List<RegisteredBlock<gregtech.content.block.machine.CrucibleCrossingBlock>> crossings() {
        return java.util.List.copyOf(CROSSINGS);
    }

    private static RegisteredBlock<gregtech.content.block.machine.MoldBlock> mold(
            ContentRegistrar registrar, gregtech.engine.machine.CrucibleSpec base) {
        var spec = gregtech.engine.machine.SmelteryCompanionDefinitions.of(
                base, gregtech.engine.machine.SmelteryCompanionDefinitions.Kind.MOLD);
        var holder = registrar.block(
                REGISTRY_NAMESPACE,
                spec.id(),
                () -> new gregtech.content.block.machine.MoldBlock(
                        spec, gregtech.content.block.machine.MoldBlock.defaultProperties(spec)),
                mold -> new gregtech.content.block.machine.CrucibleHullBlockItem(
                        mold, new Item.Properties().stacksTo(16), spec));
        MOLDS.add(holder);
        return holder;
    }

    public static RegisteredBlock<gregtech.content.block.machine.MoldBlock> CERAMIC_MOLD;

    private static void otherCompanions(ContentRegistrar registrar, gregtech.engine.machine.CrucibleSpec base) {
        var basin = gregtech.engine.machine.SmelteryCompanionDefinitions.of(
                base, gregtech.engine.machine.SmelteryCompanionDefinitions.Kind.BASIN);
        var b = registrar.block(
                REGISTRY_NAMESPACE,
                basin.id(),
                () -> new gregtech.content.block.machine.MoldBasinBlock(
                        basin, gregtech.content.block.machine.MoldBasinBlock.defaultProperties(basin)),
                hull -> new gregtech.content.block.machine.CrucibleHullBlockItem(
                        hull, new Item.Properties().stacksTo(16), basin));
        BASINS.add(b);
        var faucet = gregtech.engine.machine.SmelteryCompanionDefinitions.of(
                base, gregtech.engine.machine.SmelteryCompanionDefinitions.Kind.FAUCET);
        var f = registrar.block(
                REGISTRY_NAMESPACE,
                faucet.id(),
                () -> new gregtech.content.block.machine.CrucibleFaucetBlock(
                        faucet, gregtech.content.block.machine.CrucibleFaucetBlock.defaultProperties(faucet)),
                hull -> new gregtech.content.block.machine.CrucibleHullBlockItem(
                        hull, new Item.Properties().stacksTo(16), faucet));
        FAUCETS.add(f);
        var crossing = gregtech.engine.machine.SmelteryCompanionDefinitions.of(
                base, gregtech.engine.machine.SmelteryCompanionDefinitions.Kind.CROSSING);
        var x = registrar.block(
                REGISTRY_NAMESPACE,
                crossing.id(),
                () -> new gregtech.content.block.machine.CrucibleCrossingBlock(
                        crossing, gregtech.content.block.machine.CrucibleCrossingBlock.defaultProperties(crossing)),
                hull -> new gregtech.content.block.machine.CrucibleHullBlockItem(
                        hull, new Item.Properties().stacksTo(16), crossing));
        CROSSINGS.add(x);
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolidBurningBoxEntity>> SOLID_BURNING_BOX =
            ENTITIES.register(
                    "solid_burning_box",
                    () -> BlockEntityType.Builder.of(
                                    SolidBurningBoxEntity::new,
                                    SOLID_BOXES.stream()
                                            .map(RegisteredBlock::get)
                                            .toArray(net.minecraft.world.level.block.Block[]::new))
                            .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmeltingCrucibleEntity>> SMELTING_CRUCIBLE =
            ENTITIES.register(
                    "smelting_crucible",
                    () -> BlockEntityType.Builder.of(
                                    SmeltingCrucibleEntity::new,
                                    CRUCIBLES.stream()
                                            .map(RegisteredBlock::get)
                                            .toArray(net.minecraft.world.level.block.Block[]::new))
                            .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MoldEntity>> MOLD = ENTITIES.register(
            "mold",
            () -> BlockEntityType.Builder.of(
                            MoldEntity::new,
                            MOLDS.stream()
                                    .map(RegisteredBlock::get)
                                    .toArray(net.minecraft.world.level.block.Block[]::new))
                    .build(null));

    public static void register(ContentRegistrar registrar) {
        CHISEL = gregtech.content.registry.GTToolItems.CHISEL;
        PINCERS = gregtech.content.registry.GTToolItems.PINCERS;
        BRICK_BURNING_BOX = solidBox(registrar, InitialSmelteryDefinitions.brickBurningBox());
        CERAMIC_CRUCIBLE = crucible(registrar, InitialSmelteryDefinitions.ceramicCrucible());
        CERAMIC_MOLD = mold(registrar, InitialSmelteryDefinitions.ceramicCrucible());
        registrar.item(
                REGISTRY_NAMESPACE,
                "smelting_crucible_ceramic",
                () -> new gregtech.content.block.machine.SmeltingCrucibleBlockItem(
                        CERAMIC_CRUCIBLE.get(),
                        new Item.Properties().stacksTo(16),
                        InitialSmelteryDefinitions.ceramicCrucible()));
        for (var entry : gregtech.engine.machine.BurningBoxDefinitions.all()) {
            if (entry.spec().id().equals("burning_box_solid_brick")) continue;
            if (entry.fuelType() == gregtech.engine.machine.BurningBoxFuelType.SOLID) solidBox(registrar, entry.spec());
            else fuelBox(registrar, entry);
        }
        com.mojang.logging.LogUtils.getLogger()
                .info(
                        "[gregtech] Native original burning boxes: solid={} fluid/gas/fluidized={}",
                        SOLID_BOXES.size(),
                        FUEL_BOXES.size());
        for (var spec : gregtech.engine.machine.OriginalCrucibleDefinitions.all()) {
            if (spec.id().equals("smelting_crucible_ceramic")) continue;
            var holder = crucible(registrar, spec);
            registrar.item(
                    REGISTRY_NAMESPACE,
                    spec.id(),
                    () -> new gregtech.content.block.machine.SmeltingCrucibleBlockItem(
                            holder.get(), new Item.Properties().stacksTo(16), spec));
        }
        for (var base : gregtech.engine.machine.OriginalCrucibleDefinitions.all()) {
            if (!base.id().equals("smelting_crucible_ceramic")) mold(registrar, base);
            otherCompanions(registrar, base);
        }
        com.mojang.logging.LogUtils.getLogger()
                .info(
                        "[gregtech] Native original smeltery companions: molds={} basins={} faucets={}"
                                + " crossings={}",
                        MOLDS.size(),
                        BASINS.size(),
                        FAUCETS.size(),
                        CROSSINGS.size());
        com.mojang.logging.LogUtils.getLogger()
                .info("[gregtech] Native original smelting crucibles: {}", CRUCIBLES.size());
    }

    public static void submit(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(SmelteryRegistries::capabilities);
    }

    private static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                SOLID_BURNING_BOX.get(),
                (be, side) -> be.itemCapability(side));
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.BURNING_BOX.get(),
                gregtech.content.blockentity.machine.BurningBoxBlockEntity::itemCapability);
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.BURNING_BOX.get(),
                gregtech.content.blockentity.machine.BurningBoxBlockEntity::fluidCapability);
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.HEAT_EXCHANGER.get(),
                gregtech.content.blockentity.machine.HeatExchangerBlockEntity::fluidCapability);
    }
}
