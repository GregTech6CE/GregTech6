package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.machine.HeatExchangerBlock;
import com.gregtech.gregtech.content.energy.HeatExchangerCatalog;
import com.gregtech.gregtech.item.HeatExchangerItem;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.ArrayList;
import java.util.List;

public final class GTHeatExchangers {
    private GTHeatExchangers() {}
    private static final List<RegisteredBlock<HeatExchangerBlock>> ALL = new ArrayList<>();
    public static List<RegisteredBlock<HeatExchangerBlock>> all() { return List.copyOf(ALL); }
    public static void registerAll() {
        for (var spec : HeatExchangerCatalog.ALL) ALL.add(RegisteredBlock.register(spec.id(),
                () -> new HeatExchangerBlock(spec, BlockBehaviour.Properties.of().strength(spec.hardness()).requiresCorrectToolForDrops()),
                block -> new HeatExchangerItem(block)));
    }
}
