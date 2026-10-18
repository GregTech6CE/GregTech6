package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.energy.ChemicalBatteryBlock;
import com.gregtech.gregtech.content.energy.ChemicalBatterySpec;
import com.gregtech.gregtech.item.ChemicalBatteryItem;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
*///?} else {
import net.minecraftforge.registries.RegistryObject;
//?}
import java.util.*;

public final class GTChemicalBatteries {
//? if >=1.21.1 {
    /*private static final List<DeferredHolder<Block,ChemicalBatteryBlock>> ALL=new ArrayList<>();
    private static final List<DeferredHolder<Block,ChemicalBatteryBlock>> LEGACY=new ArrayList<>();
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,"gregtech");
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,"gregtech");
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"gregtech");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<com.gregtech.gregtech.blockentity.energy.ChemicalBatteryBlockEntity>> CHEMICAL_BATTERY=ENTITIES.register("chemical_battery",()->BlockEntityType.Builder.of(com.gregtech.gregtech.blockentity.energy.ChemicalBatteryBlockEntity::new,allRegistered().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
*///?} else {
    private static final List<RegistryObject<ChemicalBatteryBlock>> ALL=new ArrayList<>();
    private static final List<RegistryObject<ChemicalBatteryBlock>> LEGACY=new ArrayList<>();
//?}
    private GTChemicalBatteries(){}
//? if >=1.21.1 {
    /*public static void register(IEventBus bus){if(!ALL.isEmpty())throw new IllegalStateException("Chemical batteries registered twice");registerAll();BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);}
    public static List<DeferredHolder<Block,ChemicalBatteryBlock>> legacy(){return Collections.unmodifiableList(LEGACY);}
    public static List<DeferredHolder<Block,ChemicalBatteryBlock>> allRegistered(){return java.util.stream.Stream.concat(ALL.stream(),LEGACY.stream()).toList();}
    public static List<DeferredHolder<Block,ChemicalBatteryBlock>> all(){return Collections.unmodifiableList(ALL);}
*///?} else {
    public static List<RegistryObject<ChemicalBatteryBlock>> legacy(){return Collections.unmodifiableList(LEGACY);}
    public static List<RegistryObject<ChemicalBatteryBlock>> allRegistered(){return java.util.stream.Stream.concat(ALL.stream(),LEGACY.stream()).toList();}
    public static List<RegistryObject<ChemicalBatteryBlock>> all(){return Collections.unmodifiableList(ALL);}
//?}
    public static ChemicalBatteryItem item(ChemicalBatterySpec.Chemistry chemistry,int tier){
//? if >=1.21.1 {
        /*var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",new ChemicalBatterySpec(chemistry,tier).id());
*///?} else {
        var id=com.gregtech.gregtech.GregTech.id(new ChemicalBatterySpec(chemistry,tier).id());
//?}
        var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
        if(!(item instanceof ChemicalBatteryItem battery))throw new IllegalStateException("Missing chemical battery "+id);
        return battery;
    }
    public static void registerAll(){
//? if >=1.21.1 {
        /*for(var alias:com.gregtech.gregtech.content.energy.BatteryItemMigration.ALIASES)
            ITEMS.addAlias(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",alias.oldId()),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",alias.target().id()));
*///?} else {
//?}
        for(var spec:ChemicalBatterySpec.all()) {
//? if >=1.21.1 {
            /*var block=BLOCKS.register(spec.id(),()->new ChemicalBatteryBlock(spec));
*///?} else {
            var block=GTBlocks.BLOCKS.register(spec.id(),()->new ChemicalBatteryBlock(spec));
//?}
            ALL.add(block);
//? if >=1.21.1 {
            /*ITEMS.register(spec.id(),()->new ChemicalBatteryItem(block.get()));
*///?} else {
            GTBlocks.BLOCK_ITEMS.register(spec.id(),()->new ChemicalBatteryItem(block.get()));
//?}
        }

    }
}
