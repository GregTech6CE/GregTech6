package gregtech.platform.neoforge.machine;

import gregtech.api.addon.platform.AddonSensor;
import gregtech.engine.sensor.AddonSensors;
import gregtech.content.block.sensor.SensorBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Registers addon sensor blocks and items in the addon's namespace. */
public final class AddonSensorRegistration {
    private AddonSensorRegistration() {}

    public static void register(IEventBus bus) {
        AddonSensors.collect();
        var byNamespace = new LinkedHashMap<String, List<AddonSensor>>();
        for (AddonSensors.Declared declared : AddonSensors.all())
            byNamespace.computeIfAbsent(declared.addonId(), id -> new ArrayList<>()).add(declared.sensor());
        for (var entry : byNamespace.entrySet()) registerNamespace(entry.getKey(), entry.getValue(), bus);
    }

    private static void registerNamespace(String namespace, List<AddonSensor> sensors, IEventBus bus) {
        DeferredRegister<Block> blocks = DeferredRegister.create(Registries.BLOCK, namespace);
        DeferredRegister<Item> items = DeferredRegister.create(Registries.ITEM, namespace);
        for (AddonSensor sensor : sensors) {
            String path = "sensor_" + sensor.id();
            DeferredHolder<Block, SensorBlock> block = blocks.register(path, () ->
                    new SensorBlock(sensor, BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.0f, 3.0f)));
            AddonSensors.addBlock(() -> block.get());
            items.register(path, () -> new BlockItem(block.get(), new Item.Properties()));
        }
        blocks.register(bus);
        items.register(bus);
    }
}
