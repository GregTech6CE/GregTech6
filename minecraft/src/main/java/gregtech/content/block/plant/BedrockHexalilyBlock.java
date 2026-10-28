package gregtech.content.block.plant;

import gregtech.content.block.IconSetLilyBlock;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

/** GT6 BlockFlowersA meta 8 keeps lily placement and drops its own flower. */
public final class BedrockHexalilyBlock extends IconSetLilyBlock {
    public BedrockHexalilyBlock(Properties properties, String iconName) {
        super(properties, iconName);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(this));
    }
}
