package gregtech.platform.neoforge.logistics;

import gregtech.content.registry.GTBlockEntities;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class StorageContainerCapabilities {
    @SubscribeEvent
    public static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, GTBlockEntities.DUNGEON_PORTAL.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, GTBlockEntities.DUNGEON_PORTAL.get(),
                (blockEntity, side) -> blockEntity.fluidHandler(side));
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, GTBlockEntities.DUNGEON_PORTAL.get(),
                (blockEntity, side) -> blockEntity.fluxHandler(side));
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, GTBlockEntities.ZPM_DISCHARGER.get(),
                (blockEntity, side) -> blockEntity.isRemoved() ? null : blockEntity.inventory());
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, GTBlockEntities.COIN_MOLD.get(),
                (blockEntity, side) -> blockEntity.itemHandler());
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, GTBlockEntities.DUST_FUNNEL.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, GTBlockEntities.SAP_BAG.get(),
                (blockEntity, side) -> blockEntity);
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, GTBlockEntities.PROCESSING_TOOL.get(),
                (blockEntity, side) -> blockEntity.itemHandler());
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, GTBlockEntities.PROCESSING_TOOL.get(),
                (blockEntity, side) -> blockEntity.fluidHandler());
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, GTBlockEntities.MATERIAL_ANVIL.get(),
                (blockEntity, side) -> blockEntity.itemHandler());
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, GTBlockEntities.EXTENDER.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, GTBlockEntities.EXTENDER.get(),
                (blockEntity, side) -> blockEntity.fluidHandler(side));
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, GTBlockEntities.FILTER.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, GTBlockEntities.FILTER.get(),
                (blockEntity, side) -> blockEntity.fluidHandler(side));
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, GTBlockEntities.EXTENDER.get(),
                (blockEntity, side) -> blockEntity.fluxHandler(side));
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, GTBlockEntities.USB_SWITCH.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, GTBlockEntities.ENDER_GARBAGE.get(),
                (blockEntity, side) -> blockEntity.itemHandler());
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, GTBlockEntities.ENDER_GARBAGE.get(),
                (blockEntity, side) -> blockEntity.fluidHandler());
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, GTBlockEntities.DRAWER_QUAD.get(),
                (blockEntity, side) -> blockEntity.itemHandler(side));
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, GTBlockEntities.BOTTLE_CRATE.get(),
                (blockEntity, side) -> blockEntity.isRemoved() ? null : blockEntity.items());
    }
}
