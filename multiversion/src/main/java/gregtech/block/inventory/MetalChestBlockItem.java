package gregtech.block.inventory;
//? if >=1.21.1 {
/*/^* Client extension registration uses the native platform event. ^/
public final class MetalChestBlockItem extends net.minecraft.world.item.BlockItem {public MetalChestBlockItem(MetalChestBlock block,Properties properties){super(block,properties);}}
*///?} else {

import net.minecraft.world.item.BlockItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;

public final class MetalChestBlockItem extends BlockItem {
    public MetalChestBlockItem(MetalChestBlock block, Properties properties) { super(block, properties); }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return gregtech.client.MetalChestItemRenderer.instance();
            }
        });
    }
}
//?}
