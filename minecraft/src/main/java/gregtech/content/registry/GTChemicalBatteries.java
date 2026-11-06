package gregtech.content.registry;

import gregtech.content.block.energy.ChemicalBatteryBlock;
import gregtech.content.energy.ChemicalBatterySpec;
import gregtech.content.item.ChemicalBatteryItem;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
*///?} else {
//?}
import java.util.*;

public final class GTChemicalBatteries {
    private static final List<RegisteredBlock<ChemicalBatteryBlock>> ALL=new ArrayList<>();
//? if >=1.21.1 {
    /*public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<gregtech.content.blockentity.energy.ChemicalBatteryBlockEntity>> CHEMICAL_BATTERY=ENTITIES.register("chemical_battery",()->BlockEntityType.Builder.of(gregtech.content.blockentity.energy.ChemicalBatteryBlockEntity::new,allRegistered().stream().map(RegisteredBlock::get).toArray(Block[]::new)).build(null));
*///?}
    private GTChemicalBatteries(){}
//? if >=1.21.1 {
    /*public static void submit(IEventBus bus){ENTITIES.register(bus);}
*///?}
    public static List<RegisteredBlock<ChemicalBatteryBlock>> allRegistered(){return Collections.unmodifiableList(ALL);}
    public static List<RegisteredBlock<ChemicalBatteryBlock>> all(){return ALL.stream().filter(holder -> holder.get().spec() instanceof ChemicalBatterySpec).toList();}
    public static ChemicalBatteryItem item(ChemicalBatterySpec.Chemistry chemistry,int tier){
//? if >=1.21.1 {
        /*var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,new ChemicalBatterySpec(chemistry,tier).id());
*///?} else {
        var id=gregtech.GregTech.id(new ChemicalBatterySpec(chemistry,tier).id());
//?}
        var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
        if(!(item instanceof ChemicalBatteryItem battery))throw new IllegalStateException("Missing chemical battery "+id);
        return battery;
    }
    public static void registerAll(ContentRegistrar registrar){
        var specifications = new ArrayList<gregtech.content.energy.PortableBatterySpec>(ChemicalBatterySpec.all());
        specifications.addAll(gregtech.content.energy.EnergyCrystalSpec.all());
        for(var spec:specifications) {
            // Energium keeps its existing material item ID, created through GTItems.
            if (spec instanceof gregtech.content.energy.EnergyCrystalSpec) {
                var block=registrar.blockWithoutItem(
                        gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                        spec.id(),
                        () -> new ChemicalBatteryBlock(spec));
                ALL.add(block);
                continue;
            }
            var block=registrar.block(
                    gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE,
                    spec.id(),
                    () -> new ChemicalBatteryBlock(spec),
                    ChemicalBatteryItem::new);
            ALL.add(block);
        }

    }
}
