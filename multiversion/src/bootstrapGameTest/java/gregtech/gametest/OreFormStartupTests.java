package gregtech.gametest;
import gregtech.internal.recipe.Recipe;
import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.internal.material.*;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.registry.GTItems;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
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
/** Dedicated fresh-JVM startup profile, NOT mixed suite, real resource-reload or actual-stop proof. */
@GameTestHolder("gregtech_ore_form_startup_audit")
@PrefixGameTestTemplate(false)
public final class OreFormStartupTests {
 private static boolean started;
 @GameTest(template="test_empty",batch="ore_form_startup_isolated",timeoutTicks=400)
 public static void acceptedCoreFormAndPublicStartupWinnerSurviveOverlay(GameTestHelper h) throws Exception {
  String ns=System.getProperty("neoforge.enabledGameTestNamespaces",System.getProperty("forge.enabledGameTestNamespaces",""));
  h.assertTrue(ns.equals("gregtech_ore_form_startup_audit")&&!started,"exact isolated namespace, one fresh JVM case");started=true;
  var server=h.getLevel().getServer();
  String configured=System.getenv("GT6_NONORE_ORE_FORM_STARTUP_DIRECTORY");
  h.assertTrue(configured!=null&&!configured.isBlank(),"explicit disposable directory");
  Path directory=Path.of(configured).toRealPath(), world=server.getWorldPath(LevelResource.ROOT).toRealPath();
  h.assertTrue(directory.getFileName().toString().startsWith("gt6-ore-form-startup-")&&world.startsWith(directory)&&!world.equals(directory),"fresh disposable world descendant");
  h.assertTrue(server.getPlayerList().getPlayerCount()==0,"no players in diagnostic world");
  String nonce=System.getenv("GT6_NONORE_ORE_FORM_STARTUP_NONCE");
  h.assertTrue(nonce!=null&&nonce.matches("[0-9a-f]{32}"),"fresh nonce required");
  Path marker=world.resolve(".ore-form-startup-used");
  h.assertTrue(!Files.exists(marker),"never reuse fixture world");Files.writeString(marker,nonce,StandardOpenOption.CREATE_NEW);
  var packs=List.copyOf(server.getPackRepository().getSelectedIds());
  h.assertTrue(gregtech.internal.addon.AddonMaterials.acceptedForms().stream().filter(f->
    f.addonId().equals("addon_sample")&&f.materialName().equals("Dilithium")&&f.prefixName().equals("crushedPurifiedTiny")).count()==1,
    "public MaterialFormRequest accepted once during startup; no runtime policy mutation");
  h.assertTrue(GTMaterialRegistry.get("Dilithium").resolve().getId()==8317,"exact original core identity, not addon namesake");
  var addon=Class.forName("com.gregtech.addonsample.AddonSampleMod");
  h.assertTrue(addon.getField("ORE_FORM_STARTUP_CALLBACKS").getInt(null)==1
    &&addon.getField("ORE_FORM_STARTUP_WINNER_ACCEPTED").getBoolean(null),"public onRecipesReady real acceptance, not a replacement installed by test");
  Recipe winner=winner(h), continuation=continuation(h);
  h.assertTrue(!owned().contains(winner)&&owned().contains(continuation),"independent startup winner excluded from source ledger; source tiny accepted");
  String before=fingerprint(winner), tinyBefore=fingerprint(continuation);
  var raw=MachineRecipeMaps.Crusher.findRecipe(List.of(item("oreRaw","Cinnabar",1)),List.of(),false,1,16);
  h.assertTrue(raw!=null,"raw Cinnabar control exists");String rawBefore=fingerprint(raw);
  try {
   for(int round=0;round<2;round++) {
    gregtech.content.recipe.MachineRecipeOverlay.apply(server.getRecipeManager());
    h.assertTrue(winner(h)==winner&&!owned().contains(winner)&&fingerprint(winner).equals(before),"exact startup identity and complete row survive baseline/replay");
    h.assertTrue(fingerprint(continuation(h)).equals(tinyBefore),"explicit accepted form survives replay with all fields unchanged");
    h.assertTrue(MachineRecipeMaps.Crusher.findRecipe(List.of(item("oreRaw","Cinnabar",1)),List.of(),false,1,16)==raw
      &&fingerprint(raw).equals(rawBefore),"raw path untouched");
   }
  } finally {
   gregtech.content.recipe.MachineRecipeOverlay.apply(server.getRecipeManager());
   h.assertTrue(winner(h)==winner&&fingerprint(continuation(h)).equals(tinyBefore),"synchronous original startup state restored");
   h.assertTrue(List.copyOf(server.getPackRepository().getSelectedIds()).equals(packs),"no pack selection changes");
  }
  var receipt=new com.google.gson.JsonObject();receipt.addProperty("schema",1);receipt.addProperty("namespace",ns);receipt.addProperty("nonce",nonce);
  receipt.addProperty("world",world.toString());receipt.addProperty("acceptedCoreForm",true);receipt.addProperty("publicStartupWinner",true);
  receipt.addProperty("winnerIdentityPreserved",true);receipt.addProperty("rawControlPreserved",true);receipt.addProperty("restored",true);
  receipt.addProperty("probePassed",true);receipt.addProperty("verifiedRounds",2);
  receipt.addProperty("form","addon_sample:Dilithium/crushedPurifiedTiny");receipt.addProperty("winner","Dolamide/crushed/Water900");
  var originalPacks=new com.google.gson.JsonArray();for(String pack:packs)originalPacks.add(pack);receipt.add("originalPacks",originalPacks);
  var restoredPacks=new com.google.gson.JsonArray();for(String pack:server.getPackRepository().getSelectedIds())restoredPacks.add(pack);receipt.add("restoredPacks",restoredPacks);
  Path temporary=world.resolve(".ore-form-startup-cleanup.tmp"), result=world.resolve(".ore-form-startup-cleanup.json");
  Files.writeString(temporary,receipt.toString(),StandardOpenOption.CREATE_NEW);Files.move(temporary,result,StandardCopyOption.ATOMIC_MOVE);
  System.out.println("[ore-form-startup-cleanup] "+receipt);h.succeed();
 }
 private static ItemStack item(String prefix,String material,int count){return OreSourceDifferentialTests.s(prefix,material,count);}
 private static FluidStack fluid(String name,int amount){return gregtech.api.addon.platform.AddonFluids.still(name,amount);}
 private static Recipe find(boolean tiny){return MachineRecipeMaps.Sluice.findRecipe(List.of(item(tiny?"crushedTiny":"crushed","Dolamide",1)),List.of(fluid("Water",tiny?100:900)),false,1,16);}
 private static Recipe winner(GameTestHelper h){
  Recipe r=find(false);h.assertTrue(r!=null&&find(false)==r,"startup winner warm lookup");
  h.assertTrue(r.mInputs.length==1&&r.mOutputs.length==1&&r.mOutputs[0].is(Items.EMERALD)&&r.mOutputs[0].getCount()==1
   &&r.mDuration==53&&r.mEUt==16&&r.getOutputChance(0)==10000&&r.mFluidInputs.length==1&&r.mFluidInputs[0].getAmount()==900
   &&r.mFluidOutputs.length==1&&r.mFluidOutputs[0].getAmount()==37&&r.mFluidOutputs[0].getFluid()==fluid("Sluice",37).getFluid(),"literal public startup row");return r;
 }
 private static Recipe continuation(GameTestHelper h){
  Recipe r=find(true);h.assertTrue(r!=null&&find(true)==r,"tiny source continuation warm lookup");
  String[] names={"Dolamide","Dilithium","DiduraniumTrioxide","DuraniumHexafluoride","DuraniumHexabromide","Dolamide","Dolamide","Dolamide","Dolamide"};
  h.assertTrue(r.mOutputs.length==9,"literal tiny source slots");
  for(int i=0;i<9;i++)h.assertTrue(ItemStack.matches(r.mOutputs[i],item("crushedPurifiedTiny",names[i],1))&&r.getOutputChance(i)==(i==0?10000:300),"literal source tiny output/chance "+i);
  h.assertTrue(r.mDuration==16&&r.mEUt==16&&r.mFluidInputs.length==1&&r.mFluidInputs[0].getAmount()==100
   &&r.mFluidOutputs.length==1&&r.mFluidOutputs[0].getAmount()==100&&r.mFluidOutputs[0].getFluid()==fluid("Sluice",100).getFluid(),"tiny source work/fluid contract");
  return r;
 }
 @SuppressWarnings("unchecked") private static Set<Recipe> owned() throws Exception {return (Set<Recipe>)Class.forName("gregtech.content.recipe.SourceOreOutputRows").getMethod("owned").invoke(null);}
// Members for OreFormOutputRegressionTests. Requires Recipe, ItemStack, loader FluidStack;
// uses fully-qualified names otherwise. No production resolver/source-policy helper is consulted.
// This is a semantic snapshot, never an ownership key. Unknown special payload types fail closed.
private static String fingerprint(Recipe row) {
    if (row == null) return "null-recipe";
    var fields = new java.util.ArrayList<String>();
    fields.add(row.getClass().getName());
    fields.add(fpObject(row.mInputs)); fields.add(fpObject(row.mOutputs));
    fields.add(fpObject(row.mFluidInputs)); fields.add(fpObject(row.mFluidOutputs));
    fields.add(fpObject(row.mChances)); fields.add(fpObject(row.mMaxChances));
    fields.add(Long.toString(row.mDuration)); fields.add(Long.toString(row.mEUt));
    fields.add(Long.toString(row.mSpecialValue)); fields.add(fpObject(row.mSpecialItems));
    fields.add("flags=" + row.mEnabled + "," + row.mHidden + "," + row.mFakeRecipe + ","
            + row.mCanBeBuffered + "," + row.mNeedsEmptyOutput + "," + row.mMaterialRecovery + ","
            + row.mRequiresEmptyContainerInputs + "," + row.mExactItemInputs + "," + row.mExplicitCatalystsOnly);
    fields.add(fpObject(row.explicitCatalystInputs()));
    fields.add(fpObject(row.explicitCatalystFluidInputs()));
    fields.add(fpObject(row.explicitPresenceFluidInputs()));
    for (int slot = 0; slot < row.mInputs.length; slot++) {
        fields.add("effective-catalyst=" + slot + ":" + row.isCatalystInput(slot));
        fields.add("viewer=" + slot + ":" + fpObject(row.viewerInputAlternatives(slot)));
    }
    return fpSequence("recipe", fields);
}
private static String fpItem(ItemStack stack) {
    if (stack == null) return "null-item";
    if (stack.isEmpty()) return "empty-item";
//? if >=1.21.1 {
/*    var server = java.util.Objects.requireNonNull(
            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer(), "fingerprint requires running native server");
    return "item:" + fpNbt(stack.save(server.registryAccess()));
*///?} else {
    return "item:" + fpNbt(stack.save(new net.minecraft.nbt.CompoundTag()));
//?}
}
private static String fpFluid(FluidStack stack) {
    if (stack == null) return "null-fluid";
    if (stack.isEmpty()) return "empty-fluid";
//? if >=1.21.1 {
/*    var server = java.util.Objects.requireNonNull(
            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer(), "fingerprint requires running native server");
    return "fluid:" + fpNbt(stack.save(server.registryAccess()));
*///?} else {
    return "fluid:" + fpNbt(stack.writeToNBT(new net.minecraft.nbt.CompoundTag()));
//?}
}
private static String fpNbt(net.minecraft.nbt.Tag tag) {
    if (tag == null) return "null-nbt";
    if (tag instanceof net.minecraft.nbt.CompoundTag compound) {
        var fields = new java.util.ArrayList<String>();
        var keys = new java.util.ArrayList<>(compound.getAllKeys());
        java.util.Collections.sort(keys);
        for (String key : keys) { fields.add(key); fields.add(fpNbt(compound.get(key))); }
        return fpSequence("compound", fields);
    }
    if (tag instanceof net.minecraft.nbt.ListTag list) {
        var fields = new java.util.ArrayList<String>();
        for (int i = 0; i < list.size(); i++) fields.add(fpNbt(list.get(i)));
        return fpSequence("list:" + list.getElementType(), fields);
    }
    // Primitive scalar/array SNBT is ordered, exact and type-tagged; compound order handled above.
    return tag.getId() + ":" + tag.toString();
}
private static String fpSequence(String kind, java.util.List<String> values) {
    var result = new StringBuilder(kind).append('[').append(values.size()).append(':');
    for (String value : values) result.append(value.length()).append(':').append(value);
    return result.append(']').toString();
}
private static String fpObject(Object value) {
    if (value == null) return "null";
    if (value instanceof ItemStack item) return fpItem(item);
    if (value instanceof FluidStack fluid) return fpFluid(fluid);
    if (value instanceof net.minecraft.nbt.Tag tag) return fpNbt(tag);
    if (value instanceof net.minecraft.world.item.Item item)
        return "item-id:" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
    if (value instanceof net.minecraft.world.level.material.Fluid fluid)
        return "fluid-id:" + net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid);
    if (value instanceof String || value instanceof Number || value instanceof Boolean
            || value instanceof Character || value instanceof net.minecraft.resources.ResourceLocation)
        return value.getClass().getName() + ":" + value;
    if (value instanceof Enum<?> enumeration)
        return "enum:" + enumeration.getDeclaringClass().getName() + ":" + enumeration.name();
    if (value.getClass().isArray()) {
        var entries = new java.util.ArrayList<String>();
        for (int i = 0; i < java.lang.reflect.Array.getLength(value); i++)
            entries.add(fpObject(java.lang.reflect.Array.get(value, i)));
        return fpSequence("array:" + value.getClass().getComponentType().getName(), entries);
    }
    if (value instanceof java.util.Map<?, ?> map) {
        var entries = new java.util.ArrayList<String>();
        map.forEach((key, entry) -> entries.add(fpSequence("entry", java.util.List.of(fpObject(key), fpObject(entry)))));
        java.util.Collections.sort(entries);
        return fpSequence("map", entries);
    }
    if (value instanceof java.util.Collection<?> collection) {
        var entries = new java.util.ArrayList<String>();
        for (Object entry : collection) entries.add(fpObject(entry));
        if (value instanceof java.util.Set<?>) java.util.Collections.sort(entries);
        return fpSequence(value instanceof java.util.Set<?> ? "set" : "sequence", entries);
    }
    throw new IllegalArgumentException("Unhandled recipe special payload; add explicit semantic codec: " + value.getClass().getName());
}

}
