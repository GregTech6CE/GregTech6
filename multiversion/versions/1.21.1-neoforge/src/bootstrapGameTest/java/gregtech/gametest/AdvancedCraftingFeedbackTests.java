package gregtech.gametest;

import gregtech.content.block.misc.AdvancedCraftingTableBlockEntity;
import gregtech.client.gui.AdvancedCraftingMenu;
import gregtech.content.recipe.AdvancedCraftingRecipeReload;
import gregtech.content.registry.GTCraftingTables;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.Map;

/** Exercises the actual worktable inventory and recipe manager without starting either viewer. */
@GameTestHolder("gt6ce_feedback_workbench")
@PrefixGameTestTemplate(false)
public final class AdvancedCraftingFeedbackTests {
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("gt6ce_feedback_workbench",path);}
    private static final class CountedRecipe extends ShapedRecipe {
        private int matches;
        CountedRecipe(net.minecraft.world.item.Item input,net.minecraft.world.item.Item output){
            super("",CraftingBookCategory.MISC,ShapedRecipePattern.of(Map.of('X',Ingredient.of(input)),"X"),new ItemStack(output));
        }
        @Override public boolean matches(CraftingInput input,Level level){matches++;return super.matches(input,level);}
    }

    @GameTest(template="test_empty",timeoutTicks=100)
    public static void stoneLayerModeDisablesOnlyLegacyOverworldVeins(GameTestHelper helper){
        var old=gregtech.content.worldgen.GTOreVeins.OVERWORLD_VEINS;
        helper.assertTrue(old.size()==31&&old.stream().anyMatch(v->v.name().equals("lignite")),"retain the original alternate-mode catalog");
        helper.assertTrue(gregtech.content.worldgen.GTWorldgenDefaults.STONE_LAYERS
                &&gregtech.content.worldgen.GTOreVeins.activeOverworldVeins().isEmpty(),
                "GT6 default strata mode disables every legacy Overworld large vein");
        var shown=gregtech.jei.WorldgenInfoData.buildVeins();
        helper.assertTrue(shown.size()==5&&shown.stream().allMatch(v->v.dimension().equals("minecraft:the_end")),
                "JEI and EMI show only active large veins and retain the End catalog");
        helper.assertTrue(!gregtech.content.worldgen.GTOreVeins.OVERWORLD_SMALL_ORES.isEmpty()
                &&!gregtech.content.worldgen.GTStoneLayersGen.LAYERS.isEmpty(),
                "small-ore and stone-layer data remain independent");
        helper.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=100)
    public static void realTransferSlotsAndRepeatedCraftsAvoidRecipeScans(GameTestHelper helper){
        helper.setBlock(new BlockPos(1,1,1),GTCraftingTables.ADVANCED.getFirst().get());
        var table=(AdvancedCraftingTableBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1,1,1)));
        var player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu=new AdvancedCraftingMenu(1,player.getInventory(),table);
        helper.assertTrue(menu.craftingSlots().size()==9&&menu.transferInventorySlots().size()==57
                &&menu.transferInputSources().size()==66,"viewers can fill nine real cells from the player, tool rack and 16-slot store");
        for(int i=0;i<9;i++)helper.assertTrue(menu.craftingSlots().get(i).index==22+i
                &&menu.craftingSlots().get(i).getContainerSlot()==21+i,"viewer grid follows original row-major slot mapping");
        helper.assertTrue(menu.transferInventorySlots().stream().noneMatch(s->s.index==0||s.index>=31&&s.index<36),
                "blueprint, remainder destinations and virtual buttons cannot be stolen as ingredients");
        helper.assertTrue(menu.getSlot(AdvancedCraftingMenu.RESULT).getContainerSlot()==AdvancedCraftingMenu.RESULT,
                "EMI instant crafting must address result 33 instead of blueprint 0");

        var manager=helper.getLevel().getRecipeManager();
        var previous=new ArrayList<RecipeHolder<?>>(manager.getRecipes());
        var distractors=new ArrayList<CountedRecipe>();
        var recipes=new ArrayList<RecipeHolder<?>>();
        for(int i=0;i<32;i++){
            var recipe=new CountedRecipe(Items.BARRIER,Items.COAL);distractors.add(recipe);
            recipes.add(new RecipeHolder<>(id("distractor_"+i),recipe));
        }
        var target=new CountedRecipe(Items.STRUCTURE_VOID,Items.EMERALD);
        recipes.add(new RecipeHolder<>(id("target"),target));manager.replaceRecipes(recipes);
        try{
            table.items().setStackInSlot(21,new ItemStack(Items.STRUCTURE_VOID));
            table.items().setStackInSlot(35,new ItemStack(Items.STRUCTURE_VOID,8));
            helper.assertTrue(table.preview().is(Items.EMERALD),"real manager resolves initial pattern");
            int scanned=distractors.stream().mapToInt(r->r.matches).sum(),matched=target.matches;
            for(int i=0;i<20;i++){table.preview();menu.broadcastChanges();}
            helper.assertTrue(target.matches==matched&&distractors.stream().mapToInt(r->r.matches).sum()==scanned,
                    "unchanged previews and menu broadcasts never rematch recipes");
            for(int i=0;i<4;i++)helper.assertTrue(table.craftOne(player).is(Items.EMERALD),"repeat craft consumes stored ingredients");
            helper.assertTrue(distractors.stream().mapToInt(r->r.matches).sum()==scanned
                    &&table.items().getStackInSlot(35).getCount()==4&&table.items().getStackInSlot(21).getCount()==1,
                    "cached recipe ID bypasses all distractors while preserving the physical sample");

            var replacement=new CountedRecipe(Items.STRUCTURE_VOID,Items.DIAMOND);
            recipes.set(recipes.size()-1,new RecipeHolder<>(id("target"),replacement));manager.replaceRecipes(recipes);
            helper.assertTrue(table.preview().is(Items.DIAMOND),"replacing a recipe in the same manager invalidates an existing preview");
            table.items().setStackInSlot(21,new ItemStack(Items.DIRT));
            helper.assertTrue(table.preview().isEmpty(),"an unmatched pattern has no output");
            recipes.add(new RecipeHolder<>(id("added_on_reload"),new CountedRecipe(Items.DIRT,Items.GOLD_INGOT)));
            manager.replaceRecipes(recipes);AdvancedCraftingRecipeReload.invalidate();
            helper.assertTrue(table.preview().is(Items.GOLD_INGOT),"datapack reload also invalidates previously empty previews");

            for(int i=21;i<30;i++)table.items().setStackInSlot(i,ItemStack.EMPTY);
            table.items().setStackInSlot(29,new ItemStack(Items.HONEY_BOTTLE));
            recipes.add(new RecipeHolder<>(id("offset_bottle"),new CountedRecipe(Items.HONEY_BOTTLE,Items.EMERALD)));
            manager.replaceRecipes(recipes);AdvancedCraftingRecipeReload.invalidate();
            helper.assertTrue(table.craftOne(player).is(Items.EMERALD)
                    &&table.items().getStackInSlot(29).is(Items.GLASS_BOTTLE)
                    &&table.items().getStackInSlot(21).isEmpty(),
                    "cropped one-cell recipes at the bottom right return their container to the same physical cell");
        }finally{manager.replaceRecipes(previous);AdvancedCraftingRecipeReload.invalidate();}
        helper.succeed();
    }
}
