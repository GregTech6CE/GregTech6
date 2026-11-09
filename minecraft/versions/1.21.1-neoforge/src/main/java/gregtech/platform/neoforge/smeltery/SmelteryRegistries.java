package gregtech.platform.neoforge.smeltery;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.GTBurningBoxes;
import gregtech.content.registry.GTSmeltery;
import gregtech.content.registry.RegisteredBlock;
import gregtech.content.registry.RegisteredItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** NeoForge block entity types and capabilities for the shared smeltery catalogs. */
public final class SmelteryRegistries {
    private SmelteryRegistries() {}

    public static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, REGISTRY_NAMESPACE);
    public static RegisteredItem<? extends gregtech.content.item.GTToolItem> CHISEL;
    public static RegisteredItem<? extends gregtech.content.item.GTToolItem> PINCERS;

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolidBurningBoxEntity>> SOLID_BURNING_BOX =
            ENTITIES.register(
                    "solid_burning_box",
                    () -> BlockEntityType.Builder.of(
                                    SolidBurningBoxEntity::new,
                                    GTBurningBoxes.solidBoxes().stream()
                                            .map(RegisteredBlock::get)
                                            .toArray(net.minecraft.world.level.block.Block[]::new))
                            .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmeltingCrucibleEntity>> SMELTING_CRUCIBLE =
            ENTITIES.register(
                    "smelting_crucible",
                    () -> BlockEntityType.Builder.of(
                                    SmeltingCrucibleEntity::new,
                                    GTSmeltery.crucibles().stream()
                                            .map(RegisteredBlock::get)
                                            .toArray(net.minecraft.world.level.block.Block[]::new))
                            .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MoldEntity>> MOLD = ENTITIES.register(
            "mold",
            () -> BlockEntityType.Builder.of(
                            MoldEntity::new,
                            GTSmeltery.molds().stream()
                                    .map(RegisteredBlock::get)
                                    .toArray(net.minecraft.world.level.block.Block[]::new))
                    .build(null));

    public static void register(ContentRegistrar registrar) {
        CHISEL = gregtech.content.registry.GTToolItems.CHISEL;
        PINCERS = gregtech.content.registry.GTToolItems.PINCERS;
        GTBurningBoxes.registerAll(registrar);
        GTSmeltery.register(registrar);
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
