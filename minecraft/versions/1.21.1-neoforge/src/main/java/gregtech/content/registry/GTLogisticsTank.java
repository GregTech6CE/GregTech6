package gregtech.content.registry;

import gregtech.content.block.machine.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Registers the logistics barrel using the shared specification and entity implementation. */
public final class GTLogisticsTank {
    private GTLogisticsTank() {}

    public static DeferredHolder<Block, LogisticsTankBlock> BLOCK;

    public static void initialize() {
        var spec = gregtech.content.transport.fluid.LogisticsTankSpec.spec();
        BLOCK = GTBlocks.BLOCKS.register(
                spec.id(), () -> new LogisticsTankBlock(spec, TankBlock.defaultProperties(spec)));
        GTBlocks.BLOCK_ITEMS.register(
                spec.id(),
                () -> new gregtech.content.transport.fluid.FluidTransportBlockItem(
                        BLOCK.get(), new net.minecraft.world.item.Item.Properties().stacksTo(16)));
    }
}
