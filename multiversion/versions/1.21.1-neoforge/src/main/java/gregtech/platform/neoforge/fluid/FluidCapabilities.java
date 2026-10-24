package gregtech.platform.neoforge.fluid;

import gregtech.platform.neoforge.smeltery.SmelteryRegistries;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Neo fluid capabilities for display items, portable vessels and the smelting crucible. */
@EventBusSubscriber(modid = gregtech.internal.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class FluidCapabilities {
    private FluidCapabilities() {
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> ((gregtech.content.item.WoodenBucketItem) stack.getItem()).handler(stack),
                gregtech.content.registry.GTMultiItems.all().stream().map(holder -> holder.get())
                        .filter(item -> item instanceof gregtech.content.item.WoodenBucketItem)
                        .toArray(net.minecraft.world.item.Item[]::new));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                gregtech.content.registry.GTBlockEntities.PORTABLE_CONTAINER.get(),
                (vessel, side) -> vessel.fluidHandler());
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> ((FluidDisplayItem) stack.getItem()).handler(stack),
                FluidRegistries.ITEMS.getEntries().stream().map(holder -> holder.get())
                        .toArray(net.minecraft.world.item.Item[]::new));
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> ((gregtech.content.item.PortableFluidContainerItem) stack.getItem()).handler(stack),
                gregtech.content.registry.GTToolBlocks.PORTABLE.stream().map(block -> block.get().asItem())
                        .toArray(net.minecraft.world.item.Item[]::new));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SmelteryRegistries.SMELTING_CRUCIBLE.get(),
                (crucible, side) -> side == Direction.DOWN ? null : crucible.moltenFluidHandler());
    }
}
