package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.energy.SignalWireBlock;
import com.gregtech.gregtech.content.material.Materials;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
*///?} else {
import net.minecraftforge.registries.RegistryObject;
//?}
import java.util.*;

/** GT6 source IDs 27000/27006, 27050/27056, 27500/27506. These carry redstone, never EU. */
public final class GTSignalWires {
//? if >=1.21.1 {
    /*private static final List<DeferredHolder<Block, SignalWireBlock>> ALL = new ArrayList<>();
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "gt6ce");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6ce");
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "gt6ce");
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<com.gregtech.gregtech.blockentity.energy.SignalWireBlockEntity>> SIGNAL_WIRE = ENTITIES.register("signal_wire",
            () -> BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.energy.SignalWireBlockEntity::new,
            all().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
*///?} else {
    private static final List<RegistryObject<SignalWireBlock>> ALL = new ArrayList<>();
//?}
    private GTSignalWires() {
    }
//? if >=1.21.1 {
    /*public static void register(IEventBus bus) {
        if (!ALL.isEmpty()) throw new IllegalStateException("Signal wires registered twice");
        register();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
    }

    public static List<DeferredHolder<Block, SignalWireBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }
*///?} else {
    public static List<RegistryObject<SignalWireBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }
//?}
    public static void register() {
        for (var familySpec : com.gregtech.gregtech.content.energy.SignalWireCatalog.families())
            registerFamily(familySpec.name(), familySpec.material(), familySpec.range(), familySpec.luminous());
    }

    private static void registerFamily(String name, GTMaterial material, int range, boolean luminous) {
        for (boolean insulated : new boolean[]{false, true}) {
            String id = (insulated ? "cable_01_" : "wire_01_") + name;
//? if >=1.21.1 {
            /*var block = BLOCKS.register(id, () -> new SignalWireBlock(material, range, insulated,
*///?} else {
            var block = GTBlocks.BLOCKS.register(id, () -> new SignalWireBlock(material, range, insulated,
//?}
                    luminous && !insulated, BlockBehaviour.Properties.of().strength(1, 2).noOcclusion()));
            ALL.add(block);
//? if >=1.21.1 {
            /*ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
*///?} else {
            GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
//?}
        }
    }
}
