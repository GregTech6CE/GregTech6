package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.energy.SignalWireBlock;
import com.gregtech.gregtech.content.material.Materials;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.*;

/** GT6 source IDs 27000/27006, 27050/27056, 27500/27506. These carry redstone, never EU. */
public final class GTSignalWires {
    private static final List<DeferredHolder<Block,SignalWireBlock>> ALL = new ArrayList<>();
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,"gregtech");
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,"gregtech");
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"gregtech");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.gregtech.gregtech.blockentity.energy.SignalWireBlockEntity>> SIGNAL_WIRE=ENTITIES.register("signal_wire",()->BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.energy.SignalWireBlockEntity::new,all().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
    private GTSignalWires() {}
    public static void register(IEventBus bus){if(!ALL.isEmpty())throw new IllegalStateException("Signal wires registered twice");register();BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);}

    public static List<DeferredHolder<Block,SignalWireBlock>> all() { return Collections.unmodifiableList(ALL); }
    public static void register() {
        for(var def:com.gregtech.gregtech.content.energy.SignalWireCatalog.families())family(def.name(),def.material(),def.range(),def.luminous());
    }
    private static void family(String name, GTMaterial material, int range, boolean luminous) {
        for (boolean insulated : new boolean[]{false, true}) {
            String id = (insulated ? "cable_01_" : "wire_01_") + name;
            var block = BLOCKS.register(id, () -> new SignalWireBlock(material, range, insulated,
                    luminous && !insulated, BlockBehaviour.Properties.of().strength(1, 2).noOcclusion()));
            ALL.add(block);
            ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        }
    }
}
