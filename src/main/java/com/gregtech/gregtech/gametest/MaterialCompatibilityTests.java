package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.material.MaterialPresentation;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.*;
import java.util.*;
@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class MaterialCompatibilityTests {
    @GameTest(template="test_empty") public static void forgeTagsAndVanillaFormsAreBound(GameTestHelper h) {
        for(var pair:List.of(Map.entry("Iron",Items.IRON_INGOT),Map.entry("Gold",Items.GOLD_INGOT),Map.entry("Copper",Items.COPPER_INGOT))) {
            var stack=GTItems.getStack(MaterialPrefix.ingot,GTMaterialRegistry.get(pair.getKey()));
            String tag="forge:ingots/"+pair.getKey().toLowerCase();
            h.assertTrue(stack.is(ItemTags.create(ResourceLocation.parse(tag))),"Live synchronized tag "+tag);
            h.assertTrue(RecipeInputs.matches(stack,new ItemStack(pair.getValue())),"Vanilla ingot matches GT form");
            h.assertTrue(RecipeInputs.matches(new ItemStack(pair.getValue()),stack),"GT ingot matches vanilla form");
        }
        var redstone=GTItems.getStack(MaterialPrefix.dust,GTMaterialRegistry.get("Redstone"));
        h.assertTrue(RecipeInputs.matches(redstone,new ItemStack(Items.REDSTONE)),"Redstone canonical dust");
        h.assertTrue(!RecipeInputs.matches(redstone,GTItems.getStack(MaterialPrefix.dustTiny,GTMaterialRegistry.get("Redstone"))),"Tiny dust is not full dust");
        h.assertTrue(!RecipeInputs.matches(new ItemStack(Items.IRON_INGOT),new ItemStack(Items.GOLD_INGOT)),"Parent ingots tag never merges metals");
        h.assertTrue(!RecipeInputs.matches(GTItems.getStack(MaterialPrefix.oreRaw,GTMaterialRegistry.get("Iron")),new ItemStack(Items.RAW_IRON_BLOCK)),"Nine raw ores are not one");
        h.succeed();
    }
    @GameTest(template="test_empty") public static void cauldronRecyclesMetalRatherThanOre(GameTestHelper h) {
        var iron=GTMaterialRegistry.get("Iron");
        h.assertTrue(iron.getTargetCrushingMaterial()!=iron.getTargetPulverMaterial(),"Independent GT6 targets");
        var recipe=MachineRecipeMaps.Shredder.findRecipe(List.of(new ItemStack(Items.CAULDRON)),List.of(),false,1,12);
        h.assertTrue(recipe!=null,"Cauldron recipe exists");
        var output=(com.gregtech.gregtech.item.MaterialItem)recipe.mOutputs[0].getItem();
        h.assertTrue(output.getMaterial().resolve()==iron.resolve()&&recipe.mOutputs[0].getCount()==7,"Seven iron dust, not hematite");
        h.succeed();
    }
    @GameTest(template="test_empty") public static void registeredNamesHaveTranslations(GameTestHelper h) throws Exception {
        var gson=new com.google.gson.Gson();
        var missing=new TreeSet<String>();
        var languages=new ArrayList<Map<String,String>>();
        for(String language:List.of("en_us","zh_cn")) try(var stream=MaterialCompatibilityTests.class.getResourceAsStream("/assets/gregtech/lang/"+language+".json")) {
            languages.add(gson.fromJson(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8),new com.google.gson.reflect.TypeToken<Map<String,String>>(){}.getType()));
        }
        for(var item:net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues())
            if(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item).getNamespace().equals("gregtech")) inspect(item.getName(new ItemStack(item)),languages,missing);
        for(var mat:GTMaterialRegistry.allMaterials()) inspect(MaterialPresentation.name(mat),languages,missing);
        java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/localization-runtime-missing.json"),gson.toJson(missing));
        h.assertTrue(missing.isEmpty(),"Missing registered names: "+missing);
        h.succeed();
    }
    @GameTest(template="test_empty") public static void oreCrushingAndRockMassFollowDistinctPaths(GameTestHelper h) {
        var iron=GTMaterialRegistry.get("Iron");
        var raw=new ItemStack(Items.RAW_IRON);
        var crusher=MachineRecipeMaps.Crusher.findRecipe(List.of(raw),List.of(),false,1,12);
        var hammer=MachineRecipeMaps.Hammer.findRecipe(List.of(raw),List.of(),false,1,1);
        h.assertTrue(crusher!=null&&hammer!=null&&crusher.mOutputs.length==2,"Crusher two outputs, hammer one");
        h.assertTrue(crusher.mOutputs[0].getCount()==hammer.mOutputs[0].getCount(),"Same base multiplier per output");
        h.assertTrue(((com.gregtech.gregtech.item.MaterialItem)crusher.mOutputs[0].getItem()).getMaterial().resolve()==iron.getTargetCrushingMaterial().resolve(),"Ore crushing uses ore target");
        h.assertTrue(MachineRecipeMaps.Sifting.findRecipe(List.of(raw),List.of(),false,1,6)==null,"Standard raw ore is not dust ore");
        var vanilla=MachineRecipeMaps.Crusher.findRecipe(List.of(new ItemStack(Items.IRON_ORE)),List.of(),false,1,12);
        h.assertTrue(vanilla!=null&&vanilla.mOutputs[0].getCount()==crusher.mOutputs[0].getCount(),"Vanilla iron ore uses same processing rules");
        var rock=GTItems.getStack(MaterialPrefix.rockGt,iron);
        var shred=MachineRecipeMaps.Shredder.findRecipe(List.of(rock),List.of(),false,1,12);
        h.assertTrue(shred!=null,"Bearing rock recovery");
        long amount=0;
        for(var out:shred.mOutputs) if(out.getItem() instanceof com.gregtech.gregtech.item.MaterialItem m)amount+=m.getPrefix().getMaterialWeight()*out.getCount();
        h.assertTrue(amount<=MaterialPrefix.rockGt.getMaterialWeight(),"Rock cannot multiply into full ore outputs");
        h.assertTrue(MachineRecipeMaps.Anvil.containsInput(new ItemStack(Items.COPPER_INGOT)),"Vanilla copper passes anvil insertion filter");
        h.succeed();
    }
    @GameTest(template="test_empty") public static void smallOreMiningDropsProductsRatherThanItself(GameTestHelper h) {
        var material=GTMaterialRegistry.get("Diamond");
        var prefix=com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.oreSmall;
        var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech",prefix.getBlockId(material)));
        var position=h.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        h.getLevel().setBlockAndUpdate(position,block.defaultBlockState());
        var be=h.getLevel().getBlockEntity(position);
        var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.enchant(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,1);
        var loot=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.atCenterOf(position))
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL,tool)
                .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY,be);
        var drops=block.getDrops(block.defaultBlockState(),loot);
        h.assertTrue(!drops.isEmpty()&&drops.stream().noneMatch(stack->stack.is(block.asItem())),"Even silk touch uses GT6 weighted small-ore drops");
        var a=com.gregtech.gregtech.block.SmallOreDrops.drops(material,position,3,false,null);
        var b=com.gregtech.gregtech.block.SmallOreDrops.drops(material,position,3,false,null);
        h.assertTrue(a.size()==b.size(),"Location-seeded drops deterministic");
        for(int i=0;i<a.size();i++)h.assertTrue(ItemStack.matches(a.get(i),b.get(i)),"Stable drop pool");
        h.succeed();
    }
    @GameTest(template="test_empty") public static void oilsandsProduceOilWithoutConsumingOil(GameTestHelper h) {
        var sand=GTMaterialRegistry.get("Oilsands");
        var input=GTItems.getStack(MaterialPrefix.oreRaw,sand);
        h.assertTrue(!input.isEmpty(),"Oilsands raw form");
        var recipe=MachineRecipeMaps.Centrifuge.findRecipe(List.of(input),List.of(),false,1,6);
        h.assertTrue(recipe!=null&&recipe.mFluidInputs.length==0,"Oilsands require no oil input");
        h.assertTrue(recipe.mFluidOutputs.length==1&&recipe.mFluidOutputs[0].getAmount()==500,"Two-unit ore extracts 500 mB crude oil");
        h.assertTrue(recipe.mOutputs[0].getCount()==4,"Four dust units of sand");
        h.succeed();
    }
    @GameTest(template="test_empty") public static void vanillaStickAssemblesGregTechTools(GameTestHelper h) {
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}
        };
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
        var type=com.gregtech.gregtech.api.tool.GTToolType.PICKAXE;
        grid.setItem(0,GTItems.getStack(type.headPrefix(),GTMaterialRegistry.get("Iron")));
        grid.setItem(1,new ItemStack(Items.STICK));
        var recipe=new com.gregtech.gregtech.recipe.GTToolAssemblyRecipe(ResourceLocation.parse("gregtech:test_vanilla_handle"),type);
        h.assertTrue(recipe.matches(grid,h.getLevel())&&!recipe.assemble(grid,h.getLevel().registryAccess()).isEmpty(),"Vanilla stick is a real GT wood handle");
        grid.setItem(1,new ItemStack(Items.IRON_INGOT));
        h.assertTrue(!recipe.matches(grid,h.getLevel()),"An ingot is not a rod");
        h.succeed();
    }
    @GameTest(template="test_empty") public static void headlessManualToolsCraftDirectlyFromMaterial(GameTestHelper h) {
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}
        };
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
        var steel=GTMaterialRegistry.get("Steel");
        // The one-piece tools keep GT6's shaped row (Loader_Tools:305-320): 3 plates around a hammer.
        grid.setItem(0,GTItems.getStack(MaterialPrefix.plate,steel));
        grid.setItem(1,com.gregtech.gregtech.api.tool.GTToolHelper.displayTool(
                com.gregtech.gregtech.api.tool.GTToolType.HARD_HAMMER));
        grid.setItem(2,GTItems.getStack(MaterialPrefix.plate,steel));
        grid.setItem(4,GTItems.getStack(MaterialPrefix.plate,steel));
        grid.setItem(7,GTItems.getStack(MaterialPrefix.plate,steel));
        var type=com.gregtech.gregtech.api.tool.GTToolType.WRENCH;
        var recipe=new com.gregtech.gregtech.recipe.GTToolCraftingRecipe(
                ResourceLocation.parse("gregtech:test_direct_wrench"),type,0);
        h.assertTrue(recipe.matches(grid,h.getLevel()),"Three same-material plates and a hammer craft a wrench");
        var output=recipe.assemble(grid,h.getLevel().registryAccess());
        h.assertTrue(!output.isEmpty()&&com.gregtech.gregtech.api.tool.GTToolHelper.getHead(output)==steel,
                "Direct tool keeps its input material stats");
        grid.setItem(7,GTItems.getStack(MaterialPrefix.plate,GTMaterialRegistry.get("Bronze")));
        h.assertTrue(!recipe.matches(grid,h.getLevel()),"Direct tool components cannot mix materials");
        for(String id:com.gregtech.gregtech.loaders.Loader_ToolCraftingRecipes.registeredIds()) {
            h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id)).isPresent(),
                    "Tool recipe loaded: "+id);
        }
        h.succeed();
    }
    @GameTest(template="test_empty") public static void basicMachinesHaveSurvivalRecipes(GameTestHelper h) {
        int expected=0;
        for(var entry:com.gregtech.gregtech.registry.GTBasicMachines.all()) {
            var spec=entry.get().basicSpec();
            if(com.gregtech.gregtech.data.BasicMachineRecipePack.MULTIBLOCK_CONTROLLERS.contains(spec.machineName()))continue;
            expected++;
            var id=ResourceLocation.fromNamespaceAndPath("gregtech","machines/basic/"+spec.id());
            var recipe=h.getLevel().getRecipeManager().byKey(id).orElseThrow();
            // GT6 patterns contain empty cells on purpose (e.g. Rolling Mill "M "), so count
            // filled pattern cells and require exactly that many real ingredients.
            var table=com.gregtech.gregtech.data.BasicMachineCraftingRecipes.find(spec.machineName(),spec.tier());
            h.assertTrue(table!=null,"Original GT6 crafting pattern exists: "+id);
            int filled=0;
            for(var row:table.rows())for(int i=0;i<row.length();i++)if(row.charAt(i)!=' ')filled++;
            long resolved=recipe.getIngredients().stream().filter(ingredient->!ingredient.isEmpty()).count();
            h.assertTrue(resolved==filled,
                    "Basic machine recipe resolves every original ingredient: "+id+" ("+resolved+"/"+filled+")");
        }
        h.assertTrue(expected>200,"Full tiered single-block machine recipe family is covered");
        h.succeed();
    }
    private static void inspect(net.minecraft.network.chat.Component c,List<Map<String,String>> langs,Set<String> missing) {
        if(c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
            if(t.getKey().contains("gregtech")) for(int i=0;i<langs.size();i++) if(!langs.get(i).containsKey(t.getKey())) missing.add((i==0?"en_us:":"zh_cn:")+t.getKey());
            for(var arg:t.getArgs())if(arg instanceof net.minecraft.network.chat.Component nested)inspect(nested,langs,missing);
        }
        for(var sibling:c.getSiblings())inspect(sibling,langs,missing);
    }
}
