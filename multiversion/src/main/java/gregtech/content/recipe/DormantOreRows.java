package gregtech.content.recipe;

import gregtech.internal.material.GTMaterial;
import gregtech.internal.recipe.Recipe;
import gregtech.internal.recipe.RecipeMap;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.*;
import java.util.function.Predicate;

/** caller-owned immutable definitions, never discovered from live recipe signatures.
 * Instances are lifecycle-confined. No constructor/definition admission, no global map sweep.
 * Only real, ordinary one-input OPL maceration/washing rows are in this proposed slice.
 */
public final class DormantOreRows {
    private record Key(GTMaterial material, String producer, RecipeMap map) {}
    private static final class Definition {
        final Key key;
        final boolean optimize;
        final ItemStack input;
        final ItemStack[] outputs;
        final long[] chances;
        final FluidStack[] fluids;
        final long duration, eu;
        Definition(Key key, boolean optimize, ItemStack input, ItemStack[] outputs,
                   long[] chances, FluidStack[] fluids, long duration, long eu) {
            this.key=key; this.optimize=optimize; this.input=input.copy();
            this.outputs=copy(outputs);this.chances=chances==null?null:chances.clone();
            this.fluids=copy(fluids);this.duration=duration;this.eu=eu;
        }
        Recipe admit() {
            // Same public admission path as addRecipe1: optimize, validate, collision-check.
            return key.map.addRecipe(optimize,new ItemStack[]{input.copy()},copy(outputs),null,
                    chances==null?null:chances.clone(),copy(fluids),null,duration,eu,0);
        }
    }
    private final LinkedHashMap<Key,Definition> definitions=new LinkedHashMap<>();
    private final IdentityHashMap<RecipeMap,Set<Recipe>> accepted=new IdentityHashMap<>();
    private boolean sealed;
    public void define(GTMaterial material,String producer,RecipeMap map,boolean optimize,
                       ItemStack input,ItemStack[] outputs,long[] chances,FluidStack[] fluids,long duration,long eu) {
        if(sealed)throw new IllegalStateException("Definition phase already sealed");
        Objects.requireNonNull(material);Objects.requireNonNull(producer);Objects.requireNonNull(map);
        if(material.resolve()!=material||input==null||input.isEmpty())throw new IllegalArgumentException("Canonical material/input required");
        Key key=new Key(material,producer,map);
        if(definitions.containsKey(key))throw new IllegalStateException("Duplicate producer definition: "+producer);
        definitions.put(key,new Definition(key,optimize,input,outputs,chances,fluids,duration,eu));
    }
    public void seal(){sealed=true;}
    public int definitionCount(){return definitions.size();}
    /** Exclude these exact accepted instances from first baseline capture. Never excludes definitions. */
    public Set<Recipe> owned(){
        Set<Recipe> rows=Collections.newSetFromMap(new IdentityHashMap<>());
        accepted.values().forEach(rows::addAll);return Collections.unmodifiableSet(rows);
    }
    /** Clear after oven mirrors unwind. Retain definitions for a subsequent integrated server. */
    public void clear(){
        accepted.forEach((map,rows)->rows.forEach(map::unlink));accepted.clear();
    }
    /** Caller supplies one current authoritative material snapshot after external tag rebuild.
     * NO means dormant. A tag-required continuation is UNKNOWN/defer, not source ORES YES.
     * Existing startup/external rows always win admission collisions and are never claimed.
     */
    public int reconcile(Predicate<GTMaterial> active){
        if(!sealed)throw new IllegalStateException("Definition phase not sealed");
        // Evaluate all eligibility before touching map state. No hot-lookup metadata scan.
        var enabled=new ArrayList<Definition>();
        for(var d:definitions.values())if(active.test(d.key.material))enabled.add(d);
        clear();
        try {
            for(var d:enabled){
                Recipe row=d.admit();
                if(row!=null)accepted.computeIfAbsent(d.key.map,k->Collections.newSetFromMap(new IdentityHashMap<>())).add(row);
            }
        } catch(RuntimeException|Error failure){clear();throw failure;}
        return owned().size();
    }
    private static ItemStack[] copy(ItemStack[] stacks){
        if(stacks==null)return new ItemStack[0];
        ItemStack[] out=new ItemStack[stacks.length];
        for(int i=0;i<out.length;i++)out[i]=stacks[i]==null?ItemStack.EMPTY:stacks[i].copy();return out;
    }
    private static FluidStack[] copy(FluidStack[] stacks){
        if(stacks==null)return new FluidStack[0];
        FluidStack[] out=new FluidStack[stacks.length];
        for(int i=0;i<out.length;i++)out[i]=stacks[i]==null?FluidStack.EMPTY:stacks[i].copy();return out;
    }
}
