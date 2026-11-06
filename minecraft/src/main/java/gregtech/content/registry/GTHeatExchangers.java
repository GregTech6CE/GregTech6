package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.machine.HeatExchangerBlock;
import gregtech.content.energy.HeatExchangerCatalog;
import gregtech.content.item.HeatExchangerItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class GTHeatExchangers {
    private GTHeatExchangers() {}

    private static final List<RegisteredBlock<HeatExchangerBlock>> ALL = new ArrayList<>();

    public static List<RegisteredBlock<HeatExchangerBlock>> all() {
        return List.copyOf(ALL);
    }

    public static void registerAll(ContentRegistrar registrar) {
        if (!ALL.isEmpty()) throw new IllegalStateException("Heat exchangers registered twice");
        for (var spec : HeatExchangerCatalog.ALL)
            ALL.add(registrar.block(
                    REGISTRY_NAMESPACE,
                    spec.id(),
                    () -> new HeatExchangerBlock(
                            spec,
                            BlockBehaviour.Properties.of()
                                    .strength(spec.hardness())
                                    .requiresCorrectToolForDrops()),
                    HeatExchangerItem::new));
    }
}
