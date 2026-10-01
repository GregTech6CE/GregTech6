package com.gregtech.gregtech.platform.neoforge.fluid;

import com.gregtech.gregtech.platform.neoforge.smeltery.SmelteryRegistries;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Masson-style Neo capability registration bound to the integrated real crucible. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus=EventBusSubscriber.Bus.MOD)
public final class FluidCapabilities {
    private FluidCapabilities() {}
    @SubscribeEvent public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,com.gregtech.gregtech.registry.GTBlockEntities.PORTABLE_CONTAINER.get(),(vessel,side)->vessel.fluidHandler());
        event.registerItem(Capabilities.FluidHandler.ITEM,(stack,context)->((FluidDisplayItem)stack.getItem()).handler(stack),FluidRegistries.ITEMS.getEntries().stream().map(holder->holder.get()).toArray(net.minecraft.world.item.Item[]::new));
        event.registerItem(Capabilities.FluidHandler.ITEM,(stack,context)->((com.gregtech.gregtech.item.PortableFluidContainerItem)stack.getItem()).handler(stack),com.gregtech.gregtech.registry.GTToolBlocks.PORTABLE.stream().map(block->block.get().asItem()).toArray(net.minecraft.world.item.Item[]::new));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SmelteryRegistries.SMELTING_CRUCIBLE.get(),
                (crucible, side) -> side == Direction.DOWN ? null : crucible.moltenFluidHandler());
    }
}
