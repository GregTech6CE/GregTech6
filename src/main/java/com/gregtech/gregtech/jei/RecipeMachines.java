package com.gregtech.gregtech.jei;

import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.GTToolBlocks;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** One machine-to-recipe index feeds both category icons and JEI catalysts. */
final class RecipeMachines {
    private RecipeMachines() {}
    static Map<RecipeMap,List<ItemStack>> collect() {
        Map<RecipeMap,List<ItemStack>> result=new LinkedHashMap<>();
        for (var entry:MachineRegistry.basicMachines()) if(entry.isPresent() && !entry.get().basicSpec().machineName().equals("fusionreactor"))
            add(result,MachineRecipeMaps.byMachineName(entry.get().basicSpec().machineName()),new ItemStack(entry.get()));
        for (var entry:GTToolBlocks.manual()) if(entry.isPresent())
            add(result,entry.get().kind().recipeMap(),new ItemStack(entry.get()));
        for (var entry:GTToolBlocks.simpleBlocks()) if(entry.isPresent() && entry.get() instanceof com.gregtech.gregtech.block.tool.ProcessingToolBlock tool) {
            var map = tool.toolId().equals("juicer") ? MachineRecipeMaps.Juicer : tool.toolId().equals("bathing_pot") ? MachineRecipeMaps.Bath : MachineRecipeMaps.Mixer;
            add(result,map,new ItemStack(tool));
        }
        for (var entry:GTToolBlocks.ANVILS) if(entry.isPresent()) {
            add(result,MachineRecipeMaps.Anvil,new ItemStack(entry.get()));
            add(result,MachineRecipeMaps.AnvilBendSmall,new ItemStack(entry.get()));
            add(result,MachineRecipeMaps.AnvilBendBig,new ItemStack(entry.get()));
        }
        for (var entry:MachineRegistry.smeltingCrucibles()) if(entry.isPresent())
            add(result,MachineRecipeMaps.CrucibleAlloying,new ItemStack(entry.get()));
        add(result,MachineRecipeMaps.ImplosionCompressor,new ItemStack(com.gregtech.gregtech.registry.GTMultiblocks.IMPLOSION_COMPRESSOR_MAIN.get()));
        for(var entry:com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.blocks())
            add(result,com.gregtech.gregtech.data.FuelRecipeMaps.Gas,new ItemStack(entry.get()));
        for (int originalId = 17231; originalId <= 17234; originalId++)
            add(result,com.gregtech.gregtech.data.FuelRecipeMaps.Gas,
                    new ItemStack(com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(originalId)));
        add(result,com.gregtech.gregtech.data.FuelRecipeMaps.Hot,new ItemStack(com.gregtech.gregtech.registry.GTMultiblocks.HEAT_EXCHANGER_MAIN.get()));
        add(result,MachineRecipeMaps.Fusion,new ItemStack(com.gregtech.gregtech.registry.GTMultiblocks.FUSION_REACTOR_MAIN.get()));
        return result;
    }
    private static void add(Map<RecipeMap,List<ItemStack>> index,RecipeMap map,ItemStack stack) {
        if(map==null || stack.isEmpty()) return;
        var list=index.computeIfAbsent(map,key->new ArrayList<>());
        if(list.stream().noneMatch(existing->ItemStack.isSameItemSameTags(existing,stack))) list.add(stack);
    }
}
