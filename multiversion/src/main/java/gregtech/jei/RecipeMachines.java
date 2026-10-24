package gregtech.jei;





//? if >=1.21.1 {
/*import gregtech.platform.neoforge.machine.BasicMachineRegistries;
*///?} else {
import gregtech.internal.machine.MachineRegistry;
//?}



import gregtech.internal.recipe.RecipeMap;

import gregtech.content.data.MachineRecipeMaps;

import gregtech.content.registry.GTToolBlocks;

import net.minecraft.world.item.ItemStack;

import java.util.*;



/** One machine-to-recipe index feeds both category icons and JEI catalysts. */

public final class RecipeMachines {



//? if >=1.21.1 {
/*    public static net.minecraft.world.level.block.Block machine(String id) {

        return BasicMachineRegistries.all().stream()

                .filter(h -> h.get().basicSpec().id().equals(id) && "gt6ce".equals(h.get().basicSpec().namespace()))

                .findFirst().orElseThrow(() -> new IllegalStateException("Missing native machine: "+id)).get();

    }

*///?} else {
//?}


    private RecipeMachines() {}

    public static Map<RecipeMap,List<ItemStack>> collect() {

        Map<RecipeMap,List<ItemStack>> result=new LinkedHashMap<>();



//? if >=1.21.1 {
/*        for (var entry:BasicMachineRegistries.all()) if(entry.isBound() && !entry.get().basicSpec().machineName().equals("fusionreactor"))
*///?} else {
        for (var entry:MachineRegistry.basicMachines()) if(entry.isPresent() && !entry.get().basicSpec().machineName().equals("fusionreactor"))
//?}


            add(result,entry.get().basicSpec().recipeMap(),new ItemStack(entry.get()));


//? if >=1.21.1 {
/*        for (var entry:gregtech.content.registry.GTManualStations.MANUAL) if(entry.isBound())
*///?} else {
        for (var entry:GTToolBlocks.manual()) if(entry.isPresent())
//?}



            add(result,entry.get().kind().recipeMap(),new ItemStack(entry.get()));



//? if >=1.21.1 {
/*        for (var entry:gregtech.content.registry.GTManualStations.VESSELS) if(entry.isBound() && entry.get() instanceof gregtech.content.block.tool.ProcessingToolBlock tool) {
*///?} else {
        for (var entry:GTToolBlocks.simpleBlocks()) if(entry.isPresent() && entry.get() instanceof gregtech.content.block.tool.ProcessingToolBlock tool) {
//?}



            var map = tool.toolId().equals("juicer") ? MachineRecipeMaps.Juicer : tool.toolId().equals("bathing_pot") ? MachineRecipeMaps.Bath : MachineRecipeMaps.Mixer;

            add(result,map,new ItemStack(tool));

        }



//? if >=1.21.1 {
/*        for (var entry:gregtech.content.registry.GTManualStations.ANVILS) if(entry.isBound()) {
*///?} else {
        for (var entry:GTToolBlocks.ANVILS) if(entry.isPresent()) {
//?}



            add(result,MachineRecipeMaps.Anvil,new ItemStack(entry.get()));

            add(result,MachineRecipeMaps.AnvilBendSmall,new ItemStack(entry.get()));

            add(result,MachineRecipeMaps.AnvilBendBig,new ItemStack(entry.get()));

        }



//? if >=1.21.1 {
/*        for (var entry:gregtech.platform.neoforge.smeltery.SmelteryRegistries.crucibles()) if(entry.isBound())
*///?} else {
        for (var entry:MachineRegistry.smeltingCrucibles()) if(entry.isPresent())
//?}



            add(result,MachineRecipeMaps.CrucibleAlloying,new ItemStack(entry.get()));



//? if >=1.21.1 {
/*        add(result,MachineRecipeMaps.ImplosionCompressor,new ItemStack(RecipeMachines.machine("implosion_compressor_main")));
*///?} else {
        add(result,MachineRecipeMaps.ImplosionCompressor,new ItemStack(gregtech.content.registry.GTMultiblocks.IMPLOSION_COMPRESSOR_MAIN.get()));
//?}


//? if >=1.21.1 {
/*        for (var entry : gregtech.platform.neoforge.machine.FuelGeneratorRegistration.all())
*///?} else {
        for (var entry : gregtech.content.registry.FuelGeneratorRegistration.all()) if (entry.isPresent())
//?}

            add(result, RecipeMap.RECIPE_MAPS.get(entry.get().declaration().fuelMap()), new ItemStack(entry.get()));


//? if >=1.21.1 {
/*        for (var entry : gregtech.platform.neoforge.machine.BehaviorMachineRegistration.all())
*///?} else {
        for (var entry : gregtech.content.registry.BehaviorMachineRegistration.all()) if (entry.isPresent())
//?}

            gregtech.internal.addon.BehaviorRecipeCatalysts.add(result,
                    entry.get().declaration().recipeMaps(), new ItemStack(entry.get()),
                    RecipeMap.RECIPE_MAPS::get, RecipeMachines::sameStack);


        for(var entry:gregtech.content.multiblock.GasTurbineDefinitions.blocks())

            add(result,gregtech.content.data.FuelRecipeMaps.Gas,new ItemStack(entry.get()));

        for (int originalId = 17231; originalId <= 17234; originalId++)

            add(result,gregtech.content.data.FuelRecipeMaps.Gas,

                    new ItemStack(gregtech.content.multiblock.LargeMachineParts.block(originalId)));



        add(result,MachineRecipeMaps.CokeOven,new ItemStack(gregtech.content.registry.GTMultiblocks.COKE_OVEN_MAIN.get()));
        add(result,gregtech.content.data.FuelRecipeMaps.Hot,new ItemStack(gregtech.content.registry.GTMultiblocks.HEAT_EXCHANGER_MAIN.get()));
        for (var exchanger : gregtech.content.registry.GTHeatExchangers.all())
            add(result,gregtech.content.data.FuelRecipeMaps.Hot,new ItemStack(exchanger.get()));

//? if >=1.21.1 {
/*        add(result,MachineRecipeMaps.Fusion,new ItemStack(RecipeMachines.machine("fusion_reactor_main")));
*///?} else {
        add(result,MachineRecipeMaps.Fusion,new ItemStack(gregtech.content.registry.GTMultiblocks.FUSION_REACTOR_MAIN.get()));
//?}



        return result;

    }

    private static void add(Map<RecipeMap,List<ItemStack>> index,RecipeMap map,ItemStack stack) {

        if(map==null || stack.isEmpty()) return;

        var list=index.computeIfAbsent(map,key->new ArrayList<>());



        if(list.stream().noneMatch(existing->sameStack(existing,stack))) list.add(stack);



    }

    private static boolean sameStack(ItemStack existing, ItemStack stack) {

//? if >=1.21.1 {
/*        return ItemStack.isSameItemSameComponents(existing, stack);
*///?} else {
        return ItemStack.isSameItemSameTags(existing, stack);
//?}

    }

}
