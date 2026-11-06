package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.inventory.ItemBarrelBlock;
import gregtech.content.storage.ItemBarrelCatalog;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class GTItemBarrels {
    private GTItemBarrels() {}

    public static final List<RegisteredBlock<ItemBarrelBlock>> ALL = new ArrayList<>();

    /** Queues the catalog at the block phase; the platform entry submits the registers later. */
    public static void registerAll(ContentRegistrar registrar) {
        if (!ALL.isEmpty()) throw new IllegalStateException("Item barrels registered twice");
        for (var spec : ItemBarrelCatalog.ALL)
            ALL.add(registrar.block(
                    REGISTRY_NAMESPACE,
                    spec.id(),
                    () -> new ItemBarrelBlock(spec),
                    block -> new BlockItem(block, new Item.Properties().stacksTo(16))));
    }
}
