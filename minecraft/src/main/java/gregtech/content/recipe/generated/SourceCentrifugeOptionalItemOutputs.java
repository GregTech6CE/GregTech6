package gregtech.content.recipe.generated;

import gregtech.content.recipe.SourceCentrifugeOptionalBindings;
import gregtech.content.recipe.SourceCentrifugeOptionalBindings.*;
import gregtech.engine.material.MaterialEquivalence;
import java.util.*;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.neoforged.fml.ModList;
*///?} else {
import net.minecraftforge.fml.ModList;
//?}

/** C-producer-local named optional holders. Not an addon API, tag resolver, global unifier,
 * or compatibility claim for unlisted providers/versions. All injected views are local values;
 * no registry/mod list/table mutation is used by isolated contracts. */
final class SourceCentrifugeOptionalItemOutputs {
    private SourceCentrifugeOptionalItemOutputs() {}
    enum Action { APPEND, OMIT_OPTIONAL_TAIL, DEFER_ROW }
    record Result(Action action, ItemStack stack, Selection selection) {}

    static Snapshot nativeSnapshot() {
        var mods=ModList.get();
//? if >=1.21.1 {
        /*String loader="neoforge";*/
//?} else {
        String loader="forge";
//?}
        if(mods==null)return new Snapshot("",loader,false,Map.of());
        Map<String,String> versions=new TreeMap<>();
        for(var mod:mods.getMods()){
            if(mod.getModId()==null||mod.getVersion()==null||versions.putIfAbsent(mod.getModId(),mod.getVersion().toString())!=null)
                return new Snapshot("",loader,false,Map.of());
        }
        // getMods is the complete loader list, not a filter of recognized providers.
        boolean complete=versions.containsKey("minecraft")&&versions.containsKey(loader)&&versions.containsKey("gt6ce");
        return new Snapshot(versions.getOrDefault("minecraft",""),loader,complete,versions);
    }

    static Result resolve(Plan plan) {
        return resolve(plan,nativeSnapshot(),SourceCentrifugeOptionalItemOutputs::exactItem,GeneratedRecipeResolver::resolveSpec);
    }

    /** Named package-local test seam. A synthetic supplied view is not an installed optional mod. */
    static Result resolve(Plan plan,Snapshot snapshot,Function<String,ItemStack> exactLookup,
                          Function<String,ItemStack> materialLookup) {
        Map<String,ItemStack> observed=new HashMap<>();
        Function<String,ItemStack> exact=id->observed.computeIfAbsent(id,key->{
            try {
                ItemStack stack=exactLookup.apply(key);
                return plainNamedStack(stack)?copyAmountOne(stack):ItemStack.EMPTY;
            } catch (RuntimeException failedHolder) { return ItemStack.EMPTY; }
        });
        Selection selected=SourceCentrifugeOptionalBindings.choose(plan,snapshot,id->!exact.apply(id).isEmpty());
        if(selected.choice()==Choice.DEFER)return unresolved(plan,selected);
        final ItemStack stack;
        try {
            stack=selected.target().kind()==TargetKind.EXACT_ITEM
                    ?exact.apply(selected.target().value()):materialLookup.apply(selected.target().value());
        } catch (RuntimeException failedDefault) { return unresolved(plan,selected); }
        if(stack==null||stack.isEmpty())return unresolved(plan,selected);
        // OM material fallback can legitimately be four small/nine tiny dust: do NOT force count1.
        return new Result(Action.APPEND,stack.copy(),selected);
    }

    private static Result unresolved(Plan plan,Selection selected) {
        return new Result(plan.unknownTreatment()==UnknownTreatment.PRESERVE_EXISTING_PREFIX
                ?Action.OMIT_OPTIONAL_TAIL:Action.DEFER_ROW,ItemStack.EMPTY,selected);
    }
    private static ItemStack copyAmountOne(ItemStack stack){ItemStack copy=stack.copy();copy.setCount(1);return copy;}
    private static boolean plainNamedStack(ItemStack stack){
        if(stack==null||stack.isEmpty()||stack.isDamaged())return false;
//? if >=1.21.1 {
        /*if(!stack.getComponentsPatch().isEmpty())return false;*/
//?} else {
        if(stack.hasTag())return false;
//?}
        // Original IL.get applies OM unification. A related explicit native material form
        // requires a reviewed target policy; defer rather than forcibly choosing the named item.
        return MaterialEquivalence.form(stack)==null;
    }
    private static ItemStack exactItem(String id){
        ResourceLocation key=ResourceLocation.tryParse(id);
        if(key==null||!BuiltInRegistries.ITEM.containsKey(key))return ItemStack.EMPTY;
        Item item=BuiltInRegistries.ITEM.get(key);
        if(item==null||item==Items.AIR||!key.equals(BuiltInRegistries.ITEM.getKey(item)))return ItemStack.EMPTY;
        return new ItemStack(item,1);
    }
}
