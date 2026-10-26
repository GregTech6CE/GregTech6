package gregtech.content.registry;

import gregtech.content.block.sensor.SensorBlock;
import gregtech.content.blockentity.sensor.SensorBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.DeferredHolder;
*///?} else {
import net.minecraftforge.registries.RegistryObject;
//?}

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 sensor panels (fluid/item/energy/progress meters). */
public final class GTSensors {
//? if >=1.21.1 {
    /*private static final List<DeferredHolder<net.minecraft.world.level.block.Block,SensorBlock>> ALL = new ArrayList<>();
*///?} else {
    private static final List<RegistryObject<SensorBlock>> ALL = new ArrayList<>();
//?}

    private GTSensors() {}

//? if >=1.21.1 {
    /*public static List<DeferredHolder<net.minecraft.world.level.block.Block,SensorBlock>> all() {
*///?} else {
    public static List<RegistryObject<SensorBlock>> all() {
//?}
        return Collections.unmodifiableList(ALL);
    }

    private static void add(String id, SensorBlockEntity.Kind kind) {
//? if >=1.21.1 {
        /*DeferredHolder<net.minecraft.world.level.block.Block,SensorBlock> block = GTBlocks.BLOCKS.register(id, () ->
*///?} else {
        RegistryObject<SensorBlock> block = GTBlocks.BLOCKS.register(id, () ->
//?}
                new SensorBlock(kind, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(1.0f, 3.0f)));
        ALL.add(block);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void registerAll(){for(var entry:gregtech.engine.sensor.SensorCatalog.ALL)add(entry.id(),SensorBlockEntity.Kind.valueOf(entry.kind()));}
}
