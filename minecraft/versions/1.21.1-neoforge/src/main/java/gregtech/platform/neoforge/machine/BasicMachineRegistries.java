package gregtech.platform.neoforge.machine;

import gregtech.api.material.*;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.machine.BasicMachineDefinitions;
import gregtech.engine.machine.BasicMachineSpec;
import gregtech.engine.material.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;

/** Native registrations from the shared catalog; original large recipe/matter/distillation factories are bound. */
public final class BasicMachineRegistries {
    private BasicMachineRegistries() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "gt6ce");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6ce");
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "gt6ce");
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "gt6ce");
    public static final DeferredHolder<MenuType<?>, MenuType<gregtech.client.gui.BasicMachineContainerMenu>> MENU =
            MENUS.register(
                    "basic_machine",
                    () -> IMenuTypeExtension.create(gregtech.client.gui.BasicMachineContainerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<gregtech.client.gui.BehaviorMachineMenu>> BEHAVIOR_MENU =
            MENUS.register(
                    "behavior_machine",
                    () -> IMenuTypeExtension.create((id, inventory, buffer) ->
                            new gregtech.client.gui.BehaviorMachineMenu(id, inventory, buffer)));
    public static final DeferredHolder<MenuType<?>, MenuType<gregtech.client.gui.PartMenu>> PART_MENU = MENUS.register(
            "part",
            () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new gregtech.client.gui.PartMenu(id, inventory, buffer)));
    private static final List<DeferredHolder<Block, BasicMachineBlock>> MACHINES = new ArrayList<>();
    private static final List<DeferredHolder<BlockEntityType<?>, BlockEntityType<BasicMachineBlockEntity>>> TYPES =
            new ArrayList<>();

    public static List<DeferredHolder<Block, BasicMachineBlock>> all() {
        return Collections.unmodifiableList(MACHINES);
    }

    public static List<DeferredHolder<BlockEntityType<?>, BlockEntityType<BasicMachineBlockEntity>>> types() {
        return Collections.unmodifiableList(TYPES);
    }

    public static void register(IEventBus bus) {
        var blocks = new LinkedHashMap<String, DeferredRegister<Block>>();
        var items = new LinkedHashMap<String, DeferredRegister<Item>>();
        var entities = new LinkedHashMap<String, DeferredRegister<BlockEntityType<?>>>();
        blocks.put("gt6ce", BLOCKS);
        items.put("gt6ce", ITEMS);
        entities.put("gt6ce", ENTITIES);
        for (BasicMachineSpec spec : BasicMachineDefinitions.specifications()) {
            String namespace = spec.namespace();
            var blockReg = blocks.computeIfAbsent(namespace, ns -> {
                var created = DeferredRegister.create(Registries.BLOCK, ns);
                created.register(bus);
                return created;
            });
            var itemReg = items.computeIfAbsent(namespace, ns -> {
                var created = DeferredRegister.create(Registries.ITEM, ns);
                created.register(bus);
                return created;
            });
            var entityReg = entities.computeIfAbsent(namespace, ns -> {
                var created = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ns);
                created.register(bus);
                return created;
            });
            var block = gregtech.content.registry.MultiblockRegistration.register(
                    namespace,
                    spec.id(),
                    () -> spec.controllerKind() == gregtech.api.machine.MachineControllerKind.DISTILLATION
                            ? new gregtech.content.block.machine.MultiblockControllerBlock(
                                    spec,
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(spec.hardness(), spec.blastResistance())
                                            .requiresCorrectToolForDrops())
                            : new BasicMachineBlock(
                                    spec,
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(spec.hardness(), spec.blastResistance())
                                            .requiresCorrectToolForDrops()),
                    blockReg::register);
            MACHINES.add(block);
            itemReg.register(spec.id(), () -> new BasicMachineItem(block.get(), new Item.Properties().stacksTo(16)));
            var type = entityReg.register("be_" + spec.id(), () -> {
                AtomicReference<BlockEntityType<BasicMachineBlockEntity>> ref = new AtomicReference<>();
                var value = BlockEntityType.Builder.of(
                                (pos, state) -> {
                                    var be = block.get().createBlockEntity(ref.get(), pos, state);
                                    be.setSpec(spec);
                                    return be;
                                },
                                block.get())
                        .build(null);
                ref.set(value);
                block.get().setBeTypeSupplier(() -> value);
                return value;
            });
            TYPES.add(type);
        }
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        BehaviorMachineRegistration.register(bus);
        FuelGeneratorRegistration.register(bus);
        CompressedGasTankRegistration.register(bus);
        bus.addListener(BasicMachineRegistries::capabilities);
        bus.addListener(gregtech.network.MachinePayloads::register);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.BUMBLIARY.get(),
                gregtech.content.blockentity.tool.BumbliaryBlockEntity::itemCapability);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.BEDROCK_DRILL.get(),
                gregtech.content.blockentity.machine.BedrockDrillControllerBlockEntity::itemCapability);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.REACTOR_CORE.get(),
                gregtech.content.blockentity.energy.ReactorCoreBlockEntity::fluidCapability);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.REACTOR_CORE.get(),
                gregtech.content.blockentity.energy.ReactorCoreBlockEntity::itemCapability);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.REACTOR_CORE_2X2.get(),
                gregtech.content.blockentity.energy.ReactorCoreBlockEntity::fluidCapability);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.REACTOR_CORE_2X2.get(),
                gregtech.content.blockentity.energy.ReactorCoreBlockEntity::itemCapability);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.BEDROCK_DRILL.get(),
                gregtech.content.blockentity.machine.BedrockDrillControllerBlockEntity::fluidCapability);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.LARGE_CRUCIBLE.get(),
                gregtech.content.blockentity.machine.LargeCrucibleControllerBlockEntity::fluidCapability);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.LARGE_CRUCIBLE.get(),
                gregtech.content.blockentity.machine.LargeCrucibleControllerBlockEntity::itemCapability);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.LARGE_TURBINE.get(),
                gregtech.content.blockentity.machine.LargeTurbineControllerBlockEntity::fluidCapability);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.LARGE_GAS_TURBINE.get(),
                gregtech.content.blockentity.machine.LargeGasTurbineControllerBlockEntity::fluidCapability);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.LARGE_HEAT_EXCHANGER.get(),
                gregtech.content.blockentity.machine.LargeHeatExchangerControllerBlockEntity::fluidCapability);

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.ORIGINAL_LARGE_BOILER.get(),
                gregtech.content.blockentity.machine.OriginalLargeBoilerControllerBlockEntity::fluidCapability);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.MULTIBLOCK_TANK.get(),
                gregtech.content.blockentity.machine.MultiblockTankControllerBlockEntity::fluidCapability);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.COKE_OVEN.get(),
                gregtech.content.blockentity.machine.CokeOvenControllerBlockEntity::itemCapability);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.COKE_OVEN.get(),
                gregtech.content.blockentity.machine.CokeOvenControllerBlockEntity::fluidCapability);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.CRYO_DISTILLATION.get(),
                gregtech.content.blockentity.machine.CryoDistillationControllerBlockEntity::fluidCapability);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.CRYO_DISTILLATION.get(),
                gregtech.content.blockentity.machine.CryoDistillationControllerBlockEntity::itemCapability);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.MULTIBLOCK_PORT.get(),
                gregtech.content.blockentity.machine.MultiblockPortBlockEntity::itemCapability);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.MULTIBLOCK_PORT.get(),
                gregtech.content.blockentity.machine.MultiblockPortBlockEntity::fluidCapability);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.ADDON_ENERGY_PART.get(),
                gregtech.content.blockentity.machine.AddonEnergyPartBlockEntity::itemCapability);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.ADDON_ENERGY_PART.get(),
                gregtech.content.blockentity.machine.AddonEnergyPartBlockEntity::fluidCapability);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.ADDON_FUNCTIONAL_PART.get(),
                gregtech.content.blockentity.machine.AddonFunctionalPartBlockEntity::itemCapability);
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.ADDON_FUNCTIONAL_PART.get(),
                gregtech.content.blockentity.machine.AddonFunctionalPartBlockEntity::fluidCapability);
        for (var type : TYPES) {
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK, type.get(), BasicMachineBlockEntity::itemCapability);
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK, type.get(), BasicMachineBlockEntity::fluidCapability);
        }
    }

    public static void registerCompositions() {
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (!(block instanceof BasicMachineBlock machine)) continue;
            var spec = machine.basicSpec();
            gregtech.content.machine.OriginalMachineMaterialData.find(spec.machineName(), spec.tier())
                    .ifPresent(data -> ItemMaterialRegistry.register(block.asItem(), data));
        }
    }
}
