package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.misc.CoinPileBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class GTCoinPiles {

    private GTCoinPiles() {}

    public static RegisteredBlock<CoinPileBlock> COIN_PILE;

    public static void initialize(ContentRegistrar registrar) {
        COIN_PILE = registrar.block(
                REGISTRY_NAMESPACE,
                "coin_pile",
                () -> new CoinPileBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.GOLD)
                        .strength(3, 3)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL)
                        .noOcclusion()),
                block -> new BlockItem(block, new Item.Properties()));
    }
}
