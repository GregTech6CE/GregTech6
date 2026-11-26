package gregtech.content.block.inventory;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
//? if >=1.21.1 {
//?} else {
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
//?}

public final class BottleCrateBlockItem extends BlockItem {
    public BottleCrateBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

//? if >=1.21.1 {
//?} else {
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return gregtech.client.BottleCrateItemRenderer.instance();
            }
        });
    }
//?}
}
