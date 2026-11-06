package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.energy.PumpBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** GT6 rotational pumps — RU-powered fluid drainage, 4 tiers. */
public final class GTPumps {
    private static final List<RegisteredBlock<PumpBlock>> ALL = new ArrayList<>();

    private GTPumps() {}

    public static List<RegisteredBlock<PumpBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void registerAll(ContentRegistrar registrar) {
        for (var spec : gregtech.content.energy.PumpCatalog.all()) {
            String id = spec.id();
            RegisteredBlock<PumpBlock> block = registrar.block(
                    REGISTRY_NAMESPACE,
                    id,
                    () -> new PumpBlock(
                            spec,
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(4.0f, 6.0f)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()),
                    bare -> new BlockItem(bare, new Item.Properties()));
            ALL.add(block);
        }
    }

    public static void bootstrap() {
        if (ALL.isEmpty()) throw new IllegalStateException("GTPumps failed to initialize");
    }
}
