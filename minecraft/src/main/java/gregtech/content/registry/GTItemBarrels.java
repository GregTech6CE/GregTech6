package gregtech.content.registry;

import gregtech.content.block.inventory.ItemBarrelBlock;
import gregtech.content.storage.ItemBarrelCatalog;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class GTItemBarrels {
    private GTItemBarrels() {}

    public static final List<RegisteredBlock<ItemBarrelBlock>> ALL = ItemBarrelCatalog.ALL.stream()
            .map(spec -> RegisteredBlock.register(
                    spec.id(),
                    () -> new ItemBarrelBlock(spec),
                    block -> new BlockItem(block, new Item.Properties().stacksTo(16))))
            .toList();

    /** The static list queues the blocks; this hook forces class initialization at the block phase. */
    public static void registerAll() {}
}
