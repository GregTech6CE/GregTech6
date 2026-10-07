package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.content.recipe.*;
import com.gregtech.gregtech.content.transport.*;
import com.gregtech.gregtech.data.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.*;
@GameTestHolder("gregtech_panel_materials") @PrefixGameTestTemplate(false)
public final class PanelMaterialPortTests {
 private static ItemStack item(String id){var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));if(stack.isEmpty())throw new IllegalStateException("Missing material input "+id);return stack;}
 private static Map<GTMaterial,Long> units(ItemComposition data){var map=new LinkedHashMap<GTMaterial,Long>();for(var c:data.components())map.merge(c.material(),c.amount(),Math::addExact);return map;}
 @GameTest(template="test_empty",timeoutTicks=60)
 public static void originalPanelMaterialAmountsReachCrucibleAndRespectExistingBindings(GameTestHelper h){
  for(String id:List.of("concrete","asphalt","cfoam","cfoam_fresh"))h.assertTrue(ItemMaterialRegistry.get(item("gregtech:"+id)).orElseThrow().amount()==GTValues.U,"source full construction block 1U "+id);
  h.assertTrue(ItemMaterialRegistry.get(item("gregtech:cfoam_slab")).orElseThrow().amount()==GTValues.U/2,"source half slab material");
  var reinforced=units(ItemMaterialRegistry.get(item("gregtech:concrete_reinforced")).orElseThrow());
  h.assertTrue(reinforced.get(GTMaterialRegistry.get("Concrete"))==GTValues.U&&reinforced.get(GTMaterialRegistry.get("Iron"))==GTValues.U/2,"source reinforcement is one rod");
  for(var spec:PanelCatalog.ALL){
   var stack=item("gregtech:"+spec.id());var data=ItemMaterialRegistry.get(stack).orElseThrow();
   var expected=new LinkedHashMap<GTMaterial,Long>();
   for(var c:ItemMaterialRegistry.get(item(spec.input())).orElseThrow().components())expected.merge(c.material(),c.amount()/6,Math::addExact);
   expected.merge(GTMaterialRegistry.get("Iron"),GTValues.U/9,Math::addExact);
   h.assertTrue(units(data).equals(expected),"CR.REV source material split "+spec.id()+" "+data);
   h.assertTrue(data.components().size()==2&&data.amount()==GTValues.U/6,"largest source component stays primary "+spec.id());
   var payload=com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput.parse(stack);
   h.assertTrue(payload.size()==2,"both panel components enter crucible "+spec.id());
   for(var c:data.components())h.assertTrue(payload.stream().anyMatch(p->p.material==c.material()&&p.amount==c.amount()),"crucible uses precise per-item material");
   PanelMaterialRegistration.register();h.assertTrue(ItemMaterialRegistry.get(stack).orElseThrow()==data,"original addItemData preserves existing binding");
  }
  var merged=ReversibleCraftingData.perItem(Arrays.asList(MaterialComponent.of(MaterialGroups.Iron,3),MaterialComponent.of(GTMaterialRegistry.get("Iron"),3),null,MaterialComponent.of(MaterialSentinels.Invalid,999)),6,"floor boundary");
  h.assertTrue(merged.components().size()==1&&merged.amount()==1,"merge reversing group before flooring; unknown tools/input contribute nothing");
  boolean rejected=false;try{ReversibleCraftingData.perItem(List.of(),0,"invalid");}catch(IllegalArgumentException e){rejected=true;}h.assertTrue(rejected,"zero output cannot divide materials");h.succeed();
 }
 @GameTest(template="test_empty",timeoutTicks=60)
 public static void everyPanelShredderEntryConsumesSafelyAndNeverManufacturesMaterial(GameTestHelper h){
  for(var spec:PanelCatalog.ALL){var stack=item("gregtech:"+spec.id());
   var recipe=VanillaRecoveryRecipes.recipes().stream().filter(r->r.mInputs[0].is(stack.getItem())).findFirst().orElseThrow(()->new IllegalStateException("Missing shredder entry "+spec.id()));
   h.assertTrue(recipe.mEUt==16&&recipe.mMaterialRecovery&&RecipeInputs.consume(recipe,List.of(stack),List.of(),1)!=null,"registered source recovery executes "+spec.id());
   var available=new HashMap<GTMaterial,Long>();
   for(var c:ItemMaterialRegistry.get(stack).orElseThrow().components())available.merge(c.material().getTargetPulverMaterial(),MaterialRecoveryRules.pulverizedAmount(c.material(),c.amount()),Math::addExact);
   h.assertTrue(recipe.mOutputs.length==2,"construction/wood plus screw metal output "+spec.id());
   for(var output:recipe.mOutputs){var form=MaterialEquivalence.form(output);h.assertTrue(form!=null&&!output.isEmpty(),"actual dust output identity");long amount=form.prefix().getMaterialWeight()*output.getCount();long remainder=available.getOrDefault(form.material(),0L)-amount;h.assertTrue(remainder>=0&&remainder<GTValues.U/72,"source dust flooring never creates or discards representable material "+spec.id());available.put(form.material(),remainder);}
   var unsafe=stack.copy();unsafe.getOrCreateTag().put("BlockEntityTag",new net.minecraft.nbt.CompoundTag());
   h.assertTrue(!ItemMaterialRegistry.canRecover(unsafe)&&RecipeInputs.consume(recipe,List.of(unsafe),List.of(),1)==null&&com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput.parse(unsafe).isEmpty(),"stored inventory cannot be destroyed by recovery");
  }h.succeed();
 }
}
