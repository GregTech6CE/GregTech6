package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.material.MaterialFormItem;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.recipe.MachineRecipeOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}
/** Ignored native draft; dedicated serial ore audit profile, addon sample profile for addon control.
 * No new recipe rows are inserted. Sources: MT418/3029/3234/3704/3749; Ores389-461; Furnace151-166.
 */
@GameTestHolder("gregtech_ore_source_audit")
@PrefixGameTestTemplate(false)
public final class OreSourceControlTests {
    private static String material(ItemStack s) {
        return s.getItem() instanceof MaterialFormItem form?form.getMaterial().resolve().getName():"";
    }
    private static void absent(GameTestHelper h,RecipeMap map,String name,String reason) {
        h.assertTrue(map.mRecipeList.stream().noneMatch(r->Arrays.stream(r.mInputs).anyMatch(s->material(s).equals(name))),reason);
    }
    @GameTest(template="test_empty",batch="ore_source_differential")
    public static void furnaceUsesSourceFurnaceNotMeltingFlag(GameTestHelper h) {
        absent(h,MachineRecipeMaps.Furnace,"Cinnabar","Source Cinnabar FURNACE absent despite setSmelting adding MELTING");
        // Source OP/OM selects registered unification targets. Native RecipeMap canonicalizes Copper ingots.
        var copperOut=new ItemStack(net.minecraft.world.item.Items.COPPER_INGOT,1);
        var copperForm=com.gregtech.gregtech.api.material.MaterialEquivalence.form(copperOut);
        h.assertTrue(copperForm!=null&&copperForm.prefix()==com.gregtech.gregtech.data.MaterialPrefix.ingot
                &&copperForm.material()==com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Copper").resolve(),
                "explicit minecraft:copper_ingot is registered ingot/Copper, not an arbitrary alias");
        OreSourceDifferentialTests.row(h,"MT418 Copper explicit FURNACE; Furnace oreRaw fixed U",MachineRecipeMaps.Furnace,
                OreSourceDifferentialTests.s("oreRaw","Copper",1),new FluidStack[0],
                new ItemStack[]{copperOut},new long[]{10000},new FluidStack[0],16,16);
        var copperIn=OreSourceDifferentialTests.s("oreRaw","Copper",1);
        boolean nativeCopper=false;
        var cinnabarForms=new ItemStack[]{OreSourceDifferentialTests.s("oreRaw","Cinnabar",1),OreSourceDifferentialTests.s("dust","Cinnabar",1),
                OreSourceDifferentialTests.s("crushed","Cinnabar",1),OreSourceDifferentialTests.s("crushedPurified","Cinnabar",1),OreSourceDifferentialTests.s("crushedCentrifuged","Cinnabar",1)};
        for(var holder:h.getLevel().getRecipeManager().getRecipes()) {
//? if >=1.21.1 {
            /*var recipe=holder.value();
*///?} else {
            var recipe=holder;
//?}
            if(!(recipe instanceof net.minecraft.world.item.crafting.SmeltingRecipe furnace))continue;
            for(var in:cinnabarForms)h.assertTrue(furnace.getIngredients().stream().noneMatch(ingredient->ingredient.test(in)),
                    "actual native RecipeManager must not retain legacy Cinnabar furnace rows");
            if(furnace.getIngredients().stream().anyMatch(ingredient->ingredient.test(copperIn))) {
                var out=furnace.getResultItem(h.getLevel().registryAccess());
                h.assertTrue(out.is(copperOut.getItem())&&out.getCount()==1,"native Copper furnace exact ingot yield");
                nativeCopper=true;
            }
        }
        h.assertTrue(nativeCopper,"positive source Copper furnace recipe reaches actual native RecipeManager");
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential")
    public static void gemlessAndWoodlikeMaterialsDoNotAcquireGenericOreRoutes(GameTestHelper h) {
        absent(h,MachineRecipeMaps.Sifting,"Bauxite","MT3749 default oredustdcmp has no gem ladder");
        absent(h,MachineRecipeMaps.Sluice,"Wood","Wood factory is not a source ore-processing material");
        absent(h,MachineRecipeMaps.MagneticSeparator,"Cinnabar","Ores389-393: no opposite-magnetic byproduct -> no magnetic recipe");
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential")
    public static void magneticOreUsesOnlyOppositeMagneticCandidates(GameTestHelper h) {
        var in=OreSourceDifferentialTests.s("crushedPurified","Magnetite",1);
        var rows=MachineRecipeMaps.MagneticSeparator.mRecipeList.stream()
                .filter(r->r.mInputs.length==1&&r.mInputs[0].is(in.getItem())&&r.mInputs[0].getCount()==1).toList();
        h.assertTrue(rows.size()==1,"Magnetite magnetic native row exists once");
        var r=rows.get(0);
        // Source MT3234 = Hematite, Gold, Stone; Hematite magnetic is excluded, fallback is primary.
        String[] expected={"Magnetite","Gold","Stone","Magnetite","Magnetite","Magnetite"};
        h.assertTrue(r.mOutputs.length==6,"source six magnetic output slots");
        for(int i=0;i<6;i++) {
            h.assertTrue(material(r.mOutputs[i]).equals(expected[i]),"source opposite-magnetic output "+i);
            h.assertTrue(r.mOutputs[i].getCount()==(i==0?1:18),"source magnetic output count "+i);
            h.assertTrue(r.getOutputChance(i)==(i==0?10000:600),"source magnetic chance "+i);
        }
        h.assertTrue(r.mDuration==144&&r.mEUt==16,"source magnetic work");
        var expectedStacks=new ItemStack[]{OreSourceDifferentialTests.s("crushedCentrifuged","Magnetite",1),
                OreSourceDifferentialTests.s("crushedCentrifugedTiny","Gold",18),OreSourceDifferentialTests.s("dustTiny","Stone",18),
                OreSourceDifferentialTests.s("crushedCentrifugedTiny","Magnetite",18),OreSourceDifferentialTests.s("crushedCentrifugedTiny","Magnetite",18),
                OreSourceDifferentialTests.s("crushedCentrifugedTiny","Magnetite",18)};
        OreSourceDifferentialTests.row(h,"MT3234; Stone G_STONE lacks ORES -> OM dustTiny fallback",MachineRecipeMaps.MagneticSeparator,
                in,new FluidStack[0],expectedStacks,new long[]{10000,600,600,600,600,600},new FluidStack[0],144,16);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
    public static void allRegisteredSluicesKeepNineOutputsAcrossSaveLoad(GameTestHelper h) {
        var blocks=BuiltInRegistries.BLOCK.stream().filter(b->b instanceof BasicMachineBlock m
                && (m.basicSpec().machineName().equals("sluice")||m.basicSpec().machineName().equals("largesluice")))
                .map(b->(BasicMachineBlock)b).toList();
        h.assertTrue(blocks.size()==5,"four source regular tiers plus large sluice");
        var pos=h.absolutePos(new BlockPos(1,1,1));
        for(var block:blocks) {
            h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
            try {
                var be=(BasicMachineBlockEntity)h.getLevel().getBlockEntity(pos);
                h.assertTrue(be!=null&&be.outputSlots()==9&&be.inventory().getSlots()==10,"actual registered 1+9 layout");
                be.inventory().setStackInSlot(9,OreSourceDifferentialTests.s("dust","Cinnabar",7));
//? if >=1.21.1 {
                /*var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
                var saved=be.saveWithoutMetadata();
//?}
                h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
                var restored=(BasicMachineBlockEntity)h.getLevel().getBlockEntity(pos);
//? if >=1.21.1 {
                /*restored.loadWithComponents(saved,h.getLevel().registryAccess());
*///?} else {
                restored.load(saved);
//?}
                h.assertTrue(restored.inventory().getSlots()==10&&restored.inventory().getStackInSlot(9).getCount()==7
                        &&material(restored.inventory().getStackInSlot(9)).equals("Cinnabar"),"last original output slot survives native serialization");
            }finally{h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());}
        }
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential")
    public static void opaqueLapisGradesUseOriginalFallbackEvenWhenLegacyGradeItemsExist(GameTestHelper h) {
        // MT1518 G_GEM_ORES+CRYSTAL, TD601 lacks TRANSPARENT; OP174-178 require TRANSPARENT.
        var out=new ItemStack[]{OreSourceDifferentialTests.s("gem","Lapis",8),OreSourceDifferentialTests.s("gem","Lapis",4),
                OreSourceDifferentialTests.s("gem","Lapis",2),OreSourceDifferentialTests.s("gem","Lapis",1),
                OreSourceDifferentialTests.s("gem","Lapis",1),OreSourceDifferentialTests.s("gem","Lapis",1),OreSourceDifferentialTests.s("dust","Lapis",1)};
        OreSourceDifferentialTests.row(h,"OP.mat replacement preserves replacement count, not legacy generated grade",MachineRecipeMaps.Sifting,
                OreSourceDifferentialTests.s("crushedPurified","Lapis",1),new FluidStack[0],out,
                new long[]{9,90,360,1350,1800,3600,4500},new FluidStack[0],144,16);
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_differential")
    public static void sluiceRollsEachOfEightSlotsAndEachItemIndependently(GameTestHelper h) {
        var in=OreSourceDifferentialTests.s("crushed","Cinnabar",1);
        var recipe=MachineRecipeMaps.Sluice.mRecipeList.stream().filter(r->r.mInputs.length==1&&r.mInputs[0].is(in.getItem())
                &&r.mFluidInputs.length==1&&r.mFluidInputs[0].getFluid()==net.minecraft.world.level.material.Fluids.WATER).findFirst().orElseThrow();
        h.assertTrue(recipe.mOutputs.length==9,"all eight chance slots exist before RNG probe");
        for(int slot=1;slot<9;slot++) {
            var calls=new java.util.concurrent.atomic.AtomicInteger();
            long actual=recipe.rollOutputCount(slot,2,bound->{h.assertTrue(bound==10000,"source chance denominator");
                return calls.getAndIncrement()%2==0?299:300;});
            h.assertTrue(calls.get()==18&&actual==9,"9 items times2 processes: alternating boundary succeeds9, slot "+slot);
            h.assertTrue(recipe.rollOutputCount(slot,2,bound->300)==0,"equal chance boundary fails");
            h.assertTrue(recipe.rollOutputCount(slot,2,bound->299)==18,"one below chance succeeds each item");
        }
        h.assertTrue(recipe.rollOutputCount(0,2,bound->{throw new AssertionError("Guaranteed slot should not need RNG");})==2,"guaranteed primary twice");
        h.succeed();
    }
    @GameTest(template="test_empty",batch="ore_source_addon_profile",timeoutTicks=200)
    public static void realAddonOwnedMapsSurviveTwoOverlayRebuilds(GameTestHelper h) {
        // Dedicated sample-addon profile. Fail if not installed; absence is not a passing control.
        var maps=RecipeMap.RECIPE_MAP_LIST.stream().filter(m->m.mNameInternal.startsWith("addon_sample.")).toList();
        h.assertTrue(maps.size()>=2,"real sample addon owns maps before audit, no injected desired rows");
        var before=new TreeMap<String,List<String>>();
        for(var m:maps)before.put(m.mNameInternal,snapshot(h,m));
        h.assertTrue(before.values().stream().allMatch(rows->!rows.isEmpty()),"existing addon recipes loaded");
        for(int i=0;i<2;i++) {
            MachineRecipeOverlay.apply(h.getLevel().getRecipeManager());
            for(var m:maps)h.assertTrue(snapshot(h,m).equals(before.get(m.mNameInternal)),"addon map remains semantically identical after real overlay rebuild "+m.mNameInternal);
        }
        h.succeed();
    }
    private static List<String> snapshot(GameTestHelper h,RecipeMap map) {
        var result=new ArrayList<String>();
        for(var r:map.mRecipeList) {
            var fields=new ArrayList<String>();
            for(var stack:r.mInputs)fields.add("IN:"+savedItem(h,stack));
            for(var stack:r.mOutputs)fields.add("OUT:"+savedItem(h,stack));
            for(var fluid:r.mFluidInputs)fields.add("FIN:"+savedFluid(h,fluid));
            for(var fluid:r.mFluidOutputs)fields.add("FOUT:"+savedFluid(h,fluid));
            fields.add("CHANCE:"+Arrays.toString(r.mChances));fields.add("WORK:"+r.mDuration+":"+r.mEUt);
            fields.add("FLAGS:"+r.mEnabled+":"+r.mFakeRecipe);
            result.add(fields.toString());
        }
        Collections.sort(result);return result;
    }
    private static String savedItem(GameTestHelper h,ItemStack stack) {
        if(stack==null||stack.isEmpty())return "EMPTY";
//? if >=1.21.1 {
        /*return stack.save(h.getLevel().registryAccess()).toString();
*///?} else {
        return stack.save(new net.minecraft.nbt.CompoundTag()).toString();
//?}
    }
    private static String savedFluid(GameTestHelper h,FluidStack fluid) {
        if(fluid==null||fluid.isEmpty())return "EMPTY";
//? if >=1.21.1 {
        /*return fluid.save(h.getLevel().registryAccess()).toString();
*///?} else {
        return fluid.writeToNBT(new net.minecraft.nbt.CompoundTag()).toString();
//?}
    }

}
