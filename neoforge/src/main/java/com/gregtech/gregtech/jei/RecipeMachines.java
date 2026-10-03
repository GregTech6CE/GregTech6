package com.gregtech.gregtech.jei;

import com.gregtech.gregtech.platform.neoforge.machine.BasicMachineRegistries;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.GTToolBlocks;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** One machine-to-recipe index feeds both category icons and JEI catalysts. */
public final class RecipeMachines {
    public static net.minecraft.world.level.block.Block machine(String id) {
        return BasicMachineRegistries.all().stream().filter(h -> h.get().basicSpec().id().equals(id))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing native machine: "+id)).get();
    }
    private RecipeMachines() {}
    public static Map<RecipeMap,List<ItemStack>> collect() {
        Map<RecipeMap,List<ItemStack>> result=new LinkedHashMap<>();
        for (var entry:BasicMachineRegistries.all()) if(entry.isBound() && !entry.get().basicSpec().machineName().equals("fusionreactor"))
            add(result,MachineRecipeMaps.byMachineName(entry.get().basicSpec().machineName()),new ItemStack(entry.get()));
        for (var entry:com.gregtech.gregtech.registry.GTManualStations.MANUAL) if(entry.isBound())
            add(result,entry.get().kind().recipeMap(),new ItemStack(entry.get()));
        for (var entry:com.gregtech.gregtech.registry.GTManualStations.VESSELS) if(entry.isBound() && entry.get() instanceof com.gregtech.gregtech.block.tool.ProcessingToolBlock tool) {
            var map = tool.toolId().equals("juicer") ? MachineRecipeMaps.Juicer : tool.toolId().equals("bathing_pot") ? MachineRecipeMaps.Bath : MachineRecipeMaps.Mixer;
            add(result,map,new ItemStack(tool));
        }
        for (var entry:com.gregtech.gregtech.registry.GTManualStations.ANVILS) if(entry.isBound()) {
            add(result,MachineRecipeMaps.Anvil,new ItemStack(entry.get()));
            add(result,MachineRecipeMaps.AnvilBendSmall,new ItemStack(entry.get()));
            add(result,MachineRecipeMaps.AnvilBendBig,new ItemStack(entry.get()));
        }
        for (var entry:com.gregtech.gregtech.platform.neoforge.smeltery.SmelteryRegistries.crucibles()) if(entry.isBound())
            add(result,MachineRecipeMaps.CrucibleAlloying,new ItemStack(entry.get()));
        add(result,MachineRecipeMaps.ImplosionCompressor,new ItemStack(RecipeMachines.machine("implosion_compressor_main")));
        for(var entry:com.gregtech.gregtech.content.multiblock.GasTurbineDefinitions.blocks())
            add(result,com.gregtech.gregtech.data.FuelRecipeMaps.Gas,new ItemStack(entry.get()));
        for (int originalId = 17231; originalId <= 17234; originalId++)
            add(result,com.gregtech.gregtech.data.FuelRecipeMaps.Gas,
                    new ItemStack(com.gregtech.gregtech.content.multiblock.LargeMachineParts.block(originalId)));
        add(result,com.gregtech.gregtech.data.FuelRecipeMaps.Hot,new ItemStack(com.gregtech.gregtech.registry.GTMultiblocks.LARGE_HEAT_EXCHANGER_MAIN.get()));
        add(result,MachineRecipeMaps.Fusion,new ItemStack(RecipeMachines.machine("fusion_reactor_main")));
        return result;
    }
    private static void add(Map<RecipeMap,List<ItemStack>> index,RecipeMap map,ItemStack stack) {
        if(map==null || stack.isEmpty()) return;
        var list=index.computeIfAbsent(map,key->new ArrayList<>());
        if(list.stream().noneMatch(existing->ItemStack.isSameItemSameComponents(existing,stack))) list.add(stack);
    }
}
