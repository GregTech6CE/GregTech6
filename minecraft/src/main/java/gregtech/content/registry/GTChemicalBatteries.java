package gregtech.content.registry;

import gregtech.content.block.energy.ChemicalBatteryBlock;
import gregtech.content.energy.ChemicalBatterySpec;
import gregtech.content.item.ChemicalBatteryItem;
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
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,"gt6ce");
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,"gt6ce");
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"gt6ce");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.energy.ChemicalBatteryBlockEntity>> CHEMICAL_BATTERY=ENTITIES.register("chemical_battery",()->BlockEntityType.Builder.of(gregtech.content.blockentity.energy.ChemicalBatteryBlockEntity::new,allRegistered().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
*///?} else {
    private static final List<RegistryObject<ChemicalBatteryBlock>> ALL=new ArrayList<>();
//?}
    private GTChemicalBatteries(){}
//? if >=1.21.1 {
    /*public static void register(IEventBus bus){if(!ALL.isEmpty())throw new IllegalStateException("Chemical batteries registered twice");registerAll();BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);}
    public static List<DeferredHolder<Block,ChemicalBatteryBlock>> allRegistered(){return Collections.unmodifiableList(ALL);}
    public static List<DeferredHolder<Block,ChemicalBatteryBlock>> all(){return ALL.stream().filter(holder -> holder.get().spec() instanceof ChemicalBatterySpec).toList();}
*///?} else {
    public static List<RegistryObject<ChemicalBatteryBlock>> allRegistered(){return Collections.unmodifiableList(ALL);}
    public static List<RegistryObject<ChemicalBatteryBlock>> all(){return ALL.stream().filter(holder -> holder.get().spec() instanceof ChemicalBatterySpec).toList();}
//?}
    public static ChemicalBatteryItem item(ChemicalBatterySpec.Chemistry chemistry,int tier){
//? if >=1.21.1 {
        /*var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce",new ChemicalBatterySpec(chemistry,tier).id());
*///?} else {
        var id=gregtech.GregTech.id(new ChemicalBatterySpec(chemistry,tier).id());
//?}
        var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
        if(!(item instanceof ChemicalBatteryItem battery))throw new IllegalStateException("Missing chemical battery "+id);
        return battery;
    }
    public static void registerAll(){
        var specifications = new ArrayList<gregtech.content.energy.PortableBatterySpec>(ChemicalBatterySpec.all());
        specifications.addAll(gregtech.content.energy.EnergyCrystalSpec.all());
        for(var spec:specifications) {
//? if >=1.21.1 {
            /*var block=BLOCKS.register(spec.id(),()->new ChemicalBatteryBlock(spec));
*///?} else {
            var block=GTBlocks.BLOCKS.register(spec.id(),()->new ChemicalBatteryBlock(spec));
//?}
            ALL.add(block);
            // Energium keeps its existing material item ID, created through GTItems.
            if (spec instanceof gregtech.content.energy.EnergyCrystalSpec) continue;
//? if >=1.21.1 {
            /*ITEMS.register(spec.id(),()->new ChemicalBatteryItem(block.get()));
*///?} else {
            GTBlocks.BLOCK_ITEMS.register(spec.id(),()->new ChemicalBatteryItem(block.get()));
//?}
        }

    }
}
