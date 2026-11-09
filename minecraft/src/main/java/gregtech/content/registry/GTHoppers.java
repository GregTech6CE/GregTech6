package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.machine.HopperBlock;
import gregtech.content.block.machine.HopperBlockItem;
import gregtech.content.block.machine.QueueHopperBlock;
import gregtech.content.block.machine.QueueHopperBlockItem;
import gregtech.content.transport.HopperCatalog;
import gregtech.engine.machine.HopperSpec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Shared hopper and queue hopper catalog. */
public final class GTHoppers {
    private static final List<RegisteredBlock<HopperBlock>> HOPPERS = new ArrayList<>();
    private static final List<RegisteredBlock<QueueHopperBlock>> QUEUES = new ArrayList<>();

    private GTHoppers() {}

    public static List<RegisteredBlock<HopperBlock>> hoppers() {
        return List.copyOf(HOPPERS);
    }

    public static List<RegisteredBlock<QueueHopperBlock>> queues() {
        return List.copyOf(QUEUES);
    }

    public static RegisteredBlock<HopperBlock> find(String id) {
        return HOPPERS.stream()
                .filter(block -> block.getId().getPath().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static void register(ContentRegistrar registrar) {
        if (!HOPPERS.isEmpty() || !QUEUES.isEmpty()) return;
        for (var row : HopperCatalog.ALL) {
            var spec = row.spec();
            if (row.queue()) {
                QUEUES.add(registrar.block(
                        REGISTRY_NAMESPACE,
                        spec.id(),
                        () -> new QueueHopperBlock(spec, properties(spec)),
                        hopper -> new QueueHopperBlockItem(hopper, new Item.Properties().stacksTo(16), spec)));
            } else {
                HOPPERS.add(registrar.block(
                        REGISTRY_NAMESPACE,
                        spec.id(),
                        () -> new HopperBlock(spec, properties(spec)),
                        hopper -> new HopperBlockItem(hopper, new Item.Properties().stacksTo(16), spec)));
            }
        }
    }

    private static BlockBehaviour.Properties properties(HopperSpec spec) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(spec.hardness(), spec.blastResistance())
                .requiresCorrectToolForDrops();
    }
}
