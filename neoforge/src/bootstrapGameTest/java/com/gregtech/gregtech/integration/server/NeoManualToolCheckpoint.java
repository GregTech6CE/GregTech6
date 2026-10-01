package com.gregtech.gregtech.integration.server;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.recipe.GTToolRecipeSerializers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.util.*;

/** Opt-in grouped native tool crafting/component/network checkpoint; excluded from production jars. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,value=Dist.DEDICATED_SERVER)
public final class NeoManualToolCheckpoint {
 private NeoManualToolCheckpoint(){}
 private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Manual tool checkpoint: "+message);}
 private static CraftingRecipe recipe(ServerLevel level,String id){var holder=level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("gregtech",id)).orElseThrow(()->new IllegalStateException("Missing actual datapack recipe "+id));require(holder.value() instanceof CraftingRecipe,"recipe kind "+id);return (CraftingRecipe)holder.value();}
 private static ItemStack form(MaterialPrefix prefix){var stack=GTItems.getStack(prefix,Materials.Bronze,1);require(!stack.isEmpty(),"missing Bronze form "+prefix);return stack;}
 private static ItemStack tool(GTToolType type){var stack=GTToolItem.create(type,Materials.Bronze,WoodMaterials.Wood);require(GTToolHelper.isUsable(stack),"usable assembled "+type);return stack;}
 @SuppressWarnings({"unchecked","rawtypes"})
 private static CraftingRecipe network(CraftingRecipe recipe,ServerLevel level){
  RecipeSerializer serializer=recipe.getSerializer();var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
  try{serializer.streamCodec().encode(buffer,recipe);var result=(CraftingRecipe)serializer.streamCodec().decode(buffer);require(buffer.readableBytes()==0,"packet fully consumed");return result;}finally{buffer.release();}
 }
 @SubscribeEvent public static void started(ServerStartedEvent event){
  if(!Boolean.getBoolean("gregtech.integration.manualToolSmoke") && !Boolean.getBoolean("gregtech.integration.equipmentCraftingSmoke"))return;
  ServerLevel level=event.getServer().overworld();
  // The original metal wrench row: plate/hammer/plate, plate beneath, plate beneath.
  ItemStack hammer=tool(GTToolType.HARD_HAMMER);int before=hammer.getDamageValue();
  var wrenchInput=CraftingInput.of(3,3,List.of(form(MaterialPrefix.plate),hammer,form(MaterialPrefix.plate),ItemStack.EMPTY,form(MaterialPrefix.plate),ItemStack.EMPTY,ItemStack.EMPTY,form(MaterialPrefix.plate),ItemStack.EMPTY));
  var wrench=recipe(level,"tools/wrench");require(wrench.matches(wrenchInput,level),"wrench original shape");
  ItemStack result=wrench.assemble(wrenchInput,level.registryAccess());require(GTToolHelper.getType(result)==GTToolType.WRENCH&&GTToolHelper.getHead(result)==Materials.Bronze,"wrench output material");
  var remains=wrench.getRemainingItems(wrenchInput);require(remains.get(1).getItem()==hammer.getItem()&&remains.get(1).getDamageValue()==before+400,"hammer returned with original wear");require(hammer.getDamageValue()==before,"input tool not mutated by remainder query");
  var wrong=new ArrayList<>(wrenchInput.items());Collections.swap(wrong,1,4);require(!wrench.matches(CraftingInput.of(3,3,wrong),level),"wrong tool position rejected");
  require(network(wrench,level).matches(wrenchInput,level),"crafting serializer network round trip");
  var assemblyInput=CraftingInput.of(2,1,List.of(form(MaterialPrefix.toolHeadPickaxe),GTItems.getStack(MaterialPrefix.stick,WoodMaterials.Wood,1)));
  var assembly=recipe(level,"tools/pickaxe_assembly");require(assembly.matches(assemblyInput,level),"real head plus wood handle");
  ItemStack pick=assembly.assemble(assemblyInput,level.registryAccess());require(GTToolHelper.getType(pick)==GTToolType.PICKAXE&&GTToolHelper.getHandle(pick)==WoodMaterials.Wood&&pick.has(DataComponents.MAX_DAMAGE),"pick type/handle/native durability");
  var roundTrip=ItemStack.parseOptional(level.registryAccess(),(net.minecraft.nbt.CompoundTag)pick.saveOptional(level.registryAccess()));require(GTToolHelper.isUsable(roundTrip)&&GTToolHelper.getHead(roundTrip)==Materials.Bronze&&roundTrip.getMaxDamage()==pick.getMaxDamage(),"actual stack component save parse");
  require(network(assembly,level).matches(assemblyInput,level),"assembly serializer network round trip");
  var headInput=CraftingInput.of(3,2,List.of(form(MaterialPrefix.plate),form(MaterialPrefix.ingot),form(MaterialPrefix.ingot),tool(GTToolType.FILE),ItemStack.EMPTY,tool(GTToolType.HARD_HAMMER)));
  var head=recipe(level,"tool_heads/pickaxe");require(head.matches(headInput,level),"original metal pick head shape");
  require(ItemStack.isSameItemSameComponents(head.assemble(headInput,level.registryAccess()),form(MaterialPrefix.toolHeadPickaxe)),"head output material and form");require(network(head,level).matches(headInput,level),"head serializer network round trip");
  if(Boolean.getBoolean("gregtech.integration.equipmentCraftingSmoke")) equipment(level);
  com.mojang.logging.LogUtils.getLogger().info("MANUAL_TOOL_CHECKPOINT_SUCCESS {}","{\"shapedWrench\":true,\"rejectWrongShape\":true,\"hammerWear\":400,\"headHandleAssembly\":true,\"headCrafting\":true,\"nativeStackComponentRoundTrip\":true,\"threeSerializerNetworkRoundTrips\":true}");
 }
 private static ItemStack equipmentItem(String path) {
  var id=ResourceLocation.fromNamespaceAndPath("gregtech",path);
  require(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id),"registered equipment/input "+id);
  return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id));
 }
 private static ItemStack equipmentForm(MaterialPrefix prefix,com.gregtech.gregtech.api.material.GTMaterial material) {
  var result=GTItems.getStack(prefix,material,1);require(!result.isEmpty(),"equipment form "+prefix+" / "+material);return result;
 }
 public static Map<String,ItemStack> steamEngineCrafting(ServerLevel level) {
  int checked=0;
  Map<String,ItemStack> selected=new HashMap<>();
  for(var entry:com.gregtech.gregtech.content.energy.EngineCatalog.all()) {
   boolean strong=entry.spec() instanceof com.gregtech.gregtech.api.machine.StrongSteamEngineSpec;
   String id;com.gregtech.gregtech.api.material.GTMaterial material;
   if(strong) {var spec=(com.gregtech.gregtech.api.machine.StrongSteamEngineSpec)entry.spec();id=spec.id();material=spec.material();}
   else if(entry.spec() instanceof com.gregtech.gregtech.api.machine.SteamEngineSpec spec) {id=spec.id();material=spec.material();}
   else continue;
   var plate=equipmentForm(strong?MaterialPrefix.plateDense:MaterialPrefix.plateDouble,material);
   var rod=equipmentForm(MaterialPrefix.stick,material);
   var spring=equipmentForm(strong?MaterialPrefix.spring:MaterialPrefix.springSmall,material);
   var hammer=tool(GTToolType.HARD_HAMMER);var wrench=tool(GTToolType.WRENCH);
   var input=CraftingInput.of(3,3,List.of(plate.copy(),hammer,plate.copy(),rod.copy(),spring,rod.copy(),plate.copy(),wrench,plate.copy()));
   var craftRecipe=recipe(level,"engines/"+id);
   for(var ingredient:craftRecipe.getIngredients())
    if(ingredient!=Ingredient.EMPTY)require(ingredient.getItems().length>0,"engine ingredient closure "+id);
   ItemStack result=craftRecipe.assemble(input,level.registryAccess());
   require(craftRecipe.matches(input,level) && ItemStack.isSameItemSameComponents(result,equipmentItem(id)),"original steam engine pattern/output "+id);
   var remains=craftRecipe.getRemainingItems(input);
   require(remains.get(1).getDamageValue()==400 && remains.get(7).getDamageValue()==800
       && hammer.getDamageValue()==0 && wrench.getDamageValue()==0,"steam engine original wear/no input mutation "+id);
   for(int slot:List.of(0,2,3,4,5,6,8))require(remains.get(slot).isEmpty(),"engine material slots consumed "+id);
   var bad=new ArrayList<>(input.items());bad.set(4,equipmentForm(strong?MaterialPrefix.springSmall:MaterialPrefix.spring,material));
   require(!craftRecipe.matches(CraftingInput.of(3,3,bad),level),"regular/strong spring distinction "+id);
   bad=new ArrayList<>(input.items());bad.set(0,equipmentForm(MaterialPrefix.plate,material));
   require(!craftRecipe.matches(CraftingInput.of(3,3,bad),level),"single plate cannot replace double/dense "+id);
   if(material.resolve()==Materials.Bronze.resolve()) {
    require(network(craftRecipe,level).matches(input,level),"steam engine native packet "+id);
    var wrong=new ArrayList<>(input.items());wrong.set(0,equipmentForm(strong?MaterialPrefix.plateDense:MaterialPrefix.plateDouble,Materials.Copper));
    require(!craftRecipe.matches(CraftingInput.of(3,3,wrong),level),"wrong material rejected "+id);
    selected.put(id,result);
   }
   checked++;
  }
  require(checked==28 && selected.size()==2,"all retained regular/strong steam engine crafting rows");
  com.mojang.logging.LogUtils.getLogger().info("STEAM_ENGINE_CRAFTING_CHECKPOINT_SUCCESS {}","{\"platform\":\"neoforge\",\"recipes\":28,\"normalRows\":14,\"strongRows\":14,\"patternAndOutput\":true,\"prefixDistinction\":true,\"hammerWear\":400,\"wrenchWear\":800,\"selectedNativePackets\":2,\"playerCraftingClickVerified\":false}");
  return Map.copyOf(selected);
 }
 private static void equipment(ServerLevel level) {
  int loaded=0;
  for(String file:com.gregtech.gregtech.content.recipe.EquipmentCraftingCatalog.FILES) {
   String id=file.substring(0,file.length()-5);
   var holder=level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("gregtech",id))
       .orElseThrow(()->new IllegalStateException("Missing retained equipment recipe "+id));
   require(!holder.value().getResultItem(level.registryAccess()).isEmpty(),"nonempty equipment result "+id);
   for(var ingredient:holder.value().getIngredients())
    if(ingredient!=Ingredient.EMPTY)require(ingredient.getItems().length>0,"resolved equipment ingredient/tag "+id);
   loaded++;
  }
  ItemStack hammer=tool(GTToolType.HARD_HAMMER), wrench=tool(GTToolType.WRENCH);
  ItemStack dense=equipmentForm(MaterialPrefix.plateDense,Materials.Bronze);
  var boilerInput=CraftingInput.of(3,3,List.of(ItemStack.EMPTY,dense.copy(),ItemStack.EMPTY,
      dense.copy(),wrench,dense.copy(),dense.copy(),hammer,dense.copy()));
  var boiler=recipe(level,"smeltery_survival/strong_steam_boiler_bronze");
  require(boiler.matches(boilerInput,level),"actual dense bronze and tools craft strong boiler");
  var boilerResult=boiler.assemble(boilerInput,level.registryAccess());
  require(ItemStack.isSameItemSameComponents(boilerResult,equipmentItem("strong_steam_boiler_bronze")),"strong boiler output");
  var boilerRemains=boiler.getRemainingItems(boilerInput);
  require(boilerRemains.get(4).getDamageValue()==800 && boilerRemains.get(7).getDamageValue()==400,
      "retained wrench 800 / hammer 400 crafting wear");
  require(wrench.getDamageValue()==0 && hammer.getDamageValue()==0,"remaining-items query does not mutate source tools");
  require(network(boiler,level).matches(boilerInput,level),"equipment recipe native packet round trip");
  var burnerInput=CraftingInput.of(3,3,List.of(equipmentForm(MaterialPrefix.plateQuintuple,Materials.Bronze),
      equipmentForm(MaterialPrefix.plateDense,Materials.Copper),equipmentForm(MaterialPrefix.plateQuintuple,Materials.Bronze),
      equipmentForm(MaterialPrefix.plateQuintuple,Materials.Bronze),tool(GTToolType.WRENCH),equipmentForm(MaterialPrefix.plateQuintuple,Materials.Bronze),
      new ItemStack(Items.BRICKS),new ItemStack(Items.BRICKS),new ItemStack(Items.BRICKS)));
  var burner=recipe(level,"smeltery_survival/burning_box_solid_dense_bronze");
  require(burner.matches(burnerInput,level) && ItemStack.isSameItemSameComponents(burner.assemble(burnerInput,level.registryAccess()),
      equipmentItem("burning_box_solid_dense_bronze")),"actual dense burner original pattern");
  var plate=equipmentForm(MaterialPrefix.plateDouble,Materials.Steel);
  var wire=equipmentItem("wire_01_annealed_copper");
  var transformerInput=CraftingInput.of(3,3,List.of(wire.copy(),plate.copy(),wire.copy(),
      equipmentItem("wire_04_annealed_copper"),equipmentItem("casing_machine_tinalloy"),tool(GTToolType.WIRE_CUTTER),
      wire.copy(),plate.copy(),wire.copy()));
  var transformer=recipe(level,"energy_nodes/transformer_ulv_lv");
  require(transformer.matches(transformerInput,level),"ANY copper and steel double-plate families craft transformer");
  require(ItemStack.isSameItemSameComponents(transformer.assemble(transformerInput,level.registryAccess()),
      equipmentItem("transformer_ulv_lv")),"actual transformer output");
  var mirrored=new ArrayList<>(transformerInput.items());Collections.swap(mirrored,3,5);
  require(!transformer.matches(CraftingInput.of(3,3,mirrored),level),"original asymmetric no-mirror flag retained");
  var clayInput=CraftingInput.of(3,3,List.of(new ItemStack(Items.CLAY_BALL),tool(GTToolType.KNIFE),new ItemStack(Items.CLAY_BALL),
      new ItemStack(Items.CLAY_BALL),tool(GTToolType.ROLLING_PIN),new ItemStack(Items.CLAY_BALL),
      new ItemStack(Items.CLAY_BALL),new ItemStack(Items.CLAY_BALL),new ItemStack(Items.CLAY_BALL)));
  var clay=recipe(level,"smeltery_survival/clay_crucible");
  require(clay.matches(clayInput,level),"clay balls, knife and rolling pin craft unfired crucible");
  ItemStack unfired=clay.assemble(clayInput,level.registryAccess());
  require(ItemStack.isSameItemSameComponents(unfired,equipmentItem("clay_crucible")),"clay crucible result");
  var fired=level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("gregtech","smeltery_survival/ceramic_crucible_firing")).orElseThrow();
  require(fired.value() instanceof SmeltingRecipe,"actual native firing recipe kind");
  var firing=(SmeltingRecipe)fired.value();
  require(firing.matches(new SingleRecipeInput(unfired),level) && ItemStack.isSameItemSameComponents(
      firing.assemble(new SingleRecipeInput(unfired),level.registryAccess()),equipmentItem("smelting_crucible_ceramic")),
      "unfired crucible enters original furnace recipe and produces ceramic equipment");
  var reclaim=recipe(level,"smeltery_survival/clay_crucible_reclaim");
  var reclaimInput=CraftingInput.of(1,1,List.of(unfired));
  require(reclaim.matches(reclaimInput,level) && reclaim.assemble(reclaimInput,level.registryAccess()).is(Items.CLAY_BALL)
      && reclaim.assemble(reclaimInput,level.registryAccess()).getCount()==7,"original seven-clay reclaim count");
  routingCrafting(level);
  manufacturingCrafting(level);
  survivalCrafting(level);
  com.mojang.logging.LogUtils.getLogger().info("EQUIPMENT_CRAFTING_CHECKPOINT_SUCCESS loaded={} resolvedIngredients=true boiler=true burner=true transformer=true mirrorRejected=true clayFiringRecipe=true reclaimCount=7 wrenchWear=800 hammerWear=400",loaded);
 }

 private static ItemStack craft(ServerLevel level,String id,CraftingInput input,String output) {
  var actual=recipe(level,id);require(actual.matches(input,level),"routing crafting pattern "+id);
  var stack=actual.assemble(input,level.registryAccess());
  require(ItemStack.isSameItemSameComponents(stack,equipmentItem(output)),"routing crafting result "+id);
  return stack;
 }
 private static void routingCrafting(ServerLevel level) {
  ItemStack saw=tool(GTToolType.SAW),mallet=tool(GTToolType.SOFT_HAMMER);
  var woodInput=CraftingInput.of(3,3,List.of(ItemStack.EMPTY,ItemStack.EMPTY,saw,
      new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.OAK_PLANKS),
      mallet,ItemStack.EMPTY,ItemStack.EMPTY));
  craft(level,"pipes/pipe_medium_wood",woodInput,"pipe_medium_wood");
  var woodRemains=recipe(level,"pipes/pipe_medium_wood").getRemainingItems(woodInput);
  require(woodRemains.get(2).getDamageValue()==100 && woodRemains.get(6).getDamageValue()==100,
      "wood pipe saw and soft hammer original wear");
  require(saw.getDamageValue()==0 && mallet.getDamageValue()==0,"wood pipe remainder query preserves inputs");
  var blankInput=CraftingInput.of(2,2,List.of(equipmentForm(MaterialPrefix.screw,Materials.Aluminium),
      tool(GTToolType.HARD_HAMMER),equipmentForm(MaterialPrefix.plate,Materials.Aluminium),tool(GTToolType.SCREWDRIVER)));
  ItemStack blank=craft(level,"logistics/blank_cover",blankInput,"blank_cover");
  var blankRemains=recipe(level,"logistics/blank_cover").getRemainingItems(blankInput);
  require(blankRemains.get(1).getDamageValue()==400 && blankRemains.get(3).getDamageValue()==100,
      "blank cover hammer and screwdriver wear");
  var foil=equipmentForm(MaterialPrefix.foil,Materials.Zinc);
  var filterInput=CraftingInput.of(3,3,List.of(ItemStack.EMPTY,foil.copy(),ItemStack.EMPTY,
      foil.copy(),blank,foil.copy(),ItemStack.EMPTY,foil.copy(),ItemStack.EMPTY));
  ItemStack filter=craft(level,"logistics/item_filter",filterInput,"item_filter");
  var pipe=equipmentItem("item_pipe_medium_electrum");
  var machineInput=CraftingInput.of(3,3,List.of(ItemStack.EMPTY,tool(GTToolType.HARD_HAMMER),pipe.copy(),
      filter.copy(),equipmentItem("casing_machine_steelgalvanized"),filter.copy(),pipe.copy(),tool(GTToolType.WRENCH),ItemStack.EMPTY));
  ItemStack machine=craft(level,"logistics/filter_items",machineInput,"filter_items");
  machine.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("routing crafting reset specimen"));
  var resetInput=CraftingInput.of(1,1,List.of(machine));
  var reset=craft(level,"logistics/filter_items_reset",resetInput,"filter_items");
  require(!reset.has(DataComponents.CUSTOM_NAME) && machine.has(DataComponents.CUSTOM_NAME),
      "reset returns fresh output without mutating input");
  var copper=equipmentItem("wire_01_copper");
  var tin=equipmentItem("cable_01_tin");
  var motorInput=CraftingInput.of(3,3,List.of(tin.copy(),copper.copy(),equipmentForm(MaterialPrefix.stick,Materials.SteelGalvanized),
      copper.copy(),equipmentForm(MaterialPrefix.stick,Materials.IronMagnetic),copper.copy(),
      equipmentItem("plate_curved_steelgalvanized"),copper.copy(),tin.copy()));
  ItemStack motor=craft(level,"components/motor_lv",motorInput,"compact_electric_motor_lv");
  var rubber=equipmentForm(MaterialPrefix.plate,Materials.Rubber);
  var conveyorInput=CraftingInput.of(3,3,List.of(rubber.copy(),rubber.copy(),rubber.copy(),
      motor.copy(),tin.copy(),motor.copy(),rubber.copy(),rubber.copy(),rubber.copy()));
  craft(level,"components/conveyor_lv",conveyorInput,"compact_electric_conveyor_lv");
  require(network(recipe(level,"logistics/blank_cover"),level).matches(blankInput,level),"blank cover packet round trip");
  com.mojang.logging.LogUtils.getLogger().info("ROUTING_CRAFTING_CHECKPOINT_SUCCESS woodPipe=true sawWear=100 softHammerWear=100 blankCover=true filterCover=true itemFilterMachine=true resetFresh=true motor=true conveyor=true network=true");
 }
 private static com.gregtech.gregtech.api.energy.WireSpec wireSpec(ItemStack stack) {
  require(stack.getItem() instanceof net.minecraft.world.item.BlockItem,"wire recipe result/input is block item");
  var block=((net.minecraft.world.item.BlockItem)stack.getItem()).getBlock();
  require(block instanceof com.gregtech.gregtech.api.energy.WireMaterialLike,"wire recipe uses real conductor metadata");
  return ((com.gregtech.gregtech.api.energy.WireMaterialLike)block).spec();
 }
 private static void manufacturingCrafting(ServerLevel level) {
  int wires=0;
  for(String file:com.gregtech.gregtech.content.recipe.EquipmentCraftingCatalog.FILES) {
   if(!file.startsWith("wire_working/"))continue;
   String id=file.substring(0,file.length()-5);
   var actual=recipe(level,id);require(actual instanceof ShapelessRecipe,"retained shapeless wire recipe "+id);
   var inputs=new ArrayList<ItemStack>(Collections.nCopies(9,ItemStack.EMPTY));
   long inputAmount=0;com.gregtech.gregtech.api.energy.WireSpec family=null;int slot=0;
   for(var ingredient:actual.getIngredients()) {
    require(ingredient.getItems().length>0,"resolved wire ingredient "+id);
    var stack=ingredient.getItems()[0].copyWithCount(1);inputs.set(slot++,stack);
    if(stack.getItem() instanceof net.minecraft.world.item.BlockItem item
        && item.getBlock() instanceof com.gregtech.gregtech.api.energy.WireMaterialLike wire) {
     var spec=wire.spec();
     if(family!=null)require(family.id().equals(spec.id()) && family.material().resolve()==spec.material().resolve(),"single conductor family "+id);
     family=spec;inputAmount+=spec.materialAmount();
    }
   }
   var input=CraftingInput.of(3,3,inputs);require(actual.matches(input,level),"actual wire recipe matches "+id);
   var output=actual.assemble(input,level.registryAccess());var spec=wireSpec(output);
   require(family!=null && family.id().equals(spec.id()) && family.material().resolve()==spec.material().resolve(),"wire family preserved "+id);
   require(inputAmount==spec.materialAmount()*output.getCount(),"wire conductor amount conserved "+id);
   wires++;
  }
  require(wires==1316,"complete retained wire recipe family");
  var blankInput=CraftingInput.of(2,2,List.of(tool(GTToolType.HARD_HAMMER),tool(GTToolType.FILE),
      tool(GTToolType.WIRE_CUTTER),equipmentItem("plate_double_tungstencarbide")));
  ItemStack blank=craft(level,"extruder_shapes/extruder_shape_empty",blankInput,"extruder_shape_empty");
  var blankRemains=recipe(level,"extruder_shapes/extruder_shape_empty").getRemainingItems(blankInput);
  require(blankRemains.get(0).getDamageValue()==400 && blankRemains.get(1).getDamageValue()==100
      && blankRemains.get(2).getDamageValue()==400 && blankRemains.get(3).isEmpty(),"blank mold tools wear and plate consumed");
  ItemStack cutter=tool(GTToolType.WIRE_CUTTER);
  var rodInput=CraftingInput.of(2,1,List.of(blank,cutter));
  var rodRecipe=recipe(level,"extruder_shapes/extruder_shape_rod");
  ItemStack rod=craft(level,"extruder_shapes/extruder_shape_rod",rodInput,"extruder_shape_rod");
  var rodRemains=rodRecipe.getRemainingItems(rodInput);
  require(rodRemains.get(0).isEmpty() && rodRemains.get(1).getDamageValue()==400 && cutter.getDamageValue()==0,
      "blank consumed while usable cutter returned without input mutation");
  var mirrored=CraftingInput.of(2,1,List.of(cutter,blank));
  require(!rodRecipe.matches(mirrored,level) && !network(rodRecipe,level).matches(mirrored,level),"rod mold no-mirror survives native network");
  require(network(rodRecipe,level).matches(rodInput,level),"rod mold actual packet round trip");
  int collisions=0;
  for(String file:com.gregtech.gregtech.content.recipe.EquipmentCraftingCatalog.FILES)
   if(file.startsWith("extruder_shapes/") && recipe(level,file.substring(0,file.length()-5)).matches(rodInput,level))collisions++;
  require(collisions==1,"selected rod mold matches one original shape only");
  var wireInput=CraftingInput.of(1,2,List.of(tool(GTToolType.WIRE_CUTTER),rod));
  craft(level,"extruder_shapes/extruder_shape_wire",wireInput,"extruder_shape_wire");
  var wireRemains=recipe(level,"extruder_shapes/extruder_shape_wire").getRemainingItems(wireInput);
  require(wireRemains.get(0).getDamageValue()==400 && wireRemains.get(1).isEmpty(),"previous rod shape consumed when cutting wire shape");
  com.mojang.logging.LogUtils.getLogger().info("MANUFACTURING_CRAFTING_CHECKPOINT_SUCCESS wireRecipes={} conductorAmountConserved=true materialFamilyPreserved=true blankToRodToWire=true toolsWorn=true previousShapeConsumed=true rodMirrorRejected=true rodUniqueAmong78=true network=true",wires);
 }
 private static void survivalCrafting(ServerLevel level) {
  var gear=equipmentForm(MaterialPrefix.gearGtSmall,Materials.Iron);
  var rod=equipmentForm(MaterialPrefix.stick,Materials.Iron);
  var selectorInput=CraftingInput.of(3,3,List.of(gear.copy(),tool(GTToolType.HARD_HAMMER),gear.copy(),
      rod.copy(),rod.copy(),rod.copy(),gear.copy(),tool(GTToolType.WRENCH),gear.copy()));
  ItemStack selector=craft(level,"hand_components/integrated_circuit_0",selectorInput,"integrated_circuit_0");
  var screwdriver=tool(GTToolType.SCREWDRIVER);
  var configureInput=CraftingInput.of(2,2,List.of(screwdriver,ItemStack.EMPTY,ItemStack.EMPTY,selector));
  var configure=recipe(level,"hand_components/integrated_circuit_1");
  craft(level,"hand_components/integrated_circuit_1",configureInput,"integrated_circuit_1");
  var configureRemains=configure.getRemainingItems(configureInput);
  require(configureRemains.get(0).getDamageValue()==100 && configureRemains.get(3).isEmpty()
      && screwdriver.getDamageValue()==0,"selector consumed and screwdriver worn without input mutation");
  var mirrored=CraftingInput.of(2,2,List.of(ItemStack.EMPTY,screwdriver,selector,ItemStack.EMPTY));
  require(!configure.matches(mirrored,level) && !network(configure,level).matches(mirrored,level),"selector no-mirror retained in packet");
  var goldWire=equipmentItem("wire_01_gold");var goldCable=equipmentItem("cable_01_gold");
  var aluminium=equipmentForm(MaterialPrefix.plate,Materials.Aluminium);
  var screws=equipmentForm(MaterialPrefix.screw,Materials.Aluminium);
  var usbInput=CraftingInput.of(3,3,List.of(tool(GTToolType.WIRE_CUTTER),goldWire,tool(GTToolType.SCREWDRIVER),
      aluminium.copy(),goldCable.copy(),aluminium.copy(),screws.copy(),goldCable.copy(),screws.copy()));
  craft(level,"hand_components/usb1_cable",usbInput,"usb1_cable");
  var steelScrew=equipmentForm(MaterialPrefix.screw,Materials.Steel);
  var steelRod=equipmentForm(MaterialPrefix.stick,Materials.Steel);
  var scaffoldInput=CraftingInput.of(3,2,List.of(steelScrew.copy(),equipmentForm(MaterialPrefix.plate,Materials.Steel),
      steelScrew.copy(),steelRod.copy(),tool(GTToolType.SCREWDRIVER),steelRod.copy()));
  craft(level,"scaffolds/scaffold",scaffoldInput,"scaffold");
  var plank=new ItemStack(Items.OAK_PLANKS);
  var shelfInput=CraftingInput.of(3,3,List.of(plank.copy(),plank.copy(),plank.copy(),
      tool(GTToolType.SAW),tool(GTToolType.FILE),tool(GTToolType.SOFT_HAMMER),plank.copy(),plank.copy(),plank.copy()));
  craft(level,"bookshelves/bookshelf",shelfInput,"bookshelf");
  var baleInput=CraftingInput.of(3,3,new ArrayList<ItemStack>(Collections.nCopies(9,equipmentItem("barley"))));
  ItemStack bale=craft(level,"bales/bale_barley_pack",baleInput,"bale_barley");
  var unpackInput=CraftingInput.of(1,1,List.of(bale));
  require(craft(level,"bales/bale_barley_unpack",unpackInput,"barley").getCount()==9,"barley packing/unpacking count");
  var blueSpruce=equipmentItem("planks_bluespruce");
  for(var type:List.of(GTToolType.SAW,GTToolType.AXE,GTToolType.DOUBLE_AXE,GTToolType.UNIVERSAL_SPADE)) {
   var toolInput=CraftingInput.of(2,1,List.of(tool(type),blueSpruce.copy()));
   require(craft(level,"wood/slab_bluespruce",toolInput,"slab_bluespruce").getCount()==2,"original sawaxe tag accepts "+type);
  }
  var reversedWood=CraftingInput.of(2,1,List.of(blueSpruce.copy(),tool(GTToolType.AXE)));
  require(recipe(level,"wood/slab_bluespruce").matches(reversedWood,level),"wood recipe original mirror permission");
  var bowlHolder=level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("gregtech","tools/ceramic_bowl_firing")).orElseThrow();
  require(bowlHolder.value() instanceof SmeltingRecipe,"bowl original firing kind");
  var bowl=(SmeltingRecipe)bowlHolder.value();var bowlInput=new SingleRecipeInput(equipmentItem("clay_bowl"));
  require(bowl.matches(bowlInput,level) && ItemStack.isSameItemSameComponents(bowl.assemble(bowlInput,level.registryAccess()),
      equipmentItem("mixing_bowl")),"clay to ceramic bowl actual firing recipe query");
  com.mojang.logging.LogUtils.getLogger().info("SURVIVAL_CRAFTING_CHECKPOINT_SUCCESS selector=true selectorWear=100 selectorMirrorRejected=true usbCable=true scaffold=true bookshelf=true barleyRoundTrip=9 sawaxeChoices=4 woodMirrorAllowed=true bowlFiringQuery=true");
 }

}
