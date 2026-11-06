package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.sensor.SensorBlock;
import gregtech.content.blockentity.sensor.SensorBlockEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** GT6 sensor panels (fluid/item/energy/progress meters). */
public final class GTSensors {
    private static final List<RegisteredBlock<SensorBlock>> ALL = new ArrayList<>();

    private GTSensors() {}

    public static List<RegisteredBlock<SensorBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static void add(ContentRegistrar registrar, String id, SensorBlockEntity.Kind kind) {
        RegisteredBlock<SensorBlock> block = registrar.block(
                REGISTRY_NAMESPACE,
                id,
                () -> new SensorBlock(
                        kind,
                        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0f, 3.0f)),
                bare -> new BlockItem(bare, new Item.Properties()));
        ALL.add(block);
    }

    public static void registerAll(ContentRegistrar registrar) {
        for (var entry : gregtech.engine.sensor.SensorCatalog.ALL)
            add(registrar, entry.id(), SensorBlockEntity.Kind.valueOf(entry.kind()));
    }
}
