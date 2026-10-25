package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.*;
import java.util.function.Supplier;

/** Exact caller-owned OPL Sluice output definitions, admitted after tags bind.
 * This ledger never discovers ownership by recipe signature and never suppresses input forms.
 */
public final class SourceOreOutputRows {
    private SourceOreOutputRows() {}
    private record Key(GTMaterial material, String producer, RecipeMap map) {}
    private static final Map<Key,Supplier<Recipe>> DEFINITIONS = new LinkedHashMap<>();
    private static final IdentityHashMap<RecipeMap,Set<Recipe>> ACCEPTED = new IdentityHashMap<>();
    private static final ThreadLocal<Boolean> RESOLVING = ThreadLocal.withInitial(() -> false);
    private static boolean sealed;
    public static boolean affects(List<GTMaterial> materials) {
        return materials.stream().anyMatch(material -> ReviewedCrushedFormGeneration.sourceExcluded(
                material,MaterialPrefix.crushedPurifiedTiny));
    }
    public static void define(GTMaterial material,String producer,RecipeMap map,Supplier<Recipe> admission) {
        if (sealed) throw new IllegalStateException("Source output definitions already sealed");
        var key = new Key(material.resolve(),producer,map);
        if (DEFINITIONS.putIfAbsent(key,Objects.requireNonNull(admission)) != null)
            throw new IllegalStateException("Duplicate source output definition: " + producer);
    }
    public static void seal() { sealed=true; }
    static boolean resolving() { return RESOLVING.get(); }
    public static Set<Recipe> owned() {
        Set<Recipe> result=Collections.newSetFromMap(new IdentityHashMap<>());
        ACCEPTED.values().forEach(result::addAll);
        return Collections.unmodifiableSet(result);
    }
    public static void clear() {
        ACCEPTED.forEach((map,rows)->rows.forEach(map::unlink));
        ACCEPTED.clear();
    }
    /** MachineRecipeOverlay calls only after ExternalOreProcessing has rebuilt bound aliases. */
    public static void rebuild() {
        if (!sealed) throw new IllegalStateException("Source output definitions not sealed");
        clear();
        RESOLVING.set(true);
        try {
            for (var entry:DEFINITIONS.entrySet()) {
                Recipe row=entry.getValue().get(); // normal addRecipe validates and rejects collisions
                if (row!=null) ACCEPTED.computeIfAbsent(entry.getKey().map(),
                        ignored->Collections.newSetFromMap(new IdentityHashMap<>())).add(row);
            }
        } catch (RuntimeException|Error failure) { clear(); throw failure; }
        finally { RESOLVING.remove(); }
    }
}
