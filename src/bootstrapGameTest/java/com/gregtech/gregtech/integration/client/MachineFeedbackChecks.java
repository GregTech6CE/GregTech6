package com.gregtech.gregtech.integration.client;

import com.google.gson.*;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import java.util.*;

/** Finite native recipe, live registry and baked model checks for both platform deliveries. */
final class MachineFeedbackChecks {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("gregtech", path); }
    private static void require(boolean value, String message) { if(!value) throw new IllegalStateException(message); }
    private static ItemStack usable(ItemStack sample) {
        var stack=sample.copyWithCount(1);
        if(stack.getItem() instanceof com.gregtech.gregtech.item.GTToolItem tool && tool.toolType().requiresHeadAssembly())
            com.gregtech.gregtech.api.tool.GTToolHelper.write(stack,
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Steel"),
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Spruce"));
        return stack;
    }
    private static void craft(net.minecraft.server.MinecraftServer server, String path, String output, JsonArray rows) {
        var level=server.overworld();
        var loaded=server.getRecipeManager().byKey(id(path)).orElseThrow(()->new IllegalStateException("Missing native recipe "+path));
        var recipe=(CraftingRecipe)loaded;
        var ingredients=recipe.getIngredients();
        require(ingredients.size()==9, "Expected original 3x3 pattern for "+path);
        require(ingredients.stream().filter(i->i!=Ingredient.EMPTY).allMatch(i->i.getItems().length>0), "Unresolved original ingredient "+path);
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null,0){
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
        },3,3);
        for(int i=0;i<9;i++){var options=ingredients.get(i).getItems();if(options.length>0)grid.setItem(i,usable(options[0]));}
        require(recipe.matches(grid,level), "Original grid does not match live recipe "+path);
        var result=recipe.assemble(grid,level.registryAccess());
        require(result.is(BuiltInRegistries.ITEM.get(id(output))) && result.getCount()==1, "Wrong native crafting output "+path);
        if(output.startsWith("lightning_")) {
            var wrong=grid;wrong.setItem(0,new ItemStack(BuiltInRegistries.ITEM.get(id("wire_"+String.format(java.util.Locale.ROOT,"%02d",1<<((BasicMachineBlock)BuiltInRegistries.BLOCK.get(id(output))).basicSpec().tier()-1)+"_tin"))));
            require(!recipe.matches(wrong,level), "Lightning processor accepts a tin electrode instead of original iron/steel "+path);
        }
        var row=new JsonObject();row.addProperty("recipe",path);row.addProperty("output",output);
        var values=new JsonArray();ingredients.forEach(i->{var a=new JsonArray();for(var s:i.getItems())a.add(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());values.add(a);});
        row.add("resolvedIngredients",values);rows.add(row);
    }
    static void server(net.minecraft.server.MinecraftServer server,JsonObject receipt) {
        var rows=new JsonArray();
        for(var name:com.gregtech.gregtech.content.recipe.EquipmentCraftingCatalog.FILES)
            if(name.startsWith("long_distance/")) {
                String output=name.substring("long_distance/".length(),name.length()-5);
                craft(server,name.substring(0,name.length()-5),output,rows);
            }
        for(String tier:List.of("ulv","lv","mv","hv","ev","iv","luv"))
            for(String prefix:List.of("battery_box_","energy_storage_"))
                craft(server,"energy_nodes/"+prefix+tier,prefix+tier,rows);
        var machineRows=new JsonArray();
        for(String suffix:List.of("galvanized_steel","aluminium","stainless_steel","chromium","titanium")) {
            String name="lightning_"+suffix;
            craft(server,"machines/basic/"+name,name,rows);
            var block=(BasicMachineBlock)BuiltInRegistries.BLOCK.get(id(name));var spec=block.basicSpec();
            require(spec.recipeMap()==com.gregtech.gregtech.data.MachineRecipeMaps.Lightning && !spec.recipeMap().mRecipeList.isEmpty(), "No actual Lightning recipe map "+name);
            require(spec.energyIn()==(32L<<(2*(spec.tier()-1))) && spec.hardness()==4f, "Wrong original Lightning grade "+name);
            require(com.gregtech.gregtech.loaders.b.OriginCreativeContents.family(block.asItem()).equals("basic_machines"), "Missing Lightning creative entry "+name);
            machineRows.add(name);
        }
        craft(server,"machines/basic/melter_stainless_steel","melter_stainless_steel",rows);
        craft(server,"machines/multiblock/coke_oven_main","coke_oven_main",rows);
        var melter=(BasicMachineBlock)BuiltInRegistries.BLOCK.get(id("melter_stainless_steel"));var spec=melter.basicSpec();
        require(spec.material()==com.gregtech.gregtech.content.material.Materials.Iron && spec.energyIn()==32 && spec.parallelLimit()==1000, "Original Melter casing/rate/parallel differs");
        require(!spec.recipeMap().mRecipeList.isEmpty(), "Melter has no processing recipes");
        var entity=melter.newBlockEntity(BlockPos.ZERO,melter.defaultBlockState());
        try {
            var cheap=entity.getClass().getDeclaredMethod("cheapOverclocking");cheap.setAccessible(true);
            var duration=entity.getClass().getDeclaredMethod("parallelScalesDuration");duration.setAccessible(true);
            require((Boolean)cheap.invoke(entity) && (Boolean)duration.invoke(entity), "Melter still ignores original operating flags");
            var cost=com.gregtech.gregtech.api.recipe.MachineWorkCost.calculate(1,20,3,(Boolean)duration.invoke(entity),10000,spec.energyInMin(),spec.energyInMax(),(Boolean)cheap.invoke(entity));
            require(cost!=null && cost.minimumPower()==1 && cost.totalWork()==60,"Native Melter still applies expensive low-power overclocking");
        } catch(ReflectiveOperationException e) {throw new IllegalStateException(e);}
        int groups=0;
        for(var node:com.gregtech.gregtech.content.energy.EnergyNodeDefinitions.specifications()) {
            String name=node.id(); String expected=name.startsWith("electric_dynamo_")||name.startsWith("flux_dynamo_")?"dynamos"
                :name.startsWith("battery_box_")||name.startsWith("energy_storage_")?"battery_boxes"
                :name.startsWith("transformer_")?"transformers":null;
            if(expected!=null){require(com.gregtech.gregtech.loaders.b.OriginCreativeContents.family(BuiltInRegistries.ITEM.get(id(name))).equals(expected),"Wrong actual source creative family "+name);groups++;}
        }
        for(var node:com.gregtech.gregtech.content.logistics.LongDistanceCatalog.ALL)
            require(com.gregtech.gregtech.loaders.b.OriginCreativeContents.family(BuiltInRegistries.ITEM.get(id(node.id()))).equals("long_distance_transport"),"Long-distance block absent from original page "+node.id());
        receipt.add("machineFeedbackCrafting",rows);receipt.add("lightningProcessorsChecked",machineRows);
        receipt.addProperty("sourceEnergyCreativeGroupsChecked",groups);
        receipt.addProperty("longDistanceCreativeGroupsChecked",com.gregtech.gregtech.content.logistics.LongDistanceCatalog.ALL.size());
        receipt.addProperty("sourceMelterOperatingFlagChecks",3);
    }
    static void client(net.minecraft.client.Minecraft minecraft,JsonObject receipt) {
        int count=0;
        for(String suffix:List.of("galvanized_steel","aluminium","stainless_steel","chromium","titanium")) {
            var stack=new ItemStack(BuiltInRegistries.ITEM.get(id("lightning_"+suffix)));
            OriginFeedbackChecks.checkModel(minecraft,stack,true);
            var spec=((BasicMachineBlock)((BlockItem)stack.getItem()).getBlock()).basicSpec();
            require((minecraft.getItemColors().getColor(stack,0)&0xffffff)==(spec.material().getColor()&0xffffff),"Lightning inventory material tint absent "+suffix);
            require(!stack.getHoverName().getString().startsWith("block.gregtech."),"Lightning item missing language "+suffix);count++;
        }
        OriginFeedbackChecks.checkModel(minecraft,new ItemStack(BuiltInRegistries.ITEM.get(id("melter_stainless_steel"))),true);
        receipt.addProperty("sourceMachineInventoryModelsChecked",count+1);
    }
}
