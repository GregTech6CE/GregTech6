package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.energy.ChemicalBatteryBlock;
import com.gregtech.gregtech.content.energy.ChemicalBatterySpec;
import com.gregtech.gregtech.item.ChemicalBatteryItem;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.*;

public final class GTChemicalBatteries {
    private static final List<DeferredHolder<Block,ChemicalBatteryBlock>> ALL=new ArrayList<>();
    private static final List<DeferredHolder<Block,ChemicalBatteryBlock>> LEGACY=new ArrayList<>();
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,"gregtech");
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,"gregtech");
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"gregtech");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.gregtech.gregtech.blockentity.energy.ChemicalBatteryBlockEntity>> CHEMICAL_BATTERY=ENTITIES.register("chemical_battery",()->BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.energy.ChemicalBatteryBlockEntity::new,allRegistered().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
    private GTChemicalBatteries(){}
    public static void register(IEventBus bus){if(!ALL.isEmpty())throw new IllegalStateException("Chemical batteries registered twice");registerAll();BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);}
    public static List<DeferredHolder<Block,ChemicalBatteryBlock>> legacy(){return Collections.unmodifiableList(LEGACY);}
    public static List<DeferredHolder<Block,ChemicalBatteryBlock>> allRegistered(){return java.util.stream.Stream.concat(ALL.stream(),LEGACY.stream()).toList();}
    public static List<DeferredHolder<Block,ChemicalBatteryBlock>> all(){return Collections.unmodifiableList(ALL);}
    public static void registerAll(){
        for(var spec:ChemicalBatterySpec.all()) {
            var block=BLOCKS.register(spec.id(),()->new ChemicalBatteryBlock(spec));
            ALL.add(block);
            ITEMS.register(spec.id(),()->new ChemicalBatteryItem(block.get()));
        }
        for(int tier=0;tier<5;tier++){
            var spec=new ChemicalBatterySpec(ChemicalBatterySpec.Chemistry.LITHIUM_COBALT,tier);
            String id="battery_eu_"+spec.voltage();
            var block=BLOCKS.register(id,()->new ChemicalBatteryBlock(spec));
            LEGACY.add(block);
            ITEMS.register(id,()->new ChemicalBatteryItem(block.get()));
        }
    }
}
