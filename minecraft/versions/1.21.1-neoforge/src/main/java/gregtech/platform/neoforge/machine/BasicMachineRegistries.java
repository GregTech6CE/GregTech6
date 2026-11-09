package gregtech.platform.neoforge.machine;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.blockentity.machine.BehaviorMachineBlockEntity;
import gregtech.content.blockentity.machine.CompressedGasTankBlockEntity;
import gregtech.content.blockentity.machine.FuelGeneratorBlockEntity;
import gregtech.content.registry.BehaviorMachineRegistration;
import gregtech.content.registry.CompressedGasTankRegistration;
import gregtech.content.registry.FuelGeneratorRegistration;
import gregtech.content.registry.GTBasicMachines;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** NeoForge menus, capabilities and payload wiring for the shared machine catalogs. */
public final class BasicMachineRegistries {
    private BasicMachineRegistries() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, REGISTRY_NAMESPACE);
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

    public static void submit(IEventBus bus) {
        MENUS.register(bus);
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
        for (var type : GTBasicMachines.types()) {
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK, type.get(), BasicMachineBlockEntity::itemCapability);
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK, type.get(), BasicMachineBlockEntity::fluidCapability);
        }
        for (var type : BehaviorMachineRegistration.types()) {
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK, type.get(), BehaviorMachineBlockEntity::itemCapability);
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK, type.get(), BehaviorMachineBlockEntity::fluidCapability);
        }
        for (var type : FuelGeneratorRegistration.types())
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK, type.get(), FuelGeneratorBlockEntity::fluidCapability);
        for (var type : CompressedGasTankRegistration.types())
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK, type.get(), CompressedGasTankBlockEntity::fluidCapability);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.HOPPER.get(),
                (be, side) -> be.capabilityHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.QUEUE_HOPPER.get(),
                (be, side) -> be.capabilityHandler(side));
    }

    public static void registerCompositions() {
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (!(block instanceof BasicMachineBlock machine)) continue;
            var spec = machine.basicSpec();
            gregtech.content.machine.OriginalMachineMaterialData.find(spec.machineName(), spec.tier())
                    .ifPresent(data -> gregtech.engine.material.ItemMaterialRegistry.register(block.asItem(), data));
        }
    }
}
