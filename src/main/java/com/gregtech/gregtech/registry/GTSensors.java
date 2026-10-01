package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.sensor.SensorBlock;
import com.gregtech.gregtech.blockentity.sensor.SensorBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 sensor panels (fluid/item/energy/progress meters). */
public final class GTSensors {
    private static final List<RegistryObject<SensorBlock>> ALL = new ArrayList<>();

    private GTSensors() {}

    public static List<RegistryObject<SensorBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static void add(String id, SensorBlockEntity.Kind kind) {
        RegistryObject<SensorBlock> block = GTBlocks.BLOCKS.register(id, () ->
                new SensorBlock(kind, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(2.0f, 2.0f)
                        .requiresCorrectToolForDrops()));
        ALL.add(block);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void registerAll() {
        add("sensor_fluidometer", SensorBlockEntity.Kind.FLUID);
        add("sensor_itemometer", SensorBlockEntity.Kind.ITEM);
        add("sensor_electrometer", SensorBlockEntity.Kind.ENERGY);
        add("sensor_progressmeter", SensorBlockEntity.Kind.PROGRESS);
        add("sensor_thermometer", SensorBlockEntity.Kind.THERMOMETER);
        add("sensor_tachometer", SensorBlockEntity.Kind.TACHOMETER);
        // GT6 Loader_MultiTileEntities:1988-1991 - four weight-o-meters that differ only in scale.
        // The port's original id is GT6's Heavy one (tonnes, maximum 65535), so the other three are
        // added beside it rather than renaming it; §108 closes the set 16 -> 20.
        add("sensor_weightometric", SensorBlockEntity.Kind.WEIGHTOMETRIC);
        add("sensor_weightometer_light", SensorBlockEntity.Kind.WEIGHTOMETRIC_LIGHT);
        add("sensor_weightometer_medium", SensorBlockEntity.Kind.WEIGHTOMETRIC_MEDIUM);
        add("sensor_weightometer_super_heavy", SensorBlockEntity.Kind.WEIGHTOMETRIC_SUPER_HEAVY);
        add("sensor_bucketometer", SensorBlockEntity.Kind.BUCKETOMETER);
        add("sensor_kilobucketometer", SensorBlockEntity.Kind.KILOBUCKETOMETER);
        add("sensor_gibblometer", SensorBlockEntity.Kind.GIBBLOMETER);
        add("sensor_stackometer", SensorBlockEntity.Kind.STACKOMETER);
        add("sensor_luminometer", SensorBlockEntity.Kind.LUMINOMETER);
        add("sensor_playercounter", SensorBlockEntity.Kind.PLAYERCOUNTER);
        add("sensor_chronometer", SensorBlockEntity.Kind.CHRONOMETER);
        add("sensor_geiger", SensorBlockEntity.Kind.GEIGER);
        add("sensor_laserometer", SensorBlockEntity.Kind.LASEROMETER);
        add("sensor_tpsmeter", SensorBlockEntity.Kind.TPS);
    }
}
