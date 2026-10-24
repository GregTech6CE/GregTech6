package gregtech.registry;

import gregtech.block.inventory.ItemBarrelBlock;
import gregtech.content.storage.ItemBarrelCatalog;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import java.util.List;

public final class GTItemBarrels {
    private GTItemBarrels() {}
    public static final List<RegisteredBlock<ItemBarrelBlock>> ALL = ItemBarrelCatalog.ALL.stream()
            .map(spec -> RegisteredBlock.register(spec.id(), () -> new ItemBarrelBlock(spec),
                    block -> new BlockItem(block, new Item.Properties().stacksTo(16)))).toList();
    public static void registerAll() {}
}
