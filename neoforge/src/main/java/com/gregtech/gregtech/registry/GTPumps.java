package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.machine.PumpSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.energy.PumpBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 rotational pumps — RU-powered fluid drainage, 4 tiers. */
public final class GTPumps {
    private static final List<DeferredHolder<Block,PumpBlock>> ALL = new ArrayList<>();

    private GTPumps() {}

    public static List<DeferredHolder<Block,PumpBlock>> all() { return Collections.unmodifiableList(ALL); }

    public static void registerAll() {
        for(var spec:com.gregtech.gregtech.content.energy.PumpCatalog.all()){
            String id=spec.id();
            DeferredHolder<Block,PumpBlock> block = GTBlocks.BLOCKS.register(id,
                    () -> new PumpBlock(spec, BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(4.0f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));
            ALL.add(block);
            GTBlocks.BLOCK_ITEMS.register(id,
                    () -> new BlockItem(block.get(), new Item.Properties()));
        }
    }

    public static void bootstrap() {
        if (ALL.isEmpty()) throw new IllegalStateException("GTPumps failed to initialize");
    }
}
