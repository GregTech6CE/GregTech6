package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.machine.LogisticsWireBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Shared logistics wire block and item catalog; the block entity type lives in {@link GTBlockEntities}. */
public final class GTLogisticsWire {
    private GTLogisticsWire() {}

    public static RegisteredBlock<LogisticsWireBlock> WIRE;

    public static void register(ContentRegistrar registrar) {
        if (WIRE != null) return;
        WIRE = registrar.block(
                REGISTRY_NAMESPACE,
                "logistics_wire",
                () -> new LogisticsWireBlock(BlockBehaviour.Properties.of()
                        .strength(1.0F, 2.0F)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL)),
                bare -> new BlockItem(bare, new Item.Properties()));
    }
}
