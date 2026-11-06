package gregtech.content.registry;

import gregtech.api.addon.platform.AddonSensor;
import gregtech.content.block.sensor.SensorBlock;
import gregtech.engine.sensor.AddonSensors;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Registers addon sensor blocks and items in the addon's namespace. */
public final class AddonSensorRegistration {
    private AddonSensorRegistration() {}

    public static void register(ContentRegistrar registrar) {
        AddonSensors.collect();
        var byNamespace = new LinkedHashMap<String, List<AddonSensor>>();
        for (AddonSensors.Declared declared : AddonSensors.all())
            byNamespace
                    .computeIfAbsent(declared.addonId(), id -> new ArrayList<>())
                    .add(declared.sensor());
        for (var entry : byNamespace.entrySet()) registerNamespace(registrar, entry.getKey(), entry.getValue());
    }

    private static void registerNamespace(
            ContentRegistrar registrar, String namespace, List<AddonSensor> sensors) {
        for (AddonSensor sensor : sensors) {
            String path = "sensor_" + sensor.id();
            var block = registrar.block(
                    namespace,
                    path,
                    () -> new SensorBlock(
                            sensor,
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(1.0f, 3.0f)),
                    bare -> new BlockItem(bare, new Item.Properties()));
            AddonSensors.addBlock(block::get);
        }
    }
}
