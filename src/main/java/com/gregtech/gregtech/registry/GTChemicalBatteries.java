package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.energy.ChemicalBatteryBlock;
import com.gregtech.gregtech.content.energy.ChemicalBatterySpec;
import com.gregtech.gregtech.item.ChemicalBatteryItem;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

public final class GTChemicalBatteries {
    private static final List<RegistryObject<ChemicalBatteryBlock>> ALL=new ArrayList<>();
    private static final List<RegistryObject<ChemicalBatteryBlock>> LEGACY=new ArrayList<>();
    private GTChemicalBatteries(){}
    public static List<RegistryObject<ChemicalBatteryBlock>> legacy(){return Collections.unmodifiableList(LEGACY);}
    public static List<RegistryObject<ChemicalBatteryBlock>> allRegistered(){return java.util.stream.Stream.concat(ALL.stream(),LEGACY.stream()).toList();}
    public static List<RegistryObject<ChemicalBatteryBlock>> all(){return Collections.unmodifiableList(ALL);}
    public static ChemicalBatteryItem item(ChemicalBatterySpec.Chemistry chemistry,int tier){
        var id=com.gregtech.gregtech.GregTech.id(new ChemicalBatterySpec(chemistry,tier).id());
        var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
        if(!(item instanceof ChemicalBatteryItem battery))throw new IllegalStateException("Missing chemical battery "+id);
        return battery;
    }
    public static void registerAll(){
        for(var spec:ChemicalBatterySpec.all()) {
            var block=GTBlocks.BLOCKS.register(spec.id(),()->new ChemicalBatteryBlock(spec));
            ALL.add(block);
            GTBlocks.BLOCK_ITEMS.register(spec.id(),()->new ChemicalBatteryItem(block.get()));
        }
        for(int tier=0;tier<5;tier++){
            var spec=new ChemicalBatterySpec(ChemicalBatterySpec.Chemistry.LITHIUM_COBALT,tier);
            String id="battery_eu_"+spec.voltage();
            var block=GTBlocks.BLOCKS.register(id,()->new ChemicalBatteryBlock(spec));
            LEGACY.add(block);
            GTBlocks.BLOCK_ITEMS.register(id,()->new ChemicalBatteryItem(block.get()));
        }
    }
}
