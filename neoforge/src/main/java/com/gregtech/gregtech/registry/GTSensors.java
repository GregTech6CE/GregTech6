package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.sensor.SensorBlock;
import com.gregtech.gregtech.blockentity.sensor.SensorBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 sensor panels (fluid/item/energy/progress meters). */
public final class GTSensors {
    private static final List<DeferredHolder<net.minecraft.world.level.block.Block,SensorBlock>> ALL = new ArrayList<>();

    private GTSensors() {}

    public static List<DeferredHolder<net.minecraft.world.level.block.Block,SensorBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static void add(String id, SensorBlockEntity.Kind kind) {
        DeferredHolder<net.minecraft.world.level.block.Block,SensorBlock> block = GTBlocks.BLOCKS.register(id, () ->
                new SensorBlock(kind, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(1.0f, 3.0f)));
        ALL.add(block);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void registerAll(){for(var entry:com.gregtech.gregtech.api.sensor.SensorCatalog.ALL)add(entry.id(),SensorBlockEntity.Kind.valueOf(entry.kind()));}
}
