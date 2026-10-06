package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.content.recipe.*;
import com.gregtech.gregtech.content.transport.*;
import com.gregtech.gregtech.content.transport.fluid.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.recipe.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
@GameTestHolder("gregtech_transport_materials") @PrefixGameTestTemplate(false)
public final class TransportMaterialPortTests {
 private static ItemStack item(String id){var s=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));if(s.isEmpty())throw new IllegalStateException("Missing transport item "+id);return s;}
 private static Map<GTMaterial,Long> units(ItemComposition data){var result=new HashMap<GTMaterial,Long>();for(var c:data.components())result.merge(c.material(),c.amount(),Math::addExact);return result;}
 private static void checkCrucible(GameTestHelper h,ItemStack s){var data=ItemMaterialRegistry.get(s).orElseThrow();var payload=com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput.parse(s);h.assertTrue(payload.size()==data.components().size(),"native crucible receives every component "+s);for(var c:data.components())h.assertTrue(payload.stream().anyMatch(p->p.material==c.material()&&p.amount==c.amount()),"exact crucible input "+s);}
 private static void stored(ItemStack s){s.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(new net.minecraft.nbt.CompoundTag()));}
 private static CraftingInput grid(List<ItemStack> stacks){return CraftingInput.of(3,3,stacks);}
 private static Recipe<CraftingInput> row(GameTestHelper h,String path){return (Recipe<CraftingInput>)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("gregtech:hand/"+path)).orElseThrow(()->new IllegalStateException("Missing native recipe "+path)).value();}
 private static Recipe<CraftingInput> wire(GameTestHelper h,Recipe<CraftingInput> r){var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());try{if(r instanceof ToolShapedRecipe x){ToolShapedRecipe.SERIALIZER.streamCodec().encode(b,x);return ToolShapedRecipe.SERIALIZER.streamCodec().decode(b);}var x=(ToolShapelessRecipe)r;ToolShapelessRecipe.SERIALIZER.streamCodec().encode(b,x);return ToolShapelessRecipe.SERIALIZER.streamCodec().decode(b);}finally{b.release();}}
 private static List<ItemStack> inputs(TransportCraftingCatalog.Row row){var list=new ArrayList<ItemStack>();for(int i=0;i<9;i++)list.add(ItemStack.EMPTY);if(row.unpack())list.set(0,item(row.key().get('P').name()));else for(int y=0;y<row.pattern().size();y++)for(int x=0;x<row.pattern().get(y).length();x++){char c=row.pattern().get(y).charAt(x);if(c==' ')continue;var ingredient=TransportCraftingInputs.resolve(row.key().get(c));var values=ingredient.getItems();if(values.length==0)throw new IllegalStateException("Empty source ingredient "+row.path()+" "+c+" "+row.key().get(c));var value=values[0].copyWithCount(1);if(value.getItem() instanceof GTToolItem t)value=GTToolItem.create(t.toolType(),com.gregtech.gregtech.content.material.Materials.Steel,GTMaterialRegistry.get("Wood"));list.set(x+3*y,value);}return list;}
 @GameTest(template="test_empty",timeoutTicks=100)
 public static void allSourceTransportAmountsReachNativeCrucible(GameTestHelper h){
  long u=GTValues.U;long[] weights={u/2,u,u*3,u*6,u*12,u*12,u*9};
  for(var spec:FluidTransportDefinitions.pipes()){var s=item("gregtech:"+spec.id());var data=ItemMaterialRegistry.get(s).orElseThrow();h.assertTrue(data.components().size()==1&&data.material()==spec.material().resolve()&&data.amount()==weights[spec.size().ordinal()],"original OP fluid pipe weight "+spec.id());h.assertTrue(MaterialEquivalence.form(s)!=null,"native pipe prefix association "+spec.id());checkCrucible(h,s);}
  for(var mat:ItemPipeCatalog.ITEM_PIPE_MATS)for(var size:ItemPipeSpec.ItemPipeSize.values()){var s=item("gregtech:item_pipe_"+size.name().toLowerCase(Locale.ROOT)+"_"+mat.idSuffix());var data=ItemMaterialRegistry.get(s).orElseThrow();h.assertTrue(data.components().size()==1&&data.amount()==u*(size.ordinal()%3==0?3:size.ordinal()%3==1?6:12),"source restrictive association overrides REV rings "+s);checkCrucible(h,s);}
  for(var spec:FluidTransportDefinitions.tanks()){if(spec.id().equals("wood_barrel"))continue;var s=item("gregtech:"+spec.id());var data=ItemMaterialRegistry.get(s).orElseThrow();if(spec.type()==TankSpec.TankType.METAL_DRUM)h.assertTrue(data.components().size()==1&&data.amount()==u*6,"four curved plates plus two long rods "+spec.id());else if(spec.type()==TankSpec.TankType.PLASTIC_CANISTER)h.assertTrue(data.amount()==u*3,"original explicit PlasticCan 3U");else h.assertTrue(units(data).equals(Map.of(spec.material().resolve(),u*4,GTMaterialRegistry.get("Iron"),u*2)),"wood barrel plus ANY.Iron/MagicIron output rods "+spec.id());checkCrucible(h,s);}
  h.assertTrue(!com.gregtech.gregtech.api.material.MaterialItemDefinitions.candidatePrefixes().contains(MaterialPrefix.pipeMedium),"pipe metadata creates no ghost standalone item forms");h.succeed();
 }
 @GameTest(template="test_empty",timeoutTicks=140)
 public static void nativeTransportCraftingUsesSourceCountsToolsAndPayloadGuards(GameTestHelper h){
  int restrictive=0,bundles=0;for(var spec:TransportCraftingCatalog.rows()){var recipe=row(h,spec.path());var stacks=inputs(spec);var g=grid(stacks);h.assertTrue(recipe.matches(g,h.getLevel()),"actual native recipe matches source grid "+spec.path());var result=recipe.assemble(g,h.getLevel().registryAccess());h.assertTrue(result.is(item(spec.output()).getItem())&&result.getCount()==spec.count(),"original crafted identity/count "+spec.path());var remaining=recipe.getRemainingItems(g);for(int i=0;i<9;i++)if(stacks.get(i).getItem() instanceof GTToolItem t)h.assertTrue(remaining.get(i).is(t)&&remaining.get(i).getDamageValue()==t.toolType().damagePerCraft(),"original tool wear "+spec.path());
   var decoded=wire(h,recipe);h.assertTrue(decoded.matches(g,h.getLevel()),"real recipe synchronization "+spec.path());
   if(spec.empty()){int slot=spec.unpack()?0:spec.pattern().get(0).indexOf('P');if(slot<0)slot=4;var unsafe=stacks.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));stored(unsafe.get(slot));h.assertTrue(!recipe.matches(grid(unsafe),h.getLevel())&&!decoded.matches(grid(unsafe),h.getLevel()),"stored pipe contents protected before and after synchronization "+spec.path());}
   if(spec.path().startsWith("pipe/restrictive"))restrictive++;if(spec.path().startsWith("pipe/unpack"))bundles++;
  }h.assertTrue(restrictive==63&&bundles==80,"all 21 restrictive families and two unpack sizes for each of 40 materials");
  var restrictiveRow=row(h,"pipe/restrictive/item_pipe_restrictive_medium_brass");var bad=inputs(TransportCraftingCatalog.rows().stream().filter(x->x.path().equals("pipe/restrictive/item_pipe_restrictive_medium_brass")).findFirst().orElseThrow());bad.set(4,item("gregtech:item_pipe_large_brass"));h.assertTrue(!restrictiveRow.matches(grid(bad),h.getLevel()),"different diameter does not substitute");h.succeed();
 }
 @GameTest(template="test_empty",timeoutTicks=100)
 public static void nativeTransportRecoveryConservesMaterialsAndRejectsStoredContents(GameTestHelper h){
  for(var item:TransportMaterialRegistration.recoveryItems()){var s=new ItemStack(item);var recipe=VanillaRecoveryRecipes.recipes().stream().filter(x->x.mInputs[0].is(item)).findFirst().orElseThrow(()->new IllegalStateException("Missing native recovery "+s));h.assertTrue(RecipeInputs.consume(recipe,List.of(s),List.of(),1)!=null,"actual native recovery consumes one "+s);var available=new HashMap<GTMaterial,Long>();for(var c:ItemMaterialRegistry.get(s).orElseThrow().components())available.merge(c.material().getTargetPulverMaterial().resolve(),MaterialRecoveryRules.pulverizedAmount(c.material(),c.amount()),Math::addExact);for(var out:recipe.mOutputs){var form=MaterialEquivalence.form(out);h.assertTrue(form!=null,"actual dust identity "+out);long remainder=available.getOrDefault(form.material(),0L)-form.prefix().getMaterialWeight()*out.getCount();h.assertTrue(remainder>=0&&remainder<GTValues.U/72,"no extra or representably lost material "+s);available.put(form.material(),remainder);}var unsafe=s.copy();stored(unsafe);h.assertTrue(!ItemMaterialRegistry.canRecover(unsafe)&&RecipeInputs.consume(recipe,List.of(unsafe),List.of(),1)==null&&com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput.parse(unsafe).isEmpty(),"stored tank/pipe inventory survives recycling guard");}h.succeed();
 }
}
