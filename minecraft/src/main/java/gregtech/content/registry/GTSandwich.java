package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.misc.SandwichBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class GTSandwich {
    private GTSandwich() {}

    public static RegisteredBlock<SandwichBlock> SANDWICH_BLOCK;

    public static void initialize(ContentRegistrar registrar) {
        SANDWICH_BLOCK = registrar.block(
                REGISTRY_NAMESPACE,
                "sandwich_block",
                () -> new SandwichBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_YELLOW)
                        .strength(.25f, 0f)
                        .sound(SoundType.WOOL)
                        .noOcclusion()),
                block -> new gregtech.content.item.SandwichBlockItem(block, new Item.Properties()));
    }
}
