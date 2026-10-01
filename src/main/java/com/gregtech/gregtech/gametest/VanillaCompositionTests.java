package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.material.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class VanillaCompositionTests {
    @GameTest(template="test_empty") public static void allVanillaItemsHaveExplicitMaterialClassification(GameTestHelper h) {
        var missing = new ArrayList<String>();
        for (var item : ForgeRegistries.ITEMS.getValues()) if (item != Items.AIR && ForgeRegistries.ITEMS.getKey(item).getNamespace().equals("minecraft"))
            if (ItemMaterialRegistry.base(item).isEmpty()) missing.add(ForgeRegistries.ITEMS.getKey(item).toString());
        com.gregtech.gregtech.GregTech.LOGGER.info("Vanilla composition coverage: {} missing: {}", missing.size(), missing);
        h.assertTrue(missing.isEmpty(), "Missing compositions: " + missing);
        long total=ForgeRegistries.ITEMS.getValues().stream().filter(i->i!=Items.AIR&&ForgeRegistries.ITEMS.getKey(i).getNamespace().equals("minecraft")).count();
        long classified=ItemMaterialRegistry.entries().entrySet().stream().filter(e->ForgeRegistries.ITEMS.getKey(e.getKey()).getNamespace().equals("minecraft")&&!e.getValue().components().isEmpty()).count();
        com.gregtech.gregtech.GregTech.LOGGER.info("Vanilla material audit: total={}, nonempty={}, explicit nonmaterial={}",total,classified,total-classified);
        h.succeed();
    }
    @GameTest(template="test_empty") public static void sourceComponentsAndDamageArePreserved(GameTestHelper h) {
        var pick = new ItemStack(Items.IRON_PICKAXE);
        var data = ItemMaterialRegistry.get(pick).orElseThrow();
        h.assertTrue(data.components().size()==2,"Iron head and wood handle");
        h.assertTrue(units(data,"Iron")==GTValues.U*26/9&&units(data,"Wood")==GTValues.U,"GT6 source tool-head weights");
        pick.setDamageValue(pick.getMaxDamage()/2);
        var worn=ItemMaterialRegistry.get(pick).orElseThrow();
        h.assertTrue(units(worn,"Iron")==units(data,"Iron")/2,"Damaged tools do not recover a fresh tool's metal");
        h.assertTrue(com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput.parse(pick).size()==2,"Crucible reads both components");
        h.assertTrue(com.gregtech.gregtech.api.machine.FurnaceFuelValue.getBurnValue(new ItemStack(Items.IRON_PICKAXE))==0,"Wood handle cannot make iron equipment furnace fuel");
        h.assertTrue(units(ItemMaterialRegistry.get(new ItemStack(Items.CLAY)).orElseThrow(),"Clay")==GTValues.U*4,"Clay block four units");
        h.assertTrue(units(ItemMaterialRegistry.get(new ItemStack(Items.STONE)).orElseThrow(),"Stone")==GTValues.U*9,"Stone block nine units");
        h.assertTrue(units(ItemMaterialRegistry.get(new ItemStack(Items.RAW_IRON_BLOCK)).orElseThrow(),"Iron")==GTValues.U*18,"Raw block nine two-unit raw ores");
        h.succeed();
    }
    @GameTest(template="test_empty") public static void containersNeverLoseStoredContentsToRecovery(GameTestHelper h) {
        var chest=new ItemStack(Items.CHEST);
        h.assertTrue(!com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput.parse(chest).isEmpty(),"Empty chest has recoverable wood");
        chest.getOrCreateTag().put("BlockEntityTag",new net.minecraft.nbt.CompoundTag());
        h.assertTrue(com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput.parse(chest).isEmpty(),"NBT container rejected by crucible");
        h.assertTrue(!ItemMaterialRegistry.canRecover(new ItemStack(Items.COD_BUCKET)),"Live fish and filled bucket are not bulk ingredients");
        h.succeed();
    }
    @GameTest(template="test_empty") public static void shredderOutputsNeverExceedDeclaredMaterial(GameTestHelper h) {
        var recipes=com.gregtech.gregtech.content.recipe.VanillaRecoveryRecipes.recipes();
        h.assertTrue(recipes.size()>100,"Broad vanilla recovery coverage");
        for(var r:recipes){
            h.assertTrue(com.gregtech.gregtech.api.recipe.RecipeInputs.consume(r,Arrays.asList(r.mInputs),List.of(),1)!=null,"Fresh recipe executes");
            var available=new HashMap<GTMaterial,Long>();
            for(var c:ItemMaterialRegistry.get(r.mInputs[0]).orElseThrow().components()){
                var target=c.material().getTargetPulverMaterial();
                long amount=java.math.BigInteger.valueOf(c.amount()).multiply(java.math.BigInteger.valueOf(c.material().getTargetPulverAmount())).divide(java.math.BigInteger.valueOf(GTValues.U)).longValueExact();
                available.merge(target,amount,Long::sum);
            }
            for(var out:r.mOutputs){
                var form=com.gregtech.gregtech.api.material.MaterialEquivalence.form(out);
                h.assertTrue(form!=null,"Output retains a known material composition, including vanilla targets");
                long remaining=available.getOrDefault(form.material(),0L)-form.prefix().getMaterialWeight()*out.getCount();
                h.assertTrue(remaining>=0,"Recovery conserves each material");available.put(form.material(),remaining);
            }
            var unsafe=r.mInputs[0].copy();unsafe.getOrCreateTag().put("BlockEntityTag",new net.minecraft.nbt.CompoundTag());
            h.assertTrue(com.gregtech.gregtech.api.recipe.RecipeInputs.consume(r,List.of(unsafe),List.of(),1)==null,"No NBT inventory shredded");
            if(r.mInputs[0].isDamageableItem()){
                var worn=r.mInputs[0].copy();worn.setDamageValue(1);
                h.assertTrue(com.gregtech.gregtech.api.recipe.RecipeInputs.consume(r,List.of(worn),List.of(),1)==null,"Fixed-output recipe rejects worn tools; crucible handles scaled recovery");
            }
        }
        h.succeed();
    }
    @GameTest(template="test_empty") public static void vehiclePackagingRetainsBothParts(GameTestHelper h) {
        var recipes=com.gregtech.gregtech.content.recipe.VehiclePackagingRecipes.recipes();
        h.assertTrue(recipes.size()==26,"Four minecarts and nine modern chest boats/rafts, two directions");
        for(int i=0;i<recipes.size();i+=2){
            var box=recipes.get(i);var unbox=recipes.get(i+1);
            h.assertTrue(com.gregtech.gregtech.api.recipe.RecipeInputs.consume(box,Arrays.asList(box.mInputs),List.of(),1)!=null,"Box executes");
            h.assertTrue(com.gregtech.gregtech.api.recipe.RecipeInputs.consume(unbox,Arrays.asList(box.mOutputs),List.of(),1)!=null,"Unbox executes");
            h.assertTrue(ItemStack.isSameItemSameTags(unbox.mOutputs[0],box.mInputs[0])&&ItemStack.isSameItemSameTags(unbox.mOutputs[1],box.mInputs[1]),"Cargo and shell return unchanged");
            var stored=unbox.mInputs[0].copy();stored.getOrCreateTag().put("EntityTag",new net.minecraft.nbt.CompoundTag());
            h.assertTrue(com.gregtech.gregtech.api.recipe.RecipeInputs.consume(unbox,List.of(stored),List.of(),1)==null,"Entity inventory is not erased");
        }
        h.succeed();
    }
    private static long units(com.gregtech.gregtech.api.material.ItemComposition data,String material){
        return data.components().stream().filter(c->c.material().getName().equals(material)).mapToLong(MaterialComponent::amount).sum();
    }

}
