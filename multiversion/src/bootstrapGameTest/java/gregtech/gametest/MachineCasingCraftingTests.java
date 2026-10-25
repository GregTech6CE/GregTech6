package gregtech.gametest;

import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.internal.material.*;
import gregtech.internal.prefix.BlockMaterialPrefix;
import gregtech.internal.tool.GTToolType;
import gregtech.content.material.Materials;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.item.GTToolItem;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.content.recipe.ToolShapedRecipe;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayerFactory;
//?}

/** Original Loader_OreProcessing:152-155; actual native recipe selection and workbench takes. */
@GameTestHolder("gregtech_machine_casing")
@PrefixGameTestTemplate(false)
public final class MachineCasingCraftingTests {
    private static MaterialPrefix[] plates() { return new MaterialPrefix[]{MaterialPrefix.plate,
            MaterialPrefix.plateDouble, MaterialPrefix.plateQuadruple, MaterialPrefix.plateDense}; }
    private static BlockMaterialPrefix[] casings() { return new BlockMaterialPrefix[]{BlockMaterialPrefix.casingMachine,
            BlockMaterialPrefix.casingMachineDouble, BlockMaterialPrefix.casingMachineQuadruple, BlockMaterialPrefix.casingMachineDense}; }
    private static ResourceLocation id(GTMaterial material, int grade) {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "hand/machine_casing/"
                + casings()[grade].getRegistryName() + "/" + MaterialEquivalence.materialName(material));
    }
    @GameTest(template="test_empty", batch="machine_casing_crafting")
    public static void basicCasingWorkbench(GameTestHelper h) { grade(h,0); h.succeed(); }
    @GameTest(template="test_empty", batch="machine_casing_crafting")
    public static void robustCasingWorkbench(GameTestHelper h) { grade(h,1); h.succeed(); }
    @GameTest(template="test_empty", batch="machine_casing_crafting")
    public static void reinforcedCasingWorkbench(GameTestHelper h) { grade(h,2); h.succeed(); }
    @GameTest(template="test_empty", batch="machine_casing_crafting")
    public static void denseCasingWorkbench(GameTestHelper h) { grade(h,3); h.succeed(); }

    private static CraftingRecipe registered(GameTestHelper h, GTMaterial material, int grade) {
        var found = h.getLevel().getRecipeManager().byKey(id(material,grade))
                .orElseThrow(() -> new AssertionError("Missing casing workbench recipe " + id(material,grade)));
//? if >=1.21.1 {
        /*var recipe = (CraftingRecipe) found.value();
*///?} else {
        var recipe = (CraftingRecipe) found;
//?}
        h.assertTrue(recipe instanceof ToolShapedRecipe, "Uses existing tool recipe and remainder contract");
        return recipe;
    }
    private static ItemStack wrench() { return GTToolItem.create(GTToolType.WRENCH, Materials.Steel, GTMaterialRegistry.get("Wood")); }
    private static List<ItemStack> grid(GTMaterial material, int grade, ItemStack wrench) {
        var cells = new ArrayList<ItemStack>();
        for (int slot=0;slot<9;slot++) cells.add(slot==4 ? wrench : GTItems.getStack(
                slot==0 || slot==8 ? MaterialPrefix.stickLong : plates()[grade], material,1));
        return cells;
    }
    private static void check(GameTestHelper h, GTMaterial material, int grade, List<ItemStack> cells, boolean expected) {
        var recipe = registered(h,material,grade);
//? if >=1.21.1 {
        /*var input = CraftingInput.of(3,3,cells);
*///?} else {
        var holder = new AbstractContainerMenu(null,0) {
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot) { return ItemStack.EMPTY; }
            public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
        };
        var input = new TransientCraftingContainer(holder,3,3);
        for(int slot=0;slot<9;slot++) input.setItem(slot,cells.get(slot));
//?}
        h.assertTrue(recipe.matches(input,h.getLevel())==expected,"Exact material/grade/wrench/nonmirror grid: " + id(material,grade));
        if (!expected) return;
        var chosen = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow();
//? if >=1.21.1 {
        /*h.assertTrue(chosen.id().equals(id(material,grade)),"Actual RecipeManager selects casing row");
        var result = chosen.value().assemble(input,h.getLevel().registryAccess());
*///?} else {
        h.assertTrue(chosen.getId().equals(id(material,grade)),"Actual RecipeManager selects casing row");
        var result = chosen.assemble(input,h.getLevel().registryAccess());
//?}
        h.assertTrue(result.is(GTBlocks.getStack(casings()[grade],material).getItem()) && result.getCount()==1,"Exactly one requested casing");
    }
    private static void grade(GameTestHelper h,int grade) {
        for (String name : List.of("Aluminium","Iron","Steel")) {
            var material = GTMaterialRegistry.get(name);
            var cells = grid(material,grade,wrench());
            check(h,material,grade,cells,true);
            var wrong = grid(material,grade,new ItemStack(Items.STICK)); check(h,material,grade,wrong,false);
            wrong = grid(material,grade,wrench()); wrong.set(1,GTItems.getStack(plates()[grade],Materials.Copper,1)); check(h,material,grade,wrong,false);
            wrong = grid(material,grade,wrench()); wrong.set(1,GTItems.getStack(plates()[(grade+1)%4],material,1)); check(h,material,grade,wrong,false);
            wrong = grid(material,grade,wrench()); Collections.swap(wrong,0,2); Collections.swap(wrong,6,8); check(h,material,grade,wrong,false);
            take(h,material,grade,false);
        }
        take(h,Materials.Aluminium,grade,true);
    }
    private static void take(GameTestHelper h, GTMaterial material,int grade,boolean breaking) {
        var position = h.absolutePos(new BlockPos(1,1,1));
        h.assertTrue(h.getLevel().getBlockEntity(position)==null,"Fixture must not replace a block entity");
        var before = h.getLevel().getBlockState(position);
        var player = FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"CasingCraft"));
        var previous = player.containerMenu;
        try {
            h.getLevel().setBlockAndUpdate(position,Blocks.CRAFTING_TABLE.defaultBlockState());
            var menu = new CraftingMenu(1,player.getInventory(),ContainerLevelAccess.create(h.getLevel(),position));
            player.containerMenu = menu;
            var tool = wrench();
            h.assertTrue(tool.getMaxDamage()>800,"Real usable wrench with crafting durability");
            if (breaking) tool.setDamageValue(tool.getMaxDamage()-800);
            var cells=grid(material,grade,tool);
            for(int slot=0;slot<9;slot++) menu.getSlot(slot+1).set(cells.get(slot));
            h.assertTrue(menu.getSlot(0).getItem().is(GTBlocks.getStack(casings()[grade],material).getItem()),"Actual workbench result slot available");
            menu.clicked(0,0,ClickType.PICKUP,player);
            h.assertTrue(menu.getCarried().is(GTBlocks.getStack(casings()[grade],material).getItem()) && menu.getCarried().getCount()==1,"Actual result-slot take yields one casing");
            for(int slot=0;slot<9;slot++) if(slot!=4) h.assertTrue(menu.getSlot(slot+1).getItem().isEmpty(),"Consumes six plates and two long rods");
            var remainder=menu.getSlot(5).getItem();
            h.assertTrue(breaking ? remainder.isEmpty() : !remainder.isEmpty() && remainder.getDamageValue()==800,"Wrench retained with 800 wear or breaks exactly at boundary");
            com.mojang.logging.LogUtils.getLogger().info("GT6_CASING_TAKE material={} grade={} breaking={}",material.getName(),grade,breaking);
        } finally {
            player.containerMenu=previous;
            player.getInventory().clearContent();
            h.getLevel().setBlockAndUpdate(position,before);
        }
    }
    @GameTest(template="test_empty",batch="machine_casing_crafting",timeoutTicks=200)
    public static void allRegisteredCasingGradesHaveAvailablePartsRecipes(GameTestHelper h) {
        int eligible=0, missing=0, nonsmithable=0;
        for(var material:GTMaterialRegistry.allMaterials()) {
            if(!material.isValid() || material.resolve()!=material || material.has(MaterialProperty.ANTIMATTER)) continue;
            for(int grade=0;grade<4;grade++) {
                var output=GTBlocks.getStack(casings()[grade],material);
                if(output.isEmpty()) continue;
                boolean available=!GTItems.getStack(plates()[grade],material,1).isEmpty()
                        && !GTItems.getStack(MaterialPrefix.stickLong,material,1).isEmpty();
                if(available) {
                    check(h,material,grade,grid(material,grade,wrench()),true); eligible++;
                    if(!material.has(MaterialProperty.SMITHABLE) || material.has(MaterialProperty.FLAMMABLE)) nonsmithable++;
                } else {
                    h.assertTrue(h.getLevel().getRecipeManager().byKey(id(material,grade)).isEmpty(),"Missing ingredient form cannot create invalid recipe"); missing++;
                }
            }
        }
        h.assertTrue(eligible>100 && missing>0 && nonsmithable>0,"Non-vacuous native roster, missing-parts and non-Welder materials");
        com.mojang.logging.LogUtils.getLogger().info("GT6_CASING_ROSTER eligible={} missing={} nonWelder={}",eligible,missing,nonsmithable);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="machine_casing_crafting")
    public static void aluminiumWelderRoutesRemainUnchanged(GameTestHelper h) {
        for(int grade=0;grade<4;grade++) {
            final int g=grade;
            var output=GTBlocks.getStack(casings()[g],Materials.Aluminium);
            for(boolean longRod:new boolean[]{true,false}) {
                var plate=GTItems.getStack(plates()[g],Materials.Aluminium,6);
                var rod=GTItems.getStack(longRod?MaterialPrefix.stickLong:MaterialPrefix.stick,Materials.Aluminium,longRod?2:4);
                long matches=MachineRecipeMaps.Welder.mRecipeList.stream().filter(r -> r.mInputs.length==2 && r.mOutputs.length==1
                        && r.mInputs[0].is(plate.getItem()) && r.mInputs[0].getCount()==6
                        && r.mInputs[1].is(rod.getItem()) && r.mInputs[1].getCount()==rod.getCount()
                        && r.mOutputs[0].is(output.getItem()) && r.mOutputs[0].getCount()==1
                        && r.mDuration==16L * new long[]{8,14,26,56}[g] && r.mEUt==16).count();
                h.assertTrue(matches==1,"Existing aluminium Welder route retained grade="+g+" longRod="+longRod);
            }
        }
        h.succeed();
    }
}
