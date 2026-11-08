package gregtech.client;

import gregtech.platform.Mc;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Shared block / block-item model aliasing for 3D inventory display. */
public final class BlockItemClientModels {
    private BlockItemClientModels() {}

    /** Replace the inventory model of the block's item. */
    public static void aliasBlockItem(BakedModelMap models, Block block, BakedModel itemModel) {
        if (block.asItem() == net.minecraft.world.item.Items.AIR) {
            return;
        }
        ResourceLocation itemId = Mc.itemId(block.asItem());
        if (itemId != null) {
            models.put(itemId, "inventory", itemModel);
        }
    }

    /** Replace every world state of the block; the block's own state definition decides the keys. */
    public static void aliasWorldStates(BakedModelMap models, Block block, BakedModel worldModel) {
        ResourceLocation blockId = Mc.blockId(block);
        if (blockId == null) {
            return;
        }
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            models.putState(blockId, state, worldModel);
        }
    }

    public static void alias(BakedModelMap models, Block block, BakedModel worldModel, BakedModel itemModel) {
        aliasWorldStates(models, block, worldModel);
        aliasBlockItem(models, block, itemModel);
    }

    public static BakedModel asBlockItem(BakedModel shared) {
        return new BlockItemDisplayBakedModel(shared);
    }
}
