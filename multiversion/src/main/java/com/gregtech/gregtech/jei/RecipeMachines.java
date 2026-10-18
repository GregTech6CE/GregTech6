package com.gregtech.gregtech.jei;

//? if >=1.21.1 {
/*import com.gregtech.gregtech.platform.neoforge.machine.BasicMachineRegistries;
*///?} else {
import com.gregtech.gregtech.api.machine.MachineRegistry;
//?}
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.GTToolBlocks;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** One machine-to-recipe index feeds both category icons and JEI catalysts. */
public final class RecipeMachines {
//? if >=1.21.1 {
    /*public static net.minecraft.world.level.block.Block machine(String id) {
        return BasicMachineRegistries.all().stream().filter(h -> h.get().basicSpec().id().equals(id))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing native machine: "+id)).get();
    }
*///?} else {
//?}
    private RecipeMachines() {}
    public static Map<RecipeMap,List<ItemStack>> collect() {
        Map<RecipeMap,List<ItemStack>> result=new LinkedHashMap<>();
//? if >=1.21.1 {
        /*for (var entry:BasicMachineRegistries.all()) if(entry.isBound() && !entry.get().basicSpec().machineName().equals("fusionreactor"))
*///?} else {
        for (var entry:MachineRegistry.basicMachines()) if(entry.isPresent() && !entry.get().basicSpec().machineName().equals("fusionreactor"))
//?}
            add(result,MachineRecipeMaps.byMachineName(entry.get().basicSpec().machineName()),new ItemStack(entry.get()));
//? if >=1.21.1 {
        /*for (var entry:com.gregtech.gregtech.registry.GTManualStations.MANUAL) if(entry.isBound())
*///?} else {
        for (var entry:GTToolBlocks.manual()) if(entry.isPresent())
//?}
            add(result,entry.get().kind().recipeMap(),new ItemStack(entry.get()));
//? if >=1.21.1 {
        /*for (var entry:com.gregtech.gregtech.registry.GTManualStations.VESSELS) if(entry.isBound() && entry.get() instanceof com.gregtech.gregtech.block.tool.ProcessingToolBlock tool) {
*///?} else {
        for (var entry:GTToolBlocks.simpleBlocks()) if(entry.isPresent() && entry.get() instanceof com.gregtech.gregtech.block.tool.ProcessingToolBlock tool) {
//?}
            var map = tool.toolId().equals("juicer") ? MachineRecipeMaps.Juicer : tool.toolId().equals("bathing_pot") ? MachineRecipeMaps.Bath : MachineRecipeMaps.Mixer;
            add(result,map,new ItemStack(tool));
        }
//? if >=1.21.1 {
        /*for (var entry:com.gregtech.gregtech.registry.GTManualStations.ANVILS) if(entry.isBound()) {
*///?} else {
        for (var entry:GTToolBlocks.ANVILS) if(entry.isPresent()) {
//?}
            add(result,MachineRecipeMaps.Anvil,new ItemStack(entry.get()));
            add(result,MachineRecipeMaps.AnvilBendSmall,new ItemStack(entry.get()));
            add(result,MachineRecipeMaps.AnvilBendBig,new ItemStack(entry.get()));
        }
//? if >=1.21.1 {
        /*for (var entry:com.gregtech.gregtech.platform.neoforge.smeltery.SmelteryRegistries.crucibles()) if(entry.isBound())
*///?} else {
        for (var entry:MachineRegistry.smeltingCrucibles()) if(entry.isPresent())
//?}
            add(result,MachineRecipeMaps.CrucibleAlloying,new ItemStack(entry.get()));
//? if >=1.21.1 {
        /*add(result,MachineRecipeMaps.ImplosionCompressor,new ItemStack(RecipeMachines.machine("implosion_compressor_main")));
*///?} else {
        add(result,MachineRecipeMaps.ImplosionCompressor,new ItemStack(com.gregtech.gregtech.registry.GTMultiblocks.IMPLOSION_COMPRESSOR_MAIN.get()));
//?}
        for(var entry:com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.blocks())
            add(result,com.gregtech.gregtech.data.FuelRecipeMaps.Gas,new ItemStack(entry.get()));
        for (int originalId = 17231; originalId <= 17234; originalId++)
            add(result,com.gregtech.gregtech.data.FuelRecipeMaps.Gas,
                    new ItemStack(com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(originalId)));
//? if >=1.21.1 {
        /*add(result,com.gregtech.gregtech.data.FuelRecipeMaps.Hot,new ItemStack(com.gregtech.gregtech.registry.GTMultiblocks.LARGE_HEAT_EXCHANGER_MAIN.get()));
        add(result,MachineRecipeMaps.Fusion,new ItemStack(RecipeMachines.machine("fusion_reactor_main")));
*///?} else {
        add(result,com.gregtech.gregtech.data.FuelRecipeMaps.Hot,new ItemStack(com.gregtech.gregtech.registry.GTMultiblocks.HEAT_EXCHANGER_MAIN.get()));
        add(result,MachineRecipeMaps.Fusion,new ItemStack(com.gregtech.gregtech.registry.GTMultiblocks.FUSION_REACTOR_MAIN.get()));
//?}
        return result;
    }
    private static void add(Map<RecipeMap,List<ItemStack>> index,RecipeMap map,ItemStack stack) {
        if(map==null || stack.isEmpty()) return;
        var list=index.computeIfAbsent(map,key->new ArrayList<>());
//? if >=1.21.1 {
        /*if(list.stream().noneMatch(existing->ItemStack.isSameItemSameComponents(existing,stack))) list.add(stack);
*///?} else {
        if(list.stream().noneMatch(existing->ItemStack.isSameItemSameTags(existing,stack))) list.add(stack);
//?}
    }
}
